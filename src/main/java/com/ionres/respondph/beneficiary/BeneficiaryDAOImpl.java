package com.ionres.respondph.beneficiary;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;
import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BeneficiaryDAOImpl implements  BeneficiaryDAO{
    private final DBConnection dbConnection;
    private Connection conn;

    Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

    public BeneficiaryDAOImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public boolean saving(BeneficiaryModel bm) {

        String sql = "INSERT INTO beneficiary (first_name, middle_name, last_name, birthdate, gender, marital_status, solo_parent_status, latitude, longitude, mobile_number, disability_type, health_condition, clean_water_access, sanitation_facility, house_type, ownership_status, employment_status, monthly_income, education_level, digital_access, added_by, reg_date)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?)";

        try {
            conn = dbConnection.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, bm.getFirstname());
            ps.setString(2, bm.getMiddlename());
            ps.setString(3, bm.getLastname());
            ps.setString(4, bm.getBirthDate());
            ps.setString(5, bm.getGender());
            ps.setString(6, bm.getMaritalStatus());
            ps.setString(7, bm.getSoloParentStatus());
            ps.setString(8, bm.getLatitude());
            ps.setString(9, bm.getLongitude());
            ps.setString(10, bm.getMobileNumber());
            ps.setString(11, bm.getDisabilityType());
            ps.setString(12, bm.getHealthCondition());
            ps.setString(13, bm.getCleanWaterAccess());
            ps.setString(14, bm.getSanitationFacility());
            ps.setString(15, bm.getHouseType());
            ps.setString(16, bm.getOwnerShipStatus());
            ps.setString(17, bm.getEmploymentStatus());
            ps.setString(18, bm.getMonthlyIncome());
            ps.setString(19, bm.getEducationalLevel());
            ps.setString(20, bm.getDigitalAccess());
            ps.setString(21, bm.getAddedBy());
            ps.setString(22, bm.getRegDate());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
        finally {
            try {
                conn.close();
            }
            catch (SQLException e) {
                System.out.println("Error: " +  e.getMessage());
            }
        }
    }

    @Override
    public List<BeneficiaryModel> getAll() {

        List<BeneficiaryModel> beneficiaries = new ArrayList<>();
        String sql = "SELECT beneficiary_id, first_name, middle_name, last_name, " +
                "birthdate, gender, marital_status, mobile_number, added_by, reg_date " +
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
                bm.setGender(rs.getString("gender"));
                bm.setMaritalStatus(rs.getString("marital_status"));
                bm.setMobileNumber(rs.getString("mobile_number"));
                bm.setAddedBy(rs.getString("added_by"));
                bm.setRegDate(rs.getString("reg_date"));

                beneficiaries.add(bm);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                conn.close();
            }
            catch (SQLException e) {
                System.out.println("Error: " +  e.getMessage());
            }
        }

        return beneficiaries;
    }




    @Override
    public boolean delete(BeneficiaryModel bm) {
        String sql = "DELETE FROM beneficiary WHERE beneficiary_id = ?";

        try {
            Connection conn = dbConnection.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, bm.getId());


            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
        finally {
            try {
                conn.close();
            }
            catch (SQLException e) {
                System.out.println("Error: " +  e.getMessage());
            }
        }
    }

    @Override
    public boolean update(BeneficiaryModel bm) {
        String sql = "UPDATE beneficiary SET " +
                "first_name = ?, middle_name = ?, last_name = ?, birthdate = ?, gender = ?, " +
                "marital_status = ?, solo_parent_status = ?, latitude = ?, longitude = ?, " +
                "mobile_number = ?, disability_type = ?, health_condition = ?, clean_water_access = ?, " +
                "sanitation_facility = ?, house_type = ?, ownership_status = ?, employment_status = ?, " +
                "monthly_income = ?, education_level = ?, digital_access = ?, added_by = ?, reg_date = ? " +
                "WHERE beneficiary_id = ?";

        try {
            Connection conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, bm.getFirstname());
            ps.setString(2, bm.getMiddlename());
            ps.setString(3, bm.getLastname());
            ps.setString(4, bm.getBirthDate());
            ps.setString(5, bm.getGender());
            ps.setString(6, bm.getMaritalStatus());
            ps.setString(7, bm.getSoloParentStatus());
            ps.setString(8, bm.getLatitude());
            ps.setString(9, bm.getLongitude());
            ps.setString(10, bm.getMobileNumber());
            ps.setString(11, bm.getDisabilityType());
            ps.setString(12, bm.getHealthCondition());
            ps.setString(13, bm.getCleanWaterAccess());
            ps.setString(14, bm.getSanitationFacility());
            ps.setString(15, bm.getHouseType());
            ps.setString(16, bm.getOwnerShipStatus());
            ps.setString(17, bm.getEmploymentStatus());
            ps.setString(18, bm.getMonthlyIncome());
            ps.setString(19, bm.getEducationalLevel());
            ps.setString(20, bm.getDigitalAccess());
            ps.setString(21, bm.getAddedBy());
            ps.setString(22, bm.getRegDate());
            ps.setInt(23, bm.getId());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
        finally {
            try {
                conn.close();
            }
            catch (SQLException e) {
                System.out.println("Error: " +  e.getMessage());
            }
        }
    }

    @Override
    public BeneficiaryModel getById(int id) {
        BeneficiaryModel bm = null;
        String sql = "SELECT * FROM beneficiary WHERE beneficiary_id = ?";

        try {
            Connection conn = dbConnection.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                bm = new BeneficiaryModel();

                List<String> encrypted = new ArrayList<>();
                encrypted.add(rs.getString("first_name"));
                encrypted.add(rs.getString("middle_name"));
                encrypted.add(rs.getString("last_name"));
                encrypted.add(rs.getString("birthdate"));
                encrypted.add(rs.getString("gender"));
                encrypted.add(rs.getString("marital_status"));
                encrypted.add(rs.getString("solo_parent_status"));
                encrypted.add(rs.getString("latitude"));
                encrypted.add(rs.getString("longitude"));
                encrypted.add(rs.getString("mobile_number"));
                encrypted.add(rs.getString("disability_type"));
                encrypted.add(rs.getString("health_condition"));
                encrypted.add(rs.getString("clean_water_access"));
                encrypted.add(rs.getString("sanitation_facility"));
                encrypted.add(rs.getString("house_type"));
                encrypted.add(rs.getString("ownership_status"));
                encrypted.add(rs.getString("employment_status"));
                encrypted.add(rs.getString("monthly_income"));
                encrypted.add(rs.getString("education_level"));
                encrypted.add(rs.getString("digital_access"));
                encrypted.add(rs.getString("added_by"));
                encrypted.add(rs.getString("reg_date"));

                List<String> decrypted = cs.decrypt(encrypted);

                bm.setId(rs.getInt("beneficiary_id"));
                bm.setFirstname(decrypted.get(0));
                bm.setMiddlename(decrypted.get(1));
                bm.setLastname(decrypted.get(2));
                bm.setBirthDate(decrypted.get(3));
                bm.setGender(decrypted.get(4));
                bm.setMaritalStatus(decrypted.get(5));
                bm.setSoloParentStatus(decrypted.get(6));
                bm.setLatitude(decrypted.get(7));
                bm.setLongitude(decrypted.get(8));
                bm.setMobileNumber(decrypted.get(9));
                bm.setDisabilityType(decrypted.get(10));
                bm.setHealthCondition(decrypted.get(11));
                bm.setCleanWaterAccess(decrypted.get(12));
                bm.setSanitationFacility(decrypted.get(13));
                bm.setHouseType(decrypted.get(14));
                bm.setOwnerShipStatus(decrypted.get(15));
                bm.setEmploymentStatus(decrypted.get(16));
                bm.setMonthlyIncome(decrypted.get(17));
                bm.setEducationalLevel(decrypted.get(18));
                bm.setDigitalAccess(decrypted.get(19));
                bm.setAddedBy(decrypted.get(20));
                bm.setRegDate(decrypted.get(21));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(null, "Error fetching beneficiary: " + ex.getMessage());
        }
        finally {
            try {
                conn.close();
            }
            catch (SQLException e) {
                System.out.println("Error: " +  e.getMessage());
            }
        }

        return bm;
    }
}