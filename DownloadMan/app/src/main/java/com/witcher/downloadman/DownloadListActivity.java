package com.witcher.downloadman;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.witcher.downloadmanlib.entity.DownloadMission;
import com.witcher.downloadmanlib.manager.DownloadManager;
import com.witcher.downloadmanlib.util.L;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by witcher on 2017/2/13 0013.
 */

public class DownloadListActivity extends AppCompatActivity {

    DownloadManager mgr;
    ListView lv;
    DownloadAdapter adapter;
    List<DownloadMission> list;
    Subscription subscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_list);
        initData();
        initView();
    }

    private void initData() {
        mgr = MyApp.getDownloadMgr();
        startQuery();
    }

    private void startQuery() {
        subscription = Observable.interval(500, TimeUnit.MILLISECONDS, Schedulers.computation())
                .map(new Func1<Long, List<DownloadMission>>() {
                    @Override
                    public List<DownloadMission> call(Long aLong) {
                        try {
                            long time1 = System.currentTimeMillis();
                            List<DownloadMission> list = mgr.getAllMission();
                            long time2 = System.currentTimeMillis();
                            L.i("getAllMission耗时:" + (time2 - time1));
                            return list;
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<DownloadMission>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(List<DownloadMission> list) {
                        if (list != null) {
                            DownloadListActivity.this.list = list;
                            adapter.setData(DownloadListActivity.this.list);
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void initView() {
        lv = (ListView) findViewById(R.id.lv);
        list = new ArrayList<>();
        adapter = new DownloadAdapter(list, this, new DownloadAdapter.ActionListener() {
            @Override
            public void start(final int index) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            long time1 = System.currentTimeMillis();
                            mgr.startMission(list.get(index).getUrl());
                            long time2 = System.currentTimeMillis();
                            L.i("start耗时:" + (time2 - time1));
                            L.i("开始了:" + index);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            @Override
            public void pause(final int index) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            L.i("暂停了:" + index);
                            long time1 = System.currentTimeMillis();
                            mgr.pauseMission(list.get(index).getUrl());
                            long time2 = System.currentTimeMillis();
                            L.i("pause耗时:" + (time2 - time1));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            @Override
            public void cancel(final int index) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            L.i("取消了:" + index);
                            mgr.cancelMission(list.get(index).getUrl());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        lv.setAdapter(adapter);

        findViewById(R.id.bt2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mgr.cancelAll();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                Toast.makeText(DownloadListActivity.this, "清除成功", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        subscription.unsubscribe();
    }
}
