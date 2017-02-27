package com.witcher.downloadmanlib.util;

import android.text.TextUtils;

import com.witcher.downloadmanlib.entity.DownloadMission;
import com.witcher.downloadmanlib.entity.MissionState;

import java.io.Closeable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import okhttp3.internal.http.HttpHeaders;
import retrofit2.Response;

/**
 * Created by witcher on 2017/2/8 0008.
 */

public class Util {

    public static boolean isEmpty(String string) {
        return TextUtils.isEmpty(string);
    }

    public static boolean notSupportRange(Response<?> response) {
        return TextUtils.isEmpty(contentRange(response)) || contentLength(response) == -1 ||
                isChunked(response);
    }
    public static long contentLength(Response<?> response) {
        return HttpHeaders.contentLength(response.headers());
    }
    public static boolean isChunked(Response<?> response) {
        return "chunked".equals(transferEncoding(response));
    }
    private static String contentRange(Response<?> response) {
        return response.headers().get("Content-Range");
    }
    private static String transferEncoding(Response<?> response) {
        return response.headers().get("Transfer-Encoding");
    }
    public static String formatSize(long size) {
        String hrSize;
        double b = size;
        double k = size / 1024.0;
        double m = ((size / 1024.0) / 1024.0);
        double g = (((size / 1024.0) / 1024.0) / 1024.0);
        double t = ((((size / 1024.0) / 1024.0) / 1024.0) / 1024.0);
        DecimalFormat dec = new DecimalFormat("0.00");
        if (t > 1) {
            hrSize = dec.format(t).concat(" TB");
        } else if (g > 1) {
            hrSize = dec.format(g).concat(" GB");
        } else if (m > 1) {
            hrSize = dec.format(m).concat(" MB");
        } else if (k > 1) {
            hrSize = dec.format(k).concat(" KB");
        } else {
            hrSize = dec.format(b).concat(" B");
        }
        return hrSize;
    }

    public static String formatState(int state){
        switch (state){
            case MissionState.COMPLETE:return "已完成";
            case MissionState.DOWNLOADING:return "下载中";
            case MissionState.ERROR:return "错误";
            case MissionState.PAUSE:return "暂停中";
            case MissionState.WAIT:return "等待中";
            case MissionState.CONNECTING:return "连接中";

            default:return "未知状态";
        }
    }
    /**
     * 获得下载的百分比, 保留两位小数
     */
    public static String formatPercent(long totalSize,long downloadSize) {
        String percent;
        Double result;
        if (totalSize == 0L) {
            result = 0.0;
        } else {
            result = downloadSize * 1.0 / totalSize;
        }
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMinimumFractionDigits(2);
        percent = nf.format(result);
        return percent;
    }

    public static void printList(List<DownloadMission> list){
        if(list == null){
            return;
        }
        for(DownloadMission mission:list){
            L.i(mission.toString());
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

}
