package com.ionres.respondph.util;

import com.ionres.respondph.aidType_and_household_score.AidHouseholdScoreCalculate;
import com.ionres.respondph.household_score.HouseholdScoreCalculate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.ionres.respondph.util.AppContext.aidTypeService;

public class UpdateTrigger {

    public boolean triggerCascadeUpdateWithDisaster(int beneficiaryId, int disasterId) {
        try {
            System.out.println("========== CASCADE UPDATE TRIGGERED ==========");
            System.out.println("Beneficiary ID: " + beneficiaryId);
            System.out.println("Disaster ID: " + disasterId);

            HouseholdScoreCalculate householdCalc = new HouseholdScoreCalculate();
            boolean householdUpdated = householdCalc.calculateAndSaveHouseholdScore(beneficiaryId, disasterId);

            if (!householdUpdated) {
                System.err.println("Failed to update household score for beneficiary: " + beneficiaryId +
                        ", disaster: " + disasterId);
                return false;
            }
            System.out.println("✓ Household score updated successfully");

            List<AidTypeScoreRecord> existingAidScores = getExistingAidScoresWithDisaster(beneficiaryId, disasterId);

            if (existingAidScores.isEmpty()) {
                System.out.println("No existing aid-household scores to update for this disaster");
                System.out.println("========== CASCADE UPDATE COMPLETE ==========");
                return true;
            }

            System.out.println("Found " + existingAidScores.size() + " aid-household scores to update");

            // Step 3: Recalculate each aid-household score for this disaster
            AidHouseholdScoreCalculate aidCalc = new AidHouseholdScoreCalculate();
            int successCount = 0;
            int failCount = 0;

            for (AidTypeScoreRecord record : existingAidScores) {
                try {
                    boolean success = aidCalc.calculateAndSaveAidHouseholdScoreWithDisaster(
                            beneficiaryId,
                            record.aidTypeId,
                            record.adminId,
                            disasterId  // ✅ NEW
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

    public boolean triggerCascadeUpdate(int beneficiaryId) {
        try {
            System.out.println("========== CASCADE UPDATE TRIGGERED ==========");
            System.out.println("Beneficiary ID: " + beneficiaryId);

            HouseholdScoreCalculate householdCalc = new HouseholdScoreCalculate();
            boolean householdUpdated = householdCalc.autoRecalculateHouseholdScore(beneficiaryId);

            if (!householdUpdated) {
                System.err.println("Failed to update household score for beneficiary: " + beneficiaryId);
                return false;
            }
            System.out.println("✓ Household score updated successfully");

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



    private List<Integer> getDisasterIdsForBeneficiary(int beneficiaryId) {
        List<Integer> disasterIds = new ArrayList<>();
        String sql = "SELECT DISTINCT disaster_id FROM household_score WHERE beneficiary_id = ?";

        Connection conn = null;
        try {
            conn = com.ionres.respondph.database.DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                disasterIds.add(rs.getInt("disaster_id"));
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.err.println("Error fetching disaster IDs: " + e.getMessage());
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

        return disasterIds;
    }

    private List<AidTypeScoreRecord> getExistingAidScoresWithDisaster(int beneficiaryId, int disasterId) {
        List<AidTypeScoreRecord> records = new ArrayList<>();
        // ✅ MODIFIED: Added disaster_id to WHERE clause
        String sql = "SELECT aid_type_id, admin_id FROM aid_and_household_score " +
                "WHERE beneficiary_id = ? AND disaster_id = ?";

        Connection conn = null;
        try {
            conn = com.ionres.respondph.database.DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ps.setInt(2, disasterId);  // ✅ NEW
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


    public boolean triggerCascadeUpdateForNewBeneficiaryWithDisaster(int beneficiaryId, int adminId, int disasterId) {
        System.out.println("========== TRIGGERING CASCADE UPDATE FOR NEW BENEFICIARY ==========");
        System.out.println("Beneficiary ID: " + beneficiaryId);
        System.out.println("Admin ID: " + adminId);
        HouseholdScoreCalculate householdCalc = new HouseholdScoreCalculate();

        try {
            // Step 1: Calculate and save household score (without disaster)
            System.out.println("Step 1: Calculating household score...");
            boolean householdSuccess = householdCalc.autoRecalculateHouseholdScore(beneficiaryId);

            if (!householdSuccess) {
                System.err.println("✗ Failed to calculate household score");
                return false;
            }
            System.out.println("✓ Household score calculated successfully");

            // Step 2: Check if any AidTypes exist
            System.out.println("Step 2: Checking for existing AidTypes...");
            boolean hasAidTypes = aidTypeService.hasAnyAidTypes();

            if (!hasAidTypes) {
                System.out.println("⚠ No AidTypes found in system");
                System.out.println("⚠ Skipping AidHouseholdScore calculation");
                System.out.println("⚠ You can calculate AidHouseholdScores later after creating AidTypes");
                System.out.println("========== CASCADE UPDATE COMPLETED (HOUSEHOLD ONLY) ==========");
                return true; // Success - household score was saved
            }

            System.out.println("✓ AidTypes found - proceeding with AidHouseholdScore calculation");

            // Step 3: Get all AidType IDs
            List<Integer> aidTypeIds = aidTypeService.getAllAidTypeIds();
            System.out.println("Found " + aidTypeIds.size() + " AidType(s)");

            // Step 4: Calculate AidHouseholdScore for each AidType (without disaster)
            AidHouseholdScoreCalculate aidCalc = new AidHouseholdScoreCalculate();
            int successCount = 0;
            for (Integer aidTypeId : aidTypeIds) {
                System.out.println("Calculating AidHouseholdScore for AidType ID: " + aidTypeId);
                boolean aidSuccess = aidCalc.calculateAndSaveAidHouseholdScoreWithDisaster(
                        beneficiaryId, aidTypeId, adminId, disasterId
                );

                if (aidSuccess) {
                    successCount++;
                    System.out.println("✓ AidHouseholdScore calculated for AidType ID: " + aidTypeId);
                } else {
                    System.err.println("✗ Failed to calculate AidHouseholdScore for AidType ID: " + aidTypeId);
                }
            }

            System.out.println("Successfully calculated " + successCount + " out of " + aidTypeIds.size() + " AidHouseholdScores");
            System.out.println("========== CASCADE UPDATE COMPLETED (FULL) ==========");

            return successCount > 0 || aidTypeIds.isEmpty();

        } catch (Exception e) {
            System.err.println("✗ Error during cascade update: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    private static class AidTypeScoreRecord {
        int aidTypeId;
        int adminId;

        AidTypeScoreRecord(int aidTypeId, int adminId) {
            this.aidTypeId = aidTypeId;
            this.adminId = adminId;
        }
    }
}
