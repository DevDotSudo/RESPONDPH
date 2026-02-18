package com.ionres.respondph.aid.dialogs_controller;

import com.ionres.respondph.aid.*;
import com.ionres.respondph.aid.KMeansAidDistribution.BeneficiaryCluster;
import com.ionres.respondph.aid_type.AidTypeModelComboBox;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.disaster.DisasterModelComboBox;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.DashboardRefresher;
import com.itextpdf.layout.element.Cell;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.print.*;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.borders.SolidBorder;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.ObservableSet;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import java.util.Optional;

// ─── CHANGED: added import for PauseTransition and Duration ───────────────────
import javafx.animation.PauseTransition;
import javafx.util.Duration;
// ─────────────────────────────────────────────────────────────────────────────

public class AddAidController {

    @FXML private VBox        root;
    @FXML private TextField   nameFld;
    @FXML private TextField   quantityFld;
    @FXML private TextField   quantityPerBeneficiaryFld;
    @FXML private TextField   costFld;
    @FXML private TextField   providerFld;

    @FXML private RadioButton useKMeansRadio;
    @FXML private RadioButton useFCMRadio;
    @FXML private VBox        kmeansInfoBox;
    @FXML private VBox        fcmInfoBox;

    @FXML private Button previewBtn;
    @FXML private Button printCustomBtn;
    @FXML private Button saveAidBtn;
    @FXML private Button cancelBtn;
    @FXML private Button closeBtn;

    @FXML private Label  infoLabel;
    @FXML private HBox   simpleDistributionWarning;
    @FXML private HBox   selectionSummaryBox;
    @FXML private Label  selectionSummaryLabel;

    @FXML private ComboBox<AidTypeModelComboBox>  aidTypeComboBox;
    @FXML private ComboBox<DisasterModelComboBox> disasterComboBox;
    @FXML private CheckBox     useBarangayFilterCheckbox;
    @FXML private ComboBox<String> barangayComboBox;
    @FXML private RadioButton  singleBarangayRadio;
    @FXML private RadioButton  allBarangaysRadio;
    @FXML private VBox         barangaySelectionContainer;
    @FXML private VBox         singleBarangayContainer;
    @FXML private CheckBox     generalAidCheckbox;

    @FXML private VBox clusterPreviewPanel;

    private Stage progressStage;
    private ProgressBar progressBar;
    private Label progressLabel;

    private AidService     aidService;
    private AidServiceImpl aidServiceImpl;
    private AidDAO         aidDAO;
    private AidController  aidController;
    private Stage          dialogStage;

    // ─── CHANGED: added ProgressIndicator field for summary spinner ───────────
    private ProgressIndicator summaryProgressIndicator;
    // ─────────────────────────────────────────────────────────────────────────

    // ─── CHANGED: added PauseTransition for debouncing cluster schedule ───────
    private PauseTransition clusterDebounce;
    // ─────────────────────────────────────────────────────────────────────────

    // ─── CHANGED: flag to suppress barangay listener during programmatic load ─
    private boolean suppressBarangayListener = false;
    // ─────────────────────────────────────────────────────────────────────────

    private ToggleGroup algorithmGroup;
    private ToggleGroup barangayModeGroup;

    private List<BeneficiaryCluster> lastPreviewResult = new ArrayList<>();
    private volatile boolean previewLoading = false;
    private volatile long    previewGeneration = 0;

    private static final int FIXED_CLUSTERS = 3;

    private static final DeviceRgb COLOR_HIGH     = new DeviceRgb(41,  128, 185);
    private static final DeviceRgb COLOR_MODERATE = new DeviceRgb(243, 156,  18);
    private static final DeviceRgb COLOR_LOW      = new DeviceRgb(149, 165, 166);
    private static final DeviceRgb COLOR_HEADER_BG= new DeviceRgb(44,  62,  80);
    private static final DeviceRgb COLOR_WHITE    = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb COLOR_ROW_ALT  = new DeviceRgb(248, 249, 250);

    public void setAidService(AidService aidService) {
        this.aidService = aidService;
        if (aidService instanceof AidServiceImpl) {
            this.aidServiceImpl = (AidServiceImpl) aidService;
        }
    }

    public void setAidController(AidController c) { this.aidController = c; }
    public void setDialogStage(Stage stage)        { this.dialogStage   = stage; }

    public void setAidTypes(List<AidTypeModelComboBox> aidTypes) {
        aidTypeComboBox.setItems(FXCollections.observableArrayList(aidTypes));
    }

    public void setDisasters(List<DisasterModelComboBox> disasters) {
        disasterComboBox.setItems(FXCollections.observableArrayList(disasters));
    }

    public void setSelectedAidTypeAndDisaster(AidTypeModelComboBox aidType,
                                              DisasterModelComboBox disaster) {
        if (aidType  != null) aidTypeComboBox.setValue(aidType);
        if (disaster != null) {
            disasterComboBox.setValue(disaster);
            generalAidCheckbox.setSelected(false);
        }
        updateSelectionSummary();
        loadBarangays();
    }

    @FXML
    private void initialize() {
        if (aidService == null) {
            aidServiceImpl = new AidServiceImpl();
            aidService     = aidServiceImpl;
        } else if (aidService instanceof AidServiceImpl) {
            aidServiceImpl = (AidServiceImpl) aidService;
        }
        aidDAO = new AidDAOImpl(DBConnection.getInstance());

        setupAlgorithmToggle();
        setupGeneralAidOption();
        setupBarangayMode();
        setupEventHandlers();
        setupDefaultValues();
        setupComboBoxListeners();
        makeDraggable();

        hideInlinePanel();
    }

    private void setupAlgorithmToggle() {
        algorithmGroup = new ToggleGroup();
        useKMeansRadio.setToggleGroup(algorithmGroup);
        useFCMRadio.setToggleGroup(algorithmGroup);
        useKMeansRadio.setSelected(true);

        algorithmGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isFCM = (newVal == useFCMRadio);
            if (kmeansInfoBox != null) { kmeansInfoBox.setVisible(!isFCM); kmeansInfoBox.setManaged(!isFCM); }
            if (fcmInfoBox    != null) { fcmInfoBox.setVisible(isFCM);     fcmInfoBox.setManaged(isFCM);     }
            if (simpleDistributionWarning != null) {
                simpleDistributionWarning.setVisible(false);
                simpleDistributionWarning.setManaged(false);
            }
            // ─── CHANGED: clear result before rescheduling ────────────────
            lastPreviewResult = new ArrayList<>();
            // ─────────────────────────────────────────────────────────────
            scheduleBackgroundCluster();
        });
    }

    // ─── CHANGED: removed updateSelectionSummary() call; scheduleBackgroundCluster handles it ─
    private void setupGeneralAidOption() {
        if (generalAidCheckbox != null) {
            generalAidCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) { disasterComboBox.setValue(null); disasterComboBox.setDisable(true); }
                else        { disasterComboBox.setDisable(false); }
                lastPreviewResult = new ArrayList<>();
                loadBarangays();
                scheduleBackgroundCluster();
            });
        }
    }
    // ──────────────────────────────────────────────────────────────────────────

    // ─── CHANGED: removed updateSelectionSummary() calls; scheduleBackgroundCluster handles it ─
    private void setupBarangayMode() {
        barangayModeGroup = new ToggleGroup();
        singleBarangayRadio.setToggleGroup(barangayModeGroup);
        allBarangaysRadio.setToggleGroup(barangayModeGroup);
        allBarangaysRadio.setSelected(true);

        barangayModeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean single = (newVal == singleBarangayRadio);
            singleBarangayContainer.setVisible(single);
            singleBarangayContainer.setManaged(single);
            lastPreviewResult = new ArrayList<>();
            scheduleBackgroundCluster();
        });

        useBarangayFilterCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            barangaySelectionContainer.setVisible(newVal);
            barangaySelectionContainer.setManaged(newVal);
            lastPreviewResult = new ArrayList<>();
            scheduleBackgroundCluster();
        });

        barangaySelectionContainer.setVisible(false);
        barangaySelectionContainer.setManaged(false);
    }
    // ──────────────────────────────────────────────────────────────────────────

    private void setupDefaultValues() {
        useKMeansRadio.setSelected(true);
        useBarangayFilterCheckbox.setSelected(false);
        generalAidCheckbox.setSelected(false);
    }

    // ─── CHANGED: removed updateSelectionSummary() calls; added suppressBarangayListener guard ─
    private void setupComboBoxListeners() {
        aidTypeComboBox.setOnAction(e -> {
            lastPreviewResult = new ArrayList<>();
            loadBarangays();
            scheduleBackgroundCluster();
        });
        disasterComboBox.setOnAction(e -> {
            if (disasterComboBox.getValue() != null) generalAidCheckbox.setSelected(false);
            lastPreviewResult = new ArrayList<>();
            loadBarangays();
            scheduleBackgroundCluster();
        });
        barangayComboBox.setOnAction(e -> {
            // ─── CHANGED: guard against programmatic changes during loadBarangays() ─
            if (suppressBarangayListener) return;
            // ──────────────────────────────────────────────────────────────────────
            lastPreviewResult = new ArrayList<>();
            scheduleBackgroundCluster();
        });
    }
    // ──────────────────────────────────────────────────────────────────────────

    private void setupEventHandlers() {
        saveAidBtn.setOnAction(this::handleSave);
        closeBtn.setOnAction(this::handleClose);
        if (cancelBtn      != null) cancelBtn.setOnAction(this::handleClose);
        if (previewBtn     != null) previewBtn.setOnAction(this::handlePreview);
        if (printCustomBtn != null) printCustomBtn.setOnAction(this::handlePrintCustom);
    }

    // =========================================================================
    //  CHANGED: scheduleBackgroundCluster — debounced with PauseTransition
    //           so the spinner renders before the DB query fires.
    //           Labels indicate each change within.
    // =========================================================================
    private void scheduleBackgroundCluster() {
        // ─── CHANGED: clear stale result immediately ──────────────────────────
        lastPreviewResult = new ArrayList<>();
        // ─────────────────────────────────────────────────────────────────────

        if (!isPreviewSelectionReady()) {
            previewLoading = false;
            // ─── CHANGED: stop any pending debounce ──────────────────────────
            if (clusterDebounce != null) clusterDebounce.stop();
            // ─────────────────────────────────────────────────────────────────
            hideInlinePanel();
            updatePreviewButtonState();
            // ─── CHANGED: pass -2 to hide the box entirely ───────────────────
            updateSelectionSummaryWithCount(-2);
            // ─────────────────────────────────────────────────────────────────
            return;
        }

        // ─── CHANGED: show spinner IMMEDIATELY on FX thread so it renders ─────
        //             before the 300ms debounce fires the DB call
        updateSelectionSummaryWithCount(-1);
        showInlinePanelLoading();
        previewLoading = true;
        updatePreviewButtonState();
        // ─────────────────────────────────────────────────────────────────────

        // ─── CHANGED: cancel previous debounce (user still changing selections) ─
        if (clusterDebounce != null) clusterDebounce.stop();
        // ─────────────────────────────────────────────────────────────────────

        // ─── CHANGED: increment generation NOW so any in-flight task is invalid ─
        final long myGen = ++previewGeneration;
        // ─────────────────────────────────────────────────────────────────────

        // ─── CHANGED: 300ms debounce — DB query only fires after user stops ────
        clusterDebounce = new PauseTransition(Duration.millis(300));
        clusterDebounce.setOnFinished(ev -> {

            // ─── CHANGED: double-check selection still valid after delay ──────
            if (!isPreviewSelectionReady() || myGen != previewGeneration) return;
            // ─────────────────────────────────────────────────────────────────

            final int aidTypeId  = aidTypeComboBox.getValue().getAidTypeId();
            final int disasterId = getSelectedDisasterId();

            Task<List<BeneficiaryCluster>> task = new Task<>() {
                @Override protected List<BeneficiaryCluster> call() {
                    return buildClusterOnlyPreview(aidTypeId, disasterId);
                }
            };

            task.setOnSucceeded(e -> {
                // ─── CHANGED: generation guard — discard stale results ────────
                if (myGen != previewGeneration) return;
                // ─────────────────────────────────────────────────────────────
                List<BeneficiaryCluster> result = task.getValue();
                lastPreviewResult = result != null ? result : new ArrayList<>();
                previewLoading    = false;
                updatePreviewButtonState();
                renderInlinePanel(lastPreviewResult);
                // ─── CHANGED: show real count once ready ─────────────────────
                updateSelectionSummaryWithCount(lastPreviewResult.size());
                // ─────────────────────────────────────────────────────────────
            });

            task.setOnFailed(e -> {
                if (myGen != previewGeneration) return;
                lastPreviewResult = new ArrayList<>();
                previewLoading    = false;
                updatePreviewButtonState();
                showInlinePanelError("Clustering failed. Click Preview to retry.");
                // ─── CHANGED: show 0 on failure instead of leaving spinner ────
                updateSelectionSummaryWithCount(0);
                // ─────────────────────────────────────────────────────────────
            });

            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
        });
        clusterDebounce.play();
        // ─────────────────────────────────────────────────────────────────────
    }
    // =========================================================================

    private void updatePreviewButtonState() {
        if (previewBtn == null) return;
        if (previewLoading) {
            previewBtn.setText("Loading…");
            previewBtn.setDisable(true);
        } else {
            previewBtn.setText("Preview");
            previewBtn.setDisable(false);
        }
    }

    @FXML
    private void handlePreview(ActionEvent event) {
        if (!isPreviewSelectionReady()) {
            AlertDialogManager.showWarning("Selection Required",
                    "Please select an Aid Type and a Disaster (or check General Aid) to preview clustering.");
            return;
        }

        if (!lastPreviewResult.isEmpty()) { showPreviewDialog(lastPreviewResult); return; }

        if (previewLoading) {
            AlertDialogManager.showWarning("Please Wait", "Clustering is still loading. Please try again.");
            return;
        }

        int aidTypeId  = aidTypeComboBox.getValue().getAidTypeId();
        int disasterId = getSelectedDisasterId();

        previewLoading = true;
        updatePreviewButtonState();
        final long myGen = ++previewGeneration;

        Task<List<BeneficiaryCluster>> task = new Task<>() {
            @Override protected List<BeneficiaryCluster> call() {
                return buildClusterOnlyPreview(aidTypeId, disasterId);
            }
        };

        task.setOnSucceeded(e -> {
            if (myGen != previewGeneration) return;
            List<BeneficiaryCluster> result = task.getValue();
            lastPreviewResult = result != null ? result : new ArrayList<>();
            previewLoading    = false;
            updatePreviewButtonState();
            renderInlinePanel(lastPreviewResult);
            updateSelectionSummaryWithCount(lastPreviewResult.size());

            if (lastPreviewResult.isEmpty()) {
                AlertDialogManager.showWarning("No Eligible Beneficiaries",
                        "No beneficiaries are currently eligible for this selection.");
            } else {
                showPreviewDialog(lastPreviewResult);
            }
        });

        task.setOnFailed(e -> {
            if (myGen != previewGeneration) return;
            previewLoading = false;
            updatePreviewButtonState();
            AlertDialogManager.showError("Preview Error", "Failed to generate clustering preview:\n"
                    + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void showPreviewDialog(List<BeneficiaryCluster> preview) {
        String algorithm = isFCMSelected()
                ? "Fuzzy C-Means (FCM) — 3 Clusters"
                : "K-Means — 3 Clusters";

        List<Integer> ids = preview.stream().map(BeneficiaryCluster::getBeneficiaryId).collect(Collectors.toList());
        Map<Integer, String> nameMap = aidDAO.getBeneficiaryNames(ids);

        Map<Integer, String>  clusterToPriority = buildClusterPriorityMap(preview);
        Map<Integer, Double>  clusterSums   = new HashMap<>();
        Map<Integer, Integer> clusterCounts = new HashMap<>();
        for (BeneficiaryCluster b : preview) {
            int c = b.getCluster();
            clusterSums.put(c,   clusterSums.getOrDefault(c,   0.0) + b.getFinalScore());
            clusterCounts.put(c, clusterCounts.getOrDefault(c, 0)   + 1);
        }
        Map<Integer, Double> clusterAvg = new HashMap<>();
        for (Integer c : clusterSums.keySet()) clusterAvg.put(c, clusterSums.get(c) / clusterCounts.get(c));

        List<Integer> sortedClusters = new ArrayList<>(clusterAvg.keySet());
        sortedClusters.sort((a, b) -> Double.compare(clusterAvg.get(b), clusterAvg.get(a)));

        Map<Integer, List<BeneficiaryCluster>> byCluster = new HashMap<>();
        for (BeneficiaryCluster b : preview)
            byCluster.computeIfAbsent(b.getCluster(), k -> new ArrayList<>()).add(b);

        StringBuilder msg = new StringBuilder();
        msg.append("Scope   : ").append(getDistributionScopeText()).append("\n");
        msg.append("Method  : ").append(algorithm).append("\n");
        msg.append("Total   : ").append(preview.size()).append(" beneficiaries\n\n");
        msg.append("=== Clustering Result (3 Clusters) ===\n\n");

        int overallRank = 1;
        for (Integer clusterNum : sortedClusters) {
            String priorityName = clusterToPriority.getOrDefault(clusterNum, "Low Priority");
            List<BeneficiaryCluster> members = byCluster.getOrDefault(clusterNum, new ArrayList<>());
            if (members.isEmpty()) continue;
            members.sort((b1, b2) -> Double.compare(b2.getFinalScore(), b1.getFinalScore()));
            msg.append(String.format(
                    "--- %s  |  Cluster %d  |  %d beneficiaries  |  Avg Score: %.3f ---\n",
                    priorityName, clusterNum, members.size(), clusterAvg.get(clusterNum)));
            for (BeneficiaryCluster b : members) {
                String name = nameMap.getOrDefault(b.getBeneficiaryId(), "Unknown");
                msg.append(String.format("%3d.  %-30s  (ID: %-6d)  Score: %.3f\n",
                        overallRank++, name, b.getBeneficiaryId(), b.getFinalScore()));
            }
            msg.append("\n");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Clustering Preview");
        alert.setHeaderText("Beneficiary Priority Clustering — " + algorithm);
        TextArea ta = new TextArea(msg.toString());
        ta.setEditable(false); ta.setWrapText(false);
        ta.setMaxWidth(Double.MAX_VALUE); ta.setMaxHeight(Double.MAX_VALUE);
        ta.setPrefRowCount(24); ta.setPrefColumnCount(70);
        ta.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");
        alert.getDialogPane().setContent(ta);
        alert.getDialogPane().setPrefWidth(750);
        alert.showAndWait();
    }

    private void hideInlinePanel() {
        if (clusterPreviewPanel == null) return;
        clusterPreviewPanel.setVisible(false);
        clusterPreviewPanel.setManaged(false);
        clusterPreviewPanel.getChildren().clear();
    }

    private void showInlinePanelLoading() {
        if (clusterPreviewPanel == null) return;
        clusterPreviewPanel.setVisible(true);
        clusterPreviewPanel.setManaged(true);
        clusterPreviewPanel.getChildren().clear();
        Label lbl = new Label("⏳  Calculating clusters…");
        lbl.setStyle("-fx-text-fill: #666; -fx-font-style: italic; -fx-font-size: 12px;");
        lbl.setPadding(new Insets(8, 0, 8, 0));
        clusterPreviewPanel.getChildren().add(lbl);
    }

    private void showInlinePanelError(String message) {
        if (clusterPreviewPanel == null) return;
        clusterPreviewPanel.getChildren().clear();
        Label lbl = new Label("⚠  " + message);
        lbl.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 12px;");
        clusterPreviewPanel.getChildren().add(lbl);
    }

    private void renderInlinePanel(List<BeneficiaryCluster> result) {
        if (clusterPreviewPanel == null) return;
        clusterPreviewPanel.getChildren().clear();
        clusterPreviewPanel.setVisible(true);
        clusterPreviewPanel.setManaged(true);

        if (result.isEmpty()) {
            Label none = new Label("⚠  No eligible beneficiaries found for this selection.");
            none.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 12px;");
            none.setPadding(new Insets(8, 0, 4, 0));
            clusterPreviewPanel.getChildren().add(none);
            return;
        }

        List<Integer> ids = result.stream().map(BeneficiaryCluster::getBeneficiaryId).collect(Collectors.toList());
        Map<Integer, String> nameMap = aidDAO.getBeneficiaryNames(ids);

        String algorithm = isFCMSelected()
                ? "Fuzzy C-Means (FCM) — 3 Clusters"
                : "K-Means — 3 Clusters";

        Label header = new Label("CLUSTERING PREVIEW");
        header.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        header.setStyle("-fx-text-fill: #1a252f;");

        Label meta = new Label(String.format("Method: %s  ·  Total: %d beneficiaries", algorithm, result.size()));
        meta.setStyle("-fx-text-fill: #555; -fx-font-size: 11px;");
        meta.setPadding(new Insets(0, 0, 6, 0));
        clusterPreviewPanel.getChildren().addAll(header, meta);

        Map<Integer, String>  clusterToPriority = buildClusterPriorityMap(result);
        Map<Integer, Double>  sums   = new HashMap<>();
        Map<Integer, Integer> counts = new HashMap<>();
        for (BeneficiaryCluster b : result) {
            int c = b.getCluster();
            sums.put(c,   sums.getOrDefault(c,   0.0) + b.getFinalScore());
            counts.put(c, counts.getOrDefault(c, 0)   + 1);
        }
        Map<Integer, Double> avg = new HashMap<>();
        for (Integer c : sums.keySet()) avg.put(c, sums.get(c) / counts.get(c));

        List<Integer> sorted = new ArrayList<>(avg.keySet());
        sorted.sort((a, b) -> Double.compare(avg.get(b), avg.get(a)));

        Map<Integer, List<BeneficiaryCluster>> byCluster = new HashMap<>();
        for (BeneficiaryCluster b : result)
            byCluster.computeIfAbsent(b.getCluster(), k -> new ArrayList<>()).add(b);

        String[] bgs      = { "#eaf4fb", "#fef9e7", "#f8f9fa" };
        String[] borders  = { "#2980b9", "#f39c12", "#95a5a6" };
        String[] textCols = { "#1a5276", "#7d6608", "#566573" };
        int rank = 0, overallRank = 1;

        for (Integer clusterNum : sorted) {
            String priorityName = clusterToPriority.getOrDefault(clusterNum, "Low Priority");
            List<BeneficiaryCluster> members = byCluster.getOrDefault(clusterNum, new ArrayList<>());
            if (members.isEmpty()) { rank++; continue; }
            members.sort((b1, b2) -> Double.compare(b2.getFinalScore(), b1.getFinalScore()));

            String bg  = rank < bgs.length      ? bgs[rank]      : bgs[2];
            String brd = rank < borders.length  ? borders[rank]  : borders[2];
            String txt = rank < textCols.length ? textCols[rank] : textCols[2];

            VBox section = new VBox(2);
            section.setPadding(new Insets(7, 10, 7, 10));
            section.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 0 0 0 4; -fx-background-radius: 4;",
                    bg, brd));

            HBox sectionHdr = new HBox(8);
            sectionHdr.setAlignment(Pos.CENTER_LEFT);

            Label pLbl  = new Label(priorityName.toUpperCase());
            pLbl.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            pLbl.setStyle("-fx-text-fill: " + txt + ";");

            Label badge = new Label(members.size() + " beneficiaries");
            badge.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white;" +
                    "-fx-padding: 1 5 1 5; -fx-background-radius: 9; -fx-font-size: 10px;", brd));

            Label avgLbl = new Label(String.format("avg: %.3f", avg.get(clusterNum)));
            avgLbl.setStyle("-fx-text-fill: " + txt + "; -fx-font-size: 10px;");

            sectionHdr.getChildren().addAll(pLbl, badge, avgLbl);
            section.getChildren().add(sectionHdr);

            for (BeneficiaryCluster b : members) {
                String name = nameMap.getOrDefault(b.getBeneficiaryId(), "Unknown");
                HBox row = new HBox(6);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(1, 0, 1, 4));

                Label rnk = new Label(String.format("%3d.", overallRank++));
                rnk.setFont(Font.font("Courier New", 11));
                rnk.setStyle("-fx-text-fill: #999; -fx-min-width: 30;");

                Label nameLabel = new Label(name);
                nameLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 11px; -fx-min-width: 180;");

                Label id = new Label(String.format("(ID: %d)", b.getBeneficiaryId()));
                id.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 10px; -fx-min-width: 70;");

                Label sc = new Label(String.format("Score: %.3f", b.getFinalScore()));
                sc.setStyle("-fx-text-fill: " + txt + "; -fx-font-size: 11px;");

                row.getChildren().addAll(rnk, nameLabel, id, sc);
                section.getChildren().add(row);
            }
            clusterPreviewPanel.getChildren().add(section);
            VBox gap = new VBox(); gap.setMinHeight(3); gap.setMaxHeight(3);
            clusterPreviewPanel.getChildren().add(gap);
            rank++;
        }
    }

    // =========================================================================
    //  PRINT CUSTOM
    // =========================================================================

    @FXML
    private void handlePrintCustom(ActionEvent event) {
        if (aidTypeComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Selection Required", "Please select an Aid Type.");
            return;
        }
        if (lastPreviewResult.isEmpty()) {
            AlertDialogManager.showWarning("No Data",
                    "No clustering data available. Please wait for the preview to load or click Preview first.");
            return;
        }
        showProgressAndOpenPrintDialog();
    }

    private void showPrintCustomDialog(List<BeneficiaryCluster> preview) {
        Map<String, List<BeneficiaryCluster>> groups = groupBeneficiariesByPriority(preview);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Print Custom");
        dialog.setHeaderText("Choose priority levels and output format:");

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setPrefWidth(480);

        Label priorityLbl = new Label("Priority Levels to Include:");
        priorityLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        CheckBox highCheck = new CheckBox("High Priority ("
                + groups.getOrDefault("High Priority",     new ArrayList<>()).size() + " beneficiaries)");
        CheckBox medCheck  = new CheckBox("Moderate Priority ("
                + groups.getOrDefault("Moderate Priority", new ArrayList<>()).size() + " beneficiaries)");
        CheckBox lowCheck  = new CheckBox("Low Priority ("
                + groups.getOrDefault("Low Priority",      new ArrayList<>()).size() + " beneficiaries)");

        highCheck.setSelected(!groups.getOrDefault("High Priority",     new ArrayList<>()).isEmpty());
        medCheck.setSelected( !groups.getOrDefault("Moderate Priority", new ArrayList<>()).isEmpty());
        lowCheck.setSelected( !groups.getOrDefault("Low Priority",      new ArrayList<>()).isEmpty());
        if (groups.getOrDefault("High Priority",     new ArrayList<>()).isEmpty()) highCheck.setDisable(true);
        if (groups.getOrDefault("Moderate Priority", new ArrayList<>()).isEmpty()) medCheck.setDisable(true);
        if (groups.getOrDefault("Low Priority",      new ArrayList<>()).isEmpty()) lowCheck.setDisable(true);

        Separator sep1 = new Separator();

        Label outputLbl = new Label("Output Format:");
        outputLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        ToggleGroup outputGroup  = new ToggleGroup();
        RadioButton pdfRadio     = new RadioButton("Save as PDF");
        RadioButton printerRadio = new RadioButton("Send to Printer");
        pdfRadio.setToggleGroup(outputGroup);
        printerRadio.setToggleGroup(outputGroup);
        pdfRadio.setSelected(true);

        ObservableList<PrinterEntry> allEntries    = buildPrinterEntries();
        ObservableList<PrinterEntry> activeEntries = FXCollections.observableArrayList(
                allEntries.filtered(PrinterEntry::isActive));

        ComboBox<PrinterEntry> printerComboBox = new ComboBox<>(activeEntries);
        printerComboBox.setPrefWidth(440);
        printerComboBox.setMaxWidth(Double.MAX_VALUE);

        Callback<ListView<PrinterEntry>, ListCell<PrinterEntry>> nameOnlyFactory =
                lv -> new ListCell<>() {
                    @Override
                    protected void updateItem(PrinterEntry item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null); setGraphic(null);
                        } else {
                            setText(item.getPrinterName());
                            setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50;");
                            setGraphic(null);
                        }
                    }
                };

        printerComboBox.setCellFactory(nameOnlyFactory);
        printerComboBox.setButtonCell(nameOnlyFactory.call(null));

        activeEntries.stream()
                .filter(PrinterEntry::isDefault)
                .findFirst()
                .or(() -> activeEntries.isEmpty() ? Optional.empty() : Optional.of(activeEntries.get(0)))
                .ifPresent(printerComboBox::setValue);

        Separator sep2 = new Separator();
        Label printerLbl = new Label("Select Printer:");
        printerLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        VBox printerSection = new VBox(7);
        printerSection.getChildren().addAll(sep2, printerLbl, printerComboBox);
        printerSection.setVisible(false);
        printerSection.setManaged(false);

        outputGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == printerRadio) {
                if (activeEntries.isEmpty()) {
                    pdfRadio.setSelected(true);
                    Alert notConnected = new Alert(Alert.AlertType.ERROR);
                    notConnected.setTitle("Printer Not Connected");
                    notConnected.setHeaderText("Not Connected in printer");
                    notConnected.setContentText(
                            "No connected printer was detected on this system.\n\nPlease connect a printer and try again.");
                    notConnected.showAndWait();
                } else {
                    printerSection.setVisible(true);
                    printerSection.setManaged(true);
                    dialog.getDialogPane().getScene().getWindow().sizeToScene();
                }
            } else {
                printerSection.setVisible(false);
                printerSection.setManaged(false);
                dialog.getDialogPane().getScene().getWindow().sizeToScene();
            }
        });

        content.getChildren().addAll(
                priorityLbl, highCheck, medCheck, lowCheck,
                sep1, outputLbl, pdfRadio, printerRadio, printerSection);

        dialog.getDialogPane().setContent(content);

        ButtonType generateBtn = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(generateBtn, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == generateBtn) {
                List<BeneficiaryCluster> selected = new ArrayList<>();
                if (highCheck.isSelected()) selected.addAll(groups.getOrDefault("High Priority",     new ArrayList<>()));
                if (medCheck.isSelected())  selected.addAll(groups.getOrDefault("Moderate Priority", new ArrayList<>()));
                if (lowCheck.isSelected())  selected.addAll(groups.getOrDefault("Low Priority",      new ArrayList<>()));

                if (selected.isEmpty()) {
                    AlertDialogManager.showWarning("No Selection", "Please select at least one priority level.");
                    return;
                }

                if (pdfRadio.isSelected()) {
                    generateAndSavePDF(selected);
                } else {
                    PrinterEntry chosenEntry = printerComboBox.getValue();
                    if (chosenEntry == null) {
                        AlertDialogManager.showWarning("No Printer Selected", "Please select a printer from the list.");
                        return;
                    }
                    printBeneficiaryList(selected, chosenEntry.getPrinter());
                }
            }
        });
    }

    private static class PrinterEntry {
        private final Printer printer;
        private final boolean active;
        private final boolean isDefault;

        PrinterEntry(Printer p, boolean active, boolean isDefault) {
            this.printer   = p;
            this.active    = active;
            this.isDefault = isDefault;
        }

        Printer getPrinter()     { return printer; }
        String  getPrinterName() { return printer.getName(); }
        boolean isActive()       { return active; }
        boolean isDefault()      { return isDefault; }

        @Override public String toString() { return printer.getName(); }
    }

    private ObservableList<PrinterEntry> buildPrinterEntries() {
        ObservableList<PrinterEntry> entries = FXCollections.observableArrayList();
        ObservableSet<Printer> printers = Printer.getAllPrinters();
        Printer defaultPrinter = Printer.getDefaultPrinter();

        if (printers == null || printers.isEmpty()) return entries;

        for (Printer p : printers) {
            boolean active = isPrinterActive(p);
            boolean isDef  = p.equals(defaultPrinter);
            entries.add(new PrinterEntry(p, active, isDef));
        }

        entries.sort((a, b) -> {
            if (a.isDefault() != b.isDefault()) return a.isDefault() ? -1 : 1;
            if (a.isActive()  != b.isActive())  return a.isActive()  ? -1 : 1;
            return a.getPrinterName().compareToIgnoreCase(b.getPrinterName());
        });

        return entries;
    }

    private boolean isPrinterActive(Printer printer) {
        if (printer == null) return false;
        if (isVirtualPrinter(printer.getName())) return false;
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return isPrinterActiveWindows(printer.getName());
        return isPrinterActiveUnix(printer);
    }

    private boolean isVirtualPrinter(String printerName) {
        if (printerName == null) return false;
        String lower = printerName.toLowerCase();
        return lower.contains("pdf") || lower.contains("fax") || lower.contains("xps")
                || lower.contains("onenote") || lower.contains("microsoft print")
                || lower.contains("microsoft document") || lower.contains("send to")
                || lower.contains("snagit") || lower.contains("cutepdf")
                || lower.contains("cute pdf") || lower.contains("bullzip")
                || lower.contains("dopdf") || lower.contains("nitro")
                || lower.contains("foxit") || lower.contains("pdfcreator")
                || lower.contains("primopdf") || lower.contains("pdf24")
                || lower.contains("adobe pdf");
    }

    private boolean isPrinterActiveWindows(String printerName) {
        try {
            String safeName = printerName.replace("'", "''");
            String[] cmd = { "powershell", "-NoProfile", "-NonInteractive", "-Command",
                    "(Get-Printer -Name '" + safeName + "').IsOnline" };
            Process proc = Runtime.getRuntime().exec(cmd);
            String output;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(proc.getInputStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line.trim());
                output = sb.toString().trim();
            }
            try (java.io.InputStream err = proc.getErrorStream()) {
                err.transferTo(java.io.OutputStream.nullOutputStream());
            }
            proc.waitFor();
            if ("True".equalsIgnoreCase(output))  return true;
            if ("False".equalsIgnoreCase(output)) return false;
            return isPrinterActiveWindowsWmic(printerName);
        } catch (Exception e) {
            return isPrinterActiveWindowsWmic(printerName);
        }
    }

    private boolean isPrinterActiveWindowsWmic(String printerName) {
        try {
            String safeName = printerName.replace("'", "\\'");
            String[] cmd = { "wmic", "printer", "where", "Name='" + safeName + "'",
                    "get", "PrinterStatus,WorkOffline", "/format:csv" };
            Process proc = Runtime.getRuntime().exec(cmd);
            String output;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(proc.getInputStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append("\n");
                output = sb.toString();
            }
            try (java.io.InputStream err = proc.getErrorStream()) {
                err.transferTo(java.io.OutputStream.nullOutputStream());
            }
            proc.waitFor();
            for (String line : output.split("\n")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("Node") || line.startsWith("\r")) continue;
                String[] parts = line.split(",");
                if (parts.length < 3) continue;
                String statusStr   = parts[parts.length - 2].trim();
                String workOffline = parts[parts.length - 1].trim();
                if ("TRUE".equalsIgnoreCase(workOffline)) return false;
                try {
                    int status = Integer.parseInt(statusStr);
                    return (status == 3 || status == 4 || status == 5);
                } catch (NumberFormatException ex) { return false; }
            }
            return false;
        } catch (Exception e) { return false; }
    }

    private boolean isPrinterActiveUnix(Printer printer) {
        if (printer == null) return false;
        try {
            String[] cmd = { "lpstat", "-p", printer.getName() };
            Process proc = Runtime.getRuntime().exec(cmd);
            String output;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(proc.getInputStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append(" ");
                output = sb.toString().toLowerCase();
            }
            proc.waitFor();
            if (!output.isEmpty()) {
                if (output.contains("enabled"))  return true;
                if (output.contains("disabled")) return false;
            }
        } catch (Exception ignored) { }
        try {
            javax.print.PrintService[] services =
                    javax.print.PrintServiceLookup.lookupPrintServices(null, null);
            for (javax.print.PrintService svc : services) {
                if (!svc.getName().equalsIgnoreCase(printer.getName())) continue;
                javax.print.attribute.PrintServiceAttributeSet attrs = svc.getAttributes();
                javax.print.attribute.standard.PrinterIsAcceptingJobs accepting =
                        (javax.print.attribute.standard.PrinterIsAcceptingJobs)
                                attrs.get(javax.print.attribute.standard.PrinterIsAcceptingJobs.class);
                if (accepting != null &&
                        accepting == javax.print.attribute.standard.PrinterIsAcceptingJobs.NOT_ACCEPTING_JOBS)
                    return false;
                javax.print.attribute.standard.PrinterState state =
                        (javax.print.attribute.standard.PrinterState)
                                attrs.get(javax.print.attribute.standard.PrinterState.class);
                if (state != null) {
                    return state == javax.print.attribute.standard.PrinterState.IDLE
                            || state == javax.print.attribute.standard.PrinterState.PROCESSING;
                }
                break;
            }
        } catch (Exception ignored) { }
        return false;
    }

    // =========================================================================
    //  PDF GENERATION  (iText 7)
    // =========================================================================

    private void generateAndSavePDF(List<BeneficiaryCluster> beneficiaries) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Aid Distribution PDF");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files (.pdf)", ".pdf"));
        String defaultName = "AidDistribution_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        chooser.setInitialFileName(defaultName);
        File file = chooser.showSaveDialog(dialogStage);
        if (file == null) return;
        try {
            buildPDF(file, beneficiaries);
            AlertDialogManager.showInfo("PDF Saved", "PDF successfully saved to:\n" + file.getAbsolutePath());
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) desktop.open(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("PDF Error", "Failed to generate PDF:\n" + e.getMessage());
        }
    }

    private void buildPDF(File file, List<BeneficiaryCluster> beneficiaries) throws IOException {
        PdfFont fontBold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont fontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontMono   = PdfFontFactory.createFont(StandardFonts.COURIER);

        PdfDocument  pdfDoc = new PdfDocument(new PdfWriter(file));
        Document     doc    = new Document(pdfDoc, PageSize.A4);
        doc.setMargins(36, 36, 54, 36);

        String algorithm = isFCMSelected()
                ? "Fuzzy C-Means (FCM) Clustering — 3 Clusters"
                : "K-Means Clustering — 3 Clusters";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy  hh:mm a"));

        List<Integer> ids = beneficiaries.stream()
                .map(BeneficiaryCluster::getBeneficiaryId).collect(Collectors.toList());
        Map<Integer, String> nameMap = aidDAO.getBeneficiaryNames(ids);

        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth().setMarginBottom(4);
        Cell titleCell = new Cell()
                .add(new Paragraph("Aid Distribution Report")
                        .setFont(fontBold).setFontSize(20).setFontColor(COLOR_WHITE))
                .add(new Paragraph("Beneficiary Priority Clustering")
                        .setFont(fontNormal).setFontSize(11).setFontColor(new DeviceRgb(189, 215, 238)))
                .setBackgroundColor(COLOR_HEADER_BG).setPadding(14)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        Cell metaCell = new Cell()
                .add(new Paragraph("RespondPH")
                        .setFont(fontBold).setFontSize(13).setFontColor(COLOR_WHITE)
                        .setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph(timestamp)
                        .setFont(fontNormal).setFontSize(8).setFontColor(new DeviceRgb(189, 215, 238))
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(COLOR_HEADER_BG).setPadding(14)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
        headerTable.addCell(titleCell).addCell(metaCell);
        doc.add(headerTable);

        DeviceRgb metaBg = new DeviceRgb(240, 244, 248);
        Table metaTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth().setBackgroundColor(metaBg)
                .setBorder(new SolidBorder(new DeviceRgb(190, 210, 230), 1))
                .setMarginTop(10).setMarginBottom(14);
        addMetaRow(metaTable, "Distribution Scope",  getDistributionScopeText(), fontBold, fontNormal);
        addMetaRow(metaTable, "Algorithm",            algorithm,                  fontBold, fontNormal);
        addMetaRow(metaTable, "Total Recipients",     beneficiaries.size() + " beneficiaries", fontBold, fontNormal);
        addMetaRow(metaTable, "Generated On",         timestamp,                  fontBold, fontNormal);
        doc.add(metaTable);

        Map<Integer, String>  clusterToPriority = buildClusterPriorityMap(beneficiaries);
        Map<Integer, Double>  clusterSums   = new HashMap<>();
        Map<Integer, Integer> clusterCounts = new HashMap<>();
        for (BeneficiaryCluster b : beneficiaries) {
            int c = b.getCluster();
            clusterSums.put(c,   clusterSums.getOrDefault(c,   0.0) + b.getFinalScore());
            clusterCounts.put(c, clusterCounts.getOrDefault(c, 0)   + 1);
        }
        Map<Integer, Double> clusterAvg = new HashMap<>();
        for (Integer c : clusterSums.keySet())
            clusterAvg.put(c, clusterSums.get(c) / clusterCounts.get(c));
        List<Integer> sortedClusters = new ArrayList<>(clusterAvg.keySet());
        sortedClusters.sort((a, b) -> Double.compare(clusterAvg.get(b), clusterAvg.get(a)));
        Map<Integer, List<BeneficiaryCluster>> byCluster = new HashMap<>();
        for (BeneficiaryCluster b : beneficiaries)
            byCluster.computeIfAbsent(b.getCluster(), k -> new ArrayList<>()).add(b);

        DeviceRgb[] accentColors = { COLOR_HIGH, COLOR_MODERATE, COLOR_LOW };
        int rank = 0, overallRank = 1;

        for (Integer clusterNum : sortedClusters) {
            String priorityName = clusterToPriority.getOrDefault(clusterNum, "Low Priority");
            List<BeneficiaryCluster> members = byCluster.getOrDefault(clusterNum, new ArrayList<>());
            if (members.isEmpty()) { rank++; continue; }
            members.sort((b1, b2) -> Double.compare(b2.getFinalScore(), b1.getFinalScore()));
            DeviceRgb accentColor = rank < accentColors.length ? accentColors[rank] : COLOR_LOW;
            DeviceRgb lightAccent = lighten(accentColor, 0.88f);

            Paragraph sectionHeading = new Paragraph()
                    .add(new Text("  " + priorityName.toUpperCase() + "  ")
                            .setFont(fontBold).setFontSize(11).setFontColor(COLOR_WHITE))
                    .add(new Text("   " + members.size() + " beneficiaries")
                            .setFont(fontNormal).setFontSize(10).setFontColor(new DeviceRgb(220, 230, 240)))
                    .add(new Text("   Cluster " + clusterNum + "   |   Avg Score: "
                            + String.format("%.3f", clusterAvg.get(clusterNum)))
                            .setFont(fontNormal).setFontSize(9).setFontColor(new DeviceRgb(200, 215, 230)))
                    .setBackgroundColor(accentColor).setPadding(7).setMarginTop(6).setMarginBottom(0);
            doc.add(sectionHeading);

            Table table = new Table(UnitValue.createPercentArray(new float[]{6, 34, 14, 22, 24}))
                    .useAllAvailableWidth().setMarginBottom(10);
            String[] colHeaders = { "#", "Name", "Beneficiary ID", "Priority", "Score" };
            for (String col : colHeaders) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(col).setFont(fontBold).setFontSize(9).setFontColor(COLOR_WHITE))
                        .setBackgroundColor(darken(accentColor, 0.18f)).setPadding(5)
                        .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            }
            boolean alt = false;
            for (BeneficiaryCluster b : members) {
                DeviceRgb rowBg = alt ? lightAccent : COLOR_WHITE;
                alt = !alt;
                String name = nameMap.getOrDefault(b.getBeneficiaryId(), "Unknown");
                addTableRow(table, rowBg, fontNormal, fontMono,
                        String.valueOf(overallRank++), name,
                        String.valueOf(b.getBeneficiaryId()), priorityName,
                        String.format("%.3f", b.getFinalScore()));
            }
            doc.add(table);
            rank++;
        }

        doc.add(new Paragraph(" ").setMarginTop(10).setMarginBottom(0));
        LineSeparator separator = new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f));
        separator.setStrokeColor(new DeviceRgb(190, 200, 210));
        separator.setMarginBottom(8);
        doc.add(separator);

        Paragraph summaryTitle = new Paragraph("Distribution Summary")
                .setFont(fontBold).setFontSize(13).setFontColor(COLOR_HEADER_BG)
                .setMarginBottom(6).setMarginTop(4);
        doc.add(summaryTitle);

        Map<String, List<BeneficiaryCluster>> byPriority = groupBeneficiariesByPriority(beneficiaries);
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{40, 30, 30}))
                .useAllAvailableWidth().setMarginBottom(14);
        for (String col : new String[]{ "Priority Group", "Count", "Avg Score" }) {
            summaryTable.addHeaderCell(new Cell()
                    .add(new Paragraph(col).setFont(fontBold).setFontSize(10).setFontColor(COLOR_WHITE))
                    .setBackgroundColor(COLOR_HEADER_BG).setPadding(6)
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        }

        String[]    priorityNames = { "High Priority", "Moderate Priority", "Low Priority" };
        DeviceRgb[] rowColors     = { COLOR_HIGH, COLOR_MODERATE, COLOR_LOW };
        int activePriorities = 0;
        for (int pIdx = 0; pIdx < priorityNames.length; pIdx++) {
            List<BeneficiaryCluster> group = byPriority.getOrDefault(priorityNames[pIdx], new ArrayList<>());
            if (group.isEmpty()) continue;
            double grpAvg = group.stream().mapToDouble(BeneficiaryCluster::getFinalScore).average().orElse(0.0);
            DeviceRgb light = lighten(rowColors[pIdx], 0.88f);
            summaryTable.addCell(styledSummaryCell(priorityNames[pIdx], fontBold,   10, light, rowColors[pIdx]));
            summaryTable.addCell(styledSummaryCell(String.valueOf(group.size()),     fontNormal, 10, light, rowColors[pIdx]));
            summaryTable.addCell(styledSummaryCell(String.format("%.3f", grpAvg),   fontNormal, 10, light, rowColors[pIdx]));
            activePriorities++;
        }

        double totalAvg = beneficiaries.stream().mapToDouble(BeneficiaryCluster::getFinalScore).average().orElse(0.0);
        DeviceRgb totalBg = new DeviceRgb(220, 230, 240);
        summaryTable.addCell(styledSummaryCell("TOTAL (" + activePriorities + " group"
                + (activePriorities != 1 ? "s" : "") + ")", fontBold, 10, totalBg, COLOR_HEADER_BG));
        summaryTable.addCell(styledSummaryCell(String.valueOf(beneficiaries.size()), fontBold, 10, totalBg, COLOR_HEADER_BG));
        summaryTable.addCell(styledSummaryCell(String.format("%.3f", totalAvg),      fontBold, 10, totalBg, COLOR_HEADER_BG));
        doc.add(summaryTable);

        doc.add(new Paragraph(
                "This report was generated automatically by RespondPH. "
                        + "Beneficiary priority levels are determined by clustering algorithms "
                        + "applied to vulnerability and needs-assessment scores.")
                .setFont(fontNormal).setFontSize(8)
                .setFontColor(new DeviceRgb(120, 130, 140))
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(10));

        int totalPages = pdfDoc.getNumberOfPages();
        for (int i = 1; i <= totalPages; i++) {
            doc.showTextAligned(
                    new Paragraph("Page " + i + " of " + totalPages)
                            .setFont(fontNormal).setFontSize(8)
                            .setFontColor(new DeviceRgb(150, 160, 170)),
                    pdfDoc.getPage(i).getPageSize().getWidth() / 2,
                    22, i, TextAlignment.CENTER, com.itextpdf.layout.properties.VerticalAlignment.BOTTOM, 0);
        }
        doc.close();
    }

    // =========================================================================
    //  PDF HELPER METHODS
    // =========================================================================

    private void addMetaRow(Table table, String label, String value,
                            PdfFont fontBold, PdfFont fontNormal) {
        DeviceRgb labelBg = new DeviceRgb(225, 235, 245);
        DeviceRgb valueBg = new DeviceRgb(245, 248, 252);
        table.addCell(new Cell()
                .add(new Paragraph(label).setFont(fontBold).setFontSize(9)
                        .setFontColor(new DeviceRgb(40, 60, 90)))
                .setBackgroundColor(labelBg).setPadding(6)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        table.addCell(new Cell()
                .add(new Paragraph(value).setFont(fontNormal).setFontSize(9)
                        .setFontColor(new DeviceRgb(40, 60, 90)))
                .setBackgroundColor(valueBg).setPadding(6)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
    }

    private void addTableRow(Table table, DeviceRgb rowBg,
                             PdfFont fontNormal, PdfFont fontMono,
                             String rank, String name, String id,
                             String priority, String score) {
        table.addCell(cellOf(rank,     fontMono,   8, rowBg, TextAlignment.CENTER));
        table.addCell(cellOf(name,     fontNormal, 9, rowBg, TextAlignment.LEFT));
        table.addCell(cellOf(id,       fontMono,   9, rowBg, TextAlignment.CENTER));
        table.addCell(cellOf(priority, fontNormal, 8, rowBg, TextAlignment.CENTER));
        table.addCell(cellOf(score,    fontMono,   9, rowBg, TextAlignment.CENTER));
    }

    private Cell cellOf(String text, PdfFont font, float size,
                        DeviceRgb bg, TextAlignment align) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(size)
                        .setTextAlignment(align).setFontColor(new DeviceRgb(40, 50, 60)))
                .setBackgroundColor(bg)
                .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(5).setPaddingRight(5)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(new DeviceRgb(220, 225, 230), 0.5f));
    }

    private Cell styledSummaryCell(String text, PdfFont font, float size,
                                   DeviceRgb bg, DeviceRgb accentColor) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(size)
                        .setFontColor(new DeviceRgb(30, 50, 70)))
                .setBackgroundColor(bg).setPadding(6)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setBorderLeft(new SolidBorder(accentColor, 3))
                .setBorderBottom(new SolidBorder(new DeviceRgb(210, 218, 226), 0.5f));
    }

    private DeviceRgb lighten(DeviceRgb color, float factor) {
        float[] comps = color.getColorValue();
        return new DeviceRgb(
                clamp(comps[0] + (1f - comps[0]) * factor),
                clamp(comps[1] + (1f - comps[1]) * factor),
                clamp(comps[2] + (1f - comps[2]) * factor));
    }

    private DeviceRgb darken(DeviceRgb color, float factor) {
        float[] comps = color.getColorValue();
        float   scale = 1f - factor;
        return new DeviceRgb(
                clamp(comps[0] * scale),
                clamp(comps[1] * scale),
                clamp(comps[2] * scale));
    }

    private float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    // =========================================================================
    //  PRINT
    // =========================================================================

    private void printBeneficiaryList(List<BeneficiaryCluster> beneficiaries, Printer targetPrinter) {
        try {
            PrinterJob job = (targetPrinter != null)
                    ? PrinterJob.createPrinterJob(targetPrinter)
                    : PrinterJob.createPrinterJob();
            if (job == null) {
                AlertDialogManager.showError("Print Error",
                        "Could not create a print job for the selected printer.\nPlease check that the printer is connected and try again.");
                return;
            }
            if (!job.showPrintDialog(dialogStage)) return;
            if (job.printPage(createPrintContent(beneficiaries))) {
                job.endJob();
                AlertDialogManager.showInfo("Print Success",
                        "Document sent to printer: " + job.getPrinter().getName());
            } else {
                AlertDialogManager.showError("Print Error", "Failed to send document to printer.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Print Error", "Error during printing:\n" + e.getMessage());
        }
    }

    private VBox createPrintContent(List<BeneficiaryCluster> beneficiaries) {
        List<Integer> ids = beneficiaries.stream().map(BeneficiaryCluster::getBeneficiaryId).collect(Collectors.toList());
        Map<Integer, String> nameMap = aidDAO.getBeneficiaryNames(ids);
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");
        Label title = new Label("Aid Distribution — Clustering Preview");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setAlignment(Pos.CENTER);
        content.getChildren().addAll(title, new Label(""),
                new Label("Scope  : " + getDistributionScopeText()),
                new Label("Method : " + (isFCMSelected()
                        ? "Fuzzy C-Means (FCM) — 3 Clusters" : "K-Means — 3 Clusters")),
                new Label("Total  : " + beneficiaries.size() + " beneficiaries"),
                new Label(""));
        Map<String, List<BeneficiaryCluster>> groups = groupBeneficiariesByPriority(beneficiaries);
        for (String priority : List.of("High Priority", "Moderate Priority", "Low Priority")) {
            List<BeneficiaryCluster> group = groups.get(priority);
            if (group != null && !group.isEmpty())
                addPrintSection(content, priority, group, nameMap);
        }
        return content;
    }

    private void addPrintSection(VBox content, String priorityName,
                                 List<BeneficiaryCluster> members,
                                 Map<Integer, String> nameMap) {
        Label header = new Label(priorityName + " (" + members.size() + ")");
        header.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        content.getChildren().add(header);
        members.sort((b1, b2) -> Double.compare(b2.getFinalScore(), b1.getFinalScore()));
        int n = 1;
        for (BeneficiaryCluster b : members) {
            String name = nameMap.getOrDefault(b.getBeneficiaryId(), "Unknown");
            content.getChildren().add(new Label(String.format(
                    "%d. %-30s (ID: %d) | Score: %.3f", n++, name, b.getBeneficiaryId(), b.getFinalScore())));
        }
        content.getChildren().add(new Label(""));
    }

    // =========================================================================
    //  CHANGED: updateSelectionSummary — delegates to updateSelectionSummaryWithCount
    //           using -2 when not ready, -1 when loading
    // =========================================================================
    private void updateSelectionSummary() {
        boolean ready = isPreviewSelectionReady();
        updateSelectionSummaryWithCount(ready ? -1 : -2);
    }
    // =========================================================================

    // =========================================================================
    //  CHANGED: updateSelectionSummaryWithCount
    //    -2  = hide the summary box entirely (selection not ready)
    //    -1  = show spinner (loading)
    //    >=0 = show real count (done)
    // =========================================================================
    private void updateSelectionSummaryWithCount(int eligibleCount) {
        AidTypeModelComboBox  aidType  = aidTypeComboBox.getValue();
        boolean               isGen   = generalAidCheckbox != null && generalAidCheckbox.isSelected();
        DisasterModelComboBox disaster = disasterComboBox.getValue();

        // ─── CHANGED: -2 or missing selection → hide box ─────────────────────
        if (eligibleCount == -2 || aidType == null || (!isGen && disaster == null)) {
            selectionSummaryBox.setVisible(false);
            selectionSummaryBox.setManaged(false);
            removeSummarySpinner();
            infoLabel.setText("Select aid type and choose disaster or general aid option");
            return;
        }
        // ─────────────────────────────────────────────────────────────────────

        selectionSummaryBox.setVisible(true);
        selectionSummaryBox.setManaged(true);

        String barangayInfo = getBarangayInfoText();
        String disasterInfo = isGen ? "General Aid (No Disaster)"
                : "Disaster: " + disaster.getDisasterName();

        if (eligibleCount < 0) {
            // ─── CHANGED: loading state — show spinner next to label ──────────
            selectionSummaryLabel.setText(String.format(
                    "Aid Type: %s | %s%s | Eligible: ",
                    aidType.getAidName(), disasterInfo, barangayInfo));
            showSummarySpinner();
            // ─────────────────────────────────────────────────────────────────
        } else {
            // ─── CHANGED: done — remove spinner, display real count ───────────
            removeSummarySpinner();
            selectionSummaryLabel.setText(String.format(
                    "Aid Type: %s | %s%s | Eligible: %d",
                    aidType.getAidName(), disasterInfo, barangayInfo, eligibleCount));
            // ─────────────────────────────────────────────────────────────────
        }

        infoLabel.setText(String.format("Distributing %s%s%s",
                aidType.getAidName(),
                isGen ? " (General Aid)" : " for " + disaster.getDisasterName() + " disaster",
                barangayInfo));
    }
    // =========================================================================

    // ─── CHANGED: showSummarySpinner — creates and adds ProgressIndicator ────
    private void showSummarySpinner() {
        if (summaryProgressIndicator == null) {
            summaryProgressIndicator = new ProgressIndicator();
            summaryProgressIndicator.setPrefSize(16, 16);
            summaryProgressIndicator.setMaxSize(16, 16);
            summaryProgressIndicator.setMinSize(16, 16);
            summaryProgressIndicator.setStyle("-fx-progress-color: #2980b9;");
        }
        if (!selectionSummaryBox.getChildren().contains(summaryProgressIndicator)) {
            selectionSummaryBox.getChildren().add(summaryProgressIndicator);
            selectionSummaryBox.layout(); // force immediate layout pass
        }
    }
    // ─────────────────────────────────────────────────────────────────────────

    // ─── CHANGED: removeSummarySpinner — removes ProgressIndicator safely ────
    private void removeSummarySpinner() {
        if (summaryProgressIndicator != null) {
            selectionSummaryBox.getChildren().remove(summaryProgressIndicator);
        }
    }
    // ─────────────────────────────────────────────────────────────────────────

    private String getBarangayInfoText() {
        if (!useBarangayFilterCheckbox.isSelected()) return "";
        if (allBarangaysRadio.isSelected()) return " | All Barangays";
        if (singleBarangayRadio.isSelected()) {
            String b = barangayComboBox.getValue();
            return b != null ? " | Barangay: " + b : "";
        }
        return "";
    }

    private List<BeneficiaryCluster> buildClusterOnlyPreview(int aidTypeId, int disasterId) {
        if (aidServiceImpl == null) return new ArrayList<>();
        boolean fcm = isFCMSelected();
        if (!useBarangayFilterCheckbox.isSelected() || allBarangaysRadio.isSelected()) {
            return fcm
                    ? aidServiceImpl.previewClustersOnlyFCM(aidTypeId, disasterId, FIXED_CLUSTERS)
                    : aidServiceImpl.previewClustersOnly(aidTypeId, disasterId, FIXED_CLUSTERS);
        } else {
            String barangay = barangayComboBox.getValue();
            if (barangay == null) return new ArrayList<>();
            return fcm
                    ? aidServiceImpl.previewClustersOnlyFCMByBarangay(aidTypeId, disasterId, FIXED_CLUSTERS, barangay)
                    : aidServiceImpl.previewClustersOnlyByBarangay(aidTypeId, disasterId, FIXED_CLUSTERS, barangay);
        }
    }

    private Map<Integer, String> buildClusterPriorityMap(List<BeneficiaryCluster> beneficiaries) {
        Map<Integer, Double>  sumMap = new HashMap<>();
        Map<Integer, Integer> cntMap = new HashMap<>();
        for (BeneficiaryCluster b : beneficiaries) {
            int c = b.getCluster();
            sumMap.put(c, sumMap.getOrDefault(c, 0.0) + b.getFinalScore());
            cntMap.put(c, cntMap.getOrDefault(c, 0) + 1);
        }
        for (Integer c : sumMap.keySet()) sumMap.put(c, sumMap.get(c) / cntMap.get(c));
        List<Map.Entry<Integer, Double>> sorted = new ArrayList<>(sumMap.entrySet());
        sorted.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
        String[] labels = { "High Priority", "Moderate Priority", "Low Priority" };
        Map<Integer, String> labelMap = new HashMap<>();
        for (int i = 0; i < sorted.size() && i < labels.length; i++)
            labelMap.put(sorted.get(i).getKey(), labels[i]);
        return labelMap;
    }

    private Map<String, List<BeneficiaryCluster>> groupBeneficiariesByPriority(
            List<BeneficiaryCluster> beneficiaries) {
        Map<String, List<BeneficiaryCluster>> groups = new LinkedHashMap<>();
        groups.put("High Priority",     new ArrayList<>());
        groups.put("Moderate Priority", new ArrayList<>());
        groups.put("Low Priority",      new ArrayList<>());
        Map<Integer, String> map = buildClusterPriorityMap(beneficiaries);
        for (BeneficiaryCluster b : beneficiaries)
            groups.get(map.getOrDefault(b.getCluster(), "Low Priority")).add(b);
        return groups;
    }

    private boolean isFCMSelected() { return useFCMRadio != null && useFCMRadio.isSelected(); }

    private boolean isPreviewSelectionReady() {
        if (aidTypeComboBox.getValue() == null) return false;
        boolean isGen = generalAidCheckbox != null && generalAidCheckbox.isSelected();
        if (!isGen && disasterComboBox.getValue() == null) return false;
        if (useBarangayFilterCheckbox.isSelected()
                && singleBarangayRadio.isSelected()
                && barangayComboBox.getValue() == null) return false;
        return true;
    }

    // ─── CHANGED: loadBarangays — wraps setItems/selectFirst with suppressBarangayListener ─
    private void loadBarangays() {
        int aidTypeId = aidTypeComboBox.getValue() != null
                ? aidTypeComboBox.getValue().getAidTypeId() : 0;
        if (aidTypeId == 0) {
            suppressBarangayListener = true;
            barangayComboBox.setItems(FXCollections.observableArrayList());
            suppressBarangayListener = false;
            return;
        }
        List<String> barangays = aidDAO.getBarangaysByDisaster(getSelectedDisasterId(), aidTypeId);
        suppressBarangayListener = true;
        barangayComboBox.setItems(FXCollections.observableArrayList(barangays));
        if (!barangays.isEmpty()) barangayComboBox.getSelectionModel().selectFirst();
        suppressBarangayListener = false;
    }
    // ──────────────────────────────────────────────────────────────────────────

    private int getSelectedDisasterId() {
        if (generalAidCheckbox != null && generalAidCheckbox.isSelected()) return 0;
        DisasterModelComboBox d = disasterComboBox.getValue();
        return d != null ? d.getDisasterId() : 0;
    }

    private String getDistributionScopeText() {
        boolean isGen    = generalAidCheckbox != null && generalAidCheckbox.isSelected();
        String  disaster = isGen ? "General Aid (No Disaster)"
                : "Disaster: " + disasterComboBox.getValue().getDisasterName();
        String  barangay;
        if (!useBarangayFilterCheckbox.isSelected() || allBarangaysRadio.isSelected()) {
            barangay = "All Barangays";
        } else if (singleBarangayRadio.isSelected()) {
            String b = barangayComboBox.getValue();
            barangay = b != null ? "Barangay: " + b : "";
        } else {
            barangay = "";
        }
        return disaster + " | " + barangay;
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (!validateSelection()) return;
        if (!validateInput())     return;
        if (!showConfirmationDialog()) return;

        try {
            String  aidName               = nameFld.getText().trim();
            int     aidTypeId             = aidTypeComboBox.getValue().getAidTypeId();
            int     disasterId            = getSelectedDisasterId();
            int     quantity              = Integer.parseInt(quantityFld.getText().trim());
            int     quantityPerBeneficiary = Integer.parseInt(quantityPerBeneficiaryFld.getText().trim());
            double  costPerUnit           = Double.parseDouble(costFld.getText().trim());
            String  provider              = providerFld.getText().trim();
            boolean fcm                   = isFCMSelected();

            int distributedCount;
            if (fcm) {
                if (!useBarangayFilterCheckbox.isSelected() || allBarangaysRadio.isSelected()) {
                    distributedCount = aidService.distributeAidWithFCM(
                            aidName, aidTypeId, disasterId, quantity,
                            quantityPerBeneficiary, costPerUnit, provider, FIXED_CLUSTERS);
                } else {
                    distributedCount = aidService.distributeAidWithFCMByBarangay(
                            aidName, aidTypeId, disasterId, quantity,
                            quantityPerBeneficiary, costPerUnit, provider, FIXED_CLUSTERS,
                            barangayComboBox.getValue());
                }
            } else {
                if (!useBarangayFilterCheckbox.isSelected() || allBarangaysRadio.isSelected()) {
                    distributedCount = aidService.distributeAidWithKMeans(
                            aidName, aidTypeId, disasterId, quantity,
                            quantityPerBeneficiary, costPerUnit, provider, FIXED_CLUSTERS);
                } else {
                    distributedCount = aidService.distributeAidWithKMeansByBarangay(
                            aidName, aidTypeId, disasterId, quantity,
                            quantityPerBeneficiary, costPerUnit, provider, FIXED_CLUSTERS,
                            barangayComboBox.getValue());
                }
            }

            if (distributedCount > 0) {
                showSuccessDialog(aidName, distributedCount, quantityPerBeneficiary, costPerUnit);
                if (aidController != null) aidController.loadAidData();
                clearFields();
            } else {
                showNoDistributionWarning();
            }

            DashboardRefresher.refresh();
            DashboardRefresher.refreshComboBoxOfDNAndAN();

        } catch (NumberFormatException e) {
            AlertDialogManager.showError("Invalid Input", "Please check quantity and cost are valid numbers.");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Distribution Error", "Error during aid distribution:\n" + e.getMessage());
        }
    }

    // =========================================================================
    //  VALIDATION
    // =========================================================================

    private boolean validateSelection() {
        if (aidTypeComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Selection Required", "Please select an Aid Type.");
            aidTypeComboBox.requestFocus(); return false;
        }
        boolean isGen = generalAidCheckbox != null && generalAidCheckbox.isSelected();
        if (!isGen && disasterComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Selection Required",
                    "Please select a Disaster Event or check 'General Aid'.");
            return false;
        }
        if (useBarangayFilterCheckbox.isSelected()
                && singleBarangayRadio.isSelected()
                && barangayComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Selection Required", "Please select a barangay.");
            return false;
        }
        return true;
    }

    private boolean validateInput() {
        if (nameFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Aid name is required.");
            nameFld.requestFocus(); return false;
        }
        if (quantityFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Quantity is required.");
            quantityFld.requestFocus(); return false;
        }
        try {
            if (Integer.parseInt(quantityFld.getText().trim()) <= 0) {
                AlertDialogManager.showWarning("Validation Error", "Quantity must be > 0.");
                quantityFld.requestFocus(); return false;
            }
        } catch (NumberFormatException e) {
            AlertDialogManager.showWarning("Validation Error", "Enter a valid quantity.");
            quantityFld.requestFocus(); return false;
        }
        if (costFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Cost is required.");
            costFld.requestFocus(); return false;
        }
        try {
            if (Double.parseDouble(costFld.getText().trim()) < 0) {
                AlertDialogManager.showWarning("Validation Error", "Cost cannot be negative.");
                costFld.requestFocus(); return false;
            }
        } catch (NumberFormatException e) {
            AlertDialogManager.showWarning("Validation Error", "Enter a valid cost.");
            costFld.requestFocus(); return false;
        }
        if (providerFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Provider is required.");
            providerFld.requestFocus(); return false;
        }
        return true;
    }

    private boolean showConfirmationDialog() {
        int    qty  = Integer.parseInt(quantityFld.getText().trim());
        double cost = Double.parseDouble(costFld.getText().trim());
        Alert  alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Distribution");
        alert.setHeaderText("Distribute Aid to Beneficiaries");
        alert.setContentText(String.format(
                "Aid      : %s\nAid Type : %s\nScope    : %s\n"
                        + "Quantity : %s units\nUnit Cost: ₱%s\nTotal    : ₱%.2f\n"
                        + "Provider : %s\nMethod   : %s (3 Clusters)",
                nameFld.getText().trim(),
                aidTypeComboBox.getValue().getAidName(),
                getDistributionScopeText(),
                quantityFld.getText().trim(), costFld.getText().trim(), qty * cost,
                providerFld.getText().trim(),
                isFCMSelected() ? "Fuzzy C-Means (FCM)" : "K-Means"));
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showSuccessDialog(String aidName, int count, int qtyPerBeneficiary, double costPerUnit) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Distribution Successful");
        alert.setHeaderText("✓ Aid Distribution Complete");
        alert.setContentText(String.format(
                "Aid Type             : %s\nScope                : %s\n"
                        + "Beneficiaries Served : %d\nTotal Quantity       : %d units\n"
                        + "Total Cost           : ₱%.2f\nMethod               : %s",
                aidName, getDistributionScopeText(), count, count * qtyPerBeneficiary,
                (double) count * qtyPerBeneficiary * costPerUnit,
                isFCMSelected() ? "Fuzzy C-Means (FCM) (3 Clusters)" : "K-Means (3 Clusters)"));
        alert.showAndWait();
    }

    private void showNoDistributionWarning() {
        AlertDialogManager.showWarning("No Distribution",
                "No beneficiaries were eligible for this aid distribution.\n\n"
                        + "Please check:\n"
                        + "• Beneficiaries have been scored for this aid type\n"
                        + "• Selected barangay has eligible beneficiaries");
    }

    @FXML private void openKMeansResearchLink(ActionEvent event) {
        openURLInBrowser("https://link.springer.com/chapter/10.1007/978-3-642-30157-5_45");
    }

    @FXML private void openFCMResearchLink(ActionEvent event) {
        openURLInBrowser("https://journal.magisz.org/index.php/jai/article/view/196");
    }

    private void openURLInBrowser(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(new java.net.URI(url));
                } else { showBrowserUnavailableWarning(url); }
            } else { showBrowserUnavailableWarning(url); }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Error Opening Link",
                    "Could not open the research paper link.\n\nPlease visit manually:\n" + url);
        }
    }

    private void showBrowserUnavailableWarning(String url) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Browser Unavailable");
        alert.setHeaderText("Cannot Open Browser Automatically");
        alert.setContentText("Please copy and paste this URL into your browser:\n\n" + url);
        TextArea ta = new TextArea(url);
        ta.setEditable(false); ta.setWrapText(true); ta.setPrefRowCount(2);
        alert.getDialogPane().setExpandableContent(ta);
        alert.getDialogPane().setExpanded(true);
        alert.showAndWait();
    }

    @FXML private void handleClose(ActionEvent event) { closeDialog(); }

    private void clearFields() {
        nameFld.clear(); quantityFld.clear(); quantityPerBeneficiaryFld.clear();
        costFld.clear(); providerFld.clear();
        aidTypeComboBox.setValue(null);
        disasterComboBox.setValue(null);
        barangayComboBox.setValue(null);
        useKMeansRadio.setSelected(true);
        useBarangayFilterCheckbox.setSelected(false);
        generalAidCheckbox.setSelected(false);
        allBarangaysRadio.setSelected(true);
        if (kmeansInfoBox != null) { kmeansInfoBox.setVisible(true);  kmeansInfoBox.setManaged(true);  }
        if (fcmInfoBox    != null) { fcmInfoBox.setVisible(false);    fcmInfoBox.setManaged(false);    }
        lastPreviewResult = new ArrayList<>();
        previewLoading    = false;
        // ─── CHANGED: stop debounce on clear so no leftover task fires ────────
        if (clusterDebounce != null) clusterDebounce.stop();
        // ─────────────────────────────────────────────────────────────────────
        hideInlinePanel();
        updatePreviewButtonState();
        updateSelectionSummary();
    }

    private void closeDialog() {
        if (dialogStage != null) dialogStage.close();
        else ((Stage) closeBtn.getScene().getWindow()).close();
    }

    private void makeDraggable() {
        final double[] xOff = {0}, yOff = {0};
        root.setOnMousePressed(e  -> { xOff[0] = e.getSceneX(); yOff[0] = e.getSceneY(); });
        root.setOnMouseDragged(e  -> {
            if (dialogStage != null) {
                dialogStage.setX(e.getScreenX() - xOff[0]);
                dialogStage.setY(e.getScreenY() - yOff[0]);
            }
        });
    }

    private void showProgressAndOpenPrintDialog() {
        createProgressDialog("Preparing print dialog...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0, 100);
                updateMessage("Grouping beneficiaries by priority...");
                Map<String, List<BeneficiaryCluster>> groups = groupBeneficiariesByPriority(lastPreviewResult);
                updateProgress(30, 100);
                updateMessage("Detecting connected printers...");
                Thread.sleep(200);
                ObservableList<PrinterEntry> allEntries = buildPrinterEntries();
                ObservableList<PrinterEntry> activeEntries = FXCollections.observableArrayList(
                        allEntries.filtered(PrinterEntry::isActive));
                updateProgress(60, 100);
                updateMessage("Preparing dialog components...");
                Thread.sleep(200);
                updateProgress(85, 100);
                updateMessage("Ready to open dialog...");
                Thread.sleep(150);
                updateProgress(100, 100);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            closeProgressDialog();
            showPrintCustomDialog(lastPreviewResult);
        });

        task.setOnFailed(e -> {
            closeProgressDialog();
            Throwable exception = task.getException();
            AlertDialogManager.showError("Error",
                    "Failed to prepare print dialog: " +
                            (exception != null ? exception.getMessage() : "Unknown error"));
            exception.printStackTrace();
        });

        progressBar.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void createProgressDialog(String initialMessage) {
        progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.initStyle(StageStyle.UNDECORATED);
        progressStage.setAlwaysOnTop(true);

        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 1; -fx-background-radius: 5; -fx-border-radius: 5;");

        Label titleLabel = new Label("Please Wait");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");

        progressLabel = new Label(initialMessage);
        progressLabel.setFont(Font.font("Arial", 12));
        progressLabel.setStyle("-fx-text-fill: #555;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        progressBar.setPrefHeight(20);

        vbox.getChildren().addAll(titleLabel, progressLabel, progressBar);

        Scene scene = new Scene(vbox);
        progressStage.setScene(scene);
        progressStage.show();
    }

    private void closeProgressDialog() {
        if (progressStage != null) {
            Platform.runLater(() -> {
                progressStage.close();
                progressStage = null;
            });
        }
    }
}