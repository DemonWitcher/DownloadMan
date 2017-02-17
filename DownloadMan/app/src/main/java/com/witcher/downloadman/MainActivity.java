package com.witcher.downloadman;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.witcher.downloadmanlib.entity.Constant;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by witcher on 2017/2/16 0016.
 */

public class MainActivity extends Activity {

    ListView lv;
    AddMissionAdapter adapter;
    List<Mission> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }
    private void initView() {
        list = new ArrayList<>();
        list.add(new Mission("mobileqq_android.apk", "https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk"));
        list.add(new Mission("CloudMusic_official_3.7.3_153912.apk", "http://s1.music.126.net/download/android/CloudMusic_official_3.7.3_153912.apk"));
        list.add(new Mission("futureve-mobile-update-release-4.10.1(443).apk", "http://zhstatic.zhihu.com/pkg/store/zhihu/futureve-mobile-update-release-4.10.1(443).apk"));
        list.add(new Mission("zhihu-daily-zhihu-2.5.4(392).apk", "http://zhstatic.zhihu.com/pkg/store/daily/zhihu-daily-zhihu-2.5.4(392).apk"));
        lv = (ListView) findViewById(R.id.lv);
        adapter = new AddMissionAdapter(list, this, new AddMissionAdapter.OnAddListener() {
            @Override
            public void onAdd(int index) {
                try {
                    int code = MyApp.getDownloadMgr().addMission(list.get(index).name, list.get(index).url);
                    if (code == Constant.AddMission.IS_EXIST) {
                        Toast.makeText(MainActivity.this,"任务已存在",Toast.LENGTH_SHORT).show();
                    } else if (code == Constant.AddMission.SUCCESS) {
                        Toast.makeText(MainActivity.this,"添加成功",Toast.LENGTH_SHORT).show();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        lv.setAdapter(adapter);

        findViewById(R.id.bt1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, DownloadListActivity.class));
            }
        });
        findViewById(R.id.bt2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    MyApp.getDownloadMgr().cancelAll();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                Toast.makeText(MainActivity.this, "清除成功", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
