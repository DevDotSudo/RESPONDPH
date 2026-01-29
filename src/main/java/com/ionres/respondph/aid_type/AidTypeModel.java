package com.ionres.respondph.aid_type;

public class AidTypeModel {
    private int aidTypeId;
    private String aidTypeName;
    private double ageWeight;
    private double genderWeight;
    private double maritalStatusWeight;
    private double soloParentWeight;
    private double disabilityWeight;
    private double healthConditionWeight;
    private double accessToCleanWaterWeight;
    private double sanitationFacilityWeight;
    private double houseConstructionTypeWeight;
    private double ownershipWeight;
    private double damageSeverityWeight;
    private double employmentStatusWeight;
    private double monthlyIncomeWeight;
    private double educationalLevelWeight;
    private double digitalAccessWeight;
    private double dependencyRatioWeight;
    private String notes;
    private int adminId;
    private String regDate;
    private String adminName;

    public AidTypeModel(
            String aidTypeName,
            double ageWeight,
            double genderWeight,
            double maritalStatusWeight,
            double soloParentWeight,
            double disabilityWeight,
            double healthConditionWeight,
            double accessToCleanWaterWeight,
            double sanitationFacilityWeight,
            double houseConstructionTypeWeight,
            double ownershipWeight,
            double damageSeverityWeight,
            double employmentStatusWeight,
            double monthlyIncomeWeight,
            double educationalLevelWeight,
            double digitalAccessWeight,
            double dependencyRatioWeight,
            String notes,
            int adminId,
            String regDate
    ) {
        this.aidTypeName = aidTypeName;
        this.ageWeight = ageWeight;
        this.genderWeight = genderWeight;
        this.maritalStatusWeight = maritalStatusWeight;
        this.soloParentWeight = soloParentWeight;
        this.disabilityWeight = disabilityWeight;
        this.healthConditionWeight = healthConditionWeight;
        this.accessToCleanWaterWeight = accessToCleanWaterWeight;
        this.sanitationFacilityWeight = sanitationFacilityWeight;
        this.houseConstructionTypeWeight = houseConstructionTypeWeight;
        this.ownershipWeight = ownershipWeight;
        this.damageSeverityWeight = damageSeverityWeight;
        this.employmentStatusWeight = employmentStatusWeight;
        this.monthlyIncomeWeight = monthlyIncomeWeight;
        this.educationalLevelWeight = educationalLevelWeight;
        this.digitalAccessWeight = digitalAccessWeight;
        this.dependencyRatioWeight = dependencyRatioWeight;
        this.notes = notes;
        this.adminId = adminId;
        this.regDate = regDate;
    }

    public AidTypeModel(){

    }

    public int getAidTypeId() { return aidTypeId; }
    public void setAidTypeId(int aidTypeId) { this.aidTypeId = aidTypeId; }

    public String getAidTypeName() {
        return aidTypeName;
    }

    public void setAidTypeName(String aidTypeName) {
        this.aidTypeName = aidTypeName;
    }

    public double getAgeWeight() {
        return ageWeight;
    }

    public void setAgeWeight(double ageWeight) {
        this.ageWeight = ageWeight;
    }

    public double getGenderWeight() {
        return genderWeight;
    }

    public void setGenderWeight(double genderWeight) {
        this.genderWeight = genderWeight;
    }

    public double getMaritalStatusWeight() {
        return maritalStatusWeight;
    }

    public void setMaritalStatusWeight(double maritalStatusWeight) {
        this.maritalStatusWeight = maritalStatusWeight;
    }

    public double getSoloParentWeight() {
        return soloParentWeight;
    }

    public void setSoloParentWeight(double soloParentWeight) {
        this.soloParentWeight = soloParentWeight;
    }

    public double getDisabilityWeight() {
        return disabilityWeight;
    }

    public void setDisabilityWeight(double disabilityWeight) {
        this.disabilityWeight = disabilityWeight;
    }

    public double getHealthConditionWeight() {
        return healthConditionWeight;
    }

    public void setHealthConditionWeight(double healthConditionWeight) {
        this.healthConditionWeight = healthConditionWeight;
    }

    public double getAccessToCleanWaterWeight() {
        return accessToCleanWaterWeight;
    }

    public void setAccessToCleanWaterWeight(double accessToCleanWaterWeight) {
        this.accessToCleanWaterWeight = accessToCleanWaterWeight;
    }

    public double getSanitationFacilityWeight() {
        return sanitationFacilityWeight;
    }

    public void setSanitationFacilityWeight(double sanitationFacilityWeight) {
        this.sanitationFacilityWeight = sanitationFacilityWeight;
    }

    public double getHouseConstructionTypeWeight() {
        return houseConstructionTypeWeight;
    }

    public void setHouseConstructionTypeWeight(double houseConstructionTypeWeight) {
        this.houseConstructionTypeWeight = houseConstructionTypeWeight;
    }

    public double getOwnershipWeight() {
        return ownershipWeight;
    }

    public void setOwnershipWeight(double ownershipWeight) {
        this.ownershipWeight = ownershipWeight;
    }

    public double getDamageSeverityWeight() {
        return damageSeverityWeight;
    }

    public void setDamageSeverityWeight(double damageSeverityWeight) {
        this.damageSeverityWeight = damageSeverityWeight;
    }

    public double getEmploymentStatusWeight() {
        return employmentStatusWeight;
    }

    public void setEmploymentStatusWeight(double employmentStatusWeight) {
        this.employmentStatusWeight = employmentStatusWeight;
    }

    public double getMonthlyIncomeWeight() {
        return monthlyIncomeWeight;
    }

    public void setMonthlyIncomeWeight(double monthlyIncomeWeight) {
        this.monthlyIncomeWeight = monthlyIncomeWeight;
    }

    public double getEducationalLevelWeight() {
        return educationalLevelWeight;
    }

    public void setEducationalLevelWeight(double educationalLevelWeight) {
        this.educationalLevelWeight = educationalLevelWeight;
    }

    public double getDigitalAccessWeight() {
        return digitalAccessWeight;
    }

    public void setDigitalAccessWeight(double digitalAccessWeight) {
        this.digitalAccessWeight = digitalAccessWeight;
    }

    public double getDependencyRatioWeight() {
        return dependencyRatioWeight;
    }

    public void setDependencyRatioWeight(double dependencyRatioWeight) {
        this.dependencyRatioWeight = dependencyRatioWeight;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public String getRegDate() {
        return regDate;
    }

    public void setRegDate(String regDate) {
        this.regDate = regDate;
    }
}