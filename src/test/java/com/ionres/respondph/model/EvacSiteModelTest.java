package com.ionres.respondph.model;

import com.ionres.respondph.evac_site.EvacSiteModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EvacSiteModel")
class EvacSiteModelTest {

    @Test
    @DisplayName("default constructor creates model with null fields")
    void defaultConstructor() {
        EvacSiteModel model = new EvacSiteModel();
        assertEquals(0, model.getEvacId());
        assertNull(model.getName());
        assertNull(model.getCapacity());
    }

    @Test
    @DisplayName("parameterized constructor sets all fields")
    void parameterizedConstructor() {
        EvacSiteModel model = new EvacSiteModel("Barangay Hall", "500", "11.05", "122.78", "Main evac center");
        assertEquals("Barangay Hall", model.getName());
        assertEquals("500", model.getCapacity());
        assertEquals("11.05", model.getLat());
        assertEquals("122.78", model.getLongi());
        assertEquals("Main evac center", model.getNotes());
    }

    @Test
    @DisplayName("all setters and getters work")
    void settersAndGetters() {
        EvacSiteModel model = new EvacSiteModel();
        model.setEvacId(3);
        model.setName("School Gymnasium");
        model.setCapacity("1000");
        model.setLat("11.10");
        model.setLongi("122.90");
        model.setNotes("Indoor facility");

        assertEquals(3, model.getEvacId());
        assertEquals("School Gymnasium", model.getName());
        assertEquals("1000", model.getCapacity());
        assertEquals("11.10", model.getLat());
        assertEquals("122.90", model.getLongi());
        assertEquals("Indoor facility", model.getNotes());
    }
}

