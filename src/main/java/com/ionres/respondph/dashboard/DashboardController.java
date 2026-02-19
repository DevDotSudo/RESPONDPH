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
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardController {

    // ── Services ──────────────────────────────────────────────────────────────
    private final DashBoardService dashBoardService = AppContext.dashBoardService;
    private final Mapping          mapping          = new Mapping();

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<BeneficiaryMarker> beneficiaries = new ArrayList<>();
    private final List<EvacSiteMarker>    evacSites     = new ArrayList<>();

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private Pane             mapContainer;
    @FXML private Label            totalBeneficiaryLabel;
    @FXML private Label            totalDisastersLabel;
    @FXML private Label            totalAidsLabel;
    @FXML private Label            currentDateLabel;
    @FXML private Label            currentTimeLabel;
    @FXML private Label            totalEvacutaionSiteLabel;
    @FXML private Label            adminNameLabel;
    @FXML private Button           searchToggleBtn;
    @FXML private ComboBox<String> beneficiarySearchBox;
    @FXML private HBox             searchOverlay;
    @FXML private StackPane cardBeneficiary;
    @FXML private StackPane cardDisasters;
    @FXML private StackPane cardAids;
    @FXML private StackPane cardEvacuationSite;

    // ── Marker images ─────────────────────────────────────────────────────────
    private Image personMarker;
    private Image evacSiteMarker;

    private static final double MIN_ZOOM_FOR_MARKERS = 16.0;
    private static final double MARKER_WIDTH         = 32;
    private static final double MARKER_HEIGHT        = 32;
    private static final double MARKER_OFFSET_Y      = MARKER_HEIGHT;
    private static final double EVAC_MARKER_WIDTH    = 32;
    private static final double EVAC_MARKER_HEIGHT   = 32;
    private static final double EVAC_MARKER_OFFSET_Y = EVAC_MARKER_HEIGHT;

    // ── Drag / zoom ───────────────────────────────────────────────────────────
    private double dragStartX, dragStartY;
    private double currentCenterLat;
    private double currentCenterLon;
    private double currentZoom = 13.0;

    // ── Selection ─────────────────────────────────────────────────────────────
    private BeneficiaryMarker selectedBeneficiary = null;
    private VBox               infoPanel;

    // ── Combobox safe list ────────────────────────────────────────────────────
    private final ObservableList<String> searchItems      = FXCollections.observableArrayList();
    private       boolean                suppressListener = false;

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

        Platform.runLater(() -> {
            if (searchOverlay != null) {
                searchOverlay.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                searchOverlay.setPickOnBounds(false);
            }
        });

        Platform.runLater(() -> {

            // ── Load marker images ────────────────────────────────────────
            try {
                personMarker = new Image(getClass().getResourceAsStream("/images/person_marker.png"));
                if (personMarker.isError()) personMarker = null;
                evacSiteMarker = new Image(getClass().getResourceAsStream("/images/location-pin.png"));
                if (evacSiteMarker.isError()) evacSiteMarker = null;
            } catch (Exception e) {
                personMarker = null;
            }

            // ── Init map ──────────────────────────────────────────────────
            mapping.init(mapContainer);
            mapping.setAfterRedraw(() -> {
                drawBoundary();
                drawEvacSites();
                drawBeneficiaries();
                repositionPanel();
            });

            // FIX 1: panel lives inside mapContainer (a Pane), not StackPane.
            // Only Pane respects layoutX/layoutY for absolute positioning.
            buildInfoPanel();
            wireSearchComboBox();

            // ── SINGLE click — drag / dismiss ─────────────────────────────
            mapContainer.setOnMousePressed(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    BeneficiaryMarker hit = findMarkerAtScreen(e.getX(), e.getY());
                    if (hit == null && selectedBeneficiary != null) {
                        dismissPanel();
                    }
                    dragStartX = e.getX();
                    dragStartY = e.getY();
                }
            });

            mapContainer.setOnMouseDragged(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    double dx = e.getX() - dragStartX;
                    double dy = e.getY() - dragStartY;
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

            // ── DOUBLE click — show info panel ────────────────────────────
            mapContainer.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                    BeneficiaryMarker hit = findMarkerAtScreen(e.getX(), e.getY());
                    if (hit != null) {
                        selectBeneficiary(hit, false);
                    }
                }
            });

            // ── Scroll to zoom ────────────────────────────────────────────
            mapContainer.setOnScroll(e -> {
                double delta = e.getDeltaY() > 0 ? 0.5 : -0.5;
                currentZoom = Math.max(10.0, Math.min(19.0, currentZoom + delta));
                mapping.setCenter(currentCenterLat, currentCenterLon, currentZoom);
            });

            DashboardRefresher.register(this);
            loadDashBoardData();
            loadBeneficiariesFromDb();
            loadEvacSitesFromDb();
            wireCardListeners();

            Timeline centerDelay = new Timeline(
                    new KeyFrame(Duration.millis(100), e -> centerMapOnBoundary()));
            centerDelay.setCycleCount(1);
            centerDelay.play();

            // ── Clock ─────────────────────────────────────────────────────
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

            // ── Admin name ────────────────────────────────────────────────
            SessionManager.getInstance().setOnSessionChanged(() -> {
                AdminModel admin = SessionManager.getInstance().getCurrentAdmin();
                if (admin != null) {
                    String display = (admin.getFirstname() != null && !admin.getFirstname().isEmpty())
                            ? admin.getFirstname() + " " + admin.getLastname()
                            : admin.getUsername();
                    adminNameLabel.setText("Admin : " + display);
                }
            });
        });

        searchToggleBtn.setOnAction(e -> searchToggle());
    }

    private void wireCardListeners() {
        cardBeneficiary.setCursor(javafx.scene.Cursor.HAND);
        cardBeneficiary.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                onBeneficiaryCardClicked();
            }
        });

        cardDisasters.setCursor(javafx.scene.Cursor.HAND);
        cardDisasters.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                onDisastersCardClicked();
            }
        });

        cardAids.setCursor(javafx.scene.Cursor.HAND);
        cardAids.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                onAidsCardClicked();
            }
        });

        cardEvacuationSite.setCursor(javafx.scene.Cursor.HAND);
        cardEvacuationSite.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                onEvacuationSiteCardClicked();
            }
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
        if (!frame.activeBtn.getStyleClass().contains(cls))
            frame.activeBtn.getStyleClass().add(cls);
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
        frame.contentArea.getChildren().setAll(root); // ← use frame.contentArea
    }

    private void wireSearchComboBox() {
        beneficiarySearchBox.setItems(searchItems);

        beneficiarySearchBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (suppressListener) return;
            if (newVal == null) return;

            Platform.runLater(() -> {
                if (suppressListener) return;
                String filter = newVal.trim().toLowerCase();

                List<String> filtered = beneficiaries.stream()
                        .filter(b -> b.name != null)
                        .filter(b -> filter.isEmpty() || b.name.toLowerCase().contains(filter))
                        .map(b -> b.name)
                        .collect(Collectors.toList());

                // Must hide BEFORE mutating the list
                beneficiarySearchBox.hide();
                searchItems.setAll(filtered);

                if (!filter.isEmpty()) {
                    beneficiarySearchBox.show();
                }
            });
        });

        beneficiarySearchBox.setOnAction(e -> {
            if (suppressListener) return;
            String selected = beneficiarySearchBox.getValue();
            if (selected == null || selected.isBlank()) return;

            BeneficiaryMarker found = beneficiaries.stream()
                    .filter(b -> selected.equals(b.name))
                    .findFirst()
                    .orElse(null);

            if (found != null) {
                selectBeneficiary(found, true);

                suppressListener = true;
                beneficiarySearchBox.hide();
                // Reset list to full before clearing so next open shows everything
                searchItems.setAll(
                        beneficiaries.stream()
                                .filter(b -> b.name != null && !b.name.isBlank())
                                .map(b -> b.name)
                                .collect(Collectors.toList())
                );
                beneficiarySearchBox.setValue(null);
                beneficiarySearchBox.getEditor().clear();
                suppressListener = false;
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Search button toggle
    // ═════════════════════════════════════════════════════════════════════════
    private void searchToggle() {
        boolean nowVisible = !beneficiarySearchBox.isVisible();
        beneficiarySearchBox.setVisible(nowVisible);
        beneficiarySearchBox.setManaged(nowVisible);

        if (nowVisible) {
            beneficiarySearchBox.hide(); // hide before mutation
            searchItems.setAll(
                    beneficiaries.stream()
                            .filter(b -> b.name != null && !b.name.isBlank())
                            .map(b -> b.name)
                            .collect(Collectors.toList())
            );
            Platform.runLater(() -> {
                beneficiarySearchBox.getEditor().clear();
                beneficiarySearchBox.getEditor().requestFocus();
            });
        } else {
            beneficiarySearchBox.hide();
            suppressListener = true;
            beneficiarySearchBox.setValue(null);
            beneficiarySearchBox.getEditor().clear();
            suppressListener = false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FIX 1: Build info panel inside mapContainer (a Pane).
    //
    // Root cause of invisible panel:
    //   The old code added infoPanel to the StackPane parent and called
    //   setManaged(false) + setLayoutX/Y. StackPane ignores layoutX/Y on its
    //   children — it always centres them. Moving the panel into mapContainer
    //   (which is a plain Pane) means absolute positioning via layoutX/Y works.
    //
    //   setViewOrder(-1) raises it above the Canvas that mapping draws on,
    //   so it renders on top and receives mouse events correctly.
    // ═════════════════════════════════════════════════════════════════════════
    private void buildInfoPanel() {
        infoPanel = new VBox(0);
        infoPanel.setStyle(
                "-fx-background-color: #2d3b4f;" +
                        "-fx-border-color: rgba(249,115,22,0.50);" +
                        "-fx-border-width: 1.5px;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.65),14,0.35,0,4);"
        );
        infoPanel.setVisible(false);
        infoPanel.setMouseTransparent(false);
        infoPanel.setViewOrder(-1.0); // render above the canvas layer

        mapContainer.getChildren().add(infoPanel);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Populate panel content
    // ═════════════════════════════════════════════════════════════════════════
    private void populateInfoPanel(BeneficiaryMarker b) {
        infoPanel.getChildren().clear();

        List<FamilyMemberModel> members = dashBoardService.getFamilyMembers(b.id);
        int memberCount = members != null ? members.size() : 0;

        double panelWidth = Math.max(180, Math.min(360,
                longestNameWidth(b.name, members, 8.5, 56)));
        infoPanel.setPrefWidth(panelWidth);
        infoPanel.setMaxWidth(panelWidth);
        infoPanel.setMaxHeight(Region.USE_PREF_SIZE);

        // ── Header ────────────────────────────────────────────────────────
        Label headerLbl = new Label("BENEFICIARY");
        headerLbl.setStyle(
                "-fx-text-fill: rgba(249,115,22,0.90);" +
                        "-fx-font-size: 9px;" +
                        "-fx-font-weight: 700;" +
                        "-fx-font-family: 'Inter','Segoe UI',sans-serif;"
        );
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
        topDiv.setStyle("-fx-background-color: rgba(249,115,22,0.28);");

        // ── Beneficiary block ─────────────────────────────────────────────
        Label beneIdLbl = new Label("ID #" + b.id);
        beneIdLbl.setStyle(
                "-fx-text-fill: rgba(148,163,184,0.50);" +
                        "-fx-font-size: 9px;" +
                        "-fx-font-family: 'Inter','Segoe UI',sans-serif;"
        );
        Label beneNameLbl = new Label(b.name != null ? b.name : "Unknown");
        beneNameLbl.setStyle(
                "-fx-text-fill: #f1f5f9;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-font-family: 'Inter','Segoe UI',sans-serif;"
        );
        VBox beneBox = new VBox(2, beneIdLbl, beneNameLbl);
        beneBox.setPadding(new Insets(7, 12, 8, 12));
        beneBox.setStyle("-fx-background-color: rgba(249,115,22,0.07);");

        infoPanel.getChildren().addAll(headerRow, topDiv, beneBox);

        // ── Family members ─────────────────────────────────────────────────
        if (memberCount > 0) {
            Region midDiv = new Region();
            midDiv.setPrefHeight(1);
            midDiv.setStyle("-fx-background-color: rgba(255,255,255,0.06);");

            Label familyLbl = new Label("Family Members");
            familyLbl.setStyle(
                    "-fx-text-fill: rgba(148,163,184,0.65);" +
                            "-fx-font-size: 9px;" +
                            "-fx-font-weight: 700;" +
                            "-fx-font-family: 'Inter','Segoe UI',sans-serif;"
            );
            Label countLbl = new Label(memberCount + (memberCount != 1 ? " members" : " member"));
            countLbl.setStyle(
                    "-fx-text-fill: rgba(249,115,22,0.75);" +
                            "-fx-font-size: 9px;" +
                            "-fx-background-color: rgba(249,115,22,0.10);" +
                            "-fx-background-radius: 3px;" +
                            "-fx-border-color: rgba(249,115,22,0.25);" +
                            "-fx-border-radius: 3px;" +
                            "-fx-border-width: 1px;" +
                            "-fx-padding: 1px 6px;"
            );
            Region fs = new Region();
            HBox.setHgrow(fs, Priority.ALWAYS);
            HBox familyRow = new HBox(6, familyLbl, fs, countLbl);
            familyRow.setAlignment(Pos.CENTER_LEFT);
            familyRow.setPadding(new Insets(7, 12, 4, 12));

            VBox membersBox = new VBox(0);
            for (int i = 0; i < members.size(); i++) {
                FamilyMemberModel m = members.get(i);

                Label mName = new Label(m.getFullName());
                mName.setStyle(
                        "-fx-text-fill: #cbd5e1;" +
                                "-fx-font-size: 12px;" +
                                "-fx-font-weight: 600;" +
                                "-fx-font-family: 'Inter','Segoe UI',sans-serif;"
                );
                Label mId = new Label("ID #" + m.getFamilyMemberId());
                mId.setStyle(
                        "-fx-text-fill: rgba(148,163,184,0.40);" +
                                "-fx-font-size: 9px;" +
                                "-fx-font-family: 'Inter','Segoe UI',sans-serif;"
                );
                VBox mRow = new VBox(1, mName, mId);
                mRow.setPadding(new Insets(6, 12, 6, 12));
                if (i % 2 == 0) mRow.setStyle("-fx-background-color: rgba(255,255,255,0.02);");

                membersBox.getChildren().add(mRow);
                if (i < members.size() - 1) {
                    Region sep = new Region();
                    sep.setPrefHeight(1);
                    sep.setStyle("-fx-background-color: rgba(255,255,255,0.04);");
                    membersBox.getChildren().add(sep);
                }
            }

            ScrollPane scroll = new ScrollPane(membersBox);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(false);
            scroll.setMaxHeight(200);
            scroll.setStyle(
                    "-fx-background-color:transparent;" +
                            "-fx-background:transparent;" +
                            "-fx-border-color:transparent;" +
                            "-fx-padding:0;"
            );
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            // Consume scroll events at filter phase so they never reach the map zoom handler
            scroll.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, javafx.event.Event::consume);

            infoPanel.getChildren().addAll(midDiv, familyRow, scroll);
        }

        Region bottomPad = new Region();
        bottomPad.setPrefHeight(8);
        infoPanel.getChildren().add(bottomPad);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Select beneficiary
    // ═════════════════════════════════════════════════════════════════════════
    private void selectBeneficiary(BeneficiaryMarker b, boolean centerMap) {
        selectedBeneficiary = b;

        if (centerMap) {
            currentCenterLat = b.lat;
            currentCenterLon = b.lon;
            currentZoom      = 19.0;
            mapping.setCenter(currentCenterLat, currentCenterLon, currentZoom);
        }

        populateInfoPanel(b);

        // Two pulses: first allows JavaFX to measure the panel's preferred size,
        // second uses those measurements for accurate clamped positioning.
        Platform.runLater(() -> Platform.runLater(() -> {
            clampAndPositionPanel();
            showWithFade(infoPanel);
            mapping.redraw();
        }));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Reposition on every map redraw
    // ═════════════════════════════════════════════════════════════════════════
    private void repositionPanel() {
        if (!infoPanel.isVisible()) return;
        clampAndPositionPanel();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Core positioning
    // ═════════════════════════════════════════════════════════════════════════
    private void clampAndPositionPanel() {
        if (selectedBeneficiary == null || infoPanel == null || !mapping.isInitialized()) return;
        try {
            Mapping.Point p = mapping.latLonToScreen(selectedBeneficiary.lat, selectedBeneficiary.lon);

            double mapW = mapContainer.getWidth();
            double mapH = mapContainer.getHeight();

            double pw = infoPanel.prefWidth(-1);
            if (pw <= 0) pw = 200;

            double maxAllowedH = mapH - 16;
            infoPanel.setMaxHeight(maxAllowedH);

            double ph = infoPanel.prefHeight(pw);
            if (ph > maxAllowedH) ph = maxAllowedH;
            if (ph <= 0) ph = 80;

            double tx = p.x - (pw / 2.0);
            tx = Math.max(4, Math.min(tx, mapW - pw - 4));

            double ty = p.y - MARKER_OFFSET_Y - ph - 10;
            if (ty < 4)            ty = p.y + 6;
            if (ty + ph > mapH - 4) ty = mapH - ph - 4;
            ty = Math.max(4, ty);

            infoPanel.setLayoutX(tx);
            infoPanel.setLayoutY(ty);

        } catch (Exception ignored) {}
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Dismiss
    // ═════════════════════════════════════════════════════════════════════════
    private void dismissPanel() {
        selectedBeneficiary = null;
        infoPanel.setVisible(false);
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
                    hx = p.x - MARKER_WIDTH / 2;
                    hy = p.y - MARKER_OFFSET_Y;
                    hw = MARKER_WIDTH;
                    hh = MARKER_HEIGHT;
                } else {
                    double r = 8;
                    hx = p.x - r; hy = p.y - r;
                    hw = r * 2;   hh = r * 2;
                }
                if (sx >= hx && sx <= hx + hw && sy >= hy && sy <= hy + hh) return b;
            } catch (Exception ignored) {}
        }
        return null;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Center on boundary
    // ═════════════════════════════════════════════════════════════════════════
    private void centerMapOnBoundary() {
        if (boundary == null || boundary.length == 0) return;

        double minLat = Double.MAX_VALUE,  maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE,  maxLon = -Double.MAX_VALUE;
        for (double[] pt : boundary) {
            if (pt[0] < minLat) minLat = pt[0];
            if (pt[0] > maxLat) maxLat = pt[0];
            if (pt[1] < minLon) minLon = pt[1];
            if (pt[1] > maxLon) maxLon = pt[1];
        }

        currentCenterLat = (minLat + maxLat) / 2.0;
        currentCenterLon = (minLon + maxLon) / 2.0;
        currentZoom      = 13.0;

        mapping.setCenter(currentCenterLat, currentCenterLon, currentZoom);
        Platform.runLater(() -> mapping.redraw());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Data loaders
    // ═════════════════════════════════════════════════════════════════════════
    public void loadBeneficiariesFromDb() {
        beneficiaries.clear();
        beneficiaries.addAll(dashBoardService.getBeneficiaries());
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

        GraphicsContext gc  = mapping.getGc();
        double canvasW      = mapping.getCanvas().getWidth();
        double canvasH      = mapping.getCanvas().getHeight();
        double pad          = 50;
        boolean useImage    = mapping.getZoom() >= MIN_ZOOM_FOR_MARKERS;
        double dotRadius    = 4;

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
                        gc.strokeOval(
                                p.x - MARKER_WIDTH / 2 - 4,
                                p.y - MARKER_OFFSET_Y - 4,
                                MARKER_WIDTH + 8, MARKER_HEIGHT + 8);
                    }
                    gc.drawImage(personMarker,
                            p.x - MARKER_WIDTH / 2, p.y - MARKER_OFFSET_Y,
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

        GraphicsContext gc  = mapping.getGc();
        double canvasW      = mapping.getCanvas().getWidth();
        double canvasH      = mapping.getCanvas().getHeight();
        double pad          = 60;
        boolean useImage    = mapping.getZoom() >= MIN_ZOOM_FOR_MARKERS;
        double dotRadius    = 4;
        Color dotColor      = Color.rgb(234, 179, 8, 0.85);

        for (EvacSiteMarker site : evacSites) {
            if (!Mapping.isValidCoordinate(site.lat, site.lon)) continue;
            if (!isPointInPolygon(site.lon, site.lat, boundary)) continue;
            try {
                Mapping.Point p = mapping.latLonToScreen(site.lat, site.lon);
                if (p.x < -pad || p.x > canvasW + pad) continue;
                if (p.y < -pad || p.y > canvasH + pad) continue;

                if (useImage && evacSiteMarker != null) {
                    gc.drawImage(evacSiteMarker,
                            p.x - EVAC_MARKER_WIDTH / 2, p.y - EVAC_MARKER_OFFSET_Y,
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
    private double longestNameWidth(String beneName,
                                    List<FamilyMemberModel> members,
                                    double charPx, double hPad) {
        double max = (beneName != null ? beneName.length() : 8) * charPx + hPad;
        if (members != null) {
            for (FamilyMemberModel m : members) {
                String n = m.getFullName();
                double w = (n != null ? n.length() : 4) * charPx + hPad;
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
        btn.setOnMouseExited(e  -> btn.setStyle(n));
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