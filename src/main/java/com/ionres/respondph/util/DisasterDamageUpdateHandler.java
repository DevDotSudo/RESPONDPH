package com.ionres.respondph.util;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.household_score.HouseholdScoreCalculate;
import com.ionres.respondph.aidType_and_household_score.AidHouseholdScoreCalculate;
import com.ionres.respondph.household_score.HouseholdScoreDAO;
import com.ionres.respondph.household_score.HouseholdScoreDAOServiceImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DisasterDamageUpdateHandler {
    private Connection conn;
    private static final boolean DEBUG = true;

    public boolean updateDamageSeverityScores(int beneficiaryId, int disasterId) {
        debug("========== DISASTER DAMAGE UPDATE HANDLER ==========", "");
        debug("Starting damage severity update for beneficiary ID", beneficiaryId);
        debug("Disaster ID", disasterId);

        try {
            // Step 1: Update any existing NULL disaster_id records
            HouseholdScoreDAO dao = new HouseholdScoreDAOServiceImpl();
            boolean nullUpdated = dao.updateNullDisasterIdToSpecificDisaster(beneficiaryId, disasterId);

            if (nullUpdated) {
                debug("✓ Updated NULL disaster_id record to disaster", disasterId);
            } else {
                debug("No NULL disaster_id record found, will create new record", "");
            }

            // Step 2: Calculate and save household score WITH disaster ID
            debug("Step 2: Calculating household score with disaster ID", disasterId);
            HouseholdScoreCalculate householdCalculator = new HouseholdScoreCalculate();
            boolean householdUpdated = householdCalculator.calculateAndSaveHouseholdScore(beneficiaryId, disasterId);

            if (!householdUpdated) {
                System.err.println("✗ Failed to update household score for beneficiary ID: " + beneficiaryId);
                return false;
            }

            debug("✓ Household score updated successfully", "");

            // Step 3: Get all aid types for this beneficiary
            debug("Step 3: Fetching aid type IDs", "");
            List<Integer> aidTypeIds = getAidTypeIdsForBeneficiary(beneficiaryId);

            if (aidTypeIds.isEmpty()) {
                debug("⚠ No aid types found for beneficiary", beneficiaryId);
                debug("Household score saved, but no aid-household scores to update", "");
                return true; // Still return true because household score was saved
            }

            debug("Found aid types", aidTypeIds.size());

            // Step 4: Update aid-household scores WITH disaster ID
            AidHouseholdScoreCalculate aidCalculator = new AidHouseholdScoreCalculate();
            int successCount = 0;

            for (Integer aidTypeId : aidTypeIds) {
                debug("Processing aid type ID", aidTypeId);

                // Get the admin ID who created this aid-household score
                int adminId = getAdminIdForAidHouseholdScore(beneficiaryId, aidTypeId, disasterId);

                // ✅ CRITICAL FIX: Use the method WITH disaster ID
                boolean aidUpdated = aidCalculator.calculateAndSaveAidHouseholdScoreWithDisaster(
                        beneficiaryId,
                        aidTypeId,
                        adminId,
                        disasterId  // ✅ Pass the disaster ID
                );

                if (aidUpdated) {
                    successCount++;
                    debug("✓ Updated aid-household score for aid type ID", aidTypeId);
                } else {
                    System.err.println("✗ Failed to update aid-household score for aid type ID: " + aidTypeId);
                }
            }

            debug("Updated aid-household scores", successCount + "/" + aidTypeIds.size());
            debug("========== UPDATE COMPLETE ==========", "");

            return successCount == aidTypeIds.size();

        } catch (Exception e) {
            System.err.println("✗ Error updating damage severity scores: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private List<Integer> getAidTypeIdsForBeneficiary(int beneficiaryId) {
        List<Integer> aidTypeIds = new ArrayList<>();
        String sql = "SELECT DISTINCT aid_type_id FROM aid_and_household_score WHERE beneficiary_id = ?";

        try {
            conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                aidTypeIds.add(rs.getInt("aid_type_id"));
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.err.println("Error fetching aid type IDs: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return aidTypeIds;
    }

    // ✅ UPDATED: Add disasterId parameter and check for this specific disaster
    private int getAdminIdForAidHouseholdScore(int beneficiaryId, int aidTypeId, int disasterId) {
        // First try to get admin ID from existing record for this disaster
        String sqlWithDisaster = "SELECT admin_id FROM aid_and_household_score " +
                "WHERE beneficiary_id = ? AND aid_type_id = ? AND disaster_id = ?";

        try {
            conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sqlWithDisaster);
            ps.setInt(1, beneficiaryId);
            ps.setInt(2, aidTypeId);
            ps.setInt(3, disasterId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int adminId = rs.getInt("admin_id");
                rs.close();
                ps.close();
                debug("Found existing admin ID for this disaster", adminId);
                return adminId;
            }

            rs.close();
            ps.close();

            // If no record for this disaster, try to get from any record for this beneficiary+aid_type
            String sqlAny = "SELECT admin_id FROM aid_and_household_score " +
                    "WHERE beneficiary_id = ? AND aid_type_id = ? LIMIT 1";

            ps = conn.prepareStatement(sqlAny);
            ps.setInt(1, beneficiaryId);
            ps.setInt(2, aidTypeId);
            rs = ps.executeQuery();

            if (rs.next()) {
                int adminId = rs.getInt("admin_id");
                rs.close();
                ps.close();
                debug("Found admin ID from other record", adminId);
                return adminId;
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.err.println("Error fetching admin ID: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        // If still no admin found, use current logged-in admin
        try {
            int currentAdminId = SessionManager.getInstance().getCurrentAdmin().getId();
            debug("Using current logged-in admin ID", currentAdminId);
            return currentAdminId;
        } catch (Exception e) {
            debug("⚠ Could not get current admin, using default admin ID 1", "");
            return 1;
        }
    }

    private void debug(String label, Object value) {
        if (DEBUG) {
            System.out.println("[DisasterDamageUpdateHandler] " + label + " => [" + value + "]");
        }
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
