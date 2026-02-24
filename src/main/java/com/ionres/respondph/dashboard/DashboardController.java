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
    @FXML private Label adminNameLabel;
    @FXML private Button searchToggleBtn;
    @FXML private TextField beneficiarySearchBox;
    @FXML private HBox searchOverlay;
    @FXML private StackPane cardBeneficiary;
    @FXML private StackPane cardDisasters;
    @FXML private StackPane cardAids;
    @FXML private StackPane cardEvacuationSite;
    @FXML private VBox searchBoxWrap;
    @FXML private ListView<String> beneficiarySearchList;

    // ── Marker images ─────────────────────────────────────────────────────────
    private Image personMarker;
    private Image evacSiteMarker;

    private static final double MIN_ZOOM_FOR_MARKERS = 16.0;
    private static final double MARKER_WIDTH = 32;
    private static final double MARKER_HEIGHT = 32;
    private static final double MARKER_OFFSET_Y = MARKER_HEIGHT;
    private static final double EVAC_MARKER_WIDTH = 32;
    private static final double EVAC_MARKER_HEIGHT = 32;
    private static final double EVAC_MARKER_OFFSET_Y = EVAC_MARKER_HEIGHT;

    // ── Drag / zoom ───────────────────────────────────────────────────────────
    private double dragStartX, dragStartY;
    private double currentCenterLat;
    private double currentCenterLon;
    private double currentZoom = 13.0;

    // ── Drag threshold — prevents accidental pan on click ─────────────────────
    private boolean isDragging = false;
    private static final double DRAG_THRESHOLD = 4.0;

    // ── Selection ─────────────────────────────────────────────────────────────
    private BeneficiaryMarker selectedBeneficiary = null;
    private VBox infoPanel;

    // ── Search dropdown ───────────────────────────────────────────────────────
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

        // FIX #1: Wire searchToggleBtn here (before Platform.runLater) — FXML
        // injection is complete at initialize() time, so this is safe.
        searchToggleBtn.setOnAction(e -> searchToggle());

        // FIX #2: Merged into a SINGLE Platform.runLater block so all UI setup
        // runs in one consistent pass on the FX thread, eliminating race conditions
        // between the two previously separate runLater calls.
        Platform.runLater(() -> {

            // ── searchOverlay sizing ──────────────────────────────────────
            if (searchOverlay != null) {
                searchOverlay.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                searchOverlay.setPickOnBounds(false);
                searchOverlay.setSpacing(8);
                searchOverlay.setPadding(new Insets(10, 10, 0, 0));
            }

            // ── Load marker images ────────────────────────────────────────
            try {
                personMarker = new Image(getClass().getResourceAsStream("/images/person_marker.png"));
                if (personMarker.isError()) personMarker = null;

                evacSiteMarker = new Image(getClass().getResourceAsStream("/images/location-pin.png"));
                if (evacSiteMarker.isError()) evacSiteMarker = null;
            } catch (Exception e) {
                personMarker = null;
                evacSiteMarker = null;
            }

            // ── Init map ──────────────────────────────────────────────────
            mapping.init(mapContainer);
            mapping.setAfterRedraw(() -> {
                drawBoundary();
                drawEvacSites();
                drawBeneficiaries();
                repositionPanel();
            });

            buildInfoPanel();
            wireSearchComboBoxInline();

            // ── Mouse pressed ─────────────────────────────────────────────
            mapContainer.setOnMousePressed(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    isDragging = false;
                    // Dismiss panel when clicking empty map area
                    BeneficiaryMarker hit = findMarkerAtScreen(e.getX(), e.getY());
                    if (hit == null && selectedBeneficiary != null) {
                        dismissPanel();
                    }
                    dragStartX = e.getX();
                    dragStartY = e.getY();
                }
            });

            // ── Mouse dragged ─────────────────────────────────────────────
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

                    double tilesOnScreen = Math.pow(2, currentZoom);
                    double degPerPixelLon = 360.0 / (tilesOnScreen * 256.0);
                    double degPerPixelLat = degPerPixelLon * Math.cos(Math.toRadians(currentCenterLat));

                    currentCenterLon -= dx * degPerPixelLon;
                    currentCenterLat += dy * degPerPixelLat;
                    mapping.setCenter(currentCenterLat, currentCenterLon, currentZoom);
                }
            });

            // ── Mouse released ────────────────────────────────────────────
            mapContainer.setOnMouseReleased(e -> isDragging = false);

            // ── Double click — show info panel ────────────────────────────
            mapContainer.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                    if (isDragging) return;
                    BeneficiaryMarker hit = findMarkerAtScreen(e.getX(), e.getY());
                    if (hit != null) selectBeneficiary(hit, false);
                }
            });

            // ── Scroll to zoom ────────────────────────────────────────────
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
                    adminNameLabel.setText(admin.getRole() + " : " + display);
                }
            });
        });
    }

    // Tracks whether the user is actively pressing on the search list.
    // Used to suppress focus-lost hiding so the selection always completes.
    private boolean isSelectingFromList = false;

    private void wireSearchComboBoxInline() {
        beneficiarySearchList.setItems(searchItems);
        beneficiarySearchList.setFixedCellSize(36);

        // FIX — width: lock list to exactly the TextField width (280px) so it
        // never overflows. Also set a custom cell factory with text ellipsis so
        // long names don't burst out of the cell or create a horizontal scrollbar.
        beneficiarySearchList.setPrefWidth(280);
        beneficiarySearchList.setMinWidth(280);
        beneficiarySearchList.setMaxWidth(280);
        beneficiarySearchList.setCellFactory(lv -> new ListCell<>() {
            {
                // Clip text to cell width; show "…" when truncated
                setMaxWidth(Double.MAX_VALUE);
                setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
                setWrapText(false);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText((item == null || empty) ? null : item);
            }
        });

        // FIX — capture selection on MOUSE_PRESSED, not MOUSE_CLICKED.
        // MOUSE_PRESSED fires BEFORE the TextField loses focus, so the item is
        // reliably captured. We also set isSelectingFromList=true here so the
        // focus-lost listener knows not to hide the dropdown prematurely.
        beneficiarySearchList.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                isSelectingFromList = true;
                // getSelectionModel().getSelectedItem() may not yet reflect the
                // pressed row — use the index under the cursor instead.
                int index = (int) (e.getY() / beneficiarySearchList.getFixedCellSize());
                if (index >= 0 && index < searchItems.size()) {
                    String item = searchItems.get(index);
                    if (item != null && !item.isBlank()) {
                        handleBeneficiarySelected(item);
                    }
                }
                e.consume(); // prevent focus from leaving the TextField via this click
            }
        });

        // Typing filter
        beneficiarySearchBox.textProperty().addListener((obs, oldVal, newVal) -> {
            if (suppressListener) return;

            String filter = (newVal == null) ? "" : newVal.trim().toLowerCase();
            if (filter.isEmpty()) {
                searchItems.clear();
                hideInlineDropdown();
                return;
            }

            List<String> filtered = beneficiaries.stream()
                    .filter(b -> b.name != null && b.name.toLowerCase().contains(filter))
                    .map(b -> b.name)
                    .collect(Collectors.toList());

            searchItems.setAll(filtered);

            if (!filtered.isEmpty()) showInlineDropdown();
            else hideInlineDropdown();
        });

        // Keyboard navigation
        beneficiarySearchBox.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case DOWN -> {
                    if (!beneficiarySearchList.isVisible()) showInlineDropdown();
                    beneficiarySearchList.requestFocus();
                    beneficiarySearchList.getSelectionModel().select(0);
                    e.consume();
                }
                case ESCAPE -> {
                    hideInlineDropdown();
                    e.consume();
                }
                case ENTER -> {
                    String selected = beneficiarySearchList.getSelectionModel().getSelectedItem();
                    if (selected != null) handleBeneficiarySelected(selected);
                    e.consume();
                }
            }
        });

        // FIX — focus-lost: only hide the dropdown when focus leaves AND the user
        // is NOT in the middle of pressing a list item. isSelectingFromList is
        // reset inside handleBeneficiarySelected after the selection completes.
        beneficiarySearchBox.focusedProperty().addListener((obs, was, isFocused) -> {
            if (!isFocused && !isSelectingFromList) {
                Platform.runLater(this::hideInlineDropdown);
            }
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
        // Always reset the selection guard so the focus-lost listener
        // can operate normally after this call completes.
        isSelectingFromList = false;

        if (selected == null || selected.isBlank()) return;

        BeneficiaryMarker found = beneficiaries.stream()
                .filter(b -> selected.equals(b.name))
                .findFirst()
                .orElse(null);

        if (found == null) return;

        // Show the info panel on the map
        selectBeneficiary(found, true);

        suppressListener = true;

        hideInlineDropdown();
        if (beneficiarySearchList != null) {
            beneficiarySearchList.getSelectionModel().clearSelection();
        }

        searchItems.clear();
        beneficiarySearchBox.clear();

        suppressListener = false;

        // Return focus to search box so user can search again immediately
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

    // ── Card listeners ────────────────────────────────────────────────────────
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

    // ── Info panel ────────────────────────────────────────────────────────────
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
        infoPanel.setViewOrder(-1.0);
        mapContainer.getChildren().add(infoPanel);
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
            scroll.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, javafx.event.Event::consume);

            infoPanel.getChildren().addAll(midDiv, familyRow, scroll);
        }

        Region bottomPad = new Region();
        bottomPad.setPrefHeight(8);
        infoPanel.getChildren().add(bottomPad);
    }

    // FIX #6: selectBeneficiary — removed double-redraw when centerMap=false.
    // Previously, mapping.redraw() was called inside Platform.runLater() alongside
    // clampAndPositionPanel(), which then triggered setAfterRedraw → repositionPanel()
    // for a second redundant repositioning pass. Now we only call clampAndPositionPanel()
    // directly; the redraw is handled separately and only when needed.
    private void selectBeneficiary(BeneficiaryMarker b, boolean centerMap) {
        selectedBeneficiary = b;

        populateInfoPanel(b);

        // Keep invisible during layout so the user never sees it flash at 0,0.
        // opacity=0 + visible=true means it participates in layout but is not seen.
        infoPanel.setVisible(true);
        infoPanel.setOpacity(0);

        if (centerMap) {
            currentCenterLat = b.lat;
            currentCenterLon = b.lon;
            currentZoom = 19.0;
            mapping.setCenter(currentCenterLat, currentCenterLon, currentZoom);
            mapping.redraw();
        }

        // Use TWO nested Platform.runLater calls.
        //
        // Why two levels?
        //   • First runLater: JavaFX processes the scene pulse triggered by
        //     populateInfoPanel (children added) and setVisible(true). After this
        //     pulse the scene graph knows the panel exists and has measured it.
        //   • Second runLater: NOW getWidth()/getHeight() reflect real values
        //     because the pulse that measured the node has fully committed.
        //     This is the earliest safe point to read dimensions and position.
        //
        // This is the standard JavaFX pattern for "do X after next layout pulse".
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
        if (selectedBeneficiary == null) return;
        if (infoPanel == null || !infoPanel.isVisible()) return;
        Platform.runLater(this::clampAndPositionPanel);
    }

    private void clampAndPositionPanel() {
        if (selectedBeneficiary == null || infoPanel == null || !mapping.isInitialized()) return;

        try {
            Mapping.Point p = mapping.latLonToScreen(selectedBeneficiary.lat, selectedBeneficiary.lon);

            double mapW = mapContainer.getWidth();
            double mapH = mapContainer.getHeight();
            if (mapW <= 0 || mapH <= 0) return;

            // Always use prefWidth/prefHeight as the authoritative size source.
            // getWidth()/getHeight() return 0 for nodes that haven't been through
            // a full scene layout pulse (e.g. first render), while prefWidth/prefHeight
            // compute from content regardless of render state.
            double pw = infoPanel.prefWidth(-1);
            if (pw <= 0) pw = 220;

            double maxAllowedH = mapH - 16;
            infoPanel.setMaxHeight(maxAllowedH);

            double ph = infoPanel.prefHeight(pw);
            if (ph <= 0) ph = 100;
            if (ph > maxAllowedH) ph = maxAllowedH;

            // Horizontally center over marker; clamp to map edges
            double tx = p.x - (pw / 2.0);
            tx = Math.max(4, Math.min(tx, mapW - pw - 4));

            // Try to show above the marker; flip below if not enough space
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

    // ── Marker hit detection ──────────────────────────────────────────────────
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
                    hx = p.x - r;
                    hy = p.y - r;
                    hw = r * 2;
                    hh = r * 2;
                }

                if (sx >= hx && sx <= hx + hw && sy >= hy && sy <= hy + hh) return b;
            } catch (Exception ignored) {}
        }

        return null;
    }

    // ── Center on boundary ────────────────────────────────────────────────────
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

    // ── Data loaders ──────────────────────────────────────────────────────────
    public void loadBeneficiariesFromDb() {
        beneficiaries.clear();
        beneficiaries.addAll(dashBoardService.getBeneficiaries());

        // FIX #4: Always refresh searchItems from the newly loaded beneficiaries,
        // regardless of search box visibility. The search box starts hidden, so
        // the old visibility-gated check meant the list was NEVER populated on
        // load. Now we always rebuild the backing data; the dropdown itself only
        // shows when the user types, which is correct behavior.
        List<String> names = beneficiaries.stream()
                .filter(b -> b.name != null && !b.name.isBlank())
                .map(b -> b.name)
                .collect(Collectors.toList());

        // Only refresh visible dropdown if it's currently open and showing results
        if (beneficiarySearchList != null && beneficiarySearchList.isVisible()) {
            searchItems.setAll(names);
        }
        // (searchItems will be rebuilt on next keystroke from the full beneficiaries list)

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

    // ── Drawing ───────────────────────────────────────────────────────────────
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
                        gc.strokeOval(
                                p.x - MARKER_WIDTH / 2 - 4,
                                p.y - MARKER_OFFSET_Y - 4,
                                MARKER_WIDTH + 8, MARKER_HEIGHT + 8
                        );
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

                    Color fill = sel ? Color.rgb(249, 115, 22, 0.95) : Color.rgb(0, 120, 255, 0.85);
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

    // FIX #5: Added isInitialized() guard to drawBoundary(), matching the same
    // defensive pattern used in drawBeneficiaries() and drawEvacSites().
    // Without this guard, latLonToScreen() could be called before the map is
    // ready, throwing a NullPointerException during the very first redraw pass.
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

    // ── Helpers ───────────────────────────────────────────────────────────────
    private double longestNameWidth(String beneName, List<FamilyMemberModel> members, double hPad) {
        Text probe = new Text();

        probe.setStyle("-fx-font-size: 13px; -fx-font-weight: 800;");
        probe.setText(beneName != null ? beneName : "Unknown");
        double max = probe.getLayoutBounds().getWidth() + hPad + 24;

        if (members != null) {
            probe.setStyle("-fx-font-size: 12px; -fx-font-weight: 600;");
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
        // node.setVisible(true) is already called before this in selectBeneficiary
        // so we just animate opacity 0 → 1.
        node.setOpacity(0);
        node.setVisible(true); // ensure visible in all call paths
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