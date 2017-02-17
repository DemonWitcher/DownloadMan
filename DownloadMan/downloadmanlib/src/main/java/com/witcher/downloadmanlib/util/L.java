package com.witcher.downloadmanlib.util;

import android.util.Log;

import com.witcher.downloadmanlib.entity.Constant;

/**
 * Created by witcher on 2017/2/10 0010.
 */

public class L {
    private static String TAG = "witcher";
    public static void i(String s){
        if(Constant.DownloadMan.DEBUG){
            Log.i(TAG,s);
        }
    }
}
