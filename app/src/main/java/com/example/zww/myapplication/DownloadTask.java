package com.example.zww.myapplication;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by zww on 2017/8/2.
 * 下载的异步处理，启动子线程来处理
 */

public class DownloadTask extends AsyncTask<String, Integer, Integer> {

    //doInBackground的返回值
    private static final int TYPE_SUCCESS = 1;
    private static final int TYPE_FAILED = 2;
    private static final int TYPE_PAUSE = 3;
    private static final int TYPE_CANCEL = 4;

    private DownLoaderListener downLoaderListener;

    private boolean isCanceled = false;

    private boolean isPaused = false;

    private int lastProgress;

    public DownloadTask(DownLoaderListener downLoaderListener) {
        this.downLoaderListener = downLoaderListener;
    }

    @Override
    protected Integer doInBackground(String... params) {
        InputStream inputStream = null;
        RandomAccessFile saveFile = null;
        File file;

        long downloadLength = 0;//记录已经下载的长度
        String downloadUrl = params[0];
        String fileName = "";
        if (null != downloadUrl) {
            fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
        }
        String direPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        file = new File(direPath + fileName);
        if (file.exists()) {
            downloadLength = file.length();
        }

        try {
            long contentLength = getContentLength(downloadUrl);
            if (contentLength == 0) {
                return TYPE_FAILED;
            } else if (downloadLength == contentLength) {
                return TYPE_SUCCESS;
            }

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    //断点下载，指定从哪个字节开始下载
                    .addHeader("RANGE", "bytes=" + downloadLength + "-")
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if (null != response) {
                inputStream = response.body().byteStream();
                saveFile = new RandomAccessFile(file, "rw");
                saveFile.seek(downloadLength);
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while ((len = inputStream.read()) != -1) {
                    if (isCanceled) {
                        return TYPE_CANCEL;
                    } else if (isPaused) {
                        return TYPE_PAUSE;
                    } else {
                        total += len;
                    }
                    saveFile.write(b, 0, len);
                    int progress = (int) ((total + downloadLength) * 100 / contentLength);
                    publishProgress(progress);
                }
            }

            response.body().close();
            return TYPE_SUCCESS;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (saveFile != null) {
                    saveFile.close();
                }
                if (isCanceled && file != null) {
                    file.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    @Override
    protected void onPostExecute(Integer integer) {
        super.onPostExecute(integer);
        switch (integer) {
            case TYPE_SUCCESS:
                downLoaderListener.onSuccess();
                break;
            case TYPE_FAILED:
                downLoaderListener.onFailed();
                break;
            case TYPE_PAUSE:
                downLoaderListener.onPaused();
                break;
            case TYPE_CANCEL:
                downLoaderListener.onCanceled();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        int progress = values[0];
        if (progress > lastProgress) {
            downLoaderListener.onProgress(progress);
            Log.e("11111111111111111", "" + progress);
            lastProgress = progress;
        }
    }

    public void pauseDownload() {
        isPaused = true;
    }

    public void cancelDownload() {
        isCanceled = true;
    }

    private long getContentLength(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }
}
