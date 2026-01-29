package com.ionres.respondph.familymembers;

public class FamilyMembersModel {
    private int familyId;
    private int beneficiaryId;
    private String beneficiaryName;
    private String firstName;
    private String middleName;
    private String lastName;
    private String relationshipToBeneficiary;
    private String birthDate;
    private double ageScore;
    private String gender;
    private String maritalStatus;
    private String disabilityType;
    private String healthCondition;
    private String educationalLevel;
    private String employmentStatus;
    private String notes;
    private String regDate;

    public FamilyMembersModel(
            String firstName,
            String middleName,
            String lastName,
            String relationship,
            String birthDate,
            double ageScore,
            String gender,
            String maritalStatus,
            String disabilityType,
            String healthCondition,
            String employmentStatus,      // ← Position 10
            String educationalLevel,      // ← Position 11
            int beneficiaryId,
            String notes,
            String regDate) {

        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.relationshipToBeneficiary = relationship;
        this.birthDate = birthDate;
        this.ageScore = ageScore;
        this.gender = gender;
        this.maritalStatus = maritalStatus;
        this.disabilityType = disabilityType;
        this.healthCondition = healthCondition;

        // ✅ FIX: Assign in correct order!
        this.employmentStatus = employmentStatus;     // ← Was getting educationalLevel
        this.educationalLevel = educationalLevel;     // ← Was getting employmentStatus

        this.beneficiaryId = beneficiaryId;
        this.notes = notes;
        this.regDate = regDate;
    }
    public FamilyMembersModel(){

    }

    public int getFamilyId() {
        return familyId;
    }

    public int getBeneficiaryId() {
        return beneficiaryId;
    }

    public String getBeneficiaryName(){return  beneficiaryName;}

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getRelationshipToBeneficiary() {
        return relationshipToBeneficiary;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public double getAgeScore() {
        return ageScore;
    }

    public String getGender() {
        return gender;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public String getDisabilityType() {
        return disabilityType;
    }

    public String getHealthCondition() {
        return healthCondition;
    }

    public String getEducationalLevel() {
        return educationalLevel;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public String getNotes() {
        return notes;
    }
    public String getRegDate(){
        return regDate;
    }

    public void setFamilyId(int familyId) {
        this.familyId = familyId;
    }

    public void setBeneficiaryId(int beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public void setBeneficiaryName(String beneficiaryName){this.beneficiaryName = beneficiaryName;}

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setRelationshipToBeneficiary(String relationship) {
        this.relationshipToBeneficiary = relationship;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public void setAgeScore(double ageScore) {
        this.ageScore = ageScore;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public void setDisabilityType(String disabilityType) {
        this.disabilityType = disabilityType;
    }

    public void setHealthCondition(String healthCondition) {
        this.healthCondition = healthCondition;
    }

    public void setEducationalLevel(String educationalLevel) {
        this.educationalLevel = educationalLevel;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
    public void setRegDate(String regDate){
        this.regDate = regDate;
    }
}