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
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javax.swing.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AddAdminDialogController {

    @FXML
    private VBox root;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField middleNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button cancelButton;

    @FXML
    private Button saveButton;
    private Stage dialogStage;
    private boolean adminAdded = false;
    private AdminService adminService;
    private AdminController adminController;
    private double xOffset = 0;
    private  double yOffset = 0;
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }
    public void setAdminController(AdminController adminController) {
        this.adminController = adminController;
    }

    @FXML
    public void initialize() {
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            Stage dialogStage = (Stage) root.getScene().getWindow();
            dialogStage.setX(event.getScreenX() - xOffset);
            dialogStage.setY(event.getScreenY() - yOffset);
        });
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleCancel();
            }
        });

        saveButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleSave();
            }
        });

        setupValidation();
    }

    private void setupValidation() {
        usernameField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                clearError();
            }
        });

        firstNameField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                clearError();
            }
        });

        lastNameField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                clearError();
            }
        });

        passwordField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                clearError();
            }
        });

        confirmPasswordField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                clearError();
            }
        });
    }

    @FXML
    private void handleSave() {
        if (validateInput()) {
            adminAdded = true;
            createAdmin();
            adminController.refreshAdminTable();
            clearFields();

        }
    }

    @FXML
    private void handleCancel() {
        adminAdded = false;
        dialogStage.close();
    }

    private boolean validateInput() {
        String username = usernameField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty()) {
            showError("Username is required.");
            usernameField.requestFocus();
            return false;
        }

        if (username.length() < 4) {
            showError("Username must be at least 4 characters long.");
            usernameField.requestFocus();
            return false;
        }

        if (firstName.isEmpty()) {
            showError("First name is required.");
            firstNameField.requestFocus();
            return false;
        }

        if (lastName.isEmpty()) {
            showError("Last name is required.");
            lastNameField.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            showError("Password is required.");
            passwordField.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters long.");
            passwordField.requestFocus();
            return false;
        }

        if (confirmPassword.isEmpty()) {
            showError("Please confirm your password.");
            confirmPasswordField.requestFocus();
            return false;
        }

        return true;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isAdminAdded() {
        return adminAdded;
    }

    public void clearFields(){
        usernameField.setText("");
        firstNameField.setText("");
        middleNameField.setText("");
        lastNameField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
    }

    private void createAdmin() {
        String username = usernameField.getText();
        String fname = firstNameField.getText();
        String mname = middleNameField.getText();
        String lname = lastNameField.getText();
        String redDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));
        String password = passwordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (!password.equals(confirmPass)) {
            JOptionPane.showMessageDialog(null, "Passwords do not match");
            passwordField.requestFocus();
            return;
        }

        try {
            AdminModel adminModel = new AdminModel(username, fname, mname, lname, redDate, password);

            adminService.createAdmin(adminModel);

            JOptionPane.showMessageDialog(null,
                    "Admin successfully created.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
