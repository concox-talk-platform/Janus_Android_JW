package com.example.janus_android_jw.tool;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import static android.content.Context.TELEPHONY_SERVICE;

public class AppTools {
    private static String appVersion = "1.0.1";
    private static int appVersionCode = 101;

    //测试
    public static String host = "113.105.153.240";
    public static int port = 9001;

    //生产
    //public static String host = "ptt.jimilab.com";
    //public static int port = 9001;


    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

}
