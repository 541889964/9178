package com.xuanyin.app;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.*;
import androidx.appcompat.app.AppCompatActivity;
public class SplashActivity extends AppCompatActivity {
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean gone = false;
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            setContentView(R.layout.activity_splash);
            View logo = findViewById(R.id.splash_logo);
            View sub = findViewById(R.id.splash_sub);
            View glow = findViewById(R.id.splash_glow);
            if (logo != null) {
                AnimationSet set = new AnimationSet(true);
                set.addAnimation(new AlphaAnimation(0f, 1f));
                set.addAnimation(new ScaleAnimation(0.85f, 1f, 0.85f, 1f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f));
                set.setDuration(700);
                set.setInterpolator(new DecelerateInterpolator(1.4f));
                logo.startAnimation(set);
            }
            if (sub != null) {
                sub.setAlpha(0f);
                sub.animate().alpha(1f).setStartDelay(280).setDuration(500).start();
            }
            if (glow != null) {
                glow.setAlpha(0f);
                glow.animate().alpha(1f).setDuration(900).start();
            }
        } catch (Throwable ignored) {}
        h.postDelayed(this::go, 1500);
    }
    private void go() {
        if (gone) return;
        gone = true;
        try {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        } catch (Throwable t) {
            // 二次尝试
            try { startActivity(new Intent(this, MainActivity.class)); } catch (Throwable ignored) {}
        }
        h.postDelayed(this::finish, 800);
    }
    @Override protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
