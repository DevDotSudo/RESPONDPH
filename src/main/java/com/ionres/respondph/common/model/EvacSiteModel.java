package com.ionres.respondph.common.model;

public class EvacSiteModel {
    private int evacId;
    private String lat;
    private String lon;
    private String name;
    private String capacity;
    private String notes;

    public EvacSiteModel() {
    }

    public EvacSiteModel(int evacId, String lat, String lon, String name, String capacity, String notes) {
        this.evacId = evacId;
        this.lat = lat;
        this.lon = lon;
        this.name = name;
        this.capacity = capacity;
        this.notes = notes;
    }

    public int getEvacId() {
        return evacId;
    }

    public void setEvacId(int evacId) {
        this.evacId = evacId;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLon() {
        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCapacity() {
        return capacity;
    }

    public void setCapacity(String capacity) {
        this.capacity = capacity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
