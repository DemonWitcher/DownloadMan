package com.witcher.downloadmanlib.entity;

/**
 * Created by witcher on 2017/2/8 0008.
 */

public class MissionState {
    public static final int WAIT = 0x01;
    public static final int PAUSE = 0x02;
    public static final int DOWNLOADING = 0x04;
    public static final int COMPLETE = 0x08;
    public static final int ERROR = 0x10;
    public static final int CONNECTING = 0x16;
}
