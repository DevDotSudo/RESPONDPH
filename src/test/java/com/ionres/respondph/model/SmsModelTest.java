package com.ionres.respondph.model;

import com.ionres.respondph.sendsms.SmsModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SmsModel")
class SmsModelTest {

    @Test
    @DisplayName("default constructor creates model with null fields")
    void defaultConstructor() {
        SmsModel model = new SmsModel();
        assertEquals(0, model.getMessageID());
        assertNull(model.getPhonenumber());
        assertNull(model.getMessage());
        assertNull(model.getStatus());
    }

    @Test
    @DisplayName("3-arg constructor sets fields and defaults")
    void threeArgConstructor() {
        SmsModel model = new SmsModel("+639123456789", "Juan Dela Cruz", "Evacuate now!");
        assertEquals("+639123456789", model.getPhonenumber());
        assertEquals("Juan Dela Cruz", model.getFullname());
        assertEquals("Evacuate now!", model.getMessage());
        assertEquals("PENDING", model.getStatus());
        assertEquals("GSM", model.getSendMethod());
        assertNotNull(model.getDateSent());
    }

    @Test
    @DisplayName("all setters and getters work correctly")
    void settersAndGetters() {
        SmsModel model = new SmsModel();
        model.setMessageID(42);
        model.setBeneficiaryID(10);
        Timestamp ts = Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 12, 0));
        model.setDateSent(ts);
        model.setPhonenumber("09123456789");
        model.setFullname("Maria Santos");
        model.setMessage("Emergency alert!");
        model.setStatus("SENT");
        model.setPhoneString("+639123456789");
        model.setSendMethod("API");

        assertEquals(42, model.getMessageID());
        assertEquals(10, model.getBeneficiaryID());
        assertEquals(ts, model.getDateSent());
        assertEquals("09123456789", model.getPhonenumber());
        assertEquals("Maria Santos", model.getFullname());
        assertEquals("Emergency alert!", model.getMessage());
        assertEquals("SENT", model.getStatus());
        assertEquals("+639123456789", model.getPhoneString());
        assertEquals("API", model.getSendMethod());
    }
}

