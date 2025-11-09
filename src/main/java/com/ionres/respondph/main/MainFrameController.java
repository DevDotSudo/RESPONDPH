package com.ionres.respondph.main;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class MainFrameController {
    
    @FXML
    private VBox contentArea;
    
    public void initialize() {
        loadPage("/view/Dashboard.fxml");
    }

    @FXML
    private void handleDashboard() {
        loadPage("/view/Dashboard.fxml");
    }
    
    @FXML
    private void handleManageAdmins() {
        loadPage("/view/ManageAdmins.fxml");
    }
    
    @FXML
    private void handleManageBeneficiaries() {
        System.out.println("Manage Beneficiaries clicked");
    }
    
    @FXML
    private void handleLogout() {
        System.out.println("Logout clicked");
    }
    
    private void loadPage(String fxmlFile) {
        try {
            AnchorPane pane = FXMLLoader.load(getClass().getResource(fxmlFile));
            contentArea.getChildren().setAll(pane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}