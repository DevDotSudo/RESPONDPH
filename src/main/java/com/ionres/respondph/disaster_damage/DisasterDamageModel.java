package com.ionres.respondph.disaster_damage;

public class DisasterDamageModel {

    private int beneficiaryDisasterDamageId;
    private int beneficiaryId;
    private String beneficiaryFirstname;
    private int disasterId;
    private String disasterType;
    private String disasterName;
    private String houseDamageSeverity;
    private String assessmentDate;
    private String verifiedBy;
    private String notes;
    private String regDate;

    public DisasterDamageModel() {
    }

    public DisasterDamageModel(

            int beneficiaryId,
            int disasterId,
            String houseDamageSeverity,
            String assessmentDate,
            String verifiedBy,
            String notes,
            String regDate
    ) {
        this.beneficiaryId = beneficiaryId;
        this.disasterId = disasterId;
        this.houseDamageSeverity = houseDamageSeverity;
        this.assessmentDate = assessmentDate;
        this.verifiedBy = verifiedBy;
        this.notes = notes;
        this.regDate = regDate;
    }

    public int getBeneficiaryDisasterDamageId() {
        return beneficiaryDisasterDamageId;
    }

    public void setBeneficiaryDisasterDamageId(int beneficiaryDisasterDamageId) {
        this.beneficiaryDisasterDamageId = beneficiaryDisasterDamageId;
    }


    public int getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(int beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public String getBeneficiaryFirstname(){
        return beneficiaryFirstname;
    }

    public void setBeneficiaryFirstname(String beneficiaryFirstname){
        this.beneficiaryFirstname = beneficiaryFirstname;
    }

    public int getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(int disasterId) {
        this.disasterId = disasterId;
    }

    public String getDisasterType(){
        return  disasterType;
    }

    public void setDisasterType(String disasterType){
        this.disasterType = disasterType;
    }

    public String getDisasterName(){
        return  disasterName;
    }

    public void setDisasterName(String disasterName){
        this.disasterName = disasterName;
    }

    public String getHouseDamageSeverity() {
        return houseDamageSeverity;
    }

    public void setHouseDamageSeverity(String houseDamageSeverity) {
        this.houseDamageSeverity = houseDamageSeverity;
    }

    public String getAssessmentDate() {
        return assessmentDate;
    }

    public void setAssessmentDate(String assessmentDate) {
        this.assessmentDate = assessmentDate;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getRegDate(){
        return  regDate;
    }
    public void setRegDate(String regDate){
        this.regDate = regDate;
    }
}