package com.ionres.respondph.model;

import com.ionres.respondph.disaster_damage.DisasterDamageModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DisasterDamageModel")
class DisasterDamageModelTest {

    @Test
    @DisplayName("default constructor creates model with null fields")
    void defaultConstructor() {
        DisasterDamageModel model = new DisasterDamageModel();
        assertEquals(0, model.getBeneficiaryDisasterDamageId());
        assertEquals(0, model.getBeneficiaryId());
        assertNull(model.getHouseDamageSeverity());
    }

    @Test
    @DisplayName("parameterized constructor sets all fields")
    void parameterizedConstructor() {
        DisasterDamageModel model = new DisasterDamageModel(
                1, 2, "Totally Damaged", "2024-01-15",
                "Inspector A", "Roof collapsed", "2024-01-16"
        );
        assertEquals(1, model.getBeneficiaryId());
        assertEquals(2, model.getDisasterId());
        assertEquals("Totally Damaged", model.getHouseDamageSeverity());
        assertEquals("2024-01-15", model.getAssessmentDate());
        assertEquals("Inspector A", model.getVerifiedBy());
        assertEquals("Roof collapsed", model.getNotes());
        assertEquals("2024-01-16", model.getRegDate());
    }

    @Test
    @DisplayName("all setters and getters work correctly")
    void settersAndGetters() {
        DisasterDamageModel model = new DisasterDamageModel();
        model.setBeneficiaryDisasterDamageId(5);
        model.setBeneficiaryId(10);
        model.setBeneficiaryFirstname("Juan");
        model.setDisasterId(3);
        model.setDisasterType("Typhoon");
        model.setDisasterName("Yolanda");
        model.setHouseDamageSeverity("Partially Damaged");
        model.setAssessmentDate("2024-02-20");
        model.setVerifiedBy("Inspector B");
        model.setNotes("Wall cracked");
        model.setRegDate("2024-02-21");
        model.setImage(new byte[]{1, 2, 3});

        assertEquals(5, model.getBeneficiaryDisasterDamageId());
        assertEquals(10, model.getBeneficiaryId());
        assertEquals("Juan", model.getBeneficiaryFirstname());
        assertEquals(3, model.getDisasterId());
        assertEquals("Typhoon", model.getDisasterType());
        assertEquals("Yolanda", model.getDisasterName());
        assertEquals("Partially Damaged", model.getHouseDamageSeverity());
        assertEquals("2024-02-20", model.getAssessmentDate());
        assertEquals("Inspector B", model.getVerifiedBy());
        assertEquals("Wall cracked", model.getNotes());
        assertEquals("2024-02-21", model.getRegDate());
        assertArrayEquals(new byte[]{1, 2, 3}, model.getImage());
    }
}

