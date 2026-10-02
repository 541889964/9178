package com.xuanyin.app.service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.xuanyin.app.R;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public class ScreenRecordService extends Service {
    public static final String ACTION_START = "com.xuanyin.app.REC_START";
    public static final String ACTION_STOP = "com.xuanyin.app.REC_STOP";
    public static final String EXTRA_CODE = "code";
    public static final String EXTRA_DATA = "data";
    public static final String A_STATE = "com.xuanyin.app.REC_STATE";
    private static final String CH = "rec_ch";
    private static final int NID = 2001;
    private MediaProjection projection;
    private VirtualDisplay display;
    private MediaRecorder recorder;
    private boolean recording = false;
    private File outFile;
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        String a = intent.getAction();
        if (ACTION_START.equals(a)) {
            int code = intent.getIntExtra(EXTRA_CODE, -1);
            Intent data = intent.getParcelableExtra(EXTRA_DATA);
            if (code > 0 && data != null) startRecord(code, data);
        } else if (ACTION_STOP.equals(a)) {
            stopRecord();
        }
        return START_STICKY;
    }
    private void startRecord(int code, Intent data) {
        if (recording) return;
        try {
            ensureChannel();
            startForeground(NID, buildNotif());
            DisplayMetrics dm = new DisplayMetrics();
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            if (Build.VERSION.SDK_INT >= 17) wm.getDefaultDisplay().getRealMetrics(dm);
            else wm.getDefaultDisplay().getMetrics(dm);
            int w = dm.widthPixels, h = dm.heightPixels, dpi = dm.densityDpi;
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "玄音");
            if (!dir.exists()) dir.mkdirs();
            String name = "screen_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".mp4";
            outFile = new File(dir, name);
            if (Build.VERSION.SDK_INT >= 31) recorder = new MediaRecorder(this);
            else recorder = new MediaRecorder();
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            recorder.setVideoSize(w, h);
            recorder.setVideoFrameRate(30);
            recorder.setVideoEncodingBitRate(6000000);
            recorder.setOutputFile(outFile.getAbsolutePath());
            recorder.prepare();
            MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            projection = mpm.getMediaProjection(code, data);
            display = projection.createVirtualDisplay("xy_rec", w, h, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder.getSurface(), null, null);
            recorder.start();
            recording = true;
            sendState(true);
        } catch (Throwable t) { sendState(false); stopSelf(); }
    }
    private void stopRecord() {
        if (!recording) { stopSelf(); return; }
        recording = false;
        try { if (recorder != null) recorder.stop(); } catch (Throwable ignored) {}
        try { if (recorder != null) recorder.release(); } catch (Throwable ignored) {}
        recorder = null;
        try { if (display != null) display.release(); } catch (Throwable ignored) {}
        display = null;
        try { if (projection != null) projection.stop(); } catch (Throwable ignored) {}
        projection = null;
        sendState(false);
        stopSelf();
    }
    private void sendState(boolean on) {
        try {
            Intent i = new Intent(A_STATE);
            i.putExtra("on", on);
            if (outFile != null) i.putExtra("path", outFile.getAbsolutePath());
            i.setPackage(getPackageName());
            sendBroadcast(i);
        } catch (Throwable ignored) {}
    }
    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CH) == null) {
                NotificationChannel ch = new NotificationChannel(CH, "录屏", NotificationManager.IMPORTANCE_LOW);
                ch.setShowBadge(false);
                nm.createNotificationChannel(ch);
            }
        }
    }
    private Notification buildNotif() {
        return new NotificationCompat.Builder(this, CH)
            .setContentTitle("玄音·录屏")
            .setContentText("正在录屏...")
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true).build();
    }
    @Override public void onDestroy() { if (recording) stopRecord(); super.onDestroy(); }
    @Nullable @Override public IBinder onBind(Intent i) { return null; }
}
