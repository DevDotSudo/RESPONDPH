package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LoginDAOImpl implements LoginDAO {

    private final DBConnection dbConnection;

    public LoginDAOImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public AdminModel findByUsernameToLogin(String usernameInput, Cryptography cs) {
        String sql = "SELECT admin_id, username, first_name, middle_name, last_name, hash FROM admin";

        try {
            Connection conn = dbConnection.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String encryptedUsername = rs.getString("username");
                String hashedPassword = rs.getString("hash");

                String decryptedUsername = cs.decryptWithOneParameter(encryptedUsername);

                if (usernameInput.equals(decryptedUsername)) {
                    List<String> encrypted = new ArrayList<>();
                    encrypted.add(rs.getString("username"));
                    encrypted.add(rs.getString("first_name"));
                    encrypted.add(rs.getString("middle_name"));
                    encrypted.add(rs.getString("last_name"));

                    List<String> decrypted = cs.decrypt(encrypted);

                    AdminModel admin = new AdminModel();
                    admin.setId(rs.getInt("admin_id"));
                    admin.setUsername(decrypted.get(0));
                    admin.setFirstname(decrypted.get(1));
                    admin.setMiddlename(decrypted.get(2));
                    admin.setLastname(decrypted.get(3));
                    admin.setPassword(hashedPassword);
                    return admin;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void saveRememberMeToken(int adminId, String token) {
        String sql = "UPDATE admin SET remember_token = ?, token_expiry = ? WHERE admin_id = ?";

        try {
            Connection conn = dbConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            // Token expires in 30 days
            LocalDateTime expiry = LocalDateTime.now().plusDays(30);

            pstmt.setString(1, token);
            pstmt.setTimestamp(2, Timestamp.valueOf(expiry));
            pstmt.setInt(3, adminId);

            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public AdminModel findByRememberMeToken(String token, Cryptography cs) {
        System.out.println("DAO: findByRememberMeToken called with token: " + token);

        String sql = "SELECT admin_id, username, first_name, middle_name, last_name, hash " +
                "FROM admin WHERE remember_token = ? AND token_expiry > ?";

        try {
            Connection conn = dbConnection.getConnection();
            System.out.println("DAO: Database connection obtained");

            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, token);
            java.sql.Timestamp currentTime = Timestamp.valueOf(java.time.LocalDateTime.now());
            pstmt.setTimestamp(2, currentTime);

            System.out.println("DAO: Executing query with token: " + token);
            System.out.println("DAO: Current time for expiry check: " + currentTime);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("DAO: Admin record found in database");

                List<String> encrypted = new ArrayList<>();
                encrypted.add(rs.getString("username"));
                encrypted.add(rs.getString("first_name"));
                encrypted.add(rs.getString("middle_name"));
                encrypted.add(rs.getString("last_name"));

                List<String> decrypted = cs.decrypt(encrypted);

                System.out.println("DAO: Data decrypted successfully");

                AdminModel admin = new AdminModel();
                admin.setId(rs.getInt("admin_id"));
                admin.setUsername(decrypted.get(0));
                admin.setFirstname(decrypted.get(1));
                admin.setMiddlename(decrypted.get(2));
                admin.setLastname(decrypted.get(3));
                admin.setPassword(rs.getString("hash"));

                System.out.println("DAO: Admin model created - ID: " + admin.getId() + ", Username: " + admin.getUsername());
                return admin;
            } else {
                System.out.println("DAO: No matching admin record found (token may be invalid or expired)");
            }
        } catch (Exception e) {
            System.out.println("DAO: Exception occurred: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}