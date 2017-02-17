package com.witcher.downloadmanlib.entity;

/**
 * Created by witcher on 2017/2/9 0009.
 */

public class Constant {
    public interface DownloadMan {
        boolean DEBUG = true;
        int MAX_DOWNLOAD_NUMBER = 2;
        int DOWNLOAD_RANGE = 5;
        int RETRY_COUNT = 3;
        String TAG = "DownloadMan";
    }
    public interface AddMission{
        int IS_EXIST = 0;
        int SUCCESS = 200;
        int ERROR = 500;
    }
}
