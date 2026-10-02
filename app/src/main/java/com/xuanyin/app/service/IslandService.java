package com.xuanyin.app.service;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.xuanyin.app.R;
import com.xuanyin.app.RecordActivity;
import com.xuanyin.app.model.Song;
import com.xuanyin.app.util.Anim;
import com.xuanyin.app.util.Haptic;
import com.xuanyin.app.util.Prefs;
import com.xuanyin.app.util.WallpaperHelper;
import com.xuanyin.app.widget.ChargingEffectView;
import com.xuanyin.app.widget.IslandBackdropView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IslandService extends Service {
    private static final String CH = "island_ch";
    private static final int NID = 1001;
    private static final long SPLIT_OUT_MS = 400L;
    private static final long SPLIT_BACK_MS = 450L;
    private static final long NOTIF_BASE_MS = 5000L;
    private static final long NOTIF_EXTRA_PER10 = 1800L;
    private static final long IDLE_TIMEOUT = 20000L;
    private static final Interpolator EASE_ELASTIC = new PathInterpolator(0.34f, 1.15f, 0.36f, 1f);
    private static final Interpolator EASE_SMOOTH = new PathInterpolator(0.16f, 1f, 0.3f, 1f);
    private static final Interpolator EASE_OPEN = new DecelerateInterpolator(2.4f);
    private static final Interpolator EASE_SOFT = new PathInterpolator(0.4f, 0f, 0.2f, 1f);
    private static final Interpolator EASE_ROW = new DecelerateInterpolator(1.8f);

    private WindowManager wm;
    private FrameLayout root, mainIsland, notifIsland;
    private IslandBackdropView mainBackdrop, notifBackdrop;
    private ChargingEffectView mainCharging;
    private View collapsedLife, collapsedMusic, collapsedDl;
    private ScrollView expandedView;
    private TextView btnCloseExpanded;
    private View expLife, expMusic;
    private TextView tvTimeMini, tvDateMini, tvBatteryMini, tvTimeShort, tvTitleMini, tvLyricMini, tvLyric2Mini;
    private TextView tvDlTitle, tvDlPct;
    private ProgressBar dlBar;
    private ImageView ivCoverMini, ivCoverBig;
    private TextView tvTimeBig, tvDateBig, tvAlarmBig, tvBatteryBig;
    private TextView tvTitleBig, tvArtistBig;
    private TextView tvListenBig, tvRecState;
    private View rowRecord, rowTime;
    private TextView ly1, ly2, ly3, ly4, ly5;
    private ProgressBar progress;
    private TextView notifTitle, notifText, btnNotifClose;

    private WindowManager.LayoutParams params;
    private int screenWidth, mainW, expW, foldH, expH, splitPx;
    private boolean isExpanded = false;
    private boolean musicMode = false, downloadMode = false;
    private boolean charging = false, notifShowing = false, playing = false;
    private Song currentSong;
    private List<Lyr> lyrics = new ArrayList<Lyr>();
    private int lastLyricIdx = -1;
    private ValueAnimator notifAnim, sizeAnim;
    private long notifDuration = NOTIF_BASE_MS;
    private String pendingTitle = null, pendingText = null;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private static class Lyr { long t; String s; Lyr(long t, String s) { this.t = t; this.s = s; } }

    private final Runnable idleSwitch = new Runnable() {
        @Override public void run() { if (!musicMode) return; musicMode = false; refreshMode(); }
    };
    private final Runnable hideNotif = new Runnable() {
        @Override public void run() { dismissNotif(); }
    };

    private final BroadcastReceiver rx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if (a == null) return;
            try {
                if ("com.xuanyin.app.SONG_CHANGED".equals(a)) {
                    Song s = (Song) i.getSerializableExtra("song");
                    if (s != null) onSong(s);
                } else if ("com.xuanyin.app.LYRICS".equals(a)) {
                    parseLyrics(i.getStringExtra("lrc"));
                } else if ("com.xuanyin.app.PLAY_STATE".equals(a)) {
                    boolean p = i.getBooleanExtra("playing", false);
                    playing = p;
                    if (p) { musicMode = true; refreshMode(); ui.removeCallbacks(idleSwitch); }
                    else { ui.removeCallbacks(idleSwitch); ui.postDelayed(idleSwitch, IDLE_TIMEOUT); }
                } else if ("com.xuanyin.app.MODE_CHANGED".equals(a)) {
                    View bm = root.findViewById(R.id.btn_mode);
                    if (bm instanceof TextView) {
                        int m = i.getIntExtra("mode", 0);
                        ((TextView) bm).setText(m == 0 ? "🔁" : (m == 1 ? "🔂" : "🔀"));
                    }
                } else if ("com.xuanyin.app.REC_STATE".equals(a)) {
                    boolean on = i.getBooleanExtra("on", false);
                    if (tvRecState != null) {
                        tvRecState.setText(on ? "● 录制中" : "点击开始");
                        tvRecState.setTextColor(on ? 0xFFFF3B30 : 0xFFB8A8E8);
                    }
                } else if ("com.xuanyin.app.NOTIFY".equals(a)) {
                    showNotif(i.getStringExtra("title"), i.getStringExtra("text"));
                } else if ("com.xuanyin.app.ALARM".equals(a)) {
                    showAlarm();
                } else if ("com.xuanyin.app.DL_START".equals(a)) {
                    onDlStart(i.getStringExtra("title"));
                } else if ("com.xuanyin.app.DL_PROGRESS".equals(a)) {
                    onDlProg(i.getStringExtra("title"), i.getIntExtra("percent", 0));
                } else if ("com.xuanyin.app.DL_DONE".equals(a)) {
                    onDlDone();
                } else if ("com.xuanyin.app.DL_ERROR".equals(a)) {
                    onDlErr();
                } else if (Intent.ACTION_BATTERY_CHANGED.equals(a)) {
                    onBattery(i);
                }
            } catch (Throwable ignored) {}
        }
    };

    private long calcNotifDuration(String title, String text) {
        int len = 0;
        if (title != null) len += title.length();
        if (text != null) len += text.length();
        long d = NOTIF_BASE_MS;
        if (len > 14) d += ((len - 14 + 9) / 10) * NOTIF_EXTRA_PER10;
        if (d > 15000L) d = 15000L;
        return d;
    }

    private void parseLyrics(String lrc) {
        lyrics = new ArrayList<Lyr>(); lastLyricIdx = -1;
        if (lrc == null || lrc.length() == 0) return;
        try {
            String[] lines = lrc.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String raw = lines[i]; if (raw == null) continue;
                String line = raw.trim();
                if (line.length() < 4 || line.charAt(0) != '[') continue;
                int close = line.indexOf(']'); if (close < 3) continue;
                String stamp = line.substring(1, close);
                String text = line.substring(close + 1).trim();
                if (text.length() == 0) continue;
                int colon = stamp.indexOf(':'); if (colon < 1) continue;
                try {
                    int min = Integer.parseInt(stamp.substring(0, colon).trim());
                    float sec = Float.parseFloat(stamp.substring(colon + 1).trim());
                    lyrics.add(new Lyr((long)((min * 60 + sec) * 1000), text));
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }
    private int findLyricIdx(long pos) {
        if (lyrics == null || lyrics.size() == 0) return -1;
        int idx = -1;
        for (int i = 0; i < lyrics.size(); i++) { if (lyrics.get(i).t <= pos) idx = i; else break; }
        return idx;
    }

    private void onBattery(Intent i) {
        try {
            int lv = i.getIntExtra("level", 0), sc = i.getIntExtra("scale", 100);
            int pct = sc > 0 ? (int)(100f * lv / sc) : lv;
            int st = i.getIntExtra("status", 0);
            boolean isCharging = st == 2 || st == 5;
            if (isCharging != charging) {
                charging = isCharging;
                if (mainCharging != null) mainCharging.setActive(isCharging);
                if (mainBackdrop != null) {
                    mainBackdrop.setChargingBreath(isCharging);
                    mainBackdrop.setMode(isCharging ? 2 : (musicMode ? 1 : 0));
                }
                if (charging && mainIsland != null) {
                    Haptic.charge(this);
                    mainIsland.animate().cancel();
                    mainIsland.animate().scaleX(1.06f).scaleY(1.06f).setDuration(150)
                        .withEndAction(new Runnable() {
                            @Override public void run() {
                                if (mainIsland != null) mainIsland.animate()
                                    .scaleX(1f).scaleY(1f).setDuration(360).setInterpolator(EASE_SMOOTH).start();
                            }
                        }).start();
                }
            }
            String txt = isCharging ? "⚡" + pct + "%" : pct + "%";
            if (tvBatteryMini != null) { tvBatteryMini.setText(txt); tvBatteryMini.setTextColor(isCharging ? 0xFF30D158 : 0xFFFFFFFF); }
            if (tvBatteryBig != null) { tvBatteryBig.setText(txt); tvBatteryBig.setTextColor(isCharging ? 0xFF30D158 : 0xFF4CD964); }
        } catch (Throwable ignored) {}
    }

    @Override public void onCreate() {
        super.onCreate();
        screenWidth = getResources().getDisplayMetrics().widthPixels;
        foldH = dp(44); expH = dp(300);
        mainW = Math.min(dp(230), (int)(screenWidth * 0.60f));
        expW = (int)(screenWidth * 0.92f);
        splitPx = (int)(screenWidth * 0.34f);
        startFg(); initWin(); startClock();
        IntentFilter f = new IntentFilter();
        f.addAction("com.xuanyin.app.SONG_CHANGED");
        f.addAction("com.xuanyin.app.LYRICS");
        f.addAction("com.xuanyin.app.PLAY_STATE");
        f.addAction("com.xuanyin.app.MODE_CHANGED");
        f.addAction("com.xuanyin.app.REC_STATE");
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
                if (nm != null && nm.getNotificationChannel(CH) == null) {
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
        try {
            wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            root = (FrameLayout) LayoutInflater.from(this).inflate(R.layout.island_root, null);
            mainIsland = (FrameLayout) root.findViewById(R.id.main_island);
            notifIsland = (FrameLayout) root.findViewById(R.id.notif_island);
            mainBackdrop = (IslandBackdropView) root.findViewById(R.id.main_backdrop);
            notifBackdrop = (IslandBackdropView) root.findViewById(R.id.notif_backdrop);
            mainCharging = (ChargingEffectView) root.findViewById(R.id.main_charging);
            collapsedLife = root.findViewById(R.id.main_collapsed_life);
            collapsedMusic = root.findViewById(R.id.main_collapsed_music);
            collapsedDl = root.findViewById(R.id.main_collapsed_dl);
            expandedView = (ScrollView) root.findViewById(R.id.main_expanded);
            btnCloseExpanded = (TextView) root.findViewById(R.id.btn_close_expanded);
            expLife = root.findViewById(R.id.exp_life);
            expMusic = root.findViewById(R.id.exp_music);
            tvTimeMini = (TextView) root.findViewById(R.id.tv_time_mini);
            tvDateMini = (TextView) root.findViewById(R.id.tv_date_mini);
            tvBatteryMini = (TextView) root.findViewById(R.id.tv_battery_mini);
            tvTimeShort = (TextView) root.findViewById(R.id.tv_time_short);
            tvTitleMini = (TextView) root.findViewById(R.id.tv_title_mini);
            tvLyricMini = (TextView) root.findViewById(R.id.tv_lyric_mini);
            tvLyric2Mini = (TextView) root.findViewById(R.id.tv_lyric2_mini);
            tvDlTitle = (TextView) root.findViewById(R.id.tv_dl_title);
            tvDlPct = (TextView) root.findViewById(R.id.tv_dl_pct);
            dlBar = (ProgressBar) root.findViewById(R.id.dl_bar);
            ivCoverMini = (ImageView) root.findViewById(R.id.iv_cover_mini);
            ivCoverBig = (ImageView) root.findViewById(R.id.iv_cover_big);
            tvTimeBig = (TextView) root.findViewById(R.id.tv_time_big);
            tvDateBig = (TextView) root.findViewById(R.id.tv_date_big);
            tvAlarmBig = (TextView) root.findViewById(R.id.tv_alarm_big);
            tvBatteryBig = (TextView) root.findViewById(R.id.tv_battery_big);
            tvTitleBig = (TextView) root.findViewById(R.id.tv_title_big);
            tvArtistBig = (TextView) root.findViewById(R.id.tv_artist_big);
            tvListenBig = (TextView) root.findViewById(R.id.tv_listen_big);
            tvRecState = (TextView) root.findViewById(R.id.tv_rec_state);
            rowRecord = root.findViewById(R.id.row_record);
            rowTime = root.findViewById(R.id.row_time);
            ly1 = (TextView) root.findViewById(R.id.ly1);
            ly2 = (TextView) root.findViewById(R.id.ly2);
            ly3 = (TextView) root.findViewById(R.id.ly3);
            ly4 = (TextView) root.findViewById(R.id.ly4);
            ly5 = (TextView) root.findViewById(R.id.ly5);
            progress = (ProgressBar) root.findViewById(R.id.progress);
            notifTitle = (TextView) root.findViewById(R.id.notif_title);
            notifText = (TextView) root.findViewById(R.id.notif_text);
            btnNotifClose = (TextView) root.findViewById(R.id.btn_notif_close);

            // 点击岛 = 展开/收回
            if (mainIsland != null) mainIsland.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { toggle(); }
            });
            if (expandedView != null) expandedView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { if (isExpanded) setExpanded(false); }
            });
            if (btnCloseExpanded != null) btnCloseExpanded.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { Haptic.tap(v); setExpanded(false); }
            });
            if (notifIsland != null) notifIsland.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { dismissNotif(); }
            });
            if (btnNotifClose != null) btnNotifClose.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { Haptic.tap(v); dismissNotif(); }
            });

            // 录屏按钮
            if (rowRecord != null) rowRecord.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Haptic.tap(v); Anim.pressLight(v);
                    try {
                        Intent i = new Intent(IslandService.this, RecordActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    } catch (Throwable ignored) {}
                }
            });

            View bp = root.findViewById(R.id.btn_prev);
            View bpl = root.findViewById(R.id.btn_play);
            View bn = root.findViewById(R.id.btn_next);
            View bm = root.findViewById(R.id.btn_mode);
            if (bp != null) bp.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { Haptic.tap(v); Anim.pressLight(v); ctrl("prev"); } });
            if (bpl != null) bpl.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { Haptic.tap(v); Anim.pressLight(v); ctrl("toggle"); } });
            if (bn != null) bn.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { Haptic.tap(v); Anim.pressLight(v); ctrl("next"); } });
            if (bm != null) bm.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { Haptic.tap(v); Anim.pressLight(v); ctrl("mode"); } });

            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
            params = new WindowManager.LayoutParams(
                screenWidth, foldH, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
            // ★ 关键：居中显示
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.x = 0;
            params.y = getSafeTop() + dp(2);
            wm.addView(root, params);
            applyWindowSize(mainW, foldH, false);
            refreshMode();
            refreshTime();
        } catch (Throwable t) { stopSelf(); }
    }

    private int getSafeTop() {
        try { int id = getResources().getIdentifier("status_bar_height", "dimen", "android"); if (id > 0) return getResources().getDimensionPixelSize(id); } catch (Throwable ignored) {}
        return dp(24);
    }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private void refreshMode() {
        try {
            boolean dl = downloadMode, mu = musicMode && !dl, lf = !mu && !dl;
            if (collapsedLife != null) collapsedLife.setVisibility(lf ? View.VISIBLE : View.GONE);
            if (collapsedMusic != null) collapsedMusic.setVisibility(mu ? View.VISIBLE : View.GONE);
            if (collapsedDl != null) collapsedDl.setVisibility(dl ? View.VISIBLE : View.GONE);
            if (expLife != null) expLife.setVisibility(mu ? View.GONE : View.VISIBLE);
            if (expMusic != null) expMusic.setVisibility(mu ? View.VISIBLE : View.GONE);
        } catch (Throwable ignored) {}
    }

    private void refreshTime() {
        try {
            int sec = Prefs.getInt("total_play_seconds", 0);
            int min = sec / 60;
            if (tvTimeShort != null) tvTimeShort.setText("♪ " + min + "min");
            if (tvListenBig != null) tvListenBig.setText(min + " 分钟");
        } catch (Throwable ignored) {}
    }

    private void applyWindowSize(int w, int h, boolean animate) {
        try {
            if (root == null || params == null) return;
            final int startW = params.width > 0 ? params.width : mainW;
            final int startH = params.height > 0 ? params.height : foldH;
            if (!animate) {
                params.width = w; params.height = h;
                try { wm.updateViewLayout(root, params); } catch (Throwable ignored) {}
                setChildSizes(w, h); updateRadius(h); return;
            }
            if (sizeAnim != null && sizeAnim.isRunning()) sizeAnim.cancel();
            sizeAnim = ValueAnimator.ofFloat(0f, 1f);
            sizeAnim.setDuration(SPLIT_OUT_MS);
            sizeAnim.setInterpolator(EASE_OPEN);
            final int fw = w, fh = h;
            sizeAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator a) {
                    float p = (float) a.getAnimatedValue();
                    int cw = (int)(startW + (fw - startW) * p);
                    int ch = (int)(startH + (fh - startH) * p);
                    params.width = cw; params.height = ch;
                    try { wm.updateViewLayout(root, params); } catch (Throwable ignored) {}
                    setChildSizes(cw, ch); updateRadius(ch);
                }
            });
            sizeAnim.start();
        } catch (Throwable ignored) {}
    }

    private void setChildSizes(int w, int h) {
        try {
            if (mainIsland != null) {
                ViewGroup.LayoutParams lp = mainIsland.getLayoutParams();
                if (lp.width != w) { lp.width = w; mainIsland.setLayoutParams(lp); }
                if (lp.height != h) { lp.height = h; mainIsland.setLayoutParams(lp); }
            }
            if (notifIsland != null) {
                ViewGroup.LayoutParams lp = notifIsland.getLayoutParams();
                if (lp.width != w) { lp.width = w; notifIsland.setLayoutParams(lp); }
                if (lp.height != h) { lp.height = h; notifIsland.setLayoutParams(lp); }
            }
        } catch (Throwable ignored) {}
    }

    private void updateRadius(int h) {
        try {
            final float r = isExpanded ? dp(24) : Math.min(dp(22), h / 2f);
            if (mainIsland != null) {
                mainIsland.setOutlineProvider(new ViewOutlineProvider() { @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r); } });
                mainIsland.setClipToOutline(true);
            }
            if (mainBackdrop != null) mainBackdrop.setCornerRadius(r);
            if (notifIsland != null) {
                notifIsland.setOutlineProvider(new ViewOutlineProvider() { @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r); } });
                notifIsland.setClipToOutline(true);
            }
            if (notifBackdrop != null) notifBackdrop.setCornerRadius(r);
        } catch (Throwable ignored) {}
    }

    private void toggle() {
        if (notifShowing) { dismissNotif(); return; }
        setExpanded(!isExpanded);
    }

    // ★ 分列式逐行入场动画
    private void animateRowsIn() {
        View[] rows = new View[]{tvTimeBig, tvDateBig, rowRecord, rowTime};
        // 音乐模式
        if (musicMode) rows = new View[]{ivCoverBig, ly1, ly2, ly3, ly4, ly5, progress};
        for (int i = 0; i < rows.length; i++) {
            View v = rows[i];
            if (v == null) continue;
            v.setAlpha(0f);
            v.setTranslationY(30f);
            v.animate().alpha(1f).translationY(0f)
                .setDuration(360)
                .setStartDelay(60L * i)
                .setInterpolator(EASE_ROW)
                .start();
        }
    }

    private void setExpanded(boolean exp) {
        try {
            if (isExpanded == exp) return;
            isExpanded = exp;
            if (exp) {
                Haptic.expand(this);
                refreshTime();
                if (expandedView != null) {
                    expandedView.setVisibility(View.VISIBLE);
                    expandedView.setAlpha(0f);
                    expandedView.setScaleX(0.96f); expandedView.setScaleY(0.96f);
                    expandedView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(SPLIT_OUT_MS).setInterpolator(EASE_OPEN).start();
                }
                if (btnCloseExpanded != null) {
                    btnCloseExpanded.setVisibility(View.VISIBLE);
                    btnCloseExpanded.setAlpha(0f);
                    btnCloseExpanded.animate().alpha(1f).setDuration(300).start();
                }
                View c = visibleCollapsed();
                if (c != null) c.animate().alpha(0f).setDuration(SPLIT_OUT_MS / 3).withEndAction(new Runnable() { @Override public void run() { View cc = visibleCollapsed(); if (cc != null) { cc.setVisibility(View.GONE); cc.setAlpha(1f); } } }).start();
                applyWindowSize(expW, expH, true);
                // 延迟启动行入场
                ui.postDelayed(new Runnable() {
                    @Override public void run() { animateRowsIn(); }
                }, 60);
            } else {
                Haptic.collapse(this);
                View c = visibleCollapsed();
                if (c != null) { c.setVisibility(View.VISIBLE); c.setAlpha(0f); c.animate().alpha(1f).setDuration(SPLIT_OUT_MS).setInterpolator(EASE_SMOOTH).start(); }
                if (expandedView != null) expandedView.animate().alpha(0f).scaleX(0.96f).scaleY(0.96f).setDuration(SPLIT_OUT_MS / 2).setInterpolator(EASE_SOFT).withEndAction(new Runnable() { @Override public void run() { if (expandedView != null) { expandedView.setVisibility(View.GONE); expandedView.setAlpha(1f); expandedView.setScaleX(1f); expandedView.setScaleY(1f); } } }).start();
                if (btnCloseExpanded != null) btnCloseExpanded.animate().alpha(0f).setDuration(200).withEndAction(new Runnable() { @Override public void run() { if (btnCloseExpanded != null) btnCloseExpanded.setVisibility(View.GONE); } }).start();
                applyWindowSize(mainW, foldH, true);
            }
        } catch (Throwable ignored) {}
    }

    private View visibleCollapsed() {
        if (collapsedLife != null && collapsedLife.getVisibility() == View.VISIBLE) return collapsedLife;
        if (collapsedMusic != null && collapsedMusic.getVisibility() == View.VISIBLE) return collapsedMusic;
        if (collapsedDl != null && collapsedDl.getVisibility() == View.VISIBLE) return collapsedDl;
        return collapsedLife;
    }

    private void showNotif(String title, String text) {
        try {
            if (notifIsland == null || mainIsland == null) return;
            if (notifShowing) {
                pendingTitle = title; pendingText = text;
                dismissNotif();
                return;
            }
            reallyShowNotif(title, text);
        } catch (Throwable ignored) {}
    }

    private void reallyShowNotif(String title, String text) {
        try {
            if (notifTitle != null) notifTitle.setText(title == null ? "通知" : title);
            if (notifText != null) notifText.setText(text == null ? "" : text);
            if (notifTitle != null) notifTitle.setSelected(true);
            if (notifText != null) notifText.setSelected(true);
            if (notifBackdrop != null) notifBackdrop.setMode(3);
            notifDuration = calcNotifDuration(title, text);
            notifShowing = true;
            Haptic.notify(this);
            if (isExpanded) setExpanded(false);
            notifIsland.setTranslationX(0);
            notifIsland.setAlpha(0f);
            notifIsland.setVisibility(View.VISIBLE);
            mainIsland.setTranslationX(0);
            mainIsland.setAlpha(1f);
            if (notifAnim != null && notifAnim.isRunning()) notifAnim.cancel();
            notifAnim = ValueAnimator.ofFloat(0f, 2f);
            notifAnim.setDuration(SPLIT_OUT_MS + SPLIT_BACK_MS);
            notifAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator a) {
                    float v = (float) a.getAnimatedValue();
                    if (v <= 1f) {
                        float p = EASE_ELASTIC.getInterpolation(v);
                        notifIsland.setTranslationX(splitPx * p);
                        notifIsland.setAlpha(Math.min(1f, v * 2f));
                    } else {
                        float p = EASE_SMOOTH.getInterpolation(v - 1f);
                        notifIsland.setTranslationX(splitPx * (1f - p));
                        notifIsland.setAlpha(1f);
                        mainIsland.setAlpha(1f - p);
                    }
                }
            });
            notifAnim.start();
            ui.removeCallbacks(hideNotif);
            ui.postDelayed(hideNotif, notifDuration + SPLIT_OUT_MS + SPLIT_BACK_MS);
        } catch (Throwable ignored) {}
    }

    private void dismissNotif() {
        try {
            if (!notifShowing) return;
            notifShowing = false;
            Haptic.swipe(this);
            if (notifAnim != null && notifAnim.isRunning()) notifAnim.cancel();
            notifAnim = ValueAnimator.ofFloat(0f, 2f);
            notifAnim.setDuration(SPLIT_OUT_MS + SPLIT_BACK_MS);
            notifAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator a) {
                    float v = (float) a.getAnimatedValue();
                    if (v <= 1f) {
                        float p = EASE_ELASTIC.getInterpolation(v);
                        notifIsland.setTranslationX(-splitPx * p);
                        notifIsland.setAlpha(1f - v * 0.5f);
                    } else {
                        float p = EASE_SMOOTH.getInterpolation(v - 1f);
                        notifIsland.setTranslationX(-splitPx * (1f - p));
                        notifIsland.setAlpha(0.5f * (1f - p));
                        mainIsland.setAlpha(p);
                    }
                }
            });
            notifAnim.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    if (notifIsland != null) { notifIsland.setVisibility(View.GONE); notifIsland.setTranslationX(0); notifIsland.setAlpha(0f); }
                    if (mainIsland != null) { mainIsland.setAlpha(1f); mainIsland.setTranslationX(0); }
                    if (mainBackdrop != null) mainBackdrop.setMode(charging ? 2 : (musicMode ? 1 : 0));
                    if (pendingTitle != null || pendingText != null) {
                        final String t = pendingTitle;
                        final String x = pendingText;
                        pendingTitle = null; pendingText = null;
                        ui.postDelayed(new Runnable() {
                            @Override public void run() { reallyShowNotif(t, x); }
                        }, 200);
                    }
                }
            });
            notifAnim.start();
        } catch (Throwable ignored) {}
    }

    private void onSong(Song s) {
        try {
            currentSong = s; musicMode = true; downloadMode = false;
            refreshMode();
            if (tvTitleMini != null) tvTitleMini.setText(s.title);
            if (tvTitleBig != null) tvTitleBig.setText(s.title);
            if (tvArtistBig != null) tvArtistBig.setText(s.displayArtist());
            if (ivCoverMini != null) WallpaperHelper.loadCover(this, ivCoverMini, s.id);
            if (ivCoverBig != null) WallpaperHelper.loadCover(this, ivCoverBig, s.id);
            lyrics = new ArrayList<Lyr>(); lastLyricIdx = -1;
            ui.removeCallbacks(idleSwitch);
            if (mainBackdrop != null) mainBackdrop.setMode(charging ? 2 : 1);
        } catch (Throwable ignored) {}
    }
    private void onDlStart(String t) {
        downloadMode = true; refreshMode();
        if (tvDlTitle != null) tvDlTitle.setText(t == null ? "下载中" : t);
        if (tvDlPct != null) tvDlPct.setText("0%");
        if (dlBar != null) dlBar.setProgress(0);
        if (mainBackdrop != null) mainBackdrop.setMode(4);
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
        ui.postDelayed(new Runnable() { @Override public void run() { downloadMode = false; refreshMode(); if (mainBackdrop != null) mainBackdrop.setMode(charging ? 2 : (musicMode ? 1 : 0)); } }, 2000);
    }
    private void onDlErr() {
        if (tvDlPct != null) tvDlPct.setText("失败");
        ui.postDelayed(new Runnable() { @Override public void run() { downloadMode = false; refreshMode(); } }, 1600);
    }
    private void ctrl(String a) {
        Intent i = new Intent(this, MusicService.class);
        if ("prev".equals(a)) i.setAction(MusicService.ACTION_PREV);
        else if ("next".equals(a)) i.setAction(MusicService.ACTION_NEXT);
        else if ("mode".equals(a)) i.setAction(MusicService.ACTION_CYCLE_MODE);
        else i.setAction(MusicService.ACTION_TOGGLE);
        try { startService(i); } catch (Throwable ignored) {}
    }
    private void showAlarm() {
        showNotif("⏰ 闹钟", "时间到了！");
        if (mainBackdrop != null) mainBackdrop.setMode(2);
        if (notifBackdrop != null) notifBackdrop.setMode(2);
    }
    private void tickLyrics() {
        try {
            if (!musicMode || lyrics == null || lyrics.size() == 0) return;
            long pos = 0;
            try { androidx.media3.exoplayer.ExoPlayer ep = MusicService.getPlayer(); if (ep != null) pos = ep.getCurrentPosition(); } catch (Throwable ignored) {}
            int idx = findLyricIdx(pos);
            if (idx == lastLyricIdx) return;
            lastLyricIdx = idx;
            if (idx < 0) return;
            Lyr cur = lyrics.get(idx);
            if (tvLyricMini != null) tvLyricMini.setText(cur.s);
            if (tvLyric2Mini != null) { if (idx + 1 < lyrics.size()) tvLyric2Mini.setText(lyrics.get(idx + 1).s); else tvLyric2Mini.setText(""); }
            TextView[] lv = new TextView[]{ly1, ly2, ly3, ly4, ly5};
            for (int k = -2; k <= 2; k++) {
                int j = idx + k;
                TextView v = lv[k + 2];
                if (v == null) continue;
                if (j >= 0 && j < lyrics.size()) {
                    v.setText(lyrics.get(j).s);
                    if (k == 0) { v.setAlpha(1f); v.setTextSize(15f); }
                    else if (k == -1 || k == 1) { v.setAlpha(0.7f); v.setTextSize(13f); }
                    else { v.setAlpha(0.35f); v.setTextSize(13f); }
                } else { v.setText(""); }
            }
        } catch (Throwable ignored) {}
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
                    if (tvDateBig != null) tvDateBig.setText(dfL.format(n));
                    if (tvDateMini != null && !musicMode && !downloadMode && !notifShowing) tvDateMini.setText(df.format(n));
                    if (tvAlarmBig != null) {
                        int h = Prefs.getInt("alarm_hour", -1), m = Prefs.getInt("alarm_min", -1);
                        tvAlarmBig.setText((h < 0 || m < 0) ? "未设置闹钟" : String.format(Locale.getDefault(), "闹钟 %02d:%02d", h, m));
                    }
                } catch (Throwable ignored) {}
                tickLyrics();
                refreshTime();
                ui.postDelayed(this, 1000);
            }
        });
    }
    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        screenWidth = getResources().getDisplayMetrics().widthPixels;
        mainW = Math.min(dp(230), (int)(screenWidth * 0.60f));
        expW = (int)(screenWidth * 0.92f);
        splitPx = (int)(screenWidth * 0.34f);
        if (isExpanded) applyWindowSize(expW, expH, false);
        else applyWindowSize(mainW, foldH, false);
    }
    @Override public int onStartCommand(Intent i, int f, int s) {
        if (i == null) return START_STICKY;
        String a = i.getAction();
        if ("TEST_NOTIFY".equals(a)) showNotif("测试通知", "这是一条比较长的通知文字用来测试灵动岛滚动和动态时长");
        else if ("REFRESH_SIZE".equals(a)) {
            if (isExpanded) applyWindowSize(expW, expH, false);
            else applyWindowSize(mainW, foldH, false);
        }
        return START_STICKY;
    }
    @Override public void onDestroy() {
        super.onDestroy();
        ui.removeCallbacks(idleSwitch);
        ui.removeCallbacks(hideNotif);
        if (notifAnim != null) notifAnim.cancel();
        if (sizeAnim != null) sizeAnim.cancel();
        try { unregisterReceiver(rx); } catch (Throwable ignored) {}
        if (root != null && wm != null) { try { wm.removeView(root); } catch (Throwable ignored) {} root = null; }
    }
    @Nullable @Override public IBinder onBind(Intent i) { return null; }
}
