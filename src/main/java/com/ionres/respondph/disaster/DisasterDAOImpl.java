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

public class DisasterDAOImpl implements DisasterDAO {
    private final DBConnection dbConnection;
    private final Cryptography cs;

    public DisasterDAOImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
        String secretKey = ConfigLoader.get("secretKey");
        this.cs = new Cryptography(secretKey);
    }

    @Override
    public boolean saving(DisasterModel dm) {
        // ── UPDATED: now includes poly_lat_long ──────────────────────────────
        String sql = "INSERT INTO disaster (type, name, date, lat, `long`, radius, notes, reg_date, is_banate_area, poly_lat_long)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
            ps.setBoolean(9, dm.isBanateArea());
            ps.setString(10, dm.getPolyLatLong()); // null if not polygon

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
        String sql = "SELECT disaster_id, type, name, date, notes, reg_date FROM disaster";

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
            JOptionPane.showMessageDialog(null, "Error fetching data: " + ex.getMessage());
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
        // ── UPDATED: now includes poly_lat_long ──────────────────────────────
        String sql = "UPDATE disaster SET " +
                "type = ?, name = ?, date = ?, lat = ?, `long` = ?, radius = ?, " +
                "notes = ?, reg_date = ?, poly_lat_long = ? " +
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
            ps.setString(9, dm.getPolyLatLong()); // null if not polygon
            ps.setInt(10, dm.getDisasterId());

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
        // ── UPDATED: fetch poly_lat_long too ────────────────────────────────
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

                    String encLat    = rs.getString("lat");
                    String encLong   = rs.getString("long");
                    String encRadius = rs.getString("radius");

                    encrypted.add(encLat    != null ? encLat    : "");
                    encrypted.add(encLong   != null ? encLong   : "");
                    encrypted.add(encRadius != null ? encRadius : "");
                    encrypted.add(rs.getString("notes"));
                    encrypted.add(rs.getString("reg_date"));

                    // poly_lat_long is decrypted separately (may be null)
                    String encPoly = rs.getString("poly_lat_long");

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
                    dm.setIsBanateArea(rs.getBoolean("is_banate_area"));

                    // Decrypt poly_lat_long if present
                    if (encPoly != null && !encPoly.isEmpty()) {
                        dm.setPolyLatLong(cs.decryptWithOneParameter(encPoly));
                        dm.setLocationType("POLYGON");
                    } else if (dm.isBanateArea()) {
                        dm.setLocationType("BANATE");
                    } else {
                        dm.setLocationType("CIRCLE");
                    }
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
        List<DisasterModelComboBox> disasters = new ArrayList<>();
        String sql = "SELECT * FROM disaster ORDER BY date DESC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DisasterModelComboBox disaster = mapResultSetToDisaster(rs);
                disasters.add(disaster);
            }

            System.out.println("Loaded " + disasters.size() + " disasters");

        } catch (Exception e) {
            System.err.println("Error fetching all disasters: " + e.getMessage());
            e.printStackTrace();
        }

        return disasters;
    }

    @Override
    public DisasterModelComboBox mapResultSetToDisaster(ResultSet rs) throws Exception {
        DisasterModelComboBox disaster = new DisasterModelComboBox();

        disaster.setDisasterId(rs.getInt("disaster_id"));

        disaster.setDisasterTypeName(cs.decryptWithOneParameter(rs.getString("type")));
        disaster.setDisasterName(cs.decryptWithOneParameter(rs.getString("name")));

        String decryptedDate = cs.decryptWithOneParameter(rs.getString("date"));
        disaster.setDisasterDate(java.time.LocalDate.parse(decryptedDate));

        String encLat = rs.getString("lat");
        if (encLat != null && !encLat.isEmpty()) {
            String decLat = cs.decryptWithOneParameter(encLat);
            if (decLat != null && !decLat.trim().isEmpty()) {
                disaster.setLatitude(new java.math.BigDecimal(decLat.trim()));
            }
        }

        String encLong = rs.getString("long");
        if (encLong != null && !encLong.isEmpty()) {
            String decLong = cs.decryptWithOneParameter(encLong);
            if (decLong != null && !decLong.trim().isEmpty()) {
                disaster.setLongitude(new java.math.BigDecimal(decLong.trim()));
            }
        }

        String encRadius = rs.getString("radius");
        if (encRadius != null && !encRadius.isEmpty()) {
            String decRadius = cs.decryptWithOneParameter(encRadius);
            if (decRadius != null && !decRadius.trim().isEmpty()) {
                disaster.setRadiusKm(new java.math.BigDecimal(decRadius.trim()));
            }
        }

        String encNotes = rs.getString("notes");
        if (encNotes != null && !encNotes.isEmpty()) {
            disaster.setNotes(cs.decryptWithOneParameter(encNotes));
        }

        // ── NEW: poly_lat_long ───────────────────────────────────────────────
        String encPoly = rs.getString("poly_lat_long");
        if (encPoly != null && !encPoly.isEmpty()) {
            disaster.setPolyLatLong(cs.decryptWithOneParameter(encPoly));
        }
        // ─────────────────────────────────────────────────────────────────────

        return disaster;
    }
}