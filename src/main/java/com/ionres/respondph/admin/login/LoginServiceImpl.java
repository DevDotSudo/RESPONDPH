package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.ConfigLoader;
import com.ionres.respondph.util.Cryptography;
import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;
import java.util.Base64;

public class LoginServiceImpl implements LoginService {
    private final LoginDAO adminDao;
    private final Cryptography cs;

    public LoginServiceImpl(DBConnection dbConnection) {
        this.adminDao = new LoginDAOImpl(dbConnection);
        String secretKey = ConfigLoader.get("secretKey");
        this.cs = new Cryptography(secretKey);
    }

    @Override
    public AdminModel login(String usernameInput, String passwordInput) throws Exception {
        if (usernameInput == null || usernameInput.isBlank() ||
                passwordInput == null || passwordInput.isBlank()) {
            throw new Exception("Username and password are required.");
        }

        AdminModel admin = adminDao.findByUsernameToLogin(usernameInput, cs);

        if (admin == null) {
            throw new Exception("Invalid username or password.");
        }

        if (!BCrypt.checkpw(passwordInput, admin.getPassword())) {
            throw new Exception("Invalid username or password.");
        }

        return admin;
    }

    @Override
    public String createRememberMeToken(int adminId) throws Exception {
        System.out.println("Creating remember me token for admin ID: " + adminId);

        // Generate a secure random token
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String token = Base64.getEncoder().encodeToString(tokenBytes);

        System.out.println("Generated token: " + token);

        // Save token to database with expiration (e.g., 30 days)
        adminDao.saveRememberMeToken(adminId, token);

        System.out.println("Token saved to database");

        return token;
    }

    @Override
    public AdminModel loginWithToken(String token) throws Exception {
        System.out.println("LoginService.loginWithToken called with token: " + token);

        if (token == null || token.isBlank()) {
            System.out.println("Token is null or blank");
            throw new Exception("Invalid token.");
        }

        // Validate token and get admin
        AdminModel admin = adminDao.findByRememberMeToken(token, cs);

        System.out.println("Admin retrieved from DAO: " + admin);

        if (admin == null) {
            System.out.println("Admin is null - token invalid or expired");
            throw new Exception("Invalid or expired token.");
        }

        System.out.println("Token validated successfully for admin: " + admin.getUsername());
        return admin;
    }
}