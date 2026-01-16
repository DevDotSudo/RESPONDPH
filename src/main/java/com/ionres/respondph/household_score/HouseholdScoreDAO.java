package com.ionres.respondph.household_score;

public interface HouseholdScoreDAO {
    public boolean saveHouseholdScore(int beneficiaryId, double genderScore, double maritalStatusScore,
                                      double soloParentScore, double disabilityScore, double healthConditionScore,
                                      double cleanWaterScore, double sanitationScore, double houseConstructionScore,
                                      double ownershipScore, double employmentScore, double monthlyIncomeScore,
                                      double educationScore, double digitalAccessScore);
}
