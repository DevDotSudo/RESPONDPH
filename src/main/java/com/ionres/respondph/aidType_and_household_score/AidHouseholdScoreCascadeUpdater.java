

package com.ionres.respondph.aidType_and_household_score;

import com.ionres.respondph.database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AidHouseholdScoreCascadeUpdater {

    private Connection conn;

    public int recalculateAllAidScores() {
        int recalculatedCount = 0;

        List<AidScoreRecord> existingRecords = getAllAidScoreRecords();

        System.out.println("========== RECALCULATING ALL AID HOUSEHOLD SCORES ==========");
        System.out.println("Found " + existingRecords.size() + " aid score records to recalculate");

        for (AidScoreRecord record : existingRecords) {
            try {
                AidHouseholdScoreCalculate calculator = new AidHouseholdScoreCalculate();

                // ✅ MODIFIED: Pass disasterId to calculator
                boolean success = calculator.calculateAndSaveAidHouseholdScoreWithDisaster(
                        record.beneficiaryId,
                        record.aidTypeId,
                        record.adminId,
                        record.disasterId  // ✅ NEW
                );

                if (success) {
                    recalculatedCount++;
                    System.out.println("✓ Recalculated aid score for Beneficiary ID: " + record.beneficiaryId +
                            ", Aid Type ID: " + record.aidTypeId + ", Disaster ID: " + record.disasterId);
                } else {
                    System.err.println("✗ Failed to recalculate aid score for Beneficiary ID: " + record.beneficiaryId);
                }

            } catch (Exception e) {
                System.err.println("✗ Error recalculating aid score for Beneficiary ID " +
                        record.beneficiaryId + ": " + e.getMessage());
            }
        }

        System.out.println("========== AID SCORE RECALCULATION COMPLETE ==========");
        System.out.println("Successfully recalculated: " + recalculatedCount + " out of " + existingRecords.size());

        return recalculatedCount;
    }

    private List<AidScoreRecord> getAllAidScoreRecords() {
        List<AidScoreRecord> records = new ArrayList<>();
        // ✅ MODIFIED: Select disaster_id as well
        String sql = "SELECT DISTINCT beneficiary_id, aid_type_id, admin_id, disaster_id FROM aid_and_household_score";

        try {
            conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                AidScoreRecord record = new AidScoreRecord();
                record.beneficiaryId = rs.getInt("beneficiary_id");
                record.aidTypeId = rs.getInt("aid_type_id");
                record.adminId = rs.getInt("admin_id");
                record.disasterId = rs.getInt("disaster_id");  // ✅ NEW
                records.add(record);
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.err.println("Error fetching aid score records: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return records;
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

    // ✅ MODIFIED: Now recalculates for all disasters associated with this beneficiary
    public int recalculateAidScoresForBeneficiary(int beneficiaryId) {
        int recalculatedCount = 0;

        List<AidScoreRecord> records = getAidScoreRecordsForBeneficiary(beneficiaryId);

        if (records.isEmpty()) {
            System.out.println("No aid scores found for Beneficiary ID: " + beneficiaryId);
            return 0;
        }

        System.out.println("Recalculating " + records.size() + " aid score(s) for Beneficiary ID: " + beneficiaryId);

        for (AidScoreRecord record : records) {
            try {
                AidHouseholdScoreCalculate calculator = new AidHouseholdScoreCalculate();

                // ✅ MODIFIED: Pass disasterId to calculator
                boolean success = calculator.calculateAndSaveAidHouseholdScoreWithDisaster(
                        record.beneficiaryId,
                        record.aidTypeId,
                        record.adminId,
                        record.disasterId  // ✅ NEW
                );

                if (success) {
                    recalculatedCount++;
                    System.out.println("  ✓ Recalculated aid score for Aid Type ID: " + record.aidTypeId +
                            ", Disaster ID: " + record.disasterId);
                }

            } catch (Exception e) {
                System.err.println("  ✗ Error recalculating aid score: " + e.getMessage());
            }
        }

        return recalculatedCount;
    }

    private List<AidScoreRecord> getAidScoreRecordsForBeneficiary(int beneficiaryId) {
        List<AidScoreRecord> records = new ArrayList<>();
        // ✅ MODIFIED: Select disaster_id as well
        String sql = "SELECT beneficiary_id, aid_type_id, admin_id, disaster_id FROM aid_and_household_score WHERE beneficiary_id = ?";

        try {
            conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                AidScoreRecord record = new AidScoreRecord();
                record.beneficiaryId = rs.getInt("beneficiary_id");
                record.aidTypeId = rs.getInt("aid_type_id");
                record.adminId = rs.getInt("admin_id");
                record.disasterId = rs.getInt("disaster_id");  // ✅ NEW
                records.add(record);
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.err.println("Error fetching aid score records for beneficiary: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return records;
    }

    private static class AidScoreRecord {
        int beneficiaryId;
        int aidTypeId;
        int adminId;
        int disasterId;  // ✅ NEW
    }
}