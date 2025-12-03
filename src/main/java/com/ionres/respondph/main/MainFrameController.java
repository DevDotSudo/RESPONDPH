package com.ionres.respondph.main;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class MainFrameController {
    
    @FXML
    private VBox contentArea;

    @FXML
    private Button dashboardBtn;

    @FXML
    private Button manageAdminBtn;

    @FXML
    private Button manageBeneficiariesBtn;

    private Button activeBtn;

    public void initialize() {
        loadPage("/view/pages/Dashboard.fxml");
        activeButton(dashboardBtn);
    }

    @FXML
    private void handleDashboard() {
        loadPage("/view/pages/Dashboard.fxml");
        activeButton(dashboardBtn);
    }
    
    @FXML
    private void handleManageAdmins() {
        loadPage("/view/pages/ManageAdmins.fxml");
        activeButton(manageAdminBtn);
    }
    
    @FXML
    private void handleManageBeneficiaries() {
        loadPage("/view/pages/ManageBeneficiaries.fxml");
        activeButton(manageBeneficiariesBtn);
    }
    
    @FXML
    private void handleLogout() {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Logout");
            alert.setHeaderText(null);
            alert.setContentText("Do you want to logout?");
            alert.showAndWait();
    }
    
    private void loadPage(String fxmlFile) {
        try {
            AnchorPane pane = FXMLLoader.load(getClass().getResource(fxmlFile));
            contentArea.getChildren().setAll(pane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void activeButton(Button btnId) {
        if(activeBtn != null) {
            activeBtn.getStyleClass().remove("nav-button-active");
        }

        btnId.getStyleClass().add("nav-button-active");
        activeBtn = btnId;
    }
}