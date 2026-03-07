package com.ionres.respondph.model;

import com.ionres.respondph.familymembers.FamilyMembersModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FamilyMembersModel")
class FamilyMembersModelTest {

    @Nested
    @DisplayName("Constructors")
    class Constructors {

        @Test
        @DisplayName("default constructor creates model with defaults")
        void defaultConstructor() {
            FamilyMembersModel model = new FamilyMembersModel();
            assertEquals(0, model.getFamilyId());
            assertNull(model.getFirstName());
        }

        @Test
        @DisplayName("full constructor sets all fields correctly")
        void fullConstructor() {
            FamilyMembersModel model = new FamilyMembersModel(
                    "Maria", "A", "Dela Cruz",
                    "Spouse", "1985-03-20", 0.65,
                    "Female", "Married",
                    "None", "Healthy",
                    "Regular", "College",
                    1, "No notes", "2024-01-01"
            );

            assertEquals("Maria", model.getFirstName());
            assertEquals("A", model.getMiddleName());
            assertEquals("Dela Cruz", model.getLastName());
            assertEquals("Spouse", model.getRelationshipToBeneficiary());
            assertEquals("1985-03-20", model.getBirthDate());
            assertEquals(0.65, model.getAgeScore());
            assertEquals("Female", model.getGender());
            assertEquals("Married", model.getMaritalStatus());
            assertEquals("None", model.getDisabilityType());
            assertEquals("Healthy", model.getHealthCondition());
            assertEquals("Regular", model.getEmploymentStatus());
            assertEquals("College", model.getEducationalLevel());
            assertEquals(1, model.getBeneficiaryId());
            assertEquals("No notes", model.getNotes());
            assertEquals("2024-01-01", model.getRegDate());
        }
    }

    @Nested
    @DisplayName("Setters and Getters")
    class SettersGetters {

        @Test
        @DisplayName("all setters work correctly")
        void allSetters() {
            FamilyMembersModel model = new FamilyMembersModel();
            model.setFamilyId(10);
            model.setBeneficiaryId(5);
            model.setBeneficiaryName("Juan Dela Cruz");
            model.setFirstName("Pedro");
            model.setMiddleName("B");
            model.setLastName("Santos");
            model.setRelationshipToBeneficiary("Son");
            model.setBirthDate("2010-07-15");
            model.setAgeScore(0.9);
            model.setGender("Male");
            model.setMaritalStatus("Single");
            model.setDisabilityType("Physical");
            model.setHealthCondition("Chronically Ill");
            model.setEmploymentStatus("Unemployed");
            model.setEducationalLevel("Elementary");
            model.setNotes("Minor");
            model.setRegDate("2024-06-01");

            assertEquals(10, model.getFamilyId());
            assertEquals(5, model.getBeneficiaryId());
            assertEquals("Juan Dela Cruz", model.getBeneficiaryName());
            assertEquals("Pedro", model.getFirstName());
            assertEquals("B", model.getMiddleName());
            assertEquals("Santos", model.getLastName());
            assertEquals("Son", model.getRelationshipToBeneficiary());
            assertEquals("2010-07-15", model.getBirthDate());
            assertEquals(0.9, model.getAgeScore());
            assertEquals("Male", model.getGender());
            assertEquals("Single", model.getMaritalStatus());
            assertEquals("Physical", model.getDisabilityType());
            assertEquals("Chronically Ill", model.getHealthCondition());
            assertEquals("Unemployed", model.getEmploymentStatus());
            assertEquals("Elementary", model.getEducationalLevel());
            assertEquals("Minor", model.getNotes());
            assertEquals("2024-06-01", model.getRegDate());
        }
    }
}

