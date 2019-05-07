package com.example.janus_android_jw.bean;

import java.util.ArrayList;

public class GroupBean {
    private String groupName;
    private int groupId;
    private ArrayList<MessageBean> messageBeanArrayList;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public ArrayList<MessageBean> getMessageBeanArrayList() {
        return messageBeanArrayList;
    }

    public void setMessageBeanArrayList(ArrayList<MessageBean> messageBeanArrayList) {
        this.messageBeanArrayList = messageBeanArrayList;
    }
}
