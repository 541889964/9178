package com.xuanyin.app;
import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
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
            iv.setAlpha(0f); iv.setScaleX(0.8f); iv.setScaleY(0.8f);
            iv.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(400).setStartDelay(i * 40L).start();
            iv.setOnClickListener(v -> {
                v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80)
                    .withEndAction(() -> { Prefs.put("bg_index", idx); finish(); }).start();
            });
            grid.addView(iv);
        }
    }
}
