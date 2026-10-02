package com.xuanyin.app;
import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.View;
import android.view.animation.*;
import android.widget.*;
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
    private final List<Song> localSongs = new ArrayList<>();
    private final List<Song> onlineSongs = new ArrayList<>();
    private SongAdapter localAdapter, onlineAdapter;
    private View pageLocal, pageOnline;
    private View tabLocal, tabOnline;
    private View indicator;
    private int currentTab = 0;
    private boolean animating = false;
    private ImageView bg;

    @Override protected void onCreate(Bundle b) {
        try { super.onCreate(b); } catch (Throwable t) { finish(); return; }
        try {
            setContentView(R.layout.activity_main);
        } catch (Throwable t) { finish(); return; }

        if (!Prefs.getBoolean("announced_v20", false)) {
            Prefs.put("announced_v20", true);
            try { startActivity(new Intent(this, AnnouncementActivity.class)); } catch (Throwable ignored) {}
        }

        bg = (ImageView) findViewById(R.id.iv_bg);
        setText(R.id.tv_greet, WarmGreeting.greeting());
        setText(R.id.tv_sub_greet, WarmGreeting.subGreeting());
        setText(R.id.tv_quote, WarmGreeting.quote());
        try { WallpaperHelper.loadBackground(this, bg); } catch (Throwable ignored) {}

        pageLocal = findViewById(R.id.page_local);
        pageOnline = findViewById(R.id.page_online);
        tabLocal = findViewById(R.id.tab_local);
        tabOnline = findViewById(R.id.tab_online);
        indicator = findViewById(R.id.tab_indicator);

        try { setupRecycler((RecyclerView) findViewById(R.id.rv_local), true); } catch (Throwable ignored) {}
        try { setupRecycler((RecyclerView) findViewById(R.id.rv_online), false); } catch (Throwable ignored) {}

        if (tabLocal != null) tabLocal.setOnClickListener(v -> { Haptic.tap(v); switchTab(0); });
        if (tabOnline != null) tabOnline.setOnClickListener(v -> { Haptic.tap(v); switchTab(1); });

        Anim.enter(findViewById(R.id.card_greet), 0);
        Anim.enter(findViewById(R.id.card_actions), 70);
        Anim.enter(findViewById(R.id.card_search), 140);
        Anim.enter(findViewById(R.id.tab_bar), 200);

        bind(R.id.btn_local, () -> switchTab(0));
        bind(R.id.btn_island, this::ensureOverlay);
        bind(R.id.btn_settings, () -> {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
        bind(R.id.card_search, this::searchDialog);

        if (tabLocal != null) tabLocal.post(() -> moveIndicator(0, false));

        try { reqAndScan(); } catch (Throwable ignored) {}
    }
    private void setText(int id, String s) {
        try {
            TextView t = findViewById(id);
            if (t != null) t.setText(s);
        } catch (Throwable ignored) {}
    }
    private void bind(int id, Runnable r) {
        try {
            View v = findViewById(id);
            if (v == null) return;
            v.setOnClickListener(x -> { Haptic.tap(v); Anim.press(v); r.run(); });
        } catch (Throwable ignored) {}
    }
    private void setupRecycler(RecyclerView rv, boolean isLocal) {
        if (rv == null) return;
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setInitialPrefetchItemCount(10);
        rv.setLayoutManager(lm);
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(28);
        rv.setDrawingCacheEnabled(true);
        rv.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        SongAdapter ad;
        if (isLocal) { ad = new SongAdapter(localSongs, (s, p) -> playLocal(p)); localAdapter = ad; }
        else { ad = new SongAdapter(onlineSongs, (s, p) -> playOnline(p)); onlineAdapter = ad; }
        rv.setAdapter(ad);
    }
    private void playLocal(int pos) {
        try {
            MusicService.playList(this, localSongs, pos);
            startActivity(new Intent(this, PlayerActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        } catch (Throwable ignored) {}
    }
    private void playOnline(int pos) {
        try {
            MusicService.playList(this, onlineSongs, pos);
            startActivity(new Intent(this, PlayerActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        } catch (Throwable ignored) {}
    }

    private void switchTab(int tab) {
        if (animating || tab == currentTab) return;
        if (pageLocal == null || pageOnline == null) return;
        animating = true;
        Haptic.swipe(this);
        View in = tab == 0 ? pageLocal : pageOnline;
        View out = tab == 0 ? pageOnline : pageLocal;
        boolean toRight = tab > currentTab;
        moveIndicator(tab, true);

        out.setVisibility(View.VISIBLE);
        out.setAlpha(1f);

        ObjectAnimator outA = ObjectAnimator.ofFloat(out, "alpha", 1f, 0f);
        outA.setDuration(380);
        outA.setInterpolator(new AccelerateInterpolator(1.4f));

        float dir = toRight ? -1f : 1f;
        in.setVisibility(View.VISIBLE);
        in.setAlpha(0f);
        in.setTranslationX(dir * (in.getWidth() > 0 ? in.getWidth() * 0.22f : 200f));
        in.setScaleX(0.93f); in.setScaleY(0.93f);

        ObjectAnimator inA = ObjectAnimator.ofFloat(in, "alpha", 0f, 1f);
        inA.setDuration(520);
        inA.setStartDelay(70);
        inA.setInterpolator(new DecelerateInterpolator(1.7f));

        in.animate().translationX(0).scaleX(1f).scaleY(1f)
            .setDuration(520).setStartDelay(70)
            .setInterpolator(new DecelerateInterpolator(1.7f)).start();

        ValueAnimatorProxy.run(out, in, () -> {
            out.setVisibility(View.GONE);
            animating = false;
            currentTab = tab;
        });
        outA.start(); inA.start();
    }
    private void moveIndicator(int tab, boolean animate) {
        if (indicator == null || tabLocal == null) return;
        int w = tabLocal.getWidth();
        if (w == 0) { tabLocal.post(() -> moveIndicator(tab, animate)); return; }
        float target = tab * w;
        if (animate) {
            indicator.animate().translationX(target)
                .setDuration(340).setInterpolator(new OvershootInterpolator(1.1f)).start();
        } else indicator.setTranslationX(target);
        if (tabLocal != null) tabLocal.animate().alpha(tab == 0 ? 1f : 0.5f).setDuration(260).start();
        if (tabOnline != null) tabOnline.animate().alpha(tab == 1 ? 1f : 0.5f).setDuration(260).start();
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
            try {
                List<Song> l = MusicScanner.scan(this);
                new Handler(Looper.getMainLooper()).post(() -> {
                    localSongs.clear(); localSongs.addAll(l);
                    if (localAdapter != null) localAdapter.notifyDataSetChanged();
                });
            } catch (Throwable ignored) {}
        }).start();
    }
    private void ensureOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            Toast.makeText(this, "请授予悬浮窗权限", Toast.LENGTH_LONG).show();
            return;
        }
        try { startService(new Intent(this, IslandService.class)); } catch (Throwable ignored) {}
        Haptic.done(this);
        Toast.makeText(this, "玄音·灵动岛已启动", Toast.LENGTH_SHORT).show();
    }
    private void searchDialog() {
        SearchDialog d = new SearchDialog(this, k -> {
            NeteaseApi.search(k, songs -> runOnUiThread(() -> {
                onlineSongs.clear(); onlineSongs.addAll(songs);
                if (onlineAdapter != null) onlineAdapter.notifyDataSetChanged();
                switchTab(1);
            }));
        });
        d.show();
        if (d.getWindow() != null) {
            d.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.88f),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            d.getWindow().setDimAmount(0.6f);
        }
    }
    @Override protected void onResume() {
        super.onResume();
        try { WallpaperHelper.loadBackground(this, bg); } catch (Throwable ignored) {}
    }
}
