package com.ionres.respondph.familymembers;

import com.ionres.respondph.common.model.BeneficiaryModel;
import com.ionres.respondph.database.DBConnection;
import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FamilyMemberDAOImpl implements FamilyMemberDAO {
    private final DBConnection dbConnection;

    public FamilyMemberDAOImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public boolean saving(FamilyMembersModel fm) {
        String sql = "INSERT INTO family_member (first_name, middle_name, last_name, relationshiptobene,birthdate, age_score, gender, marital_status, disability_type, health_condition, employment_status, education_level, beneficiary_id, notes, reg_date)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fm.getFirstName());
            ps.setString(2,fm.getMiddleName());
            ps.setString(3, fm.getLastName());
            ps.setString(4,fm.getRelationshipToBeneficiary());
            ps.setString(5, fm.getBirthDate());
            ps.setDouble(6, fm.getAgeScore());  // Age score
            ps.setString(7, fm.getGender());
            ps.setString(8, fm.getMaritalStatus());
            ps.setString(9, fm.getDisabilityType());
            ps.setString(10, fm.getHealthCondition());
            ps.setString(11, fm.getEmploymentStatus());
            ps.setString(12, fm.getEducationalLevel());
            ps.setInt(13, fm.getBeneficiaryId());
            ps.setString(14, fm.getNotes());
            ps.setString(15, fm.getRegDate());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<FamilyMembersModel> getAll() {

        List<FamilyMembersModel> list = new ArrayList<>();

        String sql = "SELECT fm.familymember_id, fm.beneficiary_id, fm.first_name, fm.middle_name, fm.last_name, " +
                "fm.relationshiptobene, fm.birthdate, fm.gender, fm.marital_status, " +
                "fm.notes, fm.reg_date, b.first_name AS beneficiary_firstname " +
                "FROM family_member fm " +
                "INNER JOIN beneficiary b " +
                "ON fm.beneficiary_id = b.beneficiary_id";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                FamilyMembersModel fm = new FamilyMembersModel();

                fm.setFamilyId(rs.getInt("familymember_id"));
                fm.setBeneficiaryId(rs.getInt("beneficiary_id"));

                fm.setFirstName(rs.getString("first_name"));
                fm.setMiddleName(rs.getString("middle_name"));
                fm.setLastName(rs.getString("last_name"));
                fm.setRelationshipToBeneficiary(rs.getString("relationshiptobene"));
                fm.setBirthDate(rs.getString("birthdate"));
                fm.setGender(rs.getString("gender"));
                fm.setMaritalStatus(rs.getString("marital_status"));
                fm.setNotes(rs.getString("notes"));
                fm.setRegDate(rs.getString("reg_date"));
                fm.setBeneficiaryName(rs.getString("beneficiary_firstname"));

                list.add(fm);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "Error fetching family members: " + ex.getMessage()
            );
        }
        return list;
    }


    @Override
    public boolean delete(FamilyMembersModel fm) {
        String sql = "DELETE FROM family_member WHERE familymember_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, fm.getFamilyId());


            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(FamilyMembersModel fm) {
        String sql = "UPDATE family_member SET " +
                "first_name = ?, middle_name = ?, last_name = ?, relationshiptobene = ?, birthdate = ?, age_score = ?, " +
                "gender = ?, marital_status = ?, disability_type = ?, health_condition = ?, " +
                "employment_status = ?, education_level = ?, notes = ?, reg_date = ?" +
                "WHERE familymember_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fm.getFirstName());
            ps.setString(2, fm.getMiddleName());
            ps.setString(3, fm.getLastName());
            ps.setString(4, fm.getRelationshipToBeneficiary());
            ps.setString(5, fm.getBirthDate());
            ps.setDouble(6, fm.getAgeScore());
            ps.setString(7, fm.getGender());
            ps.setString(8, fm.getMaritalStatus());
            ps.setString(9, fm.getDisabilityType());
            ps.setString(10, fm.getHealthCondition());
            ps.setString(11, fm.getEmploymentStatus());
            ps.setString(12, fm.getEducationalLevel());
            ps.setString(13, fm.getNotes());
            ps.setString(14, fm.getRegDate());
            ps.setInt(15, fm.getFamilyId());


            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public FamilyMembersModel getById(int id) {
        FamilyMembersModel fm = null;
        String sql = "SELECT * FROM family_member WHERE familymember_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    fm = new FamilyMembersModel();
                    fm.setFamilyId(rs.getInt("familymember_id"));
                    fm.setBeneficiaryId(rs.getInt("beneficiary_id"));
                    fm.setFirstName(rs.getString("first_name"));
                    fm.setMiddleName(rs.getString("middle_name"));
                    fm.setLastName(rs.getString("last_name"));
                    fm.setRelationshipToBeneficiary(rs.getString("relationshiptobene"));
                    fm.setBirthDate(rs.getString("birthdate"));
                    fm.setGender(rs.getString("gender"));
                    fm.setMaritalStatus(rs.getString("marital_status"));
                    fm.setDisabilityType(rs.getString("disability_type"));
                    fm.setHealthCondition(rs.getString("health_condition"));
                    fm.setEmploymentStatus(rs.getString("employment_status"));
                    fm.setEducationalLevel(rs.getString("education_level"));
                    fm.setNotes(rs.getString("notes"));
                    fm.setRegDate(rs.getString("reg_date"));
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(null, "Error fetching beneficiary: " + ex.getMessage());
        }

        return fm;
    }

    @Override
    public List<BeneficiaryModel> getAllBeneficiaryByFirstname() {

        List<BeneficiaryModel> list = new ArrayList<>();
        String sql = "SELECT beneficiary_id, first_name, middle_name, last_name FROM beneficiary";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new BeneficiaryModel(
                        rs.getInt("beneficiary_id"),
                        rs.getString("first_name"),
                        rs.getString("middle_name"),
                        rs.getString("last_name")
                ));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error fetching beneficiaries", e);
        }
        return list;
    }
}