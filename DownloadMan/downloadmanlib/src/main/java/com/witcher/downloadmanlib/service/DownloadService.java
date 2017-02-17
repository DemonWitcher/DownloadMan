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

    private List<DownloadMission> downloadList;
    private List<DownloadMission> waitList;
    private List<DownloadMission> pauseList;
    private List<DownloadMission> errorList;
    private List<DownloadMission> completeList;
    private Map<String, DownloadMission> indexMap;

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
        downloadList = new ArrayList<>(MAX_DOWNLOAD_NUMBER);
        waitList = new ArrayList<>();
        pauseList = new ArrayList<>();
        errorList = new ArrayList<>();
        completeList = new ArrayList<>();
        indexMap = new HashMap<>();

        List<DownloadMission> list = mDBManager.getAllMission();
        for (DownloadMission mission : list) {
            indexMap.put(mission.getUrl(), mission);
            switch (mission.getState()) {
                case MissionState.PAUSE: {
                    pauseList.add(mission);
                }
                break;
                case MissionState.WAIT: {
                    waitList.add(mission);
                }
                break;
                case MissionState.DOWNLOADING: {
                    // TODO: 2017/2/16 0016 这里没想好有任务处于正在下载中,APP关了,又启动了之后的业务逻辑
                }
                break;
                case MissionState.COMPLETE: {
                    completeList.add(mission);
                }
                break;
                case MissionState.ERROR: {
                    errorList.remove(mission);
                }
                break;
                case MissionState.CONNECTING: {
                    downloadList.add(mission);
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

    private List<DownloadMission> getAllMission(){

        List<DownloadMission> list = new ArrayList<>(indexMap.size());
        for(String key:indexMap.keySet()){
            list.add(indexMap.get(key));
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
        DownloadMission mission = indexMap.get(url);
        mFileHelper.deleteByName(mission.getName());
        mDBManager.deleteMission(url);
        switch (mission.getState()) {
            case MissionState.PAUSE: {
                pauseList.remove(mission);
            }
            break;
            case MissionState.WAIT: {
                waitList.remove(mission);
            }
            break;
            case MissionState.DOWNLOADING: {
                mission.pauseAllRange();
                downloadList.remove(mission);
                checkHaveMissionToStartDownload();
            }
            break;
            case MissionState.COMPLETE: {
                // TODO: 2017/2/16 0016  暂时没想好删除完成后任务的业务逻辑
            }
            break;
            case MissionState.ERROR: {
                errorList.remove(mission);
            }
            break;
            default: {
            }
        }
        indexMap.remove(mission);
    }

    private void pauseAllMission() {
        for (DownloadMission mission : downloadList) {
            mission.pauseAllRange();
            mission.setState(MissionState.PAUSE);
            mDBManager.updateMission(mission);
            pauseList.add(mission);
        }
        downloadList.clear();
        for (DownloadMission mission : waitList) {
            mission.setState(MissionState.PAUSE);
            mDBManager.updateMission(mission);
            pauseList.add(mission);
        }
        waitList.clear();
    }

    //一个继续下载指令 有可能来自于暂停中任务,也有可能来自于错误中任务
    private void continueMission(String url) {
        DownloadMission mission = indexMap.get(url);
        if (mission.getState() == MissionState.PAUSE) {
            pauseList.remove(mission);
        } else if (mission.getState() == MissionState.ERROR) {
            errorList.remove(mission);
        }
        if (checkDownloadingIsFull()) {
            waitList.add(mission);
            mission.setState(MissionState.WAIT);
            mDBManager.updateMission(mission);
        } else {
            startDownload(mission);
        }
    }

    private void pauseMission(String url) {
        DownloadMission mission = indexMap.get(url);
        if (mission.getState() == MissionState.DOWNLOADING) {
            L.i("暂停了正在下载中的任务:" + mission.getName());
            mission.pauseAllRange();
            downloadList.remove(mission);
        } else if (mission.getState() == MissionState.WAIT) {
            L.i("暂停了正在等待中的任务:" + mission.getName());
            waitList.remove(mission);
        }
        mission.setState(MissionState.PAUSE);
        mDBManager.updateMission(mission);
        pauseList.add(mission);
        checkHaveMissionToStartDownload();
    }

    private void cancelAll() {
        mDBManager.deleteAll();
        mFileHelper.deleteAll();
        downloadList.clear();
        pauseList.clear();
        waitList.clear();
        indexMap.clear();
        errorList.clear();
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
        indexMap.put(url, mission);
        if (checkDownloadingIsFull()) {
            L.i("进入等待队列");
            waitList.add(mission);
        } else {
            startDownload(mission);
        }
        return Constant.AddMission.SUCCESS;
    }

    private void startDownload(final DownloadMission mission) {
        mission.setState(MissionState.CONNECTING);
        downloadList.add(mission);
        if (mission.getState() == MissionState.ERROR) {
            errorList.remove(mission);
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
                        downloadList.remove(mission);
                        completeList.add(mission);
                        mDBManager.updateMission(mission);
                        checkHaveMissionToStartDownload();
                    }

                    @Override
                    public void onError(Throwable e) {
                        L.i("onError:" + e.getMessage()+",错误任务:"+mission.getName());
                        mission.setState(MissionState.ERROR);
                        downloadList.remove(mission);
                        errorList.add(mission);
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
        while (!checkDownloadingIsFull()) {
            if (waitList.size() != 0) {
                startDownload(waitList.get(0));
                waitList.remove(0);
            } else {
                break;
            }
        }
    }

    private boolean checkDownloadingIsFull() {
        return downloadList.size() == MAX_DOWNLOAD_NUMBER;
    }

    private boolean checkMissionExist(String url) {
        return indexMap.containsKey(url);
    }

}
