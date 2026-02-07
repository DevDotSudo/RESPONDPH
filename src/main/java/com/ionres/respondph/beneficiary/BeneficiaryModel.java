package com.ionres.respondph.beneficiary;


public class BeneficiaryModel {
    private int id;
    private String firstname;
    private String middlename;
    private String lastname;
    private String birthDate;
    private String barangay;
    private double ageScore;
    private String gender;
    private String maritalStatus;
    private String soloParentStatus;
    private String latitude;
    private String longitude;
    private String mobileNumber;
    private String disabilityType;
    private String healthCondition;
    private String cleanWaterAccess;
    private String sanitationFacility;
    private String houseType;
    private String ownerShipStatus;
    private String employmentStatus;
    private String monthlyIncome;
    private String educationalLevel;
    private String digitalAccess;
    private String addedBy;
    private String regDate;

    public BeneficiaryModel(int id, String firstname, String middlename, 
                           String lastname, String latitude, String longitude) {
        this.id = id;
        this.firstname = firstname;
        this.middlename = middlename;
        this.lastname = lastname;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public BeneficiaryModel(String firstname, String middlename, String lastname, 
                           String birthDate, String barangay, double ageScore, String gender,
                           String maritalStatus, String soloParentStatus, 
                           String latitude, String longitude, String mobileNumber, 
                           String disabilityType, String healthCondition, 
                           String cleanWaterAccess, String sanitationFacility, 
                           String houseType, String ownerShipStatus, 
                           String employmentStatus, String monthlyIncome, 
                           String educationalLevel, String digitalAccess, 
                           String addedBy, String regDate) {
        this.firstname = firstname;
        this.middlename = middlename;
        this.lastname = lastname;
        this.birthDate = birthDate;
        this.barangay = barangay;
        this.ageScore = ageScore;
        this.gender = gender;
        this.maritalStatus = maritalStatus;
        this.soloParentStatus = soloParentStatus;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mobileNumber = mobileNumber;
        this.disabilityType = disabilityType;
        this.healthCondition = healthCondition;
        this.cleanWaterAccess = cleanWaterAccess;
        this.sanitationFacility = sanitationFacility;
        this.houseType = houseType;
        this.ownerShipStatus = ownerShipStatus;
        this.employmentStatus = employmentStatus;
        this.monthlyIncome = monthlyIncome;
        this.educationalLevel = educationalLevel;
        this.digitalAccess = digitalAccess;
        this.addedBy = addedBy;
        this.regDate = regDate;
    }

    public BeneficiaryModel() {
        // Default constructor
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getMiddlename() {
        return middlename;
    }

    public void setMiddlename(String middlename) {
        this.middlename = middlename;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getBarangay(){return barangay; }

    public void setBarangay(String barangay){this.barangay = barangay;}

    public double getAgeScore(){
        return ageScore;
    }

    public void setAgeScore(double ageScore) {
        this.ageScore = ageScore;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public String getSoloParentStatus() {
        return soloParentStatus;
    }

    public void setSoloParentStatus(String soloParentStatus) {
        this.soloParentStatus = soloParentStatus;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getDisabilityType() {
        return disabilityType;
    }

    public void setDisabilityType(String disabilityType) {
        this.disabilityType = disabilityType;
    }

    public String getHealthCondition() {
        return healthCondition;
    }

    public void setHealthCondition(String healthCondition) {
        this.healthCondition = healthCondition;
    }

    public String getCleanWaterAccess() {
        return cleanWaterAccess;
    }

    public void setCleanWaterAccess(String cleanWaterAccess) {
        this.cleanWaterAccess = cleanWaterAccess;
    }

    public String getSanitationFacility() {
        return sanitationFacility;
    }

    public void setSanitationFacility(String sanitationFacility) {
        this.sanitationFacility = sanitationFacility;
    }

    public String getHouseType() {
        return houseType;
    }

    public void setHouseType(String houseType) {
        this.houseType = houseType;
    }

    public String getOwnerShipStatus() {
        return ownerShipStatus;
    }

    public void setOwnerShipStatus(String ownerShipStatus) {
        this.ownerShipStatus = ownerShipStatus;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public String getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(String monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public String getEducationalLevel() {
        return educationalLevel;
    }

    public void setEducationalLevel(String educationalLevel) {
        this.educationalLevel = educationalLevel;
    }

    public String getDigitalAccess() {
        return digitalAccess;
    }

    public void setDigitalAccess(String digitalAccess) {
        this.digitalAccess = digitalAccess;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public String getRegDate() {
        return regDate;
    }

    public void setRegDate(String regDate) {
        this.regDate = regDate;
    }





}