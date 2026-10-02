package com.xuanyin.app.util;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

public class RingtoneHelper {
    private static final String TAG = "RingtoneHelper";
    private static Ringtone currentRingtone = null;

    public static boolean setAlarm(Context context, Object tag) {
        return setAlarm(context, tag, 0L);
    }

    public static boolean setAlarm(Context context, Object tag, long durationMs) {
        if (context == null) return false;
        try {
            stopAlarm(context);
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (uri != null) {
                currentRingtone = RingtoneManager.getRingtone(context.getApplicationContext(), uri);
                if (currentRingtone != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) currentRingtone.setLooping(true);
                    currentRingtone.play();
                }
            }
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                long[] pattern = new long[]{0, 500, 500, 500};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    v.vibrate(VibrationEffect.createWaveform(pattern, 0));
                else v.vibrate(pattern, 0);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "setAlarm failed", e);
            return false;
        }
    }

    public static void stopAlarm() {
        try {
            if (currentRingtone != null && currentRingtone.isPlaying()) currentRingtone.stop();
            currentRingtone = null;
        } catch (Exception ignored) {}
    }

    public static void stopAlarm(Context context) {
        stopAlarm();
        if (context == null) return;
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.cancel();
        } catch (Exception ignored) {}
    }

    public static boolean isPlaying() {
        try { return currentRingtone != null && currentRingtone.isPlaying(); }
        catch (Exception ignored) { return false; }
    }
}
