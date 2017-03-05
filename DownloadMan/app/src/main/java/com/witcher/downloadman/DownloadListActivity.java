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
import com.witcher.downloadmanlib.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by witcher on 2017/2/13 0013.
 */

public class DownloadListActivity extends AppCompatActivity {

    DownloadManager mgr;
    ListView lv;
    DownloadAdapter adapter;
    List<DownloadMission> list;
    Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_list);
        initView();
        initData();
    }

    private void initData() {
        mgr = MyApp.getDownloadMgr();
        startQuery();
    }

    private void startQuery() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        List<DownloadMission> list = mgr.getAllMission();
//                        if(list!=null){
                            DownloadListActivity.this.list =list;
                            adapter.setData(DownloadListActivity.this.list);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.notifyDataSetChanged();
                                }
                            });
//                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    private void initView() {
        lv = (ListView) findViewById(R.id.lv);
        list = new ArrayList<>();
        adapter = new DownloadAdapter(list, this, new DownloadAdapter.ActionListener() {
            @Override
            public void start(final int index) {
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

            @Override
            public void pause(final int index) {
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

            @Override
            public void cancel(final int index) {
                try {
                    L.i("取消了:" + index);
                    mgr.cancelMission(list.get(index).getUrl());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
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

        findViewById(R.id.bt3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Util.printList(mgr.getAllMission());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        thread.interrupt();
    }
}
