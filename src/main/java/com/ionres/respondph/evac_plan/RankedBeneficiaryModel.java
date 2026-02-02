package com.ionres.respondph.evac_plan;


public class RankedBeneficiaryModel {

    private int beneficiaryId;
    private String firstName;
    private String lastName;
    private double finalScore;
    private String scoreCategory;
    private int householdMembers; // total persons (beneficiary + family_member rows)

    public RankedBeneficiaryModel() {
    }

    public RankedBeneficiaryModel(int beneficiaryId, String firstName, String lastName,
                                  double finalScore, String scoreCategory, int householdMembers) {
        this.beneficiaryId = beneficiaryId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.finalScore = finalScore;
        this.scoreCategory = scoreCategory;
        this.householdMembers = householdMembers;
    }

    public int getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(int beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(double finalScore) {
        this.finalScore = finalScore;
    }

    public String getScoreCategory() {
        return scoreCategory;
    }

    public void setScoreCategory(String scoreCategory) {
        this.scoreCategory = scoreCategory;
    }

    public int getHouseholdMembers() {
        return householdMembers;
    }

    public void setHouseholdMembers(int householdMembers) {
        this.householdMembers = householdMembers;
    }

    @Override
    public String toString() {
        return String.format("Beneficiary #%d [%s %s] | Score: %.2f | Category: %s | Household: %d persons",
                beneficiaryId, firstName, lastName, finalScore, scoreCategory, householdMembers);
    }
}