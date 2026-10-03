package com.xuanyin.app.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.os.Build;
import android.view.Choreographer;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;

/**
 * 玄音 v47 丝滑动画核心
 * 与 Anim.java 并存，只加不删
 *
 * 核心能力：
 * 1. Choreographer 帧同步 —— 每帧只提交一次 WindowManager 更新
 * 2. 硬件层自动管理 —— 动画开始上硬件层，结束释放
 * 3. 顶级丝滑插值器 —— 贴近 iOS 灵动岛手感
 * 4. 预分配 —— 减少 GC 抖动
 */
public final class SmoothAnimator {

    private SmoothAnimator() {}

    // ==================== v47 丝滑插值器 ====================
    /** 超丝滑弹性分裂 —— 通知岛分裂主用 */
    public static final TimeInterpolator EASE_VISCOUS =
            new PathInterpolator(0.22f, 1.28f, 0.34f, 1.0f);
    /** 液态玻璃收拢 */
    public static final TimeInterpolator EASE_LIQUID =
            new PathInterpolator(0.32f, 0.72f, 0f, 1f);
    /** 灵动岛展开（iOS 手感） */
    public static final TimeInterpolator EASE_ISLAND_OPEN =
            new PathInterpolator(0.05f, 0.7f, 0.1f, 1f);
    /** 灵动岛收起（轻微过冲，回弹自然） */
    public static final TimeInterpolator EASE_ISLAND_CLOSE =
            new PathInterpolator(0.4f, 0f, 0.2f, 1.02f);
    /** 通知融合 */
    public static final TimeInterpolator EASE_MERGE =
            new PathInterpolator(0.65f, 0.05f, 0.36f, 1f);
    /** 呼吸/脉冲（充电、录屏红点） */
    public static final TimeInterpolator EASE_BREATH =
            new PathInterpolator(0.4f, 0f, 0.6f, 1f);
    /** 分列式行入场 */
    public static final TimeInterpolator EASE_ROW_STAGGER =
            new PathInterpolator(0.16f, 1f, 0.3f, 1f);
    /** 点击回弹（Q 弹） */
    public static final TimeInterpolator EASE_TAP_BOUNCE =
            new PathInterpolator(0.34f, 1.56f, 0.64f, 1f);

    // ==================== 帧同步 WindowManager 动画 ====================
    public interface OnFrame { void onFrame(int w, int h, float fraction); }

    /**
     * 丝滑 WindowManager 尺寸动画
     * 使用 Choreographer 保证每帧只提交一次
     */
    public static ValueAnimator animateWindowSize(
            final WindowManager wm,
            final WindowManager.LayoutParams params,
            final View view,
            int fromW, int toW, int fromH, int toH,
            long duration,
            TimeInterpolator interpolator,
            final OnFrame onFrame) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(duration);
        anim.setInterpolator(interpolator != null ? interpolator : EASE_ISLAND_OPEN);

        final int fw = fromW, tw = toW, fh = fromH, th = toH;
        final float[] lastFrame = new float[]{-1f};

        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                final float f = (float) a.getAnimatedValue();
                if (Math.abs(f - lastFrame[0]) < 0.0001f) return;
                lastFrame[0] = f;
                final int w = fw + Math.round((tw - fw) * f);
                final int h = fh + Math.round((th - fh) * f);
                Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
                    @Override public void doFrame(long frameTimeNanos) {
                        try {
                            params.width = w;
                            params.height = h;
                            wm.updateViewLayout(view, params);
                            if (onFrame != null) onFrame.onFrame(w, h, f);
                        } catch (Throwable ignored) {}
                    }
                });
            }
        });

        anim.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) {
                view.postDelayed(new Runnable() {
                    @Override public void run() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            view.setLayerType(View.LAYER_TYPE_NONE, null);
                        }
                    }
                }, 32);
            }
        });

        anim.start();
        return anim;
    }

    // ==================== 硬件层快捷方法 ====================
    public static void withHardwareLayer(View v, long delayMs, Runnable action) {
        if (v == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        if (action != null) action.run();
        v.postDelayed(new Runnable() {
            @Override public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    v.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }
        }, Math.max(32, delayMs));
    }

    // ==================== 呼吸动画 ====================
    public static ValueAnimator breath(final View target, float from, float to, long periodMs) {
        ValueAnimator a = ValueAnimator.ofFloat(from, to);
        a.setDuration(periodMs);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setInterpolator(EASE_BREATH);
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator v) {
                float f = (float) v.getAnimatedValue();
                target.setScaleX(f);
                target.setScaleY(f);
                target.setAlpha(Math.min(1f, f));
            }
        });
        a.start();
        return a;
    }
}
