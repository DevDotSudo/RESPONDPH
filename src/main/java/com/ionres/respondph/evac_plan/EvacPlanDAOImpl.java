package com.ionres.respondph.evac_plan;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.ConfigLoader;
import com.ionres.respondph.util.Cryptography;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EvacPlanDAOImpl implements EvacPlanDAO {

    private final DBConnection dbConnection;
    private final Cryptography cs;
    private Connection conn;

    public EvacPlanDAOImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
        String secretKey = ConfigLoader.get("secretKey");
        this.cs = new Cryptography(secretKey);
    }


    @Override
    public List<RankedBeneficiaryModel> getRankedBeneficiariesByDisaster(int disasterId) {
        List<RankedBeneficiaryModel> ranked = new ArrayList<>();


        String sql =
                "SELECT " +
                        "    ahs.beneficiary_id, " +
                        "    b.first_name, " +
                        "    b.last_name, " +
                        "    ahs.final_score, " +
                        "    ahs.score_category, " +
                        "    CASE " +
                        "        WHEN ahs.household_members IS NOT NULL AND ahs.household_members > 0 " +
                        "            THEN ahs.household_members " +
                        "        ELSE (SELECT COUNT(*) FROM family_member fm WHERE fm.beneficiary_id = ahs.beneficiary_id) + 1 " +
                        "    END AS household_members " +
                        "FROM aid_and_household_score ahs " +
                        "INNER JOIN beneficiary b ON ahs.beneficiary_id = b.beneficiary_id " +
                        "WHERE ahs.disaster_id = ? " +
                        "ORDER BY ahs.final_score DESC";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, disasterId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                // Decrypt beneficiary names
                List<String> encrypted = new ArrayList<>();
                encrypted.add(rs.getString("first_name"));
                encrypted.add(rs.getString("last_name"));

                List<String> decrypted = cs.decrypt(encrypted);

                RankedBeneficiaryModel model = new RankedBeneficiaryModel();
                model.setBeneficiaryId(rs.getInt("beneficiary_id"));
                model.setFirstName(decrypted.get(0));
                model.setLastName(decrypted.get(1));
                model.setFinalScore(rs.getDouble("final_score"));
                model.setScoreCategory(rs.getString("score_category"));
                model.setHouseholdMembers(rs.getInt("household_members"));

                ranked.add(model);
            }

            rs.close();
            ps.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error fetching ranked beneficiaries: " + ex.getMessage());
        } finally {
            closeConnection();
        }

        return ranked;
    }


    @Override
    public boolean insertEvacPlan(int beneficiaryId, int evacSiteId, int disasterId, String notes) {
        String sql = "INSERT INTO evac_plan (beneficiary_id, evac_site_id, disaster_id, datetime, notes) " +
                "VALUES (?, ?, ?, NOW(), ?)";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ps.setInt(2, evacSiteId);
            ps.setInt(3, disasterId);
            ps.setString(4, notes);

            int rowsAffected = ps.executeUpdate();
            ps.close();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error inserting evac_plan: " + e.getMessage());
            return false;
        } finally {
            closeConnection();
        }
    }


    @Override
    public int getOccupiedPersonCount(int evacSiteId, int disasterId) {

        String sql =
                "SELECT COALESCE(SUM(" +
                        "    CASE " +
                        "        WHEN ahs.household_members IS NOT NULL AND ahs.household_members > 0 " +
                        "            THEN ahs.household_members " +
                        "        ELSE (SELECT COUNT(*) FROM family_member fm WHERE fm.beneficiary_id = ep.beneficiary_id) + 1 " +
                        "    END" +
                        "), 0) AS occupied " +
                        "FROM evac_plan ep " +
                        "LEFT JOIN aid_and_household_score ahs " +
                        "    ON ep.beneficiary_id = ahs.beneficiary_id " +
                        "    AND ahs.disaster_id = ep.disaster_id " +
                        "WHERE ep.evac_site_id = ? AND ep.disaster_id = ?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, evacSiteId);
            ps.setInt(2, disasterId);
            ResultSet rs = ps.executeQuery();

            int occupied = 0;
            if (rs.next()) {
                occupied = rs.getInt("occupied");
            }

            rs.close();
            ps.close();
            return occupied;

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error getting occupied count: " + e.getMessage());
            return 0;
        } finally {
            closeConnection();
        }
    }


    @Override
    public boolean isAlreadyAssigned(int beneficiaryId, int evacSiteId, int disasterId) {
        String sql = "SELECT evac_event_id FROM evac_plan " +
                "WHERE beneficiary_id = ? AND evac_site_id = ? AND disaster_id = ?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ps.setInt(2, evacSiteId);
            ps.setInt(3, disasterId);
            ResultSet rs = ps.executeQuery();

            boolean exists = rs.next();

            rs.close();
            ps.close();
            return exists;

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error checking assignment: " + e.getMessage());
            return false;
        } finally {
            closeConnection();
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