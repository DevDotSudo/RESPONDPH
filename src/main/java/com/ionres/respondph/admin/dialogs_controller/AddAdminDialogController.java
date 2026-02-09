package com.ionres.respondph.admin.dialogs_controller;

import com.ionres.respondph.admin.AdminController;
import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.admin.AdminService;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.DialogManager;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AddAdminDialogController {

    @FXML private VBox root;
    @FXML private TextField usernameField, firstNameField, middleNameField, lastNameField;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private Button closeButton, saveButton;
    private Stage dialogStage;
    private AdminService adminService;
    private AdminController adminController;
    private boolean adminAdded;
    private double xOffset, yOffset;

    @FXML
    public void initialize() {
        makeDraggable();
        EventHandler<ActionEvent> handler = this::handleActions;
        saveButton.setOnAction(handler);
        closeButton.setOnAction(handler);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();
        if (src == saveButton) {
            handleSave();
        }
        else if (src == closeButton) {
            close();
        }
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public Stage getDialogStage() {
        return dialogStage;
    }

    public void setAdminService(AdminService service) {
        this.adminService = service;
    }

    public void setAdminController(AdminController controller) {
        this.adminController = controller;
        onShow();
    }

    public void onShow() {
        adminAdded = false;
        clearFields();
    }

    public boolean isAdminAdded() {
        return adminAdded;
    }

    private void handleSave() {
        if (!validateInput()) return;

        try {
            AdminModel admin = new AdminModel(
                    usernameField.getText().trim(),
                    firstNameField.getText().trim(),
                    middleNameField.getText().trim(),
                    lastNameField.getText().trim(),
                    LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a")
                    ),
                    passwordField.getText()
            );

            adminService.createAdmin(admin);

            AlertDialogManager.showSuccess("Admin Created",
                    "New administrator has been successfully added to the system.");

            adminAdded = true;
            adminController.refreshAdminTable();
            close();

        } catch (Exception e) {
            AlertDialogManager.showError("Create Admin Failed",
                    "Failed to create admin: " + e.getMessage());
        }
    }

    private void close() {
        if (dialogStage != null) {
            dialogStage.hide();
        }
    }

    private boolean validateInput() {
        if (usernameField.getText().trim().length() < 4) {
            AlertDialogManager.showWarning("Validation Error",
                    "Username must be at least 4 characters.");
            return false;
        }

        if (firstNameField.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error",
                    "First name is required.");
            return false;
        }

        if (lastNameField.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error",
                    "Last name is required.");
            return false;
        }

        if (passwordField.getText().length() < 6) {
            AlertDialogManager.showWarning("Validation Error",
                    "Password must be at least 6 characters.");
            return false;
        }

        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            AlertDialogManager.showWarning("Validation Error",
                    "Passwords do not match.");
            return false;
        }

        return true;
    }


    private void clearFields() {
        usernameField.clear();
        firstNameField.clear();
        middleNameField.clear();
        lastNameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }

    private void makeDraggable() {
        root.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });

        root.setOnMouseDragged(e -> {
            if (dialogStage != null) {
                dialogStage.setX(e.getScreenX() - xOffset);
                dialogStage.setY(e.getScreenY() - yOffset);
            }
        });
    }
}