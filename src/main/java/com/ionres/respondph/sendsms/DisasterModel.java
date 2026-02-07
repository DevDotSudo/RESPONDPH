package com.ionres.respondph.sendsms;

public class DisasterModel {
    private int disasterId;
    private String type;
    private String name;
    private String date;

    public DisasterModel() {}

    public DisasterModel(int disasterId, String type, String name, String date) {
        this.disasterId = disasterId;
        this.type = type;
        this.name = name;
        this.date = date;
    }

    public int getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(int disasterId) {
        this.disasterId = disasterId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return type + " - " + name + " (" + date + ")";
    }
}