package com.example.janus_android_jw.gps;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.util.List;

public class WifiControll {
    private WifiManager mWifiManager = null;
    private Context mContext;


    WifiControll(Context context){
        mContext = context;
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    protected WifiData getWifiData(){
        WifiData wifiList = new WifiData();
        int i = 0;
        WifiInfo info = mWifiManager.getConnectionInfo();
                    /*if(info != null){
                        int rssi = info.getRssi();
                        int strength = -1;
                        if(info.getBSSID()!=null){
                            strength = WifiManager.calculateSignalLevel(rssi,5);
                        }
                        Toast.makeText(LocationService.this, "rssi="+rssi+" strength＝"+strength, Toast.LENGTH_SHORT).show();
                    }*/
        List<ScanResult> scanResults=mWifiManager.getScanResults();
        for (ScanResult scanResult : scanResults) {
                        /*tv.append("\n设备名："+scanResult.SSID
                                +" 信号强度："+scanResult.level+"/n :"+mWifiManager.calculateSignalLevel(scanResult.level,4));*/
             if(i==0){
                 wifiList.setFirstWifi(mWifiManager.calculateSignalLevel(scanResult.level,4));
             }else if(i==1){
                 wifiList.setSecondWifi(mWifiManager.calculateSignalLevel(scanResult.level,4));
             }else if(i==2){
                 wifiList.setThirdWifi(mWifiManager.calculateSignalLevel(scanResult.level,4));
             }else if(i>3){
                 break;
             }
            i++;
        }

        return wifiList;
    }
}
