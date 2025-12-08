package com.ionres.respondph.admin.dialogs_controller;

import com.ionres.respondph.admin.AdminController;
import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.admin.AdminService;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.ionres.respondph.util.AlertMessage.showErrorAlert;
import static com.ionres.respondph.util.AlertMessage.showSuccess;

public class EditAdminDialogController {

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
    private Button cancelButton;

    @FXML
    private Button updateButton;

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

    public void setAdminData(AdminModel admin) {
        this.selectedAdmin = admin;

        usernameField.setText(admin.getUsername());
        firstNameField.setText(admin.getFirstname());
        middleNameField.setText(admin.getMiddlename());
        lastNameField.setText(admin.getLastname());
    }


    public void initialize() {
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleCancel();
            }
        });

        updateButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleEdit();
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
            showErrorAlert("Failed","Username is required.");
            usernameField.requestFocus();
            return false;
        }

        if (username.length() < 4) {
            showErrorAlert("Failed","Username must be at least 4 characters long.");
            usernameField.requestFocus();
            return false;
        }

        if (firstName.isEmpty()) {
            showErrorAlert("Failed", "First name is required.");
            firstNameField.requestFocus();
            return false;
        }

        if (lastName.isEmpty()) {
            showErrorAlert("Failed","Last name is required.");
            lastNameField.requestFocus();
            return false;
        }



        return true;
    }

    @FXML
    private void handleCancel() {
        adminEdit = false;
        dialogStage.close();
    }



    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;

    }

    public boolean isAdminEdit() {
        return adminEdit;

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
            String redDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            admin.setUsername(username);
            admin.setFirstname(fname);
            admin.setMiddlename(mname);
            admin.setLastname(lname);
            admin.setRegDate(redDate);

            boolean updated = adminService.updateAdmin(admin);

            if (updated) {
                showSuccess("Success", "Admin updated successfully!");
            } else {
                showErrorAlert("Failed", "Failed to update admin.");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showErrorAlert("Failed", "Error updating admin: " + ex.getMessage());
        }
    }

}
