package com.ionres.respondph.aid;

import com.ionres.respondph.aid.KMeansAidDistribution.BeneficiaryCluster;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;

import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class AidDAOServiceImpl implements AidDAO {

    private final DBConnection dbConnection;
    private final Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");
    private Connection conn;

    public AidDAOServiceImpl(DBConnection dbConnection) {
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
            ps.setInt(2, aid.getDisasterId());
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
                System.out.println("Aid saved for beneficiary #" + aid.getBeneficiaryId());
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

        // ✅ IMPROVED QUERY: Explicitly checks that beneficiary hasn't received THIS SPECIFIC AID TYPE for THIS SPECIFIC DISASTER
        String sql = "SELECT ahs.beneficiary_id, ahs.final_score, ahs.score_category " +
                "FROM aid_and_household_score ahs " +
                "INNER JOIN beneficiary_disaster_damage bdd ON ahs.beneficiary_id = bdd.beneficiary_id " +
                "WHERE ahs.aid_type_id = ? " +              // Match this aid type
                "AND bdd.disaster_id = ? " +                 // Match this disaster
                "AND NOT EXISTS (" +                         // ✅ Check if they already received THIS aid type
                "    SELECT 1 FROM aid a " +
                "    WHERE a.beneficiary_id = ahs.beneficiary_id " +
                "    AND a.aid_type_id = ? " +               // Same aid type
                "    AND a.disaster_id = ? " +               // Same disaster
                ") " +
                "ORDER BY ahs.final_score DESC";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, aidTypeId);   // aid_and_household_score filter
            ps.setInt(2, disasterId);  // disaster filter
            ps.setInt(3, aidTypeId);   // NOT EXISTS - aid type check
            ps.setInt(4, disasterId);  // NOT EXISTS - disaster check

            System.out.println("========== FETCHING ELIGIBLE BENEFICIARIES ==========");
            System.out.println("Aid Type ID: " + aidTypeId);
            System.out.println("Disaster ID: " + disasterId);
            System.out.println("Query: Beneficiaries who:");
            System.out.println("  ✓ Have scores for this aid type");
            System.out.println("  ✓ Are affected by this disaster");
            System.out.println("  ✗ Have NOT already received this aid type for this disaster");
            System.out.println("=====================================================");

            ResultSet rs = ps.executeQuery();

            int count = 0;
            while (rs.next()) {
                int beneficiaryId = rs.getInt("beneficiary_id");
                double finalScore = rs.getDouble("final_score");
                String scoreCategory = rs.getString("score_category");

                beneficiaries.add(new BeneficiaryCluster(beneficiaryId, finalScore, scoreCategory));
                count++;

                if (count <= 5) {  // Log first 5 for debugging
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

            // If no eligible beneficiaries found, provide detailed debugging
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

    /**
     * Detailed debugging when no eligible beneficiaries are found
     */
    private void debugNoEligibleBeneficiaries(int aidTypeId, int disasterId) {
        try {
            Connection debugConn = dbConnection.getConnection();

            System.out.println("\n========== DEBUGGING: WHY NO ELIGIBLE BENEFICIARIES? ==========");

            // Check 1: Are there scores for this aid type?
            String sql1 = "SELECT COUNT(*) as count, " +
                    "MIN(final_score) as min_score, " +
                    "MAX(final_score) as max_score " +
                    "FROM aid_and_household_score WHERE aid_type_id = ?";
            PreparedStatement ps1 = debugConn.prepareStatement(sql1);
            ps1.setInt(1, aidTypeId);
            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) {
                int scoreCount = rs1.getInt("count");
                System.out.println("\n1. Beneficiaries with scores for Aid Type #" + aidTypeId + ": " + scoreCount);
                if (scoreCount > 0) {
                    System.out.println("   Score range: " + rs1.getDouble("min_score") + " to " + rs1.getDouble("max_score"));
                } else {
                    System.out.println("   ⚠ NO SCORES FOUND! Need to calculate aid household scores first.");
                }
            }
            rs1.close();
            ps1.close();

            // Check 2: Are there disaster damage records?
            String sql2 = "SELECT COUNT(*) as count FROM beneficiary_disaster_damage WHERE disaster_id = ?";
            PreparedStatement ps2 = debugConn.prepareStatement(sql2);
            ps2.setInt(1, disasterId);
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) {
                int damageCount = rs2.getInt("count");
                System.out.println("\n2. Beneficiaries affected by Disaster #" + disasterId + ": " + damageCount);
                if (damageCount == 0) {
                    System.out.println("   ⚠ NO DISASTER DAMAGE RECORDS! Need to record disaster damage first.");
                }
            }
            rs2.close();
            ps2.close();

            // Check 3: Intersection - beneficiaries in BOTH tables
            String sql3 = "SELECT COUNT(DISTINCT ahs.beneficiary_id) as count " +
                    "FROM aid_and_household_score ahs " +
                    "INNER JOIN beneficiary_disaster_damage bdd ON ahs.beneficiary_id = bdd.beneficiary_id " +
                    "WHERE ahs.aid_type_id = ? AND bdd.disaster_id = ?";
            PreparedStatement ps3 = debugConn.prepareStatement(sql3);
            ps3.setInt(1, aidTypeId);
            ps3.setInt(2, disasterId);
            ResultSet rs3 = ps3.executeQuery();
            if (rs3.next()) {
                int intersectionCount = rs3.getInt("count");
                System.out.println("\n3. Beneficiaries matching BOTH conditions: " + intersectionCount);
                if (intersectionCount == 0) {
                    System.out.println("   ⚠ No overlap! Check if scored beneficiaries are affected by this disaster.");
                }
            }
            rs3.close();
            ps3.close();

            // Check 4: How many already received this aid?
            String sql4 = "SELECT COUNT(DISTINCT beneficiary_id) as count " +
                    "FROM aid WHERE aid_type_id = ? AND disaster_id = ?";
            PreparedStatement ps4 = debugConn.prepareStatement(sql4);
            ps4.setInt(1, aidTypeId);
            ps4.setInt(2, disasterId);
            ResultSet rs4 = ps4.executeQuery();
            if (rs4.next()) {
                int alreadyReceivedCount = rs4.getInt("count");
                System.out.println("\n4. Beneficiaries who ALREADY RECEIVED this aid: " + alreadyReceivedCount);
                if (alreadyReceivedCount > 0) {
                    System.out.println("   ℹ These beneficiaries are excluded from distribution.");

                    // Show WHO already received it
                    String sql5 = "SELECT DISTINCT beneficiary_id FROM aid " +
                            "WHERE aid_type_id = ? AND disaster_id = ? LIMIT 10";
                    PreparedStatement ps5 = debugConn.prepareStatement(sql5);
                    ps5.setInt(1, aidTypeId);
                    ps5.setInt(2, disasterId);
                    ResultSet rs5 = ps5.executeQuery();
                    System.out.println("   Sample beneficiaries who already received this aid:");
                    int sampleCount = 0;
                    while (rs5.next() && sampleCount < 10) {
                        System.out.println("     - Beneficiary #" + rs5.getInt("beneficiary_id"));
                        sampleCount++;
                    }
                    rs5.close();
                    ps5.close();
                }
            }
            rs4.close();
            ps4.close();

            System.out.println("\n================================================================\n");

            debugConn.close();

        } catch (SQLException e) {
            System.err.println("Error in debug queries: " + e.getMessage());
        }
    }

//    @Override
//    public List<BeneficiaryCluster> getBeneficiariesWithScores(int aidTypeId, int disasterId) {
//        List<BeneficiaryCluster> beneficiaries = new ArrayList<>();
//
//        String sql = "SELECT ahs.beneficiary_id, ahs.final_score, ahs.score_category " +
//                "FROM aid_and_household_score ahs " +
//                "INNER JOIN beneficiary_disaster_damage bdd ON ahs.beneficiary_id = bdd.beneficiary_id " +
//                "WHERE ahs.aid_type_id = ? " +
//                "AND bdd.disaster_id = ? " +
//                "AND ahs.beneficiary_id NOT IN (" +
//                "    SELECT beneficiary_id FROM aid WHERE aid_type_id = ? AND disaster_id = ?" +
//                ") " +
//                "ORDER BY ahs.final_score DESC";
//
//        try {
//            conn = dbConnection.getConnection();
//            PreparedStatement ps = conn.prepareStatement(sql);
//            ps.setInt(1, aidTypeId);
//            ps.setInt(2, disasterId);
//            ps.setInt(3, aidTypeId);
//            ps.setInt(4, disasterId);
//
//            System.out.println("=== DEBUG: Executing Query ===");
//            System.out.println("Query: " + sql);
//            System.out.println("Parameters: aidTypeId=" + aidTypeId + ", disasterId=" + disasterId);
//
//            ResultSet rs = ps.executeQuery();
//
//            boolean hasRows = false;
//            while (rs.next()) {
//                hasRows = true;
//                int beneficiaryId = rs.getInt("beneficiary_id");
//                double finalScore = rs.getDouble("final_score");
//                String scoreCategory = rs.getString("score_category");
//
//                System.out.println("Found beneficiary: ID=" + beneficiaryId +
//                        ", Score=" + finalScore +
//                        ", Category=" + scoreCategory);
//
//                beneficiaries.add(new BeneficiaryCluster(beneficiaryId, finalScore, scoreCategory));
//            }
//
//            if (!hasRows) {
//                System.out.println("DEBUG: No rows returned from query!");
//
//                debugCheckConditions(aidTypeId, disasterId);
//            }
//
//            rs.close();
//            ps.close();
//
//            System.out.println("Found " + beneficiaries.size() +
//                    " eligible beneficiaries for aid type #" + aidTypeId +
//                    " and disaster #" + disasterId);
//
//        } catch (SQLException e) {
//            System.err.println("Error fetching beneficiaries with scores: " + e.getMessage());
//            e.printStackTrace();
//        } finally {
//            closeConnection();
//        }
//
//        return beneficiaries;
//    }
//
//
//    private void debugCheckConditions(int aidTypeId, int disasterId) {
//        try {
//            Connection debugConn = dbConnection.getConnection();
//
//            String sql1 = "SELECT COUNT(*) as count FROM aid_and_household_score WHERE aid_type_id = ?";
//            PreparedStatement ps1 = debugConn.prepareStatement(sql1);
//            ps1.setInt(1, aidTypeId);
//            ResultSet rs1 = ps1.executeQuery();
//            if (rs1.next()) {
//                System.out.println("DEBUG: Scores for aid type " + aidTypeId + ": " + rs1.getInt("count"));
//            }
//            rs1.close();
//            ps1.close();
//
//            String sql2 = "SELECT COUNT(*) as count FROM beneficiary_disaster_damage WHERE disaster_id = ?";
//            PreparedStatement ps2 = debugConn.prepareStatement(sql2);
//            ps2.setInt(1, disasterId);
//            ResultSet rs2 = ps2.executeQuery();
//            if (rs2.next()) {
//                System.out.println("DEBUG: Disaster damages for disaster " + disasterId + ": " + rs2.getInt("count"));
//            }
//            rs2.close();
//            ps2.close();
//
//            // Check 3: Are there beneficiaries in both tables?
//            String sql3 = "SELECT ahs.beneficiary_id " +
//                    "FROM aid_and_household_score ahs " +
//                    "INNER JOIN beneficiary_disaster_damage bdd ON ahs.beneficiary_id = bdd.beneficiary_id " +
//                    "WHERE ahs.aid_type_id = ? AND bdd.disaster_id = ?";
//            PreparedStatement ps3 = debugConn.prepareStatement(sql3);
//            ps3.setInt(1, aidTypeId);
//            ps3.setInt(2, disasterId);
//            ResultSet rs3 = ps3.executeQuery();
//            int joinCount = 0;
//            while (rs3.next()) {
//                joinCount++;
//                System.out.println("DEBUG: Beneficiary in both tables: " + rs3.getInt("beneficiary_id"));
//            }
//            System.out.println("DEBUG: Total beneficiaries matching both conditions: " + joinCount);
//            rs3.close();
//            ps3.close();
//
//            String sql4 = "SELECT COUNT(*) as count FROM aid WHERE aid_type_id = ? AND disaster_id = ?";
//            PreparedStatement ps4 = debugConn.prepareStatement(sql4);
//            ps4.setInt(1, aidTypeId);
//            ps4.setInt(2, disasterId);
//            ResultSet rs4 = ps4.executeQuery();
//            if (rs4.next()) {
//                System.out.println("DEBUG: Already received aid: " + rs4.getInt("count"));
//            }
//            rs4.close();
//            ps4.close();
//
//            debugConn.close();
//
//        } catch (SQLException e) {
//            System.err.println("DEBUG: Error in debug queries: " + e.getMessage());
//        }
//    }

    @Override
    public boolean hasReceivedAid(int beneficiaryId, int aidTypeId, int disasterId) {
        String sql = "SELECT COUNT(*) FROM aid WHERE beneficiary_id = ? AND aid_type_id = ? AND disaster_id = ?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ps.setInt(2, aidTypeId);
            ps.setInt(3, disasterId);

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
        String sql = "SELECT * FROM aid WHERE disaster_id = ? ORDER BY date DESC";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, disasterId);
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
        String sql = "SELECT COALESCE(SUM(qty), 0) as total FROM aid WHERE aid_type_id = ? AND disaster_id = ?";
        double total = 0.0;

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, aidTypeId);
            ps.setInt(2, disasterId);

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
                "d.name " +
                "FROM aid a " +
                "INNER JOIN beneficiary b ON a.beneficiary_id = b.beneficiary_id " +
                "INNER JOIN disaster d ON a.disaster_id = d.disaster_id " +
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
                String encryptedDisasterName = rs.getString("name");

                String aidName = cs.decryptWithOneParameter(encryptedAidName);
                String provider = cs.decryptWithOneParameter(encryptedProvider);
                String firstName = cs.decryptWithOneParameter(encryptedFirstName);
                String middleName = cs.decryptWithOneParameter(encryptedMiddleName);
                String lastName = cs.decryptWithOneParameter(encryptedLastName);
                String disasterName = cs.decryptWithOneParameter(encryptedDisasterName);

                String beneficiaryName = firstName + " " +
                        (middleName != null && !middleName.isEmpty() ? middleName + " " : "") +
                        lastName;
                beneficiaryName = beneficiaryName.trim();

                AidModel aid = new AidModel();
                aid.setAidId(rs.getInt("aid_id"));
                aid.setBeneficiaryId(rs.getInt("beneficiary_id"));
                aid.setDisasterId(rs.getInt("disaster_id"));
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
        aid.setDisasterId(rs.getInt("disaster_id"));

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