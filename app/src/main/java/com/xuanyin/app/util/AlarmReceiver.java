package com.xuanyin.app.util;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import com.xuanyin.app.R;
import java.util.Calendar;
public class AlarmReceiver extends BroadcastReceiver {
    private static Ringtone ringtone;
    private static Handler fadeHandler;
    private static final String CH = "alarm_ch";
    @Override public void onReceive(Context c, Intent i) {
        try {
            Uri u = null;
            String saved = Prefs.getString("alarm_ringtone", "");
            if (saved != null && saved.length() > 0) {
                try { u = Uri.parse(saved); } catch (Throwable ignored) {}
            }
            if (u == null) {
                u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (u == null) u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            ringtone = RingtoneManager.getRingtone(c, u);
            if (ringtone != null && Build.VERSION.SDK_INT >= 21) {
                ringtone.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            }
            boolean fade = Prefs.getInt("alarm_fade", 1) == 1;
            if (fade) {
                final AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    try { am.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0); } catch (Throwable ignored) {}
                    fadeHandler = new Handler(Looper.getMainLooper());
                    final int max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                    final long start = System.currentTimeMillis();
                    fadeHandler.post(new Runnable() {
                        @Override public void run() {
                            long elapsed = System.currentTimeMillis() - start;
                            if (elapsed >= 5000L) { am.setStreamVolume(AudioManager.STREAM_ALARM, max, 0); return; }
                            int v = (int)(max * elapsed / 5000L);
                            try { am.setStreamVolume(AudioManager.STREAM_ALARM, v, 0); } catch (Throwable ignored) {}
                            fadeHandler.postDelayed(this, 200);
                        }
                    });
                }
            }
            if (ringtone != null) ringtone.play();
        } catch (Throwable ignored) {}
        try {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
                if (nm.getNotificationChannel(CH) == null) {
                    NotificationChannel ch = new NotificationChannel(CH, "闹钟", NotificationManager.IMPORTANCE_HIGH);
                    nm.createNotificationChannel(ch);
                }
            }
            Notification n = new NotificationCompat.Builder(c, CH)
                .setContentTitle("⏰ 玄音闹钟")
                .setContentText("时间到了！")
                .setSmallIcon(R.drawable.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true).build();
            if (nm != null) nm.notify(3001, n);
        } catch (Throwable ignored) {}
        Intent is = new Intent("com.xuanyin.app.ALARM");
        is.setPackage(c.getPackageName());
        c.sendBroadcast(is);
        // 智能重设：若有重复日，按下一个匹配日；否则明天
        int h = Prefs.getInt("alarm_hour", -1);
        int m = Prefs.getInt("alarm_min", -1);
        if (h >= 0 && m >= 0) AlarmHelper.set(c, h, m);
    }
}
