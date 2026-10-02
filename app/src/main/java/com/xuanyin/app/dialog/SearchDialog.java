package com.xuanyin.app.dialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.xuanyin.app.R;
import com.xuanyin.app.util.Haptic;
public class SearchDialog extends Dialog {
    public interface OnSearch { void onSearch(String k); }
    private final OnSearch cb;
    public SearchDialog(@NonNull Context c, OnSearch cb) { super(c, R.style.Dialog_XuanYin); this.cb = cb; }
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_search);
        if (getWindow() != null) getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        EditText et = findViewById(R.id.et_query);
        TextView btnOk = findViewById(R.id.btn_ok);
        TextView btnCancel = findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(v -> { Haptic.tap(v); dismissWithAnim(); });
        btnOk.setOnClickListener(v -> {
            Haptic.tap(v);
            String k = et.getText().toString().trim();
            if (!k.isEmpty()) cb.onSearch(k);
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
