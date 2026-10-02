package com.xuanyin.app.util;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;
public class AlarmHelper {
    public static void set(Context ctx, int hour, int min) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour); c.set(Calendar.MINUTE, min); c.set(Calendar.SECOND, 0);
        if (c.getTimeInMillis() < System.currentTimeMillis()) c.add(Calendar.DAY_OF_MONTH, 1);
        Intent i = new Intent(ctx, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, i,
            Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
        else am.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
        Prefs.put("alarm_hour", hour); Prefs.put("alarm_min", min);
    }
    public static String getText() {
        int h = Prefs.getInt("alarm_hour", -1), m = Prefs.getInt("alarm_min", -1);
        return (h < 0 || m < 0) ? "未设置" : String.format("%02d:%02d", h, m);
    }
}
