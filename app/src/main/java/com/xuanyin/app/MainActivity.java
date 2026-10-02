package com.xuanyin.app;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.xuanyin.app.adapter.SongAdapter;
import com.xuanyin.app.model.Song;
import com.xuanyin.app.service.IslandService;
import com.xuanyin.app.service.MusicService;
import com.xuanyin.app.util.*;
import java.util.ArrayList;
import java.util.List;
public class MainActivity extends AppCompatActivity {
    private final List<Song> songs = new ArrayList<>();
    private SongAdapter adapter;
    private ImageView bg;
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        if (!Prefs.getBoolean("announced_v12", false)) {
            startActivity(new Intent(this, AnnouncementActivity.class));
            Prefs.put("announced_v12", true);
        }
        bg = findViewById(R.id.iv_bg);
        ((TextView) findViewById(R.id.tv_greet)).setText(WarmGreeting.greeting());
        ((TextView) findViewById(R.id.tv_sub_greet)).setText(WarmGreeting.subGreeting());
        ((TextView) findViewById(R.id.tv_quote)).setText(WarmGreeting.quote());
        WallpaperHelper.loadBackground(this, bg);
        RecyclerView rv = findViewById(R.id.rv_songs);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(20);
        adapter = new SongAdapter(songs, this::onSongClick);
        rv.setAdapter(adapter);
        animate(findViewById(R.id.card_greet), 0);
        animate(findViewById(R.id.card_actions), 80);
        animate(findViewById(R.id.card_search), 160);
        animate(findViewById(R.id.tv_section), 220);
        findViewById(R.id.btn_local).setOnClickListener(v -> { press(v); reqAndScan(); });
        findViewById(R.id.btn_island).setOnClickListener(v -> { press(v); ensureOverlay(); });
        findViewById(R.id.btn_settings).setOnClickListener(v -> { press(v);
            startActivity(new Intent(this, SettingsActivity.class)); });
        findViewById(R.id.card_search).setOnClickListener(v -> { press(v); searchDialog(); });
        reqAndScan();
    }
    private void animate(View v, long d) {
        if (v == null) return;
        v.setAlpha(0f); v.setTranslationY(50f); v.setScaleX(0.96f); v.setScaleY(0.96f);
        v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(480).setStartDelay(d)
            .setInterpolator(new DecelerateInterpolator(1.4f)).start();
    }
    private void press(View v) {
        v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70)
            .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(180)
                .setInterpolator(new OvershootInterpolator(2f)).start()).start();
    }
    private void onSongClick(Song s, int pos) {
        Intent i = new Intent(this, MusicService.class);
        i.setAction(MusicService.ACTION_PLAY);
        i.putExtra("song", s);
        startService(i);
        startActivity(new Intent(this, PlayerActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
    private void reqAndScan() {
        String perm = Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{perm}, 100);
        else scan();
    }
    @Override public void onRequestPermissionsResult(int c, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(c, p, r);
        if (c == 100 && r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) scan();
    }
    private void scan() {
        new Thread(() -> {
            List<Song> l = MusicScanner.scan(this);
            new Handler(Looper.getMainLooper()).post(() -> {
                songs.clear(); songs.addAll(l); adapter.notifyDataSetChanged();
            });
        }).start();
    }
    private void ensureOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            Toast.makeText(this, "请授予悬浮窗权限", Toast.LENGTH_LONG).show();
            return;
        }
        startService(new Intent(this, IslandService.class));
        Toast.makeText(this, "玄音·灵动岛已启动", Toast.LENGTH_SHORT).show();
    }
    private void searchDialog() {
        EditText et = new EditText(this);
        et.setHint("输入歌曲 / 歌手");
        new AlertDialog.Builder(this).setTitle("在线搜索").setView(et)
            .setPositiveButton("搜索", (d, w) -> {
                String k = et.getText().toString().trim();
                if (!k.isEmpty()) {
                    Intent i = new Intent(this, PlayerActivity.class);
                    i.setAction("SEARCH"); i.putExtra("keyword", k); startActivity(i);
                }
            }).setNegativeButton("取消", null).show();
    }
    @Override protected void onResume() { super.onResume(); WallpaperHelper.loadBackground(this, bg); }
}
