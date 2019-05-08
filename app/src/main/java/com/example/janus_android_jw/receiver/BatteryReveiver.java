package com.example.janus_android_jw.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;


public class BatteryReveiver extends BroadcastReceiver {
    public static int mBatteryPower;
    private boolean isLowPower;
    private Handler mHandler;

    public BatteryReveiver(Handler handler){
        mHandler = handler;
    }
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
            //Log.d("MainActivity","MainActivity BatteryReveiver mBatteryPower="+mBatteryPower);
            //低电量告警，使用android自带的文字转语音引擎
            if (!isLowPower && mBatteryPower < 20) {
                mHandler.sendEmptyMessage(302);
                isLowPower = true;
            }
            if(isLowPower&&mBatteryPower>=10){
                isLowPower = false;
            }
        }
    }
}
