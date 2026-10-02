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
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.xuanyin.app.R;
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
    private static final long ANIM_MS = 500L;
    private static final long IDLE_TIMEOUT = 20000L;
    private static final float FOLD_W = 0.62f;
    private static final float EXP_W = 0.94f;
    private static final float SPLIT_W = 0.98f;
    private static final Interpolator EASE = new PathInterpolator(0.22f, 1f, 0.36f, 1f);
    private WindowManager wm;
    private FrameLayout root;
    private FrameLayout islandMain;
    private FrameLayout notifCard;
    private IslandBackdropView backdrop;
    private ChargingEffectView chargingFx;
    private View collapsedView, collapsedLife, collapsedMusic, collapsedDl;
    private TextView tvTimeMini, tvBatteryMini, tvCollapsedTitle, tvDlTitle, tvDlPct;
    private ProgressBar dlBar;
    private ImageView ivCoverMini;
    private TextView tvSongTitleMini, tvLyricMini, tvLyric2Mini;
    private View expandedView, lifeGroup, musicGroup;
    private TextView tvTimeBig, tvDate, tvAlarm, tvBatteryBig, tvSongTitle, tvSongArtist;
    private ImageView ivCoverBig;
    private ProgressBar progress;
    private TextView notifTitle, notifText;
    private TextView ly1, ly2, ly3, ly4, ly5;
    private WindowManager.LayoutParams params;
    private boolean expanded = false, musicMode = false, downloadMode = false;
    private boolean charging = false, notifVisible = false, playing = false;
    private Song currentSong;
    private List<Lyr> lyrics = new ArrayList<Lyr>();
    private int lastLyricIdx = -1;
    private ValueAnimator sizeAnim, windowAnim;
    private int screenWidth;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private static class Lyr { long t; String s; Lyr(long t, String s) { this.t = t; this.s = s; } }
    private final Runnable idleSwitch = new Runnable() {
        @Override public void run() { try { if (!musicMode) return; musicMode = false; setModeView(); } catch (Throwable ignored) {} }
    };
    private final Runnable autoCol = new Runnable() {
        @Override public void run() { if (expanded) doToggle(); }
    };
    private final BroadcastReceiver rx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction(); if (a == null) return;
            try {
                if ("com.xuanyin.app.SONG_CHANGED".equals(a)) { Song s = (Song) i.getSerializableExtra("song"); if (s != null) onSong(s); }
                else if ("com.xuanyin.app.LYRICS".equals(a)) parseLyrics(i.getStringExtra("lrc"));
                else if ("com.xuanyin.app.PLAY_STATE".equals(a)) {
                    boolean p = i.getBooleanExtra("playing", false); playing = p;
                    View bp = root == null ? null : root.findViewById(R.id.btn_play);
                    if (bp instanceof TextView) ((TextView) bp).setText(p ? "⏸" : "▶");
                    if (p) { musicMode = true; setModeView(); ui.removeCallbacks(idleSwitch); }
                    else { ui.removeCallbacks(idleSwitch); ui.postDelayed(idleSwitch, IDLE_TIMEOUT); }
                }
                else if ("com.xuanyin.app.NOTIFY".equals(a)) showNotify(i.getStringExtra("title"), i.getStringExtra("text"));
                else if ("com.xuanyin.app.ALARM".equals(a)) showAlarm();
                else if ("com.xuanyin.app.DL_START".equals(a)) onDlStart(i.getStringExtra("title"));
                else if ("com.xuanyin.app.DL_PROGRESS".equals(a)) onDlProg(i.getStringExtra("title"), i.getIntExtra("percent", 0));
                else if ("com.xuanyin.app.DL_DONE".equals(a)) onDlDone();
                else if ("com.xuanyin.app.DL_ERROR".equals(a)) onDlErr();
                else if (Intent.ACTION_BATTERY_CHANGED.equals(a)) onBattery(i);
            } catch (Throwable ignored) {}
        }
    };
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
                    long ms = (long)((min * 60 + sec) * 1000);
                    lyrics.add(new Lyr(ms, text));
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
            int lv = i.getIntExtra("level", 0); int sc = i.getIntExtra("scale", 100);
            int pct = sc > 0 ? (int)(100f * lv / sc) : lv;
            int st = i.getIntExtra("status", 0);
            boolean isCharging = st == 2 || st == 5;
            if (isCharging != charging) {
                charging = isCharging;
                if (chargingFx != null) chargingFx.setActive(isCharging);
                if (backdrop != null) { backdrop.setChargingBreath(isCharging); backdrop.setMode(isCharging ? 2 : (musicMode ? 1 : 0)); }
                if (charging) {
                    Haptic.charge(this);
                    if (islandMain != null) {
                        islandMain.animate().cancel();
                        islandMain.animate().scaleX(1.08f).scaleY(1.08f).setDuration(160)
                            .withEndAction(new Runnable() { @Override public void run() { if (islandMain != null) islandMain.animate().scaleX(1f).scaleY(1f).setDuration(380).setInterpolator(EASE).start(); } }).start();
                    }
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
        startFg(); initWin(); startClock();
        IntentFilter f = new IntentFilter();
        f.addAction("com.xuanyin.app.SONG_CHANGED");
        f.addAction("com.xuanyin.app.LYRICS");
        f.addAction("com.xuanyin.app.PLAY_STATE");
        f.addAction("com.xuanyin.app.NOTIFY");
        f.addAction("com.xuanyin.app.ALARM");
        f.addAction("com.xuanyin.app.DL_START");
        f.addAction("com.xuanyin.app.DL_PROGRESS");
        f.addAction("com.xuanyin.app.DL_DONE");
        f.addAction("com.xuanyin.app.DL_ERROR");
        f.addAction(Intent.ACTION_BATTERY_CHANGED);
        try { if (Build.VERSION.SDK_INT >= 33) registerReceiver(rx, f, Context.RECEIVER_NOT_EXPORTED); else registerReceiver(rx, f); } catch (Throwable ignored) {}
    }
    private void startFg() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm != null && nm.getNotificationChannel(CH) == null) { NotificationChannel ch = new NotificationChannel(CH, "玄音·灵动岛", NotificationManager.IMPORTANCE_LOW); ch.setShowBadge(false); nm.createNotificationChannel(ch); }
            }
            Notification n = new NotificationCompat.Builder(this, CH).setContentTitle("玄音").setContentText("灵动岛运行中").setSmallIcon(R.drawable.ic_launcher).setOngoing(true).build();
            startForeground(NID, n);
        } catch (Throwable ignored) {}
    }
    private void initWin() {
        try {
            wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            root = (FrameLayout) LayoutInflater.from(this).inflate(R.layout.island_root, null);
            islandMain = (FrameLayout) root.findViewById(R.id.island_main);
            notifCard = (FrameLayout) root.findViewById(R.id.island_notif_card);
            backdrop = (IslandBackdropView) root.findViewById(R.id.island_backdrop);
            try { chargingFx = (ChargingEffectView) root.findViewById(R.id.charging_effect); } catch (Throwable ignored) {}
            collapsedView = root.findViewById(R.id.island_collapsed);
            collapsedLife = root.findViewById(R.id.group_life_mini);
            collapsedMusic = root.findViewById(R.id.group_music_mini);
            collapsedDl = root.findViewById(R.id.group_download_mini);
            tvTimeMini = (TextView) root.findViewById(R.id.tv_time_mini);
            tvBatteryMini = (TextView) root.findViewById(R.id.tv_battery_mini);
            tvCollapsedTitle = (TextView) root.findViewById(R.id.tv_collapsed_title);
            tvDlTitle = (TextView) root.findViewById(R.id.tv_dl_title_mini);
            tvDlPct = (TextView) root.findViewById(R.id.tv_dl_pct_mini);
            dlBar = (ProgressBar) root.findViewById(R.id.dl_bar_mini);
            ivCoverMini = (ImageView) root.findViewById(R.id.iv_cover_mini);
            tvSongTitleMini = (TextView) root.findViewById(R.id.tv_song_title_mini);
            tvLyricMini = (TextView) root.findViewById(R.id.tv_lyric_mini);
            tvLyric2Mini = (TextView) root.findViewById(R.id.tv_lyric2_mini);
            expandedView = root.findViewById(R.id.island_expanded);
            lifeGroup = root.findViewById(R.id.group_life);
            musicGroup = root.findViewById(R.id.group_music);
            tvTimeBig = (TextView) root.findViewById(R.id.tv_time_big);
            tvDate = (TextView) root.findViewById(R.id.tv_date);
            tvAlarm = (TextView) root.findViewById(R.id.tv_alarm);
            tvBatteryBig = (TextView) root.findViewById(R.id.tv_battery_big);
            tvSongTitle = (TextView) root.findViewById(R.id.tv_song_title);
            tvSongArtist = (TextView) root.findViewById(R.id.tv_song_artist);
            ivCoverBig = (ImageView) root.findViewById(R.id.iv_cover_big);
            progress = (ProgressBar) root.findViewById(R.id.progress);
            notifTitle = (TextView) root.findViewById(R.id.notif_title);
            notifText = (TextView) root.findViewById(R.id.notif_text);
            ly1 = (TextView) root.findViewById(R.id.lyric_line1);
            ly2 = (TextView) root.findViewById(R.id.lyric_line2);
            ly3 = (TextView) root.findViewById(R.id.lyric_line3);
            ly4 = (TextView) root.findViewById(R.id.lyric_line4);
            ly5 = (TextView) root.findViewById(R.id.lyric_line5);
            View bp = root.findViewById(R.id.btn_prev);
            View bpl = root.findViewById(R.id.btn_play);
            View bn = root.findViewById(R.id.btn_next);
            if (bp != null) bp.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { Haptic.tap(v); Anim.pressLight(v); ctrl("prev"); } });
            if (bpl != null) bpl.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { Haptic.tap(v); Anim.pressLight(v); ctrl("toggle"); } });
            if (bn != null) bn.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { Haptic.tap(v); Anim.pressLight(v); ctrl("next"); } });
            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
            int initW = (int)(screenWidth * FOLD_W);
            int initH = dp(44);
            params = new WindowManager.LayoutParams(initW, initH, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = (screenWidth - initW) / 2;
            params.y = getSafeTop() + dp(8);
            wm.addView(root, params);
            islandMain.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { doToggle(); } });
            setModeView();
            layoutMain(false, false);
        } catch (Throwable t) { stopSelf(); }
    }
    private int getSafeTop() {
        try { int id = getResources().getIdentifier("status_bar_height", "dimen", "android"); if (id > 0) return getResources().getDimensionPixelSize(id); } catch (Throwable ignored) {}
        return dp(24);
    }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private void setModeView() {
        try {
            if (collapsedLife == null) return;
            boolean dl = downloadMode; boolean mu = musicMode && !dl; boolean lf = !mu && !dl;
            collapsedLife.setVisibility(lf ? View.VISIBLE : View.GONE);
            if (collapsedMusic != null) collapsedMusic.setVisibility(mu ? View.VISIBLE : View.GONE);
            if (collapsedDl != null) collapsedDl.setVisibility(dl ? View.VISIBLE : View.GONE);
            if (lifeGroup != null) lifeGroup.setVisibility(mu || dl ? View.GONE : View.VISIBLE);
            if (musicGroup != null) musicGroup.setVisibility(mu ? View.VISIBLE : View.GONE);
        } catch (Throwable ignored) {}
    }
    private void setWindowWidth(int w, boolean animate) {
        try {
            final int startW = params.width > 0 ? params.width : (int)(screenWidth * FOLD_W);
            final int startX = params.x;
            final int endX = (screenWidth - w) / 2;
            if (windowAnim != null && windowAnim.isRunning()) windowAnim.cancel();
            if (!animate) { params.width = w; params.x = endX; try { wm.updateViewLayout(root, params); } catch (Throwable ignored) {} return; }
            windowAnim = ValueAnimator.ofFloat(0f, 1f);
            windowAnim.setDuration(ANIM_MS); windowAnim.setInterpolator(EASE);
            windowAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator a) {
                    float p = (float) a.getAnimatedValue();
                    int cw = (int)(startW + (w - startW) * p);
                    int cx = (int)(startX + (endX - startX) * p);
                    params.width = cw; params.x = cx;
                    try { wm.updateViewLayout(root, params); } catch (Throwable ignored) {}
                }
            });
            windowAnim.start();
        } catch (Throwable ignored) {}
    }
    private void setMainSize(int w, int h) {
        try { ViewGroup.LayoutParams lp = islandMain.getLayoutParams(); lp.width = w; lp.height = h; islandMain.setLayoutParams(lp); } catch (Throwable ignored) {}
    }
    private void doToggle() {
        try {
            if (notifVisible) { hideNotif(); return; }
            expanded = !expanded;
            if (expanded) {
                Haptic.expand(this);
                if (expandedView != null) {
                    expandedView.setVisibility(View.VISIBLE); expandedView.setAlpha(0f); expandedView.setScaleX(0.90f); expandedView.setScaleY(0.90f);
                    expandedView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(ANIM_MS).setInterpolator(EASE).start();
                }
                setWindowWidth((int)(screenWidth * EXP_W), true);
                setMainSize((int)(screenWidth * EXP_W), dp(260));
                layoutMain(true, true);
                if (collapsedView != null) {
                    collapsedView.animate().alpha(0f).setDuration(ANIM_MS / 3).setInterpolator(EASE).withEndAction(new Runnable() {
                        @Override public void run() { if (collapsedView != null) { collapsedView.setVisibility(View.GONE); collapsedView.setAlpha(1f); } }
                    }).start();
                }
                ui.removeCallbacks(autoCol);
                ui.postDelayed(autoCol, 7000);
            } else {
                Haptic.collapse(this);
                if (collapsedView != null) {
                    collapsedView.setVisibility(View.VISIBLE); collapsedView.setAlpha(0f);
                    collapsedView.animate().alpha(1f).setDuration(ANIM_MS).setInterpolator(EASE).start();
                }
                setWindowWidth((int)(screenWidth * FOLD_W), true);
                setMainSize((int)(screenWidth * FOLD_W), dp(44));
                layoutMain(false, true);
                if (expandedView != null) {
                    expandedView.animate().alpha(0f).scaleX(0.90f).scaleY(0.90f).setDuration(ANIM_MS / 2).setInterpolator(EASE)
                        .withEndAction(new Runnable() { @Override public void run() { if (expandedView != null) { expandedView.setVisibility(View.GONE); expandedView.setAlpha(1f); expandedView.setScaleX(1f); expandedView.setScaleY(1f); } } }).start();
                }
                ui.removeCallbacks(autoCol);
            }
        } catch (Throwable ignored) {}
    }
    private void layoutMain(final boolean exp, boolean animate) {
        try {
            if (islandMain == null) return;
            final int targetW = exp ? (int)(screenWidth * EXP_W) : (int)(screenWidth * FOLD_W);
            final int targetH = exp ? dp(260) : dp(44);
            final int startW = islandMain.getWidth() > 0 ? islandMain.getWidth() : (int)(screenWidth * FOLD_W);
            final int startH = islandMain.getHeight() > 0 ? islandMain.getHeight() : dp(44);
            if (sizeAnim != null && sizeAnim.isRunning()) sizeAnim.cancel();
            if (!animate) { setMainSize(targetW, targetH); updateRadius(exp, targetH); return; }
            sizeAnim = ValueAnimator.ofFloat(0f, 1f);
            sizeAnim.setDuration(ANIM_MS); sizeAnim.setInterpolator(EASE);
            sizeAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator a) {
                    float p = (float) a.getAnimatedValue();
                    int w = (int)(startW + (targetW - startW) * p);
                    int h = (int)(startH + (targetH - startH) * p);
                    setMainSize(w, h); updateRadius(exp, h);
                }
            });
            sizeAnim.start();
        } catch (Throwable ignored) {}
    }
    private void updateRadius(boolean exp, int h) {
        try {
            if (islandMain == null) return;
            final float r = exp ? dp(28) : Math.min(dp(22), h / 2f);
            islandMain.setOutlineProvider(new ViewOutlineProvider() { @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r); } });
            islandMain.setClipToOutline(true);
            if (backdrop != null) backdrop.setCornerRadius(r);
        } catch (Throwable ignored) {}
    }
    private void showNotify(String title, String text) {
        try {
            if (notifCard == null || islandMain == null) return;
            if (notifTitle != null) notifTitle.setText(title == null ? "通知" : title);
            if (notifText != null) notifText.setText(text == null ? "" : text);
            if (backdrop != null) backdrop.setMode(3);
            if (notifVisible) { ui.removeCallbacks(hideNotifRunnable); ui.postDelayed(hideNotifRunnable, 5000); return; }
            notifVisible = true;
            Haptic.notify(this);
            final int totalW = (int)(screenWidth * SPLIT_W);
            final int mainW = (int)(screenWidth * FOLD_W);
            final int cardW = totalW - mainW;
            final int mainH = dp(44);
            setWindowWidth(totalW, true);
            islandMain.setX(0);
            setMainSize(mainW, mainH);
            updateRadius(false, mainH);
            notifCard.setVisibility(View.VISIBLE);
            notifCard.setAlpha(0f);
            ViewGroup.LayoutParams clp = notifCard.getLayoutParams();
            clp.width = cardW; clp.height = mainH;
            notifCard.setLayoutParams(clp);
            notifCard.setX(mainW);
            notifCard.setTranslationX(cardW);
            notifCard.animate().alpha(1f).translationX(0).setDuration(ANIM_MS).setInterpolator(EASE).start();
            ui.removeCallbacks(hideNotifRunnable);
            ui.postDelayed(hideNotifRunnable, 5000);
        } catch (Throwable ignored) {}
    }
    private final Runnable hideNotifRunnable = new Runnable() { @Override public void run() { hideNotif(); } };
    private void hideNotif() {
        try {
            if (notifCard == null || !notifVisible) return;
            notifVisible = false;
            final int totalW = (int)(screenWidth * SPLIT_W);
            final int mainW = (int)(screenWidth * FOLD_W);
            final int cardW = totalW - mainW;
            final int backW = expanded ? (int)(screenWidth * EXP_W) : (int)(screenWidth * FOLD_W);
            notifCard.animate().alpha(0f).translationX(cardW).setDuration(ANIM_MS / 2).setInterpolator(EASE)
                .withEndAction(new Runnable() { @Override public void run() {
                    if (notifCard != null) notifCard.setVisibility(View.GONE);
                    setWindowWidth(backW, true);
                    int w = expanded ? (int)(screenWidth * EXP_W) : (int)(screenWidth * FOLD_W);
                    int h = expanded ? dp(260) : dp(44);
                    setMainSize(w, h); islandMain.setX(0); updateRadius(expanded, h);
                    if (backdrop != null) backdrop.setMode(charging ? 2 : (musicMode ? 1 : 0));
                } }).start();
        } catch (Throwable ignored) {}
    }
    private void onSong(Song s) {
        try {
            currentSong = s; musicMode = true; downloadMode = false;
            setModeView();
            if (tvCollapsedTitle != null) tvCollapsedTitle.setText(s.title);
            if (tvSongTitle != null) tvSongTitle.setText(s.title);
            if (tvSongArtist != null) tvSongArtist.setText(s.displayArtist());
            if (tvSongTitleMini != null) tvSongTitleMini.setText(s.title);
            if (ivCoverMini != null) WallpaperHelper.loadCover(this, ivCoverMini, s.id);
            if (ivCoverBig != null) WallpaperHelper.loadCover(this, ivCoverBig, s.id);
            lyrics = new ArrayList<Lyr>(); lastLyricIdx = -1;
            ui.removeCallbacks(idleSwitch);
            if (backdrop != null) backdrop.setMode(charging ? 2 : 1);
        } catch (Throwable ignored) {}
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
        ui.postDelayed(new Runnable() { @Override public void run() { downloadMode = false; setModeView(); if (backdrop != null) backdrop.setMode(charging ? 2 : (musicMode ? 1 : 0)); } }, 2000);
    }
    private void onDlErr() {
        if (tvDlPct != null) tvDlPct.setText("失败");
        ui.postDelayed(new Runnable() { @Override public void run() { downloadMode = false; setModeView(); } }, 1600);
    }
    private void ctrl(String a) {
        Intent i = new Intent(this, MusicService.class);
        if ("prev".equals(a)) i.setAction(MusicService.ACTION_PREV);
        else if ("next".equals(a)) i.setAction(MusicService.ACTION_NEXT);
        else i.setAction(MusicService.ACTION_TOGGLE);
        try { startService(i); } catch (Throwable ignored) {}
    }
    private void showAlarm() {
        if (tvCollapsedTitle != null) tvCollapsedTitle.setText("⏰ 闹钟");
        if (backdrop != null) backdrop.setMode(2);
        ui.postDelayed(new Runnable() { @Override public void run() { if (backdrop != null) backdrop.setMode(charging ? 2 : (musicMode ? 1 : 0)); } }, 5000);
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
            TextView[] lineViews = new TextView[]{ly1, ly2, ly3, ly4, ly5};
            for (int k = -2; k <= 2; k++) {
                int j = idx + k;
                TextView v = lineViews[k + 2];
                if (v == null) continue;
                if (j >= 0 && j < lyrics.size()) {
                    v.setText(lyrics.get(j).s);
                    if (k == 0) { v.setAlpha(1f); v.setTextSize(14f); }
                    else if (k == -1 || k == 1) { v.setAlpha(0.7f); v.setTextSize(12f); }
                    else { v.setAlpha(0.35f); v.setTextSize(12f); }
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
                    if (tvDate != null) tvDate.setText(dfL.format(n));
                    if (tvCollapsedTitle != null && !musicMode && !downloadMode && !notifVisible) tvCollapsedTitle.setText(df.format(n));
                    if (tvAlarm != null) {
                        int h = Prefs.getInt("alarm_hour", -1); int m = Prefs.getInt("alarm_min", -1);
                        tvAlarm.setText((h < 0 || m < 0) ? "未设置闹钟" : String.format(Locale.getDefault(), "闹钟 %02d:%02d", h, m));
                    }
                } catch (Throwable ignored) {}
                tickLyrics();
                ui.postDelayed(this, 200);
            }
        });
    }
    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        screenWidth = getResources().getDisplayMetrics().widthPixels;
        int w = expanded ? (int)(screenWidth * EXP_W) : (int)(screenWidth * FOLD_W);
        setWindowWidth(w, false);
        layoutMain(expanded, false);
    }
    @Override public int onStartCommand(Intent i, int f, int s) {
        if (i == null) return START_STICKY;
        String a = i.getAction();
        if ("TEST_NOTIFY".equals(a)) showNotify("测试通知", "灵动岛右侧分裂效果演示");
        else if ("REFRESH_SIZE".equals(a)) { int w = expanded ? (int)(screenWidth * EXP_W) : (int)(screenWidth * FOLD_W); setWindowWidth(w, true); layoutMain(expanded, true); }
        return START_STICKY;
    }
    @Override public void onDestroy() {
        super.onDestroy();
        if (sizeAnim != null) sizeAnim.cancel();
        if (windowAnim != null) windowAnim.cancel();
        ui.removeCallbacks(idleSwitch);
        try { unregisterReceiver(rx); } catch (Throwable ignored) {}
        if (root != null && wm != null) { try { wm.removeView(root); } catch (Throwable ignored) {} root = null; }
    }
    @Nullable @Override public IBinder onBind(Intent i) { return null; }
}
