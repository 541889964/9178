package com.xuanyin.app.util;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;
public class AlarmHelper {
    public static void set(Context c, int h, int m) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, h);
            cal.set(Calendar.MINUTE, m);
            cal.set(Calendar.SECOND, 0);
            if (cal.getTimeInMillis() < System.currentTimeMillis()) cal.add(Calendar.DAY_OF_MONTH, 1);
            Intent i = new Intent(c, AlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(c, 0, i,
                Build.VERSION.SDK_INT >= 23
                    ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    : PendingIntent.FLAG_UPDATE_CURRENT);
            if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            else am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            Prefs.put("alarm_hour", h);
            Prefs.put("alarm_min", m);
        } catch (Throwable ignored) {}
    }
    public static String text() {
        int h = Prefs.getInt("alarm_hour", -1);
        int m = Prefs.getInt("alarm_min", -1);
        return (h < 0 || m < 0) ? "未设置" : String.format("%02d:%02d", h, m);
    }
    public static void cancel(Context c) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(c, AlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(c, 0, i,
                Build.VERSION.SDK_INT >= 23
                    ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    : PendingIntent.FLAG_UPDATE_CURRENT);
            am.cancel(pi);
        } catch (Throwable ignored) {}
    }
}
