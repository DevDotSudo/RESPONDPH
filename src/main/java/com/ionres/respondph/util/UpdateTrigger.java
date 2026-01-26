package com.ionres.respondph.util;

import com.ionres.respondph.aidType_and_household_score.AidHouseholdScoreCalculate;
import com.ionres.respondph.household_score.HouseholdScoreCalculate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class UpdateTrigger {


    public boolean triggerCascadeUpdate(int beneficiaryId) {
        try {
            System.out.println("========== CASCADE UPDATE TRIGGERED ==========");
            System.out.println("Beneficiary ID: " + beneficiaryId);

            // Step 1: Recalculate household score
            HouseholdScoreCalculate householdCalc = new HouseholdScoreCalculate();
            boolean householdUpdated = householdCalc.autoRecalculateHouseholdScore(beneficiaryId);

            if (!householdUpdated) {
                System.err.println("Failed to update household score for beneficiary: " + beneficiaryId);
                return false;
            }
            System.out.println("✓ Household score updated successfully");

            // Step 2: Get all aid types that have scores for this beneficiary
            List<AidTypeScoreRecord> existingAidScores = getExistingAidScores(beneficiaryId);

            if (existingAidScores.isEmpty()) {
                System.out.println("No existing aid-household scores to update");
                System.out.println("========== CASCADE UPDATE COMPLETE ==========");
                return true;
            }

            System.out.println("Found " + existingAidScores.size() + " aid-household scores to update");

            // Step 3: Recalculate each aid-household score
            AidHouseholdScoreCalculate aidCalc = new AidHouseholdScoreCalculate();
            int successCount = 0;
            int failCount = 0;

            for (AidTypeScoreRecord record : existingAidScores) {
                try {
                    boolean success = aidCalc.calculateAndSaveAidHouseholdScore(
                            beneficiaryId,
                            record.aidTypeId,
                            record.adminId
                    );

                    if (success) {
                        successCount++;
                        System.out.println("✓ Updated aid-household score for aid type ID: " + record.aidTypeId);
                    } else {
                        failCount++;
                        System.err.println("✗ Failed to update aid-household score for aid type ID: " + record.aidTypeId);
                    }
                } catch (Exception e) {
                    failCount++;
                    System.err.println("✗ Error updating aid-household score for aid type ID " +
                            record.aidTypeId + ": " + e.getMessage());
                }
            }

            System.out.println("========== CASCADE UPDATE COMPLETE ==========");
            System.out.println("Successfully updated: " + successCount + " aid-household scores");
            System.out.println("Failed: " + failCount);

            return failCount == 0;

        } catch (Exception e) {
            System.err.println("Error in cascade update: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets all existing aid-household scores for a beneficiary
     */
    private List<AidTypeScoreRecord> getExistingAidScores(int beneficiaryId) {
        List<AidTypeScoreRecord> records = new ArrayList<>();
        String sql = "SELECT aid_type_id, admin_id FROM aid_and_household_score WHERE beneficiary_id = ?";

        Connection conn = null;
        try {
            conn = com.ionres.respondph.database.DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                records.add(new AidTypeScoreRecord(
                        rs.getInt("aid_type_id"),
                        rs.getInt("admin_id")
                ));
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.err.println("Error fetching existing aid scores: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }

        return records;
    }

    /**
     * Helper class to store aid type score records
     */
    private static class AidTypeScoreRecord {
        int aidTypeId;
        int adminId;

        AidTypeScoreRecord(int aidTypeId, int adminId) {
            this.aidTypeId = aidTypeId;
            this.adminId = adminId;
        }
    }
}
