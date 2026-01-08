package com.ionres.respondph.main;

import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
    private Button disasterBtn;

    @FXML
    private Button disasterDamageBtn;

    @FXML
    private Button sendSmsBtn;

    @FXML
    private Button settingsBtn;

    private Button activeBtn;

    public void initialize() {
        loadPage("/view/dashboard/Dashboard.fxml");
        activeButton(dashboardBtn);

        EventHandler<ActionEvent> handlers = this::handleActions;

        dashboardBtn.setOnAction(handlers);
        manageAdminBtn.setOnAction(handlers);
        manageBeneficiariesBtn.setOnAction(handlers);
        disasterBtn.setOnAction(handlers);
        disasterDamageBtn.setOnAction(handlers);
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

        else if(src == disasterBtn) {
            handleDisaster();
        }

        else if(src == disasterDamageBtn) {
            handleDisasterDamage();
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
        loadPage("/view/dashboard/Dashboard.fxml");
        activeButton(dashboardBtn);
    }

    private void handleManageAdmins() {
        loadPage("/view/admin/ManageAdmins.fxml");
        activeButton(manageAdminBtn);
    }

    private void handleManageBeneficiaries() {
        loadPage("/view/beneficiary/ManageBeneficiaries.fxml");
        activeButton(manageBeneficiariesBtn);
    }

    private void handleAids() {
        loadPage("/view/aid_type/AidType.fxml");
        activeButton(aidsBtn);
    }

    private void handleFamilyMembers() {
        loadPage("/view/family/FamilyMembers.fxml");
        activeButton(familyMembersBtn);
    }

    private void handleDisaster() {
        loadPage("/view/disaster/Disaster.fxml");
        activeButton(disasterBtn);
    }

    private void handleDisasterDamage() {
        loadPage("/view/disaster_damage/DisasterDamage.fxml");
        activeButton(disasterDamageBtn);
    }

    private void handleSendSms() {
        loadPage("/view/send_sms/SendSMS.fxml");
        activeButton(sendSmsBtn);
    }

    private void handleSettings() {
        loadPage("/view/settings/Settings.fxml");
        activeButton(settingsBtn);
    }

    private void handleLogout() {

    }

    private void loadPage(String fxml) {
        SceneManager.SceneEntry<?> entry = SceneManager.load(fxml);
        contentArea.getChildren().setAll(entry.getRoot());
    }

    private void activeButton(Button btnId) {
        if(activeBtn != null) {
            activeBtn.getStyleClass().remove("nav-button-active");
        }

        btnId.getStyleClass().add("nav-button-active");
        activeBtn = btnId;
    }
}