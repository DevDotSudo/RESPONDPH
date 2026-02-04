package com.ionres.respondph.common.model;

/**
 * Simplified BeneficiaryModel for display purposes (e.g., dropdowns, lists).
 * For full beneficiary data operations, use com.ionres.respondph.beneficiary.BeneficiaryModel
 */
public class BeneficiaryModel {
    private int beneficiaryId;
    private String firstName;
    private String middlename;
    private String lastname;

    public BeneficiaryModel(int beneficiaryId, String firstName, String middlename, String lastname) {
        this.beneficiaryId = beneficiaryId;
        this.firstName = firstName;
        this.middlename = middlename;
        this.lastname = lastname;
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
