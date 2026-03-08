package com.ionres.respondph.model;

import com.ionres.respondph.household_score.HouseholdScoreModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HouseholdScoreModel")
class HouseholdScoreModelTest {

    @Test
    @DisplayName("default constructor creates model with zero scores")
    void defaultConstructor() {
        HouseholdScoreModel model = new HouseholdScoreModel();
        assertEquals(0, model.getHouseholdScoreId());
        assertEquals(0, model.getBeneficiaryId());
        assertEquals(0, model.getDisasterId());
        assertEquals(0.0, model.getAgeScore());
        assertEquals(0.0, model.getGenderScore());
    }

    @Test
    @DisplayName("all setters and getters work correctly")
    void allSettersAndGetters() {
        HouseholdScoreModel model = new HouseholdScoreModel();
        model.setHouseholdScoreId(1);
        model.setBeneficiaryId(10);
        model.setDisasterId(5);
        model.setAgeScore(0.8);
        model.setGenderScore(0.6);
        model.setMaritalStatusScore(0.5);
        model.setSoloParentScore(0.3);
        model.setDisabilityScore(0.9);
        model.setHealthConditionScore(0.7);
        model.setAccessToCleanWaterScore(0.4);
        model.setSanitationFacilitiesScore(0.5);
        model.setHouseConstructionTypeScore(0.6);
        model.setOwnershipScore(0.3);
        model.setDamageSeverityScore(0.95);
        model.setEmploymentStatusScore(0.7);
        model.setMonthlyIncomeScore(0.85);
        model.setEducationLevelScore(0.4);
        model.setDigitalAccessScore(0.2);
        model.setDependencyRatioScore(0.55);

        assertEquals(1, model.getHouseholdScoreId());
        assertEquals(10, model.getBeneficiaryId());
        assertEquals(5, model.getDisasterId());
        assertEquals(0.8, model.getAgeScore());
        assertEquals(0.6, model.getGenderScore());
        assertEquals(0.5, model.getMaritalStatusScore());
        assertEquals(0.3, model.getSoloParentScore());
        assertEquals(0.9, model.getDisabilityScore());
        assertEquals(0.7, model.getHealthConditionScore());
        assertEquals(0.4, model.getAccessToCleanWaterScore());
        assertEquals(0.5, model.getSanitationFacilitiesScore());
        assertEquals(0.6, model.getHouseConstructionTypeScore());
        assertEquals(0.3, model.getOwnershipScore());
        assertEquals(0.95, model.getDamageSeverityScore());
        assertEquals(0.7, model.getEmploymentStatusScore());
        assertEquals(0.85, model.getMonthlyIncomeScore());
        assertEquals(0.4, model.getEducationLevelScore());
        assertEquals(0.2, model.getDigitalAccessScore());
        assertEquals(0.55, model.getDependencyRatioScore());
    }
}

