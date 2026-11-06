package com.superrouter.dytool;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 图片/动图浏览：左右滑动切换，动图可一键转视频保存 */
public class ImageViewerActivity extends Activity {

    public static final String EXTRA_JSON = "pics_json";
    public static final String EXTRA_TITLE = "title";

    private final List<DyItem.Pic> pics = new ArrayList<>();
    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    private ViewPager2 pager;
    private TextView counter;
    private MaterialButton saveBtn;
    private String baseName = "douyin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        pager = findViewById(R.id.ivPager);
        counter = findViewById(R.id.ivCounter);
        saveBtn = findViewById(R.id.ivSave);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title != null && !title.isEmpty()) {
            baseName = DyItem.sanitize(title);
            if (baseName.length() > 40) baseName = baseName.substring(0, 40);
        }

        String json = getIntent().getStringExtra(EXTRA_JSON);
        parsePics(json);

        if (pics.isEmpty()) {
            Toast.makeText(this, "没有可查看的图片", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        pager.setAdapter(new PicAdapter());
        pager.setOffscreenPageLimit(1);
        pager.setPageTransformer((page, position) -> {
            float abs = Math.abs(position);
            page.setAlpha(1f - abs * 0.45f);
            page.setScaleX(0.92f + (1 - abs) * 0.08f);
            page.setScaleY(0.92f + (1 - abs) * 0.08f);
        });

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateCounter(position);
            }
        });

        updateCounter(0);
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
        saveBtn.setOnClickListener(v -> saveCurrent());
    }

    private void updateCounter(int pos) {
        int total = pics.size();
        counter.setText(String.format("%d / %d", pos + 1, total));
        DyItem.Pic p = pics.get(pos);
        saveBtn.setText(p.isAnimated() ? "转视频" : "保存");
    }

    private void parsePics(String json) {
        if (json == null || json.isEmpty()) return;
        try {
            org.json.JSONArray arr = new org.json.JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                DyItem.Pic p = new DyItem.Pic();
                p.imageUrl = o.optString("img", "");
                p.videoUrl = o.optString("video", "");
                if (!p.imageUrl.isEmpty()) pics.add(p);
            }
        } catch (Exception ignored) {
        }
    }

    private void saveCurrent() {
        int pos = pager.getCurrentItem();
        if (pos < 0 || pos >= pics.size()) return;
        final DyItem.Pic p = pics.get(pos);

        saveBtn.setEnabled(false);
        Toast.makeText(this, p.isAnimated() ? "正在转视频…" : "正在保存…", Toast.LENGTH_SHORT).show();

        pool.execute(() -> {
            try {
                String path;
                if (p.isAnimated()) {
                    // 动图 → 视频：下载动图的视频源直接保存为 MP4
                    File dest = new File(App.outDir(), baseName + "_" + (pos + 1) + ".mp4");
                    List<String> urls = new ArrayList<>();
                    urls.add(p.videoUrl);
                    File f = Downloader.download(urls, dest, null, false);
                    path = MediaSaver.save(this, f, 0);
                } else {
                    // 静态图 → 图片库
                    File dest = new File(App.outDir(), baseName + "_" + (pos + 1) + ".jpg");
                    List<String> urls = new ArrayList<>();
                    urls.add(p.imageUrl);
                    File f = Downloader.download(urls, dest, null, true);
                    path = MediaSaver.save(this, f, 2);
                }
                final String ok = path;
                runOnUiThread(() -> {
                    saveBtn.setEnabled(true);
                    Toast.makeText(this, "已保存到 " + ok, Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? "保存失败" : e.getMessage();
                runOnUiThread(() -> {
                    saveBtn.setEnabled(true);
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pool.shutdownNow();
    }

    private class PicAdapter extends RecyclerView.Adapter<PicAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_image, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            DyItem.Pic p = pics.get(position);
            h.loading.setVisibility(View.VISIBLE);
            h.gifBadge.setVisibility(p.isAnimated() ? View.VISIBLE : View.GONE);

            Glide.with(ImageViewerActivity.this)
                    .load(p.imageUrl)
                    .apply(new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .fitCenter())
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirst) {
                            h.loading.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target, DataSource src, boolean first) {
                            h.loading.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(h.image);
        }

        @Override
        public int getItemCount() {
            return pics.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView image;
            ProgressBar loading;
            TextView gifBadge;

            VH(@NonNull View v) {
                super(v);
                image = v.findViewById(R.id.ivFull);
                loading = v.findViewById(R.id.ivFullLoading);
                gifBadge = v.findViewById(R.id.ivGifBadge);
            }
        }
    }
}
