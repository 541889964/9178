package com.xuanyin.app;
import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
    private TextView tabLocal, tabOnline;
    private View indicator;
    private int currentTab = 0;
    private boolean animating = false;
    private ImageView bg;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        if (!Prefs.getBoolean("announced_v19", false)) {
            startActivity(new Intent(this, AnnouncementActivity.class));
            Prefs.put("announced_v19", true);
        }
        bg = findViewById(R.id.iv_bg);
        ((TextView) findViewById(R.id.tv_greet)).setText(WarmGreeting.greeting());
        ((TextView) findViewById(R.id.tv_sub_greet)).setText(WarmGreeting.subGreeting());
        ((TextView) findViewById(R.id.tv_quote)).setText(WarmGreeting.quote());
        WallpaperHelper.loadBackground(this, bg);

        pageLocal = findViewById(R.id.page_local);
        pageOnline = findViewById(R.id.page_online);
        tabLocal = findViewById(R.id.tab_local);
        tabOnline = findViewById(R.id.tab_online);
        indicator = findViewById(R.id.tab_indicator);

        setupRecycler((RecyclerView) findViewById(R.id.rv_local), true);
        setupRecycler((RecyclerView) findViewById(R.id.rv_online), false);

        tabLocal.setOnClickListener(v -> switchTab(0));
        tabOnline.setOnClickListener(v -> switchTab(1));

        Anim.enter(findViewById(R.id.card_greet), 0);
        Anim.enter(findViewById(R.id.card_actions), 70);
        Anim.enter(findViewById(R.id.card_search), 140);
        Anim.enter(findViewById(R.id.tab_bar), 200);

        findViewById(R.id.btn_local).setOnClickListener(v -> {
            Haptic.tap(v); Anim.press(v); switchTab(0);
        });
        findViewById(R.id.btn_island).setOnClickListener(v -> {
            Haptic.tap(v); Anim.press(v); ensureOverlay();
        });
        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            Haptic.tap(v); Anim.press(v);
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
        findViewById(R.id.card_search).setOnClickListener(v -> {
            Haptic.tap(v); Anim.pressLight(v); searchDialog();
        });

        tabLocal.post(() -> moveIndicator(0, false));
        reqAndScan();
    }
    private void setupRecycler(RecyclerView rv, boolean isLocal) {
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
        MusicService.playList(this, localSongs, pos);
        startActivity(new Intent(this, PlayerActivity.class));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
    private void playOnline(int pos) {
        MusicService.playList(this, onlineSongs, pos);
        startActivity(new Intent(this, PlayerActivity.class));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void switchTab(int tab) {
        if (animating || tab == currentTab) return;
        animating = true;
        Haptic.swipe(this);
        View in = tab == 0 ? pageLocal : pageOnline;
        View out = tab == 0 ? pageOnline : pageLocal;
        boolean toRight = tab > currentTab;
        moveIndicator(tab, true);

        out.setVisibility(View.VISIBLE);
        out.setAlpha(1f);
        float dir = toRight ? -1f : 1f;
        out.setTranslationX(0);
        out.setScaleX(1f); out.setScaleY(1f);

        ObjectAnimator outA = ObjectAnimator.ofFloat(out, "alpha", 1f, 0f);
        outA.setDuration(420);
        outA.setInterpolator(new AccelerateInterpolator(1.4f));

        in.setVisibility(View.VISIBLE);
        in.setAlpha(0f);
        in.setTranslationX(dir * in.getWidth() * 0.25f);
        in.setScaleX(0.92f); in.setScaleY(0.92f);

        ObjectAnimator inA = ObjectAnimator.ofFloat(in, "alpha", 0f, 1f);
        inA.setDuration(560);
        inA.setStartDelay(80);
        inA.setInterpolator(new DecelerateInterpolator(1.6f));
        in.animate().translationX(0).scaleX(1f).scaleY(1f)
            .setDuration(560).setStartDelay(80)
            .setInterpolator(new DecelerateInterpolator(1.6f)).start();

        ValueAnimatorProxy.run(out, in, () -> {
            out.setVisibility(View.GONE);
            animating = false;
            currentTab = tab;
        });
        outA.start(); inA.start();
    }
    private void moveIndicator(int tab, boolean animate) {
        int w = tabLocal.getWidth();
        if (w == 0) { tabLocal.post(() -> moveIndicator(tab, animate)); return; }
        float target = tab * w;
        if (animate) {
            indicator.animate().translationX(target)
                .setDuration(360).setInterpolator(new OvershootInterpolator(1.1f)).start();
        } else indicator.setTranslationX(target);
        tabLocal.animate().alpha(tab == 0 ? 1f : 0.5f).setDuration(280).start();
        tabOnline.animate().alpha(tab == 1 ? 1f : 0.5f).setDuration(280).start();
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
                localSongs.clear(); localSongs.addAll(l);
                if (localAdapter != null) localAdapter.notifyDataSetChanged();
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
    @Override protected void onResume() { super.onResume(); WallpaperHelper.loadBackground(this, bg); }
}
