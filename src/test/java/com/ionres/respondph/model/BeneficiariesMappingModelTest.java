package com.ionres.respondph.model;

import com.ionres.respondph.dashboard.BeneficiariesMappingModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BeneficiariesMappingModel")
class BeneficiariesMappingModelTest {

    @Test
    @DisplayName("constructor sets all fields")
    void constructor() {
        BeneficiariesMappingModel model = new BeneficiariesMappingModel(
                1, "Juan Dela Cruz", "11.05", "122.78");
        assertEquals(1, model.getId());
        assertEquals("Juan Dela Cruz", model.getBeneficiaryName());
        assertEquals("11.05", model.getLat());
        assertEquals("122.78", model.getLng());
    }

    @Test
    @DisplayName("setters update all fields")
    void setters() {
        BeneficiariesMappingModel model = new BeneficiariesMappingModel(
                1, "Old Name", "0", "0");
        model.setId(5);
        model.setBeneficiaryName("New Name");
        model.setLat("11.10");
        model.setLng("122.90");

        assertEquals(5, model.getId());
        assertEquals("New Name", model.getBeneficiaryName());
        assertEquals("11.10", model.getLat());
        assertEquals("122.90", model.getLng());
    }
}

