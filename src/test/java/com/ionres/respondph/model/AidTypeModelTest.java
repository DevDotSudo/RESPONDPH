package com.ionres.respondph.model;

import com.ionres.respondph.aid_type.AidTypeModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AidTypeModel")
class AidTypeModelTest {

    @Test
    @DisplayName("default constructor creates model with zero weights")
    void defaultConstructor() {
        AidTypeModel model = new AidTypeModel();
        assertEquals(0, model.getAidTypeId());
        assertNull(model.getAidTypeName());
        assertEquals(0.0, model.getAgeWeight());
    }

    @Test
    @DisplayName("parameterized constructor sets all weights and metadata")
    void parameterizedConstructor() {
        AidTypeModel model = new AidTypeModel(
                "Food Pack",
                0.15, 0.05, 0.05, 0.10,
                0.10, 0.10, 0.05, 0.05,
                0.05, 0.05, 0.05, 0.05,
                0.05, 0.03, 0.03, 0.04,
                "Basic food relief", 1, "2024-01-01"
        );
        assertEquals("Food Pack", model.getAidTypeName());
        assertEquals(0.15, model.getAgeWeight(), 0.001);
        assertEquals(0.05, model.getGenderWeight(), 0.001);
        assertEquals(0.05, model.getMaritalStatusWeight(), 0.001);
        assertEquals(0.10, model.getSoloParentWeight(), 0.001);
        assertEquals(0.10, model.getDisabilityWeight(), 0.001);
        assertEquals(0.10, model.getHealthConditionWeight(), 0.001);
        assertEquals(0.05, model.getAccessToCleanWaterWeight(), 0.001);
        assertEquals(0.05, model.getSanitationFacilityWeight(), 0.001);
        assertEquals(0.05, model.getHouseConstructionTypeWeight(), 0.001);
        assertEquals(0.05, model.getOwnershipWeight(), 0.001);
        assertEquals(0.05, model.getDamageSeverityWeight(), 0.001);
        assertEquals(0.05, model.getEmploymentStatusWeight(), 0.001);
        assertEquals(0.05, model.getMonthlyIncomeWeight(), 0.001);
        assertEquals(0.03, model.getEducationalLevelWeight(), 0.001);
        assertEquals(0.03, model.getDigitalAccessWeight(), 0.001);
        assertEquals(0.04, model.getDependencyRatioWeight(), 0.001);
        assertEquals("Basic food relief", model.getNotes());
        assertEquals(1, model.getAdminId());
        assertEquals("2024-01-01", model.getRegDate());
    }

    @Test
    @DisplayName("all setters and getters work correctly")
    void settersAndGetters() {
        AidTypeModel model = new AidTypeModel();
        model.setAidTypeId(5);
        model.setAidTypeName("Medical Kit");
        model.setAgeWeight(0.2);
        model.setGenderWeight(0.1);
        model.setMaritalStatusWeight(0.05);
        model.setSoloParentWeight(0.08);
        model.setDisabilityWeight(0.15);
        model.setHealthConditionWeight(0.12);
        model.setAccessToCleanWaterWeight(0.03);
        model.setSanitationFacilityWeight(0.03);
        model.setHouseConstructionTypeWeight(0.02);
        model.setOwnershipWeight(0.02);
        model.setDamageSeverityWeight(0.04);
        model.setEmploymentStatusWeight(0.04);
        model.setMonthlyIncomeWeight(0.04);
        model.setEducationalLevelWeight(0.03);
        model.setDigitalAccessWeight(0.02);
        model.setDependencyRatioWeight(0.03);
        model.setNotes("Medical supplies");
        model.setAdminId(2);
        model.setAdminName("Admin User");
        model.setRegDate("2024-06-01");

        assertEquals(5, model.getAidTypeId());
        assertEquals("Medical Kit", model.getAidTypeName());
        assertEquals(0.2, model.getAgeWeight());
        assertEquals(0.1, model.getGenderWeight());
        assertEquals(0.15, model.getDisabilityWeight());
        assertEquals("Medical supplies", model.getNotes());
        assertEquals(2, model.getAdminId());
        assertEquals("Admin User", model.getAdminName());
        assertEquals("2024-06-01", model.getRegDate());
    }
}

