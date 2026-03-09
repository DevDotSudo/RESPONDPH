package com.ionres.respondph.model;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BeneficiaryModel")
class BeneficiaryModelTest {

    @Nested
    @DisplayName("Constructors")
    class Constructors {

        @Test
        @DisplayName("default constructor creates model with defaults")
        void defaultConstructor() {
            BeneficiaryModel model = new BeneficiaryModel();
            assertEquals(0, model.getId());
            assertNull(model.getFirstname());
            assertNull(model.getLastname());
        }

        @Test
        @DisplayName("6-arg constructor sets id and name fields")
        void sixArgConstructor() {
            BeneficiaryModel model = new BeneficiaryModel(
                    1, "Juan", "M", "Dela Cruz", "11.05", "122.78");
            assertEquals(1, model.getId());
            assertEquals("Juan", model.getFirstname());
            assertEquals("M", model.getMiddlename());
            assertEquals("Dela Cruz", model.getLastname());
            assertEquals("11.05", model.getLatitude());
            assertEquals("122.78", model.getLongitude());
        }

        @Test
        @DisplayName("full constructor sets all fields")
        void fullConstructor() {
            BeneficiaryModel model = new BeneficiaryModel(
                    "Maria", "A", "Santos", "1990-05-15", "Brgy. 1",
                    0.75, "Female", "Single", "Not Solo Parent",
                    "11.05", "122.78", "09123456789",
                    "None", "Healthy", "Yes", "Safely Managed",
                    "Concrete", "Owned", "Regular",
                    "10000", "College", "Reliable",
                    "Admin1", "2024-01-01"
            );
            assertEquals("Maria", model.getFirstname());
            assertEquals("A", model.getMiddlename());
            assertEquals("Santos", model.getLastname());
            assertEquals("1990-05-15", model.getBirthDate());
            assertEquals("Brgy. 1", model.getBarangay());
            assertEquals(0.75, model.getAgeScore());
            assertEquals("Female", model.getGender());
            assertEquals("Single", model.getMaritalStatus());
            assertEquals("Not Solo Parent", model.getSoloParentStatus());
            assertEquals("11.05", model.getLatitude());
            assertEquals("122.78", model.getLongitude());
            assertEquals("09123456789", model.getMobileNumber());
            assertEquals("None", model.getDisabilityType());
            assertEquals("Healthy", model.getHealthCondition());
            assertEquals("Yes", model.getCleanWaterAccess());
            assertEquals("Safely Managed", model.getSanitationFacility());
            assertEquals("Concrete", model.getHouseType());
            assertEquals("Owned", model.getOwnerShipStatus());
            assertEquals("Regular", model.getEmploymentStatus());
            assertEquals("10000", model.getMonthlyIncome());
            assertEquals("College", model.getEducationalLevel());
            assertEquals("Reliable", model.getDigitalAccess());
            assertEquals("Admin1", model.getAddedBy());
            assertEquals("2024-01-01", model.getRegDate());
        }
    }

    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("all setters update values correctly")
        void allSetters() {
            BeneficiaryModel model = new BeneficiaryModel();
            model.setId(99);
            model.setFirstname("Pedro");
            model.setMiddlename("B");
            model.setLastname("Garcia");
            model.setBirthDate("2000-12-25");
            model.setBarangay("Brgy. 5");
            model.setAgeScore(0.5);
            model.setGender("Male");
            model.setMaritalStatus("Married");
            model.setSoloParentStatus("Solo Parent with SN");
            model.setLatitude("11.10");
            model.setLongitude("122.85");
            model.setMobileNumber("+639123456789");
            model.setDisabilityType("Physical");
            model.setHealthCondition("Chronically Ill");
            model.setCleanWaterAccess("No");
            model.setSanitationFacility("Shared");
            model.setHouseType("Light Materials");
            model.setOwnerShipStatus("Rented");
            model.setEmploymentStatus("Unemployed");
            model.setMonthlyIncome("5000");
            model.setEducationalLevel("Highschool");
            model.setDigitalAccess("Limited Access");
            model.setAddedBy("Admin2");
            model.setRegDate("2024-06-01");

            assertEquals(99, model.getId());
            assertEquals("Pedro", model.getFirstname());
            assertEquals("B", model.getMiddlename());
            assertEquals("Garcia", model.getLastname());
            assertEquals("2000-12-25", model.getBirthDate());
            assertEquals("Brgy. 5", model.getBarangay());
            assertEquals(0.5, model.getAgeScore());
            assertEquals("Male", model.getGender());
            assertEquals("Married", model.getMaritalStatus());
            assertEquals("Solo Parent with SN", model.getSoloParentStatus());
            assertEquals("11.10", model.getLatitude());
            assertEquals("122.85", model.getLongitude());
            assertEquals("+639123456789", model.getMobileNumber());
            assertEquals("Physical", model.getDisabilityType());
            assertEquals("Chronically Ill", model.getHealthCondition());
            assertEquals("No", model.getCleanWaterAccess());
            assertEquals("Shared", model.getSanitationFacility());
            assertEquals("Light Materials", model.getHouseType());
            assertEquals("Rented", model.getOwnerShipStatus());
            assertEquals("Unemployed", model.getEmploymentStatus());
            assertEquals("5000", model.getMonthlyIncome());
            assertEquals("Highschool", model.getEducationalLevel());
            assertEquals("Limited Access", model.getDigitalAccess());
            assertEquals("Admin2", model.getAddedBy());
            assertEquals("2024-06-01", model.getRegDate());
        }
    }
}

