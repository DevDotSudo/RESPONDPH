package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.util.CryptographyManager;
import org.mindrot.jbcrypt.BCrypt;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service implementation for user authentication.
 * Handles login validation and password verification.
 */
public class LoginServiceImpl implements LoginService {
    private static final Logger LOGGER = Logger.getLogger(LoginServiceImpl.class.getName());
    private static final Cryptography CRYPTO = CryptographyManager.getInstance();
    
    private final LoginDAO loginDAO;

    public LoginServiceImpl(DBConnection dbConnection) {
        this.loginDAO = new LoginDAOImpl(dbConnection);
    }

    /**
     * Authenticates a user with username and password.
     * 
     * @param usernameInput The username to authenticate
     * @param passwordInput The password to verify
     * @return AdminModel if authentication succeeds, null otherwise
     * @throws Exception if validation fails or authentication error occurs
     */
    @Override
    public AdminModel login(String usernameInput, String passwordInput) throws Exception {
        // Validate input parameters
        validateLoginInput(usernameInput, passwordInput);

        // Find admin by username
        AdminModel admin = loginDAO.findByUsernameToLogin(usernameInput, CRYPTO);
        
        if (admin == null) {
            LOGGER.warning("Login attempt failed: User not found - " + usernameInput);
            throw new Exception("Invalid username or password.");
        }

        // Verify password
        if (!BCrypt.checkpw(passwordInput, admin.getPassword())) {
            LOGGER.warning("Login attempt failed: Invalid password for user - " + usernameInput);
            throw new Exception("Invalid username or password.");
        }

        LOGGER.info("User authenticated successfully: " + usernameInput);
        return admin;
    }

    /**
     * Validates login input parameters.
     * 
     * @param usernameInput The username to validate
     * @param passwordInput The password to validate
     * @throws Exception if validation fails
     */
    private void validateLoginInput(String usernameInput, String passwordInput) throws Exception {
        if (usernameInput == null || usernameInput.isBlank()) {
            throw new Exception("Username is required.");
        }
        
        if (passwordInput == null || passwordInput.isBlank()) {
            throw new Exception("Password is required.");
        }
    }
}
