package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginFrameController {
    LoginService loginDAO = new LoginServiceImpl();
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private CheckBox rememberMeCheck;
    
    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        boolean rememberMe = rememberMeCheck.isSelected();

        try {
            if (username.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error",
                        "Please enter both username and password.");
                return;
            }

            AdminModel admin = null;

                admin = loginDAO.login(username, password);


            if (admin != null) {
                Stage loginStage = (Stage) usernameField.getScene().getWindow();
                loginStage.close();

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/pages/MainFrame.fxml"));
                Parent root = loader.load();

                Stage stage = new Stage();
                Scene scene = new Scene(root, 1200, 800);
                scene.getStylesheets().add(getClass().getResource("/styles/pages/mainframe.css").toExternalForm());

                stage.setTitle("RespondPH - Dashboard");
                stage.setScene(scene);
                stage.setMinWidth(1200);
                stage.setMinHeight(800);
                stage.setMaximized(true);
                stage.show();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Invalid Username or Password please Try : " );
            }

        }catch (Exception ex){
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load dashboard: " + ex.getMessage());


        }

    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}