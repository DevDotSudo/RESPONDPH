package com.ionres.respondph.util;



import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Cryptography {
    private static final int AES_KEY_SIZE = 128;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_SIZE = 12;
    private final SecretKey key;


    public Cryptography(String base64Key) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        this.key = new SecretKeySpec(decodedKey, "AES");
    }


    public List<String> encrypt(String username, String firstname, String middlename,
                                String lastname, String regDate) throws Exception {

        String[] inputs = {username, firstname, middlename, lastname, regDate};

        List<String> encryptedList = new ArrayList<>();

        for (String input : inputs) {
            byte[] iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] ciphertext = cipher.doFinal(input.getBytes());

            String combined = Base64.getEncoder().encodeToString(iv) + ":"
                    + Base64.getEncoder().encodeToString(ciphertext);
            encryptedList.add(combined);
        }

        return encryptedList;
    }


    public List<String> decrypt(List<String> encryptedList) throws Exception {
        List<String> decryptedList = new ArrayList<>();

        for (String combined : encryptedList) {

            if (!combined.contains(":")) {
                decryptedList.add(combined);
                continue;
            }

            String[] parts = combined.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid encrypted format: " + combined);
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] decrypted = cipher.doFinal(ciphertext);
            decryptedList.add(new String(decrypted));
        }
        return decryptedList;
    }

    public String encryptId(String id) throws Exception {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] ciphertext = cipher.doFinal(id.getBytes());

        return Base64.getEncoder().encodeToString(iv) + ":"
                + Base64.getEncoder().encodeToString(ciphertext);
    }

    public String decryptId(String encryptedId) throws Exception {
        String[] parts = encryptedId.split(":");
        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] ciphertext = Base64.getDecoder().decode(parts[1]);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] decrypted = cipher.doFinal(ciphertext);
        return new String(decrypted);
    }

    public List<String> encryptUpdate(String id, String fullname, String address,
                                      String username) throws Exception {

        String[] inputs = {id, fullname, address, username};
        List<String> encryptedList = new ArrayList<>();

        for (String input : inputs) {
            byte[] iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] ciphertext = cipher.doFinal(input.getBytes());

            String combined = Base64.getEncoder().encodeToString(iv) + ":"
                    + Base64.getEncoder().encodeToString(ciphertext);
            encryptedList.add(combined);
        }

        return encryptedList;
    }
}
