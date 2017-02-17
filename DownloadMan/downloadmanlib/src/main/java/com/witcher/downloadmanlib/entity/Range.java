package com.witcher.downloadmanlib.entity;

import android.os.Parcel;
import android.os.Parcelable;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by witcher on 2017/2/8 0008.
 */
@Entity
public class Range implements Parcelable {
    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private String url;
    private String name;
    public long start;
    public long end;
    public long size;
    public long progress;

    @Override
    public String toString() {
        return "Range{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", size=" + size +
                ", progress=" + progress +
                '}';
    }

    public Range(long start, long end) {
        this.start = start;
        this.end = end;
        this.size = end - start + 1;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getStart() {
        return this.start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return this.end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getSize() {
        return this.size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getProgress() {
        return this.progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    @Generated(hash = 791636481)
    public Range(Long id, @NotNull String url, String name, long start, long end,
            long size, long progress) {
        this.id = id;
        this.url = url;
        this.name = name;
        this.start = start;
        this.end = end;
        this.size = size;
        this.progress = progress;
    }

    @Generated(hash = 269891063)
    public Range() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        dest.writeLong(this.start);
        dest.writeLong(this.end);
        dest.writeLong(this.size);
        dest.writeLong(this.progress);
    }

    protected Range(Parcel in) {
        this.id = (Long) in.readValue(Long.class.getClassLoader());
        this.url = in.readString();
        this.name = in.readString();
        this.start = in.readLong();
        this.end = in.readLong();
        this.size = in.readLong();
        this.progress = in.readLong();
    }

    public static final Creator<Range> CREATOR = new Creator<Range>() {
        @Override
        public Range createFromParcel(Parcel source) {
            return new Range(source);
        }

        @Override
        public Range[] newArray(int size) {
            return new Range[size];
        }
    };
}