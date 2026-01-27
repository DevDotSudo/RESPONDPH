package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.AppContext;
import com.ionres.respondph.util.AppPreferences;
import com.ionres.respondph.util.SceneManager;
import com.ionres.respondph.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    
    private final LoginService loginService = AppContext.loginService;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberMeCheck;

    @FXML
    public void initialize() {
        loadRememberedCredentials();
        setupEnterKeyHandlers();
        attemptAutoLogin();
    }

    private void attemptAutoLogin() {
        if (AppPreferences.isRememberMeEnabled() && AppPreferences.hasToken()) {
            if (usernameField.getText() != null && !usernameField.getText().isEmpty()) {
                passwordField.requestFocus();
            }
        }
    }

    private void loadRememberedCredentials() {
        if (AppPreferences.isRememberMeEnabled()) {
            String savedUsername = AppPreferences.getSavedUsername();
            if (savedUsername != null && !savedUsername.trim().isEmpty()) {
                usernameField.setText(savedUsername);
                rememberMeCheck.setSelected(true);
                passwordField.requestFocus();
            }
        }
    }

    private void setupEnterKeyHandlers() {
        if (usernameField != null) {
            usernameField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    handleLogin();
                }
            });
        }
        
        if (passwordField != null) {
            passwordField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    handleLogin();
                }
            });
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        boolean rememberMe = rememberMeCheck.isSelected();

        if (!validateInput(username, password)) {
            return;
        }

        try {
            AdminModel admin = loginService.login(username, password);

            if (admin != null) {
                handleSuccessfulLogin(admin, rememberMe, username);
            } else {
                handleFailedLogin();
            }
        } catch (Exception e) {
            handleLoginError(e);
        }
    }

    private boolean validateInput(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            AlertDialogManager.showWarning(
                "Validation Error",
                "Please enter both username and password."
            );
            return false;
        }
        return true;
    }

    private void handleSuccessfulLogin(AdminModel admin, boolean rememberMe, String username) {
        if (rememberMe) {
            AppPreferences.setRememberMe(true);
            AppPreferences.saveUsername(username);
            String token = generateSessionToken(admin.getId(), username);
            AppPreferences.saveLoginToken(token);
        } else {
            AppPreferences.clearRememberMe();
            AppPreferences.clearToken();
        }

        SessionManager.getInstance().setCurrentAdmin(admin);
        
        LOGGER.info("User logged in successfully: " + username);

        AlertDialogManager.showSuccess(
            "Login Successful",
            "Welcome back, " + admin.getFirstname() + "!"
        );

        Stage loginStage = (Stage) usernameField.getScene().getWindow();
        loginStage.close();
        
        SceneManager.showStage(
            "/view/main/MainScreen.fxml",
            "ResponPH - Main Screen"
        );
    }

    private String generateSessionToken(int adminId, String username) {
        return adminId + ":" + username + ":" + System.currentTimeMillis();
    }

    private void handleFailedLogin() {
        AlertDialogManager.showError(
            "Login Failed",
            "Incorrect username or password. Please try again."
        );
        passwordField.clear();
        passwordField.requestFocus();
    }

    private void handleLoginError(Exception e) {
        LOGGER.log(Level.SEVERE, "Login error occurred", e);
        
        String errorMessage = e.getMessage();
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            errorMessage = "An unexpected error occurred during login.";
        }
        
        AlertDialogManager.showError(
            "Login Error",
            errorMessage
        );
        
        passwordField.clear();
    }
}