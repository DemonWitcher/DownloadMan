package com.witcher.downloadman;

import android.app.Application;

import com.witcher.downloadmanlib.manager.DownloadManager;

/**
 * Created by witcher on 2017/2/13 0013.
 */

public class MyApp extends Application {

    private static MyApp context;
    private static DownloadManager mDownloadMgr;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        mDownloadMgr = DownloadManager.getSingleton(this);
    }
    public static DownloadManager getDownloadMgr(){
        return mDownloadMgr;
    }

    public static MyApp getContext() {
        return context;
    }
}
