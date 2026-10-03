package com.magnet.downloader.adapter;

import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import com.magnet.downloader.model.DownloadTask;
import com.magnet.downloader.util.Haptic;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {

    public interface Callback {
        void onOpen(DownloadTask t);
        void onCancel(DownloadTask t);
        void onRetry(DownloadTask t);
        void onDelete(DownloadTask t);
    }

    private final List<DownloadTask> data;
    private final Callback cb;
    private int lastAnimPos = -1;

    public TaskAdapter(List<DownloadTask> data, Callback cb) {
        this.data = data;
        this.cb = cb;
        setHasStableIds(true);
    }

    @Override public long getItemId(int pos) { return data.get(pos).id; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        return new VH(LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_task, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        DownloadTask t = data.get(pos);

        h.title.setText(t.fileName == null ? t.url : t.fileName);
        h.title.setSelected(true);

        // 副标题 + 进度 + 速度
        if (t.state == DownloadTask.STATE_DONE) {
            h.sub.setText("已保存到 " + t.savePath);
            h.pct.setText("✓");
            h.pct.setTextColor(0xFF30D158);
        } else if (t.state == DownloadTask.STATE_ERROR) {
            h.sub.setText("失败: " + (t.error == null ? "未知" : t.error));
            h.pct.setText("✗");
            h.pct.setTextColor(0xFFFF3B30);
        } else if (t.state == DownloadTask.STATE_CANCELLED) {
            h.sub.setText("已取消");
            h.pct.setText("—");
            h.pct.setTextColor(0xFF8F86B8);
        } else {
            String sub;
            int p = t.percent();
            if (p >= 0) {
                sub = t.displayDownloaded() + " / " + t.displaySize();
                h.pct.setText(p + "%");
            } else {
                sub = t.displayDownloaded();
                h.pct.setText("...");
            }
            if (t.speedBps > 0) sub += "  ·  " + t.displaySpeed();
            h.sub.setText(sub);
            h.pct.setTextColor(0xFFFF6B9D);
        }

        // 进度条
        int pct = t.percent();
        if (pct >= 0 && t.state != DownloadTask.STATE_DONE) {
            h.pb.setVisibility(View.VISIBLE);
            h.pb.setProgress(pct);
        } else {
            h.pb.setVisibility(View.GONE);
        }

        // 左侧图标
        String ext = extOf(t.fileName == null ? t.url : t.fileName);
        h.ext.setText(ext);
        h.ext.setTextColor(0xFFFF6B9D);

        // 入场
        if (pos > lastAnimPos) {
            h.itemView.setAlpha(0f);
            h.itemView.setTranslationY(72f);
            h.itemView.setScaleX(0.9f);
            h.itemView.setScaleY(0.9f);
            h.itemView.animate().alpha(1f).translationY(0).scaleX(1f).scaleY(1f)
                    .setDuration(600)
                    .setStartDelay(Math.min(pos, 14) * 42L)
                    .setInterpolator(new OvershootInterpolator(0.85f))
                    .start();
            lastAnimPos = pos;
        }

        // 触摸回弹
        h.itemView.setOnTouchListener((v, ev) -> {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100)
                            .setInterpolator(new OvershootInterpolator(0.5f)).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(300)
                            .setInterpolator(new OvershootInterpolator(2.4f)).start();
                    break;
            }
            return false;
        });

        h.itemView.setOnClickListener(v -> {
            Haptic.tap(v);
            if (t.state == DownloadTask.STATE_DONE) cb.onOpen(t);
            else if (t.state == DownloadTask.STATE_ERROR) cb.onRetry(t);
        });

        h.itemView.setOnLongClickListener(v -> {
            Haptic.tap(v);
            if (t.state == DownloadTask.STATE_DONE) cb.onDelete(t);
            else cb.onCancel(t);
            return true;
        });
    }

    private String extOf(String name) {
        if (name == null) return "FILE";
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "FILE";
        String e = name.substring(dot + 1).toUpperCase();
        return e.length() > 4 ? e.substring(0, 4) : e;
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        FrameLayout cover;
        TextView ext, title, sub, pct;
        ProgressBar pb;
        VH(View v) {
            super(v);
            cover = v.findViewById(R.id.cover);
            ext = v.findViewById(R.id.tv_ext);
            title = v.findViewById(R.id.tv_title);
            sub = v.findViewById(R.id.tv_sub);
            pct = v.findViewById(R.id.tv_pct);
            pb = v.findViewById(R.id.pb);
        }
    }
}
