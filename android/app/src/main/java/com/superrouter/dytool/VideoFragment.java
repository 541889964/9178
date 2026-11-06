package com.superrouter.dytool;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
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

public class VideoFragment extends Fragment {

    private EditText etInput;
    private MaterialButton btnPaste, btnParse, btnVideo, btnAudio, btnCover;
    private LinearLayout boxStatus;
    private MaterialCardView cardResult;
    private ProgressBar progressSmall, progressBar;
    private TextView tvStatus, tvTitle, tvTags, tvMeta, tvBadge;
    private ImageView ivCover;
    private VideoView videoView;
    private LinearLayout playMask;
    private ImageButton btnClosePreview;
    private View previewBox;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private final DyParser parser = new DyParser();
    private final OkHttpClient http = App.http();

    private DyItem current;
    private volatile boolean busy = false;
    private File previewFile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        etInput = v.findViewById(R.id.etInput);
        btnPaste = v.findViewById(R.id.btnPaste);
        btnParse = v.findViewById(R.id.btnParse);
        btnVideo = v.findViewById(R.id.btnVideo);
        btnAudio = v.findViewById(R.id.btnAudio);
        btnCover = v.findViewById(R.id.btnCover);
        boxStatus = v.findViewById(R.id.boxStatus);
        cardResult = v.findViewById(R.id.cardResult);
        progressSmall = v.findViewById(R.id.progressSmall);
        progressBar = v.findViewById(R.id.progressBar);
        tvStatus = v.findViewById(R.id.tvStatus);
        tvTitle = v.findViewById(R.id.tvTitle);
        tvTags = v.findViewById(R.id.tvTags);
        tvMeta = v.findViewById(R.id.tvMeta);
        tvBadge = v.findViewById(R.id.tvBadge);
        ivCover = v.findViewById(R.id.ivCover);
        videoView = v.findViewById(R.id.videoView);
        playMask = v.findViewById(R.id.playMask);
        btnClosePreview = v.findViewById(R.id.btnClosePreview);
        previewBox = v.findViewById(R.id.previewBox);

        btnPaste.setOnClickListener(x -> doPaste());
        btnParse.setOnClickListener(x -> doParse());
        btnVideo.setOnClickListener(x -> doDownloadVideo());
        btnAudio.setOnClickListener(x -> doExtractAudio());
        btnCover.setOnClickListener(x -> doDownloadCover());
        playMask.setOnClickListener(x -> startPreview());
        btnClosePreview.setOnClickListener(x -> stopPreview());
    }

    /** 供 MainActivity 从分享进来时调用 */
    public void parseFromText(String text) {
        if (etInput == null) return;
        etInput.setText(text);
        doParse();
    }

    // ---------------- 粘贴 ----------------
    private void doPaste() {
        try {
            Activity a = getActivity();
            if (a == null) return;
            ClipboardManager cm = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip() || cm.getPrimaryClip() == null) {
                toast("剪贴板是空的");
                return;
            }
            CharSequence cs = cm.getPrimaryClip().getItemAt(0).coerceToText(a);
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

    // ---------------- 解析 ----------------
    private void doParse() {
        if (busy) {
            toast("正在处理中，请稍候");
            return;
        }
        String raw = etInput.getText().toString().trim();
        if (raw.isEmpty()) {
            toast("请先粘贴抖音分享链接");
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
        stopPreview();
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
                });
            }
        });
    }

    // ---------------- 预览播放 ----------------
    private void startPreview() {
        if (current == null) return;
        playMask.setVisibility(View.GONE);
        ivCover.setVisibility(View.GONE);
        btnClosePreview.setVisibility(View.VISIBLE);

        // 优先播放已下载文件，否则在线播放首条直链
        File local = new File(App.outDir(), current.safeName() + ".mp4");
        String uri;
        if (local.exists() && local.length() > 1024) {
            previewFile = local;
            uri = Uri.fromFile(local).toString();
        } else {
            uri = current.videoUrls.get(0);
        }

        videoView.setVisibility(View.VISIBLE);
        videoView.setVideoURI(Uri.parse(uri));
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            videoView.start();
        });
        videoView.setOnErrorListener((mp, what, extra) -> {
            toast("在线播放失败，可先下载视频");
            stopPreview();
            return true;
        });
        videoView.setOnCompletionListener(mp -> stopPreview());
    }

    private void stopPreview() {
        try {
            if (videoView != null) {
                if (videoView.isPlaying()) videoView.pause();
                videoView.stopPlayback();
                videoView.setVisibility(View.GONE);
            }
        } catch (Exception ignored) {
        }
        if (ivCover != null) ivCover.setVisibility(View.VISIBLE);
        if (playMask != null) playMask.setVisibility(View.VISIBLE);
        if (btnClosePreview != null) btnClosePreview.setVisibility(View.GONE);
    }

    // ---------------- 下载视频 ----------------
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
                String path = MediaSaver.save(getActivity(), f, 0);

                ui.post(() -> {
                    setBusy(false, null);
                    hideProgress();
                    showStatusOk("视频已保存");
                    toast("视频已保存到 " + path);
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> {
                    setBusy(false, null);
                    hideProgress();
                    showStatusErr("下载失败：" + msg);
                });
            }
        });
    }

    // ---------------- 提取音频 ----------------
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
                String path = MediaSaver.save(getActivity(), out, 1);

                ui.post(() -> {
                    setBusy(false, null);
                    hideProgress();
                    showStatusOk("音频已提取");
                    toast("音频已保存到 " + path);
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> {
                    setBusy(false, null);
                    hideProgress();
                    showStatusErr("提取失败：" + msg);
                });
            } finally {
                if (videoTmp != null && videoTmp.exists()) videoTmp.delete();
            }
        });
    }

    // ---------------- 下载封面 ----------------
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
                String path = MediaSaver.save(getActivity(), f, 2);

                ui.post(() -> {
                    setBusy(false, null);
                    showStatusOk("封面已保存");
                    toast("封面已保存到 " + path);
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> {
                    setBusy(false, null);
                    showStatusErr("封面下载失败：" + msg);
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
        if (!it.author.isEmpty()) meta.append("作者 ").append(it.author);
        if (it.width > 0 && it.height > 0) {
            if (meta.length() > 0) meta.append("    ");
            meta.append(it.width).append('×').append(it.height);
        }
        if (it.sizeBytes > 0) {
            if (meta.length() > 0) meta.append("\n");
            meta.append("大小 ").append(fmtSize(it.sizeBytes));
        }
        if (it.durationSec > 0) {
            if (meta.length() > 0) meta.append("    ");
            meta.append("时长 ").append(fmtDuration(it.durationSec));
        }
        tvMeta.setText(meta.toString());

        tvBadge.setText(it.watermark ? "含水印" : "无水印");

        playMask.setVisibility(View.VISIBLE);
        ivCover.setVisibility(View.VISIBLE);
        videoView.setVisibility(View.GONE);
        btnClosePreview.setVisibility(View.GONE);

        cardResult.setVisibility(View.VISIBLE);
        cardResult.setAlpha(0f);
        cardResult.animate().alpha(1f).setDuration(200).start();
    }

    private void loadCover(String url) {
        if (TextUtils.isEmpty(url) || ivCover == null) return;
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
                    if (bmp != null) ui.post(() -> {
                        if (ivCover != null) ivCover.setImageBitmap(bmp);
                    });
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
        tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.sub));
        tvStatus.setText(msg);
    }

    private void showStatusOk(String msg) {
        boxStatus.setVisibility(View.VISIBLE);
        progressSmall.setVisibility(View.GONE);
        tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ok));
        tvStatus.setText(msg);
    }

    private void showStatusErr(String msg) {
        boxStatus.setVisibility(View.VISIBLE);
        progressSmall.setVisibility(View.GONE);
        tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.err));
        tvStatus.setText(msg);
    }

    private void showProgress(int pct, String label, long done, long total) {
        progressBar.setVisibility(View.VISIBLE);
        if (pct < 0) {
            progressBar.setIndeterminate(true);
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.sub));
            tvStatus.setText(label);
        } else {
            progressBar.setIndeterminate(false);
            progressBar.setProgress(pct);
            String txt = label + pct + "%  " + fmtSize(done);
            if (total > 0) txt += " / " + fmtSize(total);
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.sub));
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
    static String fmtSize(long b) {
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
            Activity a = getActivity();
            if (a == null) return;
            InputMethodManager imm =
                    (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(etInput.getWindowToken(), 0);
        } catch (Exception ignored) {
        }
    }

    private void toast(String s) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPreview();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pool.shutdownNow();
    }
}