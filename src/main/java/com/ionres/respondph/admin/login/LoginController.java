package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    LoginService loginService = new LoginServiceImpl();
    private AdminModel admin = new AdminModel();

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

            admin = loginService.login(username, password);

            if (admin != null) {

                SessionManager.getInstance().setCurrentAdmin(admin);

                Stage loginStage = (Stage) usernameField.getScene().getWindow();
                loginStage.close();
                
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/pages/MainFrame.fxml"));
                Parent root = loader.load();
                
                Stage stage = new Stage();
                Scene scene = new Scene(root, 1200, 800);
                scene.getStylesheets().add(getClass().getResource("/styles/pages/mainframe.css").toExternalForm());
                
                stage.setTitle("RespondPH - Dashboard");
                stage.setScene(scene);
                stage.setMinWidth(1600);
                stage.setMinHeight(800);
                stage.setMaximized(true); 
                stage.show();
            }
            else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.");
                passwordField.clear();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load mapping: " + e.getMessage());
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