package com.xuanyin.app.service;
import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.os.*;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.xuanyin.app.R;
import com.xuanyin.app.model.Song;
import com.xuanyin.app.util.IslandAdaptiveHelper;
import com.xuanyin.app.util.Prefs;
import com.xuanyin.app.util.WallpaperHelper;
import com.xuanyin.app.widget.IslandBackdropView;
import com.xuanyin.app.widget.IslandPulseView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IslandService extends Service {
    private static final String CH = "island_ch";
    private static final int NID = 1001;

    private WindowManager wm;
    private FrameLayout root;
    private IslandBackdropView backdrop;

    // 折叠态
    private View collapsed;
    private TextView tvTimeMini, tvBatteryMini, tvCollapsedTitle;
    private View collapsedLifeGroup, collapsedMusicGroup;
    private ImageView ivCoverMini;
    private IslandPulseView pulseMini;

    // 展开态
    private View expanded;
    private TextView tvTimeBig, tvDate, tvAlarm, tvBatteryBig;
    private TextView tvSongTitle, tvSongArtist, tvLyric;
    private ImageView ivCoverBig;
    private ProgressBar progress;
    private View lifeGroup, musicGroup;
    private View btnPrev, btnPlay, btnNext;

    private WindowManager.LayoutParams params;
    private boolean expanded = false;
    private boolean musicMode = false;
    private Song currentSong;

    private final Handler ui = new Handler(Looper.getMainLooper());

    private final BroadcastReceiver rx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if (a == null) return;
            switch (a) {
                case "com.xuanyin.app.SONG_CHANGED": {
                    Song s = (Song) i.getSerializableExtra("song");
                    if (s != null) onSong(s);
                    break;
                }
                case "com.xuanyin.app.PLAY_STATE": {
                    boolean playing = i.getBooleanExtra("playing", false);
                    if (pulseMini != null) pulseMini.setPulsing(playing);
                    updatePlayIcon(playing);
                    break;
                }
                case "com.xuanyin.app.NOTIFY":
                    showNotify(i.getStringExtra("title"));
                    break;
                case "com.xuanyin.app.ALARM":
                    showAlarm();
                    break;
                case Intent.ACTION_BATTERY_CHANGED: {
                    int lv = i.getIntExtra("level", 0);
                    int sc = i.getIntExtra("scale", 100);
                    int pct = sc > 0 ? (int)(100f * lv / sc) : lv;
                    String txt = pct + "%";
                    if (tvBatteryMini != null) tvBatteryMini.setText(txt);
                    if (tvBatteryBig != null) tvBatteryBig.setText(txt);
                    break;
                }
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        startForegroundCompat();
        initWindow();
        startClock();
        IntentFilter f = new IntentFilter();
        f.addAction("com.xuanyin.app.SONG_CHANGED");
        f.addAction("com.xuanyin.app.PLAY_STATE");
        f.addAction("com.xuanyin.app.NOTIFY");
        f.addAction("com.xuanyin.app.ALARM");
        f.addAction(Intent.ACTION_BATTERY_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(rx, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(rx, f);
    }

    private void startForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm.getNotificationChannel(CH) == null) {
                NotificationChannel ch = new NotificationChannel(CH, "玄音·灵动岛", NotificationManager.IMPORTANCE_LOW);
                ch.setShowBadge(false);
                nm.createNotificationChannel(ch);
            }
        }
        Notification n = new NotificationCompat.Builder(this, CH)
            .setContentTitle("玄音").setContentText("灵动岛运行中")
            .setSmallIcon(R.drawable.ic_launcher).setOngoing(true).build();
        startForeground(NID, n);
    }

    private void initWindow() {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        root = (FrameLayout) LayoutInflater.from(this).inflate(R.layout.island_root, null);

        backdrop = root.findViewById(R.id.island_backdrop);

        // 折叠
        collapsed = root.findViewById(R.id.island_collapsed);
        tvTimeMini = root.findViewById(R.id.tv_time_mini);
        tvBatteryMini = root.findViewById(R.id.tv_battery_mini);
        tvCollapsedTitle = root.findViewById(R.id.tv_collapsed_title);
        collapsedLifeGroup = root.findViewById(R.id.group_life_mini);
        collapsedMusicGroup = root.findViewById(R.id.group_music_mini);
        ivCoverMini = root.findViewById(R.id.iv_cover_mini);
        pulseMini = root.findViewById(R.id.pulse_mini);

        // 展开
        expanded = root.findViewById(R.id.island_expanded);
        tvTimeBig = root.findViewById(R.id.tv_time_big);
        tvDate = root.findViewById(R.id.tv_date);
        tvAlarm = root.findViewById(R.id.tv_alarm);
        tvBatteryBig = root.findViewById(R.id.tv_battery_big);
        tvSongTitle = root.findViewById(R.id.tv_song_title);
        tvSongArtist = root.findViewById(R.id.tv_song_artist);
        tvLyric = root.findViewById(R.id.tv_lyric);
        ivCoverBig = root.findViewById(R.id.iv_cover_big);
        progress = root.findViewById(R.id.progress);
        lifeGroup = root.findViewById(R.id.group_life);
        musicGroup = root.findViewById(R.id.group_music);
        btnPrev = root.findViewById(R.id.btn_prev);
        btnPlay = root.findViewById(R.id.btn_play);
        btnNext = root.findViewById(R.id.btn_next);

        if (btnPrev != null) btnPrev.setOnClickListener(v -> control("prev"));
        if (btnPlay != null) btnPlay.setOnClickListener(v -> control("toggle"));
        if (btnNext != null) btnNext.setOnClickListener(v -> control("next"));

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(
            0, 0, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = IslandAdaptiveHelper.getSafeTop(this) + IslandAdaptiveHelper.dpToPx(this, 8);

        try { wm.addView(root, params); } catch (Throwable t) { return; }

        root.setOnClickListener(v -> toggle());

        // 生活模式默认
        musicMode = false;
        setModeView(false);
        applySize(false);
        syncBackdropMode();
    }

    private void setModeView(boolean music) {
        // 折叠
        if (collapsedLifeGroup != null) collapsedLifeGroup.setVisibility(music ? View.GONE : View.VISIBLE);
        if (collapsedMusicGroup != null) collapsedMusicGroup.setVisibility(music ? View.VISIBLE : View.GONE);
        // 展开
        if (lifeGroup != null) lifeGroup.setVisibility(music ? View.GONE : View.VISIBLE);
        if (musicGroup != null) musicGroup.setVisibility(music ? View.VISIBLE : View.GONE);
    }

    private void toggle() {
        expanded = !expanded;
        if (expanded) {
            // 展开：先显示再动画
            expanded.setVisibility(View.VISIBLE);
            expanded.setAlpha(0f);
            expanded.setScaleX(0.92f); expanded.setScaleY(0.92f);
            applySize(true);
            expanded.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(260).setInterpolator(new OvershootInterpolator(1.2f)).start();
            collapsed.animate().alpha(0f).setDuration(120).withEndAction(() -> {
                collapsed.setVisibility(View.GONE);
                collapsed.setAlpha(1f);
            }).start();
        } else {
            collapsed.setVisibility(View.VISIBLE);
            collapsed.setAlpha(0f);
            applySize(false);
            collapsed.animate().alpha(1f).setDuration(220).start();
            expanded.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f)
                .setDuration(180).withEndAction(() -> {
                    expanded.setVisibility(View.GONE);
                    expanded.setAlpha(1f);
                    expanded.setScaleX(1f); expanded.setScaleY(1f);
                }).start();
        }
        // 3 秒后自动回折叠
        ui.removeCallbacks(autoCollapse);
        if (expanded) ui.postDelayed(autoCollapse, 5000);
    }

    private final Runnable autoCollapse = () -> { if (expanded) toggle(); };

    private void applySize(boolean exp) {
        if (root == null || params == null) return;
        int w, h;
        if (exp) {
            w = IslandAdaptiveHelper.getExpandedWidth(this);
            h = IslandAdaptiveHelper.getExpandedHeight(this);
        } else {
            w = IslandAdaptiveHelper.getCollapsedWidth(this);
            h = IslandAdaptiveHelper.getCollapsedHeight(this);
        }
        params.width = w;
        params.height = h;
        params.y = IslandAdaptiveHelper.getSafeTop(this) + IslandAdaptiveHelper.dpToPx(this, 8);

        final float radius = IslandAdaptiveHelper.getCornerRadius(this, exp, w, h);
        root.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
        root.setClipToOutline(true);
        if (backdrop != null) backdrop.setCornerRadius(radius);
        try { wm.updateViewLayout(root, params); } catch (Throwable ignored) {}
    }

    private void onSong(Song s) {
        currentSong = s;
        musicMode = true;
        setModeView(true);
        if (tvCollapsedTitle != null) tvCollapsedTitle.setText(s.title);
        if (tvSongTitle != null) tvSongTitle.setText(s.title);
        if (tvSongArtist != null) tvSongArtist.setText(s.displayArtist());
        if (ivCoverMini != null) WallpaperHelper.loadCover(this, ivCoverMini, s.id);
        if (ivCoverBig != null) WallpaperHelper.loadCover(this, ivCoverBig, s.id);
        if (pulseMini != null) pulseMini.setPulsing(true);
        if (tvLyric != null) tvLyric.setText(s.title + "\n" + s.displayArtist());
        syncBackdropMode();
        if (backdrop != null) backdrop.setMode(1);
    }

    private void updatePlayIcon(boolean playing) {
        if (btnPlay instanceof TextView) ((TextView) btnPlay).setText(playing ? "⏸" : "▶");
    }

    private void control(String action) {
        Intent i = new Intent(this, MusicService.class);
        switch (action) {
            case "prev": i.setAction(MusicService.ACTION_PREV); break;
            case "next": i.setAction(MusicService.ACTION_NEXT); break;
            default: i.setAction(MusicService.ACTION_TOGGLE); break;
        }
        startService(i);
    }

    private void showNotify(String title) {
        if (tvCollapsedTitle != null && title != null) tvCollapsedTitle.setText(title);
        if (backdrop != null) backdrop.setMode(3);
        ui.postDelayed(this::syncBackdropMode, 4000);
    }

    private void showAlarm() {
        if (tvCollapsedTitle != null) tvCollapsedTitle.setText("⏰ 闹钟");
        if (backdrop != null) backdrop.setMode(2);
        ui.postDelayed(this::syncBackdropMode, 6000);
    }

    private void syncBackdropMode() {
        if (backdrop == null) return;
        backdrop.setMode(musicMode ? 1 : 0);
    }

    private void startClock() {
        final SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        final SimpleDateFormat df = new SimpleDateFormat("M月d日 EEEE", Locale.CHINA);
        final SimpleDateFormat dfShort = new SimpleDateFormat("M月d日 EEE", Locale.CHINA);
        ui.post(new Runnable() {
            @Override public void run() {
                Date now = new Date();
                if (tvTimeMini != null) tvTimeMini.setText(tf.format(now));
                if (tvTimeBig != null) tvTimeBig.setText(tf.format(now));
                if (tvDate != null) tvDate.setText(df.format(now));
                if (tvCollapsedTitle != null && !musicMode)
                    tvCollapsedTitle.setText(dfShort.format(now));
                ui.postDelayed(this, 1000);
            }
        });
    }

    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        applySize(expanded);
    }

    @Override public int onStartCommand(Intent i, int f, int s) {
        if (i != null && "TEST_NOTIFY".equals(i.getAction())) showNotify("测试通知");
        return START_STICKY;
    }

    @Override public void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(rx); } catch (Throwable ignored) {}
        if (root != null && wm != null) { try { wm.removeView(root); } catch (Throwable ignored) {} root = null; }
    }

    @Nullable @Override public IBinder onBind(Intent i) { return null; }
}
