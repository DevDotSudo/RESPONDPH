package com.ionres.respondph.main;

import com.ionres.respondph.util.*;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainFrameController {
    private static MainFrameController INSTANCE;
    public static MainFrameController getInstance() { return INSTANCE; }
    @FXML private VBox contentArea;
    @FXML private VBox smsProgressToast;
    @FXML private VBox smsToastBody;
    @FXML private Label smsProgressTitle;
    @FXML private Label smsProgressMessage;
    @FXML private ProgressBar smsProgressBar;
    @FXML private Button smsMinimizeBtn;
    @FXML private Button smsCloseBtn;

    // Footer (NEW)
    @FXML private HBox footerBar;
    @FXML private Label footerStatusLabel;
    @FXML private Button btnShowProgress;

    private boolean smsMinimized = false;
    private Task<?> currentSmsTask;

    // ====== Existing section headers ======
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
        INSTANCE = this;
        if (smsMinimizeBtn != null) {
            smsMinimizeBtn.setOnAction(e -> minimizeSmsToastToFooter());
        }
        if (smsCloseBtn != null) {
            smsCloseBtn.setOnAction(e -> {
                if (currentSmsTask != null) currentSmsTask.cancel();
                hideSmsProgress();
            });
        }
        if (btnShowProgress != null) {
            btnShowProgress.setOnAction(e -> showSmsProgressFromFooter());
        }

        if (smsProgressToast != null) {
            smsProgressToast.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            smsProgressToast.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        }

        if (smsToastBody != null) {
            smsToastBody.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        }

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

        // hide footer initially
        hideFooter();
        hideSmsProgress();
    }

    // =========================
    // SMS Progress API (CALL THIS FROM SendSMS)
    // =========================

    /** Call once before starting sending. */
    public void showSmsProgress(String title, int total) {
        Platform.runLater(() -> {
            smsMinimized = false;

            if (smsProgressTitle != null) smsProgressTitle.setText(title == null ? "Sending SMS" : title);
            setSmsCount(0, total);

            if (smsToastBody != null) {
                smsToastBody.setVisible(true);
                smsToastBody.setManaged(true);
            }
            if (smsMinimizeBtn != null) {
                smsMinimizeBtn.setVisible(true);
                smsMinimizeBtn.setManaged(true);
            }

            // show toast
            if (smsProgressToast != null) {
                smsProgressToast.setVisible(true);
                smsProgressToast.setManaged(true);
            }
            // hide footer while toast is visible
            hideFooter();
        });
    }

    /** Update UI: "Sending 1 of 200" and progress bar value. */
    public void setSmsCount(int sent, int total) {
        Platform.runLater(() -> {
            String msg = "Sending " + sent + " of " + total;

            if (smsProgressMessage != null) smsProgressMessage.setText(msg);
            if (smsProgressBar != null) {
                double p = (total <= 0) ? 0 : (sent * 1.0 / total);
                smsProgressBar.setProgress(p);
            }

            // If minimized, keep footer updated
            if (smsMinimized) {
                showFooter(msg);
            } else {
                if (footerStatusLabel != null) footerStatusLabel.setText(msg);
            }
        });
    }

    /** Optional: bind to a Task so it auto-hides on finish/fail/cancel. */
    public void bindSmsTask(Task<?> task) {
        Platform.runLater(() -> {
            currentSmsTask = task;
            if (task == null) return;

            task.setOnSucceeded(e -> hideSmsProgress());
            task.setOnFailed(e -> hideSmsProgress());
            task.setOnCancelled(e -> hideSmsProgress());
        });
    }

    /** Hide everything (toast + footer). */
    public void hideSmsProgress() {
        Platform.runLater(() -> {
            smsMinimized = false;
            currentSmsTask = null;

            if (smsProgressToast != null) {
                smsProgressToast.setVisible(false);
                smsProgressToast.setManaged(false);
            }
            hideFooter();
        });
    }

    private void minimizeSmsToastToFooter() {
        if (smsMinimized) return;
        smsMinimized = true;

        // hide toast body
        if (smsToastBody != null) {
            smsToastBody.setVisible(false);
            smsToastBody.setManaged(false);
        }
        // hide toast itself (so footer is the handle)
        if (smsProgressToast != null) {
            smsProgressToast.setVisible(false);
            smsProgressToast.setManaged(false);
        }

        // show footer with latest message
        String msg = (smsProgressMessage != null) ? smsProgressMessage.getText() : "Sending...";
        showFooter(msg);
    }

    private void showSmsProgressFromFooter() {
        if (!smsMinimized) return; // only relevant when minimized

        smsMinimized = false;

        if (smsToastBody != null) {
            smsToastBody.setVisible(true);
            smsToastBody.setManaged(true);
        }
        if (smsProgressToast != null) {
            smsProgressToast.setVisible(true);
            smsProgressToast.setManaged(true);
        }

        hideFooter();
    }

    private void showFooter(String text) {
        if (footerStatusLabel != null) footerStatusLabel.setText(text == null ? "SMS sending..." : text);
        if (footerBar != null) {
            footerBar.setVisible(true);
            footerBar.setManaged(true);
        }
    }

    private void hideFooter() {
        if (footerBar != null) {
            footerBar.setVisible(false);
            footerBar.setManaged(false);
        }
    }

    // =========================
    // Existing navigation/section code
    // =========================

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
        if (sectionBtn == null || sectionContent == null || chevronIcon == null) return;
        sectionBtn.setOnAction(e -> toggleSectionExclusive(sectionContent, chevronIcon));
    }

    private void toggleSectionExclusive(VBox sectionContent, FontAwesomeIconView chevronIcon) {
        boolean willOpen = !sectionContent.isVisible();

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
