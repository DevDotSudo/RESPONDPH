package com.ionres.respondph.disaster_damage;

import com.ionres.respondph.common.model.BeneficiaryModel;
import com.ionres.respondph.common.model.DisasterModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.ResourceUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DisasterDamageDAOImpl implements DisasterDamageDAO {
    private static final Logger LOGGER = Logger.getLogger(DisasterDamageDAOImpl.class.getName());
    private final DBConnection dbConnection;

    public DisasterDamageDAOImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public boolean saving(DisasterDamageModel ddm) {
        String sql = "INSERT INTO beneficiary_disaster_damage (beneficiary_id, disaster_id, house_damage_severity, assessment_date, verified_by, notes, reg_date)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = dbConnection.getConnection();
            ps = conn.prepareStatement(sql);

            ps.setInt(1, ddm.getBeneficiaryId());
            ps.setInt(2, ddm.getDisasterId());
            ps.setString(3, ddm.getHouseDamageSeverity());
            ps.setString(4, ddm.getAssessmentDate());
            ps.setString(5, ddm.getVerifiedBy());
            ps.setString(6, ddm.getNotes());
            ps.setString(7, ddm.getRegDate());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error occurred while saving disaster damage", e);
            return false;
        } finally {
            ResourceUtils.closePreparedStatement(ps);
        }
    }

    @Override
    public List<DisasterDamageModel> getAll() {
        List<DisasterDamageModel> list = new ArrayList<>();
        String sql = "SELECT dmd.beneficiary_disaster_damage_id, " +
                "dmd.beneficiary_id, " +
                "b.first_name AS beneficiary_firstname, " +
                "dmd.disaster_id, " +
                "d.type AS disaster_type, " +
                "d.name AS disaster_name, " +
                "dmd.house_damage_severity, " +
                "dmd.assessment_date, " +
                "dmd.verified_by, " +
                "dmd.notes, " +
                "dmd.reg_date " +
                "FROM beneficiary_disaster_damage dmd " +
                "INNER JOIN beneficiary b ON dmd.beneficiary_id = b.beneficiary_id " +
                "INNER JOIN disaster d ON dmd.disaster_id = d.disaster_id";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = dbConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                DisasterDamageModel ddm = new DisasterDamageModel();
                ddm.setBeneficiaryDisasterDamageId(rs.getInt("beneficiary_disaster_damage_id"));
                ddm.setBeneficiaryId(rs.getInt("beneficiary_id"));
                ddm.setBeneficiaryFirstname(rs.getString("beneficiary_firstname"));
                ddm.setDisasterId(rs.getInt("disaster_id"));
                ddm.setDisasterType(rs.getString("disaster_type"));
                ddm.setDisasterName(rs.getString("disaster_name"));
                ddm.setHouseDamageSeverity(rs.getString("house_damage_severity"));
                ddm.setAssessmentDate(rs.getString("assessment_date"));
                ddm.setVerifiedBy(rs.getString("verified_by"));
                ddm.setNotes(rs.getString("notes"));
                ddm.setRegDate(rs.getString("reg_date"));
                list.add(ddm);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching disaster damage records", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }
        return list;
    }

    @Override
    public boolean delete(DisasterDamageModel ddm) {
        String sql = "DELETE FROM beneficiary_disaster_damage WHERE beneficiary_disaster_damage_id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = dbConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, ddm.getBeneficiaryDisasterDamageId());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error occurred while deleting disaster damage", e);
            return false;
        } finally {
            ResourceUtils.closePreparedStatement(ps);
        }
    }

    @Override
    public boolean update(DisasterDamageModel ddm) {
        String sql = "UPDATE beneficiary_disaster_damage SET " +
                "beneficiary_id = ?, " +
                "disaster_id = ?, " +
                "house_damage_severity = ?, " +
                "assessment_date = ?, " +
                "verified_by = ?, " +
                "notes = ?, " +
                "reg_date = ? " +
                "WHERE beneficiary_disaster_damage_id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = dbConnection.getConnection();
            ps = conn.prepareStatement(sql);

            ps.setInt(1, ddm.getBeneficiaryId());
            ps.setInt(2, ddm.getDisasterId());
            ps.setString(3, ddm.getHouseDamageSeverity());
            ps.setString(4, ddm.getAssessmentDate());
            ps.setString(5, ddm.getVerifiedBy());
            ps.setString(6, ddm.getNotes());
            ps.setString(7, ddm.getRegDate());
            ps.setInt(8, ddm.getBeneficiaryDisasterDamageId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating disaster damage", e);
            return false;
        } finally {
            ResourceUtils.closePreparedStatement(ps);
        }
    }

    @Override
    public DisasterDamageModel getById(int id) {
        DisasterDamageModel ddm = null;
        String sql = "SELECT dmd.beneficiary_disaster_damage_id, " +
                "dmd.beneficiary_id, " +
                "b.first_name AS beneficiary_firstname, " +
                "dmd.disaster_id, " +
                "d.type AS disaster_type, " +
                "d.name AS disaster_name, " +
                "dmd.house_damage_severity, " +
                "dmd.assessment_date, " +
                "dmd.verified_by, " +
                "dmd.notes, " +
                "dmd.reg_date " +
                "FROM beneficiary_disaster_damage dmd " +
                "INNER JOIN beneficiary b ON dmd.beneficiary_id = b.beneficiary_id " +
                "INNER JOIN disaster d ON dmd.disaster_id = d.disaster_id " +
                "WHERE dmd.beneficiary_disaster_damage_id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = dbConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            rs = ps.executeQuery();

            if (rs.next()) {
                ddm = new DisasterDamageModel();
                ddm.setBeneficiaryDisasterDamageId(rs.getInt("beneficiary_disaster_damage_id"));
                ddm.setBeneficiaryId(rs.getInt("beneficiary_id"));
                ddm.setBeneficiaryFirstname(rs.getString("beneficiary_firstname"));
                ddm.setDisasterId(rs.getInt("disaster_id"));
                ddm.setDisasterType(rs.getString("disaster_type"));
                ddm.setDisasterName(rs.getString("disaster_name"));
                ddm.setHouseDamageSeverity(rs.getString("house_damage_severity"));
                ddm.setAssessmentDate(rs.getString("assessment_date"));
                ddm.setVerifiedBy(rs.getString("verified_by"));
                ddm.setNotes(rs.getString("notes"));
                ddm.setRegDate(rs.getString("reg_date"));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching disaster damage by ID", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }
        return ddm;
    }

    @Override
    public List<BeneficiaryModel> getAllBeneficiaryByFirstname() {
        List<BeneficiaryModel> list = new ArrayList<>();
        String sql = "SELECT beneficiary_id, first_name FROM beneficiary";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = dbConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new BeneficiaryModel(
                        rs.getInt("beneficiary_id"),
                        rs.getString("first_name")
                ));
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching beneficiaries", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }
        return list;
    }

    @Override
    public List<DisasterModel> getAllDisasterTypeAndName() {
        List<DisasterModel> list = new ArrayList<>();
        String sql = "SELECT disaster_id, type, name FROM disaster";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = dbConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new DisasterModel(
                        rs.getInt("disaster_id"),
                        rs.getString("type"),
                        rs.getString("name")
                ));
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching disasters", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }
        return list;
    }
}
