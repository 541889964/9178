package com.xuanyin.app.util;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
public class AlarmReceiver extends BroadcastReceiver {
    private static Ringtone ringtone;
    @Override public void onReceive(Context c, Intent i) {
        try {
            Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            ringtone = RingtoneManager.getRingtone(c, u);
            ringtone.play();
        } catch (Throwable ignored) {}
        Intent is = new Intent("com.xuanyin.app.ALARM");
        is.setPackage(c.getPackageName());
        c.sendBroadcast(is);
        int h = Prefs.getInt("alarm_hour", -1), m = Prefs.getInt("alarm_min", -1);
        if (h >= 0 && m >= 0) AlarmHelper.set(c, h, m);
    }
}
