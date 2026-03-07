package com.ionres.respondph.model;

import com.ionres.respondph.evacuation_plan.RankedBeneficiaryModel;
import com.ionres.respondph.evacuation_plan.RankedBeneficiaryWithLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RankedBeneficiaryModel & RankedBeneficiaryWithLocation")
class RankedBeneficiaryModelTest {

    @Nested
    @DisplayName("RankedBeneficiaryModel")
    class RankedModelTests {

        @Test
        @DisplayName("default constructor creates model with defaults")
        void defaultConstructor() {
            RankedBeneficiaryModel model = new RankedBeneficiaryModel();
            assertEquals(0, model.getBeneficiaryId());
            assertNull(model.getFirstName());
        }

        @Test
        @DisplayName("parameterized constructor sets all fields")
        void parameterizedConstructor() {
            RankedBeneficiaryModel model = new RankedBeneficiaryModel(
                    1, "Juan", "Dela Cruz", 0.85, "High Vulnerability", 5);
            assertEquals(1, model.getBeneficiaryId());
            assertEquals("Juan", model.getFirstName());
            assertEquals("Dela Cruz", model.getLastName());
            assertEquals(0.85, model.getFinalScore(), 0.001);
            assertEquals("High Vulnerability", model.getScoreCategory());
            assertEquals(5, model.getHouseholdMembers());
        }

        @Test
        @DisplayName("setters work correctly")
        void setters() {
            RankedBeneficiaryModel model = new RankedBeneficiaryModel();
            model.setBeneficiaryId(10);
            model.setFirstName("Maria");
            model.setLastName("Santos");
            model.setFinalScore(0.65);
            model.setScoreCategory("Moderate");
            model.setHouseholdMembers(3);

            assertEquals(10, model.getBeneficiaryId());
            assertEquals("Maria", model.getFirstName());
            assertEquals("Santos", model.getLastName());
            assertEquals(0.65, model.getFinalScore());
            assertEquals("Moderate", model.getScoreCategory());
            assertEquals(3, model.getHouseholdMembers());
        }

        @Test
        @DisplayName("toString contains key info")
        void toStringFormat() {
            RankedBeneficiaryModel model = new RankedBeneficiaryModel(
                    7, "Pedro", "Garcia", 0.72, "High", 4);
            String str = model.toString();
            assertTrue(str.contains("7"));
            assertTrue(str.contains("Pedro"));
            assertTrue(str.contains("Garcia"));
            assertTrue(str.contains("0.72"));
        }
    }

    @Nested
    @DisplayName("RankedBeneficiaryWithLocation")
    class WithLocationTests {

        @Test
        @DisplayName("default constructor creates model with defaults")
        void defaultConstructor() {
            RankedBeneficiaryWithLocation model = new RankedBeneficiaryWithLocation();
            assertEquals(0.0, model.getLatitude());
            assertEquals(0.0, model.getLongitude());
            assertEquals(0, model.getAssignedEvacSiteId());
            assertNull(model.getAssignedEvacSiteName());
        }

        @Test
        @DisplayName("parameterized constructor sets location fields")
        void parameterizedConstructor() {
            RankedBeneficiaryWithLocation model = new RankedBeneficiaryWithLocation(
                    1, "Juan", "Dela Cruz", 0.9, "High", 4, 11.05, 122.78);
            assertEquals(1, model.getBeneficiaryId());
            assertEquals("Juan", model.getFirstName());
            assertEquals(11.05, model.getLatitude());
            assertEquals(122.78, model.getLongitude());
        }

        @Test
        @DisplayName("evacuation assignment setters work")
        void evacAssignment() {
            RankedBeneficiaryWithLocation model = new RankedBeneficiaryWithLocation();
            model.setAssignedEvacSiteId(5);
            model.setAssignedEvacSiteName("Barangay Hall");
            model.setDistanceToEvacSite(2.5);

            assertEquals(5, model.getAssignedEvacSiteId());
            assertEquals("Barangay Hall", model.getAssignedEvacSiteName());
            assertEquals(2.5, model.getDistanceToEvacSite());
        }

        @Test
        @DisplayName("toString includes evac site when assigned")
        void toStringWithEvac() {
            RankedBeneficiaryWithLocation model = new RankedBeneficiaryWithLocation(
                    1, "Juan", "Dela Cruz", 0.9, "High", 4, 11.05, 122.78);
            model.setAssignedEvacSiteName("School Gym");
            model.setDistanceToEvacSite(1.5);
            String str = model.toString();
            assertTrue(str.contains("School Gym"));
            assertTrue(str.contains("1.50"));
        }

        @Test
        @DisplayName("toString without evac falls back to parent format")
        void toStringWithoutEvac() {
            RankedBeneficiaryWithLocation model = new RankedBeneficiaryWithLocation(
                    1, "Juan", "Dela Cruz", 0.9, "High", 4, 11.05, 122.78);
            String str = model.toString();
            assertTrue(str.contains("Juan"));
            assertTrue(str.contains("Dela Cruz"));
        }
    }
}

