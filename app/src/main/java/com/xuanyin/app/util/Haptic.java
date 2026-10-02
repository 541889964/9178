package com.xuanyin.app.util;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;
public class Haptic {
    private static Vibrator vb(Context c) {
        if (c == null) return null;
        try { return (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE); } catch (Throwable t) { return null; }
    }
    private static void one(Context c, long ms, int amp) {
        Vibrator v = vb(c); if (v == null || !v.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, amp));
            else v.vibrate(ms);
        } catch (Throwable ignored) {}
    }
    private static void wave(Context c, long[] p, int[] a) {
        Vibrator v = vb(c); if (v == null || !v.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(p, a, -1));
            else v.vibrate(p, -1);
        } catch (Throwable ignored) {}
    }
    public static void tap(View v) {
        if (v != null) try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
        if (v != null) one(v.getContext(), 12, 60);
    }
    public static void tap(Context c) { one(c, 12, 60); }
    public static void light(Context c) { one(c, 8, 40); }
    public static void mid(Context c) { one(c, 20, 90); }
    public static void strong(Context c) { one(c, 30, 120); }
    public static void charge(Context c) { wave(c, new long[]{0, 18, 22, 18, 22, 30}, new int[]{0, 60, 0, 80, 0, 100}); }
    public static void longPress(View v) {
        if (v != null) try { v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); } catch (Throwable ignored) {}
        if (v != null) one(v.getContext(), 34, 110);
    }
    public static void longPress(Context c) { one(c, 34, 110); }
    public static void done(Context c) { wave(c, new long[]{0, 22, 36, 45}, new int[]{0, 80, 0, 120}); }
    public static void warn(Context c) { wave(c, new long[]{0, 60, 40, 60}, new int[]{0, 150, 0, 150}); }
    public static void expand(Context c) { wave(c, new long[]{0, 14, 24, 18}, new int[]{0, 70, 0, 100}); }
    public static void collapse(Context c) { one(c, 18, 85); }
    public static void swipe(Context c) { one(c, 14, 65); }
    public static void notify(Context c) { wave(c, new long[]{0, 30, 20, 40}, new int[]{0, 100, 0, 130}); }
    public static void alarm(Context c) { wave(c, new long[]{0, 80, 100, 80, 100, 80}, new int[]{0, 200, 0, 200, 0, 200}); }
}
