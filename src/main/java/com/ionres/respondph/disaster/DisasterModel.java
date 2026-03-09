package com.ionres.respondph.disaster;

public class DisasterModel {

    private int disasterId;
    private String disasterType;
    private String disasterName;
    private String date;
    private String lat;
    private String longi;
    private String radius;
    private String notes;
    private String regDate;
    private boolean isBanateArea;

    // ── NEW ──────────────────────────────────────────────
    /** "CIRCLE", "POLYGON", or "BANATE" */
    private String locationType;
    /** Serialized polygon points: "lat,lon;lat,lon;..." — null when not polygon */
    private String polyLatLong;
    // ─────────────────────────────────────────────────────

    // ==================== CONSTRUCTORS ====================

    public DisasterModel() {}

    /** Circle / Banate constructor (existing) */
    public DisasterModel(String disasterType, String disasterName, String date,
                         String lat, String longi, String radius,
                         String notes, String regDate, boolean isBanateArea) {
        this.disasterType = disasterType;
        this.disasterName = disasterName;
        this.date = date;
        this.lat = lat;
        this.longi = longi;
        this.radius = radius;
        this.notes = notes;
        this.regDate = regDate;
        this.isBanateArea = isBanateArea;
        this.locationType = isBanateArea ? "BANATE" : "CIRCLE";
        this.polyLatLong = null;
    }

    /** Update constructor (no isBanateArea — existing) */
    public DisasterModel(String disasterType, String disasterName, String date,
                         String lat, String longi, String radius,
                         String notes, String regDate) {
        this.disasterType = disasterType;
        this.disasterName = disasterName;
        this.date = date;
        this.lat = lat;
        this.longi = longi;
        this.radius = radius;
        this.notes = notes;
        this.regDate = regDate;
    }

    /** NEW — Polygon constructor */
    public DisasterModel(String disasterType, String disasterName, String date,
                         String polyLatLong,
                         String notes, String regDate) {
        this.disasterType = disasterType;
        this.disasterName = disasterName;
        this.date = date;
        this.polyLatLong = polyLatLong;
        this.notes = notes;
        this.regDate = regDate;
        this.isBanateArea = false;
        this.locationType = "POLYGON";
        this.lat = null;
        this.longi = null;
        this.radius = null;
    }

    public int getDisasterId() { return disasterId; }
    public void setDisasterId(int disasterId) { this.disasterId = disasterId; }

    public String getDisasterType() { return disasterType; }
    public void setDisasterType(String disasterType) { this.disasterType = disasterType; }

    public String getDisasterName() { return disasterName; }
    public void setDisasterName(String disasterName) { this.disasterName = disasterName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getLat() { return lat; }
    public void setLat(String lat) { this.lat = lat; }

    public String getLongi() { return longi; }
    public void setLongi(String longi) { this.longi = longi; }

    public String getRadius() { return radius; }
    public void setRadius(String radius) { this.radius = radius; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRegDate() { return regDate; }
    public void setRegDate(String regDate) { this.regDate = regDate; }

    public boolean isBanateArea() { return isBanateArea; }
    public void setIsBanateArea(boolean isBanateArea) { this.isBanateArea = isBanateArea; }

    // ── NEW ──────────────────────────────────────────────
    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }

    public String getPolyLatLong() { return polyLatLong; }
    public void setPolyLatLong(String polyLatLong) { this.polyLatLong = polyLatLong; }
    // ─────────────────────────────────────────────────────
}