package com.xuanyin.app;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.xuanyin.app.util.Anim;
import com.xuanyin.app.util.Haptic;
public class AnnouncementActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_announcement);
        StringBuilder sb = new StringBuilder();
        sb.append("感谢选择玄音。\n\n");
        sb.append("玄音是集本地音乐、在线搜索、灵动岛、下载、通知分裂、充电特效、闹钟、生活模式于一体的音乐播放器。\n\n");
        sb.append("【设计】\n深空玻璃拟态。粉紫渐变 #FF6B9D → #9B6BFF，配合 #0A0817 深空背景。20dp 圆角卡片、1dp 白描边、内发光。所有按钮物理弹簧动画，列表项逐条错峰入场。\n\n");
        sb.append("【灵动岛】\n折叠/展开双模。无歌时生活模式（时钟、日期、电量、闹钟）；播放时音乐模式（封面、歌名、控制）；下载时显示进度。充电变绿，通知 4 秒收回。\n\n");
        sb.append("【下载】\n网易云外链，保存到 Music/玄音/，通知栏进度 + 灵动岛实时显示。\n\n");
        sb.append("【性能】\nlargeHeap 内存不限，ViewPropertyAnimator 动画，RecyclerView 视图缓存，Glide 磁盘缓存，歌词二分查找，单容器状态机。\n\n");
        sb.append("【触感】\n轻触 12ms / 长按 34ms / 成功双振 / 警告双振 / 展开收起各有专属波形。\n\n");
        sb.append("【隐私】\n不收集信息，不上传数据。\n\n");
        sb.append("愿你被世界温柔以待。\n—— 玄音 v17");
        TextView tv = findViewById(R.id.tv_announcement);
        tv.setText(sb.toString());
        Anim.enter(tv, 100);
        View btn = findViewById(R.id.btn_agree);
        Anim.fadeIn(btn, 500);
        btn.setOnClickListener(v -> {
            Haptic.tap(v);
            Anim.press(v);
            v.postDelayed(() -> { finish(); overridePendingTransition(R.anim.fade_in, R.anim.fade_out); }, 200);
        });
    }
}
