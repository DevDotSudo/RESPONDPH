package com.ionres.respondph.familymembers;

import com.ionres.respondph.common.model.BeneficiaryModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;
import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FamilyMemberDAOImpl implements FamilyMemberDAO {
    Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");
    private final DBConnection dbConnection;
    private Connection conn;

    public FamilyMemberDAOImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public boolean saving(FamilyMembersModel fm) {
        String sql = "INSERT INTO family_member (first_name, middle_name, last_name, relationshiptobene,birthdate, gender, marital_status, disability_type, health_condition, employment_status, education_level, beneficiary_id, notes, reg_date)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, fm.getFirstName());
            ps.setString(2,fm.getMiddleName());
            ps.setString(3, fm.getLastName());
            ps.setString(4,fm.getRelationshipToBeneficiary());
            ps.setString(5, fm.getBirthDate());
            ps.setString(6,fm.getGender());
            ps.setString(7, fm.getMaritalStatus());
            ps.setString(8,fm.getDisabilityType());
            ps.setString(9, fm.getHealthCondition());
            ps.setString(10,fm.getEmploymentStatus());
            ps.setString(11, fm.getEducationalLevel());
            ps.setInt(12,fm.getBeneficiaryId());
            ps.setString(13, fm.getNotes());
            ps.setString(14,fm.getRegDate());

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
    public List<FamilyMembersModel> getAll() {

        List<FamilyMembersModel> list = new ArrayList<>();

        String sql = "SELECT fm.familymember_id, fm.first_name, fm.middle_name, fm.last_name, " +
                "fm.relationshiptobene, fm.birthdate, fm.gender, fm.marital_status, " +
                "fm.notes, fm.reg_date, b.first_name AS beneficiary_firstname " +
                "FROM family_member fm " +
                "INNER JOIN beneficiary b " +
                "ON fm.beneficiary_id = b.beneficiary_id";

        try {
            conn =  dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                FamilyMembersModel fm = new FamilyMembersModel();

                fm.setFamilyId(rs.getInt("familymember_id"));
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
        finally {
            try {
                conn.close();
            }catch(SQLException e){
                System.out.println(e.getMessage());
            }
        }
        return list;
    }


    @Override
    public boolean delete(FamilyMembersModel fm) {
        String sql = "DELETE FROM family_member WHERE familymember_id = ?";

        try {conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, fm.getFamilyId());


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
    public boolean update(FamilyMembersModel fm) {
        String sql = "UPDATE family_member SET " +
                "first_name = ?, middle_name = ?, last_name = ?, relationshiptobene = ?, birthdate = ?, " +
                "gender = ?, marital_status = ?, disability_type = ?, health_condition = ?, " +
                "employment_status = ?, education_level = ?, notes = ?, reg_date = ?" +
                "WHERE familymember_id = ?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, fm.getFirstName());
            ps.setString(2, fm.getMiddleName());
            ps.setString(3, fm.getLastName());
            ps.setString(4, fm.getRelationshipToBeneficiary());
            ps.setString(5, fm.getBirthDate());
            ps.setString(6, fm.getGender());
            ps.setString(7, fm.getMaritalStatus());
            ps.setString(8, fm.getDisabilityType());
            ps.setString(9, fm.getHealthCondition());
            ps.setString(10, fm.getEmploymentStatus());
            ps.setString(11, fm.getEducationalLevel());
            ps.setString(12, fm.getNotes());
            ps.setString(13, fm.getRegDate());
            ps.setInt(14, fm.getFamilyId());


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
    public FamilyMembersModel getById(int id) {
        FamilyMembersModel fm = null;
        String sql = "SELECT * FROM family_member WHERE familymember_id = ?";

        try {
            conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                fm = new FamilyMembersModel();

                List<String> encrypted = new ArrayList<>();
                encrypted.add(rs.getString("first_name"));
                encrypted.add(rs.getString("middle_name"));
                encrypted.add(rs.getString("last_name"));
                encrypted.add(rs.getString("relationshiptobene"));
                encrypted.add(rs.getString("birthdate"));
                encrypted.add(rs.getString("gender"));
                encrypted.add(rs.getString("marital_status"));
                encrypted.add(rs.getString("disability_type"));
                encrypted.add(rs.getString("health_condition"));
                encrypted.add(rs.getString("employment_status"));
                encrypted.add(rs.getString("education_level"));
                encrypted.add(rs.getString("notes"));
                encrypted.add(rs.getString("reg_date"));

                List<String> decrypted = cs.decrypt(encrypted);

                fm.setFamilyId(rs.getInt("familymember_id"));
                fm.setFirstName(decrypted.get(0));
                fm.setMiddleName(decrypted.get(1));
                fm.setLastName(decrypted.get(2));
                fm.setRelationshipToBeneficiary(decrypted.get(3));
                fm.setBirthDate(decrypted.get(4));
                fm.setGender(decrypted.get(5));
                fm.setMaritalStatus(decrypted.get(6));
                fm.setDisabilityType(decrypted.get(7));
                fm.setHealthCondition(decrypted.get(8));
                fm.setEmploymentStatus(decrypted.get(9));
                fm.setEducationalLevel(decrypted.get(10));
                fm.setNotes(decrypted.get(11));
                fm.setRegDate(decrypted.get(12));

            }

        } catch (Exception ex) {
            ex.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(null, "Error fetching beneficiary: " + ex.getMessage());
        }
        finally {
            try {
                conn.close();
            }catch(SQLException e){
                System.out.println(e.getMessage());
            }
        }

        return fm;
    }

    @Override
    public List<BeneficiaryModel> getAllBeneficiaryByFirstname() {

        List<BeneficiaryModel> list = new ArrayList<>();
        String sql = "SELECT beneficiary_id, first_name FROM beneficiary";

        try {
            conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new BeneficiaryModel(
                        rs.getInt("beneficiary_id"),
                        rs.getString("first_name")
                ));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error fetching beneficiaries", e);
        }
        finally {
            try {
                conn.close();
            }catch(SQLException e){
                System.out.println(e.getMessage());
            }
        }
        return list;
    }
}
