package com.xuanyin.app;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.xuanyin.app.util.Prefs;
public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);

        // 折叠宽度 30~95%
        SeekBar sbCW = findViewById(R.id.sb_collapsed);
        TextView tvCW = findViewById(R.id.tv_collapsed_val);
        int cw = (int)(Prefs.getFloat("collapsed_width_percent", 0.62f) * 100);
        sbCW.setProgress(cw - 30);
        tvCW.setText(cw + "%");
        sbCW.setOnSeekBarChangeListener(new S() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                int v = 30 + p;
                tvCW.setText(v + "%");
                Prefs.put("collapsed_width_percent", v / 100f);
                restartIsland();
            }
        });

        // 折叠高度 36~72dp
        SeekBar sbCH = findViewById(R.id.sb_height);
        TextView tvCH = findViewById(R.id.tv_height_val);
        int ch = Prefs.getInt("collapsed_height_dp", 44);
        sbCH.setProgress(ch - 36);
        tvCH.setText(ch + "dp");
        sbCH.setOnSeekBarChangeListener(new S() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                int v = 36 + p;
                tvCH.setText(v + "dp");
                Prefs.put("collapsed_height_dp", v);
                restartIsland();
            }
        });

        // 展开宽度 60~100%
        SeekBar sbEW = findViewById(R.id.sb_expanded_w);
        TextView tvEW = findViewById(R.id.tv_expanded_w_val);
        int ew = (int)(Prefs.getFloat("expanded_width_percent", 0.94f) * 100);
        sbEW.setProgress(ew - 60);
        tvEW.setText(ew + "%");
        sbEW.setOnSeekBarChangeListener(new S() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                int v = 60 + p;
                tvEW.setText(v + "%");
                Prefs.put("expanded_width_percent", v / 100f);
                restartIsland();
            }
        });

        // 展开高度 120~500dp
        SeekBar sbEH = findViewById(R.id.sb_expanded_h);
        TextView tvEH = findViewById(R.id.tv_expanded_h_val);
        int eh = Prefs.getInt("expanded_height_dp", 260);
        sbEH.setProgress(eh - 120);
        tvEH.setText(eh + "dp");
        sbEH.setOnSeekBarChangeListener(new S() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                int v = 120 + p;
                tvEH.setText(v + "dp");
                Prefs.put("expanded_height_dp", v);
                restartIsland();
            }
        });

        findViewById(R.id.btn_notify_access).setOnClickListener(v ->
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
        findViewById(R.id.btn_battery).setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + getPackageName()))));
        findViewById(R.id.btn_test_notify).setOnClickListener(v ->
            startService(new Intent(this, com.xuanyin.app.service.IslandService.class).setAction("TEST_NOTIFY")));
        findViewById(R.id.btn_announcement).setOnClickListener(v ->
            startActivity(new Intent(this, AnnouncementActivity.class)));
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
