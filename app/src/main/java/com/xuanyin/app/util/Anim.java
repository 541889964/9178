package com.xuanyin.app.util;
import android.animation.TimeInterpolator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
public class Anim {
    private static final DecelerateInterpolator DEC = new DecelerateInterpolator(1.6f);
    private static final OvershootInterpolator OVER = new OvershootInterpolator(2.4f);
    private static final AccelerateDecelerateInterpolator ACC = new AccelerateDecelerateInterpolator();

    // 主按钮：按压回弹 2.4x 超冲
    public static void press(View v) {
        if (v == null) return;
        v.animate().cancel();
        v.animate().scaleX(0.91f).scaleY(0.91f).setDuration(75)
            .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f)
                .setDuration(260).setInterpolator(OVER).start()).start();
    }
    // 次级按钮：轻按
    public static void pressLight(View v) {
        if (v == null) return;
        v.animate().cancel();
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(65)
            .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f)
                .setDuration(220).setInterpolator(OVER).start()).start();
    }
    // 卡片入场：上滑 + 缩放 + 淡入 + Overshoot
    public static void enter(View v, long delay) {
        if (v == null) return;
        v.setAlpha(0f); v.setTranslationY(64f);
        v.setScaleX(0.90f); v.setScaleY(0.90f);
        v.animate().alpha(1f).translationY(0).scaleX(1f).scaleY(1f)
            .setDuration(560).setStartDelay(delay)
            .setInterpolator(new OvershootInterpolator(0.9f)).start();
    }
    // 淡入
    public static void fadeIn(View v, long d) {
        if (v == null) return;
        v.setAlpha(0f);
        v.animate().alpha(1f).setDuration(d).setInterpolator(DEC).start();
    }
    // 淡出
    public static void fadeOut(View v, long d, Runnable end) {
        if (v == null) { if (end != null) end.run(); return; }
        v.animate().alpha(0f).setDuration(d).withEndAction(end).start();
    }
    // 上下浮动
    public static void floatY(View v, float dy, long d) {
        if (v == null) return;
        v.animate().translationY(dy).setDuration(d)
            .setInterpolator(ACC).withEndAction(() -> v.animate()
                .translationY(0).setDuration(d).setInterpolator(ACC).start()).start();
    }
    // 呼吸光晕
    public static void breathe(View v) {
        if (v == null) return;
        v.animate().cancel();
        v.setAlpha(0.35f);
        v.animate().alpha(0.95f).setDuration(2400)
            .setInterpolator(ACC)
            .withEndAction(() -> v.animate().alpha(0.35f).setDuration(2400)
                .setInterpolator(ACC).withEndAction(() -> breathe(v)).start()).start();
    }
    // 弹簧展开
    public static void springScale(View v, float target) {
        if (v == null) return;
        SpringAnimation sx = new SpringAnimation(v, SpringAnimation.SCALE_X, target);
        SpringAnimation sy = new SpringAnimation(v, SpringAnimation.SCALE_Y, target);
        SpringForce f = new SpringForce(target).setDampingRatio(0.72f).setStiffness(560f);
        sx.setSpring(f); sy.setSpring(f);
        sx.start(); sy.start();
    }
    // 列表错峰入场
    public static void listEnter(View v, int pos) {
        if (v == null) return;
        v.setAlpha(0f);
        v.setTranslationY(72f);
        v.setScaleX(0.90f); v.setScaleY(0.90f);
        v.animate().alpha(1f).translationY(0).scaleX(1f).scaleY(1f)
            .setDuration(600)
            .setStartDelay(Math.min(pos, 14) * 42L)
            .setInterpolator(new OvershootInterpolator(0.85f)).start();
    }
}
