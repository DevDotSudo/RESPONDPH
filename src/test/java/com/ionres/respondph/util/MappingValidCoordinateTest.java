package com.ionres.respondph.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mapping - isValidCoordinate()")
class MappingValidCoordinateTest {

    @Nested
    @DisplayName("Valid coordinates")
    class ValidCoordinates {

        @ParameterizedTest
        @CsvSource({
                "0.0, 0.0",
                "11.05, 122.78",
                "90.0, 180.0",
                "-90.0, -180.0",
                "45.0, -90.0",
                "-45.0, 90.0"
        })
        @DisplayName("accepts valid lat/lon within global range")
        void validCoordinates(double lat, double lon) {
            assertTrue(Mapping.isValidCoordinate(lat, lon));
        }
    }

    @Nested
    @DisplayName("Invalid coordinates")
    class InvalidCoordinates {

        @Test
        @DisplayName("rejects latitude > 90")
        void latAbove90() {
            assertFalse(Mapping.isValidCoordinate(91.0, 0.0));
        }

        @Test
        @DisplayName("rejects latitude < -90")
        void latBelowMinus90() {
            assertFalse(Mapping.isValidCoordinate(-91.0, 0.0));
        }

        @Test
        @DisplayName("rejects longitude > 180")
        void lonAbove180() {
            assertFalse(Mapping.isValidCoordinate(0.0, 181.0));
        }

        @Test
        @DisplayName("rejects longitude < -180")
        void lonBelowMinus180() {
            assertFalse(Mapping.isValidCoordinate(0.0, -181.0));
        }

        @Test
        @DisplayName("rejects NaN latitude")
        void nanLat() {
            assertFalse(Mapping.isValidCoordinate(Double.NaN, 122.0));
        }

        @Test
        @DisplayName("rejects NaN longitude")
        void nanLon() {
            assertFalse(Mapping.isValidCoordinate(11.0, Double.NaN));
        }
    }
}

