package com.ionres.respondph.dashboard;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.common.model.BeneficiaryMarker;
import com.ionres.respondph.common.model.EvacSiteMarker;
import com.ionres.respondph.common.model.FamilyMemberModel;
import com.ionres.respondph.main.MainFrameController;
import com.ionres.respondph.util.*;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardController {

    // ── Services ──────────────────────────────────────────────────────────────
    private final DashBoardService dashBoardService = AppContext.dashBoardService;
    private final Mapping mapping = new Mapping();

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<BeneficiaryMarker> beneficiaries = new ArrayList<>();
    private final List<EvacSiteMarker> evacSites = new ArrayList<>();

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private Pane mapContainer;
    @FXML private Label totalBeneficiaryLabel;
    @FXML private Label totalDisastersLabel;
    @FXML private Label totalAidsLabel;
    @FXML private Label currentDateLabel;
    @FXML private Label currentTimeLabel;
    @FXML private Label totalEvacutaionSiteLabel;
    @FXML private Label adminNameText;
    @FXML private HBox adminNameLabel;          // the clickable admin row
    @FXML private StackPane adminAreaPane;
    @FXML private VBox adminDropdown;
    @FXML private HBox changePasswordMenuItem;  // fx:id is on the HBox row
    @FXML private Button searchToggleBtn;
    @FXML private TextField beneficiarySearchBox;
    @FXML private HBox searchOverlay;
    @FXML private StackPane cardBeneficiary;
    @FXML private StackPane cardDisasters;
    @FXML private StackPane cardAids;
    @FXML private StackPane cardEvacuationSite;
    @FXML private VBox searchBoxWrap;
    @FXML private ListView<String> beneficiarySearchList;

    // ── Change password FXML ──────────────────────────────────────────────────
    @FXML private StackPane changePasswordOverlay;
    @FXML private VBox changePasswordDialog;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button closePasswordDialogBtn;
    @FXML private Button cancelPasswordBtn;
    @FXML private Button confirmPasswordBtn;
    @FXML private Button toggleCurrentPwBtn;
    @FXML private Button toggleNewPwBtn;
    @FXML private Button toggleConfirmPwBtn;
    @FXML private Label passwordMessageLabel;

    // ── Password visibility state ─────────────────────────────────────────────
    private boolean showCurrentPw = false;
    private boolean showNewPw     = false;
    private boolean showConfirmPw = false;
    private TextField currentPwVisible;
    private TextField newPwVisible;
    private TextField confirmPwVisible;

    // ── Marker images ─────────────────────────────────────────────────────────
    private Image personMarker;
    private Image evacSiteMarker;

    private static final double MIN_ZOOM_FOR_MARKERS  = 16.0;
    private static final double MARKER_WIDTH          = 32;
    private static final double MARKER_HEIGHT         = 32;
    private static final double MARKER_OFFSET_Y       = MARKER_HEIGHT;
    private static final double EVAC_MARKER_WIDTH     = 32;
    private static final double EVAC_MARKER_HEIGHT    = 32;
    private static final double EVAC_MARKER_OFFSET_Y  = EVAC_MARKER_HEIGHT;

    // ── Drag / zoom ───────────────────────────────────────────────────────────
    private double dragStartX, dragStartY;
    private double currentCenterLat;
    private double currentCenterLon;
    private double currentZoom = 13.0;
    private boolean isDragging = false;
    private static final double DRAG_THRESHOLD = 4.0;

    // ── Selection ─────────────────────────────────────────────────────────────
    private BeneficiaryMarker selectedBeneficiary = null;
    private VBox infoPanel;
    private EvacSiteMarker selectedEvacSite = null;
    private VBox evacInfoPanel;

    // ── Search ────────────────────────────────────────────────────────────────
    private final ObservableList<String> searchItems = FXCollections.observableArrayList();
    private boolean suppressListener = false;

    // ── Boundary ──────────────────────────────────────────────────────────────
    private final double[][] boundary = {
            {11.0775,122.7315},{11.1031,122.7581},{11.0925,122.7618},
            {11.0912,122.7648},{11.0897,122.7662},{11.0896,122.7796},
            {11.0756,122.7942},{11.0674,122.7957},{11.0584,122.7991},
            {11.0533,122.8023},{11.0416,122.8200},{10.9914,122.8514},
            {10.9907,122.8483},{10.9899,122.8462},{10.9904,122.8449},
            {10.9920,122.8447},{10.9951,122.8433},{10.9968,122.8443},
            {10.9966,122.8417},{10.9963,122.8340},{10.9988,122.8287},
            {10.9976,122.8156},{10.9909,122.7957},{10.9919,122.7865},
            {11.0034,122.7861},{11.0480,122.7722},{11.0613,122.7499},
            {11.0681,122.7489},{11.0719,122.7453},{11.0761,122.7454}
    };

    // ═════════════════════════════════════════════════════════════════════════
    // initialize
    // ═════════════════════════════════════════════════════════════════════════
    public void initialize() {
        AdminModel admin1 = SessionManager.getInstance().getCurrentAdmin();
        System.out.println("=== DashboardController.initialize() ===");
        if (admin1 != null) System.out.println("Username: " + admin1.getUsername());

        searchToggleBtn.setOnAction(e -> searchToggle());

        Platform.runLater(() -> {

            if (searchOverlay != null) {
                searchOverlay.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                searchOverlay.setPickOnBounds(false);
                searchOverlay.setSpacing(8);
                searchOverlay.setPadding(new Insets(10, 10, 0, 0));
            }

            try {
                personMarker = new Image(getClass().getResourceAsStream("/images/person_marker.png"));
                if (personMarker.isError()) personMarker = null;
                evacSiteMarker = new Image(getClass().getResourceAsStream("/images/location-pin.png"));
                if (evacSiteMarker.isError()) evacSiteMarker = null;
            } catch (Exception e) {
                personMarker = null;
                evacSiteMarker = null;
            }

            mapping.init(mapContainer);
            mapping.setAfterRedraw(() -> {
                drawBoundary();
                drawEvacSites();
                drawBeneficiaries();
                repositionPanel();
                repositionEvacPanel();   // ← add this
            });

            buildInfoPanel();
            wireSearchComboBoxInline();
            wireAdminDropdown();
            wireChangePasswordDialog();

            mapContainer.setOnMousePressed(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    isDragging = false;
                    BeneficiaryMarker hitB = findMarkerAtScreen(e.getX(), e.getY());
                    EvacSiteMarker hitE = findEvacSiteAtScreen(e.getX(), e.getY());
                    if (hitB == null && selectedBeneficiary != null) dismissPanel();
                    if (hitE == null && selectedEvacSite != null) dismissEvacPanel();
                    dragStartX = e.getX();
                    dragStartY = e.getY();
                }
            });

            mapContainer.setOnMouseDragged(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    double dx = e.getX() - dragStartX;
                    double dy = e.getY() - dragStartY;
                    if (!isDragging) {
                        if (Math.abs(dx) < DRAG_THRESHOLD && Math.abs(dy) < DRAG_THRESHOLD) return;
                        isDragging = true;
                    }
                    dragStartX = e.getX();
                    dragStartY = e.getY();
                    double tilesOnScreen  = Math.pow(2, currentZoom);
                    double degPerPixelLon = 360.0 / (tilesOnScreen * 256.0);
                    double degPerPixelLat = degPerPixelLon * Math.cos(Math.toRadians(currentCenterLat));
                    currentCenterLon -= dx * degPerPixelLon;
                    currentCenterLat += dy * degPerPixelLat;
                    mapping.setCenter(currentCenterLat, currentCenterLon, currentZoom);
                }
            });

            mapContainer.setOnMouseReleased(e -> isDragging = false);

            mapContainer.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                    if (isDragging) return;
                    BeneficiaryMarker hitB = findMarkerAtScreen(e.getX(), e.getY());
                    if (hitB != null) { selectBeneficiary(hitB, false); return; }
                    EvacSiteMarker hitE = findEvacSiteAtScreen(e.getX(), e.getY());
                    if (hitE != null) selectEvacSite(hitE);
                }
            });

            mapContainer.setOnScroll(e -> {
                double delta = e.getDeltaY() > 0 ? 0.5 : -0.5;
                currentZoom = Math.max(10.0, Math.min(19.0, currentZoom + delta));
                mapping.setCenter(currentCenterLat, currentCenterLon, currentZoom);
            });

            loadDashBoardData();
            loadBeneficiariesFromDb();
            loadEvacSitesFromDb();
            wireCardListeners();

            Timeline centerDelay = new Timeline(
                    new KeyFrame(Duration.millis(100), e -> centerMapOnBoundary()));
            centerDelay.setCycleCount(1);
            centerDelay.play();

            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy");
            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm:ss a");
            Timeline clock = new Timeline(
                    new KeyFrame(Duration.ZERO, e -> {
                        LocalDateTime now = LocalDateTime.now();
                        currentTimeLabel.setText(now.format(timeFmt));
                        currentDateLabel.setText(now.format(dateFmt));
                    }),
                    new KeyFrame(Duration.seconds(1))
            );
            clock.setCycleCount(Timeline.INDEFINITE);
            clock.play();

            SessionManager.getInstance().setOnSessionChanged(() -> {
                AdminModel admin = SessionManager.getInstance().getCurrentAdmin();
                if (admin != null) {
                    String display = (admin.getFirstname() != null && !admin.getFirstname().isEmpty())
                            ? admin.getFirstname() + " " + admin.getLastname()
                            : admin.getUsername();
                    adminNameText.setText(admin.getRole() + " : " + display);
                }
            });
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Admin dropdown
    // ═════════════════════════════════════════════════════════════════════════
    private void wireAdminDropdown() {

        // Set cursors programmatically (FXML cursor attribute not supported for Cursor.HAND)
        adminNameLabel.setCursor(javafx.scene.Cursor.HAND);
        changePasswordMenuItem.setCursor(javafx.scene.Cursor.HAND);

        // Toggle dropdown on click
        adminNameLabel.setOnMouseClicked(e -> {
            boolean open = !adminDropdown.isVisible();
            adminDropdown.setVisible(open);
            adminDropdown.setManaged(open);
            e.consume();
        });

        // Close when clicking anywhere outside the adminAreaPane
        adminAreaPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(
                        javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                        e -> {
                            if (!adminDropdown.isVisible()) return;
                            boolean insidePane = adminAreaPane
                                    .localToScene(adminAreaPane.getBoundsInLocal())
                                    .contains(e.getSceneX(), e.getSceneY());
                            if (!insidePane) {
                                adminDropdown.setVisible(false);
                                adminDropdown.setManaged(false);
                            }
                        });
            }
        });

        // "Change Password" row click
        changePasswordMenuItem.setOnMouseClicked(e -> {
            adminDropdown.setVisible(false);
            adminDropdown.setManaged(false);
            openChangePasswordDialog();
            e.consume();
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Change password dialog
    // ═════════════════════════════════════════════════════════════════════════
    private void wireChangePasswordDialog() {
        currentPwVisible = buildVisibleField(currentPasswordField);
        newPwVisible      = buildVisibleField(newPasswordField);
        confirmPwVisible  = buildVisibleField(confirmPasswordField);

        toggleCurrentPwBtn.setOnAction(e -> {
            showCurrentPw = !showCurrentPw;
            togglePasswordVisibility(currentPasswordField, currentPwVisible,
                    toggleCurrentPwBtn, showCurrentPw);
        });
        toggleNewPwBtn.setOnAction(e -> {
            showNewPw = !showNewPw;
            togglePasswordVisibility(newPasswordField, newPwVisible,
                    toggleNewPwBtn, showNewPw);
        });
        toggleConfirmPwBtn.setOnAction(e -> {
            showConfirmPw = !showConfirmPw;
            togglePasswordVisibility(confirmPasswordField, confirmPwVisible,
                    toggleConfirmPwBtn, showConfirmPw);
        });

        closePasswordDialogBtn.setOnAction(e -> closeChangePasswordDialog());
        cancelPasswordBtn.setOnAction(e -> closeChangePasswordDialog());

        // Click the dark backdrop to close
        changePasswordOverlay.setOnMouseClicked(e -> {
            if (e.getTarget() == changePasswordOverlay) closeChangePasswordDialog();
        });

        confirmPasswordBtn.setOnAction(e -> handleChangePassword());
    }

    private TextField buildVisibleField(PasswordField pf) {
        TextField tf = new TextField();
        tf.setPromptText(pf.getPromptText());
        tf.getStyleClass().add("cpd-input");
        tf.setVisible(false);
        tf.setManaged(false);
        HBox.setHgrow(tf, Priority.ALWAYS);
        tf.textProperty().bindBidirectional(pf.textProperty());
        return tf;
    }

    private void togglePasswordVisibility(PasswordField pf, TextField tf,
                                          Button eyeBtn, boolean show) {
        HBox parent = (HBox) pf.getParent();
        if (show) {
            int idx = parent.getChildren().indexOf(pf);
            if (idx >= 0 && !parent.getChildren().contains(tf)) {
                parent.getChildren().add(idx, tf);
            }
            pf.setVisible(false);
            pf.setManaged(false);
            tf.setVisible(true);
            tf.setManaged(true);
            eyeBtn.setOpacity(1.0);
        } else {
            pf.setVisible(true);
            pf.setManaged(true);
            tf.setVisible(false);
            tf.setManaged(false);
            eyeBtn.setOpacity(0.45);
        }
    }

    private void openChangePasswordDialog() {
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        hidePasswordMessage();

        if (showCurrentPw) { showCurrentPw = false;
            togglePasswordVisibility(currentPasswordField, currentPwVisible, toggleCurrentPwBtn, false); }
        if (showNewPw)     { showNewPw = false;
            togglePasswordVisibility(newPasswordField, newPwVisible, toggleNewPwBtn, false); }
        if (showConfirmPw) { showConfirmPw = false;
            togglePasswordVisibility(confirmPasswordField, confirmPwVisible, toggleConfirmPwBtn, false); }

        changePasswordOverlay.setVisible(true);
        changePasswordOverlay.setManaged(true);
        Platform.runLater(() -> currentPasswordField.requestFocus());
    }

    private void closeChangePasswordDialog() {
        changePasswordOverlay.setVisible(false);
        changePasswordOverlay.setManaged(false);
    }

    private void handleChangePassword() {
        String current = currentPasswordField.getText().trim();
        String newPw   = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (current.isEmpty() || newPw.isEmpty() || confirm.isEmpty()) {
            showPasswordMessage("All fields are required.", false);
            return;
        }
        if (newPw.length() < 8) {
            showPasswordMessage("New password must be at least 8 characters.", false);
            return;
        }
        if (!newPw.equals(confirm)) {
            showPasswordMessage("New password and confirmation do not match.", false);
            return;
        }
        if (newPw.equals(current)) {
            showPasswordMessage("New password must differ from the current password.", false);
            return;
        }

        AdminModel admin = SessionManager.getInstance().getCurrentAdmin();
        if (admin == null) {
            showPasswordMessage("Session expired. Please log in again.", false);
            return;
        }

        boolean success = dashBoardService.changePassword(admin.getId(), current, newPw);
        if (success) {
            showPasswordMessage("Password updated successfully!", true);
            Timeline close = new Timeline(
                    new KeyFrame(Duration.seconds(1.5), e -> closeChangePasswordDialog()));
            close.setCycleCount(1);
            close.play();
        } else {
            showPasswordMessage("Current password is incorrect.", false);
        }
    }

    private void showPasswordMessage(String message, boolean isSuccess) {
        passwordMessageLabel.setText(message);
        passwordMessageLabel.getStyleClass().removeAll("cpd-message-error", "cpd-message-success");
        passwordMessageLabel.getStyleClass().add(isSuccess ? "cpd-message-success" : "cpd-message-error");
        passwordMessageLabel.setVisible(true);
        passwordMessageLabel.setManaged(true);
    }

    private void hidePasswordMessage() {
        passwordMessageLabel.setVisible(false);
        passwordMessageLabel.setManaged(false);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Search
    // ═════════════════════════════════════════════════════════════════════════
    private boolean isSelectingFromList = false;

    private void wireSearchComboBoxInline() {
        beneficiarySearchList.setItems(searchItems);
        beneficiarySearchList.setFixedCellSize(36);
        beneficiarySearchList.setPrefWidth(280);
        beneficiarySearchList.setMinWidth(280);
        beneficiarySearchList.setMaxWidth(280);
        beneficiarySearchList.setCellFactory(lv -> new ListCell<>() {
            {
                setMaxWidth(Double.MAX_VALUE);
                setTextOverrun(OverrunStyle.ELLIPSIS);
                setWrapText(false);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText((item == null || empty) ? null : item);
            }
        });

        beneficiarySearchList.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                isSelectingFromList = true;
                int index = (int) (e.getY() / beneficiarySearchList.getFixedCellSize());
                if (index >= 0 && index < searchItems.size()) {
                    String item = searchItems.get(index);
                    if (item != null && !item.isBlank()) handleBeneficiarySelected(item);
                }
                e.consume();
            }
        });

        beneficiarySearchBox.textProperty().addListener((obs, oldVal, newVal) -> {
            if (suppressListener) return;
            String filter = (newVal == null) ? "" : newVal.trim().toLowerCase();
            if (filter.isEmpty()) { searchItems.clear(); hideInlineDropdown(); return; }
            List<String> filtered = beneficiaries.stream()
                    .filter(b -> b.name != null && b.name.toLowerCase().contains(filter))
                    .map(b -> b.name).collect(Collectors.toList());
            searchItems.setAll(filtered);
            if (!filtered.isEmpty()) showInlineDropdown(); else hideInlineDropdown();
        });

        beneficiarySearchBox.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case DOWN -> {
                    if (!beneficiarySearchList.isVisible()) showInlineDropdown();
                    beneficiarySearchList.requestFocus();
                    beneficiarySearchList.getSelectionModel().select(0);
                    e.consume();
                }
                case ESCAPE -> { hideInlineDropdown(); e.consume(); }
                case ENTER -> {
                    String selected = beneficiarySearchList.getSelectionModel().getSelectedItem();
                    if (selected != null) handleBeneficiarySelected(selected);
                    e.consume();
                }
            }
        });

        beneficiarySearchBox.focusedProperty().addListener((obs, was, isFocused) -> {
            if (!isFocused && !isSelectingFromList) Platform.runLater(this::hideInlineDropdown);
        });
    }

    private void showInlineDropdown() {
        int visibleRows = Math.min(8, searchItems.size());
        double height = visibleRows * beneficiarySearchList.getFixedCellSize() + 2;
        beneficiarySearchList.setPrefHeight(height);
        beneficiarySearchList.setMinHeight(height);
        beneficiarySearchList.setMaxHeight(height);
        beneficiarySearchList.setVisible(true);
        beneficiarySearchList.setManaged(true);
    }

    private void hideInlineDropdown() {
        beneficiarySearchList.setVisible(false);
        beneficiarySearchList.setManaged(false);
    }

    private void handleBeneficiarySelected(String selected) {
        isSelectingFromList = false;
        if (selected == null || selected.isBlank()) return;
        BeneficiaryMarker found = beneficiaries.stream()
                .filter(b -> selected.equals(b.name)).findFirst().orElse(null);
        if (found == null) return;
        selectBeneficiary(found, true);
        suppressListener = true;
        hideInlineDropdown();
        if (beneficiarySearchList != null) beneficiarySearchList.getSelectionModel().clearSelection();
        searchItems.clear();
        beneficiarySearchBox.clear();
        suppressListener = false;
        Platform.runLater(() -> beneficiarySearchBox.requestFocus());
    }

    private void searchToggle() {
        boolean nowVisible = !searchBoxWrap.isVisible();
        searchBoxWrap.setVisible(nowVisible);
        searchBoxWrap.setManaged(nowVisible);
        if (nowVisible) {
            suppressListener = true;
            beneficiarySearchBox.clear();
            suppressListener = false;
            Platform.runLater(() -> beneficiarySearchBox.requestFocus());
        } else {
            hideInlineDropdown();
            suppressListener = true;
            beneficiarySearchBox.clear();
            suppressListener = false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Card listeners
    // ═════════════════════════════════════════════════════════════════════════
    private void wireCardListeners() {
        cardBeneficiary.setCursor(javafx.scene.Cursor.HAND);
        cardBeneficiary.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) onBeneficiaryCardClicked();
        });
        cardDisasters.setCursor(javafx.scene.Cursor.HAND);
        cardDisasters.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) onDisastersCardClicked();
        });
        cardAids.setCursor(javafx.scene.Cursor.HAND);
        cardAids.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) onAidsCardClicked();
        });
        cardEvacuationSite.setCursor(javafx.scene.Cursor.HAND);
        cardEvacuationSite.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) onEvacuationSiteCardClicked();
        });
    }

    private void activeButton(Button btn) {
        MainFrameController frame = MainFrameController.getInstance();
        if (btn == null || frame == null) return;
        if (frame.activeBtn != null) {
            frame.activeBtn.getStyleClass().remove("nav-button-active");
            frame.activeBtn.getStyleClass().remove("nav-button-child-active");
        }
        frame.activeBtn = btn;
        String cls = btn.getStyleClass().contains("nav-button-child")
                ? "nav-button-child-active" : "nav-button-active";
        if (!frame.activeBtn.getStyleClass().contains(cls)) frame.activeBtn.getStyleClass().add(cls);
    }

    private void onBeneficiaryCardClicked() {
        MainFrameController.getInstance().openManagementSection();
        loadPage("/view/beneficiary/ManageBeneficiaries.fxml");
        activeButton(MainFrameController.getInstance().manageBeneficiariesBtn);
    }
    private void onDisastersCardClicked() {
        MainFrameController.getInstance().openDisasterSection();
        loadPage("/view/disaster/Disaster.fxml");
        activeButton(MainFrameController.getInstance().disasterBtn);
    }
    private void onAidsCardClicked() {
        MainFrameController.getInstance().openAidsSection();
        loadPage("/view/aid/Aid.fxml");
        activeButton(MainFrameController.getInstance().aidBtn);
    }
    private void onEvacuationSiteCardClicked() {
        MainFrameController.getInstance().openEvacSection();
        loadPage("/view/evac_site/EvacSite.fxml");
        activeButton(MainFrameController.getInstance().evacBtn);
    }

    private void loadPage(String fxml) {
        MainFrameController frame = MainFrameController.getInstance();
        if (frame == null) return;
        SceneManager.SceneEntry<?> e = SceneManager.load(fxml);
        Parent root = e.getRoot();
        if (root instanceof Region r) {
            r.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            VBox.setVgrow(r, Priority.ALWAYS);
            HBox.setHgrow(r, Priority.ALWAYS);
        }
        frame.contentArea.getChildren().setAll(root);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Info panel
    // ═════════════════════════════════════════════════════════════════════════
    private void buildInfoPanel() {
        // existing beneficiary panel
        infoPanel = new VBox(0);
        infoPanel.setStyle(
                "-fx-background-color:#2d3b4f;" +
                        "-fx-border-color:rgba(249,115,22,0.50);" +
                        "-fx-border-width:1.5px;" +
                        "-fx-border-radius:8px;" +
                        "-fx-background-radius:8px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.65),14,0.35,0,4);"
        );
        infoPanel.setVisible(false);
        infoPanel.setMouseTransparent(false);
        infoPanel.setViewOrder(-1.0);
        mapContainer.getChildren().add(infoPanel);

        // evac site panel
        evacInfoPanel = new VBox(0);
        evacInfoPanel.setStyle(
                "-fx-background-color:#2d3b4f;" +
                        "-fx-border-color:rgba(234,179,8,0.55);" +
                        "-fx-border-width:1.5px;" +
                        "-fx-border-radius:8px;" +
                        "-fx-background-radius:8px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.65),14,0.35,0,4);"
        );
        evacInfoPanel.setVisible(false);
        evacInfoPanel.setMouseTransparent(false);
        evacInfoPanel.setViewOrder(-1.0);
        mapContainer.getChildren().add(evacInfoPanel);
    }

    private void populateInfoPanel(BeneficiaryMarker b) {
        infoPanel.getChildren().clear();
        List<FamilyMemberModel> members = dashBoardService.getFamilyMembers(b.id);
        int memberCount = members != null ? members.size() : 0;

        double panelWidth = Math.max(180, Math.min(360, longestNameWidth(b.name, members, 56)));
        infoPanel.setPrefWidth(panelWidth);
        infoPanel.setMaxWidth(panelWidth);
        infoPanel.setMaxHeight(Region.USE_PREF_SIZE);

        Label headerLbl = new Label("BENEFICIARY");
        headerLbl.setStyle("-fx-text-fill:rgba(249,115,22,0.90);-fx-font-size:9px;-fx-font-weight:700;-fx-font-family:'Inter','Segoe UI',sans-serif;");
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        Button closeBtn = new Button("×");
        applyCloseBtnStyle(closeBtn);
        closeBtn.setOnAction(e -> dismissPanel());
        HBox headerRow = new HBox(6, headerLbl, hSpacer, closeBtn);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(8, 10, 6, 12));
        Region topDiv = new Region();
        topDiv.setPrefHeight(1);
        topDiv.setStyle("-fx-background-color:rgba(249,115,22,0.28);");
        Label beneIdLbl = new Label("ID #" + b.id);
        beneIdLbl.setStyle("-fx-text-fill:rgba(148,163,184,0.50);-fx-font-size:9px;-fx-font-family:'Inter','Segoe UI',sans-serif;");
        Label beneNameLbl = new Label(b.name != null ? b.name : "Unknown");
        beneNameLbl.setStyle("-fx-text-fill:#f1f5f9;-fx-font-size:13px;-fx-font-weight:800;-fx-font-family:'Inter','Segoe UI',sans-serif;");
        VBox beneBox = new VBox(2, beneIdLbl, beneNameLbl);
        beneBox.setPadding(new Insets(7, 12, 8, 12));
        beneBox.setStyle("-fx-background-color:rgba(249,115,22,0.07);");
        infoPanel.getChildren().addAll(headerRow, topDiv, beneBox);

        if (memberCount > 0) {
            Region midDiv = new Region();
            midDiv.setPrefHeight(1);
            midDiv.setStyle("-fx-background-color:rgba(255,255,255,0.06);");
            Label familyLbl = new Label("Family Members");
            familyLbl.setStyle("-fx-text-fill:rgba(148,163,184,0.65);-fx-font-size:9px;-fx-font-weight:700;-fx-font-family:'Inter','Segoe UI',sans-serif;");
            Label countLbl = new Label(memberCount + (memberCount != 1 ? " members" : " member"));
            countLbl.setStyle("-fx-text-fill:rgba(249,115,22,0.75);-fx-font-size:9px;-fx-background-color:rgba(249,115,22,0.10);-fx-background-radius:3px;-fx-border-color:rgba(249,115,22,0.25);-fx-border-radius:3px;-fx-border-width:1px;-fx-padding:1px 6px;");
            Region fs = new Region();
            HBox.setHgrow(fs, Priority.ALWAYS);
            HBox familyRow = new HBox(6, familyLbl, fs, countLbl);
            familyRow.setAlignment(Pos.CENTER_LEFT);
            familyRow.setPadding(new Insets(7, 12, 4, 12));
            VBox membersBox = new VBox(0);
            for (int i = 0; i < members.size(); i++) {
                FamilyMemberModel m = members.get(i);
                Label mName = new Label(m.getFullName());
                mName.setStyle("-fx-text-fill:#cbd5e1;-fx-font-size:12px;-fx-font-weight:600;-fx-font-family:'Inter','Segoe UI',sans-serif;");
                Label mId = new Label("ID #" + m.getFamilyMemberId());
                mId.setStyle("-fx-text-fill:rgba(148,163,184,0.40);-fx-font-size:9px;-fx-font-family:'Inter','Segoe UI',sans-serif;");
                VBox mRow = new VBox(1, mName, mId);
                mRow.setPadding(new Insets(6, 12, 6, 12));
                if (i % 2 == 0) mRow.setStyle("-fx-background-color:rgba(255,255,255,0.02);");
                membersBox.getChildren().add(mRow);
                if (i < members.size() - 1) {
                    Region sep = new Region();
                    sep.setPrefHeight(1);
                    sep.setStyle("-fx-background-color:rgba(255,255,255,0.04);");
                    membersBox.getChildren().add(sep);
                }
            }
            ScrollPane scroll = new ScrollPane(membersBox);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(false);
            scroll.setMaxHeight(200);
            scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;-fx-border-color:transparent;-fx-padding:0;");
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, javafx.event.Event::consume);
            infoPanel.getChildren().addAll(midDiv, familyRow, scroll);
        }

        Region bottomPad = new Region();
        bottomPad.setPrefHeight(8);
        infoPanel.getChildren().add(bottomPad);
    }

    private void selectBeneficiary(BeneficiaryMarker b, boolean centerMap) {
        selectedBeneficiary = b;
        populateInfoPanel(b);
        infoPanel.setVisible(true);
        infoPanel.setOpacity(0);
        if (centerMap) {
            currentCenterLat = b.lat;
            currentCenterLon = b.lon;
            currentZoom = 19.0;
            mapping.setCenter(currentCenterLat, currentCenterLon, currentZoom);
            mapping.redraw();
        }
        Platform.runLater(() -> Platform.runLater(() -> {
            clampAndPositionPanel();
            showWithFade(infoPanel);
            if (centerMap) {
                Timeline settle = new Timeline(
                        new KeyFrame(Duration.millis(150), ev -> clampAndPositionPanel()),
                        new KeyFrame(Duration.millis(400), ev -> clampAndPositionPanel()),
                        new KeyFrame(Duration.millis(800), ev -> clampAndPositionPanel())
                );
                settle.setCycleCount(1);
                settle.play();
            } else {
                mapping.redraw();
            }
        }));
    }

    private void repositionPanel() {
        if (selectedBeneficiary == null || infoPanel == null || !infoPanel.isVisible()) return;
        Platform.runLater(this::clampAndPositionPanel);
    }

    private void clampAndPositionPanel() {
        if (selectedBeneficiary == null || infoPanel == null || !mapping.isInitialized()) return;
        try {
            Mapping.Point p = mapping.latLonToScreen(selectedBeneficiary.lat, selectedBeneficiary.lon);
            double mapW = mapContainer.getWidth();
            double mapH = mapContainer.getHeight();
            if (mapW <= 0 || mapH <= 0) return;
            double pw = infoPanel.prefWidth(-1);
            if (pw <= 0) pw = 220;
            double maxAllowedH = mapH - 16;
            infoPanel.setMaxHeight(maxAllowedH);
            double ph = infoPanel.prefHeight(pw);
            if (ph <= 0) ph = 100;
            if (ph > maxAllowedH) ph = maxAllowedH;
            double tx = p.x - (pw / 2.0);
            tx = Math.max(4, Math.min(tx, mapW - pw - 4));
            double ty = p.y - MARKER_OFFSET_Y - ph;
            if (ty < 4) ty = p.y;
            if (ty + ph > mapH - 4) ty = mapH - ph - 4;
            ty = Math.max(4, ty);
            infoPanel.setLayoutX(tx);
            infoPanel.setLayoutY(ty);
        } catch (Exception ignored) {}
    }

    private void dismissPanel() {
        selectedBeneficiary = null;
        if (infoPanel != null) infoPanel.setVisible(false);
        mapping.redraw();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Marker hit detection
    // ═════════════════════════════════════════════════════════════════════════
    private BeneficiaryMarker findMarkerAtScreen(double sx, double sy) {
        if (!mapping.isInitialized()) return null;
        boolean useImage = mapping.getZoom() >= MIN_ZOOM_FOR_MARKERS;
        for (BeneficiaryMarker b : beneficiaries) {
            if (!Mapping.isValidCoordinate(b.lat, b.lon)) continue;
            if (!isPointInPolygon(b.lon, b.lat, boundary)) continue;
            try {
                Mapping.Point p = mapping.latLonToScreen(b.lat, b.lon);
                double hx, hy, hw, hh;
                if (useImage && personMarker != null) {
                    hx = p.x - MARKER_WIDTH / 2; hy = p.y - MARKER_OFFSET_Y;
                    hw = MARKER_WIDTH; hh = MARKER_HEIGHT;
                } else {
                    double r = 8; hx = p.x - r; hy = p.y - r; hw = r * 2; hh = r * 2;
                }
                if (sx >= hx && sx <= hx + hw && sy >= hy && sy <= hy + hh) return b;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private EvacSiteMarker findEvacSiteAtScreen(double sx, double sy) {
        if (!mapping.isInitialized()) return null;
        boolean useImage = mapping.getZoom() >= MIN_ZOOM_FOR_MARKERS;
        for (EvacSiteMarker site : evacSites) {
            if (!Mapping.isValidCoordinate(site.lat, site.lon)) continue;
            if (!isPointInPolygon(site.lon, site.lat, boundary)) continue;
            try {
                Mapping.Point p = mapping.latLonToScreen(site.lat, site.lon);
                double hx, hy, hw, hh;
                if (useImage && evacSiteMarker != null) {
                    hx = p.x - EVAC_MARKER_WIDTH / 2; hy = p.y - EVAC_MARKER_OFFSET_Y;
                    hw = EVAC_MARKER_WIDTH; hh = EVAC_MARKER_HEIGHT;
                } else {
                    double r = 8; hx = p.x - r; hy = p.y - r; hw = r * 2; hh = r * 2;
                }
                if (sx >= hx && sx <= hx + hw && sy >= hy && sy <= hy + hh) return site;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void populateEvacInfoPanel(EvacSiteMarker site) {
        evacInfoPanel.getChildren().clear();

        double panelWidth = 220;
        evacInfoPanel.setPrefWidth(panelWidth);
        evacInfoPanel.setMaxWidth(panelWidth);
        evacInfoPanel.setMaxHeight(Region.USE_PREF_SIZE);

        // Header row
        Label headerLbl = new Label("EVACUATION SITE");
        headerLbl.setStyle("-fx-text-fill:rgba(234,179,8,0.90);-fx-font-size:9px;-fx-font-weight:700;-fx-font-family:'Inter','Segoe UI',sans-serif;");
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        Button closeBtn = new Button("×");
        applyEvacCloseBtnStyle(closeBtn);
        closeBtn.setOnAction(e -> dismissEvacPanel());
        HBox headerRow = new HBox(6, headerLbl, hSpacer, closeBtn);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(8, 10, 6, 12));

        Region topDiv = new Region();
        topDiv.setPrefHeight(1);
        topDiv.setStyle("-fx-background-color:rgba(234,179,8,0.28);");

        // Site Name
        Label nameTitleLbl = new Label("NAME");
        nameTitleLbl.setStyle("-fx-text-fill:rgba(148,163,184,0.50);-fx-font-size:9px;-fx-font-family:'Inter','Segoe UI',sans-serif;");
        Label nameLbl = new Label(site.getName() != null ? site.getName() : "Unknown");
        nameLbl.setStyle("-fx-text-fill:#f1f5f9;-fx-font-size:13px;-fx-font-weight:800;-fx-font-family:'Inter','Segoe UI',sans-serif;");
        nameLbl.setWrapText(true);

        Region midDiv = new Region();
        midDiv.setPrefHeight(1);
        midDiv.setStyle("-fx-background-color:rgba(255,255,255,0.06);");

        // Capacity
        Label capTitleLbl = new Label("CAPACITY");
        capTitleLbl.setStyle("-fx-text-fill:rgba(148,163,184,0.50);-fx-font-size:9px;-fx-font-family:'Inter','Segoe UI',sans-serif;");
        String capText = (site.getCapacity() > 0) ? String.valueOf(site.getCapacity()) + " persons" : "N/A";
        Label capLbl = new Label(capText);
        capLbl.setStyle("-fx-text-fill:rgba(234,179,8,0.95);-fx-font-size:13px;-fx-font-weight:800;-fx-font-family:'Inter','Segoe UI',sans-serif;");

        VBox contentBox = new VBox(0);
        contentBox.setPadding(new Insets(7, 12, 10, 12));
        contentBox.setSpacing(0);
        contentBox.setStyle("-fx-background-color:rgba(234,179,8,0.06);");

        VBox nameSection = new VBox(2, nameTitleLbl, nameLbl);
        nameSection.setPadding(new Insets(0, 0, 8, 0));
        VBox capSection = new VBox(2, capTitleLbl, capLbl);

        contentBox.getChildren().addAll(nameSection, midDiv, capSection);

        evacInfoPanel.getChildren().addAll(headerRow, topDiv, contentBox);
    }

    private void selectEvacSite(EvacSiteMarker site) {
        selectedBeneficiary = null;
        if (infoPanel != null) infoPanel.setVisible(false);

        selectedEvacSite = site;
        populateEvacInfoPanel(site);
        evacInfoPanel.setVisible(true);
        evacInfoPanel.setOpacity(0);

        Platform.runLater(() -> Platform.runLater(() -> {
            clampAndPositionEvacPanel();
            showWithFade(evacInfoPanel);
            mapping.redraw();
        }));
    }

    private void dismissEvacPanel() {
        selectedEvacSite = null;
        if (evacInfoPanel != null) evacInfoPanel.setVisible(false);
        mapping.redraw();
    }

    private void clampAndPositionEvacPanel() {
        if (selectedEvacSite == null || evacInfoPanel == null || !mapping.isInitialized()) return;
        try {
            Mapping.Point p = mapping.latLonToScreen(selectedEvacSite.lat, selectedEvacSite.lon);
            double mapW = mapContainer.getWidth();
            double mapH = mapContainer.getHeight();
            if (mapW <= 0 || mapH <= 0) return;
            double pw = evacInfoPanel.prefWidth(-1);
            if (pw <= 0) pw = 220;
            double maxAllowedH = mapH - 16;
            evacInfoPanel.setMaxHeight(maxAllowedH);
            double ph = evacInfoPanel.prefHeight(pw);
            if (ph <= 0) ph = 80;
            if (ph > maxAllowedH) ph = maxAllowedH;
            double tx = p.x - (pw / 2.0);
            tx = Math.max(4, Math.min(tx, mapW - pw - 4));
            double ty = p.y - EVAC_MARKER_OFFSET_Y - ph;
            if (ty < 4) ty = p.y;
            if (ty + ph > mapH - 4) ty = mapH - ph - 4;
            ty = Math.max(4, ty);
            evacInfoPanel.setLayoutX(tx);
            evacInfoPanel.setLayoutY(ty);
        } catch (Exception ignored) {}
    }

    private void repositionEvacPanel() {
        if (selectedEvacSite == null || evacInfoPanel == null || !evacInfoPanel.isVisible()) return;
        Platform.runLater(this::clampAndPositionEvacPanel);
    }

    private void applyEvacCloseBtnStyle(Button btn) {
        String n = "-fx-background-color:transparent;-fx-text-fill:rgba(148,163,184,0.65);-fx-font-size:14px;-fx-cursor:hand;-fx-padding:0;";
        String h = "-fx-background-color:transparent;-fx-text-fill:#ef4444;-fx-font-size:14px;-fx-cursor:hand;-fx-padding:0;";
        btn.setStyle(n);
        btn.setOnMouseEntered(e -> btn.setStyle(h));
        btn.setOnMouseExited(e -> btn.setStyle(n));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Center on boundary
    // ═════════════════════════════════════════════════════════════════════════
    private void centerMapOnBoundary() {
        if (boundary == null || boundary.length == 0) return;
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        for (double[] pt : boundary) {
            if (pt[0] < minLat) minLat = pt[0];
            if (pt[0] > maxLat) maxLat = pt[0];
            if (pt[1] < minLon) minLon = pt[1];
            if (pt[1] > maxLon) maxLon = pt[1];
        }
        currentCenterLat = (minLat + maxLat) / 2.0;
        currentCenterLon = (minLon + maxLon) / 2.0;
        currentZoom = 13.0;
        mapping.setCenter(currentCenterLat, currentCenterLon, currentZoom);
        Platform.runLater(mapping::redraw);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Data loaders
    // ═════════════════════════════════════════════════════════════════════════
    public void loadBeneficiariesFromDb() {
        beneficiaries.clear();
        beneficiaries.addAll(dashBoardService.getBeneficiaries());
        if (beneficiarySearchList != null && beneficiarySearchList.isVisible()) {
            List<String> names = beneficiaries.stream()
                    .filter(b -> b.name != null && !b.name.isBlank())
                    .map(b -> b.name).collect(Collectors.toList());
            searchItems.setAll(names);
        }
        mapping.redraw();
    }

    public void loadEvacSitesFromDb() {
        evacSites.clear();
        evacSites.addAll(dashBoardService.getEvacSites());
        mapping.redraw();
    }

    public void loadDashBoardData() {
        totalBeneficiaryLabel.setText(String.valueOf(dashBoardService.fetchTotalBeneficiary()));
        totalDisastersLabel.setText(String.valueOf(dashBoardService.fetchTotalDisasters()));
        totalAidsLabel.setText(String.valueOf(dashBoardService.fetchTotalAids()));
        totalEvacutaionSiteLabel.setText(String.valueOf(dashBoardService.fetchTotalEvacuationSites()));
        loadEvacSitesFromDb();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Drawing
    // ═════════════════════════════════════════════════════════════════════════
    private void drawBeneficiaries() {
        if (!mapping.isInitialized() || beneficiaries.isEmpty()) return;
        GraphicsContext gc = mapping.getGc();
        double canvasW = mapping.getCanvas().getWidth();
        double canvasH = mapping.getCanvas().getHeight();
        double pad = 50;
        boolean useImage = mapping.getZoom() >= MIN_ZOOM_FOR_MARKERS;
        double dotRadius = 4;
        for (BeneficiaryMarker b : beneficiaries) {
            if (!Mapping.isValidCoordinate(b.lat, b.lon)) continue;
            if (!isPointInPolygon(b.lon, b.lat, boundary)) continue;
            try {
                Mapping.Point p = mapping.latLonToScreen(b.lat, b.lon);
                if (p.x < -pad || p.x > canvasW + pad) continue;
                if (p.y < -pad || p.y > canvasH + pad) continue;
                boolean sel = selectedBeneficiary != null && selectedBeneficiary.id == b.id;
                if (useImage && personMarker != null) {
                    if (sel) {
                        gc.setStroke(Color.rgb(249, 115, 22, 0.85));
                        gc.setLineWidth(2.5);
                        gc.strokeOval(p.x - MARKER_WIDTH / 2 - 4, p.y - MARKER_OFFSET_Y - 4,
                                MARKER_WIDTH + 8, MARKER_HEIGHT + 8);
                    }
                    gc.drawImage(personMarker, p.x - MARKER_WIDTH / 2, p.y - MARKER_OFFSET_Y,
                            MARKER_WIDTH, MARKER_HEIGHT);
                } else {
                    if (sel) {
                        gc.setStroke(Color.rgb(249, 115, 22, 0.30));
                        gc.setLineWidth(8);
                        gc.strokeOval(p.x - dotRadius - 5, p.y - dotRadius - 5,
                                (dotRadius + 5) * 2, (dotRadius + 5) * 2);
                    }
                    Color fill   = sel ? Color.rgb(249, 115, 22, 0.95) : Color.rgb(0, 120, 255, 0.85);
                    Color stroke = sel ? Color.rgb(249, 115, 22, 1.00) : Color.rgb(0, 70, 180, 0.95);
                    gc.setFill(fill);
                    gc.fillOval(p.x - dotRadius, p.y - dotRadius, dotRadius * 2, dotRadius * 2);
                    gc.setStroke(stroke);
                    gc.setLineWidth(1.2);
                    gc.strokeOval(p.x - dotRadius, p.y - dotRadius, dotRadius * 2, dotRadius * 2);
                }
            } catch (Exception ignored) {}
        }
    }

    private void drawBoundary() {
        if (!mapping.isInitialized()) return;
        GraphicsContext gc = mapping.getGc();
        gc.setStroke(Color.rgb(120, 0, 0, 0.35));
        gc.setLineWidth(6);
        gc.beginPath();
        boolean first = true;
        for (double[] c : boundary) {
            Mapping.Point p = mapping.latLonToScreen(c[0], c[1]);
            if (first) { gc.moveTo(p.x, p.y); first = false; } else gc.lineTo(p.x, p.y);
        }
        gc.closePath();
        gc.stroke();
        gc.setStroke(Color.rgb(255, 50, 50, 0.9));
        gc.setLineWidth(2.5);
        gc.beginPath();
        first = true;
        for (double[] c : boundary) {
            Mapping.Point p = mapping.latLonToScreen(c[0], c[1]);
            if (first) { gc.moveTo(p.x, p.y); first = false; } else gc.lineTo(p.x, p.y);
        }
        gc.closePath();
        gc.stroke();
    }

    private void drawEvacSites() {
        if (!mapping.isInitialized() || evacSites.isEmpty()) return;
        GraphicsContext gc = mapping.getGc();
        double canvasW = mapping.getCanvas().getWidth();
        double canvasH = mapping.getCanvas().getHeight();
        double pad = 60;
        boolean useImage = mapping.getZoom() >= MIN_ZOOM_FOR_MARKERS;
        double dotRadius = 4;
        Color dotColor = Color.rgb(234, 179, 8, 0.85);
        for (EvacSiteMarker site : evacSites) {
            if (!Mapping.isValidCoordinate(site.lat, site.lon)) continue;
            if (!isPointInPolygon(site.lon, site.lat, boundary)) continue;
            try {
                Mapping.Point p = mapping.latLonToScreen(site.lat, site.lon);
                if (p.x < -pad || p.x > canvasW + pad) continue;
                if (p.y < -pad || p.y > canvasH + pad) continue;
                if (useImage && evacSiteMarker != null) {
                    gc.drawImage(evacSiteMarker, p.x - EVAC_MARKER_WIDTH / 2, p.y - EVAC_MARKER_OFFSET_Y,
                            EVAC_MARKER_WIDTH, EVAC_MARKER_HEIGHT);
                } else {
                    gc.setFill(dotColor);
                    gc.fillOval(p.x - dotRadius, p.y - dotRadius, dotRadius * 2, dotRadius * 2);
                    gc.setStroke(dotColor);
                    gc.setLineWidth(1.2);
                    gc.strokeOval(p.x - dotRadius, p.y - dotRadius, dotRadius * 2, dotRadius * 2);
                }
            } catch (Exception ignored) {}
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═════════════════════════════════════════════════════════════════════════
    private double longestNameWidth(String beneName, List<FamilyMemberModel> members, double hPad) {
        Text probe = new Text();
        probe.setStyle("-fx-font-size:13px;-fx-font-weight:800;");
        probe.setText(beneName != null ? beneName : "Unknown");
        double max = probe.getLayoutBounds().getWidth() + hPad + 24;
        if (members != null) {
            probe.setStyle("-fx-font-size:12px;-fx-font-weight:600;");
            for (FamilyMemberModel m : members) {
                String name = m.getFullName();
                probe.setText(name != null ? name : "");
                double w = probe.getLayoutBounds().getWidth() + hPad + 24;
                if (w > max) max = w;
            }
        }
        return max;
    }

    private void showWithFade(javafx.scene.Node node) {
        node.setOpacity(0);
        node.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void applyCloseBtnStyle(Button btn) {
        String n = "-fx-background-color:transparent;-fx-text-fill:rgba(148,163,184,0.65);-fx-font-size:14px;-fx-cursor:hand;-fx-padding:0;";
        String h = "-fx-background-color:transparent;-fx-text-fill:#ef4444;-fx-font-size:14px;-fx-cursor:hand;-fx-padding:0;";
        btn.setStyle(n);
        btn.setOnMouseEntered(e -> btn.setStyle(h));
        btn.setOnMouseExited(e -> btn.setStyle(n));
    }

    private boolean isPointInPolygon(double x, double y, double[][] polygon) {
        boolean inside = false;
        for (int i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
            double xi = polygon[i][1], yi = polygon[i][0];
            double xj = polygon[j][1], yj = polygon[j][0];
            boolean intersect = ((yi > y) != (yj > y))
                    && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }
}