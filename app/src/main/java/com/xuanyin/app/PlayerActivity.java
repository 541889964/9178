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
import com.xuanyin.app.util.DownloadManager;
import com.xuanyin.app.util.NeteaseApi;
import com.xuanyin.app.util.WallpaperHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
public class PlayerActivity extends AppCompatActivity {
    private ImageView cover;
    private View pulse;
    private ObjectAnimator rotation, pulseAnim;
    private final List<Song> list = new ArrayList<>();
    private SongAdapter adapter;
    private boolean playing = false;
    private Song currentSong;
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_player);
        cover = findViewById(R.id.iv_cover);
        pulse = findViewById(R.id.pulse);
        rotation = ObjectAnimator.ofFloat(cover, "rotation", 0f, 360f);
        rotation.setDuration(24000);
        rotation.setRepeatCount(ObjectAnimator.INFINITE);
        rotation.setInterpolator(new LinearInterpolator());
        rotation.start();
        if (pulse != null) {
            pulseAnim = ObjectAnimator.ofFloat(pulse, "alpha", 0.35f, 0.9f, 0.35f);
            pulseAnim.setDuration(2400);
            pulseAnim.setRepeatCount(ObjectAnimator.INFINITE);
            pulseAnim.start();
        }
        RecyclerView rv = findViewById(R.id.rv_result);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setHasFixedSize(true);
        adapter = new SongAdapter(list, (s, p) -> {
            s.isOnline = true;
            currentSong = s;
            MusicService.playOnline(this, s);
            ((TextView) findViewById(R.id.tv_title)).setText(s.title);
            ((TextView) findViewById(R.id.tv_artist)).setText(s.displayArtist());
            WallpaperHelper.loadCover(this, cover, s.id);
            playing = true;
            updatePlayBtn();
        });
        rv.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(70)
                .withEndAction(this::finish).start();
        });
        findViewById(R.id.btn_prev).setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(70)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(140).start()).start();
            startService(new android.content.Intent(this, MusicService.class)
                .setAction(MusicService.ACTION_PREV));
        });
        findViewById(R.id.btn_play).setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(70)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(140).start()).start();
            startService(new android.content.Intent(this, MusicService.class)
                .setAction(MusicService.ACTION_TOGGLE));
            playing = !playing;
            updatePlayBtn();
        });
        findViewById(R.id.btn_next).setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(70)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(140).start()).start();
            startService(new android.content.Intent(this, MusicService.class)
                .setAction(MusicService.ACTION_NEXT));
        });

        // 下载按钮
        View btnDownload = findViewById(R.id.btn_download);
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> {
                v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(70)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(200)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(2f)).start()).start();
                Song s = currentSong;
                if (s == null) {
                    // 尝试用 MusicService 当前歌
                    s = MusicService.getCurrent();
                }
                if (s == null) { Toast.makeText(this, "请先选一首歌", Toast.LENGTH_SHORT).show(); return; }
                if (!s.isOnline) { Toast.makeText(this, "本地歌曲无需下载", Toast.LENGTH_SHORT).show(); return; }
                TextView tvPct = findViewById(R.id.tv_download_pct);
                if (tvPct != null) tvPct.setVisibility(View.VISIBLE);
                final Song fs = s;
                DownloadManager.download(this, fs, new DownloadManager.Listener() {
                    @Override public void onStart() {
                        Toast.makeText(PlayerActivity.this, "开始下载: " + fs.title, Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onProgress(int percent) {
                        if (tvPct != null) tvPct.setText(percent + "%");
                    }
                    @Override public void onDone(File file) {
                        if (tvPct != null) {
                            tvPct.setText("✓ 已保存");
                            tvPct.postDelayed(() -> tvPct.setVisibility(View.GONE), 2500);
                        }
                        Toast.makeText(PlayerActivity.this, "已保存到 " + file.getParent(), Toast.LENGTH_LONG).show();
                    }
                    @Override public void onError(String msg) {
                        if (tvPct != null) tvPct.setVisibility(View.GONE);
                        Toast.makeText(PlayerActivity.this, "下载失败: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
            });
        }

        if ("SEARCH".equals(getIntent().getAction())) {
            String k = getIntent().getStringExtra("keyword");
            NeteaseApi.search(k, songs -> runOnUiThread(() -> {
                list.clear(); list.addAll(songs); adapter.notifyDataSetChanged();
            }));
        }
    }

    private void updatePlayBtn() {
        TextView p = findViewById(R.id.btn_play);
        if (p != null) p.setText(playing ? "⏸" : "▶");
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (rotation != null) rotation.cancel();
        if (pulseAnim != null) pulseAnim.cancel();
    }
}
