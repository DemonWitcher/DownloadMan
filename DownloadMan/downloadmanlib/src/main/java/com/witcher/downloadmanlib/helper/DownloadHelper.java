package com.witcher.downloadmanlib.helper;


import android.content.Context;
import android.util.Log;

import com.witcher.downloadmanlib.db.DBManager;
import com.witcher.downloadmanlib.entity.DownloadMission;
import com.witcher.downloadmanlib.entity.Range;
import com.witcher.downloadmanlib.util.L;
import com.witcher.downloadmanlib.util.Util;

import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.CompositeException;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

import static android.text.TextUtils.concat;
import static com.witcher.downloadmanlib.entity.Constant.DownloadMan.DOWNLOAD_RANGE;
import static com.witcher.downloadmanlib.entity.Constant.DownloadMan.RETRY_COUNT;
import static com.witcher.downloadmanlib.entity.Constant.DownloadMan.TAG;

/**
 * Created by witcher on 2017/2/8 0008.
 */

public class DownloadHelper {

    private DownloadAPI mDownloadAPI;
    private DBManager mDbManager;
    private FileHelper mFileHelper;
    private Context mContext;

    public DownloadHelper(Context context) {
        this.mContext = context;
        mDbManager = DBManager.getSingleton(mContext);
        mDownloadAPI = RetrofitProvider.getInstance().create(DownloadAPI.class);
        mFileHelper = new FileHelper();
    }

    public Observable<Range> startDownload(final DownloadMission mission) {
        L.i("startDownload_url:" + mission.getUrl());
        L.i("startDownload_name:" + mission.getName());
        mFileHelper.createDownloadDirs();
        if (mDbManager.haveMission(mission.getUrl()) && mFileHelper.downloadFileExists(mission.getName())) {
            //这里查出所有range赋给mission
            mission.setRanges(mDbManager.getRangeByUrl(mission.getUrl()));
            return launchDownload(mission);
        } else {
            return mDownloadAPI.getHttpHeader(mission.getUrl())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .flatMap(new Func1<Response<Void>, Observable<Range>>() {
                        @Override
                        public Observable<Range> call(Response<Void> voidResponse) {
                            mission.setSize(Util.contentLength(voidResponse));
                            createRange(mission);
                            return launchDownload(mission);
                        }
                    }).retry(new Func2<Integer, Throwable, Boolean>() {
                        @Override
                        public Boolean call(Integer integer, Throwable throwable) {
                            return retry(integer, throwable);
                        }
                    });
        }

    }

    private Observable<Range> launchDownload(DownloadMission mission) {
        List<Observable<Range>> list = new ArrayList<>(DOWNLOAD_RANGE);
        for (int i = 0; i < DOWNLOAD_RANGE; ++i) {
            list.add(downloadObservable(mission.getRanges().get(i)));
        }
        return Observable.merge(list);
    }

    private void createRange(DownloadMission mission) {
        int eachSize = (int) (mission.getSize() / DOWNLOAD_RANGE);
        List<Range> list = new ArrayList<>(DOWNLOAD_RANGE);
        for (int i = 0; i < DOWNLOAD_RANGE; ++i) {
            Range range;
            if (i == DOWNLOAD_RANGE - 1) {
                range = new Range(i * eachSize, mission.getSize() - 1);
            } else {
                range = new Range(i * eachSize, (i + 1) * eachSize - 1);
            }
            range.setUrl(mission.getUrl());
            range.setName(mission.getName());
            list.add(range);
            mDbManager.addRange(range);
        }
        mission.setRanges(list);
    }

    private Observable<Range> downloadObservable(final Range range) {
        String strRange = "bytes=" + (range.getProgress() + range.start) + "-" + range.end;
        L.i("start a download request:" + strRange);
        return mDownloadAPI.download(strRange, range.getUrl())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(new Func1<Response<ResponseBody>, Observable<Range>>() {
                    @Override
                    public Observable<Range> call(final Response<ResponseBody> responseBodyResponse) {
                        return progressObservable(responseBodyResponse, range);
                    }
                }).retry(new Func2<Integer, Throwable, Boolean>() {
                    @Override
                    public Boolean call(Integer integer, Throwable throwable) {
                        return retry(integer, throwable);
                    }
                });
    }

    private Observable<Range> progressObservable(final Response<ResponseBody> responseBodyResponse, final Range range) {
        return Observable.create(new Observable.OnSubscribe<Range>() {
            @Override
            public void call(Subscriber<? super Range> subscriber) {
                if (responseBodyResponse.code() == 200 || responseBodyResponse.code() == 206) {
                    mFileHelper.writeResponseBodyToDisk(subscriber, responseBodyResponse, range);
                } else {
                    subscriber.onError(new Exception("http response code error,code is " + responseBodyResponse.code()));
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .onBackpressureLatest()
                .sample(1, TimeUnit.SECONDS);
    }

    private Boolean retry(Integer integer, Throwable throwable) {
        if (throwable instanceof ProtocolException) {
            if (integer < RETRY_COUNT + 1) {
                Log.w(TAG, Thread.currentThread().getName() +
                        " we got an error in the underlying protocol, such as a TCP error, retry to connect " +
                        integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof UnknownHostException) {
            if (integer < RETRY_COUNT + 1) {
                Log.w(TAG, Thread.currentThread().getName() +
                        " no network, retry to connect " + integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof HttpException) {
            if (integer < RETRY_COUNT + 1) {
                Log.w(TAG, Thread.currentThread().getName() +
                        " had non-2XX http error, retry to connect " + integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof SocketTimeoutException) {
            if (integer < RETRY_COUNT + 1) {
                Log.w(TAG, Thread.currentThread().getName() +
                        " socket time out,retry to connect " + integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof ConnectException) {
            if (integer < RETRY_COUNT + 1) {
                Log.w(TAG, concat(Thread.currentThread().getName(), " ", throwable.getMessage(),
                        ". retry to connect ", String.valueOf(integer), " times").toString());
                return true;
            }
            return false;
        } else if (throwable instanceof SocketException) {
            if (integer < RETRY_COUNT + 1) {
                Log.w(TAG, Thread.currentThread().getName() +
                        " a network or conversion error happened, retry to connect " + integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof CompositeException) {
            Log.w(TAG, throwable.getMessage());
            return false;
        } else {
            Log.w(TAG, throwable);
            return false;
        }
    }
}
