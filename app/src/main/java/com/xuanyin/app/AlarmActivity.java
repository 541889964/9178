package com.xuanyin.app;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.xuanyin.app.util.AlarmHelper;
import com.xuanyin.app.util.Haptic;
import com.xuanyin.app.util.Prefs;
import com.xuanyin.app.util.RingtoneHelper;
public class AlarmActivity extends AppCompatActivity {
    private NumberPicker hour, min;
    private TextView[] days = new TextView[7];
    private TextView tvFade, tvRingtone, tvCurrent, btnSave, btnOff;
    private LinearLayout ringtonePicker;
    private static final String[] DAY_LABELS = {"一","二","三","四","五","六","日"};
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_alarm);
        findViewById(R.id.btn_alarm_back).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { Haptic.tap(v); finish(); }
        });
        hour = (NumberPicker) findViewById(R.id.np_hour);
        min = (NumberPicker) findViewById(R.id.np_min);
        hour.setMinValue(0); hour.setMaxValue(23);
        min.setMinValue(0); min.setMaxValue(59);
        hour.setValue(Prefs.getInt("alarm_hour", 7));
        min.setValue(Prefs.getInt("alarm_min", 0));
        int[] dayIds = new int[]{R.id.al_mon, R.id.al_tue, R.id.al_wed, R.id.al_thu, R.id.al_fri, R.id.al_sat, R.id.al_sun};
        for (int i = 0; i < 7; i++) {
            final int idx = i;
            days[i] = (TextView) findViewById(dayIds[i]);
            final TextView d = days[i];
            boolean on = Prefs.getInt("alarm_week_" + i, 0) == 1;
            d.setAlpha(on ? 1f : 0.4f);
            d.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Haptic.tap(v);
                    boolean now = Prefs.getInt("alarm_week_" + idx, 0) == 1;
                    Prefs.put("alarm_week_" + idx, now ? 0 : 1);
                    d.setAlpha(now ? 0.4f : 1f);
                }
            });
        }
        tvFade = (TextView) findViewById(R.id.tv_fade);
        boolean fade = Prefs.getInt("alarm_fade", 1) == 1;
        tvFade.setAlpha(fade ? 1f : 0.4f);
        tvFade.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Haptic.tap(v);
                boolean now = Prefs.getInt("alarm_fade", 1) == 1;
                Prefs.put("alarm_fade", now ? 0 : 1);
                tvFade.setAlpha(now ? 0.4f : 1f);
            }
        });
        tvRingtone = (TextView) findViewById(R.id.tv_ringtone);
        refreshRingtone();
        ringtonePicker = (LinearLayout) findViewById(R.id.row_ringtone);
        if (ringtonePicker != null) ringtonePicker.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { Haptic.tap(v); pickRingtone(); }
        });
        tvCurrent = (TextView) findViewById(R.id.tv_alarm_current);
        refreshCurrent();
        btnSave = (TextView) findViewById(R.id.btn_alarm_save);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Haptic.done(AlarmActivity.this);
                int h = hour.getValue(), m = min.getValue();
                AlarmHelper.set(AlarmActivity.this, h, m);
                Toast.makeText(AlarmActivity.this, "闹钟已设为 " + String.format("%02d:%02d", h, m), Toast.LENGTH_SHORT).show();
                refreshCurrent();
            }
        });
        btnOff = (TextView) findViewById(R.id.btn_alarm_off);
        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Haptic.tap(v);
                AlarmHelper.cancel(AlarmActivity.this);
                Prefs.put("alarm_hour", -1);
                Prefs.put("alarm_min", -1);
                Toast.makeText(AlarmActivity.this, "已关闭闹钟", Toast.LENGTH_SHORT).show();
                refreshCurrent();
            }
        });
    }
    private void refreshRingtone() {
        String name = Prefs.getString("alarm_ringtone_name", "系统默认");
        if (tvRingtone != null) tvRingtone.setText(name);
    }
    private void refreshCurrent() {
        if (tvCurrent == null) return;
        int h = Prefs.getInt("alarm_hour", -1);
        int m = Prefs.getInt("alarm_min", -1);
        if (h < 0 || m < 0) { tvCurrent.setText("当前无闹钟"); return; }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%02d:%02d", h, m));
        StringBuilder rd = new StringBuilder();
        for (int i = 0; i < 7; i++) if (Prefs.getInt("alarm_week_" + i, 0) == 1) rd.append(DAY_LABELS[i]).append(" ");
        if (rd.length() > 0) sb.append("  ").append(rd.toString().trim());
        tvCurrent.setText("当前: " + sb.toString());
    }
    private void pickRingtone() {
        try {
            Intent i = new Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER);
            i.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM);
            i.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "选择闹钟铃声");
            i.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
            startActivityForResult(i, 200);
        } catch (Throwable t) {
            Toast.makeText(this, "无法打开铃声选择器", Toast.LENGTH_SHORT).show();
        }
    }
    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 200 && res == RESULT_OK && data != null) {
            try {
                Uri uri = data.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (uri != null) {
                    android.media.RingtoneManager.setActualDefaultRingtoneUri(this, android.media.RingtoneManager.TYPE_ALARM, uri);
                    Prefs.put("alarm_ringtone", uri.toString());
                    String name = uri.getLastPathSegment();
                    try {
                        android.media.Ringtone r = android.media.RingtoneManager.getRingtone(this, uri);
                        if (r != null && r.getTitle(this) != null) name = r.getTitle(this);
                    } catch (Throwable ignored) {}
                    Prefs.put("alarm_ringtone_name", name);
                    refreshRingtone();
                }
            } catch (Throwable ignored) {}
        }
    }
}
