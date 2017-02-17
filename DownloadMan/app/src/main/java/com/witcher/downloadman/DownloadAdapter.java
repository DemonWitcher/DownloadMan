package com.witcher.downloadman;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.witcher.downloadmanlib.entity.DownloadMission;
import com.witcher.downloadmanlib.entity.MissionState;
import com.witcher.downloadmanlib.util.Util;

import java.util.List;

/**
 * Created by witcher on 2017/2/14 0014.
 */

public class DownloadAdapter extends BaseAdapter {

    private List<DownloadMission> list;
    private List<DownloadMission> lastList;
    private Context context;
    private ActionListener listener;

    public DownloadAdapter(List<DownloadMission> list, Context context,ActionListener listener) {
        this.list = list;
        this.listener = listener;
        this.context = context;
    }

    public void setData(List<DownloadMission> list){
        lastList = this.list;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.item_download, null);
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.progress = (TextView) convertView.findViewById(R.id.progress);
            holder.speed = (TextView) convertView.findViewById(R.id.speed);
            holder.state = (TextView) convertView.findViewById(R.id.state);
            holder.size = (TextView) convertView.findViewById(R.id.size);
            holder.start = (Button) convertView.findViewById(R.id.start);
            holder.cancel = (Button) convertView.findViewById(R.id.cancel);
            holder.progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final DownloadMission mission  = list.get(position);
        holder.name.setText(mission.getName());
        holder.state.setText(Util.formatState(mission.getState()));
        holder.size.setText(Util.formatSize(mission.getSize()));
        holder.progress.setText(Util.formatPercent(mission.getSize(),mission.getProgress()));
        try {
            long speed = (mission.getProgress()-lastList.get(position).getProgress())*2;
            speed = speed > 0 ?speed*2:0;
            holder.speed.setText(Util.formatSize(speed)+"/秒");
        }catch (Exception e){
        }
        holder.progressBar.setIndeterminate(mission.getState() == MissionState.CONNECTING);
        holder.progressBar.setMax((int) mission.getSize());
        holder.progressBar.setProgress((int) mission.getProgress());
        if(mission.getState() == MissionState.PAUSE){
            holder.start.setText("开始");
        }else {
            holder.start.setText("暂停");
        }
        holder.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.cancel(position);
            }
        });
        holder.start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mission.getState() == MissionState.PAUSE){
                    listener.start(position);
                }else {
                    listener.pause(position);
                }
            }
        });
        convertView.setTag(holder);
        return convertView;
    }

    private class ViewHolder {
        TextView name;
        TextView progress;
        TextView speed;
        TextView state;
        TextView size;
        Button start;
        Button cancel;
        ProgressBar progressBar;
    }
    public interface ActionListener{
        void start(int index);
        void pause(int index);
        void cancel(int index);
    }
}
