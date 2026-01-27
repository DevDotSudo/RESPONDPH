package com.ionres.respondph.common.model;

public class BeneficiaryEncrypted {
    public final int id;
    public final String encryptedFullName;
    public final String lat;
    public final String lng;

    public BeneficiaryEncrypted(int id, String encryptedFullName, String lat, String lng) {
        this.id = id;
        this.encryptedFullName = encryptedFullName;
        this.lat = lat;
        this.lng = lng;
    }
}
