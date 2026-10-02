package com.xuanyin.app.util;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.xuanyin.app.R;
public class AlarmReceiver extends BroadcastReceiver {
    private static Ringtone ringtone;
    private static final String CH = "alarm_ch";
    @Override public void onReceive(Context c, Intent i) {
        try {
            Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (u == null) u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ringtone = RingtoneManager.getRingtone(c, u);
            if (ringtone != null && Build.VERSION.SDK_INT >= 21) {
                ringtone.setAudioAttributes(new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
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
        int h = Prefs.getInt("alarm_hour", -1);
        int m = Prefs.getInt("alarm_min", -1);
        if (h >= 0 && m >= 0) AlarmHelper.set(c, h, m);
    }
}
