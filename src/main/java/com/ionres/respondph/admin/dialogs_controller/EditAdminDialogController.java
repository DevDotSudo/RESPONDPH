package com.ionres.respondph.admin.dialogs_controller;

import com.ionres.respondph.admin.AdminController;
import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.admin.AdminService;
import com.ionres.respondph.util.AlertDialogManager;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class EditAdminDialogController {
    @FXML private VBox root;

    @FXML private TextField usernameField;

    @FXML private TextField firstNameField;

    @FXML private TextField middleNameField;

    @FXML private TextField lastNameField;

    @FXML private Label errorLabel;

    @FXML private Button updateBtn, closeButton;

    private AdminService adminService;
    private AdminController adminController;
    private Stage dialogStage;
    private boolean adminEdit = false;
    private double yOffset = 0;
    private double xOffset = 0;
    private AdminModel selectedAdmin;

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public Stage getDialogStage() {
        return dialogStage;
    }

    public void setAdminData(AdminModel admin) {
        this.selectedAdmin = admin;
        usernameField.setText(admin.getUsername());
        firstNameField.setText(admin.getFirstname());
        middleNameField.setText(admin.getMiddlename());
        lastNameField.setText(admin.getLastname());
    }

    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    public void setAdminController(AdminController adminController) {
        this.adminController = adminController;
    }

    public void initialize() {
        updateBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleEdit();
            }
        });

        closeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                closeDialog();
            }
        });
        makeDraggable();
    }

    @FXML
    private void handleEdit() {
        if (validateInput()) {
            adminEdit = true;
            boolean updated = updateAdminInfo();
            if (updated) {
                adminController.refreshAdminTable();
                clearFields();
                closeDialog();
            }
        }
    }

    private boolean validateInput() {
        String username = usernameField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();

        if (username.isEmpty()) {
            AlertDialogManager.showError("Validation Error", "Username is required.");
            usernameField.requestFocus();
            return false;
        }

        if (username.length() < 4) {
            AlertDialogManager.showWarning("Validation Error",
                    "Username must be at least 4 characters long.");
            usernameField.requestFocus();
            return false;
        }

        if (firstName.isEmpty()) {
            AlertDialogManager.showError("Validation Error", "First name is required.");
            firstNameField.requestFocus();
            return false;
        }

        if (lastName.isEmpty()) {
            AlertDialogManager.showError("Validation Error", "Last name is required.");
            lastNameField.requestFocus();
            return false;
        }

        return true;
    }

    public void clearFields() {
        usernameField.setText("");
        firstNameField.setText("");
        middleNameField.setText("");
        lastNameField.setText("");
    }

    public boolean updateAdminInfo() {
        try {
            AdminModel admin = selectedAdmin;

            String username = usernameField.getText().trim();
            String fname = firstNameField.getText().trim();
            String mname = middleNameField.getText().trim();
            String lname = lastNameField.getText().trim();

            admin.setUsername(username);
            admin.setFirstname(fname);
            admin.setMiddlename(mname);
            admin.setLastname(lname);

            boolean updated = adminService.updateAdmin(admin);

            if (updated) {
                AlertDialogManager.showSuccess("Update Successful",
                        "Admin information has been successfully updated.");
                return true;
            } else {
                AlertDialogManager.showError("Update Failed",
                        "Failed to update admin information. Please try again.");
                return false;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            AlertDialogManager.showError("Update Error",
                    "An error occurred while updating admin: " + ex.getMessage());
            return false;
        }
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.hide();
        }
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