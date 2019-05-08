package com.example.janus_android_jw.tool;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadManager {

    private static String TAG = "DownloadManager";

    private static DownloadManager mDownloadManager;
    OkHttpClient mOkHttpClient;

    private DownloadManager(){
        mOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10000, TimeUnit.SECONDS)
                .readTimeout(60*1000,TimeUnit.SECONDS)
                .build();
    }

    public static DownloadManager getInstance(){
        if(mDownloadManager==null){
            mDownloadManager = new DownloadManager();
        }
        return mDownloadManager;
    }

    public void downloadFile(String url, final String destFileDir,final String destFileName,final OnDownloadListener listener){
        Request request = new Request.Builder()
                .url(url)
                .build();
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                listener.onDownloadFailed("");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                Log.d(TAG, TAG + " this is StreamObserver receive text  55555555");
                //储存下载文件的目录
                File dir = new File(destFileDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File file = new File(dir, destFileName);
                try {
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    Log.d(TAG, TAG + " this is StreamObserver downloading 777 total="+total);
                    while ((len = is.read(buf)) != -1) {
                        //Log.d(TAG, TAG + " this is StreamObserver downloading 888");
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        //下载中更新进度条
                        listener.onDownloading(progress);
                    }
                    fos.flush();
                    //下载完成
                    listener.onDownloadSuccess("");
                    Log.d(TAG, TAG + " this is StreamObserver download successful ...");
                } catch (Exception e) {
                    listener.onDownloadFailed("");
                    e.printStackTrace();
                    Log.d(TAG, TAG + " this is StreamObserver download onResponse e="+e.getMessage());
                }finally {
                    Log.d(TAG, TAG + " this is StreamObserver download finally");
                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {

                    }
                }
            }
        });
    }

    public interface OnDownloadListener {
        /**
         * 下载成功
         */
        void onDownloadSuccess(String msg);

        /**
         * @param progress 下载进度
         */
        void onDownloading(int progress);

        /**
         * 下载失败
         */
        void onDownloadFailed(String msg);
    }

}
