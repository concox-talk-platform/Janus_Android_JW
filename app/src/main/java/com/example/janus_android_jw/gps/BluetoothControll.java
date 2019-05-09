package com.example.janus_android_jw.gps;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;

import java.util.Formatter;

import static java.lang.Math.pow;
import static java.lang.StrictMath.abs;

public class BluetoothControll {

    private BluetoothAdapter mBluetoothAdapter = null;
    private Context mContext;
    private BluetoothInfo info;

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    scanBluetooth();
                    break;
                default:
                    break;
            }
        }
    };
    public BluetoothInfo getBluetoothStrength(){
        return info ;
    }
    BluetoothControll(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        info = new BluetoothInfo();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND); // 注册广播
        context.registerReceiver(receiver, filter); // 搜索完成的广播
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); // 注册广播
        context.registerReceiver(receiver, filter);

    }

    public void scanBluetooth() {
        if (mBluetoothAdapter == null) {
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
        if(mBluetoothAdapter.isDiscovering()){
            return;
        }
        mBluetoothAdapter.startDiscovery();
    }

    protected int bluetoothNumber = 0;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {   // 收到的广播类型
            String action = intent.getAction();   // 发现设备的广播
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {    // 从intent中获取设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                /*String aa = tvDevices.getText().toString() + "";
                if (aa.contains(device.getAddress())) {
                    return;
                } else { */    // 判断是否配对过
                    //if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        short rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);

                        if(bluetoothNumber==0){
                            info.setFirstBt(rssi);
                        }else if(bluetoothNumber==1){
                            info.setSecondBt(rssi);
                        }else if(bluetoothNumber==2){
                            info.setThirdBt(rssi);
                        }
                        bluetoothNumber++;
                        int iRssi = abs(rssi);  // 将蓝牙信号强度换算为距离
                        double power = (iRssi - 59) / 25.0;
                        String mm = new Formatter().format("%.2f", pow(10, power)).toString();
                        //tvDevices.append(device.getName() + ":" + device.getAddress() + " ：" + mm + "m" + "\n");
                    //}
                //}
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                bluetoothNumber = 0;
                mHandler.sendEmptyMessageDelayed(1, 1*60*1000);
            }
        }
    };

}