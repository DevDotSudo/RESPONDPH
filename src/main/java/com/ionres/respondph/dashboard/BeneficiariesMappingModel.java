package com.ionres.respondph.dashboard;

public class BeneficiariesMappingModel {
    int id;
    String beneficiaryName;
    String lat;
    String lng;

    public BeneficiariesMappingModel(int id, String beneficiaryName, String lat, String lng) {
        this.id = id;
        this.beneficiaryName = beneficiaryName;
        this.lat = lat;
        this.lng = lng;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public String getLat() {
        return lat;
    }

    public String getLng() {
        return lng;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }
}
