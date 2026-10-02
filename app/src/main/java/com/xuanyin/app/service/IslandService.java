package com.xuanyin.app.service;
import android.animation.ValueAnimator;
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
import com.xuanyin.app.util.*;
import com.xuanyin.app.widget.IslandBackdropView;
import com.xuanyin.app.widget.IslandPulseView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public class IslandService extends Service {
    private static final String CH = "island_ch";
    private static final int NID = 1001;
    private static final long OPEN_DURATION = 1000L;
    private static final long CLOSE_DURATION = 900L;
    private static final Interpolator EASE_OPEN = new PathInterpolator(0.16f, 1f, 0.3f, 1f);
    private static final Interpolator EASE_CLOSE = new PathInterpolator(0.4f, 0f, 0.6f, 1f);
    private WindowManager wm;
    private FrameLayout root;
    private IslandBackdropView backdrop;
    private View collapsedView, collapsedLife, collapsedMusic, collapsedDl;
    private TextView tvTimeMini, tvBatteryMini, tvCollapsedTitle, tvDlTitle, tvDlPct;
    private ProgressBar dlBar;
    private ImageView ivCoverMini;
    private IslandPulseView pulseMini;
    private View expandedView, lifeGroup, musicGroup;
    private TextView tvTimeBig, tvDate, tvAlarm, tvBatteryBig, tvSongTitle, tvSongArtist, tvLyric;
    private ImageView ivCoverBig;
    private ProgressBar progress;
    private WindowManager.LayoutParams params;
    private boolean expandedState = false, musicMode = false, downloadMode = false;
    private boolean charging = false;
    private Song currentSong;
    private ValueAnimator sizeAnim;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final BroadcastReceiver rx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if (a == null) return;
            try {
                switch (a) {
                    case "com.xuanyin.app.SONG_CHANGED":
                        Song s = (Song) i.getSerializableExtra("song");
                        if (s != null) onSong(s);
                        break;
                    case "com.xuanyin.app.PLAY_STATE":
                        boolean p = i.getBooleanExtra("playing", false);
                        if (pulseMini != null) pulseMini.setPulsing(p);
                        View bp = root == null ? null : root.findViewById(R.id.btn_play);
                        if (bp instanceof TextView) ((TextView) bp).setText(p ? "⏸" : "▶");
                        break;
                    case "com.xuanyin.app.NOTIFY": showNotify(i.getStringExtra("title")); break;
                    case "com.xuanyin.app.ALARM": showAlarm(); break;
                    case "com.xuanyin.app.DL_START": onDlStart(i.getStringExtra("title")); break;
                    case "com.xuanyin.app.DL_PROGRESS": onDlProg(i.getStringExtra("title"), i.getIntExtra("percent", 0)); break;
                    case "com.xuanyin.app.DL_DONE": onDlDone(); break;
                    case "com.xuanyin.app.DL_ERROR": onDlErr(); break;
                    case Intent.ACTION_BATTERY_CHANGED: onBattery(i); break;
                }
            } catch (Throwable ignored) {}
        }
    };
    private void onBattery(Intent i) {
        int lv = i.getIntExtra("level", 0);
        int sc = i.getIntExtra("scale", 100);
        int pct = sc > 0 ? (int)(100f * lv / sc) : lv;
        int st = i.getIntExtra("status", 0);
        boolean isCharging = st == 2 || st == 5;
        if (isCharging != charging) {
            charging = isCharging;
            if (charging) {
                Haptic.charge(this);
                if (backdrop != null) { backdrop.setMode(2); backdrop.setChargingBreath(true); }
                flashGreen();
            } else if (backdrop != null) {
                backdrop.setChargingBreath(false);
                backdrop.setMode(musicMode ? 1 : 0);
            }
        }
        String txt = isCharging ? "⚡" + pct + "%" : pct + "%";
        if (tvBatteryMini != null) {
            tvBatteryMini.setText(txt);
            tvBatteryMini.setTextColor(isCharging ? 0xFF30D158 : 0xFFFFFFFF);
        }
        if (tvBatteryBig != null) {
            tvBatteryBig.setText(txt);
            tvBatteryBig.setTextColor(isCharging ? 0xFF30D158 : 0xFF4CD964);
        }
    }
    private void flashGreen() {
        if (backdrop == null) return;
        backdrop.animate().cancel();
        backdrop.animate().scaleX(1.05f).scaleY(1.05f).setDuration(180)
            .withEndAction(() -> backdrop.animate().scaleX(1f).scaleY(1f)
                .setDuration(380).setInterpolator(EASE_CLOSE).start()).start();
    }
    @Override public void onCreate() {
        super.onCreate();
        startFg(); initWin(); startClock();
        IntentFilter f = new IntentFilter();
        f.addAction("com.xuanyin.app.SONG_CHANGED");
        f.addAction("com.xuanyin.app.PLAY_STATE");
        f.addAction("com.xuanyin.app.NOTIFY");
        f.addAction("com.xuanyin.app.ALARM");
        f.addAction("com.xuanyin.app.DL_START");
        f.addAction("com.xuanyin.app.DL_PROGRESS");
        f.addAction("com.xuanyin.app.DL_DONE");
        f.addAction("com.xuanyin.app.DL_ERROR");
        f.addAction(Intent.ACTION_BATTERY_CHANGED);
        try {
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(rx, f, Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(rx, f);
        } catch (Throwable ignored) {}
    }
    private void startFg() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm.getNotificationChannel(CH) == null) {
                    NotificationChannel ch = new NotificationChannel(CH, "玄音·灵动岛", NotificationManager.IMPORTANCE_LOW);
                    ch.setShowBadge(false); nm.createNotificationChannel(ch);
                }
            }
            Notification n = new NotificationCompat.Builder(this, CH)
                .setContentTitle("玄音").setContentText("灵动岛运行中")
                .setSmallIcon(R.drawable.ic_launcher).setOngoing(true).build();
            startForeground(NID, n);
        } catch (Throwable ignored) {}
    }
    private void initWin() {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        try {
            root = (FrameLayout) LayoutInflater.from(this).inflate(R.layout.island_root, null);
        } catch (Throwable t) { stopSelf(); return; }
        backdrop = root.findViewById(R.id.island_backdrop);
        collapsedView = root.findViewById(R.id.island_collapsed);
        collapsedLife = root.findViewById(R.id.group_life_mini);
        collapsedMusic = root.findViewById(R.id.group_music_mini);
        collapsedDl = root.findViewById(R.id.group_download_mini);
        tvTimeMini = root.findViewById(R.id.tv_time_mini);
        tvBatteryMini = root.findViewById(R.id.tv_battery_mini);
        tvCollapsedTitle = root.findViewById(R.id.tv_collapsed_title);
        tvDlTitle = root.findViewById(R.id.tv_dl_title_mini);
        tvDlPct = root.findViewById(R.id.tv_dl_pct_mini);
        dlBar = root.findViewById(R.id.dl_bar_mini);
        ivCoverMini = root.findViewById(R.id.iv_cover_mini);
        pulseMini = root.findViewById(R.id.pulse_mini);
        expandedView = root.findViewById(R.id.island_expanded);
        lifeGroup = root.findViewById(R.id.group_life);
        musicGroup = root.findViewById(R.id.group_music);
        tvTimeBig = root.findViewById(R.id.tv_time_big);
        tvDate = root.findViewById(R.id.tv_date);
        tvAlarm = root.findViewById(R.id.tv_alarm);
        tvBatteryBig = root.findViewById(R.id.tv_battery_big);
        tvSongTitle = root.findViewById(R.id.tv_song_title);
        tvSongArtist = root.findViewById(R.id.tv_song_artist);
        tvLyric = root.findViewById(R.id.tv_lyric);
        ivCoverBig = root.findViewById(R.id.iv_cover_big);
        progress = root.findViewById(R.id.progress);
        View bp = root.findViewById(R.id.btn_prev);
        View bpl = root.findViewById(R.id.btn_play);
        View bn = root.findViewById(R.id.btn_next);
        if (bp != null) bp.setOnClickListener(v -> { Haptic.tap(v); Anim.pressLight(v); ctrl("prev"); });
        if (bpl != null) bpl.setOnClickListener(v -> { Haptic.tap(v); Anim.pressLight(v); ctrl("toggle"); });
        if (bn != null) bn.setOnClickListener(v -> { Haptic.tap(v); Anim.pressLight(v); ctrl("next"); });
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(0, 0, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = IslandAdaptiveHelper.getSafeTop(this) + IslandAdaptiveHelper.dpToPx(this, 8);
        try { wm.addView(root, params); } catch (Throwable t) { stopSelf(); return; }
        root.setOnClickListener(v -> toggle());
        setModeView();
        applySize(false, false);
        if (backdrop != null) backdrop.setMode(0);
    }
    private void setModeView() {
        if (collapsedLife == null) return;
        boolean dl = downloadMode;
        boolean mu = musicMode && !dl;
        boolean lf = !mu && !dl;
        collapsedLife.setVisibility(lf ? View.VISIBLE : View.GONE);
        if (collapsedMusic != null) collapsedMusic.setVisibility(mu ? View.VISIBLE : View.GONE);
        if (collapsedDl != null) collapsedDl.setVisibility(dl ? View.VISIBLE : View.GONE);
        if (lifeGroup != null) lifeGroup.setVisibility(mu || dl ? View.GONE : View.VISIBLE);
        if (musicGroup != null) musicGroup.setVisibility(mu ? View.VISIBLE : View.GONE);
    }
    private void toggle() {
        expandedState = !expandedState;
        if (expandedState) {
            Haptic.expand(this);
            if (expandedView != null) {
                expandedView.setVisibility(View.VISIBLE);
                expandedView.setAlpha(0f);
                expandedView.setScaleX(0.86f); expandedView.setScaleY(0.86f);
                expandedView.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(OPEN_DURATION).setInterpolator(EASE_OPEN).start();
            }
            applySize(true, true, OPEN_DURATION, EASE_OPEN);
            if (collapsedView != null) {
                collapsedView.animate().alpha(0f).setDuration(OPEN_DURATION / 3)
                    .setInterpolator(EASE_OPEN).withEndAction(() -> {
                        collapsedView.setVisibility(View.GONE);
                        collapsedView.setAlpha(1f);
                    }).start();
            }
            ui.removeCallbacks(autoCol);
            ui.postDelayed(autoCol, 7000);
        } else {
            Haptic.collapse(this);
            if (collapsedView != null) {
                collapsedView.setVisibility(View.VISIBLE);
                collapsedView.setAlpha(0f);
                collapsedView.animate().alpha(1f).setDuration(CLOSE_DURATION)
                    .setInterpolator(EASE_CLOSE).start();
            }
            applySize(false, true, CLOSE_DURATION, EASE_CLOSE);
            if (expandedView != null) {
                expandedView.animate().alpha(0f).scaleX(0.86f).scaleY(0.86f)
                    .setDuration(CLOSE_DURATION / 2).setInterpolator(EASE_CLOSE).withEndAction(() -> {
                        expandedView.setVisibility(View.GONE);
                        expandedView.setAlpha(1f);
                        expandedView.setScaleX(1f); expandedView.setScaleY(1f);
                    }).start();
            }
            ui.removeCallbacks(autoCol);
        }
    }
    private final Runnable autoCol = () -> { if (expandedState) toggle(); };
    private void applySize(final boolean exp, boolean animate) {
        applySize(exp, animate, exp ? OPEN_DURATION : CLOSE_DURATION, exp ? EASE_OPEN : EASE_CLOSE);
    }
    private void applySize(final boolean exp, boolean animate, long duration, Interpolator interp) {
        if (root == null || params == null) return;
        final int tw = exp ? IslandAdaptiveHelper.getExpandedWidth(this) : IslandAdaptiveHelper.getCollapsedWidth(this);
        final int th = exp ? IslandAdaptiveHelper.getExpandedHeight(this) : IslandAdaptiveHelper.getCollapsedHeight(this);
        final int y = IslandAdaptiveHelper.getSafeTop(this) + IslandAdaptiveHelper.dpToPx(this, 8);
        if (sizeAnim != null && sizeAnim.isRunning()) sizeAnim.cancel();
        if (!animate) {
            params.width = tw; params.height = th; params.y = y;
            updateRadius(exp, tw, th);
            try { wm.updateViewLayout(root, params); } catch (Throwable ignored) {}
            return;
        }
        final int sw = params.width > 0 ? params.width : IslandAdaptiveHelper.getCollapsedWidth(this);
        final int sh = params.height > 0 ? params.height : IslandAdaptiveHelper.getCollapsedHeight(this);
        sizeAnim = ValueAnimator.ofFloat(0f, 1f);
        sizeAnim.setDuration(duration);
        sizeAnim.setInterpolator(interp);
        sizeAnim.addUpdateListener(a -> {
            float p = (float) a.getAnimatedValue();
            int w = (int)(sw + (tw - sw) * p);
            int h = (int)(sh + (th - sh) * p);
            params.width = w; params.height = h; params.y = y;
            updateRadius(exp, w, h);
            try { wm.updateViewLayout(root, params); } catch (Throwable ignored) {}
        });
        sizeAnim.start();
    }
    private void updateRadius(final boolean exp, int w, int h) {
        final float rad = IslandAdaptiveHelper.getCornerRadius(this, exp, w, h);
        root.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), rad); }
        });
        root.setClipToOutline(true);
        if (backdrop != null) backdrop.setCornerRadius(rad);
    }
    private void onSong(Song s) {
        currentSong = s; musicMode = true; downloadMode = false;
        setModeView();
        if (tvCollapsedTitle != null) tvCollapsedTitle.setText(s.title);
        if (tvSongTitle != null) tvSongTitle.setText(s.title);
        if (tvSongArtist != null) tvSongArtist.setText(s.displayArtist());
        if (ivCoverMini != null) WallpaperHelper.loadCover(this, ivCoverMini, s.id);
        if (ivCoverBig != null) WallpaperHelper.loadCover(this, ivCoverBig, s.id);
        if (pulseMini != null) pulseMini.setPulsing(true);
        if (tvLyric != null) tvLyric.setText(s.title + "\n" + s.displayArtist());
        if (backdrop != null) backdrop.setMode(charging ? 2 : 1);
    }
    private void onDlStart(String t) {
        downloadMode = true; setModeView();
        if (tvDlTitle != null) tvDlTitle.setText(t == null ? "下载中" : t);
        if (tvDlPct != null) tvDlPct.setText("0%");
        if (dlBar != null) dlBar.setProgress(0);
        if (backdrop != null) backdrop.setMode(4);
    }
    private void onDlProg(String t, int p) {
        if (!downloadMode) onDlStart(t);
        if (tvDlPct != null) tvDlPct.setText(p + "%");
        if (dlBar != null) dlBar.setProgress(p);
        if (tvDlTitle != null && t != null) tvDlTitle.setText(t);
    }
    private void onDlDone() {
        if (tvDlPct != null) tvDlPct.setText("✓");
        if (dlBar != null) dlBar.setProgress(100);
        ui.postDelayed(() -> { downloadMode = false; setModeView();
            if (backdrop != null) backdrop.setMode(charging ? 2 : (musicMode ? 1 : 0)); }, 2000);
    }
    private void onDlErr() {
        if (tvDlPct != null) tvDlPct.setText("失败");
        ui.postDelayed(() -> { downloadMode = false; setModeView(); }, 1600);
    }
    private void ctrl(String a) {
        Intent i = new Intent(this, MusicService.class);
        switch (a) {
            case "prev": i.setAction(MusicService.ACTION_PREV); break;
            case "next": i.setAction(MusicService.ACTION_NEXT); break;
            default: i.setAction(MusicService.ACTION_TOGGLE);
        }
        try { startService(i); } catch (Throwable ignored) {}
    }
    private void showNotify(String t) {
        if (tvCollapsedTitle != null && t != null) tvCollapsedTitle.setText(t);
        if (backdrop != null) backdrop.setMode(3);
        ui.postDelayed(() -> { if (backdrop != null)
            backdrop.setMode(charging ? 2 : (musicMode ? 1 : 0)); }, 3500);
    }
    private void showAlarm() {
        if (tvCollapsedTitle != null) tvCollapsedTitle.setText("⏰ 闹钟");
        if (backdrop != null) backdrop.setMode(2);
        ui.postDelayed(() -> { if (backdrop != null)
            backdrop.setMode(charging ? 2 : (musicMode ? 1 : 0)); }, 5000);
    }
    private void startClock() {
        final SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        final SimpleDateFormat df = new SimpleDateFormat("M月d日 EEE", Locale.CHINA);
        final SimpleDateFormat dfL = new SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINA);
        ui.post(new Runnable() {
            @Override public void run() {
                try {
                    Date n = new Date();
                    if (tvTimeMini != null) tvTimeMini.setText(tf.format(n));
                    if (tvTimeBig != null) tvTimeBig.setText(tf.format(n));
                    if (tvDate != null) tvDate.setText(dfL.format(n));
                    if (tvCollapsedTitle != null && !musicMode && !downloadMode)
                        tvCollapsedTitle.setText(df.format(n));
                    if (tvAlarm != null) {
                        String al = AlarmHelper.text();
                        tvAlarm.setText("未设置".equals(al) ? "未设置闹钟" : "闹钟 " + al);
                    }
                } catch (Throwable ignored) {}
                ui.postDelayed(this, 1000);
            }
        });
    }
    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        applySize(expandedState, true);
    }
    @Override public int onStartCommand(Intent i, int f, int s) {
        if (i == null) return START_STICKY;
        String a = i.getAction();
        if ("TEST_NOTIFY".equals(a)) showNotify("测试通知");
        else if ("REFRESH_SIZE".equals(a)) applySize(expandedState, true);
        return START_STICKY;
    }
    @Override public void onDestroy() {
        super.onDestroy();
        if (sizeAnim != null) sizeAnim.cancel();
        try { unregisterReceiver(rx); } catch (Throwable ignored) {}
        if (root != null && wm != null) { try { wm.removeView(root); } catch (Throwable ignored) {} root = null; }
    }
    @Nullable @Override public IBinder onBind(Intent i) { return null; }
}
