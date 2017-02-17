package com.witcher.downloadmanlib.entity;

import com.witcher.downloadmanlib.entity.DownloadMission;
import com.witcher.downloadmanlib.entity.Range;

interface IDownloadService {
    int addMission(String name,String url);
    List<DownloadMission> getAllMission();
    void pauseMission(String url);
    void pauseAllMission();
    void startMission(String url);
    void startAllMission();
    void cancelMission(String url);
    void cancelAll();
}
