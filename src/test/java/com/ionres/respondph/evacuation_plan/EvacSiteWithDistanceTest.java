package com.ionres.respondph.evacuation_plan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EvacSiteWithDistance")
class EvacSiteWithDistanceTest {

    @Nested
    @DisplayName("Constructor & Getters/Setters")
    class ConstructorAndAccessors {

        @Test
        @DisplayName("default constructor creates object with default values")
        void defaultConstructor() {
            EvacSiteWithDistance site = new EvacSiteWithDistance();
            assertEquals(0, site.getEvacSiteId());
            assertNull(site.getEvacSiteName());
            assertEquals(0.0, site.getLatitude());
            assertEquals(0.0, site.getLongitude());
            assertEquals(0, site.getRemainingCapacity());
            assertEquals(0.0, site.getDistanceInKm());
        }

        @Test
        @DisplayName("parameterized constructor sets all fields")
        void parameterizedConstructor() {
            EvacSiteWithDistance site = new EvacSiteWithDistance(
                    1, "Barangay Hall", 11.05, 122.78, 200, 2.5);
            assertEquals(1, site.getEvacSiteId());
            assertEquals("Barangay Hall", site.getEvacSiteName());
            assertEquals(11.05, site.getLatitude());
            assertEquals(122.78, site.getLongitude());
            assertEquals(200, site.getRemainingCapacity());
            assertEquals(2.5, site.getDistanceInKm());
        }

        @Test
        @DisplayName("setters update all fields")
        void setters() {
            EvacSiteWithDistance site = new EvacSiteWithDistance();
            site.setEvacSiteId(5);
            site.setEvacSiteName("School Gym");
            site.setLatitude(11.1);
            site.setLongitude(122.9);
            site.setRemainingCapacity(500);
            site.setDistanceInKm(3.7);

            assertEquals(5, site.getEvacSiteId());
            assertEquals("School Gym", site.getEvacSiteName());
            assertEquals(11.1, site.getLatitude());
            assertEquals(122.9, site.getLongitude());
            assertEquals(500, site.getRemainingCapacity());
            assertEquals(3.7, site.getDistanceInKm());
        }
    }

    @Nested
    @DisplayName("Comparable (sorting by distance)")
    class ComparableTests {

        @Test
        @DisplayName("sorts nearest first")
        void sortsNearestFirst() {
            EvacSiteWithDistance far = new EvacSiteWithDistance(1, "Far", 0, 0, 100, 10.0);
            EvacSiteWithDistance near = new EvacSiteWithDistance(2, "Near", 0, 0, 100, 1.0);
            EvacSiteWithDistance mid = new EvacSiteWithDistance(3, "Mid", 0, 0, 100, 5.0);

            List<EvacSiteWithDistance> list = new ArrayList<>(List.of(far, near, mid));
            Collections.sort(list);

            assertEquals("Near", list.get(0).getEvacSiteName());
            assertEquals("Mid", list.get(1).getEvacSiteName());
            assertEquals("Far", list.get(2).getEvacSiteName());
        }

        @Test
        @DisplayName("equal distances compare as zero")
        void equalDistances() {
            EvacSiteWithDistance a = new EvacSiteWithDistance(1, "A", 0, 0, 100, 5.0);
            EvacSiteWithDistance b = new EvacSiteWithDistance(2, "B", 0, 0, 100, 5.0);
            assertEquals(0, a.compareTo(b));
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("includes all key fields")
        void includesFields() {
            EvacSiteWithDistance site = new EvacSiteWithDistance(
                    7, "City Hall", 11.0, 122.0, 300, 4.25);
            String str = site.toString();
            assertTrue(str.contains("7"));
            assertTrue(str.contains("City Hall"));
            assertTrue(str.contains("4.25"));
            assertTrue(str.contains("300"));
        }
    }
}

