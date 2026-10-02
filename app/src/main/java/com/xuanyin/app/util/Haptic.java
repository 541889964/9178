package com.xuanyin.app.util;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;
public class Haptic {
    private static Vibrator vb(Context c) { return (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE); }
    private static void one(Context c, long ms, int amp) {
        if (c == null) return;
        Vibrator v = vb(c);
        if (v == null || !v.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, amp));
            else v.vibrate(ms);
        } catch (Throwable ignored) {}
    }
    private static void wave(Context c, long[] pat, int[] amps) {
        if (c == null) return;
        Vibrator v = vb(c);
        if (v == null || !v.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(pat, amps, -1));
            else v.vibrate(pat, -1);
        } catch (Throwable ignored) {}
    }
    // 轻触：键盘感
    public static void tap(View v) {
        if (v != null) try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
        if (v != null) one(v.getContext(), 12, 60);
    }
    public static void tap(Context c) { one(c, 12, 60); }
    // 轻按
    public static void light(Context c) { one(c, 8, 40); }
    // 中按
    public static void mid(Context c) { one(c, 20, 90); }
    // 长按
    public static void longPress(View v) {
        if (v != null) try { v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); } catch (Throwable ignored) {}
        if (v != null) one(v.getContext(), 34, 110);
    }
    // 成功：短促双振
    public static void done(Context c) { wave(c, new long[]{0, 22, 36, 45}, new int[]{0, 80, 0, 120}); }
    // 警告
    public static void warn(Context c) { wave(c, new long[]{0, 60, 40, 60}, new int[]{0, 150, 0, 150}); }
    // 灵动岛展开
    public static void expand(Context c) { wave(c, new long[]{0, 14, 24, 20}, new int[]{0, 70, 0, 100}); }
    // 灵动岛收起
    public static void collapse(Context c) { one(c, 16, 80); }
    // 翻页
    public static void swipe(Context c) { one(c, 10, 50); }
}
