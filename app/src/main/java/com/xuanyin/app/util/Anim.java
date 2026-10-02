package com.xuanyin.app.util;
import android.view.View;
import android.view.animation.OvershootInterpolator;
public class Anim {
    public static void press(View v) {
        if (v == null) return;
        v.animate().cancel();
        v.animate().scaleX(0.91f).scaleY(0.91f).setDuration(75)
            .withEndAction(new Runnable() {
                @Override public void run() {
                    if (v == null) return;
                    v.animate().scaleX(1f).scaleY(1f).setDuration(260)
                        .setInterpolator(new OvershootInterpolator(2.4f)).start();
                    public static void cancel(View v) {
        if (v == null) return;
        v.animate().cancel();
    }
}
            }).start();
    }
    public static void pressLight(View v) {
        if (v == null) return;
        v.animate().cancel();
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(65)
            .withEndAction(new Runnable() {
                @Override public void run() {
                    if (v == null) return;
                    v.animate().scaleX(1f).scaleY(1f).setDuration(220)
                        .setInterpolator(new OvershootInterpolator(2.2f)).start();
                }
            }).start();
    }
    public static void enter(View v, long delay) {
        if (v == null) return;
        v.setAlpha(0f);
        v.setTranslationY(64f);
        v.setScaleX(0.90f);
        v.setScaleY(0.90f);
        v.animate().alpha(1f).translationY(0).scaleX(1f).scaleY(1f)
            .setDuration(560).setStartDelay(delay)
            .setInterpolator(new OvershootInterpolator(0.9f)).start();
    }
    public static void fadeIn(View v, long d) {
        if (v == null) return;
        v.setAlpha(0f);
        v.animate().alpha(1f).setDuration(d).start();
    }
    public static void fadeOut(View v, long d, Runnable end) {
        if (v == null) {
            if (end != null) end.run();
            return;
        }
        v.animate().alpha(0f).setDuration(d).withEndAction(end).start();
    }
}
