package com.ionres.respondph.util;

import com.ionres.respondph.util.Cryptography;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages a shared Cryptography instance to avoid creating multiple instances.
 * This improves performance and reduces memory usage.
 */
public final class CryptographyManager {
    private static final Logger LOGGER = Logger.getLogger(CryptographyManager.class.getName());
    private static Cryptography instance;
    private static final Object lock = new Object();

    private CryptographyManager() {}

    /**
     * Gets the shared Cryptography instance.
     * The instance is lazily initialized on first access.
     */
    public static Cryptography getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    try {
                        String secretKey = ConfigLoader.get("secretKey");
                        if (secretKey == null || secretKey.trim().isEmpty()) {
                            LOGGER.severe("Secret key not found in configuration");
                            throw new RuntimeException("Secret key not configured");
                        }
                        instance = new Cryptography(secretKey);
                        LOGGER.info("Cryptography instance initialized");
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Failed to initialize Cryptography", e);
                        throw new RuntimeException("Failed to initialize Cryptography", e);
                    }
                }
            }
        }
        return instance;
    }

    /**
     * Resets the instance (useful for testing).
     */
    static void reset() {
        synchronized (lock) {
            instance = null;
        }
    }
}
