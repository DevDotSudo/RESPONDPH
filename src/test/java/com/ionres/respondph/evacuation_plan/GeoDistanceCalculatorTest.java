package com.ionres.respondph.evacuation_plan;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Haversine-based GeoDistanceCalculator used
 * in evacuation site proximity calculations.
 */
@DisplayName("GeoDistanceCalculator — Haversine Distance")
class GeoDistanceCalculatorTest {

    // ═══════════════════════════════════════════════════════════════════════
    // Known Reference Distances
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Same point → distance is 0")
    void samePoint() {
        double d = GeoDistanceCalculator.calculateDistance(
                11.052390, 122.786762,
                11.052390, 122.786762);
        assertEquals(0.0, d, 1e-10);
    }

    @Test
    @DisplayName("Banate to Iloilo City ≈ 52–56 km")
    void banateToIloiloCity() {
        // Banate center: 11.0524, 122.7868
        // Iloilo City center: 10.7202, 122.5621
        double d = GeoDistanceCalculator.calculateDistance(
                11.0524, 122.7868,
                10.7202, 122.5621);
        assertTrue(d > 40 && d < 60,
                "Expected 40-60 km, got " + d + " km");
    }

    @Test
    @DisplayName("Manila to Cebu ≈ 565 km")
    void manilaToCebu() {
        double d = GeoDistanceCalculator.calculateDistance(
                14.5995, 120.9842,
                10.3157, 123.8854);
        assertTrue(d > 540 && d < 590,
                "Expected ~565 km, got " + d + " km");
    }

    @ParameterizedTest
    @CsvSource({
            "0.0, 0.0, 0.0, 1.0,    111.19",  // 1 degree longitude at equator ≈ 111.19 km
            "0.0, 0.0, 1.0, 0.0,    111.19",  // 1 degree latitude ≈ 111.19 km
    })
    @DisplayName("1 degree at equator ≈ 111.19 km")
    void oneDegreeAtEquator(double lat1, double lon1, double lat2, double lon2, double expected) {
        double d = GeoDistanceCalculator.calculateDistance(lat1, lon1, lat2, lon2);
        assertEquals(expected, d, 1.0,
                "Expected ~" + expected + " km, got " + d);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Symmetry
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Distance is symmetric: d(A,B) == d(B,A)")
    void symmetric() {
        double d1 = GeoDistanceCalculator.calculateDistance(11.0, 122.0, 12.0, 123.0);
        double d2 = GeoDistanceCalculator.calculateDistance(12.0, 123.0, 11.0, 122.0);
        assertEquals(d1, d2, 1e-10);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Within Banate municipality — short distances
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Two points within Banate ≈ 1-5 km")
    void withinBanate() {
        // Two barangays in Banate
        double d = GeoDistanceCalculator.calculateDistance(
                11.0524, 122.7868,
                11.0700, 122.7600);
        assertTrue(d > 0.5 && d < 10,
                "Expected short distance within municipality, got " + d + " km");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // formatDistance
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("formatDistance — Human-Readable Formatting")
    class FormatDistanceTests {

        @Test
        @DisplayName("Distance < 1 km → shown in meters")
        void metersFormat() {
            String result = GeoDistanceCalculator.formatDistance(0.5);
            assertEquals("500 m", result);
        }

        @Test
        @DisplayName("Distance ≥ 1 km → shown in km with 2 decimals")
        void kmFormat() {
            String result = GeoDistanceCalculator.formatDistance(2.345);
            assertEquals("2.35 km", result);
        }

        @Test
        @DisplayName("Exactly 1 km → shown in km format")
        void exactlyOneKm() {
            String result = GeoDistanceCalculator.formatDistance(1.0);
            assertEquals("1.00 km", result);
        }

        @Test
        @DisplayName("Very small distance → 0 m")
        void verySmall() {
            String result = GeoDistanceCalculator.formatDistance(0.0001);
            assertEquals("0 m", result);
        }

        @Test
        @DisplayName("0.999 km → 999 m (still below threshold)")
        void justBelowOneKm() {
            String result = GeoDistanceCalculator.formatDistance(0.999);
            assertEquals("999 m", result);
        }

        @Test
        @DisplayName("Large distance → km format")
        void largeDistance() {
            String result = GeoDistanceCalculator.formatDistance(565.123);
            assertEquals("565.12 km", result);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Positive distance guarantee
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Distance is always non-negative")
    void nonNegative() {
        double d = GeoDistanceCalculator.calculateDistance(
                -33.8688, 151.2093,  // Sydney
                51.5074, -0.1278);    // London
        assertTrue(d > 0);
    }

    @Test
    @DisplayName("Antipodal points ≈ 20,000 km (half Earth circumference)")
    void antipodalPoints() {
        // North Pole to South Pole
        double d = GeoDistanceCalculator.calculateDistance(90, 0, -90, 0);
        assertEquals(20015.0, d, 100,
                "Pole-to-pole should be ~20,015 km");
    }
}

