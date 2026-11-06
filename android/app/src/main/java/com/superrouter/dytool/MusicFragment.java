package com.superrouter.dytool;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicFragment extends Fragment {

    private EditText etSearch;
    private MaterialButton btnSearch, btnPaste;
    private LinearLayout boxStatus, boxEmpty;
    private ProgressBar pbMusic;
    private TextView tvStatus, tvEmpty;
    private RecyclerView rvMusic;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private final List<MusicItem> data = new ArrayList<>();
    private MusicAdapter adapter;

    private MediaPlayer player;
    private int playingPos = -1;
    private volatile boolean busy = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        etSearch = v.findViewById(R.id.etSearch);
        btnSearch = v.findViewById(R.id.btnSearch);
        btnPaste = v.findViewById(R.id.btnMusicPaste);
        boxStatus = v.findViewById(R.id.boxMusicStatus);
        boxEmpty = v.findViewById(R.id.boxEmpty);
        pbMusic = v.findViewById(R.id.pbMusic);
        tvStatus = v.findViewById(R.id.tvMusicStatus);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        rvMusic = v.findViewById(R.id.rvMusic);

        adapter = new MusicAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        rvMusic.setLayoutManager(lm);
        rvMusic.setItemAnimator(new DefaultItemAnimator());
        rvMusic.setAdapter(adapter);
        rvMusic.setHasFixedSize(false);
        rvMusic.setItemViewCacheSize(12);

        btnSearch.setOnClickListener(x -> doSearch());
        if (btnPaste != null) btnPaste.setOnClickListener(x -> doPaste());
        etSearch.setOnEditorActionListener((tv, actionId, ev) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch();
                return true;
            }
            return false;
        });
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
            etSearch.setText(cs.toString());
            etSearch.setSelection(etSearch.getText().length());
            toast("已粘贴，点搜索识曲");
        } catch (Exception e) {
            toast("读取剪贴板失败");
        }
    }

    private void doSearch() {
        if (busy) {
            toast("正在搜索中…");
            return;
        }
        String raw = etSearch.getText().toString().trim();
        if (raw.isEmpty()) {
            toast("请输入歌曲、歌手，或粘贴抖音链接");
            return;
        }

        // 抖音链接识曲
        String link = DyParser.extractUrl(raw);
        if (link != null && DyParser.isDouyin(link)) {
            doRecognize(link);
            return;
        }

        hideKeyboard();
        stopPlay();
        busy = true;
        btnSearch.setEnabled(false);
        showStatus("搜索中…");
        boxEmpty.setVisibility(View.GONE);
        rvMusic.setVisibility(View.GONE);

        final String kw = raw;
        pool.execute(() -> {
            try {
                List<MusicItem> list = MusicApi.search(kw, 30);
                ui.post(() -> {
                    busy = false;
                    btnSearch.setEnabled(true);
                    if (list.isEmpty()) {
                        boxStatus.setVisibility(View.GONE);
                        showEmpty(getString(R.string.music_no_result));
                        return;
                    }
                    data.clear();
                    data.addAll(list);
                    adapter.notifyDataSetChanged();
                    boxStatus.setVisibility(View.GONE);
                    rvMusic.setVisibility(View.VISIBLE);
                    rvMusic.scrollToPosition(0);
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> {
                    busy = false;
                    btnSearch.setEnabled(true);
                    showStatusErr("搜索失败：" + msg);
                    showEmpty("搜索失败，请重试");
                });
            }
        });
    }

    /** 抖音链接识曲：解析视频，取其标题/音乐名去搜索 */
    private void doRecognize(String link) {
        hideKeyboard();
        stopPlay();
        busy = true;
        btnSearch.setEnabled(false);
        showStatus("正在识别视频音乐…");
        boxEmpty.setVisibility(View.GONE);
        rvMusic.setVisibility(View.GONE);

        pool.execute(() -> {
            try {
                DyItem item = new DyParser().parse(link);
                // 从标题提取音乐名：去掉 #话题 @提及
                String title = item.title == null ? "" : item.title;
                title = title.replaceAll("#[^\s#@]+", " ")
                        .replaceAll("@[^\s#@]+", " ")
                        .replaceAll("[《》\[\]【】]", " ")
                        .replaceAll("\s+", " ")
                        .trim();
                if (title.isEmpty()) throw new Exception("无法识别视频音乐");

                ui.post(() -> showStatus("识别到：" + title + "，正在搜索…"));

                List<MusicItem> list = MusicApi.search(title, 20);
                ui.post(() -> {
                    busy = false;
                    btnSearch.setEnabled(true);
                    etSearch.setText(title);
                    if (list.isEmpty()) {
                        boxStatus.setVisibility(View.GONE);
                        showEmpty("未找到匹配音乐");
                        return;
                    }
                    data.clear();
                    data.addAll(list);
                    adapter.notifyDataSetChanged();
                    boxStatus.setVisibility(View.GONE);
                    rvMusic.setVisibility(View.VISIBLE);
                    rvMusic.scrollToPosition(0);
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> {
                    busy = false;
                    btnSearch.setEnabled(true);
                    showStatusErr("识曲失败：" + msg);
                    showEmpty("识曲失败，请重试");
                });
            }
        });
    }

    // ---------------- 试听 ----------------
    private void togglePlay(int pos) {
        if (pos < 0 || pos >= data.size()) return;

        if (playingPos == pos && player != null) {
            if (player.isPlaying()) {
                player.pause();
                adapter.notifyItemChanged(pos);
                return;
            } else {
                player.start();
                adapter.notifyItemChanged(pos);
                return;
            }
        }

        stopPlay();
        final MusicItem m = data.get(pos);
        showStatus("正在获取播放地址…");

        pool.execute(() -> {
            try {
                List<String> urls = MusicApi.resolveAudioUrls(m.id);
                if (urls.isEmpty()) throw new Exception("该歌曲暂无免费音源");

                // 并发探测选最快可用的
                String best = null;
                Exception lastE = null;
                for (String u : urls) {
                    try {
                        String probe = probePlayable(u);
                        if (probe != null) { best = probe; break; }
                    } catch (Exception e) { lastE = e; }
                }
                if (best == null) throw new Exception("该歌曲暂无法播放");

                final String playUrl = best;
                ui.post(() -> {
                    try {
                        player = new MediaPlayer();
                        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        player.setDataSource(playUrl);
                        player.setOnPreparedListener(mp -> {
                            mp.start();
                            playingPos = pos;
                            boxStatus.setVisibility(View.GONE);
                            adapter.notifyDataSetChanged();
                        });
                        player.setOnCompletionListener(mp -> stopPlay());
                        player.setOnErrorListener((mp, w, e) -> {
                            toast("播放失败");
                            stopPlay();
                            return true;
                        });
                        player.prepareAsync();
                    } catch (Exception e) {
                        toast("播放失败");
                        stopPlay();
                    }
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> {
                    showStatusErr(msg);
                    toast(msg);
                });
            }
        });
    }

    /** 探测链接是否真能播放（拉取头部字节） */
    private String probePlayable(String url) {
        try {
            okhttp3.Request req = new okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", App.UA)
                    .header("Referer", "https://music.163.com/")
                    .header("Range", "bytes=0-1023")
                    .build();
            try (okhttp3.Response r = App.http().newCall(req).execute()) {
                if (!r.isSuccessful() && r.code() != 206) return null;
                String ct = r.header("Content-Type", "");
                if (ct != null && (ct.contains("text/") || ct.contains("json"))) return null;
                okhttp3.ResponseBody b = r.body();
                if (b == null) return null;
                byte[] head = b.bytes();
                if (head == null || head.length < 100) return null;
                return url;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void stopPlay() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
            }
        } catch (Exception ignored) {
        }
        player = null;
        playingPos = -1;
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    // ---------------- 下载 ----------------
    private void download(int pos) {
        if (pos < 0 || pos >= data.size()) return;
        if (busy) {
            toast("正在下载中，请稍候");
            return;
        }
        final MusicItem m = data.get(pos);
        busy = true;
        showStatus("正在获取音源…");

        pool.execute(() -> {
            try {
                List<String> urls = MusicApi.resolveAudioUrls(m.id);
                if (urls.isEmpty()) throw new Exception("该歌曲暂无免费音源");

                ui.post(() -> showStatus("正在下载 " + m.name));

                File dest = new File(App.outDir(), m.safeName() + ".mp3");
                File f = Downloader.download(urls, dest, (pct, done, total) -> {
                    String txt = "正在下载 " + m.name;
                    if (pct >= 0) txt += "  " + pct + "%";
                    final String t = txt;
                    ui.post(() -> showStatus(t));
                }, false);

                ui.post(() -> showStatus("正在保存到音乐库…"));
                String path = MediaSaver.save(getActivity(), f, 1);

                ui.post(() -> {
                    busy = false;
                    showStatusOk("已保存：" + path);
                    toast("音乐已保存到 " + path);
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> {
                    busy = false;
                    showStatusErr("下载失败：" + msg);
                });
            }
        });
    }

    // ---------------- 状态 ----------------
    private void showStatus(String msg) {
        boxStatus.setVisibility(View.VISIBLE);
        pbMusic.setVisibility(View.VISIBLE);
        tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.sub));
        tvStatus.setText(msg);
    }

    private void showStatusOk(String msg) {
        boxStatus.setVisibility(View.VISIBLE);
        pbMusic.setVisibility(View.GONE);
        tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ok));
        tvStatus.setText(msg);
    }

    private void showStatusErr(String msg) {
        boxStatus.setVisibility(View.VISIBLE);
        pbMusic.setVisibility(View.GONE);
        tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.err));
        tvStatus.setText(msg);
    }

    private void showEmpty(String msg) {
        boxEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(msg);
    }

    private void hideKeyboard() {
        try {
            Activity a = getActivity();
            if (a == null) return;
            InputMethodManager imm =
                    (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
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
        stopPlay();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPlay();
        pool.shutdownNow();
    }

    // ---------------- 适配器 ----------------
    private class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_music, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            MusicItem m = data.get(position);
            h.tvName.setText(m.name);
            h.tvArtist.setText(m.artist);
            h.tvAlbum.setText(m.album);

            if (!TextUtils.isEmpty(m.cover)) {
                Glide.with(MusicFragment.this)
                        .load(m.cover)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.bg_cover_round)
                                .error(R.drawable.bg_cover_round)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .centerCrop()
                                .timeout(8000))
                        .into(h.ivCover);
            } else {
                h.ivCover.setImageResource(R.drawable.bg_cover_round);
            }

            boolean playing = (position == playingPos && player != null && player.isPlaying());
            h.btnAction.setText(playing ? "暂停" : "下载");

            h.itemView.setOnClickListener(v -> togglePlay(h.getAdapterPosition()));
            h.btnAction.setOnClickListener(v -> download(h.getAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivCover;
            TextView tvName, tvArtist, tvAlbum;
            MaterialButton btnAction;

            VH(@NonNull View v) {
                super(v);
                ivCover = v.findViewById(R.id.ivCover);
                tvName = v.findViewById(R.id.tvName);
                tvArtist = v.findViewById(R.id.tvArtist);
                tvAlbum = v.findViewById(R.id.tvAlbum);
                btnAction = v.findViewById(R.id.btnAction);
            }
        }
    }
}