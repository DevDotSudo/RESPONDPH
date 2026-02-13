package com.ionres.respondph.evacuation_plan;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.ConfigLoader;
import com.ionres.respondph.util.Cryptography;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EvacuationPlanDAOImpl implements EvacuationPlanDAO {
    private final DBConnection dbConnection;
    private final Cryptography cs;
    private Connection conn;

    public EvacuationPlanDAOImpl(DBConnection dbConnection) {
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
        String sql = "INSERT IGNORE INTO evac_plan (beneficiary_id, evac_site_id, disaster_id, datetime, notes) " +
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

            if (rowsAffected == 0) {
                System.out.println("[DUPLICATE] Beneficiary #" + beneficiaryId +
                        " already assigned to disaster #" + disasterId + " — insert skipped.");
            }

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

    @Override
    public List<EvacuationPlanModel> getAll() {
        List<EvacuationPlanModel> plans = new ArrayList<>();

        String sql =
                "SELECT " +
                        "    ep.evac_event_id, " +
                        "    ep.beneficiary_id, " +
                        "    b.first_name, " +
                        "    b.last_name, " +
                        "    ep.evac_site_id, " +
                        "    es.name AS evac_site_name, " +
                        "    ep.disaster_id, " +
                        "    d.name AS disaster_name, " +
                        "    ep.datetime, " +
                        "    ep.notes, " +
                        "    COALESCE(" +
                        "        (SELECT ahs2.household_members " +
                        "         FROM aid_and_household_score ahs2 " +
                        "         WHERE ahs2.beneficiary_id = ep.beneficiary_id " +
                        "           AND ahs2.disaster_id = ep.disaster_id " +
                        "         ORDER BY ahs2.beneficiary_family_score_id DESC " +
                        "         LIMIT 1), " +
                        "        (SELECT COUNT(*) FROM family_member fm WHERE fm.beneficiary_id = ep.beneficiary_id) + 1" +
                        "    ) AS household_members " +
                        "FROM evac_plan ep " +
                        "INNER JOIN beneficiary b ON ep.beneficiary_id = b.beneficiary_id " +
                        "INNER JOIN evac_site es ON ep.evac_site_id = es.evac_id " +
                        "INNER JOIN disaster d ON ep.disaster_id = d.disaster_id " +
                        "ORDER BY ep.datetime DESC";
        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                List<String> encrypted = new ArrayList<>();
                encrypted.add(rs.getString("first_name"));
                encrypted.add(rs.getString("last_name"));
                encrypted.add(rs.getString("evac_site_name"));
                encrypted.add(rs.getString("disaster_name"));

                List<String> decrypted = cs.decrypt(encrypted);

                String beneficiaryName = decrypted.get(0) + " " + decrypted.get(1);

                EvacuationPlanModel model = new EvacuationPlanModel();
                model.setPlanId(rs.getInt("evac_event_id"));
                model.setBeneficiaryId(rs.getInt("beneficiary_id"));
                model.setBeneficiaryName(beneficiaryName);
                model.setEvacSiteId(rs.getInt("evac_site_id"));
                model.setEvacSiteName(decrypted.get(2));
                model.setDisasterId(rs.getInt("disaster_id"));
                model.setDisasterName(decrypted.get(3));
                model.setDateCreated(rs.getString("datetime"));
                model.setNotes(rs.getString("notes"));
                plans.add(model);
            }

            rs.close();
            ps.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error fetching all evacuation plans: " + ex.getMessage());
        } finally {
            closeConnection();
        }

        return plans;
    }

    @Override
    public List<EvacuationPlanModel> search(String searchText) {
        List<EvacuationPlanModel> plans = new ArrayList<>();

        String sql =
                "SELECT " +
                        "    ep.evac_event_id, " +
                        "    ep.beneficiary_id, " +
                        "    b.first_name, " +
                        "    b.last_name, " +
                        "    ep.evac_site_id, " +
                        "    es.name AS evac_site_name, " +
                        "    ep.disaster_id, " +
                        "    d.name AS disaster_name, " +
                        "    ep.datetime, " +
                        "    ep.notes, " +
                        "    COALESCE(ahs.household_members, " +
                        "        (SELECT COUNT(*) FROM family_member fm WHERE fm.beneficiary_id = ep.beneficiary_id) + 1) AS household_members " +
                        "FROM evac_plan ep " +
                        "INNER JOIN beneficiary b ON ep.beneficiary_id = b.beneficiary_id " +
                        "INNER JOIN evac_site es ON ep.evac_site_id = es.evac_id " +
                        "INNER JOIN disaster d ON ep.disaster_id = d.disaster_id " +
                        "LEFT JOIN aid_and_household_score ahs " +
                        "    ON ep.beneficiary_id = ahs.beneficiary_id " +
                        "    AND ep.disaster_id = ahs.disaster_id " +
                        "ORDER BY ep.datetime DESC";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            String lowerSearchText = searchText.toLowerCase();

            while (rs.next()) {
                List<String> encrypted = new ArrayList<>();
                encrypted.add(rs.getString("first_name"));
                encrypted.add(rs.getString("last_name"));
                encrypted.add(rs.getString("evac_site_name"));
                encrypted.add(rs.getString("disaster_name"));

                List<String> decrypted = cs.decrypt(encrypted);

                String beneficiaryName = decrypted.get(0) + " " + decrypted.get(1);
                String evacSiteName = decrypted.get(2);
                String disasterName = decrypted.get(3);
                String datetime = rs.getString("datetime");

                if (beneficiaryName.toLowerCase().contains(lowerSearchText) ||
                        evacSiteName.toLowerCase().contains(lowerSearchText) ||
                        disasterName.toLowerCase().contains(lowerSearchText) ||
                        datetime.toLowerCase().contains(lowerSearchText)) {

                    EvacuationPlanModel model = new EvacuationPlanModel();
                    model.setPlanId(rs.getInt("evac_event_id"));
                    model.setBeneficiaryId(rs.getInt("beneficiary_id"));
                    model.setBeneficiaryName(beneficiaryName);
                    model.setEvacSiteId(rs.getInt("evac_site_id"));
                    model.setEvacSiteName(evacSiteName);
                    model.setDisasterId(rs.getInt("disaster_id"));
                    model.setDisasterName(disasterName);
                    model.setDateCreated(datetime);
                    model.setNotes(rs.getString("notes"));

                    plans.add(model);
                }
            }

            rs.close();
            ps.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error searching evacuation plans: " + ex.getMessage());
        } finally {
            closeConnection();
        }

        return plans;
    }

    @Override
    public boolean isAlreadyAssignedToDisaster(int beneficiaryId, int disasterId) {
        String sql = "SELECT evac_event_id FROM evac_plan " +
                "WHERE beneficiary_id = ? AND disaster_id = ?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ps.setInt(2, disasterId);
            ResultSet rs = ps.executeQuery();

            boolean exists = rs.next();

            if (exists) {
                int evacEventId = rs.getInt("evac_event_id");
                System.out.println("[DUPLICATE CHECK] Beneficiary #" + beneficiaryId +
                        " already has assignment (evac_event_id: " + evacEventId +
                        ") for disaster #" + disasterId);
            }

            rs.close();
            ps.close();
            return exists;

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error checking disaster assignment: " + e.getMessage());
            return false;
        } finally {
            closeConnection();
        }
    }

    @Override
    public EvacuationPlanModel getById(int planId) {
        EvacuationPlanModel model = null;

        String sql =
                "SELECT " +
                        "    ep.evac_event_id, " +
                        "    ep.beneficiary_id, " +
                        "    b.first_name, " +
                        "    b.last_name, " +
                        "    ep.evac_site_id, " +
                        "    es.name AS evac_site_name, " +
                        "    ep.disaster_id, " +
                        "    d.name AS disaster_name, " +
                        "    ep.datetime, " +
                        "    ep.notes, " +
                        "    COALESCE(ahs.household_members, " +
                        "        (SELECT COUNT(*) FROM family_member fm WHERE fm.beneficiary_id = ep.beneficiary_id) + 1) AS household_members " +
                        "FROM evac_plan ep " +
                        "INNER JOIN beneficiary b ON ep.beneficiary_id = b.beneficiary_id " +
                        "INNER JOIN evac_site es ON ep.evac_site_id = es.evac_id " +
                        "INNER JOIN disaster d ON ep.disaster_id = d.disaster_id " +
                        "LEFT JOIN aid_and_household_score ahs " +
                        "    ON ep.beneficiary_id = ahs.beneficiary_id " +
                        "    AND ep.disaster_id = ahs.disaster_id " +
                        "WHERE ep.evac_event_id = ?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, planId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                List<String> encrypted = new ArrayList<>();
                encrypted.add(rs.getString("first_name"));
                encrypted.add(rs.getString("last_name"));
                encrypted.add(rs.getString("evac_site_name"));
                encrypted.add(rs.getString("disaster_name"));

                List<String> decrypted = cs.decrypt(encrypted);

                String beneficiaryName = decrypted.get(0) + " " + decrypted.get(1);

                model = new EvacuationPlanModel();
                model.setPlanId(rs.getInt("evac_event_id"));
                model.setBeneficiaryId(rs.getInt("beneficiary_id"));
                model.setBeneficiaryName(beneficiaryName);
                model.setEvacSiteId(rs.getInt("evac_site_id"));
                model.setEvacSiteName(decrypted.get(2));
                model.setDisasterId(rs.getInt("disaster_id"));
                model.setDisasterName(decrypted.get(3));
                model.setDateCreated(rs.getString("datetime"));
                model.setNotes(rs.getString("notes"));
            }

            rs.close();
            ps.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error fetching evacuation plan by ID: " + ex.getMessage());
        } finally {
            closeConnection();
        }

        return model;
    }

    @Override
    public boolean deleteEvacPlan(int planId) {
        String sql = "DELETE FROM evac_plan WHERE evac_event_id = ?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, planId);

            int rowsAffected = ps.executeUpdate();
            ps.close();

            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error deleting evac_plan: " + e.getMessage());
            return false;
        } finally {
            closeConnection();
        }
    }

    @Override
    public int getHouseholdSizeForBeneficiary(int beneficiaryId, int disasterId) {
        String sql = "SELECT COALESCE(ahs.household_members, " +
                "(SELECT COUNT(*) FROM family_member fm WHERE fm.beneficiary_id = ?) + 1) AS household_members " +
                "FROM beneficiary b " +
                "LEFT JOIN aid_and_household_score ahs " +
                "    ON b.beneficiary_id = ahs.beneficiary_id " +
                "    AND ahs.disaster_id = ? " +
                "WHERE b.beneficiary_id = ?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ps.setInt(2, disasterId);
            ps.setInt(3, beneficiaryId);
            ResultSet rs = ps.executeQuery();

            int householdSize = 1;
            if (rs.next()) {
                householdSize = rs.getInt("household_members");
            }

            rs.close();
            ps.close();
            return householdSize;

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error getting household size: " + e.getMessage());
            return 1;
        } finally {
            closeConnection();
        }
    }


    @Override
    public List<BeneficiaryModel> getBeneficiariesByEvacSiteAndDisaster(int evacSiteId, int disasterId) {
        List<BeneficiaryModel> beneficiaries = new ArrayList<>();

        String sql =
                "SELECT DISTINCT " +
                        "    b.beneficiary_id, " +
                        "    b.first_name, " +
                        "    b.middle_name, " +
                        "    b.last_name, " +
                        "    b.mobile_number, " +
                        "    b.barangay, " +
                        "    b.latitude, " +
                        "    b.longitude " +
                        "FROM evac_plan ep " +
                        "INNER JOIN beneficiary b ON ep.beneficiary_id = b.beneficiary_id " +
                        "WHERE ep.evac_site_id = ? AND ep.disaster_id = ? " +
                        "ORDER BY b.last_name, b.first_name";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, evacSiteId);
            ps.setInt(2, disasterId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                List<String> encrypted = new ArrayList<>();
                encrypted.add(rs.getString("first_name"));
                encrypted.add(rs.getString("middle_name"));
                encrypted.add(rs.getString("last_name"));
                encrypted.add(rs.getString("mobile_number"));
                encrypted.add(rs.getString("barangay"));
                encrypted.add(rs.getString("latitude"));
                encrypted.add(rs.getString("longitude"));

                List<String> decrypted = cs.decrypt(encrypted);

                BeneficiaryModel model = new BeneficiaryModel();
                model.setId(rs.getInt("beneficiary_id"));
                model.setFirstname(decrypted.get(0));
                model.setMiddlename(decrypted.get(1));
                model.setLastname(decrypted.get(2));
                model.setMobileNumber(decrypted.get(3));
                model.setBarangay(decrypted.get(4));
                model.setLatitude(decrypted.get(5));
                model.setLongitude(decrypted.get(6));

                beneficiaries.add(model);
            }

            rs.close();
            ps.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error fetching beneficiaries by evac site and disaster: " + ex.getMessage());
        } finally {
            closeConnection();
        }

        return beneficiaries;
    }

    @Override
    public Integer getAssignedEvacSiteId(int beneficiaryId, int disasterId) {
        String sql = "SELECT evac_site_id FROM evac_plan WHERE beneficiary_id = ? AND disaster_id = ? LIMIT 1";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, beneficiaryId);
            pstmt.setInt(2, disasterId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int evacSiteId = rs.getInt("evac_site_id"); // ← CHECK THIS LINE - should be "evac_site_id" NOT "evacs_site_id"

                // ADD LOGGING TO DEBUG
                System.out.println("DEBUG: Found evac_site_id = " + evacSiteId + " for beneficiary " + beneficiaryId);

                rs.close();
                pstmt.close();

                return evacSiteId;
            }

            System.out.println("DEBUG: No assignment found for beneficiary " + beneficiaryId + ", disaster " + disasterId);

            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            System.err.println("Error getting assigned evac site ID: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return null;
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