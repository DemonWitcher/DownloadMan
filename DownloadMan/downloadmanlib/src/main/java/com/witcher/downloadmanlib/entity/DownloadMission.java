package com.witcher.downloadmanlib.entity;

import android.os.Parcel;
import android.os.Parcelable;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Transient;
import org.greenrobot.greendao.annotation.Unique;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;

/**
 * Created by witcher on 2017/2/8 0008.
 */
@Entity
public class DownloadMission implements Parcelable,Comparable<DownloadMission> {
    @Id(autoincrement = true)
    private Long id;
    @Unique
    @NotNull
    private String url;
    private String name;
    private int state;
    private long size;
    @Transient
    private List<Range> ranges = new ArrayList<>();
    @Transient
    private Disposable disposable;

    public long getProgress() {
        long progress = 0;
        for (Range range : ranges) {
            progress += range.progress;
        }
        return progress;
    }

    public void pauseAllRange() {
        if (disposable != null&&!disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    public Disposable getDisposable() {
        return disposable;
    }

    public void setDisposable(Disposable disposable) {
        this.disposable = disposable;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Range> getRanges() {
        return ranges;
    }

    public void setRanges(List<Range> ranges) {
        this.ranges = ranges;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "DownloadMission{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", state=" + state +
                ", size=" + size +
                ", ranges=" + ranges +
                '}';
    }

    @Override
    public int compareTo(DownloadMission o) {
        return (int) (this.id-o.id);
    }

    public static class Builder {
        String url;
        String name;
        int state;

        public Builder setState(int state) {
            this.state = state;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public DownloadMission build() {
            DownloadMission mission = new DownloadMission();
            mission.url = url;
            mission.name = name;
            mission.state = state;
            return mission;
        }
    }

    public DownloadMission() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.id);
        dest.writeString(this.url);
        dest.writeString(this.name);
        dest.writeInt(this.state);
        dest.writeLong(this.size);
        dest.writeTypedList(this.ranges);
    }

    protected DownloadMission(Parcel in) {
        this.id = (Long) in.readValue(Long.class.getClassLoader());
        this.url = in.readString();
        this.name = in.readString();
        this.state = in.readInt();
        this.size = in.readLong();
        this.ranges = in.createTypedArrayList(Range.CREATOR);
    }

    @Generated(hash = 350131653)
    public DownloadMission(Long id, @NotNull String url, String name, int state, long size) {
        this.id = id;
        this.url = url;
        this.name = name;
        this.state = state;
        this.size = size;
    }

    public static final Creator<DownloadMission> CREATOR = new Creator<DownloadMission>() {
        @Override
        public DownloadMission createFromParcel(Parcel source) {
            return new DownloadMission(source);
        }

        @Override
        public DownloadMission[] newArray(int size) {
            return new DownloadMission[size];
        }
    };
}
