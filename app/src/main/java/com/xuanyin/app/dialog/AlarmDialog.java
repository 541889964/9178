package com.xuanyin.app.dialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.NumberPicker;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.xuanyin.app.R;
import com.xuanyin.app.util.AlarmHelper;
import com.xuanyin.app.util.Haptic;
import com.xuanyin.app.util.Prefs;
public class AlarmDialog extends Dialog {
    public AlarmDialog(@NonNull Context c) { super(c, R.style.Dialog_XuanYin); }
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_alarm);
        if (getWindow() != null) getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        final NumberPicker hour = (NumberPicker) findViewById(R.id.np_hour);
        final NumberPicker min = (NumberPicker) findViewById(R.id.np_min);
        if (hour != null) { hour.setMinValue(0); hour.setMaxValue(23); hour.setValue(Prefs.getInt("alarm_hour", 7)); }
        if (min != null) { min.setMinValue(0); min.setMaxValue(59); min.setValue(Prefs.getInt("alarm_min", 0)); }
        TextView btnCancel = (TextView) findViewById(R.id.btn_cancel);
        TextView btnOk = (TextView) findViewById(R.id.btn_ok);
        TextView btnClear = (TextView) findViewById(R.id.btn_clear);
        if (btnCancel != null) btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { Haptic.tap(v); dismissWithAnim(); }
        });
        if (btnOk != null) btnOk.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Haptic.tap(v);
                int h = hour != null ? hour.getValue() : 7;
                int m = min != null ? min.getValue() : 0;
                AlarmHelper.set(getContext(), h, m);
                dismissWithAnim();
            }
        });
        if (btnClear != null) btnClear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Haptic.tap(v);
                AlarmHelper.cancel(getContext());
                Prefs.put("alarm_hour", -1);
                Prefs.put("alarm_min", -1);
                dismissWithAnim();
            }
        });
        View card = findViewById(R.id.card);
        if (card != null) {
            card.setScaleX(0.84f); card.setScaleY(0.84f); card.setAlpha(0f);
            card.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(340).setInterpolator(new OvershootInterpolator(1.4f)).start();
        }
    }
    public void dismissWithAnim() {
        View card = findViewById(R.id.card);
        if (card == null) { dismiss(); return; }
        card.animate().scaleX(0.9f).scaleY(0.9f).alpha(0f).setDuration(180).withEndAction(new Runnable() {
            @Override public void run() { dismiss(); }
        }).start();
    }
    @Override public void onBackPressed() { dismissWithAnim(); }
}
