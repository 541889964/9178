package com.xuanyin.app;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.xuanyin.app.adapter.SongAdapter;
import com.xuanyin.app.model.Song;
import com.xuanyin.app.service.MusicService;
import com.xuanyin.app.util.NeteaseApi;
import com.xuanyin.app.util.WallpaperHelper;
import java.util.ArrayList;
import java.util.List;
public class PlayerActivity extends AppCompatActivity {
    private ImageView cover;
    private View pulse;
    private ObjectAnimator rotation, pulseAnim;
    private final List<Song> list = new ArrayList<>();
    private SongAdapter adapter;
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
            MusicService.playOnline(this, s);
            ((TextView) findViewById(R.id.tv_title)).setText(s.title);
            ((TextView) findViewById(R.id.tv_artist)).setText(s.displayArtist());
            WallpaperHelper.loadCover(this, cover, s.id);
        });
        rv.setAdapter(adapter);
        if ("SEARCH".equals(getIntent().getAction())) {
            String k = getIntent().getStringExtra("keyword");
            NeteaseApi.search(k, songs -> runOnUiThread(() -> {
                list.clear(); list.addAll(songs); adapter.notifyDataSetChanged();
            }));
        }
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(70)
                .withEndAction(this::finish).start();
        });
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (rotation != null) rotation.cancel();
        if (pulseAnim != null) pulseAnim.cancel();
    }
}
