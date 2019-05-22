package com.example.janus_android_jw.ota;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import com.example.janus_android_jw.LaunchActivity;
import com.huizhou.jimi.otasdk.OtaManager;
import com.huizhou.jimi.otasdk.feedback.FeedbackManager;
import com.huizhou.jimi.otasdk.update.UpdateManager;

/**
 * 实现升级后自动打开app的功能。<br/>
 * 1 OTA_OPEN_APP_AFTER_UPDATE = false：升级前如果app处于打开状态，升级后自动打开该app。<br/>
 * 2 OTA_OPEN_APP_AFTER_UPDATE = true：不管升级前app有没有打开，升级后都自动打开该app。<br/>
 */
public class AppInstallReceiver extends BroadcastReceiver {
    public static final String TAG = AppInstallReceiver.class.getSimpleName();
    private static boolean mIsWorking = false;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!OtaConfig.CONFIG_OTA) {
            return;
        }

        if (mIsWorking) {
            return;
        }
        if ("android.intent.action.PACKAGE_REPLACED".equals(intent.getAction())) {
            String packageData = intent.getDataString();
            String packageName = null;
            try {
                if (packageData != null) {
                    packageName = packageData.split(":")[1];
                    Log.d("long","packageName = " + packageName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if ((null != packageName) && packageName.equals(context.getPackageName())) {
                Log.d(TAG, intent.getAction() + ", packageName: " + packageName);
                mIsWorking = true;
                Thread mThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            action(context);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                mThread.setName("JM-AppInstallReceiver");
                mThread.start();
            }
        }
    }

    private void action(final Context context) {
        Log.i(TAG, context.getPackageName() + " PACKAGE_REPLACED");
        UpdateManager updateManager = UpdateManager.getInstance();
        FeedbackManager.getInstance().uploadUpdateResult(1); //update success
        if (updateManager.getRebootAfterUpdateFlag()) {
            Log.i(TAG, "RebootAfterUpdate");
            updateManager.resetUpdateFlag();
            PowerManager pManager=(PowerManager) context.getSystemService(Context.POWER_SERVICE);
            pManager.reboot("RebootAfterUpdate");
            return;
        }

        if (!OtaManager.isCurrentPackageRunning()
                && (updateManager.getOpenAppAfterUpdateFlag() || OtaConfig.OTA_OPEN_APP_AFTER_UPDATE)) {
            Log.i(TAG, "Restart MainActivity");
            Intent activityIntent = new Intent(context, LaunchActivity.class);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);
        }
        updateManager.resetUpdateFlag();
        mIsWorking = false;
    }


}

