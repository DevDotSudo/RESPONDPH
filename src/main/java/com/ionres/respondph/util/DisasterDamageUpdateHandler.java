package com.ionres.respondph.util;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.household_score.HouseholdScoreCalculate;
import com.ionres.respondph.aidType_and_household_score.AidHouseholdScoreCalculate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class DisasterDamageUpdateHandler {
    private Connection conn;
    private static final boolean DEBUG = true;


    public boolean updateDamageSeverityScores(int beneficiaryId) {
        debug("Starting damage severity update for beneficiary ID", beneficiaryId);

        try {
            HouseholdScoreCalculate householdCalculator = new HouseholdScoreCalculate();
            boolean householdUpdated = householdCalculator.autoRecalculateHouseholdScore(beneficiaryId);

            if (!householdUpdated) {
                System.err.println("Failed to update household score for beneficiary ID: " + beneficiaryId);
                return false;
            }

            debug("Household score updated successfully", "✓");

            List<Integer> aidTypeIds = getAidTypeIdsForBeneficiary(beneficiaryId);

            if (aidTypeIds.isEmpty()) {
                debug("No aid types found for beneficiary", beneficiaryId);
                return true;
            }

            AidHouseholdScoreCalculate aidCalculator = new AidHouseholdScoreCalculate();
            int successCount = 0;

            for (Integer aidTypeId : aidTypeIds) {
                int adminId = getAdminIdForAidHouseholdScore(beneficiaryId, aidTypeId);

                boolean aidUpdated = aidCalculator.calculateAndSaveAidHouseholdScore(
                        beneficiaryId,
                        aidTypeId,
                        adminId
                );

                if (aidUpdated) {
                    successCount++;
                    debug("Updated aid-household score for aid type ID", aidTypeId);
                } else {
                    System.err.println("Failed to update aid-household score for aid type ID: " + aidTypeId);
                }
            }

            debug("Updated aid-household scores", successCount + "/" + aidTypeIds.size());

            return successCount == aidTypeIds.size();

        } catch (Exception e) {
            System.err.println("Error updating damage severity scores: " + e.getMessage());
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


    private int getAdminIdForAidHouseholdScore(int beneficiaryId, int aidTypeId) {
        String sql = "SELECT admin_id FROM aid_and_household_score " +
                "WHERE beneficiary_id = ? AND aid_type_id = ?";

        try {
            conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ps.setInt(2, aidTypeId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int adminId = rs.getInt("admin_id");
                rs.close();
                ps.close();
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

        return 1;
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
