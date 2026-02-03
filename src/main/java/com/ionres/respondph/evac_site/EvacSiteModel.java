package com.ionres.respondph.evac_site;

public class EvacSiteModel {

    private int evacId;
    private String name;
    private String capacity;
    private String lat;
    private String longi;
    private String notes;

    public EvacSiteModel() {
    }

    public EvacSiteModel(String name, String capacity, String lat, String longi, String notes) {
        this.name = name;
        this.capacity = capacity;
        this.lat = lat;
        this.longi = longi;
        this.notes = notes;
    }

    public int getEvacId() {
        return evacId;
    }

    public void setEvacId(int evacId) {
        this.evacId = evacId;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}