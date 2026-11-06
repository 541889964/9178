package com.superrouter.dytool;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

/**
 * 全屏沉浸式播放器
 * 支持横屏、手势隐藏、系统媒体控制条
 */
public class FullscreenPlayerActivity extends Activity {

    public static final String EXTRA_URL = "url";
    public static final String EXTRA_TITLE = "title";

    private VideoView videoView;
    private ProgressBar loading;
    private TextView tvTitle;
    private View topBar;
    private boolean barVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_player);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUi();

        videoView = findViewById(R.id.fsVideo);
        loading = findViewById(R.id.fsLoading);
        tvTitle = findViewById(R.id.fsTitle);
        topBar = findViewById(R.id.fsTopBar);

        String url = getIntent().getStringExtra(EXTRA_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        tvTitle.setText(title == null ? "" : title);

        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "没有可播放的地址", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        MediaController controller = new MediaController(this);
        controller.setAnchorView(videoView);
        videoView.setMediaController(controller);

        videoView.setOnPreparedListener(mp -> {
            loading.setVisibility(View.GONE);
            mp.setLooping(false);
            videoView.start();
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            loading.setVisibility(View.GONE);
            Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show();
            finish();
            return true;
        });

        videoView.setOnCompletionListener(mp -> finish());
        videoView.setVideoURI(Uri.parse(url));

        findViewById(R.id.fsBack).setOnClickListener(v -> finish());

        videoView.setOnClickListener(v -> {
            barVisible = !barVisible;
            topBar.animate().alpha(barVisible ? 1f : 0f).setDuration(180).start();
        });
    }

    private void hideSystemUi() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null) videoView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (videoView != null) videoView.stopPlayback();
        } catch (Exception ignored) {
        }
    }
}
