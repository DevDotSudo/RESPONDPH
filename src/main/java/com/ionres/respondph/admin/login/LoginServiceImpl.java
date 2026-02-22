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
    public AdminModel login(String usernameInput, String passwordInput, String role) throws Exception {
        AdminModel admin = adminDao.findByUsernameToLogin(usernameInput, role, cs);

        // ADD THIS:
        System.out.println("Login attempt - role selected: " + role);
        System.out.println("Admin found: " + admin);
        System.out.println("Admin role from DB: " + (admin != null ? admin.getRole() : "null - not found")); // pass role

        if (admin == null) {
            throw new Exception("Invalid username, password, or role.");
        }

        if (!BCrypt.checkpw(passwordInput, admin.getPassword())) {
            throw new Exception("Invalid username, password, or role.");
        }

        return admin;
    }

    @Override
    public String createRememberMeToken(int adminId) throws Exception {
        System.out.println("Creating remember me token for admin ID: " + adminId);

        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String token = Base64.getEncoder().encodeToString(tokenBytes);

        System.out.println("Generated token: " + token);

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