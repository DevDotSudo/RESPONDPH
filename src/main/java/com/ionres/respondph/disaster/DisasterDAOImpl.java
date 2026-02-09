package com.ionres.respondph.disaster;

import com.ionres.respondph.aid_type.AidTypeModelComboBox;
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

public class DisasterDAOImpl implements DisasterDAO{
    private final DBConnection dbConnection;
    private final Cryptography cs;

    public DisasterDAOImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
        String secretKey = ConfigLoader.get("secretKey");
        this.cs = new Cryptography(secretKey);
    }

    @Override
    public boolean saving(DisasterModel dm) {
        String sql = "INSERT INTO disaster (type, name, date, lat, `long`, radius, notes, reg_date)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1,dm.getDisasterType());
            ps.setString(2,dm.getDisasterName());
            ps.setString(3,dm.getDate());
            ps.setString(4,dm.getLat());
            ps.setString(5,dm.getLongi());
            ps.setString(6,dm.getRadius());
            ps.setString(7,dm.getNotes());
            ps.setString(8,dm.getRegDate());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<DisasterModel> getAll() {
        List<DisasterModel> disaster = new ArrayList<>();
        String sql = "SELECT disaster_id, type, name, date, notes, reg_date  FROM disaster";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                DisasterModel dm = new DisasterModel();

                List<String> encrypted = new ArrayList<>();

                encrypted.add(rs.getString("type"));
                encrypted.add(rs.getString("name"));
                encrypted.add(rs.getString("date"));
                encrypted.add(rs.getString("notes"));
                encrypted.add(rs.getString("reg_date"));



                List<String> decrypted = cs.decrypt(encrypted);

                dm.setDisasterId(rs.getInt("disaster_id"));
                dm.setDisasterType(decrypted.get(0));
                dm.setDisasterName(decrypted.get(1));
                dm.setDate(decrypted.get(2));
                dm.setNotes(decrypted.get(3));
                dm.setRegDate(decrypted.get(4));



                disaster.add(dm);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(null, "Error fetching data: " + ex.getMessage());
        }

        return disaster;
    }

    @Override
    public boolean delete(DisasterModel dm) {
        String sql = "DELETE FROM disaster WHERE disaster_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dm.getDisasterId());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(DisasterModel dm) {
        String sql = "UPDATE disaster SET " +
                "type = ?, name = ?, date = ?, lat = ?, `long` = ?, radius = ?, notes = ?, reg_date = ? " +
                "WHERE disaster_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, dm.getDisasterType());
            ps.setString(2, dm.getDisasterName());
            ps.setString(3, dm.getDate());
            ps.setString(4, dm.getLat());
            ps.setString(5, dm.getLongi());
            ps.setString(6, dm.getRadius());
            ps.setString(7, dm.getNotes());
            ps.setString(8, dm.getRegDate());
            ps.setInt(9, dm.getDisasterId());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public DisasterModel getById(int id) {
        DisasterModel dm = null;
        String sql = "SELECT * FROM disaster WHERE disaster_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    dm = new DisasterModel();

                    List<String> encrypted = new ArrayList<>();
                    encrypted.add(rs.getString("type"));
                    encrypted.add(rs.getString("name"));
                    encrypted.add(rs.getString("date"));
                    encrypted.add(rs.getString("lat"));
                    encrypted.add(rs.getString("long"));
                    encrypted.add(rs.getString("radius"));
                    encrypted.add(rs.getString("notes"));
                    encrypted.add(rs.getString("reg_date"));

                    List<String> decrypted = cs.decrypt(encrypted);

                    dm.setDisasterId(rs.getInt("disaster_id"));
                    dm.setDisasterType(decrypted.get(0));
                    dm.setDisasterName(decrypted.get(1));
                    dm.setDate(decrypted.get(2));
                    dm.setLat(decrypted.get(3));
                    dm.setLongi(decrypted.get(4));
                    dm.setRadius(decrypted.get(5));
                    dm.setNotes(decrypted.get(6));
                    dm.setRegDate(decrypted.get(7));
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error fetching disaster: " + ex.getMessage());
        }
        return dm;
    }


    @Override
    public List<DisasterModelComboBox> findAll() {
        List<DisasterModelComboBox> aidTypes = new ArrayList<>();
        String sql = "SELECT * FROM disaster ORDER BY date DESC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DisasterModelComboBox aidType = mapResultSetToDisaster(rs);
                aidTypes.add(aidType);
            }

            System.out.println("Loaded " + aidTypes.size() + " aid types");

        } catch (Exception e) {
            System.err.println("Error fetching all aid types: " + e.getMessage());
            e.printStackTrace();
        }

        return aidTypes;
    }

    @Override
    public DisasterModelComboBox mapResultSetToDisaster(ResultSet rs) throws Exception {
        DisasterModelComboBox disaster = new DisasterModelComboBox();

        disaster.setDisasterId(rs.getInt("disaster_id"));

        // Decrypt fields
        String encryptedType = rs.getString("type");
        disaster.setDisasterTypeName(cs.decryptWithOneParameter(encryptedType));

        String encryptedName = rs.getString("name");
        disaster.setDisasterName(cs.decryptWithOneParameter(encryptedName));

        String encryptedDate = rs.getString("date");
        String decryptedDate = cs.decryptWithOneParameter(encryptedDate);
        disaster.setDisasterDate(java.time.LocalDate.parse(decryptedDate));

        // Decrypt optional fields
        String encryptedLat = rs.getString("lat");
        if (encryptedLat != null && !encryptedLat.isEmpty()) {
            String decryptedLat = cs.decryptWithOneParameter(encryptedLat);
            disaster.setLatitude(new java.math.BigDecimal(decryptedLat));
        }

        String encryptedLong = rs.getString("long");
        if (encryptedLong != null && !encryptedLong.isEmpty()) {
            String decryptedLong = cs.decryptWithOneParameter(encryptedLong);
            disaster.setLongitude(new java.math.BigDecimal(decryptedLong));
        }

        String encryptedRadius = rs.getString("radius");
        if (encryptedRadius != null && !encryptedRadius.isEmpty()) {
            String decryptedRadius = cs.decryptWithOneParameter(encryptedRadius);
            disaster.setRadiusKm(new java.math.BigDecimal(decryptedRadius));
        }

        String encryptedNotes = rs.getString("notes");
        if (encryptedNotes != null && !encryptedNotes.isEmpty()) {
            disaster.setNotes(cs.decryptWithOneParameter(encryptedNotes));
        }

        return disaster;
    }
}