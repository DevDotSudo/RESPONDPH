package com.ionres.respondph.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PhoneNumberValidator")
class PhoneNumberValidatorTest {

    // ─── isValid ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isValid()")
    class IsValid {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("returns false for null, empty, and blank inputs")
        void returnsFalseForBlank(String input) {
            assertFalse(PhoneNumberValidator.isValid(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"+639123456789", "+63 9123456789", "+63-9123456789"})
        @DisplayName("accepts valid +63 format numbers")
        void acceptsValidInternationalFormat(String input) {
            assertTrue(PhoneNumberValidator.isValid(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"09123456789", "091 234 56789", "091-234-56789"})
        @DisplayName("accepts valid 09 local format numbers")
        void acceptsValidLocalFormat(String input) {
            assertTrue(PhoneNumberValidator.isValid(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "+6391234567",    // too short
                "+63912345678901", // too long
                "+630123456789",  // doesn't start with 9 after +63
                "08123456789",    // starts with 08
                "12345",          // random digits
                "abcdefghijk",    // letters
                "+1234567890"     // wrong country code
        })
        @DisplayName("rejects invalid numbers")
        void rejectsInvalidNumbers(String input) {
            assertFalse(PhoneNumberValidator.isValid(input));
        }
    }

    // ─── getErrorMessage ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getErrorMessage()")
    class GetErrorMessage {

        @Test
        @DisplayName("returns required message for null")
        void nullInput() {
            assertEquals("Mobile number is required", PhoneNumberValidator.getErrorMessage(null));
        }

        @Test
        @DisplayName("returns required message for empty string")
        void emptyInput() {
            assertEquals("Mobile number is required", PhoneNumberValidator.getErrorMessage(""));
        }

        @Test
        @DisplayName("returns length message for short +63 number")
        void shortInternational() {
            String msg = PhoneNumberValidator.getErrorMessage("+6391234567");
            assertTrue(msg.contains("13 digits"));
        }

        @Test
        @DisplayName("returns start-with-9 message for +630 prefix")
        void wrongDigitAfterPrefix() {
            String msg = PhoneNumberValidator.getErrorMessage("+630123456789");
            assertTrue(msg.contains("must start with 9"));
        }

        @Test
        @DisplayName("returns format message for unknown prefix")
        void unknownPrefix() {
            String msg = PhoneNumberValidator.getErrorMessage("12345678901");
            assertTrue(msg.contains("must start with +63 or 09"));
        }
    }

    // ─── normalize ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("normalize()")
    class Normalize {

        @Test
        @DisplayName("returns null for null input")
        void nullInput() {
            assertNull(PhoneNumberValidator.normalize(null));
        }

        @Test
        @DisplayName("removes spaces and dashes")
        void removesWhitespaceAndDashes() {
            assertEquals("+639123456789", PhoneNumberValidator.normalize("+63 912-345-6789"));
        }

        @Test
        @DisplayName("leaves clean number unchanged")
        void cleanNumber() {
            assertEquals("09123456789", PhoneNumberValidator.normalize("09123456789"));
        }
    }

    // ─── toInternationalFormat ────────────────────────────────────────────────

    @Nested
    @DisplayName("toInternationalFormat()")
    class ToInternationalFormat {

        @Test
        @DisplayName("returns null for null")
        void nullInput() {
            assertNull(PhoneNumberValidator.toInternationalFormat(null));
        }

        @Test
        @DisplayName("returns null for empty")
        void emptyInput() {
            assertNull(PhoneNumberValidator.toInternationalFormat(""));
        }

        @Test
        @DisplayName("keeps +63 format as-is")
        void alreadyInternational() {
            assertEquals("+639123456789",
                    PhoneNumberValidator.toInternationalFormat("+639123456789"));
        }

        @Test
        @DisplayName("converts 09 to +63 format")
        void convertsLocalToInternational() {
            assertEquals("+639123456789",
                    PhoneNumberValidator.toInternationalFormat("09123456789"));
        }

        @Test
        @DisplayName("returns null for unconvertible number")
        void unconvertible() {
            assertNull(PhoneNumberValidator.toInternationalFormat("12345"));
        }
    }

    // ─── toLocalFormat ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toLocalFormat()")
    class ToLocalFormat {

        @Test
        @DisplayName("returns null for null")
        void nullInput() {
            assertNull(PhoneNumberValidator.toLocalFormat(null));
        }

        @Test
        @DisplayName("returns null for empty")
        void emptyInput() {
            assertNull(PhoneNumberValidator.toLocalFormat(""));
        }

        @Test
        @DisplayName("keeps 09 format as-is")
        void alreadyLocal() {
            assertEquals("09123456789",
                    PhoneNumberValidator.toLocalFormat("09123456789"));
        }

        @Test
        @DisplayName("converts +63 to local format")
        void convertsInternationalToLocal() {
            assertEquals("09123456789",
                    PhoneNumberValidator.toLocalFormat("+639123456789"));
        }

        @Test
        @DisplayName("returns null for unconvertible number")
        void unconvertible() {
            assertNull(PhoneNumberValidator.toLocalFormat("12345"));
        }
    }
}

