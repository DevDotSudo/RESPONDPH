package com.ionres.respondph.mapping;

public class BeneficiariesMappingModel {
    String beneficiaryName;
    double lat;
    double lng;

    public BeneficiariesMappingModel(String beneficiaryName, double lat, double lng) {
        this.beneficiaryName = beneficiaryName;
        this.lat = lat;
        this.lng = lng;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}
