package com.witcher.downloadman;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

/**
 * Created by witcher on 2017/2/14 0014.
 */

public class AddMissionAdapter extends BaseAdapter {

    private List<Mission> list;
    private Context context;
    private OnAddListener onAddListener;

    public AddMissionAdapter(List<Mission> list, Context context,OnAddListener listener) {
        this.list = list;
        this.context = context;
        this.onAddListener = listener;
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
            convertView = View.inflate(context, R.layout.item_add_mission, null);
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.add = (Button) convertView.findViewById(R.id.add);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final Mission mission = list.get(position);
        holder.name.setText(mission.name);
        holder.add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddListener.onAdd(position);
            }
        });
        convertView.setTag(holder);
        return convertView;
    }

    private class ViewHolder {
        TextView name;
        Button add;
    }
    public interface OnAddListener {
        void onAdd(int index);
    }
}
