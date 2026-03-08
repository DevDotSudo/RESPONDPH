package com.ionres.respondph.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cryptography")
class CryptographyTest {

    private Cryptography crypto;
    private static final String TEST_KEY;

    static {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128, new SecureRandom());
            TEST_KEY = Base64.getEncoder().encodeToString(keyGen.generateKey().getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test AES key", e);
        }
    }

    @BeforeEach
    void setUp() {
        crypto = new Cryptography(TEST_KEY);
    }

    // ─── encryptWithOneParameter / decryptWithOneParameter ────────────────────

    @Nested
    @DisplayName("Single-parameter encrypt/decrypt")
    class SingleParameter {

        @Test
        @DisplayName("encrypts and decrypts correctly")
        void roundTrip() throws Exception {
            String original = "Hello World";
            String encrypted = crypto.encryptWithOneParameter(original);
            String decrypted = crypto.decryptWithOneParameter(encrypted);
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("encrypted text contains IV:ciphertext separator")
        void formatCheck() throws Exception {
            String encrypted = crypto.encryptWithOneParameter("test");
            assertTrue(encrypted.contains(":"));
            String[] parts = encrypted.split(":");
            assertEquals(2, parts.length);
        }

        @Test
        @DisplayName("different encryptions of same text produce different ciphertexts")
        void differentIVs() throws Exception {
            String text = "same text";
            String enc1 = crypto.encryptWithOneParameter(text);
            String enc2 = crypto.encryptWithOneParameter(text);
            assertNotEquals(enc1, enc2, "Each encryption should use a unique IV");
        }

        @Test
        @DisplayName("handles empty string")
        void emptyString() throws Exception {
            String encrypted = crypto.encryptWithOneParameter("");
            String decrypted = crypto.decryptWithOneParameter(encrypted);
            assertEquals("", decrypted);
        }

        @Test
        @DisplayName("handles unicode characters")
        void unicode() throws Exception {
            String original = "こんにちは世界 🌍";
            String encrypted = crypto.encryptWithOneParameter(original);
            String decrypted = crypto.decryptWithOneParameter(encrypted);
            assertEquals(original, decrypted);
        }
    }

    // ─── encrypt / decrypt (list) ─────────────────────────────────────────────

    @Nested
    @DisplayName("Multi-field encrypt/decrypt")
    class MultiField {

        @Test
        @DisplayName("encrypts and decrypts list of admin fields")
        void roundTrip() throws Exception {
            String username = "admin1";
            String firstname = "John";
            String middlename = "M";
            String lastname = "Doe";
            String regDate = "2024-01-01";
            String role = "Admin";

            List<String> encrypted = crypto.encrypt(username, firstname, middlename, lastname, regDate, role);
            assertEquals(6, encrypted.size());

            List<String> decrypted = crypto.decrypt(encrypted);
            assertEquals(6, decrypted.size());
            assertEquals(username, decrypted.get(0));
            assertEquals(firstname, decrypted.get(1));
            assertEquals(middlename, decrypted.get(2));
            assertEquals(lastname, decrypted.get(3));
            assertEquals(regDate, decrypted.get(4));
            assertEquals(role, decrypted.get(5));
        }
    }

    // ─── encryptDouble / decryptDouble ────────────────────────────────────────

    @Nested
    @DisplayName("Double encrypt/decrypt")
    class DoubleValues {

        @Test
        @DisplayName("encrypts and decrypts positive double")
        void positiveDouble() throws Exception {
            double original = 3.14159;
            String encrypted = crypto.encryptDouble(original);
            double decrypted = crypto.decryptDouble(encrypted);
            assertEquals(original, decrypted, 0.00001);
        }

        @Test
        @DisplayName("encrypts and decrypts zero")
        void zeroValue() throws Exception {
            String encrypted = crypto.encryptDouble(0.0);
            double decrypted = crypto.decryptDouble(encrypted);
            assertEquals(0.0, decrypted, 0.0);
        }

        @Test
        @DisplayName("encrypts and decrypts negative double")
        void negativeDouble() throws Exception {
            double original = -42.5;
            String encrypted = crypto.encryptDouble(original);
            double decrypted = crypto.decryptDouble(encrypted);
            assertEquals(original, decrypted, 0.00001);
        }

        @Test
        @DisplayName("throws exception for invalid format (no colon)")
        void invalidFormat() {
            assertThrows(IllegalArgumentException.class,
                    () -> crypto.decryptDouble("invaliddata"));
        }
    }

    // ─── encryptId / decryptWithOneParameter ──────────────────────────────────

    @Nested
    @DisplayName("ID encrypt/decrypt")
    class IdEncryption {

        @Test
        @DisplayName("encrypts and decrypts an ID string")
        void roundTrip() throws Exception {
            String id = "12345";
            String encrypted = crypto.encryptId(id);
            String decrypted = crypto.decryptWithOneParameter(encrypted);
            assertEquals(id, decrypted);
        }
    }

    // ─── encryptUpdate ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("encryptUpdate()")
    class EncryptUpdate {

        @Test
        @DisplayName("encrypts 5 fields and decrypts correctly")
        void roundTrip() throws Exception {
            List<String> encrypted = crypto.encryptUpdate("user1", "Jane", "A", "Smith", "Secretary");
            assertEquals(5, encrypted.size());

            List<String> decrypted = crypto.decrypt(encrypted);
            assertEquals("user1", decrypted.get(0));
            assertEquals("Jane", decrypted.get(1));
            assertEquals("A", decrypted.get(2));
            assertEquals("Smith", decrypted.get(3));
            assertEquals("Secretary", decrypted.get(4));
        }
    }

    // ─── decrypt edge cases ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Decrypt edge cases")
    class DecryptEdgeCases {

        @Test
        @DisplayName("decrypt passes through values without colon separator")
        void noColonPassthrough() throws Exception {
            List<String> input = Arrays.asList("plaintext_no_colon");
            List<String> result = crypto.decrypt(input);
            assertEquals("plaintext_no_colon", result.get(0));
        }

        @Test
        @DisplayName("throws on invalid two-part format with bad base64")
        void invalidBase64() {
            List<String> input = Arrays.asList("not-valid-base64:also-invalid");
            assertThrows(Exception.class, () -> crypto.decrypt(input));
        }
    }

    // ─── Cross-key test ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Cross-key security")
    class CrossKey {

        @Test
        @DisplayName("decryption with different key fails")
        void differentKeyFails() throws Exception {
            String encrypted = crypto.encryptWithOneParameter("secret data");

            // Create a different crypto instance with a new key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128, new SecureRandom());
            String otherKey = Base64.getEncoder().encodeToString(keyGen.generateKey().getEncoded());
            Cryptography otherCrypto = new Cryptography(otherKey);

            assertThrows(Exception.class,
                    () -> otherCrypto.decryptWithOneParameter(encrypted));
        }
    }
}

