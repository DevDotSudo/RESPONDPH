package com.ionres.respondph.familymembers;

public class BeneficiaryModel {

    private int beneficiaryId;
    private String firstName;

    public BeneficiaryModel(int beneficiaryId, String firstName) {
        this.beneficiaryId = beneficiaryId;
        this.firstName = firstName;
    }

    public int getBeneficiaryId() {
        return beneficiaryId;
    }

    public String getFirstName() {
        return firstName;
    }

    @Override
    public String toString() {
        return firstName;
    }
}