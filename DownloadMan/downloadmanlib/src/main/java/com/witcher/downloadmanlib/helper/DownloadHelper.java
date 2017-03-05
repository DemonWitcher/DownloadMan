package com.witcher.downloadmanlib.helper;


import android.content.Context;
import android.util.Log;

import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import com.witcher.downloadmanlib.db.DBManager;
import com.witcher.downloadmanlib.entity.DownloadMission;
import com.witcher.downloadmanlib.entity.Range;
import com.witcher.downloadmanlib.util.L;
import com.witcher.downloadmanlib.util.Util;

import org.reactivestreams.Publisher;

import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.BiPredicate;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Response;

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
            return launchDownload(mission).toObservable();
        } else {
            return mDownloadAPI.getHttpHeader(mission.getUrl())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .unsubscribeOn(Schedulers.computation())
                    .flatMap(new Function<Response<Void>, Publisher<Range>>() {
                        @Override
                        public Publisher<Range> apply(@NonNull Response<Void> voidResponse) throws Exception {
                            mission.setSize(Util.contentLength(voidResponse));
                            createRange(mission);
                            return launchDownload(mission);
                        }
                    }).retry(new BiPredicate<Integer, Throwable>() {
                        @Override
                        public boolean test(@NonNull Integer integer, @NonNull Throwable throwable) throws Exception {
                            return retry(integer, throwable);
                        }
                    }).toObservable();
        }

    }

    private Flowable<Range> launchDownload(DownloadMission mission) {
        List<Publisher<Range>> list = new ArrayList<>(DOWNLOAD_RANGE);
        for (int i = 0; i < DOWNLOAD_RANGE; ++i) {
            Range range = mission.getRanges().get(i);
            if (range.progress < range.size) {
                list.add(downloadObservable(range));
            }
        }
        return Flowable.merge(list)
                .unsubscribeOn(Schedulers.computation());
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

    private Flowable<Range> downloadObservable(final Range range) {
        String strRange = "bytes=" + (range.getProgress() + range.start) + "-" + range.end;
        L.i("start a download request:" + strRange);
        return mDownloadAPI.download(strRange, range.getUrl())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.computation())
                .flatMap(new Function<Response<ResponseBody>, Flowable<Range>>() {
                    @Override
                    public Flowable<Range> apply(@NonNull Response<ResponseBody> responseBodyResponse) throws Exception {
                        return progressObservable(responseBodyResponse, range);
                    }
                })
                .unsubscribeOn(Schedulers.computation())
                .retry(new BiPredicate<Integer, Throwable>() {
                    @Override
                    public boolean test(@NonNull Integer integer, @NonNull Throwable throwable) throws Exception {
                        return retry(integer, throwable);
                    }
                });
    }
    private Flowable<Range> progressObservable(final Response<ResponseBody> responseBodyResponse, final Range range) {
        return Flowable.create(new FlowableOnSubscribe<Range>() {
            @Override
            public void subscribe(FlowableEmitter<Range> emitter) throws Exception {
                if (responseBodyResponse.code() == 200 || responseBodyResponse.code() == 206) {
                    mFileHelper.writeResponseBodyToDisk(emitter, responseBodyResponse, range);
                } else {
                    emitter.onError(new Exception("http response code error,code is " + responseBodyResponse.code()));
                }
            }
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.computation());
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
