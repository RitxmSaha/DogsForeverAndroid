package com.example.dogsforeverandroid;

import android.graphics.Bitmap;

import java.util.HashMap;
import java.util.Map;

public class Dog {
    private String name;
    private String medications;
    private String feedInstr;
    private String handleInfo;
    private String misc;
    private int dogUID;
    private Bitmap dogPhoto;
    private boolean archived;

    public Dog() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Dog(String name, String medications,String feedInstr, String handleInfo, String misc, int dogUID, Bitmap dogPhoto) {
        this.name = name;
        this.medications = medications;
        this.feedInstr = feedInstr;
        this.handleInfo = handleInfo;
        this.misc = misc;
        this.dogUID = dogUID;
        this.dogPhoto = dogPhoto;
        this.archived = false;
    }

    public Dog(String name, String medications,String feedInstr, String handleInfo, String misc, int dogUID, Bitmap dogPhoto,boolean archived) {
        this.name = name;
        this.medications = medications;
        this.feedInstr = feedInstr;
        this.handleInfo = handleInfo;
        this.misc = misc;
        this.dogUID = dogUID;
        this.dogPhoto = dogPhoto;
        this.archived = archived;
    }
    //nonzero shelterID are real shelter ID's.

    public Map<String,Object> getTaskMap() {
        Map<String,Object> taskMap = new HashMap<>();
        taskMap.put("name", name);
        taskMap.put("medications", medications);
        taskMap.put("feedInstr", feedInstr);
        taskMap.put("handleInfo", handleInfo);
        taskMap.put("misc", misc);
        taskMap.put("dogUID",dogUID);
        taskMap.put("archived",archived);
        return taskMap;
    }

    public Bitmap getDogPhoto() {
        return this.dogPhoto;
    }

    public void setBitmap(Bitmap bm) {
        this.dogPhoto = bm;
    }

    public int getDogUID() {
        return this.dogUID;
    }

    public String getListViewString() {
        return "name: "+name+" id: "+dogUID;
    }

    public String getName() {
        return name;
    }

    public String getHandleInfo() {
        return handleInfo;
    }

    public String getFeedInstr() {
        return feedInstr;
    }

    public String getMedications() {
        return medications;
    }

    public String getMisc() {
        return misc;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMedications(String medications) {
        this.medications = medications;
    }

    public void setFeedInstr(String feedInstr) {
        this.feedInstr = feedInstr;
    }

    public void setHandleInfo(String handleInfo) {
        this.handleInfo = handleInfo;
    }

    public void setMisc(String misc) {
        this.misc = misc;
    }

    public void setDogUID(int dogUID) {
        this.dogUID = dogUID;
    }

    public void setDogPhoto(Bitmap dogPhoto) {
        this.dogPhoto = dogPhoto;
    }

    public boolean isArchived() {return archived;}

    public void setArchived(boolean archived) {this.archived = archived;}
}
