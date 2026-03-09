package com.ionres.respondph.common.model;

/**
 * Simplified DisasterModel for display purposes (e.g., dropdowns, lists).
 * For full disaster data operations, use com.ionres.respondph.disaster.DisasterModel
 */
public class DisasterModel {
    private int disasterId;
    private String disasterType;
    private String disasterName;
    private boolean isBanateArea;

    public DisasterModel(int id, String type, String name) {
        this.disasterId = id;
        this.disasterType = type;
        this.disasterName = name;
        this.isBanateArea = false;
    }

    public DisasterModel(int id, String type, String name, boolean isBanateArea) {
        this.disasterId = id;
        this.disasterType = type;
        this.disasterName = name;
        this.isBanateArea = isBanateArea;
    }

    public int getDisasterId() {
        return disasterId;
    }

    public String getDisasterType() {
        return disasterType;
    }

    public String getDisasterName() {
        return disasterName;
    }

    public boolean isBanateArea() {
        return isBanateArea;
    }

    public void setIsBanateArea(boolean isBanateArea) {
        this.isBanateArea = isBanateArea;
    }

    @Override
    public String toString() {
        return disasterName;
    }
}