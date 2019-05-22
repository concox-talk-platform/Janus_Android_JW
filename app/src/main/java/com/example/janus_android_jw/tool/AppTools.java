package com.example.janus_android_jw.tool;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import static android.content.Context.TELEPHONY_SERVICE;

public class AppTools {
    private static String appVersion = "1.0.1";
    private static int appVersionCode = 101;

    //生产--grpc
    //public static String host = "ptt.jimilab.com";
    //public static int port = 9001;
    //生产--janus
    //public static String JANUS_URI = "ws://ptt.jimilab.com:9188";

    //测试--grpc-联通
    public static String host = "114.119.113.97";
    public static int port = 9001;
    //测试--janus-联通
    public static String JANUS_URI = "ws://114.119.113.97:9188";

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
