package com.example.janus_android_jw.gps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

import org.json.JSONException;

import java.util.List;

public class TelephonyControll {

    private Context mContext;

    TelephonyControll(Context context){
        mContext = context;
    }
    /**
     * 获取手机基站信息
     *
     * @throws JSONException
     */
    public BaseStationInfo getGSMCellLocationInfo() {
        BaseStationInfo bsInfo = new BaseStationInfo();
        if(!hasSimCard()){
            return bsInfo;
        }
        TelephonyManager manager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        String operator = manager.getNetworkOperator();
        /**通过operator获取 MCC 和MNC */
        int mcc = Integer.parseInt(operator.substring(0, 3));
        int mnc = Integer.parseInt(operator.substring(3));
        int lac = 0;
        int cellid = 0;
        int i = 0;
        bsInfo.setMcc(mcc);
        bsInfo.setMnc(mnc);
        if (ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {

            if(manager.getPhoneType()==TelephonyManager.PHONE_TYPE_CDMA){
                /**通过CdmaCellLocation获取中国电信 LAC 和cellID */
                 CdmaCellLocation location1 = (CdmaCellLocation) manager.getCellLocation();
                 lac = location1.getNetworkId();
                cellid = location1.getBaseStationId();
            }else{
                GsmCellLocation location = (GsmCellLocation) manager.getCellLocation();
                /**通过GsmCellLocation获取中国移动和联通 LAC 和cellID */
                 lac = location.getLac();
                 cellid = location.getCid();
            }
            bsInfo.setLac(lac);
            bsInfo.setCid(cellid);
            //Toast.makeText(mContext, "this is telephony mcc="+mcc+" mnc="+mnc+" lac="+lac+" ce="+cellid, Toast.LENGTH_SHORT).show();
            int strength = 0;
            /**通过getNeighboringCellInfo获取BSSS */
            List<NeighboringCellInfo> infoLists = manager.getNeighboringCellInfo();
            //Toast.makeText(mContext, "this is telephony size="+infoLists.size(), Toast.LENGTH_SHORT).show();
            for (NeighboringCellInfo info : infoLists) {
                strength += (-133 + 2 * info.getRssi());// 获取邻区基站信号强度
                int nlac = info.getLac();// 取出当前邻区的LAC
                int ncid = info.getCid();// 取出当前邻区的CID
                System.out.println("rssi:" + info.getRssi() + "   strength:" + strength);
                if (i==0){
                    //Toast.makeText(mContext, "this is telephony num1="+strength, Toast.LENGTH_SHORT).show();
                    bsInfo.setFirstBs(strength);
                    bsInfo.setFirstBsLac(nlac);
                    bsInfo.setFirstBsCid(ncid);
                }else if(i==1){
                    bsInfo.setSecondBs(strength);
                    bsInfo.setSecondBsCid(ncid);
                    bsInfo.setSecondBsLac(nlac);
                    //Toast.makeText(mContext, "this is telephony num2="+strength, Toast.LENGTH_SHORT).show();
                }else if(i==2){
                    bsInfo.setThirdBs(strength);
                    bsInfo.setThirdBsLac(nlac);
                    bsInfo.setThirdBsCid(ncid);
                    //Toast.makeText(mContext, "this is telephony num3="+strength, Toast.LENGTH_SHORT).show();
                }
                i++;
            }
        }
        return bsInfo;
    }

    public boolean hasSimCard() {
        TelephonyManager mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        int simState = mTelephonyManager.getSimState();
        boolean result = true;
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                result = false; // 没有SIM卡
                break;
            case TelephonyManager.SIM_STATE_UNKNOWN:
                result = false;
                break;
        }
        return result;
    }
}