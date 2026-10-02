package com.xuanyin.app;
import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.xuanyin.app.util.Haptic;
import com.xuanyin.app.util.Prefs;
import com.xuanyin.app.util.WallpaperHelper;
public class GalleryActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_gallery);
        GridLayout grid = findViewById(R.id.grid);
        int[] res = WallpaperHelper.all();
        int size = (int)(getResources().getDisplayMetrics().density * 100);
        for (int i = 0; i < res.length; i++) {
            final int idx = i;
            ImageView iv = new ImageView(this);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = size; lp.height = size; lp.setMargins(12, 12, 12, 12);
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageResource(res[i]);
            iv.setAlpha(0f); iv.setScaleX(0.7f); iv.setScaleY(0.7f);
            iv.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(500).setStartDelay(i * 45L)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f)).start();
            iv.setOnClickListener(v -> {
                Haptic.tap(v);
                v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80)
                    .withEndAction(() -> { Prefs.put("bg_index", idx); finish(); }).start();
            });
            grid.addView(iv);
        }
    }
}
