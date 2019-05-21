package com.example.janus_android_jw.ota;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.huizhou.jimi.otasdk.feedback.FeedbackManager;
import com.huizhou.jimi.otasdk.update.UpdateManager;


/**
 *  开机启动网络升级Service以及上报开机信息。
 */
public class BootReceiver extends BroadcastReceiver {
    public static final String TAG = BootReceiver.class.getSimpleName();
    static final String BOOT_ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!OtaConfig.CONFIG_OTA) {
            return;
        }

        if (BOOT_ACTION.equals(intent.getAction())) {
            Thread mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10 * 1000);
                        action();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            mThread.setName("JM-BootReceiver");
            mThread.start();
        }
    }

    private void action() {
        Log.i(TAG, "start update service");
        UpdateManager.startService();//service的启动会发起一次自动更新
        FeedbackManager.getInstance().uploadStatus("BOOT_COMPLETED");
    }




}




