package com.xuanyin.app;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.xuanyin.app.util.Prefs;
public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);
        SeekBar sbC = findViewById(R.id.sb_collapsed);
        SeekBar sbH = findViewById(R.id.sb_height);
        TextView tvC = findViewById(R.id.tv_collapsed_val);
        TextView tvH = findViewById(R.id.tv_height_val);
        int cw = (int)(Prefs.getFloat("collapsed_width_percent", 0.62f) * 100);
        int ch = Prefs.getInt("collapsed_height_dp", 44);
        sbC.setProgress(cw); sbH.setProgress(Math.max(0, ch - 40));
        tvC.setText(cw + "%"); tvH.setText(ch + "dp");
        sbC.setOnSeekBarChangeListener(new S() { @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
            tvC.setText(p + "%"); Prefs.put("collapsed_width_percent", p / 100f); } });
        sbH.setOnSeekBarChangeListener(new S() { @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
            int v = 40 + p; tvH.setText(v + "dp"); Prefs.put("collapsed_height_dp", v); } });
        findViewById(R.id.btn_notify_access).setOnClickListener(v ->
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
        findViewById(R.id.btn_battery).setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()))));
        findViewById(R.id.btn_test_notify).setOnClickListener(v ->
            startService(new Intent(this, com.xuanyin.app.service.IslandService.class).setAction("TEST_NOTIFY")));
        findViewById(R.id.btn_announcement).setOnClickListener(v ->
            startActivity(new Intent(this, AnnouncementActivity.class)));
    }
    static abstract class S implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar s) {}
        @Override public void onStopTrackingTouch(SeekBar s) {}
    }
}
