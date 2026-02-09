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

    // Section headers (some may be absent depending on FXML)
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

    // Nav buttons
    @FXML private Button dashboardBtn;
    @FXML private Button manageAdminBtn;
    @FXML private Button manageBeneficiariesBtn;
    @FXML private Button familyMembersBtn;

    @FXML private Button disasterBtn;
    @FXML private Button disasterMappingBtn;
    @FXML private Button disasterDamageBtn;

    @FXML private Button vulnerabilityBtn;

    @FXML private Button evacBtn;
    @FXML private Button evacPlanBtn;

    @FXML private Button aidTypeBtn;
    @FXML private Button aidBtn;

    @FXML private Button sendSmsBtn;
    @FXML private Button settingsBtn;
    @FXML private Button logoutBtn;

    private Button activeBtn;

    private static final Duration CHEVRON_DURATION = Duration.millis(180);
    private static final double CHEVRON_COLLAPSED = 0;
    private static final double CHEVRON_EXPANDED = 90;

    @FXML
    public void initialize() {

        // Only wires sections that exist in your FXML (prevents NPE)
        setupSectionToggle(managementSectionBtn, managementSectionContent, managementSectionIcon);
        setupSectionToggle(disasterSectionBtn, disasterSectionContent, disasterSectionIcon);
        setupSectionToggle(aidsSectionBtn, aidsSectionContent, aidsSectionIcon);
        setupSectionToggle(evacSectionBtn, evacSectionContent, evacSectionIcon);

        collapseAllSections(); // collapses only those that exist

        // Default page
        loadPage("/view/dashboard/Dashboard.fxml");
        activeButton(dashboardBtn);

        EventHandler<ActionEvent> handlers = this::handleActions;

        if (dashboardBtn != null) dashboardBtn.setOnAction(handlers);
        if (manageAdminBtn != null) manageAdminBtn.setOnAction(handlers);
        if (manageBeneficiariesBtn != null) manageBeneficiariesBtn.setOnAction(handlers);
        if (familyMembersBtn != null) familyMembersBtn.setOnAction(handlers);

        if (disasterBtn != null) disasterBtn.setOnAction(handlers);
        if (disasterMappingBtn != null) disasterMappingBtn.setOnAction(handlers);
        if (disasterDamageBtn != null) disasterDamageBtn.setOnAction(handlers);

        if (aidBtn != null) aidBtn.setOnAction(handlers);
        if (aidTypeBtn != null) aidTypeBtn.setOnAction(handlers);

        if (evacBtn != null) evacBtn.setOnAction(handlers);
        if (evacPlanBtn != null) evacPlanBtn.setOnAction(handlers);

        if (vulnerabilityBtn != null) vulnerabilityBtn.setOnAction(handlers);
        if (sendSmsBtn != null) sendSmsBtn.setOnAction(handlers);
        if (settingsBtn != null) settingsBtn.setOnAction(handlers);
        if (logoutBtn != null) logoutBtn.setOnAction(handlers);
    }

    private void collapseAllSections() {
        collapseSection(managementSectionContent, managementSectionIcon);
        collapseSection(disasterSectionContent, disasterSectionIcon);
        collapseSection(aidsSectionContent, aidsSectionIcon);
        collapseSection(evacSectionContent, evacSectionIcon);
    }

    private void collapseSection(VBox sectionContent, FontAwesomeIconView chevronIcon) {
        if (sectionContent == null || chevronIcon == null) return;
        sectionContent.setVisible(false);
        sectionContent.setManaged(false);
        chevronIcon.setRotate(CHEVRON_COLLAPSED);
    }

    private void setupSectionToggle(Button sectionBtn, VBox sectionContent, FontAwesomeIconView chevronIcon) {
        // This is the actual fix for your NullPointerException
        if (sectionBtn == null || sectionContent == null || chevronIcon == null) return;
        sectionBtn.setOnAction(e -> toggleSectionExclusive(sectionContent, chevronIcon));
    }

    private void toggleSectionExclusive(VBox sectionContent, FontAwesomeIconView chevronIcon) {
        boolean willOpen = !sectionContent.isVisible();

        // Collapse only those that exist
        collapseAllSections();

        if (willOpen) {
            sectionContent.setVisible(true);
            sectionContent.setManaged(true);
            animateChevron(chevronIcon, CHEVRON_EXPANDED);
        } else {
            sectionContent.setVisible(false);
            sectionContent.setManaged(false);
            animateChevron(chevronIcon, CHEVRON_COLLAPSED);
        }
    }

    private void ensureSectionOpen(VBox sectionContent, FontAwesomeIconView chevronIcon) {
        if (sectionContent == null || chevronIcon == null) return;
        if (!sectionContent.isVisible()) {
            collapseAllSections();
            sectionContent.setVisible(true);
            sectionContent.setManaged(true);
            animateChevron(chevronIcon, CHEVRON_EXPANDED);
        }
    }

    private void animateChevron(FontAwesomeIconView icon, double toAngle) {
        if (icon == null) return;
        RotateTransition rotate = new RotateTransition(CHEVRON_DURATION, icon);
        rotate.setToAngle(toAngle);
        rotate.play();
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == dashboardBtn) {
            handleDashboard();
        } else if (src == manageAdminBtn) {
            ensureSectionOpen(managementSectionContent, managementSectionIcon);
            handleManageAdmins();
        } else if (src == manageBeneficiariesBtn) {
            ensureSectionOpen(managementSectionContent, managementSectionIcon);
            handleManageBeneficiaries();
        } else if (src == familyMembersBtn) {
            ensureSectionOpen(managementSectionContent, managementSectionIcon);
            handleFamilyMembers();
        } else if (src == disasterBtn) {
            ensureSectionOpen(disasterSectionContent, disasterSectionIcon);
            handleDisaster();
        } else if (src == disasterMappingBtn) {
            ensureSectionOpen(disasterSectionContent, disasterSectionIcon);
            handleDisasterMapping();
        } else if (src == disasterDamageBtn) {
            ensureSectionOpen(disasterSectionContent, disasterSectionIcon);
            handleDisasterDamage();
        } else if (src == vulnerabilityBtn) {
            handleVulnerabilityIndicator();
        } else if (src == sendSmsBtn) {
            handleSendSms();
        } else if (src == settingsBtn) {
            handleSettings();
        } else if (src == logoutBtn) {
            handleLogout();
        }
        // Aids/Evac child buttons will work once you add them in FXML
        else if (src == aidBtn) {
            ensureSectionOpen(aidsSectionContent, aidsSectionIcon);
            handleAid();
        } else if (src == aidTypeBtn) {
            ensureSectionOpen(aidsSectionContent, aidsSectionIcon);
            handleAidType();
        } else if (src == evacBtn) {
            ensureSectionOpen(evacSectionContent, evacSectionIcon);
            handleEvacSite();
        } else if (src == evacPlanBtn) {
            ensureSectionOpen(evacSectionContent, evacSectionIcon);
            handleEvacPlan();
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

    private void handleEvacPlan() {
        loadPage("/view/evacuation_plan/EvacuationPlan.fxml");
        activeButton(evacPlanBtn);
    }

    private void handleVulnerabilityIndicator() {
        DashboardRefresher.refreshFlds();
        loadPage("/view/vulnerability_indicator/VulnerabilityIndicator.fxml");
        activeButton(vulnerabilityBtn);
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
        boolean confirm = AlertDialogManager.showConfirmation("Logout", "Do you want to logout?");
        if (confirm) {
            AppPreferences prefs = new AppPreferences();
            prefs.clearRememberMe();
            SessionManager.getInstance().clearSession();
            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            stage.close();
            SceneManager.showStage("/view/auth/Login.fxml", "RESPONDPH - Login");
        }
    }

    private void loadPage(String fxml) {
        SceneManager.SceneEntry<?> entry = SceneManager.load(fxml);
        Parent root = entry.getRoot();

        // Java 11 compatible instanceof
        if (root instanceof Region) {
            Region region = (Region) root;
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            VBox.setVgrow(region, Priority.ALWAYS);
        }

        contentArea.getChildren().setAll(root);
    }

    private void activeButton(Button btnId) {
        if (btnId == null) return;

        if (activeBtn != null) {
            activeBtn.getStyleClass().remove("nav-button-active");
            activeBtn.getStyleClass().remove("nav-button-child-active");
        }

        activeBtn = btnId;

        if (btnId.getStyleClass().contains("nav-button-child")) {
            if (!activeBtn.getStyleClass().contains("nav-button-child-active")) {
                activeBtn.getStyleClass().add("nav-button-child-active");
            }
        } else {
            if (!activeBtn.getStyleClass().contains("nav-button-active")) {
                activeBtn.getStyleClass().add("nav-button-active");
            }
        }
    }
}
