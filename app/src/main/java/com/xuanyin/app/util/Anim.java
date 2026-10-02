package com.xuanyin.app.util;
import android.view.View;
import android.view.animation.OvershootInterpolator;
public class Anim {
    public static void cancel(View v) { if (v != null) v.animate().cancel(); }
    public static void press(View v) {
        if (v == null) return;
        v.animate().cancel();
        v.animate().scaleX(0.90f).scaleY(0.90f).setDuration(80)
            .withEndAction(new Runnable() { @Override public void run() {
                if (v == null) return;
                v.animate().scaleX(1f).scaleY(1f).setDuration(280).setInterpolator(new OvershootInterpolator(2.6f)).start();
            }}).start();
    }
    public static void pressLight(View v) {
        if (v == null) return;
        v.animate().cancel();
        v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70)
            .withEndAction(new Runnable() { @Override public void run() {
                if (v == null) return;
                v.animate().scaleX(1f).scaleY(1f).setDuration(240).setInterpolator(new OvershootInterpolator(2.4f)).start();
            }}).start();
    }
    public static void enter(View v, long delay) {
        if (v == null) return;
        v.setAlpha(0f); v.setTranslationY(60f); v.setScaleX(0.92f); v.setScaleY(0.92f);
        v.animate().alpha(1f).translationY(0).scaleX(1f).scaleY(1f)
            .setDuration(540).setStartDelay(delay).setInterpolator(new OvershootInterpolator(1.0f)).start();
    }
    public static void fadeIn(View v, long d) { if (v == null) return; v.setAlpha(0f); v.animate().alpha(1f).setDuration(d).start(); }
    public static void fadeOut(View v, long d, Runnable end) {
        if (v == null) { if (end != null) end.run(); return; }
        v.animate().alpha(0f).setDuration(d).withEndAction(end).start();
    }
}
