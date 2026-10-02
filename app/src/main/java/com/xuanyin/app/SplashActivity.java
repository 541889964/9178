package com.xuanyin.app;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.*;
import androidx.appcompat.app.AppCompatActivity;
public class SplashActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_splash);
        View logo = findViewById(R.id.splash_logo);
        View sub = findViewById(R.id.splash_sub);
        View glow = findViewById(R.id.splash_glow);
        AnimationSet set = new AnimationSet(true);
        set.addAnimation(new AlphaAnimation(0f, 1f));
        set.addAnimation(new ScaleAnimation(0.86f, 1f, 0.86f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f));
        set.setDuration(720);
        set.setInterpolator(new DecelerateInterpolator(1.6f));
        logo.startAnimation(set);
        if (sub != null) {
            sub.setAlpha(0f);
            sub.animate().alpha(1f).setStartDelay(320).setDuration(500).start();
        }
        if (glow != null) {
            glow.setAlpha(0f);
            glow.animate().alpha(1f).setDuration(900).start();
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1200);
    }
}
