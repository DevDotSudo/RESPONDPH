package com.ionres.respondph.evac_plan;

/**
 * Represents an evacuation site with its distance from a beneficiary.
 * Used for finding the nearest evacuation site to assign beneficiaries.
 */
public class EvacSiteWithDistance implements Comparable<EvacSiteWithDistance> {

    private int evacSiteId;
    private String evacSiteName;
    private double latitude;
    private double longitude;
    private int remainingCapacity;
    private double distanceInKm; // Distance from beneficiary to this evac site

    public EvacSiteWithDistance() {
    }

    public EvacSiteWithDistance(int evacSiteId, String evacSiteName, double latitude,
                                double longitude, int remainingCapacity, double distanceInKm) {
        this.evacSiteId = evacSiteId;
        this.evacSiteName = evacSiteName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.remainingCapacity = remainingCapacity;
        this.distanceInKm = distanceInKm;
    }

    public int getEvacSiteId() {
        return evacSiteId;
    }

    public void setEvacSiteId(int evacSiteId) {
        this.evacSiteId = evacSiteId;
    }

    public String getEvacSiteName() {
        return evacSiteName;
    }

    public void setEvacSiteName(String evacSiteName) {
        this.evacSiteName = evacSiteName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(int remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    public double getDistanceInKm() {
        return distanceInKm;
    }

    public void setDistanceInKm(double distanceInKm) {
        this.distanceInKm = distanceInKm;
    }

    @Override
    public int compareTo(EvacSiteWithDistance other) {
        // Sort by distance (nearest first)
        return Double.compare(this.distanceInKm, other.distanceInKm);
    }

    @Override
    public String toString() {
        return String.format("EvacSite #%d [%s] | Distance: %.2f km | Capacity: %d",
                evacSiteId, evacSiteName, distanceInKm, remainingCapacity);
    }
}