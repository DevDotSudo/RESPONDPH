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

/**
 * Controller for the login screen.
 * Handles user authentication and remember me functionality.
 */
public class LoginController {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    
    private final LoginService loginService = AppContext.loginService;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberMeCheck;

    /**
     * Initializes the login form.
     * Attempts auto-login if remember me was enabled.
     * Loads saved username if remember me was enabled previously.
     * Sets up Enter key handlers for quick login.
     */
    @FXML
    public void initialize() {
        loadRememberedCredentials();
        setupEnterKeyHandlers();
        attemptAutoLogin();
    }
    
    /**
     * Attempts auto-login if remember me is enabled and a valid token exists.
     * If successful, navigates directly to main screen.
     */
    private void attemptAutoLogin() {
        if (AppPreferences.isRememberMeEnabled() && AppPreferences.hasToken()) {
            // For now, we'll just pre-fill and focus password field
            // True auto-login would require token-based authentication
            // which needs backend support for token validation
            if (usernameField.getText() != null && !usernameField.getText().isEmpty()) {
                passwordField.requestFocus();
            }
        }
    }

    /**
     * Loads saved username if remember me was enabled.
     */
    private void loadRememberedCredentials() {
        if (AppPreferences.isRememberMeEnabled()) {
            String savedUsername = AppPreferences.getSavedUsername();
            if (savedUsername != null && !savedUsername.trim().isEmpty()) {
                usernameField.setText(savedUsername);
                rememberMeCheck.setSelected(true);
                // Focus password field for quick entry
                passwordField.requestFocus();
            }
        }
    }

    /**
     * Sets up Enter key handlers for username and password fields.
     */
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

    /**
     * Handles the login button click.
     * Validates input, authenticates user, and manages remember me functionality.
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        boolean rememberMe = rememberMeCheck.isSelected();

        // Validate input
        if (!validateInput(username, password)) {
            return;
        }

        try {
            // Attempt login
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

    /**
     * Validates username and password input.
     * 
     * @param username The username to validate
     * @param password The password to validate
     * @return true if input is valid, false otherwise
     */
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

    /**
     * Handles successful login.
     * Saves credentials if remember me is enabled, sets session, and navigates to main screen.
     * 
     * @param admin The authenticated admin model
     * @param rememberMe Whether to remember the username
     * @param username The username to save if remember me is enabled
     */
    private void handleSuccessfulLogin(AdminModel admin, boolean rememberMe, String username) {
        // Save or clear remember me preferences
        if (rememberMe) {
            AppPreferences.setRememberMe(true);
            AppPreferences.saveUsername(username);
            // Generate and save a simple session token (in production, use JWT or similar)
            String token = generateSessionToken(admin.getId(), username);
            AppPreferences.saveLoginToken(token);
        } else {
            AppPreferences.clearRememberMe();
            AppPreferences.clearToken();
        }

        // Set session
        SessionManager.getInstance().setCurrentAdmin(admin);
        
        LOGGER.info("User logged in successfully: " + username);

        // Show success message
        AlertDialogManager.showSuccess(
            "Login Successful",
            "Welcome back, " + admin.getFirstname() + "!"
        );

        // Navigate to main screen
        Stage loginStage = (Stage) usernameField.getScene().getWindow();
        loginStage.close();
        
        SceneManager.showStage(
            "/view/main/MainScreen.fxml",
            "ResponPH - Main Screen"
        );
    }
    
    /**
     * Generates a simple session token for auto-login.
     * In production, use proper JWT or secure token generation.
     * 
     * @param adminId The admin ID
     * @param username The username
     * @return A session token string
     */
    private String generateSessionToken(int adminId, String username) {
        // Simple token: adminId:username:timestamp
        // In production, use proper encryption and expiration
        return adminId + ":" + username + ":" + System.currentTimeMillis();
    }

    /**
     * Handles failed login attempt.
     */
    private void handleFailedLogin() {
        AlertDialogManager.showError(
            "Login Failed",
            "Incorrect username or password. Please try again."
        );
        passwordField.clear();
        passwordField.requestFocus();
    }

    /**
     * Handles login errors and exceptions.
     * 
     * @param e The exception that occurred
     */
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