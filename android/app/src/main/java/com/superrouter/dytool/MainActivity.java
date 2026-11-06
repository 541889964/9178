package com.superrouter.dytool;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 pager;
    private BottomNavigationView nav;
    private VideoFragment videoFrag;
    private MusicFragment musicFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pager = findViewById(R.id.pager);
        nav = findViewById(R.id.nav);

        videoFrag = new VideoFragment();
        musicFrag = new MusicFragment();

        pager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        pager.setOffscreenPageLimit(2);

        pager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return position == 0 ? videoFrag : musicFrag;
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        // 视差 + 深度位移 + 渐进透明度，营造层叠纵深质感
        pager.setPageTransformer((page, position) -> {
            float abs = Math.abs(position);
            float width = page.getWidth();

            // 横向视差：相邻页以 35% 速度跟随，产生景深
            page.setTranslationX(-position * width * 0.35f);

            // 缩放：1.0 → 0.82
            float scale = 1f - abs * 0.18f;
            page.setScaleX(scale);
            page.setScaleY(scale);

            // 透明度：1.0 → 0.35
            page.setAlpha(1f - abs * 0.65f);

            // 圆角随滑动收放
            float radius = abs * 28f;
            page.setOutlineProvider(null);
            page.setClipToOutline(false);
        });

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                nav.getMenu().getItem(position).setChecked(true);
            }
        });

        nav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            int target = id == R.id.tab_video ? 0 : 1;
            if (pager.getCurrentItem() != target) {
                pager.setCurrentItem(target, true);
            }
            nav.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            return true;
        });

        startKeepAlive();
        handleShare(getIntent());
    }

    private void startKeepAlive() {
        try {
            Intent i = new Intent(this, KeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
            else startService(i);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleShare(intent);
    }

    private void handleShare(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (TextUtils.isEmpty(text)) return;

        pager.setCurrentItem(0, false);
        if (videoFrag != null && videoFrag.getView() != null) {
            videoFrag.getView().postDelayed(() -> videoFrag.parseFromText(text), 350);
        }
    }

    @Override
    public void onBackPressed() {
        if (pager.getCurrentItem() != 0) {
            pager.setCurrentItem(0, true);
            return;
        }
        super.onBackPressed();
    }
}
