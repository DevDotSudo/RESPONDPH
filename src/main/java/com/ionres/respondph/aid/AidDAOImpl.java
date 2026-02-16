package com.ionres.respondph.aid;

import com.ionres.respondph.aid.KMeansAidDistribution.BeneficiaryCluster;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.util.GeographicUtils;
import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;


public class AidDAOImpl implements AidDAO {

    private final DBConnection dbConnection;
    private final Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");
    private Connection conn;

    public AidDAOImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public boolean saving(AidModel aid) {
        String sql = "INSERT INTO aid (beneficiary_id, disaster_id, name, date, qty, cost, provider, aid_type_id, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            String encryptedName = cs.encryptWithOneParameter(aid.getName());
            String encryptedProvider = cs.encryptWithOneParameter(aid.getProvider());

            ps.setInt(1, aid.getBeneficiaryId());

            if (aid.getDisasterId() > 0) {
                ps.setInt(2, aid.getDisasterId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }

            ps.setString(3, encryptedName);
            ps.setDate(4, Date.valueOf(aid.getDate()));
            ps.setDouble(5, aid.getQuantity());
            ps.setDouble(6, aid.getCost());
            ps.setString(7, encryptedProvider);
            ps.setInt(8, aid.getAidTypeId());
            ps.setString(9, aid.getNotes());

            int rowsAffected = ps.executeUpdate();
            ps.close();

            if (rowsAffected > 0) {
                String disasterInfo = aid.getDisasterId() > 0 ? " for disaster #" + aid.getDisasterId() : " (General Distribution)";
                System.out.println("Aid saved for beneficiary #" + aid.getBeneficiaryId() + disasterInfo);
            }

            return rowsAffected > 0;

        } catch (Exception e) {
            System.err.println("Error saving aid: " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Database error occurred: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        } finally {
            closeConnection();
        }
    }

    @Override
    public List<AidModel> getAll() {
        List<AidModel> aidList = new ArrayList<>();
        String sql = "SELECT * FROM aid ORDER BY date DESC";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                AidModel aid = mapResultSetToAidModel(rs);
                aidList.add(aid);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            System.err.println("Error fetching all aid records: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return aidList;
    }

    @Override
    public boolean delete(AidModel aid) {
        String sql = "DELETE FROM aid WHERE aid_id = ?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, aid.getAidId());

            int rowsAffected = ps.executeUpdate();
            ps.close();

            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting aid: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            closeConnection();
        }
    }

    @Override
    public boolean update(AidModel aid) {
        String sql = "UPDATE aid SET name=?, date=?, qty=?, cost=?, provider=?, notes=? WHERE aid_id=?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            String encryptedName = cs.encryptWithOneParameter(aid.getName());
            String encryptedProvider = cs.encryptWithOneParameter(aid.getProvider());

            ps.setString(1, encryptedName);
            ps.setDate(2, Date.valueOf(aid.getDate()));
            ps.setDouble(3, aid.getQuantity());
            ps.setDouble(4, aid.getCost());
            ps.setString(5, encryptedProvider);
            ps.setString(6, aid.getNotes());
            ps.setInt(7, aid.getAidId());

            int rowsAffected = ps.executeUpdate();
            ps.close();

            return rowsAffected > 0;

        } catch (Exception e) {
            System.err.println("Error updating aid: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            closeConnection();
        }
    }

    @Override
    public List<BeneficiaryCluster> getBeneficiariesWithScores(int aidTypeId, int disasterId) {
        List<BeneficiaryCluster> beneficiaries = new ArrayList<>();

        String sql;
        if (disasterId > 0) {
            // Get ALL beneficiaries with scores (including general scores) AND their coordinates
            // We'll filter by disaster geography in memory
            sql = "SELECT ahs.beneficiary_id, ahs.final_score, ahs.score_category, " +
                    "       b.latitude, b.longitude " +
                    "FROM aid_and_household_score ahs " +
                    "INNER JOIN beneficiary b ON ahs.beneficiary_id = b.beneficiary_id " +
                    "WHERE ahs.aid_type_id = ? " +
                    "AND b.latitude IS NOT NULL AND b.longitude IS NOT NULL " +
                    "AND b.latitude != '' AND b.longitude != '' " +
                    "AND NOT EXISTS (" +
                    "    SELECT 1 FROM aid a " +
                    "    WHERE a.beneficiary_id = ahs.beneficiary_id " +
                    "    AND a.aid_type_id = ? " +
                    "    AND a.disaster_id = ?" +
                    ") " +
                    "ORDER BY ahs.final_score DESC";
        } else {
            // General aid - no geographic filtering needed
            sql = "SELECT ahs.beneficiary_id, MAX(ahs.final_score) AS final_score, " +
                    "       MAX(ahs.score_category) AS score_category " +
                    "FROM aid_and_household_score ahs " +
                    "WHERE ahs.aid_type_id = ? " +
                    "AND NOT EXISTS (" +
                    "    SELECT 1 FROM aid a " +
                    "    WHERE a.beneficiary_id = ahs.beneficiary_id " +
                    "    AND a.aid_type_id = ? " +
                    "    AND a.disaster_id IS NULL" +
                    ") " +
                    "GROUP BY ahs.beneficiary_id " +
                    "ORDER BY final_score DESC";
        }

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            if (disasterId > 0) {
                ps.setInt(1, aidTypeId);
                ps.setInt(2, aidTypeId);
                ps.setInt(3, disasterId);
            } else {
                ps.setInt(1, aidTypeId);
                ps.setInt(2, aidTypeId);
            }

            System.out.println("========== FETCHING ELIGIBLE BENEFICIARIES ==========");
            System.out.println("Aid Type ID: " + aidTypeId);
            if (disasterId > 0) {
                System.out.println("Disaster ID: " + disasterId);
                System.out.println("Query: Beneficiaries who:");
                System.out.println("  ✓ Have ANY scores for this aid type (disaster-specific OR general)");
                System.out.println("  ✓ Have VALID COORDINATES (will be filtered by disaster circle)");
                System.out.println("  ✗ Have NOT already received this aid for THIS DISASTER");
            } else {
                System.out.println("Disaster: NONE (General Aid Distribution)");
                System.out.println("Query: Beneficiaries who:");
                System.out.println("  ✓ Have GENERAL scores (ahs.disaster_id IS NULL)");
                System.out.println("  ✗ Have NOT already received this general aid");
            }
            System.out.println("=====================================================");

            ResultSet rs = ps.executeQuery();

            // If disaster-specific, get disaster circle for geographic filtering
            DisasterCircle disasterCircle = null;
            if (disasterId > 0) {
                disasterCircle = getDisasterCircle(disasterId);
                if (disasterCircle != null) {
                    System.out.println("\n📍 Disaster Circle Retrieved:");
                    System.out.println("   Center: (" + disasterCircle.lat + ", " + disasterCircle.lon + ")");
                    System.out.println("   Radius: " + disasterCircle.radius + " meters");
                } else {
                    System.out.println("\n⚠ WARNING: No disaster circle found for disaster #" + disasterId);
                    System.out.println("   All beneficiaries with scores will be included (no geographic filter)");
                }
            }

            int count = 0;
            int filteredOut = 0;

            while (rs.next()) {
                int beneficiaryId = rs.getInt("beneficiary_id");
                double finalScore = rs.getDouble("final_score");
                String scoreCategory = rs.getString("score_category");

                // For disaster-specific aid, check if beneficiary is inside disaster circle
                if (disasterId > 0 && disasterCircle != null) {
                    String encryptedLat = rs.getString("latitude");
                    String encryptedLon = rs.getString("longitude");

                    try {
                        String latStr = cs.decryptWithOneParameter(encryptedLat);
                        String lonStr = cs.decryptWithOneParameter(encryptedLon);

                        double beneficiaryLat = Double.parseDouble(latStr);
                        double beneficiaryLon = Double.parseDouble(lonStr);

                        // Calculate distance from disaster center
                        double distance = GeographicUtils.calculateDistance(
                                beneficiaryLat, beneficiaryLon,
                                disasterCircle.lat, disasterCircle.lon
                        );

                        // Only include if within disaster radius
                        if (distance <= disasterCircle.radius) {
                            beneficiaries.add(new BeneficiaryCluster(beneficiaryId, finalScore, scoreCategory));
                            count++;

                            if (count <= 5) {
                                System.out.println("  ✓ " + count + ". Beneficiary #" + beneficiaryId +
                                        " | Score: " + finalScore + " | Category: " + scoreCategory +
                                        " | Distance: " + String.format("%.0f", distance) + "m");
                            }
                        } else {
                            filteredOut++;
                            if (filteredOut <= 3) {
                                System.out.println("  ✗ Beneficiary #" + beneficiaryId +
                                        " OUTSIDE disaster area (distance: " +
                                        String.format("%.0f", distance) + "m > " +
                                        String.format("%.0f", disasterCircle.radius) + "m)");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("  ⚠ Error processing coordinates for beneficiary #" +
                                beneficiaryId + ": " + e.getMessage());
                    }
                } else {
                    // General aid or no disaster circle - include all
                    beneficiaries.add(new BeneficiaryCluster(beneficiaryId, finalScore, scoreCategory));
                    count++;

                    if (count <= 5) {
                        System.out.println("  " + count + ". Beneficiary #" + beneficiaryId +
                                " | Score: " + finalScore + " | Category: " + scoreCategory);
                    }
                }
            }

            if (count > 5) {
                System.out.println("  ... and " + (count - 5) + " more beneficiaries");
            }

            if (filteredOut > 3) {
                System.out.println("  ... and " + (filteredOut - 3) + " more beneficiaries filtered out");
            }

            rs.close();
            ps.close();

            if (disasterId > 0 && disasterCircle != null) {
                System.out.println("\n✓ Found " + beneficiaries.size() +
                        " eligible beneficiaries INSIDE disaster area");
                System.out.println("✗ Filtered out " + filteredOut +
                        " beneficiaries OUTSIDE disaster area");
            } else {
                System.out.println("\n✓ Found " + beneficiaries.size() + " eligible beneficiaries");
            }
            System.out.println("=====================================================\n");

            if (beneficiaries.isEmpty()) {
                System.out.println("⚠ WARNING: No eligible beneficiaries found!");
                debugNoEligibleBeneficiaries(aidTypeId, disasterId);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching beneficiaries with scores: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return beneficiaries;
    }

    @Override
    public List<BeneficiaryCluster> getBeneficiariesWithScoresByBarangays(
            int aidTypeId, int disasterId, List<String> barangays) {

        List<BeneficiaryCluster> beneficiaries = new ArrayList<>();

        if (barangays == null || barangays.isEmpty()) {
            return beneficiaries;
        }

        String sql;
        if (disasterId > 0) {
            // Get ALL beneficiaries with scores (including general scores) AND their coordinates
            // Filter by disaster geography in memory
            sql = "SELECT ahs.beneficiary_id, ahs.final_score, ahs.score_category, " +
                    "       b.barangay, b.latitude, b.longitude " +
                    "FROM aid_and_household_score ahs " +
                    "INNER JOIN beneficiary b ON ahs.beneficiary_id = b.beneficiary_id " +
                    "WHERE ahs.aid_type_id = ? " +
                    "AND b.latitude IS NOT NULL AND b.longitude IS NOT NULL " +
                    "AND b.latitude != '' AND b.longitude != '' " +
                    "AND NOT EXISTS (" +
                    "    SELECT 1 FROM aid a " +
                    "    WHERE a.beneficiary_id = ahs.beneficiary_id " +
                    "    AND a.aid_type_id = ? " +
                    "    AND a.disaster_id = ? " +
                    ") " +
                    "ORDER BY ahs.final_score DESC";
        } else {
            sql = "SELECT ahs.beneficiary_id, MAX(ahs.final_score) AS final_score, " +
                    "       MAX(ahs.score_category) AS score_category, b.barangay " +
                    "FROM aid_and_household_score ahs " +
                    "INNER JOIN beneficiary b ON ahs.beneficiary_id = b.beneficiary_id " +
                    "WHERE ahs.aid_type_id = ? " +
                    "AND NOT EXISTS (" +
                    "    SELECT 1 FROM aid a " +
                    "    WHERE a.beneficiary_id = ahs.beneficiary_id " +
                    "    AND a.aid_type_id = ? " +
                    "    AND a.disaster_id IS NULL" +
                    ") " +
                    "GROUP BY ahs.beneficiary_id, b.barangay " +
                    "ORDER BY final_score DESC";
        }

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            if (disasterId > 0) {
                ps.setInt(1, aidTypeId);
                ps.setInt(2, aidTypeId);
                ps.setInt(3, disasterId);
            } else {
                ps.setInt(1, aidTypeId);
                ps.setInt(2, aidTypeId);
            }

            System.out.println("========== FETCHING BARANGAY-FILTERED BENEFICIARIES ==========");
            System.out.println("Aid Type ID: " + aidTypeId);
            if (disasterId > 0) {
                System.out.println("Disaster ID: " + disasterId);
                System.out.println("Using ANY scores (disaster-specific OR general) + geographic filtering");
            } else {
                System.out.println("Disaster: NONE (General Aid)");
            }
            System.out.println("Barangays: " + String.join(", ", barangays));
            System.out.println("===============================================================");

            // Get disaster circle for geographic filtering
            DisasterCircle disasterCircle = null;
            if (disasterId > 0) {
                disasterCircle = getDisasterCircle(disasterId);
                if (disasterCircle != null) {
                    System.out.println("\n📍 Disaster Circle: (" + disasterCircle.lat + ", " +
                            disasterCircle.lon + ") radius " + disasterCircle.radius + "m");
                }
            }

            ResultSet rs = ps.executeQuery();

            int count = 0;
            int filteredByBarangay = 0;
            int filteredByGeography = 0;

            while (rs.next()) {
                String encryptedBarangay = rs.getString("barangay");

                try {
                    String decryptedBarangay = cs.decryptWithOneParameter(encryptedBarangay);

                    // Check barangay filter
                    if (decryptedBarangay != null && barangays.contains(decryptedBarangay)) {
                        int beneficiaryId = rs.getInt("beneficiary_id");
                        double finalScore = rs.getDouble("final_score");
                        String scoreCategory = rs.getString("score_category");

                        // For disaster-specific aid, check geographic proximity
                        boolean includeThisBeneficiary = true;

                        if (disasterId > 0 && disasterCircle != null) {
                            String encryptedLat = rs.getString("latitude");
                            String encryptedLon = rs.getString("longitude");

                            try {
                                String latStr = cs.decryptWithOneParameter(encryptedLat);
                                String lonStr = cs.decryptWithOneParameter(encryptedLon);

                                double beneficiaryLat = Double.parseDouble(latStr);
                                double beneficiaryLon = Double.parseDouble(lonStr);

                                double distance = GeographicUtils.calculateDistance(
                                        beneficiaryLat, beneficiaryLon,
                                        disasterCircle.lat, disasterCircle.lon
                                );

                                if (distance > disasterCircle.radius) {
                                    includeThisBeneficiary = false;
                                    filteredByGeography++;
                                }
                            } catch (Exception e) {
                                System.err.println("  ⚠ Error checking coordinates for beneficiary #" +
                                        beneficiaryId);
                            }
                        }

                        if (includeThisBeneficiary) {
                            beneficiaries.add(new BeneficiaryCluster(beneficiaryId, finalScore, scoreCategory));
                            count++;

                            if (count <= 5) {
                                System.out.println("  " + count + ". Beneficiary #" + beneficiaryId +
                                        " | Score: " + finalScore + " | Category: " + scoreCategory +
                                        " | Barangay: " + decryptedBarangay);
                            }
                        }
                    } else {
                        filteredByBarangay++;
                    }
                } catch (Exception e) {
                    System.err.println("Error decrypting barangay: " + e.getMessage());
                }
            }

            if (count > 5) {
                System.out.println("  ... and " + (count - 5) + " more beneficiaries");
            }

            rs.close();
            ps.close();

            System.out.println("\n✓ Found " + beneficiaries.size() +
                    " eligible beneficiaries across " + barangays.size() + " barangay(s)");
            if (disasterId > 0 && disasterCircle != null) {
                System.out.println("✗ Filtered out " + filteredByGeography +
                        " beneficiaries OUTSIDE disaster area");
            }
            System.out.println("===============================================================\n");

        } catch (Exception e) {
            System.err.println("Error fetching beneficiaries by barangays: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return beneficiaries;
    }

    /**
     * Helper method to get disaster circle coordinates and radius.
     */
    private DisasterCircle getDisasterCircle(int disasterId) {
        String sql = "SELECT lat, `long`, radius FROM disaster WHERE disaster_id = ? " +
                "AND lat IS NOT NULL AND `long` IS NOT NULL AND radius IS NOT NULL";

        try {
            Connection tempConn = dbConnection.getConnection();
            PreparedStatement ps = tempConn.prepareStatement(sql);
            ps.setInt(1, disasterId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String encryptedLat = rs.getString("lat");
                String encryptedLon = rs.getString("long");
                String encryptedRadius = rs.getString("radius");

                try {
                    String latStr = cs.decryptWithOneParameter(encryptedLat);
                    String lonStr = cs.decryptWithOneParameter(encryptedLon);
                    String radiusStr = cs.decryptWithOneParameter(encryptedRadius);

                    double lat = Double.parseDouble(latStr);
                    double lon = Double.parseDouble(lonStr);
                    double radius = Double.parseDouble(radiusStr);

                    rs.close();
                    ps.close();
                    tempConn.close();

                    return new DisasterCircle(lat, lon, radius);
                } catch (Exception e) {
                    System.err.println("Error decrypting disaster coordinates: " + e.getMessage());
                }
            }

            rs.close();
            ps.close();
            tempConn.close();
        } catch (SQLException e) {
            System.err.println("Error fetching disaster circle: " + e.getMessage());
        }

        return null;
    }

    /**
     * Inner class to hold disaster circle data.
     */
    private static class DisasterCircle {
        final double lat;
        final double lon;
        final double radius;

        DisasterCircle(double lat, double lon, double radius) {
            this.lat = lat;
            this.lon = lon;
            this.radius = radius;
        }
    }

    @Override
    public List<String> getBarangaysByDisaster(int disasterId, int aidTypeId) {
        LinkedHashSet<String> barangaySet = new LinkedHashSet<>();

        String sql;
        if (disasterId > 0) {
            // Get ALL beneficiaries with scores (not just disaster-specific)
            // We'll filter by geography in memory
            sql = "SELECT b.barangay, b.latitude, b.longitude " +
                    "FROM beneficiary b " +
                    "INNER JOIN aid_and_household_score ahs ON b.beneficiary_id = ahs.beneficiary_id " +
                    "WHERE ahs.aid_type_id = ? " +
                    "AND b.barangay IS NOT NULL " +
                    "AND b.latitude IS NOT NULL AND b.longitude IS NOT NULL " +
                    "AND b.latitude != '' AND b.longitude != ''";
        } else {
            // General aid - no geographic filtering
            sql = "SELECT b.barangay " +
                    "FROM beneficiary b " +
                    "INNER JOIN aid_and_household_score ahs ON b.beneficiary_id = ahs.beneficiary_id " +
                    "WHERE ahs.aid_type_id = ? " +
                    "AND b.barangay IS NOT NULL";
        }

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, aidTypeId);

            ResultSet rs = ps.executeQuery();

            // Get disaster circle for geographic filtering
            DisasterCircle disasterCircle = null;
            if (disasterId > 0) {
                disasterCircle = getDisasterCircle(disasterId);
            }

            while (rs.next()) {
                String encryptedBarangay = rs.getString("barangay");
                if (encryptedBarangay != null && !encryptedBarangay.trim().isEmpty()) {
                    try {
                        String decryptedBarangay = cs.decryptWithOneParameter(encryptedBarangay);
                        if (decryptedBarangay != null && !decryptedBarangay.trim().isEmpty()) {

                            // For disaster-specific, check if beneficiary is inside disaster circle
                            boolean includeBarangay = true;
                            if (disasterId > 0 && disasterCircle != null) {
                                String encryptedLat = rs.getString("latitude");
                                String encryptedLon = rs.getString("longitude");

                                try {
                                    String latStr = cs.decryptWithOneParameter(encryptedLat);
                                    String lonStr = cs.decryptWithOneParameter(encryptedLon);

                                    double beneficiaryLat = Double.parseDouble(latStr);
                                    double beneficiaryLon = Double.parseDouble(lonStr);

                                    double distance = GeographicUtils.calculateDistance(
                                            beneficiaryLat, beneficiaryLon,
                                            disasterCircle.lat, disasterCircle.lon
                                    );

                                    // Only include barangay if at least one beneficiary is inside
                                    includeBarangay = (distance <= disasterCircle.radius);
                                } catch (Exception e) {
                                    // If coordinates are invalid, skip this beneficiary
                                    includeBarangay = false;
                                }
                            }

                            if (includeBarangay) {
                                barangaySet.add(decryptedBarangay.trim());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error decrypting barangay: " + e.getMessage());
                    }
                }
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.err.println("Error fetching barangays: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        List<String> barangays = new ArrayList<>(barangaySet);
        java.util.Collections.sort(barangays);

        System.out.println("Found " + barangays.size() +
                " unique barangays (after deduplication)" +
                (disasterId > 0 ? " for disaster #" + disasterId : " for general aid"));

        return barangays;
    }

    @Override
    public List<String> getAllBarangays() {
        LinkedHashSet<String> barangaySet = new LinkedHashSet<>();
        String sql = "SELECT barangay FROM beneficiary WHERE barangay IS NOT NULL";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String encryptedBarangay = rs.getString("barangay");
                if (encryptedBarangay != null && !encryptedBarangay.trim().isEmpty()) {
                    try {
                        String decryptedBarangay = cs.decryptWithOneParameter(encryptedBarangay);
                        if (decryptedBarangay != null && !decryptedBarangay.trim().isEmpty()) {
                            barangaySet.add(decryptedBarangay.trim());
                        }
                    } catch (Exception e) {
                        System.err.println("Error decrypting barangay: " + e.getMessage());
                    }
                }
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.err.println("Error fetching all barangays: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        List<String> barangays = new ArrayList<>(barangaySet);
        java.util.Collections.sort(barangays);

        System.out.println("Found " + barangays.size() + " unique barangays (after deduplication)");
        return barangays;
    }

    @Override
    public List<String> getBarangaysByAidNameAndDisaster(int disasterId, String aidName) {
        LinkedHashSet<String> barangaySet = new LinkedHashSet<>();

        String sql;
        if (disasterId > 0) {
            sql = "SELECT b.barangay " +
                    "FROM beneficiary b " +
                    "INNER JOIN aid a ON b.beneficiary_id = a.beneficiary_id " +
                    "WHERE a.disaster_id = ? " +
                    "AND b.barangay IS NOT NULL";
        } else {
            sql = "SELECT b.barangay " +
                    "FROM beneficiary b " +
                    "INNER JOIN aid a ON b.beneficiary_id = a.beneficiary_id " +
                    "WHERE a.disaster_id IS NULL " +
                    "AND b.barangay IS NOT NULL";
        }

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            if (disasterId > 0) {
                ps.setInt(1, disasterId);
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String encryptedBarangay = rs.getString("barangay");
                if (encryptedBarangay != null && !encryptedBarangay.trim().isEmpty()) {
                    try {
                        String decryptedBarangay = cs.decryptWithOneParameter(encryptedBarangay);
                        if (decryptedBarangay != null && !decryptedBarangay.trim().isEmpty()) {
                            barangaySet.add(decryptedBarangay.trim());
                        }
                    } catch (Exception e) {
                        System.err.println("Error decrypting barangay: " + e.getMessage());
                    }
                }
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.err.println("Error fetching barangays by aid name: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        List<String> barangays = new ArrayList<>(barangaySet);
        java.util.Collections.sort(barangays);

        System.out.println("Found " + barangays.size() +
                " unique barangays (after deduplication) for aid: " + aidName);

        return barangays;
    }

    @Override
    public boolean hasReceivedAid(int beneficiaryId, int aidTypeId, int disasterId) {
        String sql;
        if (disasterId > 0) {
            sql = "SELECT COUNT(*) FROM aid WHERE beneficiary_id = ? AND aid_type_id = ? AND disaster_id = ?";
        } else {
            sql = "SELECT COUNT(*) FROM aid WHERE beneficiary_id = ? AND aid_type_id = ? AND disaster_id IS NULL";
        }

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ps.setInt(2, aidTypeId);
            if (disasterId > 0) {
                ps.setInt(3, disasterId);
            }

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1);
                rs.close();
                ps.close();
                return count > 0;
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.err.println("Error checking if beneficiary received aid: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return false;
    }

    @Override
    public List<AidModel> getAidByBeneficiary(int beneficiaryId) {
        List<AidModel> aidList = new ArrayList<>();
        String sql = "SELECT * FROM aid WHERE beneficiary_id = ? ORDER BY date DESC";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                AidModel aid = mapResultSetToAidModel(rs);
                aidList.add(aid);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            System.err.println("Error fetching aid by beneficiary: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return aidList;
    }

    @Override
    public List<AidModel> getAidByDisaster(int disasterId) {
        List<AidModel> aidList = new ArrayList<>();
        String sql;

        if (disasterId > 0) {
            sql = "SELECT * FROM aid WHERE disaster_id = ? ORDER BY date DESC";
        } else {
            sql = "SELECT * FROM aid WHERE disaster_id IS NULL ORDER BY date DESC";
        }

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            if (disasterId > 0) {
                ps.setInt(1, disasterId);
            }
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                AidModel aid = mapResultSetToAidModel(rs);
                aidList.add(aid);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            System.err.println("Error fetching aid by disaster: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return aidList;
    }

    @Override
    public List<AidModel> getAidByType(int aidTypeId) {
        List<AidModel> aidList = new ArrayList<>();
        String sql = "SELECT * FROM aid WHERE aid_type_id = ? ORDER BY date DESC";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, aidTypeId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                AidModel aid = mapResultSetToAidModel(rs);
                aidList.add(aid);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            System.err.println("Error fetching aid by type: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return aidList;
    }

    @Override
    public double getTotalQuantityDistributed(int aidTypeId, int disasterId) {
        String sql;
        if (disasterId > 0) {
            sql = "SELECT COALESCE(SUM(qty), 0) as total FROM aid WHERE aid_type_id = ? AND disaster_id = ?";
        } else {
            sql = "SELECT COALESCE(SUM(qty), 0) as total FROM aid WHERE aid_type_id = ? AND disaster_id IS NULL";
        }

        double total = 0.0;

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, aidTypeId);
            if (disasterId > 0) {
                ps.setInt(2, disasterId);
            }

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                total = rs.getDouble("total");
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.err.println("Error getting total quantity distributed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return total;
    }

    @Override
    public List<AidModel> getAllAidForTable() {
        List<AidModel> aidList = new ArrayList<>();

        String sql = "SELECT a.aid_id, a.name as aid_name, a.date, a.qty, a.cost, " +
                "a.provider, a.notes, a.beneficiary_id, a.disaster_id, a.aid_type_id, " +
                "b.first_name, b.middle_name, b.last_name, " +
                "d.name as disaster_name " +
                "FROM aid a " +
                "INNER JOIN beneficiary b ON a.beneficiary_id = b.beneficiary_id " +
                "LEFT JOIN disaster d ON a.disaster_id = d.disaster_id " +
                "ORDER BY a.date DESC";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String encryptedAidName = rs.getString("aid_name");
                String encryptedProvider = rs.getString("provider");
                String encryptedFirstName = rs.getString("first_name");
                String encryptedMiddleName = rs.getString("middle_name");
                String encryptedLastName = rs.getString("last_name");
                String encryptedDisasterName = rs.getString("disaster_name");

                String aidName = cs.decryptWithOneParameter(encryptedAidName);
                String provider = cs.decryptWithOneParameter(encryptedProvider);
                String firstName = cs.decryptWithOneParameter(encryptedFirstName);
                String middleName = cs.decryptWithOneParameter(encryptedMiddleName);
                String lastName = cs.decryptWithOneParameter(encryptedLastName);
                String disasterName = encryptedDisasterName != null
                        ? cs.decryptWithOneParameter(encryptedDisasterName)
                        : "General Aid (No Disaster)";

                String beneficiaryName = firstName + " " +
                        (middleName != null && !middleName.isEmpty() ? middleName + " " : "") +
                        lastName;
                beneficiaryName = beneficiaryName.trim();

                AidModel aid = new AidModel();
                aid.setAidId(rs.getInt("aid_id"));
                aid.setBeneficiaryId(rs.getInt("beneficiary_id"));

                int disasterId = rs.getInt("disaster_id");
                aid.setDisasterId(rs.wasNull() ? 0 : disasterId);

                aid.setName(aidName);
                aid.setDate(rs.getDate("date").toLocalDate());
                aid.setQuantity(rs.getDouble("qty"));
                aid.setCost(rs.getDouble("cost"));
                aid.setProvider(provider);
                aid.setAidTypeId(rs.getInt("aid_type_id"));
                aid.setNotes(rs.getString("notes"));

                aid.setBeneficiaryName(beneficiaryName);
                aid.setDisasterName(disasterName);

                aidList.add(aid);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            System.err.println("Error fetching aid table data: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return aidList;
    }

    @Override
    public List<String> getDistinctAidNames() {
        List<String> aidNames = new ArrayList<>();

        String sql = "SELECT name FROM aid WHERE name IS NOT NULL";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            LinkedHashSet<String> seen = new LinkedHashSet<>();

            while (rs.next()) {
                String encryptedName = rs.getString("name");
                try {
                    String aidName = cs.decryptWithOneParameter(encryptedName);
                    if (aidName != null && !aidName.trim().isEmpty()) {
                        seen.add(aidName.trim());
                    }
                } catch (Exception e) {
                    System.err.println("Error decrypting aid name: " + e.getMessage());
                }
            }

            aidNames.addAll(seen);

            rs.close();
            ps.close();

            System.out.println("DEBUG: Found " + aidNames.size() + " distinct aid names");

        } catch (SQLException e) {
            System.err.println("Error fetching distinct aid names: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return aidNames;
    }

    private void debugNoEligibleBeneficiaries(int aidTypeId, int disasterId) {
        try {
            Connection debugConn = dbConnection.getConnection();

            System.out.println("\n========== DEBUGGING: WHY NO ELIGIBLE BENEFICIARIES? ==========");

            // Check 1: Scores for this aid type (ANY scores, not just disaster-specific)
            String sql1 = "SELECT COUNT(*) as count, MIN(final_score) as min_score, MAX(final_score) as max_score " +
                    "FROM aid_and_household_score WHERE aid_type_id = ?";

            PreparedStatement ps1 = debugConn.prepareStatement(sql1);
            ps1.setInt(1, aidTypeId);

            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) {
                int scoreCount = rs1.getInt("count");
                System.out.println("\n1. Beneficiaries with scores for Aid Type #" + aidTypeId +
                        " (ANY disaster or general): " + scoreCount);
                if (scoreCount > 0) {
                    System.out.println("   Score range: " + rs1.getDouble("min_score") + " to " + rs1.getDouble("max_score"));
                    if (disasterId > 0) {
                        System.out.println("   ℹ For disaster-specific aid, we use these scores + geographic filtering");
                    }
                } else {
                    System.out.println("   ⚠ NO SCORES FOUND! Need to calculate aid household scores first.");
                }
            }
            rs1.close();
            ps1.close();

            // Check 2: Already received aid
            String sql2 = disasterId > 0
                    ? "SELECT COUNT(DISTINCT beneficiary_id) as count FROM aid WHERE aid_type_id = ? AND disaster_id = ?"
                    : "SELECT COUNT(DISTINCT beneficiary_id) as count FROM aid WHERE aid_type_id = ? AND disaster_id IS NULL";

            PreparedStatement ps2 = debugConn.prepareStatement(sql2);
            ps2.setInt(1, aidTypeId);
            if (disasterId > 0) {
                ps2.setInt(2, disasterId);
            }

            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) {
                int alreadyReceivedCount = rs2.getInt("count");
                String contextMsg = disasterId > 0
                        ? "this aid for this disaster"
                        : "this general aid";
                System.out.println("\n2. Beneficiaries who ALREADY RECEIVED " + contextMsg + ": " + alreadyReceivedCount);
                if (alreadyReceivedCount > 0) {
                    System.out.println("   ℹ These beneficiaries are excluded from distribution.");
                }
            }
            rs2.close();
            ps2.close();

            // Check 3: Disaster circle info
            if (disasterId > 0) {
                DisasterCircle circle = getDisasterCircle(disasterId);
                if (circle == null) {
                    System.out.println("\n3. ⚠ NO DISASTER CIRCLE FOUND for disaster #" + disasterId);
                    System.out.println("   Without disaster coordinates, all beneficiaries are included.");
                } else {
                    System.out.println("\n3. Disaster Circle: (" + circle.lat + ", " + circle.lon +
                            ") with radius " + circle.radius + "m");
                    System.out.println("   Beneficiaries outside this area are excluded.");
                }
            }

            System.out.println("\n================================================================\n");

            debugConn.close();

        } catch (SQLException e) {
            System.err.println("Error in debug queries: " + e.getMessage());
        }
    }

    private AidModel mapResultSetToAidModel(ResultSet rs) throws Exception {
        AidModel aid = new AidModel();
        aid.setAidId(rs.getInt("aid_id"));
        aid.setBeneficiaryId(rs.getInt("beneficiary_id"));

        int disasterId = rs.getInt("disaster_id");
        aid.setDisasterId(rs.wasNull() ? 0 : disasterId);

        String encryptedName = rs.getString("name");
        String encryptedProvider = rs.getString("provider");

        aid.setName(cs.decryptWithOneParameter(encryptedName));
        aid.setDate(rs.getDate("date").toLocalDate());
        aid.setQuantity(rs.getDouble("qty"));
        aid.setCost(rs.getDouble("cost"));
        aid.setProvider(cs.decryptWithOneParameter(encryptedProvider));
        aid.setAidTypeId(rs.getInt("aid_type_id"));
        aid.setNotes(rs.getString("notes"));

        return aid;
    }

    private void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}