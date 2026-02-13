package com.ionres.respondph.aid;

import com.ionres.respondph.aid.KMeansAidDistribution.BeneficiaryCluster;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;
import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
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
            // With disaster filtering
            sql = "SELECT ahs.beneficiary_id, ahs.final_score, ahs.score_category " +
                    "FROM aid_and_household_score ahs " +
                    "WHERE ahs.aid_type_id = ? " +
                    "AND ahs.disaster_id = ? " +
                    "AND NOT EXISTS (" +
                    "    SELECT 1 FROM aid a " +
                    "    WHERE a.beneficiary_id = ahs.beneficiary_id " +
                    "    AND a.aid_type_id = ? " +
                    "    AND (a.disaster_id = ? OR (? = 0 AND a.disaster_id IS NULL))" +
                    ") " +
                    "ORDER BY ahs.final_score DESC";
        } else {
            sql = "SELECT ahs.beneficiary_id, MAX(ahs.final_score) AS final_score, " +
                    "MAX(ahs.score_category) AS score_category " +
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
                ps.setInt(2, disasterId);
                ps.setInt(3, aidTypeId);
                ps.setInt(4, disasterId);
                ps.setInt(5, disasterId);
            } else {
                ps.setInt(1, aidTypeId);
                ps.setInt(2, aidTypeId);
            }

            System.out.println("========== FETCHING ELIGIBLE BENEFICIARIES ==========");
            System.out.println("Aid Type ID: " + aidTypeId);
            if (disasterId > 0) {
                System.out.println("Disaster ID: " + disasterId);
                System.out.println("Query: Beneficiaries who:");
                System.out.println("  ✓ Have DISASTER-SPECIFIC scores (ahs.disaster_id = " + disasterId + ")");
                System.out.println("  ✗ Have NOT already received this aid for this disaster");
            } else {
                System.out.println("Disaster: NONE (General Aid Distribution)");
                System.out.println("Query: Beneficiaries who:");
                System.out.println("  ✓ Have GENERAL scores (ahs.disaster_id IS NULL)");
                System.out.println("  ✗ Have NOT already received this general aid");
            }
            System.out.println("=====================================================");

            ResultSet rs = ps.executeQuery();

            int count = 0;
            while (rs.next()) {
                int beneficiaryId = rs.getInt("beneficiary_id");
                double finalScore = rs.getDouble("final_score");
                String scoreCategory = rs.getString("score_category");

                beneficiaries.add(new BeneficiaryCluster(beneficiaryId, finalScore, scoreCategory));
                count++;

                if (count <= 5) {
                    System.out.println("  " + count + ". Beneficiary #" + beneficiaryId +
                            " | Score: " + finalScore + " | Category: " + scoreCategory);
                }
            }

            if (count > 5) {
                System.out.println("  ... and " + (count - 5) + " more beneficiaries");
            }

            rs.close();
            ps.close();

            System.out.println("\n✓ Found " + beneficiaries.size() + " eligible beneficiaries");
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
    public List<String> getBarangaysByDisaster(int disasterId, int aidTypeId) {
        // LinkedHashSet automatically removes duplicate barangay names after decryption
        java.util.LinkedHashSet<String> barangaySet = new java.util.LinkedHashSet<>();

        String sql;
        if (disasterId > 0) {
            sql = "SELECT b.barangay " +
                    "FROM beneficiary b " +
                    "INNER JOIN beneficiary_disaster_damage bdd ON b.beneficiary_id = bdd.beneficiary_id " +
                    "INNER JOIN aid_and_household_score ahs ON b.beneficiary_id = ahs.beneficiary_id " +
                    "WHERE bdd.disaster_id = ? " +
                    "AND ahs.aid_type_id = ? " +
                    "AND ahs.disaster_id = ? " +
                    "AND b.barangay IS NOT NULL";
        } else {
            // General Aid: only barangays that have at least one beneficiary with ANY score
            sql = "SELECT b.barangay " +
                    "FROM beneficiary b " +
                    "INNER JOIN aid_and_household_score ahs ON b.beneficiary_id = ahs.beneficiary_id " +
                    "WHERE ahs.aid_type_id = ? " +
                    "AND b.barangay IS NOT NULL";
        }

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            if (disasterId > 0) {
                ps.setInt(1, disasterId);
                ps.setInt(2, aidTypeId);
                ps.setInt(3, disasterId);
            } else {
                ps.setInt(1, aidTypeId);
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String encryptedBarangay = rs.getString("barangay");
                if (encryptedBarangay != null && !encryptedBarangay.trim().isEmpty()) {
                    try {
                        String decryptedBarangay = cs.decryptWithOneParameter(encryptedBarangay);
                        if (decryptedBarangay != null && !decryptedBarangay.trim().isEmpty()) {
                            barangaySet.add(decryptedBarangay.trim()); // duplicates auto-ignored
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

        // Convert Set → sorted List
        List<String> barangays = new ArrayList<>(barangaySet);
        java.util.Collections.sort(barangays);

        System.out.println("Found " + barangays.size() +
                " unique barangays (after deduplication)" +
                (disasterId > 0 ? " for disaster #" + disasterId : " for general aid"));

        return barangays;
    }
    @Override
    public List<String> getAllBarangays() {
        java.util.LinkedHashSet<String> barangaySet = new java.util.LinkedHashSet<>();
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
        java.util.LinkedHashSet<String> barangaySet = new java.util.LinkedHashSet<>();

        // Fetch all beneficiary barangays that have already received this specific aid
        String sql;
        if (disasterId > 0) {
            sql = "SELECT b.barangay " +
                    "FROM beneficiary b " +
                    "INNER JOIN aid a ON b.beneficiary_id = a.beneficiary_id " +
                    "WHERE a.disaster_id = ? " +
                    "AND b.barangay IS NOT NULL";
        } else {
            // General aid — disaster_id IS NULL
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
                            barangaySet.add(decryptedBarangay.trim()); // duplicates ignored
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

    private void debugNoEligibleBeneficiaries(int aidTypeId, int disasterId) {
        try {
            Connection debugConn = dbConnection.getConnection();

            System.out.println("\n========== DEBUGGING: WHY NO ELIGIBLE BENEFICIARIES? ==========");

            // Check 1: Scores for this aid type
            String sql1 = disasterId > 0
                    ? "SELECT COUNT(*) as count, MIN(final_score) as min_score, MAX(final_score) as max_score " +
                    "FROM aid_and_household_score WHERE aid_type_id = ? AND disaster_id = ?"
                    : "SELECT COUNT(*) as count, MIN(final_score) as min_score, MAX(final_score) as max_score " +
                    "FROM aid_and_household_score WHERE aid_type_id = ? AND disaster_id IS NULL";

            PreparedStatement ps1 = debugConn.prepareStatement(sql1);
            ps1.setInt(1, aidTypeId);
            if (disasterId > 0) {
                ps1.setInt(2, disasterId);
            }

            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) {
                int scoreCount = rs1.getInt("count");
                String contextMsg = disasterId > 0
                        ? "for Aid Type #" + aidTypeId + " and Disaster #" + disasterId
                        : "for Aid Type #" + aidTypeId + " (General)";
                System.out.println("\n1. Beneficiaries with scores " + contextMsg + ": " + scoreCount);
                if (scoreCount > 0) {
                    System.out.println("   Score range: " + rs1.getDouble("min_score") + " to " + rs1.getDouble("max_score"));
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

            System.out.println("\n================================================================\n");

            debugConn.close();

        } catch (SQLException e) {
            System.err.println("Error in debug queries: " + e.getMessage());
        }
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

    @Override
    public List<BeneficiaryCluster> getBeneficiariesWithScoresByBarangays(
            int aidTypeId, int disasterId, List<String> barangays) {

        List<BeneficiaryCluster> beneficiaries = new ArrayList<>();

        if (barangays == null || barangays.isEmpty()) {
            return beneficiaries;
        }

        String sql;
        if (disasterId > 0) {
            sql = "SELECT ahs.beneficiary_id, ahs.final_score, ahs.score_category, b.barangay " +
                    "FROM aid_and_household_score ahs " +
                    "INNER JOIN beneficiary b ON ahs.beneficiary_id = b.beneficiary_id " +
                    "WHERE ahs.aid_type_id = ? " +
                    "AND ahs.disaster_id = ? " +
                    "AND NOT EXISTS (" +
                    "    SELECT 1 FROM aid a " +
                    "    WHERE a.beneficiary_id = ahs.beneficiary_id " +
                    "    AND a.aid_type_id = ? " +
                    "    AND a.disaster_id = ? " +
                    ") " +
                    "ORDER BY ahs.final_score DESC";
        } else {
            sql = "SELECT ahs.beneficiary_id, MAX(ahs.final_score) AS final_score, " +
                    "MAX(ahs.score_category) AS score_category, b.barangay " +
                    "FROM aid_and_household_score ahs " +
                    "INNER JOIN beneficiary b ON ahs.beneficiary_id = b.beneficiary_id " +
                    "WHERE ahs.aid_type_id = ? " +
                    // ← disaster_id filter REMOVED
                    "AND NOT EXISTS (" +
                    "    SELECT 1 FROM aid a " +
                    "    WHERE a.beneficiary_id = ahs.beneficiary_id " +
                    "    AND a.aid_type_id = ? " +
                    "    AND a.disaster_id IS NULL" +   // ← still blocks duplicate GENERAL aid
                    ") " +
                    "GROUP BY ahs.beneficiary_id, b.barangay " +
                    "ORDER BY final_score DESC";
        }

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            if (disasterId > 0) {
                ps.setInt(1, aidTypeId);
                ps.setInt(2, disasterId);
                ps.setInt(3, aidTypeId);
                ps.setInt(4, disasterId);
            } else {
                ps.setInt(1, aidTypeId);
                ps.setInt(2, aidTypeId);
            }

            System.out.println("========== FETCHING BARANGAY-FILTERED BENEFICIARIES ==========");
            System.out.println("Aid Type ID: " + aidTypeId);
            if (disasterId > 0) {
                System.out.println("Disaster ID: " + disasterId);
            } else {
                System.out.println("Disaster: NONE (General Aid)");
            }
            System.out.println("Barangays: " + String.join(", ", barangays));
            System.out.println("===============================================================");

            ResultSet rs = ps.executeQuery();

            int count = 0;
            while (rs.next()) {
                String encryptedBarangay = rs.getString("barangay");

                try {
                    String decryptedBarangay = cs.decryptWithOneParameter(encryptedBarangay);

                    // Check if this barangay is in our list
                    if (decryptedBarangay != null && barangays.contains(decryptedBarangay)) {
                        int beneficiaryId = rs.getInt("beneficiary_id");
                        double finalScore = rs.getDouble("final_score");
                        String scoreCategory = rs.getString("score_category");

                        beneficiaries.add(new BeneficiaryCluster(beneficiaryId, finalScore, scoreCategory));
                        count++;

                        if (count <= 5) {
                            System.out.println("  " + count + ". Beneficiary #" + beneficiaryId +
                                    " | Score: " + finalScore + " | Category: " + scoreCategory +
                                    " | Barangay: " + decryptedBarangay);
                        }
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
            System.out.println("===============================================================\n");

        } catch (Exception e) {
            System.err.println("Error fetching beneficiaries by barangays: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return beneficiaries;
    }



    @Override
    public List<String> getDistinctAidNames() {
        List<String> aidNames = new ArrayList<>();
        String sql = "SELECT DISTINCT name FROM aid WHERE name IS NOT NULL ORDER BY name";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String encryptedName = rs.getString("name");

                try {
                    // Decrypt the aid name
                    String aidName = cs.decryptWithOneParameter(encryptedName);
                    if (aidName != null && !aidName.trim().isEmpty()) {
                        aidNames.add(aidName);
                    }
                } catch (Exception e) {
                    System.err.println("Error decrypting aid name: " + e.getMessage());
                }
            }

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