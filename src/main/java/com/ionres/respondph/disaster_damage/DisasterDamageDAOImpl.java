package com.ionres.respondph.disaster_damage;

import com.ionres.respondph.common.model.BeneficiaryModel;
import com.ionres.respondph.common.model.DisasterModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.DisasterDamageUpdateHandler;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DisasterDamageDAOImpl implements  DisasterDamageDAO {
    private final DBConnection dbConnection;
    private Connection conn;
    public DisasterDamageDAOImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public boolean saving(DisasterDamageModel ddm) {
        String sql = "INSERT INTO beneficiary_disaster_damage (beneficiary_id, disaster_id, house_damage_severity, assessment_date, verified_by, notes, reg_date)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?)";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, ddm.getBeneficiaryId());
            ps.setInt(2, ddm.getDisasterId());
            ps.setString(3, ddm.getHouseDamageSeverity());
            ps.setString(4, ddm.getAssessmentDate());
            ps.setString(5, ddm.getVerifiedBy());
            ps.setString(6, ddm.getNotes());
            ps.setString(7, ddm.getRegDate());

            int rowsAffected = ps.executeUpdate();
            ps.close();

            if (rowsAffected > 0) {
                System.out.println("Disaster damage inserted successfully");

                DisasterDamageUpdateHandler updateHandler = new DisasterDamageUpdateHandler();
                boolean scoresUpdated = updateHandler.updateDamageSeverityScores(
                        ddm.getBeneficiaryId(), ddm.getDisasterId()
                );

                if (scoresUpdated) {
                    System.out.println("✓ Damage severity scores updated automatically");
                } else {
                    System.err.println("⚠ Warning: Damage inserted but scores not updated");
                }

                return true;
            }

            return false;

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
    public List<DisasterDamageModel> getAll() {
        List<DisasterDamageModel> list = new ArrayList<>();
        String sql =
                "SELECT dmd.beneficiary_disaster_damage_id, " +
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
                        "INNER JOIN beneficiary b " +
                        "   ON dmd.beneficiary_id = b.beneficiary_id " +
                        "INNER JOIN disaster d " +
                        "   ON dmd.disaster_id = d.disaster_id";

        try  {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                DisasterDamageModel ddm = new DisasterDamageModel();

                ddm.setBeneficiaryDisasterDamageId(
                        rs.getInt("beneficiary_disaster_damage_id")
                );

                ddm.setBeneficiaryId(
                        rs.getInt("beneficiary_id")
                );

                ddm.setBeneficiaryFirstname(
                        rs.getString("beneficiary_firstname")
                );

                ddm.setDisasterId(
                        rs.getInt("disaster_id")
                );

                ddm.setDisasterType(
                        rs.getString("disaster_type")
                );

                ddm.setDisasterName(
                        rs.getString("disaster_name")
                );

                ddm.setHouseDamageSeverity(
                        rs.getString("house_damage_severity")
                );

                ddm.setAssessmentDate(
                        rs.getString("assessment_date")
                );

                ddm.setVerifiedBy(
                        rs.getString("verified_by")
                );

                ddm.setNotes(
                        rs.getString("notes")
                );

                ddm.setRegDate(
                        rs.getString("reg_date")
                );

                list.add(ddm);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Error fetching disaster damage records: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
        finally {
            try {
                conn.close();
            }
            catch (SQLException e) {
                System.out.println("Error: " +  e.getMessage());
            }
        }

        return list;
    }

    @Override
    public boolean delete(DisasterDamageModel ddm) {
        String sql = "DELETE FROM beneficiary_disaster_damage WHERE beneficiary_disaster_damage_id = ?";

        try {
            conn = dbConnection.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, ddm.getBeneficiaryDisasterDamageId());

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
    public boolean update(DisasterDamageModel ddm) {

        String sql = "UPDATE beneficiary_disaster_damage SET " +
                "beneficiary_id = ?, " +
                "disaster_id = ?, " +
                "house_damage_severity = ?, " +
                "assessment_date = ?, " +
                "verified_by = ?, " +
                "notes = ?, " +
                "reg_date = ?"+
                "WHERE beneficiary_disaster_damage_id = ?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, ddm.getBeneficiaryId());
            ps.setInt(2, ddm.getDisasterId());
            ps.setString(3, ddm.getHouseDamageSeverity());
            ps.setString(4, ddm.getAssessmentDate());
            ps.setString(5, ddm.getVerifiedBy());
            ps.setString(6, ddm.getNotes());
            ps.setString(7, ddm.getRegDate());
            ps.setInt(8, ddm.getBeneficiaryDisasterDamageId());

            int rowsAffected = ps.executeUpdate();
            ps.close();

            if (rowsAffected > 0) {
                System.out.println("Disaster damage updated successfully");

                DisasterDamageUpdateHandler updateHandler = new DisasterDamageUpdateHandler();
                boolean scoresUpdated = updateHandler.updateDamageSeverityScores(
                        ddm.getBeneficiaryId(), ddm.getDisasterId()
                );

                if (scoresUpdated) {
                    System.out.println("✓ Damage severity scores updated automatically");
                } else {
                    System.err.println("⚠ Warning: Damage updated but scores not recalculated");
                }

                return true;
            }

            return false;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Error updating disaster damage: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
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
    public DisasterDamageModel getById(int id) {
        DisasterDamageModel ddm = null;

        String sql =
                "SELECT dmd.beneficiary_disaster_damage_id, " +
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

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                ddm = new DisasterDamageModel();

                ddm.setBeneficiaryDisasterDamageId(
                        rs.getInt("beneficiary_disaster_damage_id")
                );
                ddm.setBeneficiaryId(
                        rs.getInt("beneficiary_id")
                );
                ddm.setBeneficiaryFirstname(
                        rs.getString("beneficiary_firstname")
                );
                ddm.setDisasterId(
                        rs.getInt("disaster_id")
                );
                ddm.setDisasterType(
                        rs.getString("disaster_type")
                );
                ddm.setDisasterName(
                        rs.getString("disaster_name")
                );
                ddm.setHouseDamageSeverity(
                        rs.getString("house_damage_severity")
                );
                ddm.setAssessmentDate(
                        rs.getString("assessment_date")
                );
                ddm.setVerifiedBy(
                        rs.getString("verified_by")
                );
                ddm.setNotes(
                        rs.getString("notes")
                );
                ddm.setRegDate(
                        rs.getString("reg_date")
                );
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Error fetching disaster damage: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
        }
        finally {
            try {
                conn.close();
            }
            catch (SQLException e) {
                System.out.println("Error: " +  e.getMessage());
            }
        }

        return ddm;
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
            }
            catch (SQLException e) {
                System.out.println("Error: " +  e.getMessage());
            }
        }

        return list;
    }

    @Override
    public List<DisasterModel> getAllDisasterTypeAndName() {
        List<DisasterModel> list = new ArrayList<>();
        String sql = "SELECT disaster_id, type, name FROM disaster";

        try {
            conn = dbConnection.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new DisasterModel(
                        rs.getInt("disaster_id"),
                        rs.getString("type"),
                        rs.getString("name")
                ));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error fetching Disaster", e);
        }
        finally {
            try {
                conn.close();
            }
            catch (SQLException e) {
                System.out.println("Error: " +  e.getMessage());
            }
        }
        return list;
    }
}