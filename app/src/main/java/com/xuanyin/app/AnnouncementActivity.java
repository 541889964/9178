package com.xuanyin.app;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.xuanyin.app.util.Haptic;
public class AnnouncementActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle b) {
        try { super.onCreate(b); } catch (Throwable t) { finish(); return; }
        try {
            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(0xFF0A0817);
            root.setPadding(dp(22), dp(56), dp(22), dp(22));
            TextView title = new TextView(this);
            title.setText("玄音公告");
            title.setTextColor(Color.WHITE);
            title.setTextSize(26f);
            title.setTypeface(null, Typeface.BOLD);
            root.addView(title);
            View line = new View(this);
            LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(dp(60), dp(3));
            lineLp.topMargin = dp(10);
            line.setLayoutParams(lineLp);
            GradientDrawable lg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFFFF6B9D, 0xFF9B6BFF});
            lg.setCornerRadius(dp(2));
            line.setBackground(lg);
            root.addView(line);
            ScrollView sv = new ScrollView(this);
            LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            svLp.topMargin = dp(18);
            sv.setLayoutParams(svLp);
            TextView body = new TextView(this);
            body.setTextColor(0xFFC9BFF5);
            body.setTextSize(14f);
            body.setLineSpacing(dp(4), 1.5f);
            StringBuilder sb = new StringBuilder();
            sb.append("感谢选择玄音。\n\n");
            sb.append("玄音是集本地音乐、在线搜索、灵动岛、下载、通知分裂、充电特效、闹钟、录屏、生活模式于一体的音乐播放器。\n\n");
            sb.append("【设计】\n液态玻璃材质，粉紫渐变。所有按钮物理弹簧动画，列表项逐条错峰入场。\n\n");
            sb.append("【灵动岛】\n折叠/展开双模。无歌时生活模式；播放时音乐模式（封面、歌名、两行歌词）；通知来时窗口扩到 98%，主岛靠左，通知卡从右侧滑出；充电时粒子上升。\n\n");
            sb.append("【下载】\n网易云外链，保存到 Music/玄音/，通知栏进度 + 灵动岛实时显示。\n\n");
            sb.append("【性能】\nlargeHeap 内存不限，ViewPropertyAnimator 动画，RecyclerView 视图缓存，Glide 磁盘缓存。\n\n");
            sb.append("【触感】\n轻触 12ms / 长按 34ms / 成功双振 / 警告双振 / 充电专属波形。\n\n");
            sb.append("【隐私】\n不收集信息，不上传数据。\n\n");
            sb.append("愿你被世界温柔以待。\n—— 玄音 v36");
            body.setText(sb.toString());
            sv.addView(body);
            root.addView(sv);
            Button btn = new Button(this);
            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
            btnLp.topMargin = dp(16);
            btn.setLayoutParams(btnLp);
            btn.setText("我已阅读并同意");
            btn.setTextColor(Color.WHITE);
            btn.setTextSize(15f);
            btn.setAllCaps(false);
            btn.setGravity(Gravity.CENTER);
            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFFFF6B9D, 0xFF9B6BFF});
            bg.setCornerRadius(dp(16));
            btn.setBackground(bg);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try { Haptic.tap(v); } catch (Throwable ignored) {}
                    finish();
                }
            });
            root.addView(btn);
            setContentView(root);
        } catch (Throwable t) { finish(); }
    }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
