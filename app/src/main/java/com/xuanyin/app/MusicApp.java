package com.xuanyin.app;
import android.app.Application;
import com.xuanyin.app.util.Prefs;
import java.io.File;
import java.io.PrintWriter;
public class MusicApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        installCrashHandler();
        try { Prefs.init(this); } catch (Throwable ignored) {}
    }
    private void installCrashHandler() {
        final Thread.UncaughtExceptionHandler def = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                File dir = new File("/storage/emulated/0/MT2/errors");
                dir.mkdirs();
                File f = new File(dir, "crash_" + System.currentTimeMillis() + ".txt");
                PrintWriter pw = new PrintWriter(f);
                pw.println("Thread: " + t.getName());
                pw.println("Time: " + new java.util.Date());
                e.printStackTrace(pw);
                pw.close();
            } catch (Throwable ignored) {}
            if (def != null) def.uncaughtException(t, e);
        });
    }
    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        try { com.bumptech.glide.Glide.get(this).clearMemory(); } catch (Throwable ignored) {}
    }
}
