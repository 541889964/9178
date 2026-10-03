package com.magnet.downloader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.magnet.downloader.adapter.DownloadAdapter;
import com.magnet.downloader.dialog.AnnouncementDialog;
import com.magnet.downloader.model.DownloadItem;
import com.magnet.downloader.util.Haptic;
import com.magnet.downloader.util.Prefs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private final List<DownloadItem> list = new ArrayList<>();
    private DownloadAdapter adapter;
    private TextView tvTab1, tvTab2, tvTab1Icon, tvTab2Icon;
    private View indicator1, indicator2;
    private int currentTab = 0;
    private final Handler clockH = new Handler(Looper.getMainLooper());

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            updateGreeting();
            clockH.postDelayed(this, 60000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.init(this);
        setContentView(R.layout.activity_main);

        ImageView ivBg = findViewById(R.id.iv_bg);
        ivBg.setAlpha(0.30f);

        updateGreeting();

        TextView tvQuote = findViewById(R.id.tv_quote);
        tvQuote.setText("生活明朗，万物可爱");

        // 交错入场
        enter(R.id.card_greet, 0);
        enter(R.id.card_actions, 70);
        enter(R.id.card_search, 140);

        // 快捷按钮
        bind(R.id.btn_local);
        bind(R.id.btn_island);
        bind(R.id.btn_settings);

        // 列表
        RecyclerView rv = findViewById(R.id.rv_list);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setInitialPrefetchItemCount(10);
        rv.setLayoutManager(lm);
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(28);
        rv.setItemAnimator(null);

        adapter = new DownloadAdapter(list, (item, pos) -> {
            // TODO 打开下载详情
        });
        rv.setAdapter(adapter);

        // 假数据
        Random rnd = new Random();
        String[] names = {
            "Ubuntu-22.04.iso", "Android-Studio.dmg", "Python-3.12.tar.gz",
            "NodeJS-20.10.msi", "Kotlin-1.9.20.zip", "Docker-24.0.deb",
            "Git-2.43.pkg", "VSCode-1.85.zip", "FFmpeg-6.0.7z",
            "OpenJDK-17.tar.gz"
        };
        for (int i = 0; i < names.length; i++) {
            DownloadItem item = new DownloadItem();
            item.title = names[i];
            item.sub = "HTTP · 多线程";
            item.totalBytes = (long) (50 + rnd.nextInt(500)) * 1024 * 1024;
            item.downloadedBytes = (long) (item.totalBytes * (rnd.nextInt(100) / 100.0));
            item.progress = (int)(item.downloadedBytes * 100 / item.totalBytes);
            item.status = item.progress >= 100 ? "✓" : item.progress + "%";
            item.url = "https://example.com/file" + i + ".zip";
            list.add(item);
        }
        adapter.notifyDataSetChanged();

        TextView tvCount = findViewById(R.id.tv_count);
        tvCount.setText(list.size() + " 项");

        // Tab
        tvTab1 = findViewById(R.id.tv_tab1);
        tvTab2 = findViewById(R.id.tv_tab2);
        tvTab1Icon = findViewById(R.id.tv_tab1_icon);
        tvTab2Icon = findViewById(R.id.tv_tab2_icon);
        indicator1 = findViewById(R.id.indicator1);
        indicator2 = findViewById(R.id.indicator2);

        findViewById(R.id.tab_local).setOnClickListener(v -> { Haptic.tap(v); switchTab(0); });
        findViewById(R.id.tab_online).setOnClickListener(v -> { Haptic.tap(v); switchTab(1); });

        // 公告
        if (!Prefs.getBoolean("announced_v1", false)) {
            String content = getString(R.string.app_name) + "\n\n" + ANNOUNCEMENT;
            new AnnouncementDialog(this, ANNOUNCEMENT).show();
            Prefs.put("announced_v1", true);
        }

        clockH.post(clockTick);
    }

    private void updateGreeting() {
        Calendar c = Calendar.getInstance();
        int h = c.get(Calendar.HOUR_OF_DAY);
        TextView tvGreet = findViewById(R.id.tv_greet);
        if (h < 6) tvGreet.setText("凌晨好呀 🌙");
        else if (h < 12) tvGreet.setText("早上好呀 ☀️");
        else if (h < 14) tvGreet.setText("中午好呀 🍜");
        else if (h < 18) tvGreet.setText("下午好呀 ☕");
        else if (h < 22) tvGreet.setText("晚上好呀 🌆");
        else tvGreet.setText("夜深了 🌌");
    }

    private void enter(int viewId, long delay) {
        View v = findViewById(viewId);
        if (v == null) return;
        v.setAlpha(0f);
        v.setTranslationY(60f);
        v.setScaleX(0.92f);
        v.setScaleY(0.92f);
        v.animate().alpha(1f).translationY(0).scaleX(1f).scaleY(1f)
                .setDuration(540).setStartDelay(delay)
                .setInterpolator(new OvershootInterpolator(1.0f)).start();
    }

    private void bind(int viewId) {
        View v = findViewById(viewId);
        if (v == null) return;
        v.setOnClickListener(x -> {
            Haptic.tap(x);
            x.animate().cancel();
            x.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80)
                    .withEndAction(() -> x.animate().scaleX(1f).scaleY(1f)
                            .setDuration(320)
                            .setInterpolator(new OvershootInterpolator(2.4f))
                            .start()).start();
        });
    }

    private void switchTab(int tab) {
        if (currentTab == tab) return;
        currentTab = tab;
        if (tab == 0) {
            tvTab1.setTextColor(0xFFFFFFFF); tvTab1Icon.setTextColor(0xFFFFFFFF);
            tvTab2.setTextColor(0xFF8F86B8); tvTab2Icon.setTextColor(0xFF8F86B8);
            indicator1.setVisibility(View.VISIBLE);
            indicator2.setVisibility(View.INVISIBLE);
        } else {
            tvTab1.setTextColor(0xFF8F86B8); tvTab1Icon.setTextColor(0xFF8F86B8);
            tvTab2.setTextColor(0xFFFFFFFF); tvTab2Icon.setTextColor(0xFFFFFFFF);
            indicator1.setVisibility(View.INVISIBLE);
            indicator2.setVisibility(View.VISIBLE);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        clockH.removeCallbacks(clockTick);
    }

    // ==================== 公告 2000 字 ====================
    private static final String ANNOUNCEMENT =
        "欢迎使用【磁力快下】，一款为速度与美感而生的下载工具。\n\n" +
        "我们生活在一个信息唾手可得的时代，每天都会遇到想保存的视频、想收藏的音乐、想留存的文档、想珍藏的图片。但传统的下载方式总让人失望——要么速度慢得让人抓狂，要么广告满天飞，要么在后台偷偷上传你的隐私。我们做这款工具，就是想让下载这件事回归本质：简单、快速、干净、优雅。\n\n" +
        "【关于我们】\n" +
        "磁力快下由一支四人小团队开发。我们不融资，不投放广告，不追求日活数据。我们只想做一款自己每天愿意用的工具，然后把它分享给同样在意效率与美感的你。\n\n" +
        "【核心功能】\n" +
        "我们支持 HTTP、HTTPS、FTP、磁力链接、BT 种子等多种下载协议。无论你想下载的是普通网页文件，还是 P2P 资源，都能一站式搞定。多线程分片下载技术让你的网速跑满，不再浪费每一兆带宽。断点续传让你随时暂停、随时继续，地铁上、电梯里、信号切换时都不会中断你的下载任务。智能限速让你在下载的同时，依然能流畅看视频、开会、玩游戏。\n\n" +
        "【UI 设计】\n" +
        "我们花了三个月时间打磨界面。每一张卡片都是真正的液态玻璃质感，不是简单的半透明色块。它有底层、中层、顶层三层结构：底层是极浅的半透明白色，让背景隐约可见；中层是顶部到中间的柔和高光渐变，模拟玻璃反光；顶层是细致的 1dp 白色描边，模拟玻璃边缘。少了任何一层，卡片就会显得又平又假。整个应用以深紫色为主调，粉紫渐变作为点缀，配合柔和的极光动画背景。三团彩色光斑在屏幕上缓慢游动，周期长达 300 秒，你不会注意到它在动，但一旦停下来，就会觉得少了什么。\n\n" +
        "【交互动效】\n" +
        "所有按钮都采用物理弹簧动画，按下时迅速缩小到 92%，松开时带一点点过冲弹回，就像真的按到了一个有弹性的按钮。整个动画只有 400 毫秒——比你眨一次眼睛还快——但就是这一瞬间，让你感受到『这个应用是活的』。列表项采用交错入场动画，从上到下依次落下，每一条延迟 42 毫秒，营造出瀑布般的流动感。触摸列表项时，卡片会轻微下压，松开时带过冲回弹，跟你手指的动作严丝合缝。\n\n" +
        "【隐私承诺】\n" +
        "我们不收集你的任何下载记录。你的下载历史保存在本机数据库，从不上传到服务器。我们不分析你的使用习惯，不推送广告，不做任何形式的用户画像。这是一款纯粹的、只为你服务的工具。应用需要存储权限只是为了把文件保存到你指定的目录。应用需要网络权限只是为了下载文件本身。除此之外，没有任何隐藏权限，没有任何后台进程，没有任何 SDK 追踪。\n\n" +
        "【性能优化】\n" +
        "我们使用了最新的 RecyclerView 复用机制，即使你有上千条下载记录，滚动依然丝滑流畅。图片加载使用三级缓存（内存、磁盘、网络），即使在弱网环境下也能快速显示封面。我们自研的动画调度器会把所有动画对齐到垂直同步信号，杜绝撕裂和掉帧。整个应用在千元机上都能稳定 60 帧运行。\n\n" +
        "【使用建议】\n" +
        "推荐在 WiFi 环境下使用多线程下载，移动网络下会自动切换为单线程省流模式。对于大型文件，建议开启『夜间自动下载』，让手机在你睡觉的时候默默工作。对于磁力链接，建议先添加任务再批量开始，避免同时启动太多连接拖慢整体速度。\n\n" +
        "【关于未来】\n" +
        "我们还会持续优化：云加速节点、浏览器插件、投屏下载、远程任务管理、NAS 直连、离线播放等功能都在开发计划中。我们会保持每两周一次的小版本更新，每月一次的大版本更新。\n\n" +
        "【致谢】\n" +
        "感谢你选择磁力快下。愿你每一次下载，都又快又稳又美好。";

}
