package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.util.ResourceUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object implementation for login operations.
 * Handles database queries for user authentication.
 */
public class LoginDAOImpl implements LoginDAO {
    private static final Logger LOGGER = Logger.getLogger(LoginDAOImpl.class.getName());
    
    private static final String SQL_SELECT_ADMIN = 
        "SELECT admin_id, username, first_name, middle_name, last_name, hash FROM admin";
    
    private final DBConnection dbConnection;

    public LoginDAOImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    /**
     * Finds an admin by username for login purposes.
     * Decrypts encrypted fields and matches against the provided username.
     * 
     * @param usernameInput The username to search for (plain text)
     * @param cryptography The cryptography instance for decryption
     * @return AdminModel if found, null otherwise
     */
    @Override
    public AdminModel findByUsernameToLogin(String usernameInput, Cryptography cryptography) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(SQL_SELECT_ADMIN);

            while (rs.next()) {
                String encryptedUsername = rs.getString("username");
                String decryptedUsername = cryptography.decryptWithOneParameter(encryptedUsername);

                // Match username (case-sensitive)
                if (usernameInput.equals(decryptedUsername)) {
                    return buildAdminModel(rs, cryptography);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding admin by username: " + usernameInput, e);
        } finally {
            ResourceUtils.closeResources(rs, stmt);
        }
        
        return null;
    }

    /**
     * Builds an AdminModel from the current ResultSet row.
     * Decrypts all encrypted fields.
     * 
     * @param rs The ResultSet positioned at the admin record
     * @param cryptography The cryptography instance for decryption
     * @return AdminModel with decrypted data
     * @throws Exception if decryption fails
     */
    private AdminModel buildAdminModel(ResultSet rs, Cryptography cryptography) throws Exception {
        // Collect encrypted fields
        List<String> encryptedFields = new ArrayList<>();
        encryptedFields.add(rs.getString("username"));
        encryptedFields.add(rs.getString("first_name"));
        encryptedFields.add(rs.getString("middle_name"));
        encryptedFields.add(rs.getString("last_name"));

        // Decrypt all fields at once
        List<String> decryptedFields = cryptography.decrypt(encryptedFields);

        // Build and populate admin model
        AdminModel admin = new AdminModel();
        admin.setId(rs.getInt("admin_id"));
        admin.setUsername(decryptedFields.get(0));
        admin.setFirstname(decryptedFields.get(1));
        admin.setMiddlename(decryptedFields.get(2));
        admin.setLastname(decryptedFields.get(3));
        admin.setPassword(rs.getString("hash")); // Password hash is not encrypted

        return admin;
    }
}