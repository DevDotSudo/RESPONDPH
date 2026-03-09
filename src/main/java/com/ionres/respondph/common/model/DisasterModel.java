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
    private String locationType;   // "CIRCLE", "POLYGON", or "BANATE"
    private String polyLatLong;    // "lat,lon;lat,lon;..." — only set when POLYGON

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

    public DisasterModel(int id, String type, String name, String locationType, String polyLatLong) {
        this.disasterId = id;
        this.disasterType = type;
        this.disasterName = name;
        this.isBanateArea = "BANATE".equals(locationType);
        this.locationType = locationType;
        this.polyLatLong = polyLatLong;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

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

    /**
     * Returns the location type string: "CIRCLE", "POLYGON", or "BANATE".
     * Falls back to "BANATE" when {@link #isBanateArea()} is true and no
     * explicit type was set, so callers can safely rely on this value alone.
     */
    public String getLocationType() {
        if (locationType != null && !locationType.isEmpty()) {
            return locationType;
        }
        return isBanateArea ? "BANATE" : "CIRCLE";
    }

    /**
     * Returns the serialized polygon points ("lat,lon;lat,lon;...").
     * Non-null only when {@link #getLocationType()} is "POLYGON".
     */
    public String getPolyLatLong() {
        return polyLatLong;
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setIsBanateArea(boolean isBanateArea) {
        this.isBanateArea = isBanateArea;
        if (isBanateArea && !"BANATE".equals(locationType)) {
            this.locationType = "BANATE";
        }
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
        this.isBanateArea = "BANATE".equals(locationType);
    }

    public void setPolyLatLong(String polyLatLong) {
        this.polyLatLong = polyLatLong;
    }

    @Override
    public String toString() {
        return disasterName;
    }
}