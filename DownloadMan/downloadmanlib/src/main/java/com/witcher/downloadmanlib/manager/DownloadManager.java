package com.witcher.downloadmanlib.manager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.witcher.downloadmanlib.entity.Constant;
import com.witcher.downloadmanlib.entity.DownloadMission;
import com.witcher.downloadmanlib.entity.IDownloadService;

import java.util.List;

/**
 * Created by witcher on 2017/2/8 0008.
 */

public class DownloadManager {

    private IDownloadService mDownloadService;
    private Context mContext;
    private static DownloadManager singleton;

    public static DownloadManager getSingleton(Context context) {
        if (singleton == null) {
            synchronized (DownloadManager.class) {
                if (singleton == null) {
                    singleton = new DownloadManager(context);
                }
            }
        }
        return singleton;
    }

    private DownloadManager(Context context) {
        mContext = context;
        bindService();
    }

    private void bindService() {
        mContext.bindService(new Intent("com.witcher.downloadmanlib.service.DownloadService"), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mDownloadService = IDownloadService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mDownloadService = null;
        }
    };


    public int addMission(String name, String url) throws RemoteException {
        if (mDownloadService != null)
            return mDownloadService.addMission(name, url);
        return Constant.AddMission.ERROR;
    }

    public List<DownloadMission> getAllMission() throws RemoteException {
        if (mDownloadService != null)
            return mDownloadService.getAllMission();
        return null;
    }

    public void pauseMission(String url) throws RemoteException {
        if (mDownloadService != null)
            mDownloadService.pauseMission(url);
    }

    public void pauseAllMission() throws RemoteException {
        if (mDownloadService != null)
            mDownloadService.pauseAllMission();
    }

    public void startMission(String url) throws RemoteException {
        if (mDownloadService != null)
            mDownloadService.startMission(url);
    }

    public void startAllMission() throws RemoteException {
        if (mDownloadService != null)
            mDownloadService.startAllMission();
    }

    public void cancelMission(String url) throws RemoteException {
        if (mDownloadService != null)
            mDownloadService.cancelMission(url);
    }

    public void cancelAll() throws RemoteException {
        if (mDownloadService != null)
            mDownloadService.cancelAll();
    }
}
