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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.xuanyin.app.R;
import com.xuanyin.app.model.Song;
import com.xuanyin.app.util.Anim;
import com.xuanyin.app.util.Haptic;
import com.xuanyin.app.util.IslandAdaptiveHelper;
import com.xuanyin.app.util.Prefs;
import com.xuanyin.app.util.WallpaperHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IslandService extends Service {
    private static final String CH = "island_ch";
    private static final int NID = 1001;
    private static final long OPEN_DURATION = 1100L;
    private static final long CLOSE_DURATION = 950L;
    private static final long IDLE_TIMEOUT = 20000L;
    private static final Interpolator EASE_OPEN = new PathInterpolator(0.16f, 1f, 0.3f, 1f);
    private static final Interpolator EASE_CLOSE = new PathInterpolator(0.4f, 0f, 0.6f, 1f);

    // 简单歌词行
    private static class L { long t; String s; L(long t, String s) { this.t = t; this.s = s; } }

    private WindowManager wm;
    private FrameLayout root;
    private View islandMain, notifCard, backdrop, chargingFx;
    private View collapsedView, collapsedLife, collapsedMusic, collapsedDl;
    private TextView tvTimeMini, tvBatteryMini, tvCollapsedTitle, tvDlTitle, tvDlPct;
    private ProgressBar dlBar;
    private ImageView ivCoverMini;
    private TextView tvSongTitleMini, tvLyricMini, tvLyric2Mini;
    private View[] waveBars;
    private View expandedView, lifeGroup, musicGroup;
    private TextView tvTimeBig, tvDate, tvAlarm, tvBatteryBig, tvSongTitle, tvSongArtist;
    private ImageView ivCoverBig;
    private ProgressBar progress;
    private TextView notifTitle, notifText;
    private TextView lyric1, lyric2, lyric3, lyric4, lyric5;

    private WindowManager.LayoutParams params;
    private boolean expandedState = false, musicMode = false, downloadMode = false;
    private boolean charging = false, notifVisible = false, playing = false;
    private Song currentSong;
    private List<L> lyrics;
    private int lastLyricIdx = -1;
    private ValueAnimator sizeAnim;
    private int screenWidth;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private final Runnable waveTick = new Runnable() {
        @Override public void run() {
            if (waveBars != null && playing) {
                float d = getResources().getDisplayMetrics().density;
                for (int i = 0; i < waveBars.length; i++) {
                    if (waveBars[i] == null) continue;
                    float h = 6f + (float)(Math.random() * 14f);
                    ViewGroup.LayoutParams lp = waveBars[i].getLayoutParams();
                    if (lp != null) {
                        lp.height = (int)(h * d);
                        waveBars[i].setLayoutParams(lp);
                    }
                }
            }
            ui.postDelayed(this, 180);
        }
    };

    private final Runnable idleSwitch = new Runnable() {
        @Override public void run() {
            try {
                if (!musicMode) return;
                musicMode = false;
                setModeView();
            } catch (Throwable ignored) {}
        }
    };

    private final Runnable autoCol = new Runnable() {
        @Override public void run() { if (expandedState) toggle(); }
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
                    String lrc = i.getStringExtra("lrc");
                    parseLyrics(lrc);
                    lastLyricIdx = -1;
                } else if ("com.xuanyin.app.PLAY_STATE".equals(a)) {
                    boolean p = i.getBooleanExtra("playing", false);
                    playing = p;
                    View bp = root == null ? null : root.findViewById(R.id.btn_play);
                    if (bp instanceof TextView) ((TextView) bp).setText(p ? "⏸" : "▶");
                    if (p) {
                        musicMode = true;
                        setModeView();
                        ui.removeCallbacks(idleSwitch);
                        ui.removeCallbacks(waveTick);
                        ui.post(waveTick);
                    } else {
                        ui.removeCallbacks(waveTick);
                        ui.removeCallbacks(idleSwitch);
                        ui.postDelayed(idleSwitch, IDLE_TIMEOUT);
                    }
                } else if ("com.xuanyin.app.NOTIFY".equals(a)) {
                    showNotify(i.getStringExtra("title"), i.getStringExtra("text"));
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

    // 极简歌词解析
    private void parseLyrics(String lrc) {
        lyrics = new ArrayList<L>();
        if (lrc == null || lrc.isEmpty()) return;
        try {
            String[] lines = lrc.split("\n");
            for (String raw : lines) {
                if (raw == null) continue;
                String line = raw.trim();
                if (!line.startsWith("[")) continue;
                int close = line.indexOf(']');
                if (close <= 1) continue;
                String stamp = line.substring(1, close);
                String text = line.substring(close + 1).trim();
                if (text.isEmpty()) continue;
                int colon = stamp.indexOf(':');
                if (colon <= 0) continue;
                try {
                    int min = Integer.parseInt(stamp.substring(0, colon).trim());
                    String secStr = stamp.substring(colon + 1).trim();
                    int dot = secStr.indexOf('.');
                    float sec;
                    if (dot > 0) sec = Float.parseFloat(secStr.substring(0, dot)) + Float.parseFloat("0" + secStr.substring(dot)) ;
                    else sec = Float.parseFloat(secStr);
                    long ms = (long)((min * 60 + sec) * 1000);
                    lyrics.add(new L(ms, text));
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private int findLyricIdx(long pos) {
        if (lyrics == null || lyrics.isEmpty()) return -1;
        int idx = -1;
        for (int i = 0; i < lyrics.size(); i++) {
            if (lyrics.get(i).t <= pos) idx = i;
            else break;
        }
        return idx;
    }

    private void onBattery(Intent i) {
        try {
            int lv = i.getIntExtra("level", 0);
            int sc = i.getIntExtra("scale", 100);
            int pct = sc > 0 ? (int)(100f * lv / sc) : lv;
            int st = i.getIntExtra("status", 0);
            boolean isCharging = st == 2 || st == 5;
            if (isCharging != charging) {
                charging = isCharging;
                if (charging && islandMain != null) {
                    Haptic.charge(this);
                    islandMain.animate().cancel();
                    islandMain.animate().scaleX(1.06f).scaleY(1.06f).setDuration(180)
                        .withEndAction(new Runnable() {
                            @Override public void run() {
                                if (islandMain != null) islandMain.animate()
                                    .scaleX(1f).scaleY(1f).setDuration(380)
                                    .setInterpolator(EASE_CLOSE).start();
                            }
                        }).start();
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
                    ch.setShowBadge(false);
                    nm.createNotificationChannel(ch);
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
            islandMain = root.findViewById(R.id.island_main);
            notifCard = root.findViewById(R.id.island_notif_card);
            backdrop = root.findViewById(R.id.island_backdrop);
            chargingFx = root.findViewById(R.id.charging_effect);

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

            lyric1 = (TextView) root.findViewById(R.id.lyric_line1);
            lyric2 = (TextView) root.findViewById(R.id.lyric_line2);
            lyric3 = (TextView) root.findViewById(R.id.lyric_line3);
            lyric4 = (TextView) root.findViewById(R.id.lyric_line4);
            lyric5 = (TextView) root.findViewById(R.id.lyric_line5);

            waveBars = new View[]{
                root.findViewById(R.id.wave1),
                root.findViewById(R.id.wave2),
                root.findViewById(R.id.wave3),
                root.findViewById(R.id.wave4),
                root.findViewById(R.id.wave5)
            };

            View bp = root.findViewById(R.id.btn_prev);
            View bpl = root.findViewById(R.id.btn_play);
            View bn = root.findViewById(R.id.btn_next);
            if (bp != null) bp.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { Haptic.tap(v); Anim.pressLight(v); ctrl("prev"); }
            });
            if (bpl != null) bpl.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { Haptic.tap(v); Anim.pressLight(v); ctrl("toggle"); }
            });
            if (bn != null) bn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { Haptic.tap(v); Anim.pressLight(v); ctrl("next"); }
            });

            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
            params = new WindowManager.LayoutParams(screenWidth, WindowManager.LayoutParams.WRAP_CONTENT, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.y = IslandAdaptiveHelper.getSafeTop(this) + IslandAdaptiveHelper.dpToPx(this, 8);
            wm.addView(root, params);
            root.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { toggle(); }
            });
            setModeView();
            applyMainSize(false, false, OPEN_DURATION, EASE_OPEN);
        } catch (Throwable t) { stopSelf(); }
    }

    private void setModeView() {
        try {
            if (collapsedLife == null) return;
            boolean dl = downloadMode;
            boolean mu = musicMode && !dl;
            boolean lf = !mu && !dl;
            collapsedLife.setVisibility(lf ? View.VISIBLE : View.GONE);
            if (collapsedMusic != null) collapsedMusic.setVisibility(mu ? View.VISIBLE : View.GONE);
            if (collapsedDl != null) collapsedDl.setVisibility(dl ? View.VISIBLE : View.GONE);
            if (lifeGroup != null) lifeGroup.setVisibility(mu || dl ? View.GONE : View.VISIBLE);
            if (musicGroup != null) musicGroup.setVisibility(mu ? View.VISIBLE : View.GONE);
        } catch (Throwable ignored) {}
    }

    private void toggle() {
        try {
            if (notifVisible) { hideNotifyCard(); return; }
            expandedState = !expandedState;
            if (expandedState) {
                Haptic.expand(this);
                if (expandedView != null) {
                    expandedView.setVisibility(View.VISIBLE);
                    expandedView.setAlpha(0f);
                    expandedView.setScaleX(0.86f);
                    expandedView.setScaleY(0.86f);
                    expandedView.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(OPEN_DURATION).setInterpolator(EASE_OPEN).start();
                }
                applyMainSize(true, true, OPEN_DURATION, EASE_OPEN);
                if (collapsedView != null) {
                    collapsedView.animate().alpha(0f).setDuration(OPEN_DURATION / 3)
                        .setInterpolator(EASE_OPEN).withEndAction(new Runnable() {
                            @Override public void run() {
                                if (collapsedView != null) {
                                    collapsedView.setVisibility(View.GONE);
                                    collapsedView.setAlpha(1f);
                                }
                            }
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
                applyMainSize(false, true, CLOSE_DURATION, EASE_CLOSE);
                if (expandedView != null) {
                    expandedView.animate().alpha(0f).scaleX(0.86f).scaleY(0.86f)
                        .setDuration(CLOSE_DURATION / 2).setInterpolator(EASE_CLOSE)
                        .withEndAction(new Runnable() {
                            @Override public void run() {
                                if (expandedView != null) {
                                    expandedView.setVisibility(View.GONE);
                                    expandedView.setAlpha(1f);
                                    expandedView.setScaleX(1f);
                                    expandedView.setScaleY(1f);
                                }
                            }
                        }).start();
                }
                ui.removeCallbacks(autoCol);
            }
        } catch (Throwable ignored) {}
    }

    private void applyMainSize(final boolean exp, boolean animate) {
        applyMainSize(exp, animate, exp ? OPEN_DURATION : CLOSE_DURATION, exp ? EASE_OPEN : EASE_CLOSE);
    }
    private void applyMainSize(final boolean exp, boolean animate, long duration, Interpolator interp) {
        if (islandMain == null) return;
        final int tw = exp ? (int)(screenWidth * 0.94f) : (int)(screenWidth * 0.62f);
        final int th = exp ? IslandAdaptiveHelper.getExpandedHeight(this) : IslandAdaptiveHelper.getCollapsedHeight(this);
        if (sizeAnim != null && sizeAnim.isRunning()) sizeAnim.cancel();
        if (!animate) { setMainWidth(tw); setMainHeight(th); updateRadius(exp, th); return; }
        final int sw = islandMain.getWidth() > 0 ? islandMain.getWidth() : (int)(screenWidth * 0.62f);
        final int sh = islandMain.getHeight() > 0 ? islandMain.getHeight() : IslandAdaptiveHelper.getCollapsedHeight(this);
        sizeAnim = ValueAnimator.ofFloat(0f, 1f);
        sizeAnim.setDuration(duration);
        sizeAnim.setInterpolator(interp);
        sizeAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                float p = (float) a.getAnimatedValue();
                int w = (int)(sw + (tw - sw) * p);
                int h = (int)(sh + (th - sh) * p);
                setMainWidth(w); setMainHeight(h); updateRadius(exp, h);
            }
        });
        sizeAnim.start();
    }
    private void setMainWidth(int w) {
        if (islandMain == null) return;
        ViewGroup.LayoutParams lp = islandMain.getLayoutParams();
        if (lp.width != w) { lp.width = w; islandMain.setLayoutParams(lp); }
    }
    private void setMainHeight(int h) {
        if (islandMain == null) return;
        ViewGroup.LayoutParams lp = islandMain.getLayoutParams();
        if (lp.height != h) { lp.height = h; islandMain.setLayoutParams(lp); }
    }
    private void updateRadius(final boolean exp, int h) {
        final float rad = exp ? IslandAdaptiveHelper.dpToPx(this, 44f) : h / 2f;
        if (islandMain == null) return;
        islandMain.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), rad);
            }
        });
        islandMain.setClipToOutline(true);
    }

    private void showNotify(String title, String text) {
        try {
            if (notifCard == null || islandMain == null) return;
            if (notifTitle != null) notifTitle.setText(title == null ? "通知" : title);
            if (notifText != null) notifText.setText(text == null ? "" : text);
            if (notifVisible) {
                ui.removeCallbacks(hideNotif);
                ui.postDelayed(hideNotif, 5200);
                return;
            }
            notifVisible = true;
            int currentMainW = islandMain.getWidth() > 0 ? islandMain.getWidth() : (int)(screenWidth * 0.62f);
            int currentMainH = islandMain.getHeight() > 0 ? islandMain.getHeight() : IslandAdaptiveHelper.getCollapsedHeight(this);
            final int targetMainW = (int)(screenWidth * 0.60f);
            final int targetCardW = (int)(screenWidth * 0.36f);
            notifCard.setVisibility(View.VISIBLE);
            notifCard.setAlpha(0f);
            notifCard.setTranslationX(60f);
            ViewGroup.LayoutParams clp = notifCard.getLayoutParams();
            clp.width = 0;
            clp.height = currentMainH;
            notifCard.setLayoutParams(clp);
            final int startMainW = currentMainW;
            final int startMainH = currentMainH;
            ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(620);
            anim.setInterpolator(EASE_OPEN);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator a) {
                    float p = (float) a.getAnimatedValue();
                    setMainWidth((int)(startMainW + (targetMainW - startMainW) * p));
                    if (notifCard == null) return;
                    ViewGroup.LayoutParams lp = notifCard.getLayoutParams();
                    lp.width = (int)(targetCardW * p);
                    lp.height = startMainH;
                    notifCard.setLayoutParams(lp);
                    notifCard.setAlpha(p);
                    notifCard.setTranslationX((1f - p) * 60f);
                }
            });
            anim.start();
            Haptic.mid(this);
            ui.removeCallbacks(hideNotif);
            ui.postDelayed(hideNotif, 5200);
        } catch (Throwable ignored) {}
    }
    private final Runnable hideNotif = new Runnable() {
        @Override public void run() { hideNotifyCard(); }
    };
    private void hideNotifyCard() {
        try {
            if (notifCard == null || !notifVisible) return;
            int currentMainW = islandMain.getWidth();
            int targetMainW = expandedState ? (int)(screenWidth * 0.94f) : (int)(screenWidth * 0.62f);
            final int startMainW = currentMainW;
            final int startCardW = notifCard.getWidth();
            ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(480);
            anim.setInterpolator(EASE_CLOSE);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator a) {
                    float p = (float) a.getAnimatedValue();
                    setMainWidth((int)(startMainW + (targetMainW - startMainW) * p));
                    if (notifCard == null) return;
                    ViewGroup.LayoutParams lp = notifCard.getLayoutParams();
                    lp.width = (int)(startCardW * (1f - p));
                    notifCard.setLayoutParams(lp);
                    notifCard.setAlpha(1f - p);
                    notifCard.setTranslationX(p * 60f);
                }
            });
            anim.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    if (notifCard != null) notifCard.setVisibility(View.GONE);
                    notifVisible = false;
                }
            });
            anim.start();
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
            lyrics = null;
            lastLyricIdx = -1;
            ui.removeCallbacks(idleSwitch);
        } catch (Throwable ignored) {}
    }
    private void onDlStart(String t) {
        downloadMode = true; setModeView();
        if (tvDlTitle != null) tvDlTitle.setText(t == null ? "下载中" : t);
        if (tvDlPct != null) tvDlPct.setText("0%");
        if (dlBar != null) dlBar.setProgress(0);
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
        ui.postDelayed(new Runnable() {
            @Override public void run() { downloadMode = false; setModeView(); }
        }, 2000);
    }
    private void onDlErr() {
        if (tvDlPct != null) tvDlPct.setText("失败");
        ui.postDelayed(new Runnable() {
            @Override public void run() { downloadMode = false; setModeView(); }
        }, 1600);
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
    }

    private void tickLyrics() {
        try {
            if (!musicMode || lyrics == null || lyrics.isEmpty()) return;
            long pos = 0;
            try {
                androidx.media3.exoplayer.ExoPlayer ep = MusicService.getPlayer();
                if (ep != null) pos = ep.getCurrentPosition();
            } catch (Throwable ignored) {}
            int idx = findLyricIdx(pos);
            if (idx == lastLyricIdx) return;
            lastLyricIdx = idx;
            if (idx < 0) return;
            L cur = lyrics.get(idx);
            if (tvLyricMini != null) tvLyricMini.setText(cur.s);
            if (tvLyric2Mini != null) {
                if (idx + 1 < lyrics.size()) tvLyric2Mini.setText(lyrics.get(idx + 1).s);
                else tvLyric2Mini.setText("");
            }
            TextView[] lines = new TextView[]{lyric1, lyric2, lyric3, lyric4, lyric5};
            for (int k = -2; k <= 2; k++) {
                int j = idx + k;
                TextView v = lines[k + 2];
                if (v == null) continue;
                if (j >= 0 && j < lyrics.size()) {
                    v.setText(lyrics.get(j).s);
                    v.setAlpha(k == 0 ? 1f : (k == -1 || k == 1 ? 0.55f : 0.3f));
                    v.setTextSize(k == 0 ? 16f : 14f);
                } else {
                    v.setText("");
                }
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
                    if (tvCollapsedTitle != null && !musicMode && !downloadMode && !notifVisible)
                        tvCollapsedTitle.setText(df.format(n));
                    if (tvAlarm != null) {
                        int h = Prefs.getInt("alarm_hour", -1);
                        int m = Prefs.getInt("alarm_min", -1);
                        tvAlarm.setText((h < 0 || m < 0) ? "未设置闹钟" : String.format("闹钟 %02d:%02d", h, m));
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
        if (params != null) params.width = screenWidth;
        applyMainSize(expandedState, true);
    }
    @Override public int onStartCommand(Intent i, int f, int s) {
        if (i == null) return START_STICKY;
        String a = i.getAction();
        if ("TEST_NOTIFY".equals(a)) showNotify("测试通知", "灵动岛右侧分裂效果演示");
        else if ("REFRESH_SIZE".equals(a)) applyMainSize(expandedState, true);
        return START_STICKY;
    }
    @Override public void onDestroy() {
        super.onDestroy();
        if (sizeAnim != null) sizeAnim.cancel();
        ui.removeCallbacks(idleSwitch);
        ui.removeCallbacks(waveTick);
        try { unregisterReceiver(rx); } catch (Throwable ignored) {}
        if (root != null && wm != null) {
            try { wm.removeView(root); } catch (Throwable ignored) {}
            root = null;
        }
    }
    @Nullable @Override public IBinder onBind(Intent i) { return null; }
}
