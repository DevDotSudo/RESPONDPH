package com.ionres.respondph.aid_type;

public class AidTypeModel {
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
    private String addedBy;
    private String regDate;

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
            String addedBy,
            String regDate
    ){
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
        this.addedBy = addedBy;
        this.regDate = regDate;
    }

    public void setAidTypeName(String newAidTypeName){
        this.aidTypeName = newAidTypeName;
    }

    public String getAidTypeName(){
        return aidTypeName;
    }


}
