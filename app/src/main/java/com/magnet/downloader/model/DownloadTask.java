package com.magnet.downloader.model;

/**
 * 下载任务模型
 */
public class DownloadTask {
    public long id;
    public String url;
    public String fileName;
    public String savePath;
    public long totalBytes;
    public long downloadedBytes;
    public long speedBps;
    public int state;        // 0=等待 1=下载中 2=暂停 3=完成 4=失败 5=取消
    public String error;
    public long createdAt;

    public static final int STATE_WAITING = 0;
    public static final int STATE_DOWNLOADING = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_DONE = 3;
    public static final int STATE_ERROR = 4;
    public static final int STATE_CANCELLED = 5;

    public DownloadTask() {
        this.id = System.currentTimeMillis() + (long)(Math.random() * 10000);
        this.createdAt = System.currentTimeMillis();
    }

    public int percent() {
        if (totalBytes <= 0) return -1;
        return (int)(downloadedBytes * 100 / totalBytes);
    }

    public String displaySize() {
        if (totalBytes <= 0) return "未知";
        return humanSize(totalBytes);
    }

    public String displayDownloaded() {
        return humanSize(downloadedBytes);
    }

    public String displaySpeed() {
        if (speedBps <= 0) return "";
        return humanSize(speedBps) + "/s";
    }

    public static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / 1024.0 / 1024);
        return String.format("%.2f GB", bytes / 1024.0 / 1024 / 1024);
    }
}
