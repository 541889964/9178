package com.magnet.downloader.model;
public class DownloadItem {
    public long id;
    public String title;
    public String sub;
    public String status;
    public int progress;
    public String url;
    public long totalBytes;
    public long downloadedBytes;
    public DownloadItem(){
        this.id = System.currentTimeMillis() + (long)(Math.random()*1000);
    }
    public String displaySize(){
        if(totalBytes<=0)return "未知";
        double mb = totalBytes/1024.0/1024.0;
        if(mb>=1024) return String.format("%.2f GB", mb/1024);
        return String.format("%.1f MB", mb);
    }
}
