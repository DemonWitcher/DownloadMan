package com.witcher.downloadmanlib.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.witcher.downloadmanlib.db.DBManager2;
import com.witcher.downloadmanlib.entity.Constant;
import com.witcher.downloadmanlib.entity.DownloadMission;
import com.witcher.downloadmanlib.entity.IDownloadService;
import com.witcher.downloadmanlib.entity.MissionState;
import com.witcher.downloadmanlib.helper.DownloadHelper;
import com.witcher.downloadmanlib.helper.FileHelper;
import com.witcher.downloadmanlib.util.L;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Subscriber;
import rx.Subscription;

import static com.witcher.downloadmanlib.entity.Constant.DownloadMan.MAX_DOWNLOAD_NUMBER;


/**
 * Created by witcher on 2017/2/8 0008.
 */

public class DownloadService extends Service {

    private List<DownloadMission> mDownloadList;
    private List<DownloadMission> mWaitList;
    private List<DownloadMission> mPauseList;
    private List<DownloadMission> mErrorList;
    private List<DownloadMission> mCompleteList;
    private Map<String, DownloadMission> mIndexMap;

//    private Lock lock = new ReentrantLock();

    private DownloadHelper mDownloadHelper;
    private DBManager2 mDBManager;
    private FileHelper mFileHelper;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mDownloadHelper = new DownloadHelper(this);
        mDBManager = DBManager2.getSingleton(this);
        mFileHelper = new FileHelper(this);
        mDownloadList = new ArrayList<>(MAX_DOWNLOAD_NUMBER);
        mWaitList = new ArrayList<>();
        mPauseList = new ArrayList<>();
        mErrorList = new ArrayList<>();
        mCompleteList = new ArrayList<>();
        mIndexMap = new HashMap<>();

        List<DownloadMission> list = mDBManager.getAllMission();
        for (DownloadMission mission : list) {
            mIndexMap.put(mission.getUrl(), mission);
            switch (mission.getState()) {
                case MissionState.PAUSE: {
                    mPauseList.add(mission);
                }
                break;
                case MissionState.WAIT: {
                    mWaitList.add(mission);
                }
                break;
                case MissionState.DOWNLOADING: {
                    // TODO: 2017/2/16 0016 这里没想好有任务处于正在下载中,APP关了,又启动了之后的业务逻辑
                }
                break;
                case MissionState.COMPLETE: {
                    mCompleteList.add(mission);
                }
                break;
                case MissionState.ERROR: {
                    mErrorList.remove(mission);
                }
                break;
                case MissionState.CONNECTING: {
                    mDownloadList.add(mission);
                }
                break;
                default: {
                }
            }
        }

        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // TODO: 2017/2/16 0016 业务逻辑未想好,暂时全部暂停
        pauseAllMission();
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
        checkHaveMissionToStartDownload();
    }

    private void cancelMission(String url) {
        DownloadMission mission = mIndexMap.get(url);
        mFileHelper.deleteByName(mission.getName());
        mDBManager.deleteMission(url);
        switch (mission.getState()) {
            case MissionState.PAUSE: {
                mPauseList.remove(mission);
            }
            break;
            case MissionState.WAIT: {
                mWaitList.remove(mission);
            }
            break;
            case MissionState.DOWNLOADING: {
                mission.pauseAllRange();
                mDownloadList.remove(mission);
                checkHaveMissionToStartDownload();
            }
            break;
            case MissionState.COMPLETE: {
                // TODO: 2017/2/16 0016  暂时没想好删除完成后任务的业务逻辑
            }
            break;
            case MissionState.ERROR: {
                mErrorList.remove(mission);
            }
            break;
            default: {
            }
        }
        mIndexMap.remove(mission);
    }

    private void pauseAllMission() {
        for (DownloadMission mission : mDownloadList) {
            mission.pauseAllRange();
            mission.setState(MissionState.PAUSE);
            mDBManager.updateMission(mission);
            mPauseList.add(mission);
        }
        mDownloadList.clear();
        for (DownloadMission mission : mWaitList) {
            mission.setState(MissionState.PAUSE);
            mDBManager.updateMission(mission);
            mPauseList.add(mission);
        }
        mWaitList.clear();
    }

    //一个继续下载指令 有可能来自于暂停中任务,也有可能来自于错误中任务
    private void continueMission(String url) {
        DownloadMission mission = mIndexMap.get(url);
        if (mission.getState() == MissionState.PAUSE) {
            mPauseList.remove(mission);
        } else if (mission.getState() == MissionState.ERROR) {
            mErrorList.remove(mission);
        }
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
            mDownloadList.remove(mission);
        } else if (mission.getState() == MissionState.WAIT) {
            L.i("暂停了正在等待中的任务:" + mission.getName());
            mWaitList.remove(mission);
        }
        mission.setState(MissionState.PAUSE);
        mDBManager.updateMission(mission);
        mPauseList.add(mission);
        checkHaveMissionToStartDownload();
    }

    private void cancelAll() {
        mDBManager.deleteAll();
        mFileHelper.deleteAll();
        mDownloadList.clear();
        mPauseList.clear();
        mWaitList.clear();
        mIndexMap.clear();
        mErrorList.clear();
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
        mDownloadList.add(mission);
        if (mission.getState() == MissionState.ERROR) {
            mErrorList.remove(mission);
        }
        Subscription sub = mDownloadHelper.startDownload(mission)
                .subscribe(new Subscriber<DownloadMission>() {
                    @Override
                    public void onStart() {
                        super.onStart();
                        L.i("onStart");
                        mission.setState(MissionState.DOWNLOADING);
                        mDBManager.updateMission(mission);
                    }

                    @Override
                    public void onCompleted() {
                        L.i("onCompleted");
                        mission.setState(MissionState.COMPLETE);
                        mDownloadList.remove(mission);
                        mCompleteList.add(mission);
                        mDBManager.updateMission(mission);
                        checkHaveMissionToStartDownload();
                    }

                    @Override
                    public void onError(Throwable e) {
                        L.i("onError:" + e.getMessage() + ",错误任务:" + mission.getName());
                        mission.setState(MissionState.ERROR);
                        mDownloadList.remove(mission);
                        mErrorList.add(mission);
                        mDBManager.updateMission(mission);
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(DownloadMission o) {
                        L.i("onNext");
                    }
                });
        mission.setSubscription(sub);
    }

    private void checkHaveMissionToStartDownload() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!checkDownloadingIsFull()) {
                    if (mWaitList.size() != 0) {
                        DownloadMission mission = mWaitList.get(0);
                        mWaitList.remove(0);
                        startDownload(mission);
                    } else {
                        break;
                    }
                }
            }
        }).start();
    }

    private boolean checkDownloadingIsFull() {
        return mDownloadList.size() == MAX_DOWNLOAD_NUMBER;
    }

    private boolean checkMissionExist(String url) {
        return mIndexMap.containsKey(url);
    }

}
