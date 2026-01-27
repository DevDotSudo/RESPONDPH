package com.ionres.respondph.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class NameDecryptionUtils {
    private static final Logger LOGGER = Logger.getLogger(NameDecryptionUtils.class.getName());
    private static final Cryptography CRYPTO = CryptographyManager.getInstance();

    private NameDecryptionUtils() {}

    public static String decryptFullName(String encryptedFullName) {
        if (encryptedFullName == null || encryptedFullName.trim().isEmpty()) {
            return "";
        }

        try {
            String[] nameParts = encryptedFullName.split("\\|");
            String firstName = "";
            String middleName = "";
            String lastName = "";

            if (nameParts.length >= 1 && nameParts[0] != null && !nameParts[0].isEmpty()) {
                firstName = CRYPTO.decryptWithOneParameter(nameParts[0]);
            }

            if (nameParts.length >= 2 && nameParts[1] != null && !nameParts[1].isEmpty()) {
                middleName = CRYPTO.decryptWithOneParameter(nameParts[1]);
            }

            if (nameParts.length >= 3 && nameParts[2] != null && !nameParts[2].isEmpty()) {
                lastName = CRYPTO.decryptWithOneParameter(nameParts[2]);
            }

            String fullName = firstName.trim();
            if (!middleName.trim().isEmpty()) {
                fullName += " " + middleName.trim();
            }
            fullName += " " + lastName.trim();
            return fullName.trim();

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error decrypting full name", e);
            return "";
        }
    }
}
