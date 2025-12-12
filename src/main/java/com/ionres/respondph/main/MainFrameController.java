package com.ionres.respondph.main;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.event.EventHandler;

public class MainFrameController {
    
    @FXML
    private VBox contentArea;

    @FXML
    private Button dashboardBtn;

    @FXML
    private Button manageAdminBtn;

    @FXML
    private Button manageBeneficiariesBtn;

    @FXML
    private Button aidsBtn;

    @FXML Button logoutBtn;

    @FXML
    private Button familyMembersBtn;

    @FXML
    private Button sendSmsBtn;

    @FXML
    private Button settingsBtn;

    private Button activeBtn;

    public void initialize() {
        loadPage("/view/pages/Dashboard.fxml");
        activeButton(dashboardBtn);

        EventHandler<ActionEvent> handlers = this::handleActions;

        dashboardBtn.setOnAction(handlers);
        manageAdminBtn.setOnAction(handlers);
        manageBeneficiariesBtn.setOnAction(handlers);
        aidsBtn.setOnAction(handlers);
        familyMembersBtn.setOnAction(handlers);
        sendSmsBtn.setOnAction(handlers);
        settingsBtn.setOnAction(handlers);
        logoutBtn.setOnAction(handlers);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if(src == dashboardBtn) {
            handleDashboard();
        }

        else if(src == manageAdminBtn) {
            handleManageAdmins();
        }

        else if(src == manageBeneficiariesBtn) {
            handleManageBeneficiaries();
        }

        else if(src == aidsBtn) {
            handleAids();
        }

        else if(src == familyMembersBtn) {
            handleFamilyMembers();
        }

        else if(src == sendSmsBtn) {
            handleSendSms();
        }

        else if(src == settingsBtn) {
            handleSettings();
        }

        else if(src == logoutBtn) {
            handleLogout();
        }
    }

    private void handleDashboard() {
        loadPage("/view/pages/Dashboard.fxml");
        activeButton(dashboardBtn);
    }
    
    private void handleManageAdmins() {
        loadPage("/view/pages/ManageAdmins.fxml");
        activeButton(manageAdminBtn);
    }
    
    private void handleManageBeneficiaries() {
        loadPage("/view/pages/ManageBeneficiaries.fxml");
        activeButton(manageBeneficiariesBtn);
    }

    private void handleAids() {
        loadPage("/view/pages/Aids.fxml");
        activeButton(aidsBtn);
    }

    private void handleFamilyMembers() {
        loadPage("/view/pages/FamilyMembers.fxml");
        activeButton(familyMembersBtn);
    }

    private void handleSendSms() {
        loadPage("/view/pages/SendSMS.fxml");
        activeButton(sendSmsBtn);
    }

    private void handleSettings() {
        loadPage("/view/pages/Settings.fxml");
        activeButton(settingsBtn);
    }

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