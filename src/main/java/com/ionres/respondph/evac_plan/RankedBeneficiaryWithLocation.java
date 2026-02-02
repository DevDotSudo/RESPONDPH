package com.ionres.respondph.evac_plan;


public class RankedBeneficiaryWithLocation extends RankedBeneficiaryModel {

    private double latitude;
    private double longitude;
    private int assignedEvacSiteId;
    private String assignedEvacSiteName;
    private double distanceToEvacSite; // in kilometers

    public RankedBeneficiaryWithLocation() {
        super();
    }

    public RankedBeneficiaryWithLocation(int beneficiaryId, String firstName, String lastName,
                                         double finalScore, String scoreCategory, int householdMembers,
                                         double latitude, double longitude) {
        super(beneficiaryId, firstName, lastName, finalScore, scoreCategory, householdMembers);
        this.latitude = latitude;
        this.longitude = longitude;
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

    public int getAssignedEvacSiteId() {
        return assignedEvacSiteId;
    }

    public void setAssignedEvacSiteId(int assignedEvacSiteId) {
        this.assignedEvacSiteId = assignedEvacSiteId;
    }

    public String getAssignedEvacSiteName() {
        return assignedEvacSiteName;
    }

    public void setAssignedEvacSiteName(String assignedEvacSiteName) {
        this.assignedEvacSiteName = assignedEvacSiteName;
    }

    public double getDistanceToEvacSite() {
        return distanceToEvacSite;
    }

    public void setDistanceToEvacSite(double distanceToEvacSite) {
        this.distanceToEvacSite = distanceToEvacSite;
    }

    @Override
    public String toString() {
        if (assignedEvacSiteName != null) {
            return String.format("Beneficiary #%d [%s %s] | Score: %.2f | Household: %d | Assigned to: %s (%.2f km)",
                    getBeneficiaryId(), getFirstName(), getLastName(),
                    getFinalScore(), getHouseholdMembers(),
                    assignedEvacSiteName, distanceToEvacSite);
        } else {
            return super.toString();
        }
    }
}