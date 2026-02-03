package com.ionres.respondph.evac_site;

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

public class EvacSiteDAOServiceImpl implements EvacSiteDAO {
    private final DBConnection dbConnection;
    private final Cryptography cs;
    private Connection conn;

    public EvacSiteDAOServiceImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
        String secretKey = ConfigLoader.get("secretKey");
        this.cs = new Cryptography(secretKey);
    }

    @Override
    public boolean saving(EvacSiteModel evacSite) {
        String sql = "INSERT INTO evac_site (lat, `long`, name, capacity, notes) VALUES (?, ?, ?, ?, ?)";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setDouble(1, Double.parseDouble(evacSite.getLat()));
            ps.setDouble(2, Double.parseDouble(evacSite.getLongi()));
            ps.setString(3, evacSite.getName()); // encrypted
            ps.setInt(4, Integer.parseInt(evacSite.getCapacity()));
            ps.setString(5, evacSite.getNotes()); // encrypted

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error occurred: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    @Override
    public List<EvacSiteModel> getAll() {
        List<EvacSiteModel> evacSites = new ArrayList<>();
        String sql = "SELECT evac_id, name, capacity, lat, `long`, notes FROM evac_site";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                EvacSiteModel evacSite = new EvacSiteModel();

                List<String> encrypted = new ArrayList<>();
                encrypted.add(rs.getString("name"));
                encrypted.add(rs.getString("notes"));

                List<String> decrypted = cs.decrypt(encrypted);

                evacSite.setEvacId(rs.getInt("evac_id"));
                evacSite.setName(decrypted.get(0));
                evacSite.setCapacity(String.valueOf(rs.getInt("capacity")));
                evacSite.setLat(String.valueOf(rs.getDouble("lat")));
                evacSite.setLongi(String.valueOf(rs.getDouble("long")));
                evacSite.setNotes(decrypted.get(1));

                evacSites.add(evacSite);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error fetching data: " + ex.getMessage());
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        return evacSites;
    }

    @Override
    public boolean delete(EvacSiteModel evacSite) {
        String sql = "DELETE FROM evac_site WHERE evac_id = ?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, evacSite.getEvacId());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error occurred: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean update(EvacSiteModel evacSite) {
        String sql = "UPDATE evac_site SET name = ?, capacity = ?, lat = ?, `long` = ?, notes = ? WHERE evac_id = ?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, evacSite.getName()); // encrypted
            ps.setInt(2, Integer.parseInt(evacSite.getCapacity()));
            ps.setDouble(3, Double.parseDouble(evacSite.getLat()));
            ps.setDouble(4, Double.parseDouble(evacSite.getLongi()));
            ps.setString(5, evacSite.getNotes()); // encrypted
            ps.setInt(6, evacSite.getEvacId());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error occurred: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    @Override
    public EvacSiteModel getById(int id) {
        EvacSiteModel evacSite = null;
        String sql = "SELECT * FROM evac_site WHERE evac_id = ?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                evacSite = new EvacSiteModel();

                List<String> encrypted = new ArrayList<>();
                encrypted.add(rs.getString("name"));
                encrypted.add(rs.getString("notes"));

                List<String> decrypted = cs.decrypt(encrypted);

                evacSite.setEvacId(rs.getInt("evac_id"));
                evacSite.setName(decrypted.get(0));
                evacSite.setCapacity(String.valueOf(rs.getInt("capacity")));
                evacSite.setLat(String.valueOf(rs.getDouble("lat")));
                evacSite.setLongi(String.valueOf(rs.getDouble("long")));
                evacSite.setNotes(decrypted.get(1));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error fetching evacuation site: " + ex.getMessage());
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        return evacSite;
    }
}