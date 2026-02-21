package com.ionres.respondph.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PhoneNumberValidator — Philippine mobile number validation.
 * Tests +63 and 09 formats, normalization, and format conversion.
 */
@DisplayName("PhoneNumberValidator — PH Mobile Number Validation")
class PhoneNumberValidatorTest {

    // ═══════════════════════════════════════════════════════════════════════
    // Valid numbers
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("isValid — Valid Philippine Numbers")
    class ValidNumberTests {

        @ParameterizedTest
        @ValueSource(strings = {"+639171234567", "+639991234567", "+639001234567"})
        @DisplayName("Valid +63 format (13 chars, starts with 9)")
        void validInternational(String phone) {
            assertTrue(PhoneNumberValidator.isValid(phone),
                    phone + " should be valid");
        }

        @ParameterizedTest
        @ValueSource(strings = {"09171234567", "09991234567", "09001234567"})
        @DisplayName("Valid 09 format (11 digits)")
        void validLocal(String phone) {
            assertTrue(PhoneNumberValidator.isValid(phone),
                    phone + " should be valid");
        }

        @Test
        @DisplayName("Number with spaces is valid (spaces are stripped)")
        void numberWithSpaces() {
            assertTrue(PhoneNumberValidator.isValid("+63 917 123 4567"));
        }

        @Test
        @DisplayName("Number with dashes is valid (dashes are stripped)")
        void numberWithDashes() {
            assertTrue(PhoneNumberValidator.isValid("0917-123-4567"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Invalid numbers
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("isValid — Invalid Inputs")
    class InvalidNumberTests {

        @Test
        @DisplayName("Null → false")
        void nullIsInvalid() {
            assertFalse(PhoneNumberValidator.isValid(null));
        }

        @Test
        @DisplayName("Empty string → false")
        void emptyIsInvalid() {
            assertFalse(PhoneNumberValidator.isValid(""));
            assertFalse(PhoneNumberValidator.isValid("   "));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "12345",              // too short
                "+6391712345",        // +63 too short
                "+63917123456789",    // +63 too long
                "091712345",          // 09 too short
                "091712345678",       // 09 too long
                "+630171234567",      // doesn't start with 9 after +63
                "08171234567",        // starts with 08 not 09
                "abcdefghijk",        // letters
                "+1234567890123",     // non-PH international
        })
        @DisplayName("Various invalid formats")
        void invalidFormats(String phone) {
            assertFalse(PhoneNumberValidator.isValid(phone),
                    phone + " should be invalid");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // getErrorMessage
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getErrorMessage — Descriptive Error Messages")
    class ErrorMessageTests {

        @Test
        @DisplayName("Null → 'required' message")
        void nullMessage() {
            String msg = PhoneNumberValidator.getErrorMessage(null);
            assertTrue(msg.toLowerCase().contains("required"));
        }

        @Test
        @DisplayName("Empty → 'required' message")
        void emptyMessage() {
            String msg = PhoneNumberValidator.getErrorMessage("");
            assertTrue(msg.toLowerCase().contains("required"));
        }

        @Test
        @DisplayName("+63 wrong length → descriptive error")
        void wrongLengthInternational() {
            String msg = PhoneNumberValidator.getErrorMessage("+6391712345");
            assertTrue(msg.contains("13 digits"), "Should mention expected length");
        }

        @Test
        @DisplayName("Doesn't start with +63 or 09 → format hint")
        void wrongPrefix() {
            String msg = PhoneNumberValidator.getErrorMessage("12345678901");
            assertTrue(msg.contains("+63") || msg.contains("09"),
                    "Should hint correct format");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // normalize
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("normalize — Strip Spaces and Dashes")
    class NormalizeTests {

        @Test
        @DisplayName("Strips spaces")
        void stripsSpaces() {
            assertEquals("+639171234567", PhoneNumberValidator.normalize("+63 917 123 4567"));
        }

        @Test
        @DisplayName("Strips dashes")
        void stripsDashes() {
            assertEquals("09171234567", PhoneNumberValidator.normalize("0917-123-4567"));
        }

        @Test
        @DisplayName("Already clean → unchanged")
        void alreadyClean() {
            assertEquals("+639171234567", PhoneNumberValidator.normalize("+639171234567"));
        }

        @Test
        @DisplayName("Null → null")
        void nullInput() {
            assertNull(PhoneNumberValidator.normalize(null));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // toInternationalFormat
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("toInternationalFormat — Convert to +63 Format")
    class ToInternationalTests {

        @Test
        @DisplayName("09 → +63")
        void localToInternational() {
            assertEquals("+639171234567",
                    PhoneNumberValidator.toInternationalFormat("09171234567"));
        }

        @Test
        @DisplayName("Already +63 → unchanged")
        void alreadyInternational() {
            assertEquals("+639171234567",
                    PhoneNumberValidator.toInternationalFormat("+639171234567"));
        }

        @Test
        @DisplayName("Null → null")
        void nullInput() {
            assertNull(PhoneNumberValidator.toInternationalFormat(null));
        }

        @Test
        @DisplayName("Empty → null")
        void emptyInput() {
            assertNull(PhoneNumberValidator.toInternationalFormat(""));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // toLocalFormat
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("toLocalFormat — Convert to 09 Format")
    class ToLocalTests {

        @Test
        @DisplayName("+63 → 09")
        void internationalToLocal() {
            assertEquals("09171234567",
                    PhoneNumberValidator.toLocalFormat("+639171234567"));
        }

        @Test
        @DisplayName("Already 09 → unchanged")
        void alreadyLocal() {
            assertEquals("09171234567",
                    PhoneNumberValidator.toLocalFormat("09171234567"));
        }

        @Test
        @DisplayName("Null → null")
        void nullInput() {
            assertNull(PhoneNumberValidator.toLocalFormat(null));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Round-trip: local → international → local
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Round-trip: 09 → +63 → 09")
    void roundTrip() {
        String local = "09171234567";
        String international = PhoneNumberValidator.toInternationalFormat(local);
        String backToLocal = PhoneNumberValidator.toLocalFormat(international);

        assertEquals(local, backToLocal);
    }
}

