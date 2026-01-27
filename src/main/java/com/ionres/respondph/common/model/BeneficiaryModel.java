package com.ionres.respondph.common.model;

/**
 * Simplified BeneficiaryModel for display purposes (e.g., dropdowns, lists).
 * For full beneficiary data operations, use com.ionres.respondph.beneficiary.BeneficiaryModel
 */
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
