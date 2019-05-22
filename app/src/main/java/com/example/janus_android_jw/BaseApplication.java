package com.example.janus_android_jw;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDexApplication;

import com.example.janus_android_jw.ota.OtaConfig;
import com.huizhou.jimi.otasdk.OtaManager;
import com.huizhou.jimi.otasdk.crash.CrashManager;
import com.huizhou.jimi.otasdk.feedback.FeedbackManager;
import com.huizhou.jimi.otasdk.update.UpdateManager;

public class BaseApplication extends MultiDexApplication {
    private static BaseApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        if (OtaConfig.CONFIG_OTA) {
            OtaManager otaManager = OtaManager.getInstance();
            otaManager.setContext(this);
            otaManager.setAppName(this.getResources().getString(R.string.app_name));
            /**
             *  sdk 默认使用IMEI作为Device id。
             *  如需使用其他id，请调用setDeviceSn。
             */
//            otaManager.setDeviceSn("12345678");

            /**
             *  sdk 默认使用服务器http://ota.jimi-iot.com/apiv1/app
             *  如需使用其他服务器，请调用setUrl。
             */
//            otaManager.setUrl("http://ota.jimi-iot.com/apiv1/app");

            CrashManager crashManager = CrashManager.getInstance();
            crashManager.register();

            UpdateManager.startService();
            FeedbackManager.getInstance().uploadLogin("App Login", 5);
        }
    }

    public static BaseApplication getInstance() {
        return instance;
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        android.support.multidex.MultiDex.install(this);
    }
}
