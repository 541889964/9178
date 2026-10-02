package com.xuanyin.app.service;
import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.xuanyin.app.R;
import com.xuanyin.app.model.Song;
import com.xuanyin.app.util.IslandAdaptiveHelper;
import com.xuanyin.app.util.Prefs;
import com.xuanyin.app.util.WallpaperHelper;
import com.xuanyin.app.widget.IslandBackdropView;
import com.xuanyin.app.widget.IslandProgressGlowView;
import com.xuanyin.app.widget.IslandPulseView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public class IslandService extends Service {
    private static final String CH = "island_ch";
    private static final int NID = 1001;
    private WindowManager wm;
    private FrameLayout root;
    private View backdrop, pulse, glow, collapsed, expanded;
    private TextView tvTime, tvDate, tvBattery, tvCollapsedTitle;
    private ImageView ivCover;
    private WindowManager.LayoutParams params;
    private boolean expandedState = false, adaptive = true;
    private int collapsedH = 44;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if (a == null) return;
            switch (a) {
                case "com.xuanyin.app.SONG_CHANGED":
                    Song s = (Song) i.getSerializableExtra("song");
                    if (s != null) syncMusic(s, true);
                    break;
                case "com.xuanyin.app.NOTIFY":
                    showNotify(i.getStringExtra("title"));
                    break;
                case "com.xuanyin.app.ALARM":
                    syncBackdrop(IslandBackdropView.MODE_CHARGING, false);
                    break;
                case Intent.ACTION_BATTERY_CHANGED:
                    int st = i.getIntExtra("status", 0);
                    boolean charging = st == 2 || st == 5;
                    int lv = i.getIntExtra("level", 0);
                    int sc = i.getIntExtra("scale", 100);
                    int pct = sc > 0 ? (int)(100f * lv / sc) : lv;
                    if (tvBattery != null) tvBattery.setText(pct + "%");
                    if (charging) syncBackdrop(IslandBackdropView.MODE_CHARGING, true);
                    break;
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
        f.addAction("com.xuanyin.app.NOTIFY");
        f.addAction("com.xuanyin.app.ALARM");
        f.addAction(Intent.ACTION_BATTERY_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver, f);
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
        pulse = root.findViewById(R.id.island_pulse);
        glow = root.findViewById(R.id.island_progress_glow);
        collapsed = root.findViewById(R.id.island_collapsed);
        expanded = root.findViewById(R.id.island_expanded);
        tvTime = root.findViewById(R.id.tv_time);
        tvDate = root.findViewById(R.id.tv_date);
        tvBattery = root.findViewById(R.id.tv_battery);
        tvCollapsedTitle = root.findViewById(R.id.tv_collapsed_title);
        ivCover = root.findViewById(R.id.iv_cover);
        adaptive = Prefs.getBoolean("adaptive_enable", true);
        collapsedH = Prefs.getInt("collapsed_height_dp", 44);
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = IslandAdaptiveHelper.getSafeTop(this) + IslandAdaptiveHelper.dpToPx(this, 8);
        try { wm.addView(root, params); } catch (Throwable ignored) { return; }
        root.setOnClickListener(v -> toggleExpand());
        applyAdaptive(false);
        syncBackdrop(IslandBackdropView.MODE_LIFE, false);
    }
    private void toggleExpand() {
        expandedState = !expandedState;
        if (expandedState) {
            collapsed.animate().alpha(0f).setDuration(120).withEndAction(() -> {
                collapsed.setVisibility(View.GONE);
                expanded.setVisibility(View.VISIBLE);
                expanded.setAlpha(0f); expanded.setScaleX(0.92f); expanded.setScaleY(0.92f);
                expanded.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(280).start();
            }).start();
        } else {
            expanded.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f).setDuration(160).withEndAction(() -> {
                expanded.setVisibility(View.GONE);
                collapsed.setVisibility(View.VISIBLE);
                collapsed.setAlpha(0f);
                collapsed.animate().alpha(1f).setDuration(220).start();
            }).start();
        }
        root.measure(View.MeasureSpec.makeMeasureSpec(IslandAdaptiveHelper.getExpandedMaxWidth(this), View.MeasureSpec.AT_MOST),
                     View.MeasureSpec.makeMeasureSpec(IslandAdaptiveHelper.getExpandedMaxHeight(this), View.MeasureSpec.AT_MOST));
        applyAdaptive(expandedState);
    }
    private void applyAdaptive(boolean expanded) {
        if (root == null || params == null) return;
        int maxW = IslandAdaptiveHelper.getExpandedMaxWidth(this);
        int maxH = IslandAdaptiveHelper.getExpandedMaxHeight(this);
        root.measure(View.MeasureSpec.makeMeasureSpec(maxW, View.MeasureSpec.AT_MOST),
                     View.MeasureSpec.makeMeasureSpec(maxH, View.MeasureSpec.AT_MOST));
        int mw = Math.max(root.getMeasuredWidth(), IslandAdaptiveHelper.dpToPx(this, 120));
        int mh = Math.max(root.getMeasuredHeight(), IslandAdaptiveHelper.dpToPx(this, 40));
        int tw, th;
        if (expanded) { tw = Math.min(mw, maxW); th = Math.min(mh, maxH); }
        else {
            tw = adaptive ? IslandAdaptiveHelper.getCollapsedWidth(this, mw) : mw;
            th = adaptive ? IslandAdaptiveHelper.getCollapsedHeight(this, collapsedH) : IslandAdaptiveHelper.dpToPx(this, collapsedH);
        }
        params.width = tw; params.height = th;
        params.y = IslandAdaptiveHelper.getSafeTop(this) + IslandAdaptiveHelper.dpToPx(this, 8);
        final float radius = IslandAdaptiveHelper.getCornerRadius(this, expanded, tw);
        root.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius); }
        });
        root.setClipToOutline(true);
        if (backdrop instanceof IslandBackdropView) ((IslandBackdropView) backdrop).setCornerRadius(radius);
        try { wm.updateViewLayout(root, params); } catch (Throwable ignored) {}
    }
    private void syncBackdrop(int mode, boolean playing) {
        if (backdrop instanceof IslandBackdropView) {
            IslandBackdropView b = (IslandBackdropView) backdrop;
            b.setMode(mode);
            b.setChargingBreath(mode == IslandBackdropView.MODE_CHARGING);
        }
        if (pulse instanceof IslandPulseView) {
            IslandPulseView p = (IslandPulseView) pulse;
            p.setAccentColor(mode == IslandBackdropView.MODE_MUSIC ? 0xFFFF6B9D : 0xFF5AC8FA);
            p.setPulsing(playing && mode == IslandBackdropView.MODE_MUSIC);
        }
        if (glow instanceof IslandProgressGlowView) ((IslandProgressGlowView) glow).setRunning(playing);
    }
    private void syncMusic(Song s, boolean playing) {
        if (s == null) return;
        if (tvCollapsedTitle != null) tvCollapsedTitle.setText(s.title);
        if (ivCover != null) WallpaperHelper.loadCover(this, ivCover, s.id);
        syncBackdrop(IslandBackdropView.MODE_MUSIC, playing);
    }
    private void showNotify(String title) {
        if (tvCollapsedTitle != null && title != null) tvCollapsedTitle.setText(title);
        syncBackdrop(IslandBackdropView.MODE_NOTIFICATION, true);
        new Handler().postDelayed(() -> syncBackdrop(IslandBackdropView.MODE_LIFE, false), 5000);
    }
    private void startClock() {
        final Handler h = new Handler();
        final SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        final SimpleDateFormat df = new SimpleDateFormat("M月d日 EEE", Locale.CHINA);
        h.post(new Runnable() {
            @Override public void run() {
                Date now = new Date();
                if (tvTime != null) tvTime.setText(tf.format(now));
                if (tvDate != null) tvDate.setText(df.format(now));
                if (tvCollapsedTitle != null && !expandedState) tvCollapsedTitle.setText(tf.format(now));
                h.postDelayed(this, 1000);
            }
        });
    }
    @Override public void onConfigurationChanged(Configuration c) { super.onConfigurationChanged(c); applyAdaptive(expandedState); }
    @Override public int onStartCommand(Intent i, int f, int s) {
        if (i != null && "TEST_NOTIFY".equals(i.getAction())) showNotify("测试通知");
        return START_STICKY;
    }
    @Override public void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(receiver); } catch (Throwable ignored) {}
        if (root != null && wm != null) { try { wm.removeView(root); } catch (Throwable ignored) {} root = null; }
    }
    @Nullable @Override public IBinder onBind(Intent i) { return null; }
}
