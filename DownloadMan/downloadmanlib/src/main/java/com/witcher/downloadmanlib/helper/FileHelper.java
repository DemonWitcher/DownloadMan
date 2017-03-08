package com.witcher.downloadmanlib.helper;

import android.os.Environment;

import com.witcher.downloadmanlib.entity.Range;
import com.witcher.downloadmanlib.util.L;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import io.reactivex.FlowableEmitter;
import okhttp3.ResponseBody;
import retrofit2.Response;

import static com.witcher.downloadmanlib.util.Util.closeQuietly;
import static java.nio.channels.FileChannel.MapMode.READ_WRITE;

/**
 * Created by witcher on 2017/2/8 0008.
 */

public class FileHelper {

    private static final int BUFFER_SIZE = 32 * 1024;

    public static void createDownloadDirs() {
        mkdirs(getDownloadPath());
    }

    public static String getDownloadPath() {
        return Environment.getExternalStorageDirectory() + File.separator + "downloadman" + File.separator + "download";
    }

    public static boolean downloadFileExists(String name) {
        return getFile(getDownloadPath() + File.separator + name).exists();
    }

    private static File getFile(String path) {
        return new File(path);
    }

    public static void mkdirs(String... paths) {
        for (String each : paths) {
            File file = new File(each);
            if (file.exists() && file.isDirectory()) {
            } else {
                file.mkdirs();
            }
        }
    }

    private void createFile(File file) throws IOException {
        if (!file.exists()) {
            boolean flag = file.createNewFile();
            if (!flag) {
                throw new IOException();
            }
        }
        if (!file.isFile()) {
            file.delete();
            boolean flag = file.createNewFile();
            if (!flag) {
                throw new IOException();
            }
        }
    }

    public static void writeResponseBodyToDisk(FlowableEmitter<Range> emitter, Response<ResponseBody> responseBodyResponse, Range range) {
        L.i("writeResponseBodyToDisk");
        try {
            RandomAccessFile raf = new RandomAccessFile(getDownloadPath() + File.separator + range.getName(), "rws");
            FileChannel saveChannel = raf.getChannel();
            InputStream inputStream = null;

            try {
                int readLen;
                byte[] buffer = new byte[BUFFER_SIZE];

                long contentLength = responseBodyResponse.body().contentLength();
                if (range.getProgress() == 0) {
                    range.setSize(contentLength);
                }
                L.i("contentLength:" + contentLength);
                long progress = range.getProgress();

                MappedByteBuffer saveBuffer = saveChannel.map(READ_WRITE, range.start + range.getProgress(), contentLength);//position,size
                inputStream = responseBodyResponse.body().byteStream();

                while ((readLen = inputStream.read(buffer)) != -1) {
                    saveBuffer.put(buffer, 0, readLen);
                    progress += readLen;
                    range.progress = progress;
                    emitter.onNext(range);
                }
                emitter.onComplete();
            } catch (Exception e) {
                if(!emitter.isCancelled()){
                    emitter.onError(e);
                }
            } finally {
                closeQuietly(raf);
                closeQuietly(saveChannel);
                closeQuietly(inputStream);
                closeQuietly(responseBodyResponse.body());
            }
        } catch (IOException e) {
            if(!emitter.isCancelled()){
                emitter.onError(e);
            }
        }
    }

    public static void deleteAll() {
        File[] files = new File(getDownloadPath()).listFiles();
        for (File file : files) {
            file.delete();
        }
    }

    public static void deleteByName(String name) {
        File file = new File(getDownloadPath() + File.separator + name);
        if (file.exists()) {
            file.delete();
        }
    }
}
