package com.ionres.respondph.common.model;

public class BeneficiaryMarker {
    public final int id;
    public final String name;
    public final double lat;
    public final double lon;

    public BeneficiaryMarker(int id, String name, double lat, double lon) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
    }
}