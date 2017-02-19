package com.witcher.downloadmanlib.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.witcher.downloadmanlib.entity.DownloadMission;
import com.witcher.downloadmanlib.entity.Range;

import java.util.List;

/**
 * Created by witcher on 2017/2/14 0014.
 */

public class DBManager {

    private DaoMaster.DevOpenHelper mHelper;
    private SQLiteDatabase mDb;
    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;
    private Context mContext;
    private static DBManager singleton;

    public static DBManager getSingleton(Context context) {
        if (singleton == null) {
            synchronized (DBManager.class) {
                if (singleton == null) {
                    singleton = new DBManager(context);
                }
            }
        }
        return singleton;
    }

    private DBManager(Context context) {
        this.mContext = context;
        setDatabase();
    }

    private void setDatabase() {
        mHelper = new DaoMaster.DevOpenHelper(mContext, "downloadman-db", null);
        mDb = mHelper.getWritableDatabase();
        mDaoMaster = new DaoMaster(mDb);
        mDaoSession = mDaoMaster.newSession();
    }

    public void addMission(DownloadMission mission){
        mDaoSession.getDownloadMissionDao().insert(mission);
    }

    public void updateMission(DownloadMission mission){
        mDaoSession.getDownloadMissionDao().update(mission);
    }

    public void updateRange(Range range){
        mDaoSession.getRangeDao().update(range);
    }

    public List<DownloadMission> getAllMission(){
        List<DownloadMission> list = mDaoSession.getDownloadMissionDao().loadAll();
        for(int i=0;i<list.size();++i){
            list.get(i).setRanges(getRangeByUrl(list.get(i).getUrl()));
        }
        return list;
    }

    public DownloadMission getMission(String url){
        return mDaoSession.getDownloadMissionDao().queryBuilder().where(DownloadMissionDao.Properties.Url.eq(url)).unique();
    }

    public boolean haveMission(String url){
        return mDaoSession.getDownloadMissionDao().queryBuilder().where(DownloadMissionDao.Properties.Url.eq(url)).list().size() != 0;
    }

    public void deleteAll(){
        mDaoSession.getDownloadMissionDao().deleteAll();
        mDaoSession.getRangeDao().deleteAll();
    }

    public void deleteMission(String url){
        mDaoSession.getDownloadMissionDao().delete(getMission(url));
        mDaoSession.getRangeDao().deleteInTx(getRangeByUrl(url));
    }

    public void addRange(Range range){
        mDaoSession.getRangeDao().insert(range);
    }

    public List<Range> getRangeByUrl(String url){
        return mDaoSession.getRangeDao().queryBuilder().where(RangeDao.Properties.Url.eq(url)).list();
    }

}
