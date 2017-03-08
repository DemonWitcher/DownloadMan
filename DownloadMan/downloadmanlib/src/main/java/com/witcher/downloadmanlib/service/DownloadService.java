package com.witcher.downloadmanlib.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.witcher.downloadmanlib.db.DBManager;
import com.witcher.downloadmanlib.entity.Constant;
import com.witcher.downloadmanlib.entity.DownloadMission;
import com.witcher.downloadmanlib.entity.IDownloadService;
import com.witcher.downloadmanlib.entity.MissionState;
import com.witcher.downloadmanlib.entity.Range;
import com.witcher.downloadmanlib.helper.DownloadHelper;
import com.witcher.downloadmanlib.helper.FileHelper;
import com.witcher.downloadmanlib.util.L;

import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

import static com.witcher.downloadmanlib.entity.Constant.DownloadMan.MAX_DOWNLOAD_NUMBER;


/**
 * Created by witcher on 2017/2/8 0008.
 */

public class DownloadService extends Service {

    static {
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                if (throwable instanceof InterruptedException) {
                    L.i("Thread interrupted");
                } else if (throwable instanceof InterruptedIOException) {
                    L.i("Io interrupted");
                } else if (throwable instanceof SocketException) {
                    L.i("Socket error");
                }
            }
        });
    }

    private final AtomicInteger mIntDownloadingCount = new AtomicInteger();
    private ConcurrentLinkedQueue<DownloadMission> mWaitList;
    private ConcurrentHashMap<String, DownloadMission> mIndexMap;

    private DownloadHelper mDownloadHelper;
    private DBManager mDBManager;
//    private Looper mLooper;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mDownloadHelper = new DownloadHelper(this);
        mDBManager = DBManager.getSingleton(this);
        mWaitList = new ConcurrentLinkedQueue<>();
        mIndexMap = new ConcurrentHashMap<>();

        List<DownloadMission> list = mDBManager.getAllMission();
        for (DownloadMission mission : list) {
            mIndexMap.put(mission.getUrl(), mission);
            if (mission.getState() == MissionState.WAIT) {
                mWaitList.add(mission);
            }
        }

//        mLooper = new Looper();
//        mLooper.loop();
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // TODO: 2017/2/16 0016 业务逻辑未想好,暂时全部暂停
        pauseAllMission();
//        mLooper.interrupt();
        L.i("onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private IBinder binder = new IDownloadService.Stub() {

        @Override
        public int addMission(String name, String url) throws RemoteException {
            return DownloadService.this.addMission(name, url);
        }

        @Override
        public List<DownloadMission> getAllMission() throws RemoteException {
            return DownloadService.this.getAllMission();
        }

        @Override
        public void pauseMission(String url) throws RemoteException {
            DownloadService.this.pauseMission(url);
        }

        @Override
        public void pauseAllMission() throws RemoteException {
            DownloadService.this.pauseAllMission();
        }

        @Override
        public void startMission(String url) throws RemoteException {
            DownloadService.this.continueMission(url);
        }

        @Override
        public void startAllMission() throws RemoteException {
            DownloadService.this.startAllMission();
        }

        @Override
        public void cancelMission(String url) throws RemoteException {
            DownloadService.this.cancelMission(url);
        }

        @Override
        public void cancelAll() throws RemoteException {
            DownloadService.this.cancelAll();
        }
    };

    private List<DownloadMission> getAllMission() {
        List<DownloadMission> list = new ArrayList<>(mIndexMap.size());
        for (String key : mIndexMap.keySet()) {
            list.add(mIndexMap.get(key));
        }
        Collections.sort(list);
        return list;
    }

    /*
    1.添加一个任务并且开始下载
    2.有一个下载完了,在等待队列里拿了一个下载
    3.一个暂停了的任务继续下载
     */

    private void startAllMission() {
//        checkHaveMissionToStartDownload();
        //把全部暂停与错误任务置为等待中
        for (String url : mIndexMap.keySet()) {
            DownloadMission mission = mIndexMap.get(url);
            if (mission.getState() == (MissionState.PAUSE | MissionState.ERROR)) {
                mission.setState(MissionState.WAIT);
                mWaitList.add(mission);
                mDBManager.updateMission(mission);
            }
        }
    }

    private void cancelMission(String url) {
        DownloadMission mission = mIndexMap.remove(url);
        switch (mission.getState()) {
            case MissionState.PAUSE: {
            }
            break;
            case MissionState.WAIT: {
                mission.setState(MissionState.CANCEL);
            }
            break;
            case MissionState.DOWNLOADING: {
                mission.pauseAllRange();
                mIntDownloadingCount.getAndDecrement();
                checkHaveMissionToStartDownload();
            }
            break;
            case MissionState.COMPLETE: {
                // TODO: 2017/2/16 0016  暂时没想好删除完成后任务的业务逻辑
            }
            break;
            case MissionState.ERROR: {
            }
            break;
            default: {
            }
        }
        FileHelper.deleteByName(mission.getName());
        mDBManager.deleteMission(url);
    }

    private void pauseAllMission() {
        for (String url : mIndexMap.keySet()) {
            DownloadMission mission = mIndexMap.get(url);
            if (mission.getState() == MissionState.DOWNLOADING) {
                mission.pauseAllRange();
                mission.setState(MissionState.PAUSE);
                mDBManager.updateMission(mission);
                mIntDownloadingCount.getAndDecrement();
            } else if (mission.getState() == MissionState.WAIT) {
                mission.setState(MissionState.PAUSE);
                mDBManager.updateMission(mission);
            }
        }
        mWaitList.clear();
    }

    //一个继续下载指令 有可能来自于暂停中任务,也有可能来自于错误中任务
    private void continueMission(String url) {
        DownloadMission mission = mIndexMap.get(url);
        mWaitList.add(mission);
        mission.setState(MissionState.WAIT);
        mDBManager.updateMission(mission);
        checkHaveMissionToStartDownload();
    }

    private void pauseMission(String url) {
        DownloadMission mission = mIndexMap.get(url);
        if (mission.getState() == MissionState.DOWNLOADING) {
            L.i("暂停了正在下载中的任务:" + mission.getName());
            mission.pauseAllRange();
            mIntDownloadingCount.getAndDecrement();
        } else if (mission.getState() == MissionState.WAIT) {
            L.i("暂停了正在等待中的任务:" + mission.getName());
//            mWaitList.remove(mission);
        }
        mission.setState(MissionState.PAUSE);
        mDBManager.updateMission(mission);
        checkHaveMissionToStartDownload();
    }

    private void cancelAll() {
        mDBManager.deleteAll();
        FileHelper.deleteAll();
        mIntDownloadingCount.set(0);
        mWaitList.clear();
        mIndexMap.clear();
    }

    private int addMission(String name, String url) {
        if (checkMissionExist(url)) {
            L.i("任务已存在");
            return Constant.AddMission.IS_EXIST;
        }
        DownloadMission mission = new DownloadMission.Builder()
                .setUrl(url)
                .setName(name)
                .setState(MissionState.WAIT)
                .build();
        mDBManager.addMission(mission);
        mIndexMap.put(url, mission);
        mWaitList.add(mission);
        checkHaveMissionToStartDownload();
        return Constant.AddMission.SUCCESS;
    }

    private void startDownload(final DownloadMission mission) {
        mission.setState(MissionState.CONNECTING);
        mIntDownloadingCount.getAndIncrement();
        mDownloadHelper.startDownload(mission)
//                .unsubscribeOn(Schedulers.computation())
                .subscribe(new Observer<Range>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        L.i("onSubscribe");
                        mission.setState(MissionState.DOWNLOADING);
                        mDBManager.updateMission(mission);
                        mission.setDisposable(d);
                    }

                    @Override
                    public void onNext(Range range) {
                        mDBManager.updateRange(range);
                        if (range.progress > range.size) {
                            setMissionError(mission);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        L.i("onError:" + e.getMessage() + ",错误任务:" + mission.getName());
                        L.i(Log.getStackTraceString(e));
                        setMissionError(mission);
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        L.i("onCompleted");
                        mission.setState(MissionState.COMPLETE);
                        mIntDownloadingCount.getAndDecrement();
                        mDBManager.updateMission(mission);
                        checkHaveMissionToStartDownload();
                    }
                });
    }

    private void setMissionError(DownloadMission mission) {
        mission.setState(MissionState.ERROR);
        mIntDownloadingCount.getAndDecrement();
        mDBManager.updateMission(mission);
        mission.pauseAllRange();
    }

    private void checkHaveMissionToStartDownload() {
        while (!checkDownloadingIsFull()) {
            if (mWaitList.size() != 0) {
                DownloadMission mission = mWaitList.poll();
                if (mission.getState() != MissionState.WAIT) {
                    continue;
                }
                startDownload(mission);
            } else {
                break;
            }
        }
    }

    private boolean checkDownloadingIsFull() {
        return mIntDownloadingCount.get() == MAX_DOWNLOAD_NUMBER;
    }

    private boolean checkMissionExist(String url) {
        return mIndexMap.containsKey(url);
    }

//    private class Looper extends Thread {
//
//        Looper() {
//            super(new Runnable() {
//                @Override
//                public void run() {
//                    while (!Thread.currentThread().isInterrupted()) {
//                        if (checkDownloadingIsFull()) {
//                            continue;
//                        }
//                        if (mWaitList.size() == 0) {
//                            continue;
//                        }
//                        DownloadMission mission = mWaitList.poll();
//                        if (mission.getState() != MissionState.WAIT) {
//                            continue;
//                        }
//                        mission.setState(MissionState.CONNECTING);
//                        mIntDownloadingCount.getAndIncrement();
//                        startDownload(mission);
//                    }
//                }
//            });
//        }
//
//        private void loop() {
//            this.start();
//        }
//    }

}
