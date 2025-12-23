package com.ionres.respondph.admin.dialogs_controller;

import com.ionres.respondph.admin.AdminController;
import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.admin.AdminService;
import com.ionres.respondph.util.AlertDialog;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class EditAdminDialogController {
    AlertDialog alertDialog = new AlertDialog();
    @FXML
    private TextField usernameField;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField middleNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button updateBtn, closeButton;

    private AdminService adminService;
    private AdminController adminController;
    private Stage dialogStage;
    private boolean adminEdit = false;

    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }
    public void setAdminController(AdminController adminController) {
        this.adminController = adminController;
    }

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
    }

    @FXML
    private void handleEdit() {
        if (validateInput()) {
            adminEdit = true;
            updateAdminInfo();
            adminController.refreshAdminTable();
            clearFields();
        }
    }

    private boolean validateInput() {
        String username = usernameField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();

        if (username.isEmpty()) {
            alertDialog.showErrorAlert("Failed","Username is required.");
            usernameField.requestFocus();
            return false;
        }

        if (username.length() < 4) {
            alertDialog.showErrorAlert("Failed","Username must be at least 4 characters long.");
            usernameField.requestFocus();
            return false;
        }

        if (firstName.isEmpty()) {
            alertDialog.showErrorAlert("Failed", "First name is required.");
            firstNameField.requestFocus();
            return false;
        }

        if (lastName.isEmpty()) {
            alertDialog.showErrorAlert("Failed","Last name is required.");
            lastNameField.requestFocus();
            return false;
        }
        return true;
    }

    public void clearFields(){
        usernameField.setText("");
        firstNameField.setText("");
        middleNameField.setText("");
        lastNameField.setText("");
    }

    public void updateAdminInfo() {
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
                alertDialog.showSuccess("Success", "Admin updated successfully!");
            } else {
                alertDialog.showErrorAlert("Failed", "Failed to update admin.");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            alertDialog.showErrorAlert("Failed", "Error updating admin: " + ex.getMessage());
        }
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.hide();
        }
    }
}