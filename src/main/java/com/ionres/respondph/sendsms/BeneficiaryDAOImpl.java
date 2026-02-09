package com.ionres.respondph.sendsms;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;

import java.sql.*;
import java.util.*;

public class BeneficiaryDAOImpl implements BeneficiaryDAO {

    private final Cryptography crypto = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

    @Override
    public List<BeneficiaryModel> getAllBeneficiaries() {
        List<BeneficiaryModel> beneficiaries = new ArrayList<>();
        String sql = "SELECT beneficiary_id, first_name, middle_name, last_name, " +
                "latitude, longitude, mobile_number, barangay FROM beneficiary " +
                "WHERE mobile_number IS NOT NULL AND mobile_number != ''";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                BeneficiaryModel beneficiary = mapResultSetToBeneficiary(rs);
                if (beneficiary != null) {
                    beneficiaries.add(beneficiary);
                }
            }

            System.out.println("DEBUG: Retrieved " + beneficiaries.size() + " beneficiaries with valid phone numbers");

        } catch (SQLException e) {
            System.err.println("Error retrieving beneficiaries: " + e.getMessage());
            e.printStackTrace();
        }

        return beneficiaries;
    }

    @Override
    public List<BeneficiaryModel> getBeneficiariesByBarangay(String barangay) {
        List<BeneficiaryModel> beneficiaries = new ArrayList<>();

        String barangayName = extractBarangayName(barangay);
        System.out.println("DEBUG: Searching for beneficiaries in barangay: " + barangayName);

        String sql = "SELECT beneficiary_id, first_name, middle_name, last_name, " +
                "latitude, longitude, mobile_number, barangay FROM beneficiary " +
                "WHERE mobile_number IS NOT NULL AND mobile_number != '' " +
                "AND barangay IS NOT NULL AND barangay != ''";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String encryptedBarangay = rs.getString("barangay");
                String decryptedBarangay = decryptBarangay(encryptedBarangay);

                // Check if decrypted barangay matches the requested one
                if (decryptedBarangay != null && decryptedBarangay.equals(barangayName)) {
                    BeneficiaryModel beneficiary = mapResultSetToBeneficiary(rs);
                    if (beneficiary != null) {
                        beneficiaries.add(beneficiary);
                    }
                }
            }

            System.out.println("DEBUG: Found " + beneficiaries.size() + " beneficiaries in " + barangayName);

        } catch (SQLException e) {
            System.err.println("Error retrieving beneficiaries by barangay: " + e.getMessage());
            e.printStackTrace();
        }

        return beneficiaries;
    }

    @Override
    public List<String> getAllBarangays() {
        Map<String, Integer> barangayCount = new LinkedHashMap<>();

        String sql = "SELECT barangay FROM beneficiary " +
                "WHERE barangay IS NOT NULL AND barangay != '' " +
                "AND mobile_number IS NOT NULL AND mobile_number != ''";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String encryptedBarangay = rs.getString("barangay");

                String barangay = decryptBarangay(encryptedBarangay);

                if (barangay != null && !barangay.trim().isEmpty()) {
                    barangayCount.put(barangay, barangayCount.getOrDefault(barangay, 0) + 1);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving barangays: " + e.getMessage());
            e.printStackTrace();
        }

        if (barangayCount.isEmpty()) {
            List<String> result = new ArrayList<>();
            result.add("No barangays found in database");
            return result;
        }

        List<String> barangays = new ArrayList<>();
        barangayCount.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry ->
                        barangays.add(entry.getKey() + " (" + entry.getValue() + " beneficiaries)")
                );

        System.out.println("DEBUG: Found " + barangays.size() + " unique barangays");
        return barangays;
    }


    private String decryptCoordinate(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isEmpty()) {
            return null;
        }

        if (!encryptedValue.contains(":")) {
            return encryptedValue;
        }

        try {
            if (crypto == null) {
                System.err.println("Cryptography instance is null - cannot decrypt coordinates");
                return null;
            }
            return crypto.decryptWithOneParameter(encryptedValue);
        } catch (Exception e) {
            System.err.println("Error decrypting coordinate: " + e.getMessage());
            return null;
        }
    }


    private String decryptBarangay(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isEmpty()) {
            return null;
        }

        if (!encryptedValue.contains(":")) {
            // Not encrypted, return as-is
            return encryptedValue;
        }

        try {
            if (crypto == null) {
                System.err.println("Cryptography instance is null - cannot decrypt barangay");
                return null;
            }
            return crypto.decryptWithOneParameter(encryptedValue);
        } catch (Exception e) {
            System.err.println("Error decrypting barangay: " + e.getMessage());
            return null;
        }
    }


    private boolean isValidCoordinate(String coordinate) {
        if (coordinate == null || coordinate.isEmpty()) {
            return false;
        }

        try {
            double value = Double.parseDouble(coordinate);

            if (coordinate.contains(".")) {
                if (value >= -90 && value <= 90) return true;
                // Otherwise assume longitude
                return value >= -180 && value <= 180;
            }

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    private String extractBarangayName(String barangay) {
        if (barangay == null) return null;

        int parenIndex = barangay.indexOf(" (");
        if (parenIndex > 0) {
            return barangay.substring(0, parenIndex);
        }
        return barangay;
    }


    private BeneficiaryModel mapResultSetToBeneficiary(ResultSet rs) throws SQLException {
        int beneficiaryId = rs.getInt("beneficiary_id");

        String encryptedLat = rs.getString("latitude");
        String encryptedLon = rs.getString("longitude");

        String lat = decryptCoordinate(encryptedLat);
        String lon = decryptCoordinate(encryptedLon);

        System.out.println("DEBUG: Processing beneficiary " + beneficiaryId);
        System.out.println("  - Lat: " + lat + " (from " + encryptedLat + ")");
        System.out.println("  - Lon: " + lon + " (from " + encryptedLon + ")");

        if (!isValidCoordinate(lat) || !isValidCoordinate(lon)) {
            System.err.println("Skipping beneficiary " + beneficiaryId +
                    " - invalid coordinates after decryption (lat=" + lat + ", lon=" + lon + ")");
            return null;
        }

        String firstName = decryptField(rs.getString("first_name"));
        String middleName = decryptField(rs.getString("middle_name"));
        String lastName = decryptField(rs.getString("last_name"));
        String mobileNumber = decryptField(rs.getString("mobile_number"));

        System.out.println("  - Name: " + firstName + " " + middleName + " " + lastName);
        System.out.println("  - Phone: " + mobileNumber);

        BeneficiaryModel beneficiary = new BeneficiaryModel();
        beneficiary.setId(beneficiaryId);
        beneficiary.setFirstname(firstName);
        beneficiary.setMiddlename(middleName);
        beneficiary.setLastname(lastName);
        beneficiary.setLatitude(lat);
        beneficiary.setLongitude(lon);
        beneficiary.setMobileNumber(mobileNumber);

        String encryptedBarangay = rs.getString("barangay");
        String barangay = decryptBarangay(encryptedBarangay);

        System.out.println("  - Barangay: " + barangay);

        if (barangay != null && !barangay.trim().isEmpty()) {
            try {
                beneficiary.setBarangay(barangay);
            } catch (Exception e) {
                System.err.println("Warning: setBarangay method not found in BeneficiaryModel");
            }
        }

        System.out.println("  - Beneficiary processed successfully");

        return beneficiary;
    }
    private String decryptField(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isEmpty()) {
            return null;
        }

        if (!encryptedValue.contains(":")) {
            return encryptedValue;
        }

        try {
            if (crypto == null) {
                System.err.println("Cryptography instance is null - cannot decrypt field");
                return null;
            }
            return crypto.decryptWithOneParameter(encryptedValue);
        } catch (Exception e) {
            System.err.println("Error decrypting field: " + e.getMessage());
            return encryptedValue; // Return original if decryption fails
        }
    }

    @Override
    public List<BeneficiaryModel> getBeneficiariesByDisaster(int disasterId) {
        List<BeneficiaryModel> beneficiaries = new ArrayList<>();

        System.out.println("DEBUG: Searching for beneficiaries affected by disaster ID: " + disasterId);

        String sql = "SELECT DISTINCT b.beneficiary_id, b.first_name, b.middle_name, b.last_name, " +
                "b.latitude, b.longitude, b.mobile_number, b.barangay " +
                "FROM beneficiary b " +
                "INNER JOIN beneficiary_disaster_damage bdd ON b.beneficiary_id = bdd.beneficiary_id " +
                "WHERE bdd.disaster_id = ? " +
                "AND b.mobile_number IS NOT NULL AND b.mobile_number != ''";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, disasterId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BeneficiaryModel beneficiary = mapResultSetToBeneficiary(rs);
                    if (beneficiary != null) {
                        beneficiaries.add(beneficiary);
                    }
                }
            }


            System.out.println("DEBUG: Found " + beneficiaries.size() + " beneficiaries affected by disaster " + disasterId);

        } catch (SQLException e) {
            System.err.println("Error retrieving beneficiaries by disaster: " + e.getMessage());
            e.printStackTrace();
        }

        return beneficiaries;
    }
}