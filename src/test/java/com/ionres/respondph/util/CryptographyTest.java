package com.ionres.respondph.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.KeyGenerator;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the AES-128-GCM Cryptography engine.
 * Tests encryption/decryption symmetry, IV uniqueness, edge cases,
 * and data integrity for all supported data types.
 */
@DisplayName("Cryptography — AES-128-GCM Encryption Engine")
class CryptographyTest {

    private Cryptography crypto;

    @BeforeEach
    void setUp() throws Exception {
        // Generate a fresh AES-128 key for test isolation
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        byte[] keyBytes = keyGen.generateKey().getEncoded();
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        crypto = new Cryptography(base64Key);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // encryptWithOneParameter / decryptWithOneParameter — Symmetry Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Single-Parameter Encrypt/Decrypt")
    class SingleParameterTests {

        @Test
        @DisplayName("Encrypt then decrypt returns original plaintext")
        void encryptDecryptRoundTrip() throws Exception {
            String plaintext = "Juan Dela Cruz";
            String encrypted = crypto.encryptWithOneParameter(plaintext);
            String decrypted = crypto.decryptWithOneParameter(encrypted);

            assertEquals(plaintext, decrypted);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Hello",
                "María Santos",
                "09171234567",
                "+639171234567",
                "Barangay 1, Banate, Iloilo",
                "None",
                "Physical",
                "Chronically Ill",
                "Solo Parent (without Support)",
                "₱12,500.00",
                ""  // empty string
        })
        @DisplayName("Round-trip works for various real-world inputs")
        void encryptDecryptVariousInputs(String input) throws Exception {
            String encrypted = crypto.encryptWithOneParameter(input);
            String decrypted = crypto.decryptWithOneParameter(encrypted);

            assertEquals(input, decrypted,
                    "Round-trip failed for input: \"" + input + "\"");
        }

        @Test
        @DisplayName("Encrypted output contains IV:Ciphertext format")
        void encryptedFormatIsValid() throws Exception {
            String encrypted = crypto.encryptWithOneParameter("test");

            assertTrue(encrypted.contains(":"),
                    "Encrypted output must contain ':' separator between IV and ciphertext");

            String[] parts = encrypted.split(":");
            assertEquals(2, parts.length,
                    "Encrypted output must have exactly 2 parts (IV:Ciphertext)");

            // IV should be 12 bytes → 16 Base64 characters
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            assertEquals(12, iv.length, "IV must be 12 bytes");
        }

        @Test
        @DisplayName("Two encryptions of the same plaintext produce different ciphertexts (random IV)")
        void ivUniquenessGuaranteesDistinctCiphertexts() throws Exception {
            String plaintext = "Identical Input";
            Set<String> ciphertexts = new HashSet<>();

            for (int i = 0; i < 50; i++) {
                ciphertexts.add(crypto.encryptWithOneParameter(plaintext));
            }

            assertEquals(50, ciphertexts.size(),
                    "50 encryptions of the same plaintext should produce 50 unique ciphertexts");
        }

        @Test
        @DisplayName("Unicode characters survive round-trip")
        void unicodeRoundTrip() throws Exception {
            String unicode = "こんにちは 🌏 Banate — Hiligaynon";
            String encrypted = crypto.encryptWithOneParameter(unicode);
            String decrypted = crypto.decryptWithOneParameter(encrypted);

            assertEquals(unicode, decrypted);
        }

        @Test
        @DisplayName("Long text survives round-trip")
        void longTextRoundTrip() throws Exception {
            String longText = "A".repeat(10_000);
            String encrypted = crypto.encryptWithOneParameter(longText);
            String decrypted = crypto.decryptWithOneParameter(encrypted);

            assertEquals(longText, decrypted);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // encryptDouble / decryptDouble
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Double Encrypt/Decrypt")
    class DoubleEncryptionTests {

        @Test
        @DisplayName("Round-trip for positive double")
        void positiveDouble() throws Exception {
            double value = 0.867;
            String encrypted = crypto.encryptDouble(value);
            double decrypted = crypto.decryptDouble(encrypted);

            assertEquals(value, decrypted, 1e-15);
        }

        @Test
        @DisplayName("Round-trip for zero")
        void zeroDouble() throws Exception {
            double value = 0.0;
            String encrypted = crypto.encryptDouble(value);
            double decrypted = crypto.decryptDouble(encrypted);

            assertEquals(value, decrypted, 1e-15);
        }

        @Test
        @DisplayName("Round-trip for negative double")
        void negativeDouble() throws Exception {
            double value = -3.14159;
            String encrypted = crypto.encryptDouble(value);
            double decrypted = crypto.decryptDouble(encrypted);

            assertEquals(value, decrypted, 1e-15);
        }

        @Test
        @DisplayName("Round-trip for very small double")
        void verySmallDouble() throws Exception {
            double value = 1e-10;
            String encrypted = crypto.encryptDouble(value);
            double decrypted = crypto.decryptDouble(encrypted);

            assertEquals(value, decrypted, 1e-25);
        }

        @Test
        @DisplayName("Round-trip for 1.0 (max vulnerability score)")
        void maxVulnerabilityScore() throws Exception {
            double value = 1.0;
            String encrypted = crypto.encryptDouble(value);
            double decrypted = crypto.decryptDouble(encrypted);

            assertEquals(value, decrypted, 1e-15);
        }

        @Test
        @DisplayName("Invalid format throws IllegalArgumentException")
        void invalidFormatThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> crypto.decryptDouble("noColonHere"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Batch encrypt / decrypt
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Batch Encrypt/Decrypt (Admin fields)")
    class BatchEncryptionTests {

        @Test
        @DisplayName("encrypt() returns 5 encrypted elements for admin fields")
        void batchEncryptReturnsFiveElements() throws Exception {
            List<String> encrypted = crypto.encrypt(
                    "admin_user", "Juan", "Reyes", "Dela Cruz", "2025-01-15");

            assertEquals(5, encrypted.size());
            for (String enc : encrypted) {
                assertTrue(enc.contains(":"), "Each element must be in IV:Ciphertext format");
            }
        }

        @Test
        @DisplayName("Batch encrypt then decrypt recovers all original values")
        void batchRoundTrip() throws Exception {
            String username = "admin_user";
            String firstname = "Juan";
            String middlename = "Reyes";
            String lastname = "Dela Cruz";
            String regDate = "2025-01-15";

            List<String> encrypted = crypto.encrypt(username, firstname, middlename, lastname, regDate);
            List<String> decrypted = crypto.decrypt(encrypted);

            assertEquals(5, decrypted.size());
            assertEquals(username, decrypted.get(0));
            assertEquals(firstname, decrypted.get(1));
            assertEquals(middlename, decrypted.get(2));
            assertEquals(lastname, decrypted.get(3));
            assertEquals(regDate, decrypted.get(4));
        }

        @Test
        @DisplayName("encryptUpdate() returns 4 elements for update operations")
        void updateEncryptReturnsFourElements() throws Exception {
            List<String> encrypted = crypto.encryptUpdate(
                    "new_user", "Maria", "Santos", "Cruz");

            assertEquals(4, encrypted.size());
        }

        @Test
        @DisplayName("encryptUpdate then decrypt round-trip")
        void updateRoundTrip() throws Exception {
            List<String> encrypted = crypto.encryptUpdate("new_user", "Maria", "Santos", "Cruz");
            List<String> decrypted = crypto.decrypt(encrypted);

            assertEquals("new_user", decrypted.get(0));
            assertEquals("Maria", decrypted.get(1));
            assertEquals("Santos", decrypted.get(2));
            assertEquals("Cruz", decrypted.get(3));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // encryptId / decryptWithOneParameter
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ID Encryption")
    class IdEncryptionTests {

        @Test
        @DisplayName("encryptId round-trip for numeric ID")
        void numericId() throws Exception {
            String id = "12345";
            String encrypted = crypto.encryptId(id);
            String decrypted = crypto.decryptWithOneParameter(encrypted);

            assertEquals(id, decrypted);
        }

        @Test
        @DisplayName("encryptId round-trip for UUID-like ID")
        void uuidLikeId() throws Exception {
            String id = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
            String encrypted = crypto.encryptId(id);
            String decrypted = crypto.decryptWithOneParameter(encrypted);

            assertEquals(id, decrypted);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Tamper Detection (GCM authentication tag)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GCM Tamper Detection")
    class TamperDetectionTests {

        @Test
        @DisplayName("Tampered ciphertext throws exception on decryption")
        void tamperedCiphertextThrows() throws Exception {
            String encrypted = crypto.encryptWithOneParameter("sensitive data");
            String[] parts = encrypted.split(":");

            // Flip a byte in the ciphertext
            byte[] ciphertextBytes = Base64.getDecoder().decode(parts[1]);
            ciphertextBytes[0] ^= 0xFF;  // flip all bits of first byte
            String tampered = parts[0] + ":" + Base64.getEncoder().encodeToString(ciphertextBytes);

            assertThrows(Exception.class,
                    () -> crypto.decryptWithOneParameter(tampered),
                    "Tampered ciphertext must fail GCM authentication");
        }

        @Test
        @DisplayName("Tampered IV throws exception on decryption")
        void tamperedIvThrows() throws Exception {
            String encrypted = crypto.encryptWithOneParameter("sensitive data");
            String[] parts = encrypted.split(":");

            // Flip a byte in the IV
            byte[] ivBytes = Base64.getDecoder().decode(parts[0]);
            ivBytes[0] ^= 0xFF;
            String tampered = Base64.getEncoder().encodeToString(ivBytes) + ":" + parts[1];

            assertThrows(Exception.class,
                    () -> crypto.decryptWithOneParameter(tampered),
                    "Tampered IV must fail GCM authentication");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Cross-instance decryption
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Decryption with wrong key throws exception")
    void wrongKeyThrowsException() throws Exception {
        String encrypted = crypto.encryptWithOneParameter("secret");

        // Create a different key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        byte[] wrongKeyBytes = keyGen.generateKey().getEncoded();
        String wrongBase64Key = Base64.getEncoder().encodeToString(wrongKeyBytes);
        Cryptography wrongCrypto = new Cryptography(wrongBase64Key);

        assertThrows(Exception.class,
                () -> wrongCrypto.decryptWithOneParameter(encrypted),
                "Decrypting with a different key must fail");
    }

    @Test
    @DisplayName("Same key in different Cryptography instance can decrypt")
    void sameKeyCrossInstance() throws Exception {
        // Get the key used by 'crypto' — we re-generate and share the same key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        byte[] sharedKey = keyGen.generateKey().getEncoded();
        String base64Key = Base64.getEncoder().encodeToString(sharedKey);

        Cryptography instance1 = new Cryptography(base64Key);
        Cryptography instance2 = new Cryptography(base64Key);

        String encrypted = instance1.encryptWithOneParameter("cross-instance test");
        String decrypted = instance2.decryptWithOneParameter(encrypted);

        assertEquals("cross-instance test", decrypted);
    }
}

