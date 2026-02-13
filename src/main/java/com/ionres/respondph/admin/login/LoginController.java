package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Scene;
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
        System.out.println("LoginController initialized");

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

        // Wait for scene to be available before checking auto-login
        usernameField.sceneProperty().addListener(new ChangeListener<Scene>() {
            @Override
            public void changed(ObservableValue<? extends Scene> observable, Scene oldScene, Scene newScene) {
                if (newScene != null) {
                    System.out.println("Scene is now available, running auto-login check...");
                    // Remove listener after first call
                    usernameField.sceneProperty().removeListener(this);
                    // Run auto-login check
                    Platform.runLater(() -> checkRememberedLogin());
                }
            }
        });
    }

    private void checkRememberedLogin() {
        try {
            String rememberedToken = prefs.getRememberMeToken();
            System.out.println("Checking remembered token: " + rememberedToken);

            if (rememberedToken != null && !rememberedToken.isEmpty()) {
                System.out.println("Token found, attempting login...");

                admin = loginService.loginWithToken(rememberedToken);

                System.out.println("Admin after token login: " + admin);

                if (admin != null) {
                    System.out.println("Admin details - ID: " + admin.getId() + ", Username: " + admin.getUsername());

                    SessionManager.getInstance().setCurrentAdmin(admin);
                    System.out.println("Session manager updated");

                    // Get the stage
                    System.out.println("Checking scene availability...");
                    System.out.println("usernameField.getScene(): " + usernameField.getScene());

                    if (usernameField.getScene() != null && usernameField.getScene().getWindow() != null) {
                        Stage loginStage = (Stage) usernameField.getScene().getWindow();
                        System.out.println("Closing login stage...");
                        loginStage.close();


                        System.out.println("Opening main screen...");
                        SceneManager.showStage("/view/main/MainScreen.fxml", "ResponPH - Main Screen");
                        System.out.println("Auto-login completed successfully!");
                    } else {
                        System.out.println("ERROR: Scene or Window is not available!");
                    }
                } else {
                    // Token invalid or expired, clear it
                    System.out.println("Token invalid or expired, clearing...");
                    prefs.clearRememberMe();
                }
            } else {
                System.out.println("No remembered token found");
            }
        } catch (Exception e) {
            System.out.println("Exception during auto-login: " + e.getMessage());
            e.printStackTrace();
            prefs.clearRememberMe();
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
                // Handle remember me
                if (rememberMe) {
                    String token = loginService.createRememberMeToken(admin.getId());
                    prefs.saveRememberMeToken(token);
                    System.out.println("Remember me token saved: " + token);
                } else {
                    prefs.clearRememberMe();
                    System.out.println("Remember me cleared");
                }

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
                    "Failed to login: " + e.getMessage());
        }
    }
}