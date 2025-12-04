package com.ionres.respondph.beneficiary.dialogs_controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddBeneficiariesDialogController {
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

    @FXML
    public void initialize() {
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
            dialogStage.close();
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

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
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
}
