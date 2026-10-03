package com.magnet.downloader;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.magnet.downloader.adapter.TaskAdapter;
import com.magnet.downloader.engine.DownloadEngine;
import com.magnet.downloader.engine.DownloadListener;
import com.magnet.downloader.model.DownloadTask;
import com.magnet.downloader.util.FileUtils;
import com.magnet.downloader.util.Haptic;
import com.magnet.downloader.util.PermissionUtils;
import com.magnet.downloader.util.Prefs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final List<DownloadTask> tasks = new ArrayList<>();
    private TaskAdapter adapter;
    private RecyclerView rv;
    private EditText etUrl;
    private TextView tvEmpty, tvCount;
    private View btnPaste, btnStart;
    private boolean dataLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.init(this);
        setContentView(R.layout.activity_main);

        etUrl = findViewById(R.id.et_url);
        tvEmpty = findViewById(R.id.tv_empty);
        tvCount = findViewById(R.id.tv_count);
        btnPaste = findViewById(R.id.btn_paste);
        btnStart = findViewById(R.id.btn_start);

        // 权限
        if (!PermissionUtils.hasStorage(this)) PermissionUtils.requestStorage(this);
        PermissionUtils.requestNotification(this);

        // 恢复任务
        loadTasks();

        // 列表
        rv = findViewById(R.id.rv_tasks);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setInitialPrefetchItemCount(10);
        rv.setLayoutManager(lm);
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(20);
        rv.setItemAnimator(null);

        adapter = new TaskAdapter(tasks, new TaskAdapter.Callback() {
            @Override public void onOpen(DownloadTask t) { openFile(t); }
            @Override public void onCancel(DownloadTask t) {
                DownloadEngine.cancel(t.id);
                t.state = DownloadTask.STATE_CANCELLED;
                adapter.notifyDataSetChanged();
                saveTasks();
            }
            @Override public void onRetry(DownloadTask t) {
                t.state = DownloadTask.STATE_WAITING;
                t.downloadedBytes = 0;
                t.error = null;
                adapter.notifyDataSetChanged();
                startTask(t);
            }
            @Override public void onDelete(DownloadTask t) {
                try { new File(t.savePath).delete(); } catch (Throwable ignored) {}
                tasks.remove(t);
                adapter.notifyDataSetChanged();
                saveTasks();
                Toast.makeText(MainActivity.this, "已删除", Toast.LENGTH_SHORT).show();
            }
        });
        rv.setAdapter(adapter);

        // 粘贴
        btnPaste.setOnClickListener(v -> {
            Haptic.tap(v);
            pasteFromClipboard();
        });

        // 开始
        btnStart.setOnClickListener(v -> {
            Haptic.tap(v);
            pressEffect(v);
            String url = etUrl.getText().toString().trim();
            if (TextUtils.isEmpty(url)) {
                Toast.makeText(this, "请粘贴或输入链接", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Toast.makeText(this, "只支持 http/https 链接", Toast.LENGTH_LONG).show();
                return;
            }
            addTask(url);
            etUrl.setText("");
        });

        updateEmpty();
    }

    private void pasteFromClipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip()) {
                Toast.makeText(this, "剪贴板为空", Toast.LENGTH_SHORT).show();
                return;
            }
            ClipData clip = cm.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return;
            CharSequence text = clip.getItemAt(0).coerceToText(this);
            if (text == null || text.length() == 0) {
                Toast.makeText(this, "剪贴板为空", Toast.LENGTH_SHORT).show();
                return;
            }
            String s = text.toString().trim();
            etUrl.setText(s);
            etUrl.setSelection(s.length());
        } catch (Throwable t) {
            Toast.makeText(this, "读取剪贴板失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void addTask(String url) {
        DownloadTask task = new DownloadTask();
        task.url = url;
        task.state = DownloadTask.STATE_WAITING;
        tasks.add(0, task);
        adapter.notifyItemInserted(0);
        rv.scrollToPosition(0);
        updateEmpty();
        saveTasks();
        startTask(task);
    }

    private void startTask(DownloadTask task) {
        task.state = DownloadTask.STATE_DOWNLOADING;
        DownloadEngine.start(this, task, new DownloadListener() {
            @Override public void onStart(DownloadTask t, String fileName, long totalBytes) {
                t.fileName = fileName;
                t.totalBytes = totalBytes;
                int idx = tasks.indexOf(t);
                if (idx >= 0) adapter.notifyItemChanged(idx);
            }
            @Override public void onProgress(DownloadTask t, long downloaded, long total, int pct, long speedBps) {
                t.downloadedBytes = downloaded;
                t.totalBytes = total;
                t.speedBps = speedBps;
                int idx = tasks.indexOf(t);
                if (idx >= 0) adapter.notifyItemChanged(idx);
            }
            @Override public void onDone(DownloadTask t, File file) {
                t.state = DownloadTask.STATE_DONE;
                t.savePath = file.getAbsolutePath();
                t.downloadedBytes = t.totalBytes;
                int idx = tasks.indexOf(t);
                if (idx >= 0) adapter.notifyItemChanged(idx);
                Haptic.done(MainActivity.this);
                Toast.makeText(MainActivity.this,
                        "下载完成: " + file.getName() + "\n" + file.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();
                saveTasks();
            }
            @Override public void onError(DownloadTask t, String error) {
                t.state = DownloadTask.STATE_ERROR;
                t.error = error;
                int idx = tasks.indexOf(t);
                if (idx >= 0) adapter.notifyItemChanged(idx);
                Haptic.error(MainActivity.this);
                Toast.makeText(MainActivity.this, "失败: " + error, Toast.LENGTH_LONG).show();
                saveTasks();
            }
            @Override public void onCancel(DownloadTask t) {
                t.state = DownloadTask.STATE_CANCELLED;
                int idx = tasks.indexOf(t);
                if (idx >= 0) adapter.notifyItemChanged(idx);
                saveTasks();
            }
        });
    }

    private void openFile(DownloadTask t) {
        try {
            File f = new File(t.savePath);
            if (!f.exists()) {
                Toast.makeText(this, "文件已丢失", Toast.LENGTH_SHORT).show();
                return;
            }
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", f);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "*/*");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(i, "打开方式"));
        } catch (Throwable e) {
            Toast.makeText(this, "无法打开: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateEmpty() {
        tvEmpty.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
        tvCount.setText(tasks.size() + " 项");
    }

    private void pressEffect(View v) {
        v.animate().cancel();
        v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f)
                        .setDuration(280)
                        .setInterpolator(new OvershootInterpolator(2.4f))
                        .start()).start();
    }

    // 简单持久化（SharedPreferences + 分隔符）
    private void saveTasks() {
        try {
            StringBuilder sb = new StringBuilder();
            int n = 0;
            for (DownloadTask t : tasks) {
                if (n++ >= 50) break;
                sb.append(t.id).append('\u0001')
                  .append(t.url == null ? "" : t.url).append('\u0001')
                  .append(t.fileName == null ? "" : t.fileName).append('\u0001')
                  .append(t.savePath == null ? "" : t.savePath).append('\u0001')
                  .append(t.totalBytes).append('\u0001')
                  .append(t.downloadedBytes).append('\u0001')
                  .append(t.state).append('\u0002');
            }
            Prefs.put("tasks", sb.toString());
        } catch (Throwable ignored) {}
    }

    private void loadTasks() {
        if (dataLoaded) return;
        dataLoaded = true;
        try {
            String s = Prefs.getString("tasks", "");
            if (s == null || s.isEmpty()) return;
            for (String line : s.split("\u0002")) {
                if (line.isEmpty()) continue;
                String[] p = line.split("\u0001");
                if (p.length < 7) continue;
                DownloadTask t = new DownloadTask();
                t.id = Long.parseLong(p[0]);
                t.url = p[1];
                t.fileName = p[2];
                t.savePath = p[3];
                t.totalBytes = Long.parseLong(p[4]);
                t.downloadedBytes = Long.parseLong(p[5]);
                t.state = Integer.parseInt(p[6]);
                if (t.state == DownloadTask.STATE_DOWNLOADING)
                    t.state = DownloadTask.STATE_WAITING;
                tasks.add(t);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms, @NonNull int[] res) {
        super.onRequestPermissionsResult(req, perms, res);
        if (req == PermissionUtils.REQ_STORAGE) {
            boolean ok = res.length > 0 && res[0] == 0;
            if (!ok) Toast.makeText(this, "没有存储权限，无法保存文件", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        saveTasks();
    }
}
