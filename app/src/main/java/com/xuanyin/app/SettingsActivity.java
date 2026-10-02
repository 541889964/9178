package com.xuanyin.app;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.xuanyin.app.util.Haptic;
import com.xuanyin.app.util.Prefs;
public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);
        bindCw(); bindCh(); bindEw(); bindEh();
        findViewById(R.id.btn_notify_access).setOnClickListener(v -> { Haptic.tap(v);
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")); });
        findViewById(R.id.btn_battery).setOnClickListener(v -> { Haptic.tap(v);
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + getPackageName()))); });
        findViewById(R.id.btn_test_notify).setOnClickListener(v -> { Haptic.tap(v);
            startService(new Intent(this, com.xuanyin.app.service.IslandService.class).setAction("TEST_NOTIFY")); });
        findViewById(R.id.btn_announcement).setOnClickListener(v -> { Haptic.tap(v);
            startActivity(new Intent(this, AnnouncementActivity.class)); });
    }
    private void bindCw() {
        SeekBar sb = findViewById(R.id.sb_collapsed);
        TextView tv = findViewById(R.id.tv_collapsed_val);
        int v = (int)(Prefs.getFloat("collapsed_width_percent", 0.62f) * 100);
        sb.setProgress(v - 30); tv.setText(v + "%");
        sb.setOnSeekBarChangeListener(new S() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                int val = 30 + p; tv.setText(val + "%");
                Prefs.put("collapsed_width_percent", val / 100f);
                if (u) restartIsland();
            }
            @Override public void onStopTrackingTouch(SeekBar s) { Haptic.tap(s); }
        });
    }
    private void bindCh() {
        SeekBar sb = findViewById(R.id.sb_height);
        TextView tv = findViewById(R.id.tv_height_val);
        int v = Prefs.getInt("collapsed_height_dp", 44);
        sb.setProgress(v - 36); tv.setText(v + "dp");
        sb.setOnSeekBarChangeListener(new S() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                int val = 36 + p; tv.setText(val + "dp");
                Prefs.put("collapsed_height_dp", val);
                if (u) restartIsland();
            }
            @Override public void onStopTrackingTouch(SeekBar s) { Haptic.tap(s); }
        });
    }
    private void bindEw() {
        SeekBar sb = findViewById(R.id.sb_expanded_w);
        TextView tv = findViewById(R.id.tv_expanded_w_val);
        int v = (int)(Prefs.getFloat("expanded_width_percent", 0.94f) * 100);
        sb.setProgress(v - 60); tv.setText(v + "%");
        sb.setOnSeekBarChangeListener(new S() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                int val = 60 + p; tv.setText(val + "%");
                Prefs.put("expanded_width_percent", val / 100f);
                if (u) restartIsland();
            }
            @Override public void onStopTrackingTouch(SeekBar s) { Haptic.tap(s); }
        });
    }
    private void bindEh() {
        SeekBar sb = findViewById(R.id.sb_expanded_h);
        TextView tv = findViewById(R.id.tv_expanded_h_val);
        int v = Prefs.getInt("expanded_height_dp", 260);
        sb.setProgress(v - 120); tv.setText(v + "dp");
        sb.setOnSeekBarChangeListener(new S() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                int val = 120 + p; tv.setText(val + "dp");
                Prefs.put("expanded_height_dp", val);
                if (u) restartIsland();
            }
            @Override public void onStopTrackingTouch(SeekBar s) { Haptic.tap(s); }
        });
    }
    private void restartIsland() {
        stopService(new Intent(this, com.xuanyin.app.service.IslandService.class));
        startService(new Intent(this, com.xuanyin.app.service.IslandService.class));
    }
    static abstract class S implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar s) {}
        @Override public void onStopTrackingTouch(SeekBar s) {}
    }
}
