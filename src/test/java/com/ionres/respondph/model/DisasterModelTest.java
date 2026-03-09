package com.ionres.respondph.model;

import com.ionres.respondph.disaster.DisasterModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DisasterModel")
class DisasterModelTest {

    @Test
    @DisplayName("default constructor creates model with null fields")
    void defaultConstructor() {
        DisasterModel model = new DisasterModel();
        assertEquals(0, model.getDisasterId());
        assertNull(model.getDisasterType());
        assertNull(model.getDisasterName());
    }

    @Test
    @DisplayName("parameterized constructor sets all fields")
    void parameterizedConstructor() {
        DisasterModel model = new DisasterModel(
                "Typhoon", "Super Typhoon Yolanda", "2013-11-08",
                "11.25", "125.00", "150000",
                "Category 5 typhoon", "2013-11-10"
        );
        assertEquals("Typhoon", model.getDisasterType());
        assertEquals("Super Typhoon Yolanda", model.getDisasterName());
        assertEquals("2013-11-08", model.getDate());
        assertEquals("11.25", model.getLat());
        assertEquals("125.00", model.getLongi());
        assertEquals("150000", model.getRadius());
        assertEquals("Category 5 typhoon", model.getNotes());
        assertEquals("2013-11-10", model.getRegDate());
    }

    @Test
    @DisplayName("setters and getters work correctly")
    void settersAndGetters() {
        DisasterModel model = new DisasterModel();
        model.setDisasterId(5);
        model.setDisasterType("Earthquake");
        model.setDisasterName("Bohol Earthquake");
        model.setDate("2013-10-15");
        model.setLat("9.85");
        model.setLongi("124.03");
        model.setRadius("50000");
        model.setNotes("7.2 magnitude");
        model.setRegDate("2013-10-16");

        assertEquals(5, model.getDisasterId());
        assertEquals("Earthquake", model.getDisasterType());
        assertEquals("Bohol Earthquake", model.getDisasterName());
        assertEquals("2013-10-15", model.getDate());
        assertEquals("9.85", model.getLat());
        assertEquals("124.03", model.getLongi());
        assertEquals("50000", model.getRadius());
        assertEquals("7.2 magnitude", model.getNotes());
        assertEquals("2013-10-16", model.getRegDate());
    }
}

