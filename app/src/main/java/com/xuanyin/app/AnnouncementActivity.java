package com.xuanyin.app;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
public class AnnouncementActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_announcement);
        StringBuilder sb = new StringBuilder();
        sb.append("感谢选择玄音。\n\n");
        sb.append("玄音是一款集本地音乐、在线搜索、灵动岛、通知分裂、充电特效、闹钟、生活模式于一体的音乐播放器。\n\n");
        sb.append("【设计】\n深空玻璃拟态。粉紫渐变 #FF6B9D → #9B6BFF，配合 #0A0817 深空背景。20dp 大圆角卡片、1dp 白描边、内发光。所有按钮物理弹簧动画，列表项逐条错峰入场。\n\n");
        sb.append("【灵动岛】\n折叠/展开双模自动切换。无歌时生活模式（时钟、日期、电量、闹钟）；播放时音乐模式（旋转封面、歌名、歌词、控制）。充电变绿，通知分裂 5 秒收回。圆角随宽度动态计算，刘海与挖孔自动避让。\n\n");
        sb.append("【性能】\nViewPropertyAnimator 动画，避免每帧 layout；RecyclerView 固定尺寸 + 视图缓存；Glide 磁盘缓存；歌词二分查找；单容器状态机。\n\n");
        sb.append("【隐私】\n不收集任何信息，不上传任何数据。扫描通过 MediaStore，搜索走网易云公开接口，通知监听仅转发到灵动岛，数据全本地。\n\n");
        sb.append("【未来】\n歌词逐行高亮、充电粒子、通知分裂卡、睡眠定时、频谱、平板适配、主题切换。\n\n");
        sb.append("愿你被世界温柔以待。\n—— 玄音开发团队 2026");
        ((TextView) findViewById(R.id.tv_announcement)).setText(sb.toString());
        findViewById(R.id.btn_agree).setOnClickListener(v ->
            v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80)
                .withEndAction(() -> { finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); }).start());
    }
}
