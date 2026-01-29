package com.ionres.respondph.disaster;

public class DisasterModel {

    private int disasterId;
    private String disasterType;
    private String disasterName;
    private String date;
    private String lat;
    private String longi;
    private String radius;
    private String notes;
    private String regDate;

    public DisasterModel() {
    }

    public DisasterModel(String disasterType, String disasterName,
                         String date, String lat, String longi, String radius,
                         String notes, String regDate) {
        this.disasterType = disasterType;
        this.disasterName = disasterName;
        this.date = date;
        this.lat = lat;
        this.longi = longi;
        this.radius = radius;
        this.notes = notes;
        this.regDate = regDate;
    }

    public int getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(int disasterId) {
        this.disasterId = disasterId;
    }

    public String getDisasterType() {
        return disasterType;
    }

    public void setDisasterType(String disasterType) {
        this.disasterType = disasterType;
    }

    public String getDisasterName() {
        return disasterName;
    }

    public void setDisasterName(String disasterName) {
        this.disasterName = disasterName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLongi() {
        return longi;
    }

    public void setLongi(String longi) {
        this.longi = longi;
    }

    public String getRadius() {
        return radius;
    }

    public void setRadius(String radius) {
        this.radius = radius;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getRegDate() {
        return regDate;
    }

    public void setRegDate(String regDate) {
        this.regDate = regDate;
    }
}