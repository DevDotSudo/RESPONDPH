package com.ionres.respondph.beneficiary;

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

public class BeneficiaryDAOImpl implements BeneficiaryDAO {
    private static final Logger LOGGER = Logger.getLogger(BeneficiaryDAOImpl.class.getName());
    private final DBConnection dbConnection;

    public BeneficiaryDAOImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public boolean saving(BeneficiaryModel bm) {
        String sql = "INSERT INTO beneficiary (first_name, middle_name, last_name, birthdate, barangay, age_score, gender, " +
                "marital_status, solo_parent_status, latitude, longitude, mobile_number, disability_type, " +
                "health_condition, clean_water_access, sanitation_facility, house_type, ownership_status, " +
                "employment_status, monthly_income, education_level, digital_access, added_by, reg_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, bm.getFirstname());
            ps.setString(2, bm.getMiddlename());
            ps.setString(3, bm.getLastname());
            ps.setString(4, bm.getBirthDate());
            ps.setString(5, bm.getBarangay());
            ps.setDouble(6, bm.getAgeScore());
            ps.setString(7, bm.getGender());
            ps.setString(8, bm.getMaritalStatus());
            ps.setString(9, bm.getSoloParentStatus());
            ps.setString(10, bm.getLatitude());
            ps.setString(11, bm.getLongitude());
            ps.setString(12, bm.getMobileNumber());
            ps.setString(13, bm.getDisabilityType());
            ps.setString(14, bm.getHealthCondition());
            ps.setString(15, bm.getCleanWaterAccess());
            ps.setString(16, bm.getSanitationFacility());
            ps.setString(17, bm.getHouseType());
            ps.setString(18, bm.getOwnerShipStatus());
            ps.setString(19, bm.getEmploymentStatus());
            ps.setString(20, bm.getMonthlyIncome());
            ps.setString(21, bm.getEducationalLevel());
            ps.setString(22, bm.getDigitalAccess());
            ps.setString(23, bm.getAddedBy());
            ps.setString(24, bm.getRegDate());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error occurred while saving beneficiary", e);
            return false;
        }
    }

    @Override
    public List<BeneficiaryModel> getAll() {
        List<BeneficiaryModel> beneficiaries = new ArrayList<>();
        String sql = "SELECT beneficiary_id, first_name, middle_name, last_name, " +
                "birthdate, age_score, gender, marital_status, mobile_number, added_by, reg_date " +
                "FROM beneficiary";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                BeneficiaryModel bm = new BeneficiaryModel();

                bm.setId(rs.getInt("beneficiary_id"));
                bm.setFirstname(rs.getString("first_name"));
                bm.setMiddlename(rs.getString("middle_name"));
                bm.setLastname(rs.getString("last_name"));
                bm.setBirthDate(rs.getString("birthdate"));
                bm.setAgeScore(rs.getDouble("age_score"));
                bm.setGender(rs.getString("gender"));
                bm.setMaritalStatus(rs.getString("marital_status"));
                bm.setMobileNumber(rs.getString("mobile_number"));
                bm.setAddedBy(rs.getString("added_by"));
                bm.setRegDate(rs.getString("reg_date"));

                beneficiaries.add(bm);
            }

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error fetching beneficiaries", ex);
        }

        return beneficiaries;
    }

    @Override
    public boolean delete(BeneficiaryModel bm) {
        String sql = "DELETE FROM beneficiary WHERE beneficiary_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bm.getId());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error occurred while deleting beneficiary", e);
            return false;
        }
    }

    @Override
    public boolean update(BeneficiaryModel bm) {
        String sql = "UPDATE beneficiary SET " +
                "first_name = ?, middle_name = ?, last_name = ?, birthdate = ?, barangay = ?, age_score = ?, gender = ?, " +
                "marital_status = ?, solo_parent_status = ?, latitude = ?, longitude = ?, " +
                "mobile_number = ?, disability_type = ?, health_condition = ?, clean_water_access = ?, " +
                "sanitation_facility = ?, house_type = ?, ownership_status = ?, employment_status = ?, " +
                "monthly_income = ?, education_level = ?, digital_access = ?, added_by = ?, reg_date = ? " +
                "WHERE beneficiary_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, bm.getFirstname());
            ps.setString(2, bm.getMiddlename());
            ps.setString(3, bm.getLastname());
            ps.setString(4, bm.getBirthDate());
            ps.setString(5, bm.getBarangay());
            ps.setDouble(6, bm.getAgeScore());
            ps.setString(7, bm.getGender());
            ps.setString(8, bm.getMaritalStatus());
            ps.setString(9, bm.getSoloParentStatus());
            ps.setString(10, bm.getLatitude());
            ps.setString(11, bm.getLongitude());
            ps.setString(12, bm.getMobileNumber());
            ps.setString(13, bm.getDisabilityType());
            ps.setString(14, bm.getHealthCondition());
            ps.setString(15, bm.getCleanWaterAccess());
            ps.setString(16, bm.getSanitationFacility());
            ps.setString(17, bm.getHouseType());
            ps.setString(18, bm.getOwnerShipStatus());
            ps.setString(19, bm.getEmploymentStatus());
            ps.setString(20, bm.getMonthlyIncome());
            ps.setString(21, bm.getEducationalLevel());
            ps.setString(22, bm.getDigitalAccess());
            ps.setString(23, bm.getAddedBy());
            ps.setString(24, bm.getRegDate());
            ps.setInt(25, bm.getId());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error occurred while updating beneficiary", e);
            return false;
        }
    }

    @Override
    public BeneficiaryModel getById(int id) {
        BeneficiaryModel bm = null;
        String sql = "SELECT * FROM beneficiary WHERE beneficiary_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    bm = new BeneficiaryModel();

                    bm.setId(rs.getInt("beneficiary_id"));
                    bm.setFirstname(rs.getString("first_name"));
                    bm.setMiddlename(rs.getString("middle_name"));
                    bm.setLastname(rs.getString("last_name"));
                    bm.setBirthDate(rs.getString("birthdate"));
                    bm.setBarangay(rs.getString("barangay"));
                    bm.setAgeScore(rs.getDouble("age_score"));
                    bm.setGender(rs.getString("gender"));
                    bm.setMaritalStatus(rs.getString("marital_status"));
                    bm.setSoloParentStatus(rs.getString("solo_parent_status"));
                    bm.setLatitude(rs.getString("latitude"));
                    bm.setLongitude(rs.getString("longitude"));
                    bm.setMobileNumber(rs.getString("mobile_number"));
                    bm.setDisabilityType(rs.getString("disability_type"));
                    bm.setHealthCondition(rs.getString("health_condition"));
                    bm.setCleanWaterAccess(rs.getString("clean_water_access"));
                    bm.setSanitationFacility(rs.getString("sanitation_facility"));
                    bm.setHouseType(rs.getString("house_type"));
                    bm.setOwnerShipStatus(rs.getString("ownership_status"));
                    bm.setEmploymentStatus(rs.getString("employment_status"));
                    bm.setMonthlyIncome(rs.getString("monthly_income"));
                    bm.setEducationalLevel(rs.getString("education_level"));
                    bm.setDigitalAccess(rs.getString("digital_access"));
                    bm.setAddedBy(rs.getString("added_by"));
                    bm.setRegDate(rs.getString("reg_date"));
                }
            }

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error fetching beneficiary by ID", ex);
        }

        return bm;
    }

    @Override
    public List<String[]> getEncryptedLocations() {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT beneficiary_id, latitude, longitude FROM beneficiary";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new String[]{
                        rs.getString("beneficiary_id"),
                        rs.getString("latitude"),
                        rs.getString("longitude")
                });
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching encrypted locations", e);
        }
        return list;
    }
}