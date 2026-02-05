package com.ionres.respondph.evacuation_plan;

public class EvacuationPlanModel {
    private int planId;
    private int beneficiaryId;
    private String beneficiaryName;
    private int evacSiteId;
    private String evacSiteName;
    private int disasterId;
    private String disasterName;
    private String dateCreated;
    private String notes;

    public EvacuationPlanModel() {}

    public EvacuationPlanModel(int planId, int beneficiaryId, String beneficiaryName,
                               int evacSiteId, String evacSiteName,
                               int disasterId, String disasterName,
                               String dateCreated) {
        this.planId = planId;
        this.beneficiaryId = beneficiaryId;
        this.beneficiaryName = beneficiaryName;
        this.evacSiteId = evacSiteId;
        this.evacSiteName = evacSiteName;
        this.disasterId = disasterId;
        this.disasterName = disasterName;
        this.dateCreated = dateCreated;
    }

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }

    public int getBeneficiaryId() { return beneficiaryId; }
    public void setBeneficiaryId(int beneficiaryId) { this.beneficiaryId = beneficiaryId; }

    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }

    public int getEvacSiteId() { return evacSiteId; }
    public void setEvacSiteId(int evacSiteId) { this.evacSiteId = evacSiteId; }

    public String getEvacSiteName() { return evacSiteName; }
    public void setEvacSiteName(String evacSiteName) { this.evacSiteName = evacSiteName; }

    public int getDisasterId() { return disasterId; }
    public void setDisasterId(int disasterId) { this.disasterId = disasterId; }

    public String getDisasterName() { return disasterName; }
    public void setDisasterName(String disasterName) { this.disasterName = disasterName; }

    public String getDateCreated() { return dateCreated; }
    public void setDateCreated(String dateCreated) { this.dateCreated = dateCreated; }


    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}