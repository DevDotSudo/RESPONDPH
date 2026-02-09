package com.ionres.respondph.sendsms;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DisasterDAOImpl implements DisasterDAO {

    private final Cryptography crypto = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

    @Override
    public List<DisasterModel> getAllDisasters() {
        List<DisasterModel> disasters = new ArrayList<>();

        String sql = "SELECT disaster_id, type, name, date FROM disaster ORDER BY date DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DisasterModel disaster = new DisasterModel();
                disaster.setDisasterId(rs.getInt("disaster_id"));
                disaster.setType(decryptField(rs.getString("type")));
                disaster.setName(decryptField(rs.getString("name")));
                disaster.setDate(decryptField(rs.getString("date")));
                disasters.add(disaster);
            }

            System.out.println("DEBUG: Retrieved " + disasters.size() + " disasters");

        } catch (SQLException e) {
            System.err.println("Error retrieving disasters: " + e.getMessage());
            e.printStackTrace();
        }

        return disasters;
    }

    @Override
    public DisasterModel getDisasterById(int disasterId) {
        String sql = "SELECT disaster_id, type, name, date FROM disaster WHERE disaster_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, disasterId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DisasterModel disaster = new DisasterModel();
                    disaster.setDisasterId(rs.getInt("disaster_id"));
                    disaster.setType(decryptField(rs.getString("type")));
                    disaster.setName(decryptField(rs.getString("name")));
                    disaster.setDate(decryptField(rs.getString("date")));
                    return disaster;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving disaster by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    private String decryptField(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isEmpty()) {
            return null;
        }

        if (!encryptedValue.contains(":")) {
            return encryptedValue;
        }

        try {
            if (crypto == null) {
                System.err.println("Cryptography instance is null - cannot decrypt field");
                return null;
            }
            return crypto.decryptWithOneParameter(encryptedValue);
        } catch (Exception e) {
            System.err.println("Error decrypting field: " + e.getMessage());
            return encryptedValue;
        }
    }
}