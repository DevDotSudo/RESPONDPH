package com.ionres.respondph.common.model;

public class DisasterModel {

    private int disasterId;
    private String disasterType;
    private String disasterName;

    public DisasterModel(int disasterId, String disasterType, String disasterName) {
        this.disasterId = disasterId;
        this.disasterType = disasterType;
        this.disasterName = disasterName;
    }

    public int getDisasterId() {
        return disasterId;
    }

    public String getDisasterType() {
        return disasterType;
    }

    public String getDisasterName(){
        return  disasterName;
    }

    @Override
    public String toString() {
        return disasterType +" "+ disasterName;
    }
}