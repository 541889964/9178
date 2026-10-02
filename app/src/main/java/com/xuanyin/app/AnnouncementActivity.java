package com.xuanyin.app;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
public class AnnouncementActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_announcement);
        ((TextView) findViewById(R.id.tv_announcement)).setText(R.string.announcement_full);
        findViewById(R.id.btn_agree).setOnClickListener(v -> {
            v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80)
                .withEndAction(() -> { finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); }).start();
        });
    }
}
