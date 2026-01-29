package com.ionres.respondph.aid_type;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AidTypeDAOImpl implements AidTypeDAO{
    private final DBConnection dbConnection;
    private Connection conn;
    private final Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");


    public AidTypeDAOImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;

    }

    @Override
    public List<AidTypeModelComboBox> findAll() {
        List<AidTypeModelComboBox> aidTypes = new ArrayList<>();
        String sql = "SELECT * FROM aid_type ORDER BY aid_name";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                AidTypeModelComboBox aidType = mapResultSetToAidType(rs);
                aidTypes.add(aidType);
            }

            rs.close();
            ps.close();

            System.out.println("Loaded " + aidTypes.size() + " aid types");

        } catch (Exception e) {
            System.err.println("Error fetching all aid types: " + e.getMessage());
            e.printStackTrace();
        }

        return aidTypes;
    }
    @Override
    public boolean saving(AidTypeModel atm) {
        String sql = "INSERT INTO aid_type (aid_name, age_weight, gender_weight, marital_status_weight, solo_parent_weight, disability_weight, health_condition_weight, access_to_clean_water_weight, sanitation_facilities_weight, house_construction_type_weight, ownership_weight, damage_severity_weight, employment_status_weight, monthly_income_weight, education_level_weight, digital_access_weight, dependency_ratio_weight, notes, admin_id, reg_date)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1,atm.getAidTypeName());
            ps.setDouble(2, atm.getAgeWeight());
            ps.setDouble(3, atm.getGenderWeight());
            ps.setDouble(4, atm.getMaritalStatusWeight());
            ps.setDouble(5, atm.getSoloParentWeight());
            ps.setDouble(6, atm.getDisabilityWeight());
            ps.setDouble(7, atm.getHealthConditionWeight());
            ps.setDouble(8, atm.getAccessToCleanWaterWeight());
            ps.setDouble(9, atm.getSanitationFacilityWeight());
            ps.setDouble(10, atm.getHouseConstructionTypeWeight());
            ps.setDouble(11, atm.getOwnershipWeight());
            ps.setDouble(12, atm.getDamageSeverityWeight());
            ps.setDouble(13, atm.getEmploymentStatusWeight());
            ps.setDouble(14, atm.getMonthlyIncomeWeight());
            ps.setDouble(15, atm.getEducationalLevelWeight());
            ps.setDouble(16, atm.getDigitalAccessWeight());
            ps.setDouble(17, atm.getDependencyRatioWeight());
            ps.setString(18, atm.getNotes());
            ps.setInt(19, atm.getAdminId());
            ps.setString(20, atm.getRegDate());

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
            }catch(SQLException e){
                System.out.println(e.getMessage());
            }
        }
    }

    @Override
    public List<AidTypeModel> getAll() {

        List<AidTypeModel> list = new ArrayList<>();

        String sql = "SELECT at.aid_type_id, at.aid_name, at.notes, at.reg_date, at.admin_id, " +
                "a.first_name AS admin_firstname " +
                "FROM aid_type at " +
                "INNER JOIN admin a " +
                "ON at.admin_id = a.admin_id";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                AidTypeModel at = new AidTypeModel();

                at.setAidTypeId(rs.getInt("aid_type_id"));         // You may need to add this setter
                at.setAidTypeName(rs.getString("aid_name"));
                at.setNotes(rs.getString("notes"));
                at.setRegDate(rs.getString("reg_date"));
                at.setAdminId(rs.getInt("admin_id"));

                at.setAdminName(rs.getString("admin_firstname")); // You need a new field + setter in AidTypeModel

                list.add(at);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "Error fetching aid types: " + ex.getMessage()
            );
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        return list;
    }

    @Override
    public boolean delete(AidTypeModel atm) {
        String sql = "DELETE FROM aid_type WHERE aid_type_id = ?";

        try {conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, atm.getAidTypeId());

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
            }catch(SQLException e){
                System.out.println(e.getMessage());
            }
        }
    }

    @Override
    public AidTypeModel getById(int id) {
        AidTypeModel at = null;

        String sql = "SELECT * FROM aid_type WHERE aid_type_id = ?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                at = new AidTypeModel();

                at.setAidTypeId(rs.getInt("aid_type_id"));
                at.setAidTypeName(rs.getString("aid_name"));
                at.setAgeWeight(rs.getDouble("age_weight"));
                at.setGenderWeight(rs.getDouble("gender_weight"));
                at.setMaritalStatusWeight(rs.getDouble("marital_status_weight"));
                at.setSoloParentWeight(rs.getDouble("solo_parent_weight"));
                at.setDisabilityWeight(rs.getDouble("disability_weight"));
                at.setHealthConditionWeight(rs.getDouble("health_condition_weight"));
                at.setAccessToCleanWaterWeight(rs.getDouble("access_to_clean_water_weight"));
                at.setSanitationFacilityWeight(rs.getDouble("sanitation_facilities_weight"));
                at.setHouseConstructionTypeWeight(rs.getDouble("house_construction_type_weight"));
                at.setOwnershipWeight(rs.getDouble("ownership_weight"));
                at.setDamageSeverityWeight(rs.getDouble("damage_severity_weight"));
                at.setEmploymentStatusWeight(rs.getDouble("employment_status_weight"));
                at.setMonthlyIncomeWeight(rs.getDouble("monthly_income_weight"));
                at.setEducationalLevelWeight(rs.getDouble("education_level_weight"));
                at.setDigitalAccessWeight(rs.getDouble("digital_access_weight"));
                at.setDependencyRatioWeight(rs.getDouble("dependency_ratio_weight"));
                at.setNotes(rs.getString("notes"));
                at.setRegDate(rs.getString("reg_date"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error fetching aid type: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        return at;
    }



    @Override
    public boolean update(AidTypeModel atm) {

        String sql = "UPDATE aid_type SET " +
                "aid_name = ?, " +
                "age_weight = ?, " +
                "gender_weight = ?, " +
                "marital_status_weight = ?, " +
                "solo_parent_weight = ?, " +
                "disability_weight = ?, " +
                "health_condition_weight = ?, " +
                "access_to_clean_water_weight = ?, " +
                "sanitation_facilities_weight = ?, " +
                "house_construction_type_weight = ?, " +
                "ownership_weight = ?, " +
                "damage_severity_weight = ?, " +
                "employment_status_weight = ?, " +
                "monthly_income_weight = ?, " +
                "education_level_weight = ?, " +
                "digital_access_weight = ?, " +
                "dependency_ratio_weight = ?, " +
                "notes = ?, " +
                "admin_id = ?, " +
                "reg_date = ? " +
                "WHERE aid_type_id = ?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, atm.getAidTypeName());
            ps.setDouble(2, atm.getAgeWeight());
            ps.setDouble(3, atm.getGenderWeight());
            ps.setDouble(4, atm.getMaritalStatusWeight());
            ps.setDouble(5, atm.getSoloParentWeight());
            ps.setDouble(6, atm.getDisabilityWeight());
            ps.setDouble(7, atm.getHealthConditionWeight());
            ps.setDouble(8, atm.getAccessToCleanWaterWeight());
            ps.setDouble(9, atm.getSanitationFacilityWeight());
            ps.setDouble(10, atm.getHouseConstructionTypeWeight());
            ps.setDouble(11, atm.getOwnershipWeight());
            ps.setDouble(12, atm.getDamageSeverityWeight());
            ps.setDouble(13, atm.getEmploymentStatusWeight());
            ps.setDouble(14, atm.getMonthlyIncomeWeight());
            ps.setDouble(15, atm.getEducationalLevelWeight());
            ps.setDouble(16, atm.getDigitalAccessWeight());
            ps.setDouble(17, atm.getDependencyRatioWeight());
            ps.setString(18, atm.getNotes());
            ps.setInt(19, atm.getAdminId());
            ps.setString(20, atm.getRegDate());
            ps.setInt(21, atm.getAidTypeId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Database error occurred: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;

        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Override
    public AidTypeModelComboBox mapResultSetToAidType(ResultSet rs) throws Exception {
        AidTypeModelComboBox aidType = new AidTypeModelComboBox();

        aidType.setAidTypeId(rs.getInt("aid_type_id"));

        String encryptedName = rs.getString("aid_name");
        aidType.setAidName(cs.decryptWithOneParameter(encryptedName));

        String encryptedNotes = rs.getString("notes");
        if (encryptedNotes != null && !encryptedNotes.isEmpty()) {
            aidType.setNotes(cs.decryptWithOneParameter(encryptedNotes));
        }

        aidType.setCreatedByAdminId(rs.getInt("admin_id"));


        return aidType;
    }

    @Override
    public List<Integer> getAllAidTypeIds() {
        List<Integer> aidTypeIds = new ArrayList<>();
        String sql = "SELECT aid_type_id FROM aid_type";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
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
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        return aidTypeIds;
    }

    @Override
    public boolean hasAnyAidTypes() {
        String sql = "SELECT COUNT(*) as count FROM aid_type";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int count = rs.getInt("count");
                rs.close();
                ps.close();
                return count > 0;
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.err.println("Error checking for aid types: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        return false;
    }





}