package com.ionres.respondph.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GeographicUtils")
class GeographicUtilsTest {

    private static final double DELTA = 1.0; // 1 meter tolerance

    // ─── calculateDistance ────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateDistance()")
    class CalculateDistance {

        @Test
        @DisplayName("returns 0 for same point")
        void samePoint() {
            double d = GeographicUtils.calculateDistance(11.05, 122.78, 11.05, 122.78);
            assertEquals(0.0, d, DELTA);
        }

        @Test
        @DisplayName("returns correct approximate distance for known coordinates")
        void knownDistance() {
            // Manila (14.5995, 120.9842) to Cebu (10.3157, 123.8854)
            // Approximate great-circle distance ~ 565 km = 565,000 m
            double d = GeographicUtils.calculateDistance(14.5995, 120.9842, 10.3157, 123.8854);
            assertTrue(d > 550_000 && d < 580_000,
                    "Expected ~565 km but got " + d / 1000 + " km");
        }

        @Test
        @DisplayName("returns NaN for NaN latitude")
        void nanLatitude() {
            assertTrue(Double.isNaN(GeographicUtils.calculateDistance(Double.NaN, 122.0, 11.0, 122.0)));
        }

        @Test
        @DisplayName("returns NaN for NaN longitude")
        void nanLongitude() {
            assertTrue(Double.isNaN(GeographicUtils.calculateDistance(11.0, Double.NaN, 11.0, 122.0)));
        }

        @Test
        @DisplayName("returns NaN for invalid coordinates (lat > 90)")
        void invalidLatitude() {
            assertTrue(Double.isNaN(GeographicUtils.calculateDistance(91.0, 122.0, 11.0, 122.0)));
        }

        @Test
        @DisplayName("returns NaN for invalid coordinates (lon > 180)")
        void invalidLongitude() {
            assertTrue(Double.isNaN(GeographicUtils.calculateDistance(11.0, 181.0, 11.0, 122.0)));
        }

        @Test
        @DisplayName("distance is symmetric")
        void symmetric() {
            double d1 = GeographicUtils.calculateDistance(11.0, 122.7, 11.1, 122.8);
            double d2 = GeographicUtils.calculateDistance(11.1, 122.8, 11.0, 122.7);
            assertEquals(d1, d2, 0.01);
        }
    }

    // ─── isInsideCircle ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("isInsideCircle()")
    class IsInsideCircle {

        @Test
        @DisplayName("returns true when point is at center")
        void pointAtCenter() {
            assertTrue(GeographicUtils.isInsideCircle(11.0, 122.0, 11.0, 122.0, 1000));
        }

        @Test
        @DisplayName("returns true when point is inside radius")
        void pointInside() {
            // Two very close points (~1 km apart) with 5 km radius
            assertTrue(GeographicUtils.isInsideCircle(11.001, 122.001, 11.0, 122.0, 5000));
        }

        @Test
        @DisplayName("returns false when point is outside radius")
        void pointOutside() {
            // Points ~15 km apart with 1 km radius
            assertFalse(GeographicUtils.isInsideCircle(11.1, 122.1, 11.0, 122.0, 1000));
        }

        @Test
        @DisplayName("returns false for NaN point coordinates")
        void nanPoint() {
            assertFalse(GeographicUtils.isInsideCircle(Double.NaN, 122.0, 11.0, 122.0, 1000));
        }

        @Test
        @DisplayName("returns false for NaN circle coordinates")
        void nanCircle() {
            assertFalse(GeographicUtils.isInsideCircle(11.0, 122.0, Double.NaN, 122.0, 1000));
        }

        @Test
        @DisplayName("returns false for negative radius")
        void negativeRadius() {
            assertFalse(GeographicUtils.isInsideCircle(11.0, 122.0, 11.0, 122.0, -100));
        }

        @Test
        @DisplayName("returns false for zero radius")
        void zeroRadius() {
            assertFalse(GeographicUtils.isInsideCircle(11.0, 122.0, 11.0, 122.0, 0));
        }

        @Test
        @DisplayName("returns false for NaN radius")
        void nanRadius() {
            assertFalse(GeographicUtils.isInsideCircle(11.0, 122.0, 11.0, 122.0, Double.NaN));
        }
    }
}

