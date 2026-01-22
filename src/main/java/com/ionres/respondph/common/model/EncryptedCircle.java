package com.ionres.respondph.common.model;

public class EncryptedCircle {
    public final String lat;
    public final String lon;
    public final String radius;

    public EncryptedCircle(String lat, String lon, String radius) {
        this.lat = lat;
        this.lon = lon;
        this.radius = radius;
    }
}