package com.xuanyin.app;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.xuanyin.app.adapter.SongAdapter;
import com.xuanyin.app.dialog.SearchDialog;
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
        if (!Prefs.getBoolean("announced_v17", false)) {
            startActivity(new Intent(this, AnnouncementActivity.class));
            Prefs.put("announced_v17", true);
        }
        bg = findViewById(R.id.iv_bg);
        ((TextView) findViewById(R.id.tv_greet)).setText(WarmGreeting.greeting());
        ((TextView) findViewById(R.id.tv_sub_greet)).setText(WarmGreeting.subGreeting());
        ((TextView) findViewById(R.id.tv_quote)).setText(WarmGreeting.quote());
        WallpaperHelper.loadBackground(this, bg);

        RecyclerView rv = findViewById(R.id.rv_songs);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setInitialPrefetchItemCount(10);
        rv.setLayoutManager(lm);
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(28);
        rv.setDrawingCacheEnabled(true);
        rv.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        adapter = new SongAdapter(songs, this::onSongClick);
        rv.setAdapter(adapter);

        Anim.enter(findViewById(R.id.card_greet), 0);
        Anim.enter(findViewById(R.id.card_actions), 70);
        Anim.enter(findViewById(R.id.card_search), 140);
        Anim.enter(findViewById(R.id.tv_section), 200);

        bind(R.id.btn_local, this::reqAndScan);
        bind(R.id.btn_island, this::ensureOverlay);
        bind(R.id.btn_settings, () -> startActivity(new Intent(this, SettingsActivity.class)));
        bind(R.id.card_search, this::searchDialog);
        reqAndScan();
    }
    private void bind(int id, Runnable r) {
        View v = findViewById(id);
        if (v == null) return;
        v.setOnClickListener(x -> { Haptic.tap(v); Anim.press(v); r.run(); });
    }
    private void onSongClick(Song s, int pos) {
        Intent i = new Intent(this, MusicService.class);
        i.setAction(MusicService.ACTION_PLAY);
        i.putExtra("song", s);
        startService(i);
        startActivity(new Intent(this, PlayerActivity.class));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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
        Haptic.done(this);
        Toast.makeText(this, "玄音·灵动岛已启动", Toast.LENGTH_SHORT).show();
    }
    private void searchDialog() {
        SearchDialog d = new SearchDialog(this, k -> {
            Intent i = new Intent(this, PlayerActivity.class);
            i.setAction("SEARCH"); i.putExtra("keyword", k); startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
        d.show();
        if (d.getWindow() != null) {
            d.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.88f),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            d.getWindow().setDimAmount(0.6f);
        }
    }
    @Override protected void onResume() { super.onResume(); WallpaperHelper.loadBackground(this, bg); }
}
