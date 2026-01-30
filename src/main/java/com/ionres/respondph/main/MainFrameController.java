package com.ionres.respondph.main;

import com.ionres.respondph.util.*;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.animation.RotateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainFrameController {

    @FXML private VBox contentArea;

    @FXML private Button managementSectionBtn;
    @FXML private Button disasterSectionBtn;
    @FXML private Button aidsSectionBtn;
    @FXML private Button evacSectionBtn;

    @FXML private VBox managementSectionContent;
    @FXML private VBox disasterSectionContent;
    @FXML private VBox aidsSectionContent;
    @FXML private VBox evacSectionContent;

    @FXML private FontAwesomeIconView managementSectionIcon;
    @FXML private FontAwesomeIconView disasterSectionIcon;
    @FXML private FontAwesomeIconView aidsSectionIcon;
    @FXML private FontAwesomeIconView evacSectionIcon;

    @FXML private Button dashboardBtn;
    @FXML private Button manageAdminBtn;
    @FXML private Button manageBeneficiariesBtn;
    @FXML private Button familyMembersBtn;
    @FXML private Button disasterBtn;
    @FXML private Button disasterMappingBtn;
    @FXML private Button disasterDamageBtn;
    @FXML private Button vulnerabilityBtn;
    @FXML private Button evacBtn;
    @FXML private Button aidTypeBtn;
    @FXML private Button aidBtn;
    @FXML private Button sendSmsBtn;
    @FXML private Button settingsBtn;
    @FXML private Button logoutBtn;

    private Button activeBtn;

    @FXML
    public void initialize() {
        setupSectionToggle(managementSectionBtn, managementSectionContent, managementSectionIcon);
        setupSectionToggle(disasterSectionBtn, disasterSectionContent, disasterSectionIcon);
        setupSectionToggle(aidsSectionBtn, aidsSectionContent, aidsSectionIcon);
        setupSectionToggle(evacSectionBtn, evacSectionContent, evacSectionIcon);
        collapseAllSections();
        
        loadPage("/view/dashboard/Dashboard.fxml");
        activeButton(dashboardBtn);

        EventHandler<ActionEvent> handlers = this::handleActions;
        dashboardBtn.setOnAction(handlers);
        manageAdminBtn.setOnAction(handlers);
        manageBeneficiariesBtn.setOnAction(handlers);
        disasterBtn.setOnAction(handlers);
        disasterMappingBtn.setOnAction(handlers);
        disasterDamageBtn.setOnAction(handlers);
        aidBtn.setOnAction(handlers);
        aidTypeBtn.setOnAction(handlers);
        evacBtn.setOnAction(handlers);
        vulnerabilityBtn.setOnAction(handlers);
        familyMembersBtn.setOnAction(handlers);
        sendSmsBtn.setOnAction(handlers);
        settingsBtn.setOnAction(handlers);
        logoutBtn.setOnAction(handlers);
    }

    private void collapseAllSections() {
        collapseSection(managementSectionContent, managementSectionIcon);
        collapseSection(disasterSectionContent, disasterSectionIcon);
        collapseSection(aidsSectionContent, aidsSectionIcon);
        collapseSection(evacSectionContent, evacSectionIcon);
    }

    private void collapseSection(VBox sectionContent, FontAwesomeIconView chevronIcon) {
        sectionContent.setVisible(false);
        sectionContent.setManaged(false);
        chevronIcon.setRotate(0);
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
        else if(src == aidBtn) {
            handleAid();
        }
        else if (src == aidTypeBtn) {
            handleAidType();
        }
        else if (src == evacBtn) {
            handleEvacSite();
        }
        else if(src == vulnerabilityBtn) {
            handleVulnerabilityIndicator();
        }
        else if(src == familyMembersBtn) {
            handleFamilyMembers();
        }
        else if(src == disasterBtn) {
            handleDisaster();
        }
        else if(src == disasterMappingBtn) {
            handleDisasterMapping();
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

    private void handleAid() {
        loadPage("/view/aid/Aid.fxml");
        activeButton(aidBtn);
    }

    private void handleAidType() {
        loadPage("/view/aid_type/AidType.fxml");
        activeButton(aidTypeBtn);
    }

    private void handleEvacSite() {
        loadPage("/view/evac_site/EvacSite.fxml");
        activeButton(evacBtn);
    }

    private void handleVulnerabilityIndicator() {
        DashboardRefresher.refreshFlds();
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

    private void handleDisasterMapping() {
        loadPage("/view/disaster_mapping/DisasterMapping.fxml");
        activeButton(disasterMappingBtn);
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
            AppPreferences prefs = new AppPreferences();
            prefs.clearRememberMe();
            SessionManager.getInstance().clearSession();
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
        Parent root = entry.getRoot();

        if (root instanceof Region) {
            Region region = (Region) root;
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            VBox.setVgrow(region, Priority.ALWAYS);
        }

        contentArea.getChildren().setAll(root);
    }

    private void activeButton(Button btnId) {
        if (activeBtn != null) {
            activeBtn.getStyleClass().remove("nav-button-active");
            activeBtn.getStyleClass().remove("nav-button-child-active");
        }
        activeBtn = btnId;

        if (btnId.getStyleClass().contains("nav-button-child")) {
            activeBtn.getStyleClass().add("nav-button-child-active");
        } else {
            activeBtn.getStyleClass().add("nav-button-active");
        }
    }
}