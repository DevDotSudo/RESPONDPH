package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

public class LoginController {
    private final LoginService loginService = AppContext.loginService;
    private AdminModel admin = new AdminModel();
    private AppPreferences prefs = new AppPreferences();

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberMeCheck;

    @FXML
    public void initialize() {
        if (usernameField != null) {
            usernameField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) handleLogin();
            });
        }
        if (passwordField != null) {
            passwordField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) handleLogin();
            });
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        boolean rememberMe = rememberMeCheck.isSelected();

        try {
            if (username.isEmpty() || password.isEmpty()) {
                AlertDialogManager.showWarning("Validation Error",
                        "Please enter both username and password.");
                return;
            }

            admin = loginService.login(username, password);

            if (admin != null) {
                AlertDialogManager.showSuccess("Login Successful",
                        "Welcome back. You are now logged in.");

                SessionManager.getInstance().setCurrentAdmin(admin);
                Stage loginStage = (Stage) usernameField.getScene().getWindow();
                loginStage.close();
                SceneManager.showStage("/view/main/MainScreen.fxml", "ResponPH - Main Screen");
            }
            else {
                AlertDialogManager.showError("Login Failed",
                        "Incorrect username or password.");
                passwordField.clear();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Error",
                    "Failed to load mapping: " + e.getMessage());
        }
    }
}