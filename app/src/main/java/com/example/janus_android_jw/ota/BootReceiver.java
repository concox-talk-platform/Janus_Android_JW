package com.example.janus_android_jw.ota;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.janus_android_jw.LaunchActivity;
import com.huizhou.jimi.otasdk.feedback.FeedbackManager;
import com.huizhou.jimi.otasdk.update.UpdateManager;


/**
 *  开机启动网络升级Service以及上报开机信息。
 */
public class BootReceiver extends BroadcastReceiver {
    static final String BOOT_ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BOOT_ACTION.equals(intent.getAction())) {
            //开机启动
            Intent mainActivityIntent = new Intent(context, LaunchActivity.class);
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainActivityIntent);

            if (!OtaConfig.CONFIG_OTA) {//没有ota功能就返回
                return;
            } else {
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
    }

    private void action() {
//        UpdateManager.startService();//service的启动会发起一次自动更新,惠州张工表示还会记录应用重启信息
        FeedbackManager.getInstance().uploadStatus("BOOT_COMPLETED");
    }




}




