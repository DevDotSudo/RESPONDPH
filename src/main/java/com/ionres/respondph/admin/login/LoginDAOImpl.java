package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class LoginDAOImpl implements LoginDAO{

    @Override
    public AdminModel findByUsernameToLogin(String usernameInput, Cryptography cs) {
        String sql = "SELECT admin_id, username, first_name, middle_name, last_name, hash FROM admin";

        try (Connection con = DBConnection.getInstance().getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String encryptedUsername = rs.getString("username");
                String hashedPassword = rs.getString("hash");

                String decryptedUsername = cs.decryptId(encryptedUsername);

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
}