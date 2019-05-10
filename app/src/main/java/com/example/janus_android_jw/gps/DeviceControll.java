package com.example.janus_android_jw.gps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;

import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;


import com.example.janus_android_jw.receiver.BatteryReceiver;

import java.util.Locale;

public class DeviceControll{
    //private int mBatteryPower;
    //private TextToSpeech tts;
    private Context mContext;
    //private boolean isLowPower;

    DeviceControll(Context context) {
        /*IntentFilter filter2 = new IntentFilter();
        filter2.addAction(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(batteryReveiver, filter2);*/
        //tts = new TextToSpeech(context, this);
        mContext = context;
    }

    public String getImei(Context context) {
        String imei = "";
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            imei = telephonyManager.getDeviceId();
        }
        return imei;
    }

    public int getBattery(Context context) {
        int battery = 0;
        BatteryManager manager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        Log.d("location", "Build.VERSION.SDK_INT=" + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= 21)
            battery = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return battery;
    }

    public int getDeviceType() {
        return 1;
    }

    public DeviceInfo getDeviceDetail(Context context) {
        DeviceInfo info = new DeviceInfo();
        info.setImei(getImei(context));
        //info.setBattery(getBattery(context));
        //info.setBattery(mBatteryPower);
        info.setBattery(BatteryReceiver.mBatteryPower);
        info.setType(getDeviceType());
        return info;
    }

    /*private BroadcastReceiver batteryReveiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //如果捕捉到的Action是ACTION_BATTERY_CHANGED则运行onBatteryInforECEIVER()
            if (intent.ACTION_BATTERY_CHANGED.equals(action)) {
                //获得当前电量
                int level = intent.getIntExtra("level", 0);
                //获得手机总电量
                int scale = intent.getIntExtra("scale", 100);
                // 在下面会定义这个函数，显示手机当前电量
                mBatteryPower = level * 100 / scale;
                //低电量告警，使用android自带的文字转语音引擎
                if (!isLowPower && mBatteryPower < 10) {
                    tts.speak("Electricity is less than 20%", TextToSpeech.QUEUE_FLUSH, null);
                    isLowPower = true;
                }
                if(isLowPower&&mBatteryPower>=10){
                    isLowPower = false;
                }
            }
        }
    };*/

    /*@Override
    public void onInit(int status) {
        Log.d("device", "this is device status=" + status);
        // 判断是否转化成功
        if (status == TextToSpeech.SUCCESS) {
            //默认设定语言为中文，原生的android貌似不支持中文。
            if(isZh()){
                int result = tts.setLanguage(Locale.CHINESE);
                Log.d("device", "this is device result=" + result);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.US);
                }
            }else{
                tts.setLanguage(Locale.US);
            }
        }
    }*/

    private boolean isZh() {
        Locale locale = mContext.getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        Log.d("device","this is device and language = "+language);
        if (language.endsWith("zh"))
            return true;
        else
            return false;
    }

}
