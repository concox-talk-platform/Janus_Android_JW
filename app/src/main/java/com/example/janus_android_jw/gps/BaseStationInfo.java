package com.example.janus_android_jw.gps;

public class BaseStationInfo {
    private int mcc;//移动国家代码（中国的为460）
    private int mnc;//移动网络号码（中国移动为00，中国联通为01）
    private int lac;//位置区域码
    private int cid;//基站编号
    private int firstBs;//第一个邻区基站
    private int firstBsLac;
    private int firstBsCid;
    private int secondBs;//第二个邻区基站
    private int secondBsLac;
    private int secondBsCid;
    private int thirdBs;//第三个邻区基站
    private int thirdBsLac;
    private int thirdBsCid;

    public int getFirstBsLac() {
        return firstBsLac;
    }

    public void setFirstBsLac(int firstBsLac) {
        this.firstBsLac = firstBsLac;
    }

    public int getFirstBsCid() {
        return firstBsCid;
    }

    public void setFirstBsCid(int firstBsCid) {
        this.firstBsCid = firstBsCid;
    }

    public int getSecondBsLac() {
        return secondBsLac;
    }

    public void setSecondBsLac(int secondBsLac) {
        this.secondBsLac = secondBsLac;
    }

    public int getSecondBsCid() {
        return secondBsCid;
    }

    public void setSecondBsCid(int secondBsCid) {
        this.secondBsCid = secondBsCid;
    }

    public int getThirdBsLac() {
        return thirdBsLac;
    }

    public void setThirdBsLac(int thirdBsLac) {
        this.thirdBsLac = thirdBsLac;
    }

    public int getThirdBsCid() {
        return thirdBsCid;
    }

    public void setThirdBsCid(int thirdBsCid) {
        this.thirdBsCid = thirdBsCid;
    }

    public int getMcc() {
        return mcc;
    }

    public void setMcc(int mcc) {
        this.mcc = mcc;
    }

    public int getMnc() {
        return mnc;
    }

    public void setMnc(int mnc) {
        this.mnc = mnc;
    }

    public int getLac() {
        return lac;
    }

    public void setLac(int lac) {
        this.lac = lac;
    }

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public int getFirstBs() {
        return firstBs;
    }

    public void setFirstBs(int firstBs) {
        this.firstBs = firstBs;
    }

    public int getSecondBs() {
        return secondBs;
    }

    public void setSecondBs(int secondBs) {
        this.secondBs = secondBs;
    }

    public int getThirdBs() {
        return thirdBs;
    }

    public void setThirdBs(int thirdBs) {
        this.thirdBs = thirdBs;
    }

}
