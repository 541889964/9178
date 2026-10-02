package com.xuanyin.app.util;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
public class ValueAnimatorProxy {
    public static void run(View out, View in, Runnable end) {
        ValueAnimator v = ValueAnimator.ofFloat(0f, 1f);
        v.setDuration(620);
        v.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) { if (end != null) end.run(); }
        });
        v.start();
    }
}
