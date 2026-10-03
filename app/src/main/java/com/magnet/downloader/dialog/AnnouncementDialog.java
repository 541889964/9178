package com.magnet.downloader.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.magnet.downloader.R;

public class AnnouncementDialog extends Dialog {
    private String content;

    public AnnouncementDialog(@NonNull Context context, String content) {
        super(context, R.style.Dialog_Transparent);
        this.content = content;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_announcement);
        setCanceledOnTouchOutside(true);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.width = (int)(getContext().getResources().getDisplayMetrics().widthPixels * 0.88f);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            getWindow().setAttributes(lp);
            getWindow().setDimAmount(0.7f);
        }

        TextView tv = findViewById(R.id.tv_content);
        if (tv != null && content != null) tv.setText(content);

        FrameLayout card = findViewById(R.id.card);
        if (card != null) {
            card.setScaleX(0.8f); card.setScaleY(0.8f);
            card.setAlpha(0f); card.setTranslationY(60f);
            card.animate().scaleX(1f).scaleY(1f).alpha(1f).translationY(0f)
                    .setDuration(420)
                    .setInterpolator(new OvershootInterpolator(1.2f)).start();
        }

        View btn = findViewById(R.id.btn_ok);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80)
                        .withEndAction(() -> {
                            v.animate().scaleX(1f).scaleY(1f).setDuration(240)
                                    .setInterpolator(new OvershootInterpolator(2f)).start();
                            v.postDelayed(this::dismissWithAnim, 200);
                        }).start();
            });
        }
    }

    private void dismissWithAnim() {
        FrameLayout card = findViewById(R.id.card);
        if (card != null) {
            card.animate().scaleX(0.92f).scaleY(0.92f).alpha(0f).setDuration(200)
                    .withEndAction(this::dismiss).start();
        } else dismiss();
    }

    @Override
    public void onBackPressed() { dismissWithAnim(); }
}
