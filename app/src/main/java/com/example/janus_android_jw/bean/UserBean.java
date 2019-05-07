package com.example.janus_android_jw.bean;

import java.util.ArrayList;

public class UserBean {
    private String userName;
    private int userId;
    private int defaultGroupId;
    private ArrayList<GroupBean> groupBeanArrayList;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getDefaultGroupId() {
        return defaultGroupId;
    }

    public void setDefaultGroupId(int defaultGroupId) {
        this.defaultGroupId = defaultGroupId;
    }

    public ArrayList<GroupBean> getGroupBeanArrayList() {
        return groupBeanArrayList;
    }

    public void setGroupBeanArrayList(ArrayList<GroupBean> groupBeanArrayList) {
        this.groupBeanArrayList = groupBeanArrayList;
    }

    private static UserBean userBeanObj = null;
    public static UserBean setUserBean(UserBean userBean){
        userBeanObj = userBean;
        return userBeanObj;
    }
    public static UserBean getUserBean() {
        return userBeanObj;
    }
    public static void clearUserBean(){
        userBeanObj = null;
    }

}
