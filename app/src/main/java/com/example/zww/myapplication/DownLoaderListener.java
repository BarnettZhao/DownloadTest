package com.example.zww.myapplication;

/**
 * Created by zww on 2017/8/2.
 * 下载的回调监听
 */

public interface DownLoaderListener {
    void onProgress(int progress);

    void onSuccess();

    void onFailed();

    void onPaused();

    void onCanceled();
}
