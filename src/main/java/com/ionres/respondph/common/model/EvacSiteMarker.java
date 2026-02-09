package com.ionres.respondph.common.model;

public class EvacSiteMarker {
    private int evacId;
    private double lat;
    private double lon;
    private String name;
    private int capacity;
    private String notes;

    public EvacSiteMarker() {
    }

    public EvacSiteMarker(int evacId, double lat, double lon, String name, int capacity, String notes) {
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

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}