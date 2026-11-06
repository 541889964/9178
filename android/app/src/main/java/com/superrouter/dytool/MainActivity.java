package com.superrouter.dytool;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView nav;
    private Fragment videoFrag;
    private Fragment musicFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nav = findViewById(R.id.nav);
        videoFrag = new VideoFragment();
        musicFrag = new MusicFragment();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, videoFrag, "video")
                    .add(R.id.container, musicFrag, "music")
                    .hide(musicFrag)
                    .commit();
        } else {
            videoFrag = getSupportFragmentManager().findFragmentByTag("video");
            musicFrag = getSupportFragmentManager().findFragmentByTag("music");
            if (videoFrag == null) videoFrag = new VideoFragment();
            if (musicFrag == null) musicFrag = new MusicFragment();
        }

        nav.setOnNavigationItemSelectedListener(this::onNavSelected);

        startKeepAlive();
        handleShare(getIntent());
    }

    private boolean onNavSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.tab_video) {
            show(videoFrag, musicFrag);
            return true;
        } else if (id == R.id.tab_music) {
            show(musicFrag, videoFrag);
            return true;
        }
        return false;
    }

    private void show(Fragment target, Fragment other) {
        if (target == null || target.isAdded()) {
            if (target != null && other != null) {
                getSupportFragmentManager().beginTransaction()
                        .show(target).hide(other).commitAllowingStateLoss();
            }
            return;
        }
        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, target).hide(other)
                .commitAllowingStateLoss();
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

    /** 从抖音分享到本应用时，自动切到视频页并解析 */
    private void handleShare(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (TextUtils.isEmpty(text)) return;

        nav.setSelectedItemId(R.id.tab_video);
        show(videoFrag, musicFrag);

        if (videoFrag instanceof VideoFragment) {
            final String t = text;
            videoFrag.getView().postDelayed(() -> ((VideoFragment) videoFrag).parseFromText(t), 300);
        }
    }

    @Override
    public void onBackPressed() {
        if (nav.getSelectedItemId() != R.id.tab_video) {
            nav.setSelectedItemId(R.id.tab_video);
            show(videoFrag, musicFrag);
            return;
        }
        super.onBackPressed();
    }
}