package com.ionres.respondph.sendsms;

public class DisasterModel {
    private int disasterId;
    private String type;
    private String name;
    private String date;
    private String locationType;   // "CIRCLE", "POLYGON", or "BANATE"
    private String polyLatLong;    // "lat,lon;lat,lon;..." — null if not polygon

    public DisasterModel() {}

    public DisasterModel(int disasterId, String type, String name, String date) {
        this.disasterId = disasterId;
        this.type = type;
        this.name = name;
        this.date = date;
    }

    public DisasterModel(int disasterId, String type, String name, String date,
                         String locationType, String polyLatLong) {
        this.disasterId = disasterId;
        this.type = type;
        this.name = name;
        this.date = date;
        this.locationType = locationType;
        this.polyLatLong = polyLatLong;
    }

    public int getDisasterId() { return disasterId; }
    public void setDisasterId(int disasterId) { this.disasterId = disasterId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }

    public String getPolyLatLong() { return polyLatLong; }
    public void setPolyLatLong(String polyLatLong) { this.polyLatLong = polyLatLong; }

    /** Convenience — true when this disaster uses a drawn polygon area. */
    public boolean isPolygon() {
        return "POLYGON".equalsIgnoreCase(locationType);
    }

    @Override
    public String toString() {
        return type + " - " + name + " (" + date + ")";
    }
}