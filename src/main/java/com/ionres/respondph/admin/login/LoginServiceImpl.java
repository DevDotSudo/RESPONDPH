package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;

public class LoginServiceImpl implements LoginService{
    private final LoginDAO adminDao;

    public LoginServiceImpl(DBConnection dbConnection) {
        this.adminDao = new LoginDAOImpl(dbConnection);
    }

    private final Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

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

}
