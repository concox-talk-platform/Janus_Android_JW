package com.example.janus_android_jw.gps;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;


import com.example.janus_android_jw.bean.UserBean;
import com.example.janus_android_jw.tool.AppTools;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import talk_cloud.TalkCloudLocationGrpc;
import talk_cloud.TalkCloudLocationOuterClass;
import talk_cloud.TalkCloudLocationOuterClass.BaseStation;
import talk_cloud.TalkCloudLocationOuterClass.BlueTooth;
import talk_cloud.TalkCloudLocationOuterClass.Device;
import talk_cloud.TalkCloudLocationOuterClass.GPS;
import talk_cloud.TalkCloudLocationOuterClass.Location;
import talk_cloud.TalkCloudLocationOuterClass.Wifi;

public class LocationService extends Service {

    private GPSControll mGPSControll;
    private WifiControll mWifiControll;
    private TelephonyControll mTelephonyControll;
    private DeviceControll mDeviceControll;
    private BluetoothControll mBluetoothControll;
    private final static long LOCATION_SEND_TIME = 5*60 * 1000;//间隔5min发送一次
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:

                    DeviceInfo deviceInfo = mDeviceControll.getDeviceDetail(LocationService.this);
                    GPSInfo gpsInfo = mGPSControll.getCurrentLocationInfo();
                    WifiData wifiData = mWifiControll.getWifiData();
                    BaseStationInfo bsInfo = mTelephonyControll.getGSMCellLocationInfo();
                    BluetoothInfo btInfo = mBluetoothControll.getBluetoothStrength();
                    Log.d("location","LocationService gpsinfo="+gpsInfo);
                    //gps数据为空时，不上传数据
                    if(gpsInfo!=null){
                        new GrpcLocationTask().execute(getLocationData(deviceInfo,gpsInfo,wifiData,bsInfo,btInfo));
                    }
                    mHandler.sendEmptyMessageDelayed(1, LOCATION_SEND_TIME);
                    break;
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
        mHandler.sendEmptyMessageDelayed(1,10*1000);
        //Toast.makeText(this, "start locationservice", Toast.LENGTH_LONG).show();
    }

    private void init(){
        if (mGPSControll == null) {
            mGPSControll = new GPSControll(this);
            mGPSControll.startLocation();
        }
        if (mWifiControll == null) {
            mWifiControll = new WifiControll(this);
        }
        if (mTelephonyControll == null) {
            mTelephonyControll = new TelephonyControll(this);
        }
        if(mDeviceControll == null){
            mDeviceControll = new DeviceControll(this);
        }
        if(mBluetoothControll == null){
            mBluetoothControll = new BluetoothControll(this);
            mBluetoothControll.scanBluetooth();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    private HashMap<String, Object> getLocationData(DeviceInfo dInfo, GPSInfo gpsInfo, WifiData wifiInfo, BaseStationInfo bsInfo,BluetoothInfo btInfo) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        Device device = Device.newBuilder().setId(UserBean.getUserBean().getUserId()).setBattery(dInfo.getBattery()).setDeviceType(dInfo.getType()).build();
        GPS gps = GPS.newBuilder().setLocalTime(gpsInfo.getTime()).setLatitude(gpsInfo.getLatitude()).setLongitude(gpsInfo.getLongitude()).setSpeed(gpsInfo.getSpeed())
                .setCourse(gpsInfo.getBearing()).build();
        BaseStation bStation = BaseStation.newBuilder().setCountry(bsInfo.getMcc()).setOperator(bsInfo.getMnc()).setCid(bsInfo.getCid()).setLac(bsInfo.getLac())
                .setFirstBs(bsInfo.getFirstBs()).setSecondBs(bsInfo.getSecondBs()).setThirdBs(bsInfo.getThirdBs()).build();
        Wifi wifi = Wifi.newBuilder().setFirstWifi(wifiInfo.getFirstWifi()).setSecondWifi(wifiInfo.getSecondWifi()).setThirdWifi(wifiInfo.getThirdWifi()).build();
        BlueTooth blueTooth = BlueTooth.newBuilder().setFirstBt(btInfo.getFirstBt()).setSecondBt(btInfo.getSecondBt()).setThirdBt(btInfo.getThirdBt()).build();
        Location location = Location.newBuilder().setGpsInfo(gps).setBSInfo(bStation).setWifiInfo(wifi).setBtInfo(blueTooth).build();
        map.put("imei",dInfo.getImei());
        map.put("device",device);
        map.put("location",location);
        return map;
    }

    class GrpcLocationTask extends AsyncTask<HashMap<String, Object>, Void, TalkCloudLocationOuterClass.ReportDataResp> {
        private ManagedChannel channel;

        @Override
        protected TalkCloudLocationOuterClass.ReportDataResp doInBackground(HashMap<String, Object>... params) {
            String ime = (String) params[0].get("imei");
            Device dev = (Device) params[0].get("device");
            Location location = (Location) params[0].get("location");
            TalkCloudLocationOuterClass.ReportDataResp replay = null;
            Log.d("location","LocationService imei="+ime);
            Log.d("location","LocationService battery="+dev.getBattery()+" type="+dev.getDeviceType()+"userid="+ UserBean.getUserBean().getUserId());
            Log.d("location","LocationService mcc="+location.getBSInfo().getCountry()+"mnc="+location.getBSInfo().getOperator()+" lac="+location.getBSInfo().getLac()
                     +"cid="+location.getBSInfo().getCid()+" la1="+location.getBSInfo().getFirstBs()+" la2="+location.getBSInfo().getSecondBs());
            Log.d("location","LocationService bt1="+location.getBtInfo().getFirstBt()+"bt2="+location.getBtInfo().getSecondBt()+" bt3="+location.getBtInfo().getThirdBt());
            Log.d("location","LocationService lat="+location.getGpsInfo().getLatitude()+"lon="+location.getGpsInfo().getLongitude()+" spe="+location.getGpsInfo().getSpeed()+
                    "cou="+location.getGpsInfo().getCourse()+"time="+location.getGpsInfo().getLocalTime());
            Log.d("location","LocationService wfir1="+location.getWifiInfo().getFirstWifi()+" wsen2="+location.getWifiInfo().getSecondWifi()+" sth3="+location.getWifiInfo().getThirdWifi());
            try {
            channel = ManagedChannelBuilder.forAddress(AppTools.host, AppTools.port).usePlaintext().build();
            TalkCloudLocationGrpc.TalkCloudLocationBlockingStub stub = TalkCloudLocationGrpc.newBlockingStub(channel);
            TalkCloudLocationOuterClass.ReportDataReq dataReq = TalkCloudLocationOuterClass.ReportDataReq.newBuilder().setIMei(ime).
                    setDeviceInfo(dev).setLocationInfo(location).build();
            replay = stub.reportGPSData(dataReq);
            return replay;
            } catch (Exception e) {
                Log.d("location","LocationService this is error "+e.getMessage());
                return replay;
            }
        }

        @Override
        protected void onPostExecute(TalkCloudLocationOuterClass.ReportDataResp result) {
            super.onPostExecute(result);

            try {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Log.d("location","LocationService this is onPostExecute");
            if(result==null){
                Toast.makeText(LocationService.this, "返回结果为空", Toast.LENGTH_SHORT).show();
            }else{
                Log.d("location","LocationService result="+result.getRes().getCode());
                //Toast.makeText(LocationService.this, "result="+result.getRes().getCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

}
