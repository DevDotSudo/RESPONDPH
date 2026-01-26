package com.ionres.respondph.aid;

import java.time.LocalDate;


public class AidModel {
    private int aidId;
    private int beneficiaryId;
    private int disasterId;
    private String name;
    private LocalDate date;
    private double quantity;
    private double cost;
    private String provider;
    private int aidTypeId;
    private String notes;


    private String beneficiaryName;
    private String disasterName;

    public AidModel() {
    }

    public AidModel(int beneficiaryId, int disasterId, String name, LocalDate date,
                    double quantity, double cost, String provider, int aidTypeId) {
        this.beneficiaryId = beneficiaryId;
        this.disasterId = disasterId;
        this.name = name;
        this.date = date;
        this.quantity = quantity;
        this.cost = cost;
        this.provider = provider;
        this.aidTypeId = aidTypeId;
    }

    public int getAidId() {
        return aidId;
    }

    public void setAidId(int aidId) {
        this.aidId = aidId;
    }

    public int getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(int beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public int getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(int disasterId) {
        this.disasterId = disasterId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getAidTypeId() {
        return aidTypeId;
    }

    public void setAidTypeId(int aidTypeId) {
        this.aidTypeId = aidTypeId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getDisasterName() {
        return disasterName;
    }

    public void setDisasterName(String disasterName) {
        this.disasterName = disasterName;
    }

    @Override
    public String toString() {
        return "AidModel{" +
                "aidId=" + aidId +
                ", beneficiaryId=" + beneficiaryId +
                ", disasterId=" + disasterId +
                ", name='" + name + '\'' +
                ", date=" + date +
                ", quantity=" + quantity +
                ", cost=" + cost +
                ", provider='" + provider + '\'' +
                ", aidTypeId=" + aidTypeId +
                ", notes='" + notes + '\'' +
                '}';
    }
}