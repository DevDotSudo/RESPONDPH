package com.ionres.respondph.household;

public class HouseholdModel {
    private int householdScoreId;
    private int beneficiaryId;
    private double ageScore;
    private double maritalStatusScore;
    private double soloParentScore;
    private double disabilityScore;
    private double healthConditionScore;
    private double accessToCleanWaterScore;
    private double sanitationScore;
    private double houseConstructionScore;
    private double ownershipScore;
    private double damageSeverityScore;
    private double employmentStatusScore;
    private double monthlyIncomeScore;
    private double educationLevelScore;
    private double digitalAccessScore;
    private String creationDate;
    private String updatingDate;
    private String notes;

    public HouseholdModel(int householdScoreId, int beneficiaryId, double ageScore, double maritalStatusScore,
                          double soloParentScore, double disabilityScore, double healthConditionScore, double accessToCleanWaterScore,
                          double sanitationScore, double houseConstructionScore, double ownershipScore, double damageSeverityScore,
                          double employmentStatusScore, double monthlyIncomeScore, double educationLevelScore, double digitalAccessScore,
                          String creationDate, String updatingDate, String notes){
        this.householdScoreId = householdScoreId;
        this.beneficiaryId = beneficiaryId;
        this.ageScore = ageScore;
        this.maritalStatusScore = maritalStatusScore;
        this.soloParentScore = soloParentScore;
        this.disabilityScore = disabilityScore;
        this.healthConditionScore = healthConditionScore;
        this.accessToCleanWaterScore = accessToCleanWaterScore;
        this.sanitationScore = sanitationScore;
        this.houseConstructionScore = houseConstructionScore;
        this.ownershipScore = ownershipScore;
        this.damageSeverityScore = damageSeverityScore;
        this.employmentStatusScore = employmentStatusScore;
        this.monthlyIncomeScore = monthlyIncomeScore;
        this.educationLevelScore = educationLevelScore;
        this.digitalAccessScore = digitalAccessScore;
        this.creationDate = creationDate;
        this.updatingDate = updatingDate;
        this.notes = notes;
    }

    public int getHouseholdScoreId(){
        return householdScoreId;
    }

    public void setHouseholdScoreId(int newHouseholdScoreId){
        this.householdScoreId = newHouseholdScoreId;
    }
    public  int getBeneficiaryId(){
        return  beneficiaryId;
    }
    public void setBeneficiaryId(int newBeneficiaryId){
        this.beneficiaryId = newBeneficiaryId;

    }
    public double getAgeScore(){
        return  ageScore;
    }
    public void setAgeScore(double newAgeScore){
        this.ageScore = newAgeScore;
    }
    public double getMaritalStatusScore(){
        return  maritalStatusScore;
    }
    public  void setMaritalStatusScore(double newMaritalStatusScore){
        this.maritalStatusScore = newMaritalStatusScore;
    }
    public  double getSoloParentScore(){
        return  soloParentScore;
    }
    public  void  setSoloParentScore(double newSoloParentScore){
        this.soloParentScore = newSoloParentScore;
    }
    public double getDisabilityScore(){
        return disabilityScore;
    }
    public void setDisabilityScore(double newDisabilityScore){
        this.disabilityScore = newDisabilityScore;
    }
    public double getHealthConditionScore(){
        return  healthConditionScore;
    }
    public void setHealthConditionScore(double newHealthConditionScore){
        this.healthConditionScore = newHealthConditionScore;
    }
    public double getAccessToCleanWaterScore(){
        return accessToCleanWaterScore;
    }
    public void setAccessToCleanWaterScore(double newAccessToCleanWaterScore){
        this.accessToCleanWaterScore = newAccessToCleanWaterScore;
    }
    public double getSanitationScore(){
        return sanitationScore;
    }
    public  void  setSanitationScore(double newSanitationScore){
        this.sanitationScore = newSanitationScore;
    }
    public double getHouseConstructionScore(){
        return  houseConstructionScore;
    }
    public void setHouseConstructionScore(double newHouseConstructionScore){
        this.houseConstructionScore = newHouseConstructionScore;
    }
    public  double getOwnershipScore(){
        return ownershipScore;
    }
    public void  setOwnershipScore(double newOwnershipScore){
        this.ownershipScore = newOwnershipScore;
    }
    public double getDamageSeverityScore(){
        return  damageSeverityScore;
    }
    public void setDamageSeverityScore(double newDamageSeverityScore){
        this.damageSeverityScore = newDamageSeverityScore;
    }
    public double getEmploymentStatusScore(){
        return  employmentStatusScore;
    }
    public void setEmploymentStatusScore(double newEmploymentStatusScore){
        this.employmentStatusScore = newEmploymentStatusScore;
    }
    public double getMonthlyIncomeScore(){
        return monthlyIncomeScore;
    }

    public void setMonthlyIncomeScore(double newMonthlyIncomeScore) {
        this.monthlyIncomeScore = newMonthlyIncomeScore;
    }

    public double getEducationLevelScore(){
        return  educationLevelScore;
    }
    public void setEducationLevelScore(double newEducationLevelScore){
        this.educationLevelScore = newEducationLevelScore;
    }

    public double getDigitalAccessScore() {
        return digitalAccessScore;
    }

    public void setDigitalAccessScore(double newDigitalAccessScore) {
        this.digitalAccessScore = newDigitalAccessScore;
    }

    public String getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(String newCreationDate){
        this.creationDate = newCreationDate;
    }
    public String getUpdatingDate(){
        return  updatingDate;
    }
    public void setUpdatingDate(String newUpdatingDate){
        this.updatingDate = newUpdatingDate;
    }
    public String getNotes(){
        return notes;
    }
    public void setNotes(String newNotes){
        this.notes = newNotes;
    }

}
