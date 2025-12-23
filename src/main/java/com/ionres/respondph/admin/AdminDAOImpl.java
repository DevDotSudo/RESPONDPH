package com.ionres.respondph.admin;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminDAOImpl implements AdminDAO {
    private final DBConnection dbConnection;

    public AdminDAOImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public boolean saving(AdminModel am) {
        String sql = "INSERT INTO admin (username, first_name, middle_name, last_name, reg_date, hash) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            Connection conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, am.getUsername());
            ps.setString(2, am.getFirstname());
            ps.setString(3, am.getMiddlename());
            ps.setString(4, am.getLastname());
            ps.setString(5, am.getRegDate());
            ps.setString(6, am.getPassword());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean existsByUsername(String encryptedUsername) {
        String sql = "SELECT COUNT(*) FROM admin WHERE username = ?";

        try {
            Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, encryptedUsername);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<AdminModel> getAll() {

        List<AdminModel> admins = new ArrayList<>();
        String query = "SELECT * FROM admin";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                AdminModel admin = new AdminModel();
                admin.setId(rs.getInt("admin_id"));

                admin.setUsername(rs.getString("username"));
                admin.setFirstname(rs.getString("first_name"));
                admin.setMiddlename(rs.getString("middle_name"));
                admin.setLastname(rs.getString("last_name"));
                admin.setRegDate(rs.getString("reg_date"));

                admins.add(admin);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return admins;
    }

    @Override
    public boolean delete(AdminModel am) {
        String sql = "DELETE FROM admin WHERE admin_id = ?";

        try{
            Connection conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, am.getId());


            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(AdminModel am) {
        String sql = "UPDATE admin set username = ?, first_name = ?, middle_name = ?, last_name = ? WHERE admin_id = ?";

        try {
            Connection conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, am.getUsername());
            ps.setString(2, am.getFirstname());
            ps.setString(3, am.getMiddlename());
            ps.setString(4, am.getLastname());
            ps.setInt(5, am.getId());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }
}
