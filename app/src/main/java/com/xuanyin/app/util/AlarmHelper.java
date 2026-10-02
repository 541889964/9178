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
            cal.set(Calendar.MILLISECOND, 0);
            long now = System.currentTimeMillis();
            // 检查是否有重复日
            boolean anyDay = false;
            for (int i = 0; i < 7; i++) if (Prefs.getInt("alarm_week_" + i, 0) == 1) { anyDay = true; break; }
            if (!anyDay) {
                // 无重复日 → 单次（今天或明天）
                if (cal.getTimeInMillis() <= now) cal.add(Calendar.DAY_OF_MONTH, 1);
            } else {
                // 有重复日 → 找下一个匹配的星期几（周一=1 ... 周日=7）
                // 我们的存储：0=周一...6=周日
                // Calendar.DAY_OF_WEEK: 1=周日 ... 7=周六
                int tries = 0;
                while (tries < 8) {
                    int dow = cal.get(Calendar.DAY_OF_WEEK);
                    int ourIdx = (dow == Calendar.SUNDAY) ? 6 : (dow - 2);
                    boolean match = Prefs.getInt("alarm_week_" + ourIdx, 0) == 1;
                    if (match && cal.getTimeInMillis() > now) break;
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                    tries++;
                }
            }
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
