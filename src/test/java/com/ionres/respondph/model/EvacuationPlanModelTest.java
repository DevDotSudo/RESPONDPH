package com.ionres.respondph.model;

import com.ionres.respondph.evacuation_plan.EvacuationPlanModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EvacuationPlanModel")
class EvacuationPlanModelTest {

    @Test
    @DisplayName("default constructor creates model with defaults")
    void defaultConstructor() {
        EvacuationPlanModel model = new EvacuationPlanModel();
        assertEquals(0, model.getPlanId());
        assertNull(model.getBeneficiaryName());
        assertNull(model.getEvacSiteName());
    }

    @Test
    @DisplayName("parameterized constructor sets all fields")
    void parameterizedConstructor() {
        EvacuationPlanModel model = new EvacuationPlanModel(
                1, 10, "Juan Dela Cruz",
                5, "Barangay Hall",
                3, "Typhoon Yolanda",
                "2024-01-15"
        );
        assertEquals(1, model.getPlanId());
        assertEquals(10, model.getBeneficiaryId());
        assertEquals("Juan Dela Cruz", model.getBeneficiaryName());
        assertEquals(5, model.getEvacSiteId());
        assertEquals("Barangay Hall", model.getEvacSiteName());
        assertEquals(3, model.getDisasterId());
        assertEquals("Typhoon Yolanda", model.getDisasterName());
        assertEquals("2024-01-15", model.getDateCreated());
    }

    @Test
    @DisplayName("setters update values correctly")
    void settersAndGetters() {
        EvacuationPlanModel model = new EvacuationPlanModel();
        model.setPlanId(7);
        model.setBeneficiaryId(20);
        model.setBeneficiaryName("Maria Santos");
        model.setEvacSiteId(8);
        model.setEvacSiteName("School Gym");
        model.setDisasterId(4);
        model.setDisasterName("Earthquake");
        model.setDateCreated("2024-06-01");
        model.setNotes("Priority evacuation");

        assertEquals(7, model.getPlanId());
        assertEquals(20, model.getBeneficiaryId());
        assertEquals("Maria Santos", model.getBeneficiaryName());
        assertEquals(8, model.getEvacSiteId());
        assertEquals("School Gym", model.getEvacSiteName());
        assertEquals(4, model.getDisasterId());
        assertEquals("Earthquake", model.getDisasterName());
        assertEquals("2024-06-01", model.getDateCreated());
        assertEquals("Priority evacuation", model.getNotes());
    }
}

