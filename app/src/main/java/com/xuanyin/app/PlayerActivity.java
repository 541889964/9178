package com.xuanyin.app;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.xuanyin.app.adapter.SongAdapter;
import com.xuanyin.app.model.Song;
import com.xuanyin.app.service.MusicService;
import com.xuanyin.app.util.Anim;
import com.xuanyin.app.util.DownloadManager;
import com.xuanyin.app.util.Haptic;
import com.xuanyin.app.util.NeteaseApi;
import com.xuanyin.app.util.WallpaperHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {
    private ImageView cover;
    private ObjectAnimator rotation;
    private final List<Song> list = new ArrayList<>();
    private SongAdapter adapter;
    private boolean playing = false;
    private Song currentSong;

    @Override protected void onCreate(Bundle b) {
        try { super.onCreate(b); } catch (Throwable t) { finish(); return; }
        try { setContentView(R.layout.activity_player); }
        catch (Throwable t) { finish(); return; }

        cover = (ImageView) findViewById(R.id.iv_cover);

        try {
            rotation = ObjectAnimator.ofFloat(cover, "rotation", 0f, 360f);
            rotation.setDuration(22000);
            rotation.setRepeatCount(ObjectAnimator.INFINITE);
            rotation.setInterpolator(new LinearInterpolator());
            rotation.start();
        } catch (Throwable ignored) {}

        try {
            RecyclerView rv = (RecyclerView) findViewById(R.id.rv_result);
            LinearLayoutManager lm = new LinearLayoutManager(this);
            lm.setInitialPrefetchItemCount(8);
            rv.setLayoutManager(lm);
            rv.setHasFixedSize(true);
            rv.setItemViewCacheSize(24);
            adapter = new SongAdapter(list, (s, p) -> {
                Haptic.tap(rv);
                s.isOnline = true;
                currentSong = s;
                MusicService.playOnline(this, s);
                try {
                    ((TextView) findViewById(R.id.tv_title)).setText(s.title);
                    ((TextView) findViewById(R.id.tv_artist)).setText(s.displayArtist());
                    WallpaperHelper.loadCover(this, cover, s.id);
                } catch (Throwable ignored) {}
                playing = true;
                updatePlayBtn();
            });
            rv.setAdapter(adapter);
        } catch (Throwable ignored) {}

        bind(R.id.btn_back, () -> {
            finish();
            try { overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); } catch (Throwable ignored) {}
        });
        bind(R.id.btn_prev, () -> {
            try { startService(new android.content.Intent(this, MusicService.class)
                .setAction(MusicService.ACTION_PREV)); } catch (Throwable ignored) {}
        });
        bind(R.id.btn_play, () -> {
            try { startService(new android.content.Intent(this, MusicService.class)
                .setAction(MusicService.ACTION_TOGGLE)); } catch (Throwable ignored) {}
            playing = !playing;
            updatePlayBtn();
        });
        bind(R.id.btn_next, () -> {
            try { startService(new android.content.Intent(this, MusicService.class)
                .setAction(MusicService.ACTION_NEXT)); } catch (Throwable ignored) {}
        });
        bind(R.id.btn_download, this::download);

        if ("SEARCH".equals(getIntent().getAction())) {
            String k = getIntent().getStringExtra("keyword");
            if (k != null) {
                NeteaseApi.search(k, songs -> runOnUiThread(() -> {
                    list.clear();
                    list.addAll(songs);
                    if (adapter != null) adapter.notifyDataSetChanged();
                }));
            }
        }
    }

    private void bind(int id, Runnable r) {
        try {
            View v = findViewById(id);
            if (v == null) return;
            v.setOnClickListener(x -> {
                Haptic.tap(v);
                Anim.press(v);
                r.run();
            });
        } catch (Throwable ignored) {}
    }

    private void download() {
        Song s = currentSong != null ? currentSong : MusicService.getCurrent();
        if (s == null) { Toast.makeText(this, "请先选一首歌", Toast.LENGTH_SHORT).show(); return; }
        if (!s.isOnline) { Toast.makeText(this, "本地歌曲无需下载", Toast.LENGTH_SHORT).show(); return; }
        TextView tvPct = (TextView) findViewById(R.id.tv_download_pct);
        if (tvPct != null) { tvPct.setVisibility(View.VISIBLE); tvPct.setText("0%"); }
        final Song fs = s;
        DownloadManager.download(this, fs, new DownloadManager.Listener() {
            @Override public void onStart() {}
            @Override public void onProgress(int p) { if (tvPct != null) tvPct.setText(p + "%"); }
            @Override public void onDone(File f) {
                Haptic.done(PlayerActivity.this);
                if (tvPct != null) {
                    tvPct.setText("✓ 已保存");
                    tvPct.postDelayed(() -> tvPct.setVisibility(View.GONE), 2500);
                }
                Toast.makeText(PlayerActivity.this, "已保存到 " + f.getParent(), Toast.LENGTH_LONG).show();
            }
            @Override public void onError(String m) {
                Haptic.warn(PlayerActivity.this);
                if (tvPct != null) tvPct.setVisibility(View.GONE);
                Toast.makeText(PlayerActivity.this, "下载失败: " + m, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updatePlayBtn() {
        try {
            TextView p = (TextView) findViewById(R.id.btn_play);
            if (p != null) p.setText(playing ? "⏸" : "▶");
        } catch (Throwable ignored) {}
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (rotation != null) rotation.cancel();
    }
}
