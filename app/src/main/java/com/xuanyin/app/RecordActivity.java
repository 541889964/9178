package com.xuanyin.app;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.xuanyin.app.service.ScreenRecordService;
import com.xuanyin.app.util.Haptic;
public class RecordActivity extends AppCompatActivity {
    private static final int REQ_CODE = 1001;
    private boolean recording = false;
    private TextView tvStatus;
    private Button btnToggle;
    private final BroadcastReceiver rx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if (ScreenRecordService.A_STATE.equals(a)) {
                recording = i.getBooleanExtra("on", false);
                updateUi();
                String p = i.getStringExtra("path");
                if (!recording && p != null) Toast.makeText(RecordActivity.this, "已保存: " + p, Toast.LENGTH_LONG).show();
            }
        }
    };
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_record);
        tvStatus = (TextView) findViewById(R.id.tv_rec_status);
        btnToggle = (Button) findViewById(R.id.btn_rec_toggle);
        findViewById(R.id.btn_rec_back).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { Haptic.tap(v); finish(); }
        });
        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Haptic.tap(v);
                if (recording) stopRec();
                else requestStart();
            }
        });
        updateUi();
        IntentFilter f = new IntentFilter(ScreenRecordService.A_STATE);
        try {
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(rx, f, Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(rx, f);
        } catch (Throwable ignored) {}
    }
    private void updateUi() {
        if (recording) {
            tvStatus.setText("● 正在录屏");
            tvStatus.setTextColor(0xFFFF3B30);
            btnToggle.setText("停止录屏");
        } else {
            tvStatus.setText("○ 未录屏");
            tvStatus.setTextColor(0xFFC9BFF5);
            btnToggle.setText("开始录屏");
        }
    }
    private void requestStart() {
        try {
            MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CODE);
        } catch (Throwable t) { Toast.makeText(this, "无法请求录屏权限", Toast.LENGTH_SHORT).show(); }
    }
    private void stopRec() {
        try {
            Intent i = new Intent(this, ScreenRecordService.class);
            i.setAction(ScreenRecordService.ACTION_STOP);
            startService(i);
        } catch (Throwable ignored) {}
    }
    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_CODE) {
            if (res == Activity.RESULT_OK && data != null) {
                try {
                    Intent i = new Intent(this, ScreenRecordService.class);
                    i.setAction(ScreenRecordService.ACTION_START);
                    i.putExtra(ScreenRecordService.EXTRA_CODE, res);
                    i.putExtra(ScreenRecordService.EXTRA_DATA, data);
                    startService(i);
                    recording = true;
                    updateUi();
                } catch (Throwable t) { Toast.makeText(this, "启动失败", Toast.LENGTH_SHORT).show(); }
            } else {
                Toast.makeText(this, "已取消", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(rx); } catch (Throwable ignored) {}
    }
}
