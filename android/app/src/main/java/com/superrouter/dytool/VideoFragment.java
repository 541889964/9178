package com.superrouter.dytool;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoFragment extends Fragment {

    private EditText etInput;
    private MaterialButton btnPaste, btnParse, btnVideo, btnAudio, btnCover, btnOriginAudio;
    private LinearLayout boxStatus;
    private MaterialCardView cardResult;
    private ProgressBar progressSmall, progressBar;
    private TextView tvStatus, tvTitle, tvTags, tvMeta, tvBadge;
    private ImageView ivCover;
    private VideoView videoView;
    private LinearLayout playMask;
    private ImageButton btnClosePreview, btnFullscreen;
    private RecyclerView rvPics;
    private View previewBox;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private final DyParser parser = new DyParser();

    private DyItem current;
    private volatile boolean busy = false;

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
        btnOriginAudio = v.findViewById(R.id.btnOriginAudio);
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
        btnFullscreen = v.findViewById(R.id.btnFullscreen);
        rvPics = v.findViewById(R.id.rvPics);
        previewBox = v.findViewById(R.id.previewBox);

        rvPics.setLayoutManager(new GridLayoutManager(getContext(), 3));
        rvPics.setNestedScrollingEnabled(false);

        btnPaste.setOnClickListener(x -> doPaste());
        btnParse.setOnClickListener(x -> doParse());
        btnVideo.setOnClickListener(x -> doDownloadVideo());
        btnAudio.setOnClickListener(x -> doExtractAudio());
        btnCover.setOnClickListener(x -> doDownloadCover());
        btnOriginAudio.setOnClickListener(x -> doDownloadOriginAudio());
        playMask.setOnClickListener(x -> startPreview());
        btnClosePreview.setOnClickListener(x -> stopPreview());
        btnFullscreen.setOnClickListener(x -> openFullscreen());
    }

    public void parseFromText(String text) {
        if (etInput == null) return;
        etInput.setText(text);
        doParse();
    }

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
                    showStatusOk(item.isImagePost() ? "解析成功，共 " + item.pics.size() + " 张图" : "解析成功");
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

    // ---------------- 全屏 ----------------
    private void openFullscreen() {
        if (current == null || current.videoUrls.isEmpty()) {
            toast("没有可播放的视频");
            return;
        }
        File local = new File(App.outDir(), current.safeName() + ".mp4");
        String uri;
        if (local.exists() && local.length() > 1024) {
            uri = android.net.Uri.fromFile(local).toString();
        } else {
            uri = current.videoUrls.get(0);
        }
        Intent i = new Intent(getActivity(), FullscreenPlayerActivity.class);
        i.putExtra(FullscreenPlayerActivity.EXTRA_URL, uri);
        i.putExtra(FullscreenPlayerActivity.EXTRA_TITLE, current.title);
        startActivity(i);
    }

    private void startPreview() {
        if (current == null || current.videoUrls.isEmpty()) return;
        playMask.setVisibility(View.GONE);
        ivCover.setVisibility(View.GONE);
        btnClosePreview.setVisibility(View.VISIBLE);
        btnFullscreen.setVisibility(View.VISIBLE);

        File local = new File(App.outDir(), current.safeName() + ".mp4");
        String uri;
        if (local.exists() && local.length() > 1024) {
            uri = android.net.Uri.fromFile(local).toString();
        } else {
            uri = current.videoUrls.get(0);
        }

        videoView.setVisibility(View.VISIBLE);
        videoView.setVideoURI(android.net.Uri.parse(uri));
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
        if (ivCover != null && current != null && !current.isImagePost()) {
            ivCover.setVisibility(View.VISIBLE);
        }
        if (playMask != null && current != null && !current.isImagePost()) {
            playMask.setVisibility(View.VISIBLE);
        }
        if (btnClosePreview != null) btnClosePreview.setVisibility(View.GONE);
        if (btnFullscreen != null) btnFullscreen.setVisibility(View.GONE);
    }

    // ---------------- 下载 ----------------
    private void doDownloadVideo() {
        if (current == null) {
            toast("请先解析");
            return;
        }
        if (busy) return;
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

    private void doDownloadOriginAudio() {
        if (current == null || TextUtils.isEmpty(current.musicUrl)) {
            toast("该作品没有原声");
            return;
        }
        if (busy) return;
        setBusy(true, "正在下载原声…");

        pool.execute(() -> {
            try {
                String name = current.musicTitle == null ? "origin_audio" : current.musicTitle;
                name = DyItem.sanitize(name);
                if (name.length() > 50) name = name.substring(0, 50);
                if (name.isEmpty()) name = "origin_audio_" + current.awemeId;

                File dest = new File(App.outDir(), name + ".mp3");
                List<String> urls = new ArrayList<>();
                urls.add(current.musicUrl);

                File f = Downloader.download(urls, dest, (pct, done, total) ->
                        ui.post(() -> showProgress(pct, "正在下载原声 ", done, total)), false);

                ui.post(() -> showProgress(-2, "正在保存到音乐库…", 0, 0));
                String path = MediaSaver.save(getActivity(), f, 1);

                ui.post(() -> {
                    setBusy(false, null);
                    hideProgress();
                    showStatusOk("原声已保存");
                    toast("原声已保存到 " + path);
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> {
                    setBusy(false, null);
                    hideProgress();
                    showStatusErr("原声下载失败：" + msg);
                });
            }
        });
    }

    private void doExtractAudio() {
        if (current == null) {
            toast("请先解析");
            return;
        }
        if (busy) return;
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

    private void doDownloadCover() {
        if (current == null) {
            toast("请先解析");
            return;
        }
        if (TextUtils.isEmpty(current.cover)) {
            toast("该作品没有封面");
            return;
        }
        if (busy) return;
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
        tvTitle.setText(it.title.isEmpty() ? "抖音作品" : it.title);

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
        if (it.isImagePost()) {
            if (meta.length() > 0) meta.append("    ");
            meta.append(it.pics.size()).append(" 张图片");
        } else {
            if (it.width > 0 && it.height > 0) {
                if (meta.length() > 0) meta.append("    ");
                meta.append(it.width).append('x').append(it.height);
            }
            if (it.sizeBytes > 0) {
                if (meta.length() > 0) meta.append("\n");
                meta.append("大小 ").append(fmtSize(it.sizeBytes));
            }
            if (it.durationSec > 0) {
                if (meta.length() > 0) meta.append("    ");
                meta.append("时长 ").append(fmtDuration(it.durationSec));
            }
        }
        tvMeta.setText(meta.toString());

        if (it.isImagePost()) {
            tvBadge.setText("图集");
            previewBox.setVisibility(View.GONE);
            rvPics.setVisibility(View.VISIBLE);
            btnVideo.setVisibility(View.GONE);
            btnAudio.setVisibility(View.GONE);
            btnCover.setVisibility(View.GONE);
            bindPics(it);
        } else {
            tvBadge.setText(it.watermark ? "含水印" : "无水印");
            previewBox.setVisibility(View.VISIBLE);
            rvPics.setVisibility(View.GONE);
            btnVideo.setVisibility(View.VISIBLE);
            btnAudio.setVisibility(View.VISIBLE);
            btnCover.setVisibility(View.VISIBLE);

            playMask.setVisibility(View.VISIBLE);
            ivCover.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.GONE);
            btnClosePreview.setVisibility(View.GONE);
            btnFullscreen.setVisibility(View.GONE);
            loadCover(it.cover);
        }

        btnOriginAudio.setVisibility(TextUtils.isEmpty(it.musicUrl) ? View.GONE : View.VISIBLE);
        if (!TextUtils.isEmpty(it.musicUrl)) {
            String nm = TextUtils.isEmpty(it.musicTitle) ? "抖音原声" : it.musicTitle;
            btnOriginAudio.setText("下载原声 · " + nm);
        }

        cardResult.setVisibility(View.VISIBLE);
        cardResult.setAlpha(0f);
        cardResult.animate().alpha(1f).setDuration(220).start();
    }

    private void bindPics(DyItem it) {
        rvPics.setAdapter(new PicGridAdapter(it.pics));
    }

    private void loadCover(String url) {
        if (TextUtils.isEmpty(url) || ivCover == null) return;
        Glide.with(VideoFragment.this)
                .load(url)
                .apply(new RequestOptions()
                        .placeholder(R.drawable.bg_cover_round)
                        .error(R.drawable.bg_cover_round)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .timeout(10000))
                .into(ivCover);
    }

    // ---------------- 图集适配器 ----------------
    private class PicGridAdapter extends RecyclerView.Adapter<PicGridAdapter.VH> {
        private final List<DyItem.Pic> list;

        PicGridAdapter(List<DyItem.Pic> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pic_grid, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            DyItem.Pic p = list.get(position);
            Glide.with(VideoFragment.this)
                    .load(p.imageUrl)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.bg_cover_round)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop())
                    .into(h.img);
            h.gif.setVisibility(p.isAnimated() ? View.VISIBLE : View.GONE);
            h.itemView.setOnClickListener(v -> openViewer(position));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView img;
            TextView gif;

            VH(@NonNull View v) {
                super(v);
                img = v.findViewById(R.id.ivGrid);
                gif = v.findViewById(R.id.tvGifTag);
            }
        }
    }

    private void openViewer(int index) {
        if (current == null) return;
        try {
            JSONArray arr = new JSONArray();
            for (DyItem.Pic p : current.pics) {
                JSONObject o = new JSONObject();
                o.put("img", p.imageUrl);
                o.put("video", p.videoUrl == null ? "" : p.videoUrl);
                arr.put(o);
            }
            Intent i = new Intent(getActivity(), ImageViewerActivity.class);
            i.putExtra(ImageViewerActivity.EXTRA_JSON, arr.toString());
            i.putExtra(ImageViewerActivity.EXTRA_TITLE, current.title);
            startActivity(i);
        } catch (Exception e) {
            toast("打开失败");
        }
    }

    // ---------------- 状态 ----------------
    private void setBusy(boolean b, String msg) {
        busy = b;
        btnParse.setEnabled(!b);
        btnVideo.setEnabled(!b);
        btnAudio.setEnabled(!b);
        btnCover.setEnabled(!b);
        btnOriginAudio.setEnabled(!b);
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
