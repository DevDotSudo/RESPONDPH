package com.ionres.respondph.evacuation_plan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GeoDistanceCalculator")
class GeoDistanceCalculatorTest {

    private static final double KM_DELTA = 0.5; // 0.5 km tolerance

    @Nested
    @DisplayName("calculateDistance()")
    class CalculateDistanceTests {

        @Test
        @DisplayName("returns 0 for same coordinates")
        void sameCoordinates() {
            double d = GeoDistanceCalculator.calculateDistance(11.05, 122.78, 11.05, 122.78);
            assertEquals(0.0, d, 0.001);
        }

        @Test
        @DisplayName("returns correct approximate distance for Manila to Cebu")
        void manilaToCebu() {
            double d = GeoDistanceCalculator.calculateDistance(14.5995, 120.9842, 10.3157, 123.8854);
            // ~565 km
            assertTrue(d > 550 && d < 580,
                    "Expected ~565 km but got " + d + " km");
        }

        @Test
        @DisplayName("distance is symmetric")
        void symmetric() {
            double d1 = GeoDistanceCalculator.calculateDistance(11.0, 122.0, 11.1, 122.1);
            double d2 = GeoDistanceCalculator.calculateDistance(11.1, 122.1, 11.0, 122.0);
            assertEquals(d1, d2, 0.0001);
        }

        @Test
        @DisplayName("short distance within a city ~1km")
        void shortDistance() {
            // ~1 km apart
            double d = GeoDistanceCalculator.calculateDistance(11.0, 122.0, 11.009, 122.0);
            assertTrue(d > 0.5 && d < 1.5, "Expected ~1 km but got " + d + " km");
        }
    }

    @Nested
    @DisplayName("formatDistance()")
    class FormatDistanceTests {

        @Test
        @DisplayName("formats distance < 1 km as meters")
        void metersFormat() {
            assertEquals("500 m", GeoDistanceCalculator.formatDistance(0.5));
        }

        @Test
        @DisplayName("formats very small distance as meters")
        void verySmall() {
            assertEquals("50 m", GeoDistanceCalculator.formatDistance(0.05));
        }

        @Test
        @DisplayName("formats distance >= 1 km with two decimal places")
        void kmFormat() {
            assertEquals("5.50 km", GeoDistanceCalculator.formatDistance(5.5));
        }

        @Test
        @DisplayName("formats exact 1 km")
        void exactOneKm() {
            assertEquals("1.00 km", GeoDistanceCalculator.formatDistance(1.0));
        }

        @Test
        @DisplayName("formats large distance")
        void largeDistance() {
            assertEquals("100.25 km", GeoDistanceCalculator.formatDistance(100.25));
        }

        @Test
        @DisplayName("formats 0 distance as meters")
        void zeroDistance() {
            assertEquals("0 m", GeoDistanceCalculator.formatDistance(0.0));
        }
    }
}

