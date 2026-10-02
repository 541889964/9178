package com.xuanyin.app.util;
import android.content.Context;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.xuanyin.app.R;
public class WallpaperHelper {
    private static final int[] RES = {
        R.drawable.sucai_01, R.drawable.sucai_02, R.drawable.sucai_03,
        R.drawable.sucai_04, R.drawable.sucai_05, R.drawable.sucai_06,
        R.drawable.sucai_07, R.drawable.sucai_08, R.drawable.sucai_09,
        R.drawable.sucai_10, R.drawable.sucai_11, R.drawable.sucai_12
    };
    public static int coverFor(long id) { return RES[(int)(Math.abs(id) % RES.length)]; }
    public static void loadCover(Context c, ImageView iv, long id) {
        Glide.with(c).load(coverFor(id))
            .apply(new RequestOptions().transform(new CircleCrop()).diskCacheStrategy(DiskCacheStrategy.ALL))
            .into(iv);
    }
    public static void loadCoverRect(Context c, ImageView iv, long id) {
        Glide.with(c).load(coverFor(id))
            .apply(new RequestOptions().centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL))
            .into(iv);
    }
    public static void loadBackground(Context c, ImageView iv) {
        int idx = Prefs.getInt("bg_index", 0) % RES.length;
        Glide.with(c).load(RES[idx])
            .apply(new RequestOptions().centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL))
            .into(iv);
    }
    public static int[] all() { return RES; }
}
