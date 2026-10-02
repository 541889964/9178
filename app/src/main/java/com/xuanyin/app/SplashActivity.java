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
        set.addAnimation(new ScaleAnimation(0.8f, 1f, 0.8f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f));
        set.setDuration(850);
        set.setInterpolator(new OvershootInterpolator(1.3f));
        logo.startAnimation(set);
        if (sub != null) { sub.setAlpha(0f); sub.setTranslationY(24f);
            sub.animate().alpha(1f).translationY(0).setStartDelay(360).setDuration(540).start(); }
        if (glow != null) { glow.setAlpha(0f); glow.animate().alpha(1f).setDuration(1150).start(); }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }, 1380);
    }
}
