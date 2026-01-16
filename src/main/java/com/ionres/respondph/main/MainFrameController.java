package com.ionres.respondph.main;

import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.DashboardRefresher;
import com.ionres.respondph.util.SceneManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.animation.RotateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainFrameController {

    @FXML private VBox contentArea;

    @FXML private Button managementSectionBtn;
    @FXML private Button disasterSectionBtn;
    @FXML private Button aidsSectionBtn;

    @FXML private VBox managementSectionContent;
    @FXML private VBox disasterSectionContent;
    @FXML private VBox aidsSectionContent;

    @FXML private FontAwesomeIconView managementSectionIcon;
    @FXML private FontAwesomeIconView disasterSectionIcon;
    @FXML private FontAwesomeIconView aidsSectionIcon;

    @FXML private Button dashboardBtn;
    @FXML private Button manageAdminBtn;
    @FXML private Button manageBeneficiariesBtn;
    @FXML private Button familyMembersBtn;
    @FXML private Button disasterBtn;
    @FXML private Button disasterDamageBtn;
    @FXML private Button vulnerabilityBtn;
    @FXML private Button aidsBtn;
    @FXML private Button sendSmsBtn;
    @FXML private Button settingsBtn;
    @FXML private Button logoutBtn;

    private Button activeBtn;

    @FXML
    public void initialize() {
        setupSectionToggle(managementSectionBtn, managementSectionContent, managementSectionIcon);
        setupSectionToggle(disasterSectionBtn, disasterSectionContent, disasterSectionIcon);
        setupSectionToggle(aidsSectionBtn, aidsSectionContent, aidsSectionIcon);
        loadPage("/view/dashboard/Dashboard.fxml");
        activeButton(dashboardBtn);

        EventHandler<ActionEvent> handlers = this::handleActions;
        dashboardBtn.setOnAction(handlers);
        manageAdminBtn.setOnAction(handlers);
        manageBeneficiariesBtn.setOnAction(handlers);
        disasterBtn.setOnAction(handlers);
        disasterDamageBtn.setOnAction(handlers);
        aidsBtn.setOnAction(handlers);
        vulnerabilityBtn.setOnAction(handlers);
        familyMembersBtn.setOnAction(handlers);
        sendSmsBtn.setOnAction(handlers);
        settingsBtn.setOnAction(handlers);
        logoutBtn.setOnAction(handlers);
    }

    private void setupSectionToggle(Button sectionBtn, VBox sectionContent, FontAwesomeIconView chevronIcon) {
        sectionBtn.setOnAction(e -> toggleSection(sectionContent, chevronIcon));
    }

    private void toggleSection(VBox sectionContent, FontAwesomeIconView chevronIcon) {
        boolean isVisible = sectionContent.isVisible();

        sectionContent.setVisible(!isVisible);
        sectionContent.setManaged(!isVisible);

        RotateTransition rotate = new RotateTransition(Duration.millis(200), chevronIcon);
        rotate.setToAngle(isVisible ? 0 : 90);
        rotate.play();
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
        else if(src == vulnerabilityBtn) {
            DashboardRefresher.refreshFlds();
            handleVulnerabilityIndicator();
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

    private void handleVulnerabilityIndicator() {
        loadPage("/view/vulnerability_indicator/VulnerabilityIndicator.fxml");
        activeButton(vulnerabilityBtn);
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
        boolean confirm = AlertDialogManager.showConfirmation(
                "Logout",
                "Do you want to logout?"
        );

        if (confirm) {
            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            stage.close();
            SceneManager.showStage(
                    "/view/auth/Login.fxml",
                    "RESPONDPH - Login"
            );
        }
    }

    private void loadPage(String fxml) {
        SceneManager.SceneEntry<?> entry = SceneManager.load(fxml);
        contentArea.getChildren().setAll(entry.getRoot());
    }

    private void activeButton(Button btnId) {
        if(activeBtn != null) {
            activeBtn.getStyleClass().remove("nav-button-active");
            activeBtn.getStyleClass().remove("nav-button-child-active");
        }
        activeBtn = btnId;
    }
}