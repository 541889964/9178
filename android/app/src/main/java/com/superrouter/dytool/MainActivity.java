package com.superrouter.dytool;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERM = 2001;

    private EditText etInput;
    private Button btnPaste, btnClear, btnParse, btnVideo, btnAudio, btnCover;
    private LinearLayout boxStatus, cardResult;
    private ProgressBar progressSmall, progressBar;
    private TextView tvStatus, tvTitle, tvTags, tvMeta, tvBadge;
    private ImageView ivCover;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private final DyParser parser = new DyParser();
    private final OkHttpClient http = App.http();

    private DyItem current;
    private volatile boolean busy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        bindActions();
        startKeepAlive();
        requestPermsIfNeed();
        handleShareIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleShareIntent(intent);
    }

    /** 从抖音「分享到」本应用时自动填入 */
    private void handleShareIntent(Intent intent) {
        if (intent == null) return;
        if (!Intent.ACTION_SEND.equals(intent.getAction())) return;
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (TextUtils.isEmpty(text)) return;
        etInput.setText(text);
        ui.postDelayed(this::doParse, 260);
    }

    private void bindViews() {
        etInput = findViewById(R.id.etInput);
        btnPaste = findViewById(R.id.btnPaste);
        btnClear = findViewById(R.id.btnClear);
        btnParse = findViewById(R.id.btnParse);
        btnVideo = findViewById(R.id.btnVideo);
        btnAudio = findViewById(R.id.btnAudio);
        btnCover = findViewById(R.id.btnCover);
        boxStatus = findViewById(R.id.boxStatus);
        cardResult = findViewById(R.id.cardResult);
        progressSmall = findViewById(R.id.progressSmall);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        tvTitle = findViewById(R.id.tvTitle);
        tvTags = findViewById(R.id.tvTags);
        tvMeta = findViewById(R.id.tvMeta);
        tvBadge = findViewById(R.id.tvBadge);
        ivCover = findViewById(R.id.ivCover);
    }

    private void bindActions() {
        btnPaste.setOnClickListener(v -> doPaste());
        btnClear.setOnClickListener(v -> {
            etInput.setText("");
            cardResult.setVisibility(View.GONE);
            hideStatus();
            current = null;
        });
        btnParse.setOnClickListener(v -> doParse());
        btnVideo.setOnClickListener(v -> doDownloadVideo());
        btnAudio.setOnClickListener(v -> doExtractAudio());
        btnCover.setOnClickListener(v -> doDownloadCover());
    }

    // ---------------- 权限 ----------------
    private void requestPermsIfNeed() {
        List<String> need = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
                need.add("android.permission.POST_NOTIFICATIONS");
            }
        }
        if (Build.VERSION.SDK_INT <= 28) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                need.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        if (!need.isEmpty()) {
            ActivityCompat.requestPermissions(this, need.toArray(new String[0]), REQ_PERM);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM) {
            for (int i = 0; i < permissions.length && i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED
                        && Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[i])) {
                    toast("未授予存储权限，保存到公共目录可能失败");
                }
            }
        }
    }

    private void startKeepAlive() {
        try {
            Intent i = new Intent(this, KeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(i);
            } else {
                startService(i);
            }
        } catch (Exception ignored) {
        }
    }

    // ---------------- 交互 ----------------
    private void doPaste() {
        try {
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) getSystemService(Activity.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip()) {
                toast("剪贴板是空的");
                return;
            }
            CharSequence cs = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
            if (cs == null || cs.length() == 0) {
                toast("剪贴板是空的");
                return;
            }
            etInput.setText(cs.toString());
            etInput.setSelection(etInput.getText().length());
            toast("已粘贴");
        } catch (Exception e) {
            toast("读取剪贴板失败");
        }
    }

    private void doParse() {
        if (busy) {
            toast("正在处理中，请稍候");
            return;
        }
        String raw = etInput.getText().toString().trim();
        if (raw.isEmpty()) {
            toast("请先粘贴抖音分享链接");
            etInput.requestFocus();
            return;
        }
        final String link = DyParser.extractUrl(raw);
        if (link == null) {
            toast("没找到链接，请粘贴完整分享内容");
            return;
        }
        if (!DyParser.isDouyin(link)) {
            toast("只支持抖音 douyin.com 链接");
            return;
        }

        hideKeyboard();
        setBusy(true, "正在解析，请稍候…");
        cardResult.setVisibility(View.GONE);

        pool.execute(() -> {
            try {
                DyItem item = parser.parse(link);
                ui.post(() -> {
                    current = item;
                    render(item);
                    setBusy(false, null);
                    showStatusOk("解析成功");
                    loadCover(item.cover);
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> {
                    setBusy(false, null);
                    showStatusErr("解析失败：" + msg);
                    toast("解析失败");
                });
            }
        });
    }

    private void doDownloadVideo() {
        if (current == null) {
            toast("请先解析");
            return;
        }
        if (busy) {
            toast("正在处理中，请稍候");
            return;
        }
        setBusy(true, "正在下载视频…");

        pool.execute(() -> {
            try {
                File dest = new File(App.outDir(), current.safeName() + ".mp4");
                File f = Downloader.download(current.videoUrls, dest, (pct, done, total) ->
                        ui.post(() -> showProgress(pct, "正在下载视频 ", done, total)), false);

                ui.post(() -> showProgress(-2, "正在保存到相册…", 0, 0));
                String path = MediaSaver.save(this, f, 0);

                ui.post(() -> {
                    setBusy(false, null);
                    hideProgress();
                    showStatusOk("视频已保存");
                    toast("✅ 视频已保存
" + path);
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> {
                    setBusy(false, null);
                    hideProgress();
                    showStatusErr("下载失败：" + msg);
                    toast("下载失败");
                });
            }
        });
    }

    /** 只解析音频：下载视频 → 抽出音轨 → 存为 m4a */
    private void doExtractAudio() {
        if (current == null) {
            toast("请先解析");
            return;
        }
        if (busy) {
            toast("正在处理中，请稍候");
            return;
        }
        setBusy(true, "正在提取音频…");

        pool.execute(() -> {
            File videoTmp = null;
            try {
                File cacheDir = App.cacheDirCustom();
                String base = current.safeName();
                videoTmp = new File(cacheDir, base + "_src.mp4");

                File f = Downloader.download(current.videoUrls, videoTmp, (pct, done, total) ->
                        ui.post(() -> showProgress(pct, "正在下载音源 ", done, total)), false);

                ui.post(() -> showProgress(-2, "正在提取音轨…", 0, 0));

                File out = new File(App.outDir(), base + ".m4a");
                AudioExtractor.extract(f, out);

                ui.post(() -> showProgress(-2, "正在保存到音乐库…", 0, 0));
                String path = MediaSaver.save(this, out, 1);

                ui.post(() -> {
                    setBusy(false, null);
                    hideProgress();
                    showStatusOk("音频已提取");
                    toast("🎵 音频已保存
" + path);
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> {
                    setBusy(false, null);
                    hideProgress();
                    showStatusErr("提取失败：" + msg);
                    toast("音频提取失败");
                });
            } finally {
                if (videoTmp != null && videoTmp.exists()) videoTmp.delete();
            }
        });
    }

    private void doDownloadCover() {
        if (current == null) {
            toast("请先解析");
            return;
        }
        if (TextUtils.isEmpty(current.cover)) {
            toast("该作品没有封面");
            return;
        }
        if (busy) {
            toast("正在处理中，请稍候");
            return;
        }
        setBusy(true, "正在下载封面…");

        pool.execute(() -> {
            try {
                File dest = new File(App.outDir(), current.safeName() + "_cover.jpg");
                List<String> urls = new ArrayList<>();
                urls.add(current.cover);

                File f = Downloader.download(urls, dest, null, true);
                String path = MediaSaver.save(this, f, 2);

                ui.post(() -> {
                    setBusy(false, null);
                    showStatusOk("封面已保存");
                    toast("🖼 封面已保存
" + path);
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> {
                    setBusy(false, null);
                    showStatusErr("封面下载失败：" + msg);
                    toast("封面下载失败");
                });
            }
        });
    }

    // ---------------- 渲染 ----------------
    private void render(DyItem it) {
        tvTitle.setText(it.title.isEmpty() ? "抖音视频" : it.title);

        if (it.tags.isEmpty()) {
            tvTags.setVisibility(View.GONE);
        } else {
            tvTags.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < it.tags.size(); i++) {
                if (i > 0) sb.append("  ");
                sb.append('#').append(it.tags.get(i));
            }
            tvTags.setText(sb.toString());
        }

        StringBuilder meta = new StringBuilder();
        if (!it.author.isEmpty()) meta.append("👤 ").append(it.author);
        if (it.width > 0 && it.height > 0) {
            if (meta.length() > 0) meta.append("    ");
            meta.append("📐 ").append(it.width).append('×').append(it.height);
        }
        if (it.sizeBytes > 0) {
            if (meta.length() > 0) meta.append("
");
            meta.append("💾 ").append(fmtSize(it.sizeBytes));
        }
        if (it.durationSec > 0) {
            if (meta.length() > 0) meta.append("    ");
            meta.append("⏱ ").append(fmtDuration(it.durationSec));
        }
        tvMeta.setText(meta.toString());

        tvBadge.setText(it.watermark ? "含水印" : "无水印");
        tvBadge.setTextColor(ContextCompat.getColor(this,
                it.watermark ? R.color.err : R.color.ok));

        cardResult.setVisibility(View.VISIBLE);
        cardResult.setAlpha(0f);
        cardResult.animate().alpha(1f).setDuration(220).start();
    }

    private void loadCover(String url) {
        if (TextUtils.isEmpty(url)) return;
        pool.execute(() -> {
            try {
                Request req = new Request.Builder()
                        .url(url)
                        .header("User-Agent", App.UA)
                        .header("Referer", "https://www.douyin.com/")
                        .build();
                try (Response r = http.newCall(req).execute()) {
                    ResponseBody b = r.body();
                    if (b == null) return;
                    InputStream in = b.byteStream();
                    Bitmap bmp = BitmapFactory.decodeStream(in);
                    in.close();
                    if (bmp != null) ui.post(() -> ivCover.setImageBitmap(bmp));
                }
            } catch (Exception ignored) {
            }
        });
    }

    // ---------------- 状态 ----------------
    private void setBusy(boolean b, String msg) {
        busy = b;
        btnParse.setEnabled(!b);
        btnVideo.setEnabled(!b);
        btnAudio.setEnabled(!b);
        btnCover.setEnabled(!b);
        btnParse.setAlpha(b ? 0.55f : 1f);
        if (b && msg != null) showStatus(msg);
    }

    private void showStatus(String msg) {
        boxStatus.setVisibility(View.VISIBLE);
        progressSmall.setVisibility(View.VISIBLE);
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.sub));
        tvStatus.setText(msg);
    }

    private void showStatusOk(String msg) {
        boxStatus.setVisibility(View.VISIBLE);
        progressSmall.setVisibility(View.GONE);
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.ok));
        tvStatus.setText(msg);
    }

    private void showStatusErr(String msg) {
        boxStatus.setVisibility(View.VISIBLE);
        progressSmall.setVisibility(View.GONE);
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.err));
        tvStatus.setText(msg);
    }

    private void hideStatus() {
        boxStatus.setVisibility(View.GONE);
    }

    private void showProgress(int pct, String label, long done, long total) {
        progressBar.setVisibility(View.VISIBLE);
        if (pct < 0) {
            progressBar.setIndeterminate(true);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.sub));
            tvStatus.setText(label);
        } else {
            progressBar.setIndeterminate(false);
            progressBar.setProgress(pct);
            String txt = label + pct + "%  " + fmtSize(done);
            if (total > 0) txt += " / " + fmtSize(total);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.sub));
            tvStatus.setText(txt);
        }
        boxStatus.setVisibility(View.VISIBLE);
        progressSmall.setVisibility(View.GONE);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
        progressBar.setIndeterminate(false);
    }

    // ---------------- 工具 ----------------
    private static String fmtSize(long b) {
        if (b <= 0) return "0 B";
        String[] u = {"B", "KB", "MB", "GB"};
        double v = b;
        int i = 0;
        while (v >= 1024 && i < u.length - 1) {
            v /= 1024;
            i++;
        }
        return String.format(Locale.US, i == 0 ? "%.0f %s" : "%.1f %s", v, u[i]);
    }

    private static String fmtDuration(double sec) {
        int s = (int) Math.round(sec);
        int m = s / 60;
        int r = s % 60;
        return m > 0 ? String.format(Locale.US, "%d:%02d", m, r)
                : String.format(Locale.US, "0:%02d", r);
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(etInput.getWindowToken(), 0);
        } catch (Exception ignored) {
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pool.shutdownNow();
    }
}