package com.ionres.respondph.common.model;

public class DisasterCircleEncrypted {
    public final String lat;
    public final String lon;
    public final String radius;
    public final int disasterId;
    public final String disasterName;
    public final String disasterType;

    public DisasterCircleEncrypted(String lat, String lon, String radius,
                                   int disasterId, String disasterName, String disasterType) {
        this.lat = lat;
        this.lon = lon;
        this.radius = radius;
        this.disasterId = disasterId;
        this.disasterName = disasterName;
        this.disasterType = disasterType;
    }
}