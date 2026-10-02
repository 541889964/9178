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
        NumberPicker hour = findViewById(R.id.np_hour);
        NumberPicker min = findViewById(R.id.np_min);
        hour.setMinValue(0); hour.setMaxValue(23);
        min.setMinValue(0); min.setMaxValue(59);
        hour.setValue(Prefs.getInt("alarm_hour", 7));
        min.setValue(Prefs.getInt("alarm_min", 0));
        TextView btnCancel = findViewById(R.id.btn_cancel);
        TextView btnOk = findViewById(R.id.btn_ok);
        TextView btnClear = findViewById(R.id.btn_clear);
        btnCancel.setOnClickListener(v -> { Haptic.tap(v); dismissWithAnim(); });
        btnOk.setOnClickListener(v -> {
            Haptic.tap(v);
            AlarmHelper.set(getContext(), hour.getValue(), min.getValue());
            dismissWithAnim();
        });
        btnClear.setOnClickListener(v -> {
            Haptic.tap(v);
            Prefs.put("alarm_hour", -1);
            Prefs.put("alarm_min", -1);
            dismissWithAnim();
        });
        View card = findViewById(R.id.card);
        card.setScaleX(0.84f); card.setScaleY(0.84f); card.setAlpha(0f);
        card.animate().scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(340).setInterpolator(new OvershootInterpolator(1.4f)).start();
    }
    public void dismissWithAnim() {
        View card = findViewById(R.id.card);
        card.animate().scaleX(0.9f).scaleY(0.9f).alpha(0f)
            .setDuration(180).withEndAction(this::dismiss).start();
    }
    @Override public void onBackPressed() { dismissWithAnim(); }
}
