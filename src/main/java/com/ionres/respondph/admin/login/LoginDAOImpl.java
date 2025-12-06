package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class LoginDAOImpl implements LoginDAO{

        @Override
        public AdminModel findByUsernameToLogin(String usernameInput, Cryptography cs) {
            String sql = "SELECT username, hash FROM admin";

            try (Connection con = DBConnection.getInstance().getConnection();
                 Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    String encryptedUsername = rs.getString("username");
                    String hashedPassword = rs.getString("hash");

                    String decryptedUsername = cs.decryptId(encryptedUsername);

                    if (usernameInput.equals(decryptedUsername)) {
                        AdminModel admin = new AdminModel();
                        admin.setUsername(decryptedUsername);
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
