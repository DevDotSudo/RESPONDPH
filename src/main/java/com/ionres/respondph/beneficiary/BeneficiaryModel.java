package com.ionres.respondph.beneficiary;

public class BeneficiaryModel {
    private int id;
    private String firstname;
    private String middlename;
    private String lastname;
    private String birthDate;
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
    private String regDate;

    public BeneficiaryModel(String firstname, String middlename, String lastname, String birthDate, String gender, String maritalStatus, String soloParentStatus, String latitude,
                            String longitude,String mobileNumber, String disabilityType, String healthCondition, String cleanWaterAccess, String sanitationFacility, String houseType,
                             String ownerShipStatus, String employmentStatus, String monthlyIncome, String educationalLevel, String digitalAccess, String regDate){
        this.firstname = firstname;
        this.middlename = middlename;
        this.lastname = lastname;
        this.birthDate = birthDate;
        this.gender = gender;
        this.maritalStatus = maritalStatus;
        this.soloParentStatus = soloParentStatus;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mobileNumber =mobileNumber;
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
        this.regDate = regDate;


    }
    public BeneficiaryModel(){

    }

    public int getId(){
        return id;
    }
    public void setId(int newId){
        this.id = newId;
    }

    public String getFirstname(){
        return firstname;
    }
    public void setFirstname(String newFirstname){
        this.firstname = newFirstname;
    }
    public String getMiddlename(){
        return middlename;
    }
    public void setMiddlename(String newMiddlename){
        this.middlename = newMiddlename;
    }
    public String getLastname(){
        return lastname;
    }
    public void setLastname(String newLastname){
        this.lastname = newLastname;
    }
    public String getBirthDate(){
        return  birthDate;
    }
    public void  setBirthDate(String newBirthDate){
        this.birthDate = newBirthDate;
    }
    public String getGender(){
        return gender;
    }
    public void setGender(String newGender){
        this.gender = newGender;
    }
    public String getMaritalStatus(){
        return maritalStatus;
    }
    public void setMaritalStatus(String newMaritalStatus){
        this.maritalStatus = newMaritalStatus;
    }
    public String getSoloParentStatus(){
        return soloParentStatus;
    }
    public void setSoloParentStatus(String newSoloParentStatus){
        this.soloParentStatus = newSoloParentStatus;
    }
    public String getLatitude(){
        return  latitude;
    }
    public void setLatitude(String newLatitude){
        this.latitude = newLatitude;
    }
    public String getLongitude(){
        return  longitude;
    }
    public void setLongitude(String newLongitude){
        this.longitude = newLongitude;
    }
    public String getMobileNumber(){
        return mobileNumber;
    }
    public void setMobileNumber(String newMobileNumber){
        this.mobileNumber = newMobileNumber;
    }
    public String getDisabilityType(){
        return  disabilityType;
    }
    public void setDisabilityType(String newDisabilityType){
        this.disabilityType = newDisabilityType;
    }
    public String getHealthCondition(){
        return  healthCondition;
    }
    public void setHealthCondition(String newHealthCondition){
        this.healthCondition = newHealthCondition;
    }
    public String getCleanWaterAccess(){
        return cleanWaterAccess;
    }
    public void setCleanWaterAccess(String newCleanWaterAccess){
        this.cleanWaterAccess = newCleanWaterAccess;
    }
    public String getSanitationFacility() {
        return sanitationFacility;
    }
    public void setSanitationFacility(String newSanitationFacility){
        this.sanitationFacility = newSanitationFacility;
    }
    public String getHouseType(){
        return  houseType;
    }
    public void setHouseType(String newHouseType){
        this.houseType = newHouseType;
    }
    public String getOwnerShipStatus(){
        return ownerShipStatus;
    }
    public void setOwnerShipStatus(String newOwnerShipStatus){
        this.ownerShipStatus = newOwnerShipStatus;
    }
    public String getEmploymentStatus(){
        return employmentStatus;
    }
    public void setEmploymentStatus(String newEmploymentStatus){
        this.employmentStatus = newEmploymentStatus;
    }
    public String getMonthlyIncome(){
        return monthlyIncome;
    }
    public void setMonthlyIncome(String newMonthlyIncome){
        this.monthlyIncome = newMonthlyIncome;
    }
    public String getEducationalLevel(){
        return  educationalLevel;
    }
    public void setEducationalLevel(String newEducationalLevel){
        this.educationalLevel = newEducationalLevel;
    }
    public String getDigitalAccess(){
        return digitalAccess;
    }
    public void setDigitalAccess(String newDigitalAccess){
        this.digitalAccess = newDigitalAccess;
    }
    public String getRegDate(){
        return regDate;
    }
    public void setRegDate(String newRegDate){
        this.regDate = newRegDate;
    }




    
}
