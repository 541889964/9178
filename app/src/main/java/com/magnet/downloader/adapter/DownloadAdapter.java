package com.magnet.downloader.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.magnet.downloader.R;
import com.magnet.downloader.model.DownloadItem;
import com.magnet.downloader.util.Haptic;
import java.util.List;

/**
 * 下载列表 Adapter
 * - 交错入场动画
 * - 3D 倾斜滚动
 * - 触摸按下回弹
 */
public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.VH> {

    public interface OnClick { void onClick(DownloadItem item, int pos); }

    private final List<DownloadItem> data;
    private final OnClick click;
    private int lastAnimPos = -1;

    public DownloadAdapter(List<DownloadItem> data, OnClick click) {
        this.data = data;
        this.click = click;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View view = LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_download, p, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        final DownloadItem item = data.get(pos);
        h.title.setText(item.title);
        h.sub.setText(item.sub + " · " + item.displaySize());
        h.status.setText(item.status);

        if (item.progress > 0 && item.progress < 100) {
            h.pb.setVisibility(View.VISIBLE);
            h.pb.setProgress(item.progress);
        } else {
            h.pb.setVisibility(View.GONE);
        }

        // 交错入场
        if (pos > lastAnimPos) {
            h.itemView.setAlpha(0f);
            h.itemView.setTranslationY(72f);
            h.itemView.setScaleX(0.90f);
            h.itemView.setScaleY(0.90f);
            h.itemView.animate()
                    .alpha(1f).translationY(0).scaleX(1f).scaleY(1f)
                    .setDuration(600)
                    .setStartDelay(Math.min(pos, 14) * 42L)
                    .setInterpolator(new OvershootInterpolator(0.85f))
                    .start();
            lastAnimPos = pos;
        }

        // 3D 倾斜滚动效果
        h.itemView.setCameraDistance(h.itemView.getResources().getDisplayMetrics().density * 1000);
        h.itemView.setRotationX(0f);
        h.itemView.setTranslationZ(0f);

        // 触摸按下特效
        h.itemView.setOnTouchListener((v, ev) -> {
            switch (ev.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.94f).scaleY(0.94f)
                            .setDuration(120)
                            .setInterpolator(new OvershootInterpolator(0.5f))
                            .start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f)
                            .setDuration(320)
                            .setInterpolator(new OvershootInterpolator(2.4f))
                            .start();
                    break;
            }
            return false;
        });

        h.itemView.setOnClickListener(v -> {
            Haptic.tap(v);
            v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f)
                            .setDuration(320)
                            .setInterpolator(new OvershootInterpolator(2.4f))
                            .start()).start();
            if (click != null) click.onClick(item, h.getAdapterPosition());
        });

        h.itemView.setOnLongClickListener(v -> {
            Haptic.longPress(v);
            return true;
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        FrameLayout cover; ImageView iv; TextView title, sub, status;
        ProgressBar pb;
        VH(View v) {
            super(v);
            title = v.findViewById(R.id.tv_title);
            sub = v.findViewById(R.id.tv_sub);
            status = v.findViewById(R.id.tv_status);
            pb = v.findViewById(R.id.pb_progress);
            iv = v.findViewById(R.id.iv_cover);
        }
    }
}
