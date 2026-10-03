package com.xuanyin.app.util;

import android.content.Context;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;

/**
 * 玄音动画工具
 * v45 全部方法保留 + v47 新增丝滑入口
 * 只加不删
 */
public class Anim {

    // ==================== v45 缓动器（全部保留）====================
    public static final Interpolator EASE_ELASTIC =
            new PathInterpolator(0.34f, 1.15f, 0.36f, 1f);
    public static final Interpolator EASE_SMOOTH =
            new PathInterpolator(0.16f, 1f, 0.3f, 1f);
    public static final Interpolator EASE_OPEN = new DecelerateInterpolator(2.4f);
    public static final Interpolator EASE_SOFT =
            new PathInterpolator(0.4f, 0f, 0.2f, 1f);
    public static final Interpolator EASE_ROW = new DecelerateInterpolator(1.8f);

    // ==================== v47 新增丝滑插值器 ====================
    public static final Interpolator EASE_VISCOUS =
            new PathInterpolator(0.22f, 1.28f, 0.34f, 1.0f);
    public static final Interpolator EASE_LIQUID =
            new PathInterpolator(0.32f, 0.72f, 0f, 1f);
    public static final Interpolator EASE_ISLAND_OPEN =
            new PathInterpolator(0.05f, 0.7f, 0.1f, 1f);
    public static final Interpolator EASE_ISLAND_CLOSE =
            new PathInterpolator(0.4f, 0f, 0.2f, 1.02f);
    public static final Interpolator EASE_MERGE =
            new PathInterpolator(0.65f, 0.05f, 0.36f, 1f);
    public static final Interpolator EASE_ROW_STAGGER =
            new PathInterpolator(0.16f, 1f, 0.3f, 1f);
    public static final Interpolator EASE_TAP_BOUNCE =
            new PathInterpolator(0.34f, 1.56f, 0.64f, 1f);

    // ==================== v45 原有方法（全部保留）====================
    public static void cancel(View v) {
        if (v != null) v.animate().cancel();
    }

    public static void press(View v) {
        if (v == null) return;
        v.animate().cancel();
        v.animate().scaleX(0.90f).scaleY(0.90f).setDuration(80)
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        if (v != null) v.animate().scaleX(1f).scaleY(1f)
                                .setDuration(280)
                                .setInterpolator(new OvershootInterpolator(2.6f)).start();
                    }
                }).start();
    }

    public static void pressLight(View v) {
        if (v == null) return;
        v.animate().cancel();
        v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70)
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        if (v != null) v.animate().scaleX(1f).scaleY(1f)
                                .setDuration(240)
                                .setInterpolator(new OvershootInterpolator(2.4f)).start();
                    }
                }).start();
    }

    public static void enter(View v, long delay) {
        if (v == null) return;
        v.setAlpha(0f); v.setTranslationY(60f);
        v.setScaleX(0.92f); v.setScaleY(0.92f);
        v.animate().alpha(1f).translationY(0).scaleX(1f).scaleY(1f)
                .setDuration(540).setStartDelay(delay)
                .setInterpolator(new OvershootInterpolator(1.0f)).start();
    }

    public static void fadeIn(View v, long d) {
        if (v == null) return;
        v.setAlpha(0f);
        v.animate().alpha(1f).setDuration(d).start();
    }

    public static void fadeOut(View v, long d, Runnable end) {
        if (v == null) { if (end != null) end.run(); return; }
        v.animate().alpha(0f).setDuration(d).withEndAction(end).start();
    }

    // ==================== v47 新增方法 ====================
    /** v47：分列式入场 —— 列表项依次滑入 */
    public static void staggerIn(final View[] items, long baseDelay, long stepMs) {
        if (items == null) return;
        for (int i = 0; i < items.length; i++) {
            final View v = items[i];
            if (v == null) continue;
            v.setAlpha(0f);
            v.setTranslationY(18f * v.getContext().getResources().getDisplayMetrics().density);
            v.setScaleX(0.94f); v.setScaleY(0.94f);
            v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setStartDelay(baseDelay + i * stepMs)
                    .setDuration(360).setInterpolator(EASE_ROW_STAGGER).start();
        }
    }

    /** v47：灵动岛点击回弹 */
    public static void islandTapBounce(final View v) {
        if (v == null) return;
        v.animate().cancel();
        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(70)
                .setInterpolator(EASE_SOFT)
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        v.animate().scaleX(1f).scaleY(1f)
                                .setDuration(260)
                                .setInterpolator(EASE_TAP_BOUNCE).start();
                    }
                }).start();
    }

    /** v47：dp 工具 */
    private static float dp(Context c, float v) {
        return v * c.getResources().getDisplayMetrics().density;
    }
}
