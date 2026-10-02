package com.xuanyin.app.adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.xuanyin.app.R;
import com.xuanyin.app.model.Song;
import com.xuanyin.app.util.Haptic;
import com.xuanyin.app.util.WallpaperHelper;
import java.util.List;
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.VH> {
    public interface OnClick { void onItem(Song s, int pos); }
    private final List<Song> data;
    private final OnClick click;
    private long currentId = -1;
    private int lastAnimPos = -1;
    public SongAdapter(List<Song> d, OnClick c) { data = d; click = c; }
    public void setCurrent(long id) { currentId = id; notifyDataSetChanged(); }
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_song, p, false));
    }
    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Song s = data.get(pos);
        h.title.setText(s.title);
        h.sub.setText(s.displayArtist() + " · " + s.displayAlbum());
        h.dur.setText(s.durationText());
        WallpaperHelper.loadCover(h.itemView.getContext(), h.cover, s.id);
        h.title.setTextColor(s.id == currentId ? 0xFFFF6B9D : 0xFFFFFFFF);
        if (pos > lastAnimPos) {
            h.itemView.setAlpha(0f);
            h.itemView.setTranslationY(72f);
            h.itemView.setScaleX(0.90f); h.itemView.setScaleY(0.90f);
            h.itemView.setRotationX(-8f);
            h.itemView.setCameraDistance(12000f);
            h.itemView.animate()
                .alpha(1f).translationY(0).scaleX(1f).scaleY(1f).rotationX(0f)
                .setDuration(600).setStartDelay(Math.min(pos, 14) * 42L)
                .setInterpolator(new OvershootInterpolator(0.85f)).start();
            lastAnimPos = pos;
        }
        h.itemView.setOnClickListener(v -> {
            Haptic.tap(v);
            v.setCameraDistance(12000f);
            v.animate().scaleX(0.92f).scaleY(0.92f).rotationX(6f).setDuration(90)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).rotationX(0f)
                    .setDuration(320).setInterpolator(new OvershootInterpolator(2.4f)).start()).start();
            click.onItem(s, h.getAdapterPosition());
        });
        h.itemView.setOnLongClickListener(v -> { Haptic.longPress(v); return true; });
    }
    @Override public int getItemCount() { return data.size(); }
    static class VH extends RecyclerView.ViewHolder {
        ImageView cover; TextView title, sub, dur;
        VH(View v) { super(v);
            cover = v.findViewById(R.id.iv_cover);
            title = v.findViewById(R.id.tv_title);
            sub = v.findViewById(R.id.tv_sub);
            dur = v.findViewById(R.id.tv_dur);
        }
    }
}
