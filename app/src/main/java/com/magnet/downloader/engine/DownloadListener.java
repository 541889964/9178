package com.magnet.downloader.engine;

import com.magnet.downloader.model.DownloadTask;
import java.io.File;

public interface DownloadListener {
    void onStart(DownloadTask task, String fileName, long totalBytes);
    void onProgress(DownloadTask task, long downloaded, long total, int percent, long speedBps);
    void onDone(DownloadTask task, File file);
    void onError(DownloadTask task, String error);
    void onCancel(DownloadTask task);
}
