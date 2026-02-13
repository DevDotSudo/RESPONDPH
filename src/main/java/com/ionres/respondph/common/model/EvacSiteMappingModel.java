package com.ionres.respondph.common.model;

public class EvacSiteMappingModel {
    private int evacId;
    private String name;
    private String lat;
    private String lng;
    private String capacity;

    public EvacSiteMappingModel(int evacId, String name, String lat, String lng, String capacity) {
        this.evacId = evacId;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.capacity = capacity;
    }

    public int getEvacId() {
        return evacId;
    }

    public String getName() {
        return name;
    }

    public String getLat() {
        return lat;
    }

    public String getLng() {
        return lng;
    }

    public String getCapacity() {
        return capacity;
    }
}