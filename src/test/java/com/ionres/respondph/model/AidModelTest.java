package com.ionres.respondph.model;

import com.ionres.respondph.aid.AidModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AidModel")
class AidModelTest {

    @Test
    @DisplayName("default constructor creates model with defaults")
    void defaultConstructor() {
        AidModel model = new AidModel();
        assertEquals(0, model.getAidId());
        assertEquals(0, model.getBeneficiaryId());
        assertNull(model.getName());
        assertNull(model.getDate());
    }

    @Test
    @DisplayName("parameterized constructor sets all fields")
    void parameterizedConstructor() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        AidModel model = new AidModel(1, 2, "Food Pack", date, 10.0, 500.0, "Red Cross", 3);

        assertEquals(1, model.getBeneficiaryId());
        assertEquals(2, model.getDisasterId());
        assertEquals("Food Pack", model.getName());
        assertEquals(date, model.getDate());
        assertEquals(10.0, model.getQuantity());
        assertEquals(500.0, model.getCost());
        assertEquals("Red Cross", model.getProvider());
        assertEquals(3, model.getAidTypeId());
    }

    @Test
    @DisplayName("all setters and getters work")
    void settersAndGetters() {
        AidModel model = new AidModel();
        model.setAidId(10);
        model.setBeneficiaryId(20);
        model.setDisasterId(30);
        model.setName("Medicines");
        model.setDate(LocalDate.of(2024, 5, 1));
        model.setQuantity(25.5);
        model.setCost(1000.0);
        model.setProvider("DSWD");
        model.setAidTypeId(5);
        model.setNotes("Urgent delivery");
        model.setBeneficiaryName("Juan Dela Cruz");
        model.setDisasterName("Typhoon Paeng");

        assertEquals(10, model.getAidId());
        assertEquals(20, model.getBeneficiaryId());
        assertEquals(30, model.getDisasterId());
        assertEquals("Medicines", model.getName());
        assertEquals(LocalDate.of(2024, 5, 1), model.getDate());
        assertEquals(25.5, model.getQuantity());
        assertEquals(1000.0, model.getCost());
        assertEquals("DSWD", model.getProvider());
        assertEquals(5, model.getAidTypeId());
        assertEquals("Urgent delivery", model.getNotes());
        assertEquals("Juan Dela Cruz", model.getBeneficiaryName());
        assertEquals("Typhoon Paeng", model.getDisasterName());
    }

    @Test
    @DisplayName("toString includes key fields")
    void toStringFormat() {
        AidModel model = new AidModel();
        model.setAidId(7);
        model.setName("Relief Goods");
        String str = model.toString();
        assertTrue(str.contains("7"));
        assertTrue(str.contains("Relief Goods"));
    }
}

