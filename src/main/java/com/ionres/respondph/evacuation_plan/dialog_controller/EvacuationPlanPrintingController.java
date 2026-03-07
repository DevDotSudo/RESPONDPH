package com.ionres.respondph.evacuation_plan.dialog_controller;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.evacuation_plan.EvacuationPlanModel;
import com.ionres.respondph.evacuation_plan.EvacuationPlanService;
import com.ionres.respondph.evacuation_plan.EvacuationPlanServiceImpl;
import com.ionres.respondph.evac_site.EvacSiteDAO;
import com.ionres.respondph.evac_site.EvacSiteDAOServiceImpl;
import com.ionres.respondph.evac_site.EvacSiteModel;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.PdfProgressRunner;
import com.ionres.respondph.util.SessionManager;
import com.ionres.respondph.util.ThemeManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class EvacuationPlanPrintingController {

    // ── Page Layout Constants ───────────────────────────────────────────────
    private static final double PAGE_WIDTH    = 650;
    private static final double MARGIN        = 36;
    private static final double CONTENT_WIDTH = PAGE_WIDTH - (2 * MARGIN); // 578

    // ── PDF color palette ─────────────────────────────────────────────────────
    private static final DeviceRgb PDF_HEADER_BG = new DeviceRgb(44,  62,  80);
    private static final DeviceRgb PDF_WHITE     = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb PDF_BLUE      = new DeviceRgb(41,  128, 185);
    private static final DeviceRgb PDF_GREEN     = new DeviceRgb(39,  174,  96);
    private static final DeviceRgb PDF_RED       = new DeviceRgb(192,  57,  43);
    private static final DeviceRgb PDF_ORANGE    = new DeviceRgb(230, 126,  34);

    // ── FXML fields ──────────────────────────────────────────────────────────
    @FXML private ComboBox<String> disasterComboBox;
    @FXML private ComboBox<String> evacuationSiteComboBox;
    @FXML private HBox             capacitySummary;
    @FXML private Label            capacityLabel;
    @FXML private RadioButton      beneficiaryListRadio;
    @FXML private RadioButton      evacuationPlanRadio;
    @FXML private RadioButton      capacityReportRadio;
    @FXML private ComboBox<String> bondPaperSizeComboBox;
    @FXML private RadioButton      portraitRadio;
    @FXML private RadioButton      landscapeRadio;
    @FXML private Spinner<Integer> copiesSpinner;
    @FXML private CheckBox         includeHeaderCheckbox;
    @FXML private CheckBox         includeFooterCheckbox;
    @FXML private CheckBox         includePageNumbersCheckbox;
    @FXML private Button           previewBtn;
    @FXML private Button           cancelBtn;
    @FXML private Button           printBtn;
    @FXML private Button           closeBtn;

    // ── Progress dialog ───────────────────────────────────────────────────────
    private Stage       progressStage;
    private ProgressBar progressBar;
    private Label       progressLabel;

    private Stage dialogStage;

    private final EvacuationPlanService service =
            new EvacuationPlanServiceImpl(DBConnection.getInstance());
    private final EvacSiteDAO evacSiteDAO =
            new EvacSiteDAOServiceImpl(DBConnection.getInstance());

    private List<EvacuationPlanModel> allPlans = new ArrayList<>();

    private final Map<String, Integer> disasterNameToId   = new LinkedHashMap<>();
    private final Map<String, Integer> evacSiteNameToId   = new LinkedHashMap<>();
    private final Map<String, Integer> evacSiteCapacities = new LinkedHashMap<>();

    private final ToggleGroup reportTypeGroup  = new ToggleGroup();
    private final ToggleGroup orientationGroup = new ToggleGroup();

    // ── Inner data class ──────────────────────────────────────────────────────
    private static class PrintDialogData {
        List<EvacuationPlanModel> filteredPlans;
        String disasterName;
        String siteName;
        int disasterId;
        int siteId;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    // =========================================================================
    // INIT
    // =========================================================================

    @FXML
    public void initialize() {
        wireToggleGroups();
        wirePaperSizeDefault();
        wireDisasterComboListener();
        wireEvacSiteComboListener();
        wireButtonHandlers();
        loadData();
    }

    private void wireToggleGroups() {
        beneficiaryListRadio.setToggleGroup(reportTypeGroup);
        evacuationPlanRadio .setToggleGroup(reportTypeGroup);
        capacityReportRadio .setToggleGroup(reportTypeGroup);
        portraitRadio .setToggleGroup(orientationGroup);
        landscapeRadio.setToggleGroup(orientationGroup);
    }

    private void wirePaperSizeDefault() {
        if (bondPaperSizeComboBox.getItems().isEmpty()) {
            bondPaperSizeComboBox.setItems(FXCollections.observableArrayList(
                    "A4 (210mm x 297mm)",
                    "Letter (8.5\" x 11\")",
                    "Legal (8.5\" x 14\")"
            ));
        }
        bondPaperSizeComboBox.getSelectionModel().selectFirst();
    }

    private void wireDisasterComboListener() {
        disasterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) populateEvacSiteComboForDisaster(newVal);
            else { evacuationSiteComboBox.getItems().clear(); hideCapacitySummary(); }
        });
    }

    private void wireEvacSiteComboListener() {
        evacuationSiteComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) updateCapacitySummary(newVal);
            else                hideCapacitySummary();
        });
    }

    private void wireButtonHandlers() {
        closeBtn  .setOnAction(e -> closeDialog());
        cancelBtn .setOnAction(e -> closeDialog());
        previewBtn.setOnAction(e -> handlePreview());
        printBtn  .setOnAction(e -> showProgressAndOpenDialog());
    }

    // =========================================================================
    // DATA LOADING
    // =========================================================================

    private void loadData() {
        Task<List<EvacuationPlanModel>> task = new Task<>() {
            @Override protected List<EvacuationPlanModel> call() {
                return service.getAllEvacuationPlans();
            }
            @Override protected void succeeded() {
                allPlans = getValue();
                populateDisasterCombo();
            }
            @Override protected void failed() {
                AlertDialogManager.showError("Load Error",
                        "Failed to load evacuation data: " + getException().getMessage());
            }
        };
        new Thread(task).start();
    }

    // =========================================================================
    // PROGRESS + BACKGROUND TASK
    // =========================================================================

    private void showProgressAndOpenDialog() {
        if (!validate()) return;

        String  disasterName = disasterComboBox.getValue();
        String  siteName     = evacuationSiteComboBox.getValue();
        Integer disasterId   = disasterNameToId.get(disasterName);
        Integer siteId       = evacSiteNameToId.get(siteName);

        if (disasterId == null || siteId == null) {
            AlertDialogManager.showWarning("No Selection", "Please select a disaster and site.");
            return;
        }

        createProgressDialog("Preparing data for printing/export...");

        Task<PrintDialogData> task = new Task<>() {
            @Override
            protected PrintDialogData call() throws Exception {
                PrintDialogData dialogData = new PrintDialogData();

                updateProgress(0, 100);
                updateMessage("Filtering evacuation plans...");

                List<EvacuationPlanModel> filtered = allPlans.stream()
                        .filter(p -> disasterId.equals(p.getDisasterId())
                                && siteId.equals(p.getEvacSiteId()))
                        .sorted(Comparator.comparing(EvacuationPlanModel::getBeneficiaryName))
                        .collect(Collectors.toList());

                if (filtered.isEmpty()) {
                    throw new Exception("No records found for the selected disaster and evacuation site.");
                }

                dialogData.filteredPlans = filtered;
                dialogData.disasterName  = disasterName;
                dialogData.siteName      = siteName;
                dialogData.disasterId    = disasterId;
                dialogData.siteId        = siteId;

                updateProgress(40, 100);
                updateMessage("Detecting connected printers...");
                Thread.sleep(300);

                buildPrinterEntries().filtered(PrinterEntry::isActive);

                updateProgress(70, 100);
                updateMessage("Preparing dialog components...");
                Thread.sleep(300);
                updateProgress(90, 100);
                updateMessage("Ready to open dialog...");
                Thread.sleep(200);
                updateProgress(100, 100);

                return dialogData;
            }
        };
        ExecutorService executor = Executors.newSingleThreadExecutor();


        task.setOnSucceeded(e -> {
            closeProgressDialog();
            PrintDialogData data = task.getValue();
            showOutputFormatDialog(
                    data.filteredPlans, data.disasterName,
                    data.siteName, data.disasterId, data.siteId);
            shutDownExecutor(executor);

        });

        task.setOnFailed(e -> {
            closeProgressDialog();
            Throwable ex = task.getException();
            if (ex != null && ex.getMessage() != null) {
                if (ex.getMessage().contains("No records found")) {
                    AlertDialogManager.showWarning("No Data", ex.getMessage());
                } else {
                    AlertDialogManager.showError("Error", "Failed to load data: " + ex.getMessage());
                }
            } else {
                AlertDialogManager.showError("Error", "An unknown error occurred.");
            }
            if (ex != null) ex.printStackTrace();
            shutDownExecutor(executor);

        });

        progressBar.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());


        executor.submit(task);

    }

    private void shutDownExecutor(ExecutorService executor){
        try {
            System.out.println("attempt to shutdown executor");
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }catch (InterruptedException e) {
            System.err.println("tasks interrupted");
        }finally {
            if (!executor.isTerminated()) {
                System.err.println("cancel non-finished tasks");
            }
            executor.shutdownNow();
            System.out.println("shutdown finished");
        }
    }

    public void setEvacuationPlans(ObservableList<EvacuationPlanModel> plans) {
        this.allPlans = new ArrayList<>(plans);

        // ✅ Also rebuild the lookup maps, not just the combo items
        Platform.runLater(() -> populateDisasterCombo());
    }

    private void createProgressDialog(String initialMessage) {
        progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.initStyle(StageStyle.UNDECORATED);
        progressStage.setAlwaysOnTop(true);

        boolean light = ThemeManager.getInstance().isLightMode();

        VBox card = new VBox(0);
        card.setPrefWidth(420);
        card.setStyle(
                "-fx-background-color: " + (light ? "#EDE8DF" : "#0b1220") + ";" +
                        "-fx-border-color: " + (light ? "rgba(176,200,178,0.80)" : "rgba(148,163,184,0.22)") + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0," + (light ? "0.18" : "0.45") + "), 28, 0.0, 0, 6);"
        );

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 22, 18, 22));
        header.setStyle(
                "-fx-background-color: " + (light ? "#5C8A79" : "rgba(255,255,255,0.025)") + ";" +
                        "-fx-border-color: " + (light ? "rgba(90,130,115,0.45)" : "rgba(148,163,184,0.12)") + ";" +
                        "-fx-border-width: 0 0 1 0;" +
                        "-fx-background-radius: 10 10 0 0;"
        );

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(22, 22);
        spinner.setMaxSize(22, 22);
        spinner.setMinSize(22, 22);
        spinner.setStyle("-fx-progress-color: " + (light ? "#B85507" : "rgba(249,115,22,0.95)") + ";");

        VBox titleBlock = new VBox(3);
        Label titleLabel = new Label("Please Wait");
        titleLabel.setFont(Font.font("Inter", FontWeight.BLACK, 16));
        titleLabel.setStyle(
                "-fx-text-fill: " + (light ? "#FFFFFF" : "rgba(248,250,252,0.98)") + ";" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: 900;"
        );
        Label subtitleLabel = new Label("Processing your request…");
        subtitleLabel.setStyle(
                "-fx-text-fill: " + (light ? "rgba(255,255,255,0.75)" : "rgba(148,163,184,0.80)") + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: 600;"
        );
        titleBlock.getChildren().addAll(titleLabel, subtitleLabel);
        header.getChildren().addAll(spinner, titleBlock);

        VBox body = new VBox(14);
        body.setPadding(new Insets(22, 22, 24, 22));
        body.setAlignment(Pos.CENTER_LEFT);
        body.setStyle("-fx-background-color: transparent;");

        progressLabel = new Label(initialMessage);
        progressLabel.setWrapText(true);
        progressLabel.setMaxWidth(Double.MAX_VALUE);
        progressLabel.setStyle(
                "-fx-text-fill: " + (light ? "#1A1A1A" : "rgba(226,232,240,0.85)") + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 600;"
        );

        VBox barWrapper = new VBox(0);
        barWrapper.setStyle(
                "-fx-background-color: " + (light ? "rgba(176,200,178,0.35)" : "rgba(255,255,255,0.06)") + ";" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: " + (light ? "rgba(176,200,178,0.70)" : "rgba(148,163,184,0.14)") + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-padding: 0;"
        );

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle(
                "-fx-accent: " + (light ? "#B85507" : "rgba(249,115,22,0.95)") + ";" +
                        "-fx-background-color: transparent;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-radius: 6;"
        );
        barWrapper.getChildren().add(progressBar);

        Label pctLabel = new Label("0%");
        pctLabel.setStyle(
                "-fx-text-fill: " + (light ? "#4A7566" : "rgba(148,163,184,0.70)") + ";" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: 700;"
        );
        HBox pctRow = new HBox();
        Region pctSpacer = new Region();
        HBox.setHgrow(pctSpacer, Priority.ALWAYS);
        pctRow.getChildren().addAll(pctSpacer, pctLabel);

        progressBar.progressProperty().addListener((obs, oldVal, newVal) -> {
            double pct = newVal.doubleValue();
            pctLabel.setText(pct < 0 ? "…" : String.format("%.0f%%", pct * 100));
        });

        body.getChildren().addAll(progressLabel, barWrapper, pctRow);
        card.getChildren().addAll(header, body);

        Scene scene = new Scene(card);
        scene.setFill(null);
        progressStage.setScene(scene);

        if (dialogStage != null) {
            progressStage.initOwner(dialogStage);
            progressStage.setX(dialogStage.getX() + (dialogStage.getWidth()  - 420) / 2);
            progressStage.setY(dialogStage.getY() + (dialogStage.getHeight() - 160) / 2);
        }

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

    // =========================================================================
    // OUTPUT FORMAT DIALOG  (dark-card design)
    // =========================================================================

    private void showOutputFormatDialog(List<EvacuationPlanModel> filtered,
                                        String disasterName, String siteName,
                                        int disasterId, int siteId) {
        if (filtered.isEmpty()) {
            AlertDialogManager.showWarning("No Data",
                    "No records found for the selected disaster and evacuation site.");
            return;
        }

        // ── Stage ────────────────────────────────────────────────────────────
        Stage outputStage = new Stage();
        outputStage.initModality(Modality.APPLICATION_MODAL);
        outputStage.initStyle(StageStyle.UNDECORATED);
        if (dialogStage != null) outputStage.initOwner(dialogStage);

        boolean light = ThemeManager.getInstance().isLightMode();

        // ── Root card ────────────────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setPrefWidth(500);
        card.setStyle(
                "-fx-background-color: " + (light ? "#EDE8DF" : "#0b1220") + ";" +
                        "-fx-border-color: " + (light ? "rgba(176,200,178,0.80)" : "rgba(148,163,184,0.22)") + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0," + (light ? "0.18" : "0.45") + "), 28, 0.0, 0, 6);"
        );

        // ── HEADER ───────────────────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 22, 18, 22));
        header.setStyle(
                "-fx-background-color: " + (light ? "#5C8A79" : "rgba(255,255,255,0.025)") + ";" +
                        "-fx-border-color: " + (light ? "rgba(90,130,115,0.45)" : "rgba(148,163,184,0.12)") + ";" +
                        "-fx-border-width: 0 0 1 0;" +
                        "-fx-background-radius: 10 10 0 0;"
        );

        FontAwesomeIconView headerIcon = new FontAwesomeIconView(FontAwesomeIcon.SHARE_SQUARE_ALT);
        headerIcon.setSize("20");
        headerIcon.setGlyphStyle("-fx-fill: " + (light ? "rgba(255,255,255,0.92)" : "rgba(249,115,22,0.95)") + ";");

        VBox titleBlock = new VBox(3);
        Label titleLabel = new Label("Export / Print");
        titleLabel.setStyle(
                "-fx-text-fill: " + (light ? "#FFFFFF" : "rgba(248,250,252,0.98)") + ";" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: 900;"
        );
        Label subtitleLabel = new Label("Choose how to output the evacuation report");
        subtitleLabel.setStyle(
                "-fx-text-fill: " + (light ? "rgba(255,255,255,0.75)" : "rgba(148,163,184,0.80)") + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: 600;"
        );
        titleBlock.getChildren().addAll(titleLabel, subtitleLabel);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button headerCloseBtn = new Button();
        FontAwesomeIconView timesIcon = new FontAwesomeIconView(FontAwesomeIcon.TIMES);
        timesIcon.setSize("13");
        timesIcon.setGlyphStyle("-fx-fill: " + (light ? "rgba(255,255,255,0.90)" : "rgba(248,250,252,0.95)") + ";");
        headerCloseBtn.setGraphic(timesIcon);
        headerCloseBtn.setStyle(
                "-fx-background-color: " + (light ? "rgba(255,255,255,0.15)" : "rgba(255,255,255,0.03)") + ";" +
                        "-fx-border-color: " + (light ? "rgba(255,255,255,0.25)" : "rgba(148,163,184,0.20)") + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-radius: 6;" +
                        "-fx-padding: 8 12 8 12;" +
                        "-fx-cursor: hand;"
        );

        header.getChildren().addAll(headerIcon, titleBlock, headerSpacer, headerCloseBtn);

        // ── BODY ─────────────────────────────────────────────────────────────
        VBox body = new VBox(20);
        body.setPadding(new Insets(22, 22, 24, 22));
        body.setStyle("-fx-background-color: transparent;");

        // ── SECTION: Output Format ────────────────────────────────────────────
        VBox outputSection = buildSection(
                FontAwesomeIcon.SHARE_SQUARE_ALT, "Output Format",
                "Choose how to export the evacuation report", light
        );

        ToggleGroup outputGroup  = new ToggleGroup();
        RadioButton pdfRadio     = buildRadioCard("Save as PDF",     FontAwesomeIcon.FILE_PDF_ALT, outputGroup, light);
        RadioButton printerRadio = buildRadioCard("Send to Printer", FontAwesomeIcon.PRINT,        outputGroup, light);
        pdfRadio.setSelected(true);

        HBox radioRow = new HBox(10);
        radioRow.getChildren().addAll(pdfRadio, printerRadio);
        outputSection.getChildren().add(radioRow);

        // ── SECTION: Printer (hidden by default) ──────────────────────────────
        VBox printerSection = new VBox(10);
        printerSection.setVisible(false);
        printerSection.setManaged(false);

        VBox printerCard = buildSection(
                FontAwesomeIcon.DESKTOP, "Select Printer",
                "Only connected printers are shown", light
        );

        ObservableList<PrinterEntry> allEntries    = buildPrinterEntries();
        ObservableList<PrinterEntry> activeEntries = FXCollections.observableArrayList(
                allEntries.filtered(PrinterEntry::isActive));

        ComboBox<PrinterEntry> printerComboBox = new ComboBox<>(activeEntries);
        printerComboBox.setMaxWidth(Double.MAX_VALUE);
        printerComboBox.setStyle(
                "-fx-background-color: " + (light ? "#E8E2D8" : "rgba(255,255,255,0.04)") + ";" +
                        "-fx-border-color: " + (light ? "rgba(176,200,178,0.75)" : "rgba(148,163,184,0.20)") + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-radius: 6;" +
                        "-fx-padding: 4 10 4 10;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 700;"
        );

        String cellBg    = light ? "#F0EAE0" : "#0b1220";
        String cellBdr   = light ? "rgba(176,200,178,0.65)" : "rgba(148,163,184,0.22)";
        String cellText  = light ? "#1A1A1A" : "rgba(226,232,240,0.95)";
        String cellHover = light ? "rgba(193,216,195,0.35)" : "rgba(249,115,22,0.10)";
        String cellHoverText = light ? "#B85507" : "rgba(249,115,22,0.95)";

        Callback<ListView<PrinterEntry>, ListCell<PrinterEntry>> printerCellFactory =
                lv -> {
                    if (lv != null) {
                        lv.setStyle(
                                "-fx-background-color: " + cellBg + ";" +
                                        "-fx-border-color: " + cellBdr + ";" +
                                        "-fx-border-width: 1;" +
                                        "-fx-border-radius: 6;" +
                                        "-fx-background-radius: 6;"
                        );
                    }
                    return new ListCell<>() {
                        @Override
                        protected void updateItem(PrinterEntry item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                                setGraphic(null);
                                setStyle("-fx-background-color: transparent;");
                            } else {
                                setText(item.getPrinterName());
                                setStyle(
                                        "-fx-font-size: 13px;" +
                                                "-fx-font-weight: 700;" +
                                                "-fx-text-fill: " + cellText + ";" +
                                                "-fx-background-color: transparent;" +
                                                "-fx-padding: 10 12 10 12;"
                                );
                                setOnMouseEntered(e -> setStyle(
                                        "-fx-font-size: 13px;" +
                                                "-fx-font-weight: 700;" +
                                                "-fx-text-fill: " + cellHoverText + ";" +
                                                "-fx-background-color: " + cellHover + ";" +
                                                "-fx-padding: 10 12 10 12;"
                                ));
                                setOnMouseExited(e -> setStyle(
                                        "-fx-font-size: 13px;" +
                                                "-fx-font-weight: 700;" +
                                                "-fx-text-fill: " + cellText + ";" +
                                                "-fx-background-color: transparent;" +
                                                "-fx-padding: 10 12 10 12;"
                                ));
                            }
                        }
                    };
                };
        printerComboBox.setCellFactory(printerCellFactory);
        printerComboBox.setButtonCell(printerCellFactory.call(null));

        activeEntries.stream()
                .filter(PrinterEntry::isDefault).findFirst()
                .or(() -> activeEntries.isEmpty()
                        ? Optional.empty() : Optional.of(activeEntries.get(0)))
                .ifPresent(printerComboBox::setValue);

        printerCard.getChildren().add(printerComboBox);
        printerSection.getChildren().add(printerCard);

        // ── Declare generateBtn BEFORE the toggle listener ────────────────────
        Button cancelBtn   = buildFooterButton("Cancel",   FontAwesomeIcon.TIMES,    false, light);
        Button generateBtn = buildFooterButton("Generate", FontAwesomeIcon.DOWNLOAD, true,  light);

        // ── Toggle: show/hide printer section ────────────────────────────────
        outputGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == printerRadio) {
                if (activeEntries.isEmpty()) {
                    pdfRadio.setSelected(true);
                    AlertDialogManager.showError(
                            "No Printer Connected",
                            "No connected printer was detected on this system.\n\n" +
                                    "Please connect a printer and try again.");
                } else {
                    printerSection.setVisible(true);
                    printerSection.setManaged(true);
                    outputStage.sizeToScene();

                    FontAwesomeIconView printIcon = new FontAwesomeIconView(FontAwesomeIcon.PRINT);
                    printIcon.setSize("13");
                    printIcon.setGlyphStyle("-fx-fill: rgba(255,255,255,0.95);");
                    generateBtn.setText("Print");
                    generateBtn.setGraphic(printIcon);
                }
            } else {
                printerSection.setVisible(false);
                printerSection.setManaged(false);
                outputStage.sizeToScene();

                FontAwesomeIconView downloadIcon = new FontAwesomeIconView(FontAwesomeIcon.DOWNLOAD);
                downloadIcon.setSize("13");
                downloadIcon.setGlyphStyle("-fx-fill: rgba(255,255,255,0.95);");
                generateBtn.setText("Generate");
                generateBtn.setGraphic(downloadIcon);
            }
        });

        body.getChildren().addAll(outputSection, printerSection);

        // ── FOOTER ───────────────────────────────────────────────────────────
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 22, 18, 22));
        footer.setStyle(
                "-fx-background-color: " + (light ? "rgba(240,232,220,0.70)" : "rgba(255,255,255,0.02)") + ";" +
                        "-fx-border-color: " + (light ? "rgba(193,216,195,0.55)" : "rgba(148,163,184,0.12)") + ";" +
                        "-fx-border-width: 1 0 0 0;" +
                        "-fx-background-radius: 0 0 10 10;"
        );
        footer.getChildren().addAll(cancelBtn, generateBtn);

        card.getChildren().addAll(header, body, footer);

        // ── Wire actions ──────────────────────────────────────────────────────
        headerCloseBtn.setOnAction(e -> outputStage.close());
        cancelBtn.setOnAction(e -> outputStage.close());

        generateBtn.setOnAction(e -> {
            outputStage.close();
            if (pdfRadio.isSelected()) {
                generateAndSavePDF(filtered, disasterName, siteName);
            } else {
                PrinterEntry chosen = printerComboBox.getValue();
                if (chosen == null) {
                    AlertDialogManager.showWarning("No Printer Selected",
                            "Please select a printer from the list.");
                    return;
                }
                sendToPrinter(filtered, chosen.getPrinter());
            }
        });

        Scene scene = new Scene(card);
        scene.setFill(null);
        outputStage.setScene(scene);

        // Center on owner
        if (dialogStage != null) {
            outputStage.setX(dialogStage.getX() + (dialogStage.getWidth()  - 500) / 2);
            outputStage.setY(dialogStage.getY() + (dialogStage.getHeight() - 400) / 2);
        } else {
            outputStage.setOnShown(e -> {
                javafx.geometry.Rectangle2D screen =
                        javafx.stage.Screen.getPrimary().getVisualBounds();
                outputStage.setX((screen.getWidth()  - card.getWidth())  / 2);
                outputStage.setY((screen.getHeight() - card.getHeight()) / 2);
            });
        }

        outputStage.show();
    }

    // =========================================================================
    // SHARED DARK-CARD HELPERS
    // =========================================================================

    private VBox buildSection(FontAwesomeIcon icon, String title, String subtitle, boolean light) {
        VBox section = new VBox(10);
        section.setStyle(
                "-fx-background-color: " + (light ? "rgba(232,224,212,0.65)" : "rgba(255,255,255,0.03)") + ";" +
                        "-fx-border-color: " + (light ? "rgba(176,200,178,0.65)" : "rgba(148,163,184,0.14)") + ";" +
                        "-fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 14 16 14 16;"
        );
        HBox sh = new HBox(10);
        sh.setAlignment(Pos.CENTER_LEFT);
        FontAwesomeIconView si = new FontAwesomeIconView(icon);
        si.setSize("14");
        si.setGlyphStyle("-fx-fill: " + (light ? "#B85507" : "rgba(249,115,22,0.90)") + ";");
        VBox stb = new VBox(2);
        Label st = new Label(title);
        st.setStyle("-fx-text-fill: " + (light ? "#1A1A1A" : "rgba(248,250,252,0.95)") + "; -fx-font-size: 13px; -fx-font-weight: 800;");
        Label ss = new Label(subtitle);
        ss.setStyle("-fx-text-fill: " + (light ? "#4A7566" : "rgba(148,163,184,0.70)") + "; -fx-font-size: 11px; -fx-font-weight: 600;");
        stb.getChildren().addAll(st, ss);
        sh.getChildren().addAll(si, stb);
        section.getChildren().add(sh);
        return section;
    }

    private RadioButton buildRadioCard(String label, FontAwesomeIcon icon, ToggleGroup group, boolean light) {
        RadioButton radio = new RadioButton(label);
        radio.setToggleGroup(group);
        FontAwesomeIconView radioIcon = new FontAwesomeIconView(icon);
        radioIcon.setSize("13");
        radioIcon.setGlyphStyle("-fx-fill: " + (light ? "#4A7566" : "rgba(226,232,240,0.80)") + ";");
        radio.setGraphic(radioIcon);
        String base = "-fx-background-color: " + (light ? "#E8E2D8" : "rgba(255,255,255,0.04)") + "; -fx-border-color: " + (light ? "rgba(176,200,178,0.75)" : "rgba(148,163,184,0.20)") + "; -fx-border-width: 1; -fx-background-radius: 7; -fx-border-radius: 7; -fx-padding: 10 16 10 16; -fx-text-fill: " + (light ? "#1A1A1A" : "rgba(226,232,240,0.90)") + "; -fx-font-size: 12px; -fx-font-weight: 700; -fx-cursor: hand;";
        String sel  = "-fx-background-color: " + (light ? "rgba(184,85,7,0.10)" : "rgba(249,115,22,0.12)") + "; -fx-border-color: " + (light ? "#B85507" : "rgba(249,115,22,0.60)") + "; -fx-border-width: 1; -fx-background-radius: 7; -fx-border-radius: 7; -fx-padding: 10 16 10 16; -fx-text-fill: " + (light ? "#B85507" : "rgba(249,115,22,0.95)") + "; -fx-font-size: 12px; -fx-font-weight: 700; -fx-cursor: hand;";
        radio.setStyle(base);
        radio.selectedProperty().addListener((obs, was, isSelected) -> {
            radio.setStyle(isSelected ? sel : base);
            radioIcon.setGlyphStyle("-fx-fill: " + (isSelected ? (light ? "#B85507" : "rgba(249,115,22,0.95)") : (light ? "#4A7566" : "rgba(226,232,240,0.80)")) + ";");
        });
        return radio;
    }

    private Button buildFooterButton(String label, FontAwesomeIcon icon, boolean isPrimary, boolean light) {
        FontAwesomeIconView btnIcon = new FontAwesomeIconView(icon);
        btnIcon.setSize("13");
        btnIcon.setGlyphStyle(isPrimary ? "-fx-fill: rgba(255,255,255,0.95);" : "-fx-fill: " + (light ? "#1A1A1A" : "rgba(148,163,184,0.90)") + ";");
        Button btn = new Button(label, btnIcon);
        btn.setMinWidth(126);
        btn.setMinHeight(40);
        String normalStyle = isPrimary
                ? "-fx-background-color: " + (light ? "#B85507" : "rgba(249,115,22,0.92)") + "; -fx-border-color: " + (light ? "rgba(205,92,8,0.40)" : "rgba(249,115,22,0.70)") + "; -fx-border-width: 1; -fx-background-radius: 7; -fx-border-radius: 7; -fx-padding: 9 20 9 20; -fx-text-fill: rgba(255,255,255,0.95); -fx-font-size: 13px; -fx-font-weight: 800; -fx-cursor: hand;"
                : "-fx-background-color: " + (light ? "#EDE8DF" : "rgba(255,255,255,0.10)") + "; -fx-border-color: " + (light ? "rgba(176,200,178,0.75)" : "rgba(148,163,184,0.38)") + "; -fx-border-width: 1; -fx-background-radius: 7; -fx-border-radius: 7; -fx-padding: 9 20 9 20; -fx-text-fill: " + (light ? "#1A1A1A" : "rgba(226,232,240,0.96)") + "; -fx-font-size: 13px; -fx-font-weight: 700; -fx-cursor: hand;";
        String hoverStyle = isPrimary
                ? "-fx-background-color: " + (light ? "#A34A06" : "rgba(249,115,22,1.0)") + "; -fx-border-color: " + (light ? "rgba(205,92,8,0.60)" : "rgba(249,115,22,0.90)") + "; -fx-border-width: 1; -fx-background-radius: 7; -fx-border-radius: 7; -fx-padding: 9 20 9 20; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 800; -fx-cursor: hand;"
                : "-fx-background-color: " + (light ? "#E8E2D8" : "rgba(255,255,255,0.15)") + "; -fx-border-color: " + (light ? "rgba(184,85,7,0.45)" : "rgba(148,163,184,0.50)") + "; -fx-border-width: 1; -fx-background-radius: 7; -fx-border-radius: 7; -fx-padding: 9 20 9 20; -fx-text-fill: " + (light ? "#1A1A1A" : "rgba(226,232,240,0.95)") + "; -fx-font-size: 13px; -fx-font-weight: 700; -fx-cursor: hand;";
        btn.setStyle(normalStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(normalStyle));
        return btn;
    }



    private void sendToPrinter(List<EvacuationPlanModel> filtered, Printer targetPrinter) {
        String reportType = getSelectedReportType();
        VBox   content    = buildReportContent(reportType);
        if (content == null) return;

        PrinterJob job = (targetPrinter != null)
                ? PrinterJob.createPrinterJob(targetPrinter)
                : PrinterJob.createPrinterJob();

        if (job == null) {
            AlertDialogManager.showError("Print Error",
                    "Could not create a print job for the selected printer.\n"
                            + "Please check that the printer is connected and try again.");
            return;
        }

        // Apply paper size + orientation from UI
        applyPageLayout(job);
        PageLayout pageLayout = job.getJobSettings().getPageLayout();

        // ── Calculate actual page count based on data ─────────────────────────
        int actualPageCount = calculatePageCount(filtered, pageLayout, reportType);

        // Set copies from spinner
        int copies = copiesSpinner.getValue();
        job.getJobSettings().setCopies(copies);

        // Lock page range to actual data pages only (not 1–9999)
        job.getJobSettings().setPageRanges(new PageRange(1, actualPageCount));
        // ─────────────────────────────────────────────────────────────────────

        // Silent print — no dialog
        if (job.printPage(pageLayout, content)) {
            job.endJob();
            AlertDialogManager.showInfo("Print Successful",
                    String.format("Document sent to: %s%n%d page(s) × %d %s printed successfully.",
                            job.getPrinter().getName(),
                            actualPageCount,
                            copies, copies == 1 ? "copy" : "copies"));
            closeDialog();
        } else {
            AlertDialogManager.showError("Print Error",
                    "An error occurred while printing. Please try again.");
        }
    }

    private int calculatePageCount(List<EvacuationPlanModel> filtered,
                                   PageLayout pageLayout, String reportType) {
        if (filtered == null || filtered.isEmpty()) return 1;

        double pageHeight = pageLayout.getPrintableHeight();

        double usableHeight = pageHeight - 120;
        int rowHeight;

        switch (reportType) {
            case "Evacuation Plan":
                rowHeight = 24;
                break;
            case "Capacity Report":
                return 1;
            default:
                rowHeight = 22;
                break;
        }

        int rowsPerPage = Math.max(1, (int) (usableHeight / rowHeight));
        int totalRows   = filtered.size();

        if ("Evacuation Plan".equals(reportType)) {
            long groupCount = filtered.stream()
                    .map(p -> Character.toUpperCase(p.getBeneficiaryName().charAt(0)))
                    .distinct().count();
            totalRows += (int) groupCount; // each group label takes ~1 extra row
        }

        return Math.max(1, (int) Math.ceil((double) totalRows / rowsPerPage));
    }

    private void applyPageLayout(PrinterJob job) {
        Printer         printer     = job.getPrinter();
        Paper           paper       = resolvePaper(printer);
        PageOrientation orientation = landscapeRadio.isSelected()
                ? PageOrientation.LANDSCAPE : PageOrientation.PORTRAIT;
        PageLayout layout = printer.createPageLayout(
                paper, orientation, MARGIN, MARGIN, MARGIN, MARGIN);
        job.getJobSettings().setPageLayout(layout);
    }

    private Paper resolvePaper(Printer printer) {
        String selected = bondPaperSizeComboBox.getValue();
        if (selected == null) return Paper.A4;
        if (selected.startsWith("Letter")) return Paper.NA_LETTER;
        if (selected.startsWith("Legal"))  return Paper.LEGAL;
        return Paper.A4;
    }

    private void generateAndSavePDF(List<EvacuationPlanModel> filtered,
                                    String disasterName, String siteName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Evacuation Report as PDF");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));
        chooser.setInitialFileName("EvacuationReport_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + ".pdf");

        Stage owner;
        try { owner = (Stage) printBtn.getScene().getWindow(); }
        catch (Exception e) { owner = null; }

        File file = chooser.showSaveDialog(owner);
        if (file == null) return;

        PdfProgressRunner.run(
                dialogStage,

                progress -> buildPDF(file, filtered, disasterName, siteName, progress),

                () -> {
                    AlertDialogManager.showInfo("PDF Saved",
                            "PDF successfully saved to:\n" + file.getAbsolutePath());
                    try {
                        if (java.awt.Desktop.isDesktopSupported()) {
                            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                            if (desktop.isSupported(java.awt.Desktop.Action.OPEN))
                                desktop.open(file);
                        }
                    } catch (Exception ex) { ex.printStackTrace(); }
                },

                errorMsg -> AlertDialogManager.showError("PDF Error",
                        "Failed to generate PDF:\n" + errorMsg)
        );
    }


    private void buildPDF(File file, List<EvacuationPlanModel> filtered,
                          String disasterName, String siteName,
                          PdfProgressRunner.PdfProgressCallback progress) throws Exception {

        PageSize pageSize = PageSize.A4;
        float    pageWidth = pageSize.getWidth();

        progress.update(8.0, "Creating fonts...");
        PdfFont fontBold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont fontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontMono   = PdfFontFactory.createFont(StandardFonts.COURIER);

        String reportType = getSelectedReportType();
        String timestamp  = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy  hh:mm a"));


        progress.update(15.0, "Building document (Pass 1 of 2)...");
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        PdfWriter   writer1 = new PdfWriter(baos);
        PdfDocument pdfDoc1 = new PdfDocument(writer1);
        Document    doc1    = new Document(pdfDoc1, pageSize);
        doc1.setMargins(36, 36, 54, 36);

        progress.update(20.0, "Writing report header...");
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth().setMarginBottom(4);
        headerTable.addCell(new Cell()
                .add(new Paragraph(reportType.toUpperCase())
                        .setFont(fontBold).setFontSize(18).setFontColor(PDF_WHITE))
                .add(new Paragraph("MUNICIPALITY OF BANATE Disaster Risk Reduction and Management")
                        .setFont(fontNormal).setFontSize(10)
                        .setFontColor(new DeviceRgb(189, 215, 238)))
                .setBackgroundColor(PDF_HEADER_BG).setPadding(14)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        headerTable.addCell(new Cell()
                .add(new Paragraph("RESPOND-PH")
                        .setFont(fontBold).setFontSize(13).setFontColor(PDF_WHITE)
                        .setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph(timestamp)
                        .setFont(fontNormal).setFontSize(8)
                        .setFontColor(new DeviceRgb(189, 215, 238))
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(PDF_HEADER_BG).setPadding(14)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE));
        doc1.add(headerTable);

        progress.update(25.0, "Writing metadata...");
        Table metaTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setBackgroundColor(new DeviceRgb(240, 244, 248))
                .setBorder(new SolidBorder(new DeviceRgb(190, 210, 230), 1))
                .setMarginTop(10).setMarginBottom(14);
        addMetaRow(metaTable, "Disaster Event",  disasterName,                       fontBold, fontNormal);
        addMetaRow(metaTable, "Evacuation Site", siteName,                            fontBold, fontNormal);
        addMetaRow(metaTable, "Report Type",     reportType,                          fontBold, fontNormal);
        addMetaRow(metaTable, "Total Records",   filtered.size() + " beneficiaries",  fontBold, fontNormal);
        addMetaRow(metaTable, "Generated On",    timestamp,                           fontBold, fontNormal);
        doc1.add(metaTable);

        progress.update(32.0, "Writing report body...");
        switch (reportType) {
            case "Beneficiary List" ->
                    buildPdfBeneficiaryList(doc1, filtered, disasterName, siteName, fontBold, fontNormal, fontMono);
            case "Evacuation Plan"  ->
                    buildPdfEvacuationPlan(doc1, filtered, siteName, fontBold, fontNormal);
            case "Capacity Report"  ->
                    buildPdfCapacityReport(doc1, filtered, siteName, fontBold, fontNormal);
        }

        progress.update(68.0, "Writing footer note...");
        doc1.add(new Paragraph(
                "This report was generated automatically by RESPOND-PH. " +
                        "Data reflects the latest evacuation plan records.")
                .setFont(fontNormal).setFontSize(8)
                .setFontColor(new DeviceRgb(120, 130, 140))
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(10));

        progress.update(72.0, "Finalizing first pass...");
        doc1.close();

        progress.update(75.0, "Counting pages...");
        int totalPages;
        try (PdfDocument counter = new PdfDocument(
                new com.itextpdf.kernel.pdf.PdfReader(
                        new java.io.ByteArrayInputStream(baos.toByteArray())))) {
            totalPages = counter.getNumberOfPages();
        }


        progress.update(78.0, "Stamping page numbers (Pass 2 of 2)...");
        PdfDocument pdfDoc2 = new PdfDocument(
                new com.itextpdf.kernel.pdf.PdfReader(
                        new java.io.ByteArrayInputStream(baos.toByteArray())),
                new PdfWriter(file));

        PdfFont stampFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        for (int i = 1; i <= totalPages; i++) {
            double stampPct = 78.0 + (i / (double) totalPages) * 18.0;
            progress.update(stampPct, "Stamping page " + i + " of " + totalPages + "...");

            com.itextpdf.kernel.pdf.PdfPage page = pdfDoc2.getPage(i);
            com.itextpdf.kernel.pdf.canvas.PdfCanvas canvas =
                    new com.itextpdf.kernel.pdf.canvas.PdfCanvas(page);
            canvas.beginText()
                    .setFontAndSize(stampFont, 8)
                    .setColor(new DeviceRgb(150, 160, 170), true)
                    .moveText(pageWidth / 2 - 20, 22)
                    .showText("Page " + i + " of " + totalPages)
                    .endText()
                    .release();
        }

        progress.update(97.0, "Saving file to disk...");
        pdfDoc2.close();
    }


    private void buildPdfBeneficiaryList(Document doc, List<EvacuationPlanModel> plans,
                                         String disasterName, String siteName,
                                         PdfFont fontBold, PdfFont fontNormal, PdfFont fontMono) {
        doc.add(new Paragraph("Beneficiary List")
                .setFont(fontBold).setFontSize(13).setFontColor(PDF_HEADER_BG).setMarginBottom(4));

        Table table = new Table(UnitValue.createPercentArray(new float[]{6, 60, 34}))
                .useAllAvailableWidth().setMarginBottom(12);

        for (String col : new String[]{"#", "Beneficiary Name", "Date Assigned"}) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(col).setFont(fontBold).setFontSize(9).setFontColor(PDF_WHITE))
                    .setBackgroundColor(PDF_HEADER_BG).setPadding(5)
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        }

        boolean alt = false;
        for (int i = 0; i < plans.size(); i++) {
            EvacuationPlanModel p = plans.get(i);
            DeviceRgb bg = alt ? new DeviceRgb(245, 248, 252) : new DeviceRgb(255, 255, 255);
            alt = !alt;
            table.addCell(pdfCell(String.valueOf(i + 1),          fontMono,   8, bg, TextAlignment.CENTER));
            table.addCell(pdfCell(nvl(p.getBeneficiaryName()),    fontNormal, 9, bg, TextAlignment.LEFT));
            table.addCell(pdfCell(formatDate(p.getDateCreated()), fontNormal, 9, bg, TextAlignment.CENTER));
        }
        doc.add(table);
        doc.add(new Paragraph("Total Beneficiaries: " + plans.size())
                .setFont(fontBold).setFontSize(10).setFontColor(PDF_HEADER_BG).setMarginTop(4));
    }

    private void buildPdfEvacuationPlan(Document doc, List<EvacuationPlanModel> plans,
                                        String siteName, PdfFont fontBold, PdfFont fontNormal) {
        doc.add(new Paragraph("Evacuation Plan")
                .setFont(fontBold).setFontSize(13).setFontColor(PDF_HEADER_BG).setMarginBottom(4));

        Map<Character, List<EvacuationPlanModel>> byLetter = plans.stream()
                .collect(Collectors.groupingBy(
                        p -> Character.toUpperCase(p.getBeneficiaryName().charAt(0)),
                        TreeMap::new, Collectors.toList()));

        int counter = 1;
        for (Map.Entry<Character, List<EvacuationPlanModel>> entry : byLetter.entrySet()) {
            doc.add(new Paragraph(String.valueOf(entry.getKey()))
                    .setFont(fontBold).setFontSize(12)
                    .setFontColor(new DeviceRgb(26, 82, 118))
                    .setMarginTop(8).setMarginBottom(2));

            Table group = new Table(UnitValue.createPercentArray(new float[]{6, 54, 40}))
                    .useAllAvailableWidth().setMarginBottom(2);
            for (EvacuationPlanModel p : entry.getValue()) {
                DeviceRgb bg = new DeviceRgb(249, 251, 253);
                group.addCell(pdfCell(String.valueOf(counter++), fontNormal, 9, bg, TextAlignment.CENTER));
                group.addCell(pdfCell(nvl(p.getBeneficiaryName()), fontNormal, 9, bg, TextAlignment.LEFT));
                group.addCell(pdfCell("✔ " + siteName, fontNormal, 8,
                        new DeviceRgb(235, 245, 235), TextAlignment.LEFT));
            }
            doc.add(group);
        }
        doc.add(new Paragraph("Total: " + plans.size() + " beneficiaries assigned")
                .setFont(fontBold).setFontSize(10).setFontColor(PDF_HEADER_BG).setMarginTop(6));
    }

    private void buildPdfCapacityReport(Document doc, List<EvacuationPlanModel> plans,
                                        String siteName, PdfFont fontBold, PdfFont fontNormal) {
        doc.add(new Paragraph("Capacity Report")
                .setFont(fontBold).setFontSize(13).setFontColor(PDF_HEADER_BG).setMarginBottom(6));

        int    totalCapacity = evacSiteCapacities.getOrDefault(siteName, 0);
        int    occupied      = plans.size();
        int    available     = Math.max(0, totalCapacity - occupied);
        double pct           = totalCapacity > 0 ? (occupied * 100.0 / totalCapacity) : 0.0;

        Table statsTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth().setMarginBottom(10);
        addMetaRow(statsTable, "Evacuation Site",    siteName,                     fontBold, fontNormal);
        addMetaRow(statsTable, "Total Capacity",      totalCapacity + " persons",   fontBold, fontNormal);
        addMetaRow(statsTable, "Currently Occupied",  occupied + " persons",        fontBold, fontNormal);
        addMetaRow(statsTable, "Available Slots",     available + " persons",       fontBold, fontNormal);
        addMetaRow(statsTable, "Occupancy Rate",      String.format("%.1f%%", pct), fontBold, fontNormal);
        doc.add(statsTable);

        DeviceRgb barColor  = pct >= 100 ? PDF_RED : pct >= 80 ? PDF_ORANGE : PDF_GREEN;
        float     filledPct = (float) Math.min(pct, 100.0);

        Table barTable = new Table(UnitValue.createPercentArray(
                filledPct < 100 ? new float[]{filledPct, 100 - filledPct} : new float[]{100f}))
                .useAllAvailableWidth().setHeight(18).setMarginBottom(8);
        barTable.addCell(new Cell()
                .add(new Paragraph(String.format("%.0f%%", pct))
                        .setFont(fontBold).setFontSize(8).setFontColor(PDF_WHITE)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(barColor).setPadding(2)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        if (filledPct < 100) {
            barTable.addCell(new Cell().add(new Paragraph(""))
                    .setBackgroundColor(new DeviceRgb(220, 220, 220)).setPadding(2)
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        }
        doc.add(barTable);

        String statusNote = pct >= 100
                ? "⚠ Site is at full capacity — no more evacuees can be accommodated."
                : pct >= 80 ? "⚠ Site is nearly full — consider opening additional sites."
                :              "✔ Site has available space for more evacuees.";
        doc.add(new Paragraph(statusNote)
                .setFont(fontBold).setFontSize(10)
                .setFontColor(pct >= 80 ? PDF_RED : PDF_GREEN).setMarginTop(4));
    }

    // ── PDF helpers ───────────────────────────────────────────────────────────

    private void addMetaRow(Table table, String label, String value,
                            PdfFont fontBold, PdfFont fontNormal) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setFont(fontBold).setFontSize(9)
                        .setFontColor(new DeviceRgb(40, 60, 90)))
                .setBackgroundColor(new DeviceRgb(225, 235, 245)).setPadding(6)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        table.addCell(new Cell()
                .add(new Paragraph(value).setFont(fontNormal).setFontSize(9)
                        .setFontColor(new DeviceRgb(40, 60, 90)))
                .setBackgroundColor(new DeviceRgb(245, 248, 252)).setPadding(6)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
    }

    private Cell pdfCell(String text, PdfFont font, float size,
                         DeviceRgb bg, TextAlignment align) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(size)
                        .setTextAlignment(align).setFontColor(new DeviceRgb(40, 50, 60)))
                .setBackgroundColor(bg)
                .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(5).setPaddingRight(5)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(new DeviceRgb(220, 225, 230), 0.5f));
    }

    private PageSize resolvePdfPageSize() {
        String sel = bondPaperSizeComboBox.getValue();
        if (sel == null) return PageSize.A4;
        if (sel.startsWith("Letter")) return PageSize.LETTER;
        if (sel.startsWith("Legal"))  return PageSize.LEGAL;
        return PageSize.A4;
    }

    // =========================================================================
    // PRINTER DETECTION
    // =========================================================================

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
            entries.add(new PrinterEntry(p, isPrinterActive(p), p.equals(defaultPrinter)));
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
        return os.contains("win")
                ? isPrinterActiveWindows(printer.getName())
                : isPrinterActiveUnix(printer);
    }

    private boolean isVirtualPrinter(String name) {
        if (name == null) return false;
        String l = name.toLowerCase();
        return l.contains("pdf")  || l.contains("fax")     || l.contains("xps")
                || l.contains("onenote")   || l.contains("microsoft print")
                || l.contains("microsoft document") || l.contains("send to")
                || l.contains("snagit")    || l.contains("cutepdf")
                || l.contains("cute pdf")  || l.contains("bullzip")
                || l.contains("dopdf")     || l.contains("nitro")
                || l.contains("foxit")     || l.contains("pdfcreator")
                || l.contains("primopdf")  || l.contains("pdf24")
                || l.contains("adobe pdf");
    }

    private boolean isPrinterActiveWindows(String printerName) {
        try {
            String safeName = printerName.replace("'", "''");
            String[] cmd = { "powershell", "-NoProfile", "-NonInteractive", "-Command",
                    "(Get-Printer -Name '" + safeName + "').IsOnline" };
            Process proc = Runtime.getRuntime().exec(cmd);
            String output;
            try (java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(proc.getInputStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line.trim());
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
            String[] cmd = { "wmic", "printer",
                    "where", "Name='" + printerName.replace("'", "\\'") + "'",
                    "get", "PrinterStatus,WorkOffline", "/format:csv" };
            Process proc = Runtime.getRuntime().exec(cmd);
            String output;
            try (java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(proc.getInputStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append("\n");
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
                if ("TRUE".equalsIgnoreCase(parts[parts.length - 1].trim())) return false;
                try {
                    int s = Integer.parseInt(parts[parts.length - 2].trim());
                    return s == 3 || s == 4 || s == 5;
                } catch (NumberFormatException ex) { return false; }
            }
            return false;
        } catch (Exception e) { return false; }
    }

    private boolean isPrinterActiveUnix(Printer printer) {
        if (printer == null) return false;
        try {
            Process proc = Runtime.getRuntime().exec(
                    new String[]{ "lpstat", "-p", printer.getName() });
            String output;
            try (java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(proc.getInputStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append(" ");
                output = sb.toString().toLowerCase();
            }
            proc.waitFor();
            if (output.contains("enabled"))  return true;
            if (output.contains("disabled")) return false;
        } catch (Exception ignored) { }
        try {
            for (javax.print.PrintService svc :
                    javax.print.PrintServiceLookup.lookupPrintServices(null, null)) {
                if (!svc.getName().equalsIgnoreCase(printer.getName())) continue;
                javax.print.attribute.PrintServiceAttributeSet attrs = svc.getAttributes();
                javax.print.attribute.standard.PrinterIsAcceptingJobs acc =
                        (javax.print.attribute.standard.PrinterIsAcceptingJobs)
                                attrs.get(javax.print.attribute.standard.PrinterIsAcceptingJobs.class);
                if (acc != null && acc ==
                        javax.print.attribute.standard.PrinterIsAcceptingJobs.NOT_ACCEPTING_JOBS)
                    return false;
                javax.print.attribute.standard.PrinterState state =
                        (javax.print.attribute.standard.PrinterState)
                                attrs.get(javax.print.attribute.standard.PrinterState.class);
                if (state != null)
                    return state == javax.print.attribute.standard.PrinterState.IDLE
                            || state == javax.print.attribute.standard.PrinterState.PROCESSING;
                break;
            }
        } catch (Exception ignored) { }
        return false;
    }

    // =========================================================================
    // JAVAFX REPORT BUILDERS (all unchanged)
    // =========================================================================

    private boolean validate() {
        if (disasterComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Validation", "Please select a disaster event.");
            return false;
        }
        if (evacuationSiteComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Validation", "Please select an evacuation site.");
            return false;
        }
        if (bondPaperSizeComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Validation", "Please select a paper size.");
            return false;
        }
        return true;
    }

    private void handlePreview() {
        if (!validate()) return;
        VBox content = buildReportContent(getSelectedReportType());
        if (content == null) return;

        // ✅ Wrap pages in a centering container
        VBox wrapper = new VBox(20);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setPadding(new Insets(20));
        wrapper.setStyle("-fx-background-color: #5a5a5a;");
        wrapper.getChildren().add(content);

        ScrollPane scrollPane = new ScrollPane(wrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // ✅ no horizontal scroll
        scrollPane.setStyle("-fx-background-color: #5a5a5a;");

        Stage previewStage = new Stage();
        previewStage.initModality(Modality.APPLICATION_MODAL);
        previewStage.setTitle("Preview – " + getSelectedReportType());

        // ✅ Wide enough to show full page card + padding
        previewStage.setScene(new Scene(scrollPane, 720, 920));
        previewStage.setMinWidth(720);
        previewStage.setMinHeight(600);
        previewStage.show();
    }

    private void populateDisasterCombo() {
        disasterNameToId.clear();
        allPlans.stream()
                .filter(p -> p.getDisasterName() != null)
                .sorted(Comparator.comparing(EvacuationPlanModel::getDisasterName))
                .forEach(p -> disasterNameToId.put(p.getDisasterName(), p.getDisasterId()));

        ObservableList<String> names =
                FXCollections.observableArrayList(disasterNameToId.keySet());
        disasterComboBox.setItems(names);
        if (!names.isEmpty()) disasterComboBox.getSelectionModel().selectFirst();
    }

    private void populateEvacSiteComboForDisaster(String disasterName) {
        evacuationSiteComboBox.getItems().clear();
        evacSiteNameToId.clear();
        evacSiteCapacities.clear();
        hideCapacitySummary();

        Integer disasterId = disasterNameToId.get(disasterName);
        if (disasterId == null) return;

        Task<Map<String, Integer>> task = new Task<>() {
            @Override protected Map<String, Integer> call() {
                return allPlans.stream()
                        .filter(p -> disasterId.equals(p.getDisasterId()))
                        .filter(p -> p.getEvacSiteName() != null)
                        .collect(Collectors.toMap(
                                EvacuationPlanModel::getEvacSiteName,
                                EvacuationPlanModel::getEvacSiteId,
                                (a, b) -> a, LinkedHashMap::new));
            }
            @Override protected void succeeded() {
                Map<String, Integer> siteMap = getValue();
                evacSiteNameToId.putAll(siteMap);
                siteMap.forEach((siteName, siteId) -> {
                    EvacSiteModel model = evacSiteDAO.getById(siteId);
                    if (model != null) {
                        try {
                            evacSiteCapacities.put(siteName,
                                    Integer.parseInt(model.getCapacity()));
                        } catch (NumberFormatException ignored) {
                            evacSiteCapacities.put(siteName, 0);
                        }
                    }
                });
                ObservableList<String> sorted = FXCollections.observableArrayList(
                        siteMap.keySet().stream().sorted().collect(Collectors.toList()));
                evacuationSiteComboBox.setItems(sorted);
                if (!sorted.isEmpty()) evacuationSiteComboBox.getSelectionModel().selectFirst();
            }
            @Override protected void failed() {
                AlertDialogManager.showError("Error",
                        "Failed to load evacuation sites: " + getException().getMessage());
            }
        };
        new Thread(task).start();
    }

    private void updateCapacitySummary(String siteName) {
        String  disasterName = disasterComboBox.getValue();
        Integer disasterId   = disasterNameToId.get(disasterName);
        Integer siteId       = evacSiteNameToId.get(siteName);
        if (disasterId == null || siteId == null) { hideCapacitySummary(); return; }

        long    occupied = allPlans.stream()
                .filter(p -> disasterId.equals(p.getDisasterId()) && siteId.equals(p.getEvacSiteId()))
                .count();
        Integer capacity = evacSiteCapacities.getOrDefault(siteName, 0);
        capacityLabel.setText(String.format("Capacity: %d / %d  (%.0f%% occupied)",
                occupied, capacity,
                capacity > 0 ? (occupied * 100.0 / capacity) : 0.0));
        capacitySummary.setVisible(true);
        capacitySummary.setManaged(true);
    }

    private void hideCapacitySummary() {
        capacitySummary.setVisible(false);
        capacitySummary.setManaged(false);
    }

    private String getSelectedReportType() {
        if (evacuationPlanRadio.isSelected()) return "Evacuation Plan";
        if (capacityReportRadio.isSelected()) return "Capacity Report";
        return "Beneficiary List";
    }

    private VBox buildReportContent(String reportType) {
        String  disasterName = disasterComboBox.getValue();
        String  siteName     = evacuationSiteComboBox.getValue();
        Integer disasterId   = disasterNameToId.get(disasterName);
        Integer siteId       = evacSiteNameToId.get(siteName);

        if (disasterId == null || siteId == null) {
            AlertDialogManager.showWarning("No Selection", "Please select a disaster and site.");
            return null;
        }

        List<EvacuationPlanModel> filtered = allPlans.stream()
                .filter(p -> disasterId.equals(p.getDisasterId()) && siteId.equals(p.getEvacSiteId()))
                .sorted(Comparator.comparing(EvacuationPlanModel::getBeneficiaryName))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            AlertDialogManager.showWarning("No Data",
                    "No records found for the selected disaster and evacuation site.");
            return null;
        }

        return switch (reportType) {
            case "Evacuation Plan" -> buildEvacuationPlanContent(disasterName, siteName, filtered);
            case "Capacity Report" -> buildCapacityReportContent(disasterName, siteName,
                    siteId, disasterId, filtered);
            default                -> buildBeneficiaryListContent(disasterName, siteName, filtered);
        };
    }

    private VBox buildBeneficiaryListContent(String disasterName, String siteName,
                                             List<EvacuationPlanModel> plans) {
        VBox root = printPage();
        if (includeHeaderCheckbox.isSelected())
            root.getChildren().add(buildHeader("BENEFICIARY LIST", disasterName, siteName));
        root.getChildren().add(separator());

        HBox colHeader = styledRow(true);
        colHeader.getChildren().addAll(
                colCell("#", 50, true),
                colCell("Beneficiary Name", 340, true),
                colCell("Date Assigned", 150, true));
        root.getChildren().add(colHeader);

        for (int i = 0; i < plans.size(); i++) {
            EvacuationPlanModel p = plans.get(i);
            HBox row = styledRow(i % 2 == 0);
            row.getChildren().addAll(
                    colCell(String.valueOf(i + 1), 50, false),
                    colCell(p.getBeneficiaryName(), 340, false),
                    colCell(formatDate(p.getDateCreated()), 150, false));
            root.getChildren().add(row);
        }

        root.getChildren().add(separator());
        Label totalLabel = new Label("Total Beneficiaries: " + plans.size());
        totalLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold;");
        root.getChildren().add(totalLabel);
        if (includeFooterCheckbox.isSelected())     root.getChildren().add(buildFooter(plans.size()));
        if (includePageNumbersCheckbox.isSelected()) root.getChildren().add(pageNumber(1));
        return root;
    }

    private VBox buildEvacuationPlanContent(String disasterName, String siteName,
                                            List<EvacuationPlanModel> plans) {
        VBox root = printPage();
        if (includeHeaderCheckbox.isSelected())
            root.getChildren().add(buildHeader("EVACUATION PLAN", disasterName, siteName));
        root.getChildren().add(separator());

        Map<Character, List<EvacuationPlanModel>> byLetter = plans.stream()
                .collect(Collectors.groupingBy(
                        p -> Character.toUpperCase(p.getBeneficiaryName().charAt(0)),
                        TreeMap::new, Collectors.toList()));

        int counter = 1;
        for (Map.Entry<Character, List<EvacuationPlanModel>> group : byLetter.entrySet()) {
            Label groupLabel = new Label(String.valueOf(group.getKey()));
            groupLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; " +
                    "-fx-text-fill: #1a5276; -fx-padding: 6 0 2 0;");
            root.getChildren().add(groupLabel);

            for (EvacuationPlanModel p : group.getValue()) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(3, 0, 3, 12));
                Label numLabel  = new Label(counter++ + ".");
                numLabel.setMinWidth(30);
                numLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #555;");
                Label nameLabel = new Label(p.getBeneficiaryName());
                nameLabel.setStyle("-fx-font-size: 11;");
                Label siteTag = new Label("  ✔ " + siteName);
                siteTag.setStyle("-fx-font-size: 10; -fx-text-fill: #1a7a4a;");
                row.getChildren().addAll(numLabel, nameLabel, siteTag);
                root.getChildren().add(row);
            }
        }

        root.getChildren().add(separator());
        if (includeFooterCheckbox.isSelected())     root.getChildren().add(buildFooter(plans.size()));
        if (includePageNumbersCheckbox.isSelected()) root.getChildren().add(pageNumber(1));
        return root;
    }

    private VBox buildCapacityReportContent(String disasterName, String siteName,
                                            int siteId, int disasterId,
                                            List<EvacuationPlanModel> plans) {
        VBox root = printPage();
        if (includeHeaderCheckbox.isSelected())
            root.getChildren().add(buildHeader("CAPACITY REPORT", disasterName, siteName));
        root.getChildren().add(separator());

        int    totalCapacity = evacSiteCapacities.getOrDefault(siteName, 0);
        int    occupied      = plans.size();
        int    available     = Math.max(0, totalCapacity - occupied);
        double pct           = totalCapacity > 0 ? (occupied * 100.0 / totalCapacity) : 0.0;

        root.getChildren().addAll(
                statRow("Evacuation Site",    siteName),
                statRow("Disaster Event",     disasterName),
                statRow("Total Capacity",     totalCapacity + " persons"),
                statRow("Currently Occupied", occupied + " persons"),
                statRow("Available Slots",    available + " persons"),
                statRow("Occupancy Rate",     String.format("%.1f%%", pct)));
        root.getChildren().add(separator());

        double barWidth = 480.0;
        double filled   = barWidth * (pct / 100.0);
        StackPane bar = new StackPane();
        bar.setMaxWidth(barWidth); bar.setMinWidth(barWidth);
        bar.setMinHeight(22);      bar.setMaxHeight(22);
        bar.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 4;");
        HBox fill = new HBox();
        fill.setMinWidth(filled); fill.setMaxWidth(filled);
        fill.setMinHeight(22);    fill.setMaxHeight(22);
        fill.setStyle("-fx-background-color: " + capacityColor(pct) + "; -fx-background-radius: 4;");
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        bar.getChildren().addAll(fill, labelCentered(String.format("%.0f%% occupied", pct)));
        HBox barBox = new HBox(bar);
        barBox.setPadding(new Insets(10, 0, 10, 0));
        root.getChildren().add(barBox);
        root.getChildren().add(separator());

        String note = pct >= 100 ? "⚠ Site is at full capacity."
                : pct >= 80 ? "⚠ Site is nearly full." : "✔ Site has available space.";
        Label noteLabel = new Label(note);
        noteLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: "
                + (pct >= 80 ? "#c0392b" : "#1a7a4a") + ";");
        root.getChildren().add(noteLabel);

        if (includeFooterCheckbox.isSelected())     root.getChildren().add(buildFooter(occupied));
        if (includePageNumbersCheckbox.isSelected()) root.getChildren().add(pageNumber(1));
        return root;
    }

    // ── JavaFX UI helpers ─────────────────────────────────────────────────────

    private VBox printPage() {
        VBox page = new VBox(6);
        page.setPrefWidth(620);   // ✅ full bond paper card width
        page.setMaxWidth(620);
        page.setMinWidth(620);
        page.setPrefHeight(resolvePageHeight());
        page.setMinHeight(resolvePageHeight());
        page.setPadding(new Insets(36));
        page.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #bbbbbb;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.20), 10, 0, 0, 3);"
        );
        page.setAlignment(Pos.TOP_CENTER);
        return page;
    }

    private double resolvePageHeight() {
        String paper = bondPaperSizeComboBox.getValue();
        if (paper == null) return 842;
        if (paper.startsWith("Letter")) return 792;
        if (paper.startsWith("Legal"))  return 1008;
        return 842; // A4
    }
    private VBox buildHeader(String reportTitle, String disasterName, String siteName) {
        VBox h = new VBox(4);
        h.setAlignment(Pos.CENTER);
        Label republic  = new Label("Republic of the Philippines");
        republic.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");
        Label title     = new Label("BARANGAY DISASTER RISK REDUCTION");
        title.setStyle("-fx-font-size: 12; -fx-font-weight: bold;");
        Label title2    = new Label("AND MANAGEMENT");
        title2.setStyle("-fx-font-size: 12; -fx-font-weight: bold;");
        Label rptLabel  = new Label(reportTitle);
        rptLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1a3a6b; -fx-padding: 4 0 0 0;");
        Label disLabel  = new Label("Disaster: " + disasterName);
        disLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #c0392b;");
        Label siteLabel = new Label("Evacuation Site: " + siteName);
        siteLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #333;");
        Label dateLabel = new Label("Generated: " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy  HH:mm")));
        dateLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #777;");
        h.getChildren().addAll(republic, title, title2, rptLabel, disLabel, siteLabel, dateLabel);
        return h;
    }

    private HBox buildFooter(int totalRecords) {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 0, 0, 0));
        Label left = new Label("Total Records: " + totalRecords);
        left.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        String preparedBy = SessionManager.getInstance().getCurrentAdminFullName();
        Label right = new Label("Prepared by" + preparedBy);
        right.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");
        footer.getChildren().addAll(left, spacer, right);
        return footer;
    }

    private Label pageNumber(int page) {
        Label lbl = new Label("Page " + page);
        lbl.setStyle("-fx-font-size: 9; -fx-text-fill: #aaa;");
        HBox.setHgrow(lbl, Priority.ALWAYS);
        return lbl;
    }

    private Region separator() {
        Region sep = new Region();
        sep.setPrefWidth(CONTENT_WIDTH); sep.setMaxWidth(CONTENT_WIDTH);
        sep.setMinHeight(1); sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: #bdc3c7;");
        VBox.setMargin(sep, new Insets(6, 0, 6, 0));
        return sep;
    }

    private HBox styledRow(boolean shaded) {
        HBox row = new HBox();
        row.setPrefWidth(CONTENT_WIDTH); row.setMaxWidth(CONTENT_WIDTH);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(3, 6, 3, 6));
        row.setStyle("-fx-background-color: " + (shaded ? "#f2f3f4" : "white") + ";");
        return row;
    }

    private Label colCell(String text, double width, boolean header) {
        Label lbl = new Label(text);
        lbl.setMinWidth(width); lbl.setMaxWidth(width);
        lbl.setWrapText(true);
        lbl.setStyle(header
                ? "-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #1a3a6b;"
                : "-fx-font-size: 11;");
        return lbl;
    }

    private HBox statRow(String label, String value) {
        HBox row = new HBox(20);
        row.setPadding(new Insets(4, 0, 4, 0));
        Label lbl = new Label(label + ":");
        lbl.setMinWidth(180);
        lbl.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #444;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 11;");
        row.getChildren().addAll(lbl, val);
        return row;
    }

    private Label labelCentered(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: white;");
        return lbl;
    }

    private String capacityColor(double pct) {
        if (pct >= 100) return "#c0392b";
        if (pct >= 80)  return "#e67e22";
        return "#1a7a4a";
    }

    private String formatDate(String raw) {
        if (raw == null || raw.isBlank()) return "—";
        try { return raw.substring(0, Math.min(raw.length(), 10)); }
        catch (Exception e) { return raw; }
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "—" : s; }

    private javafx.stage.Window getOwnerWindow() {
        try { return printBtn.getScene().getWindow(); }
        catch (Exception e) { return null; }
    }

    private void closeDialog() {
        try { ((Stage) closeBtn.getScene().getWindow()).close(); }
        catch (Exception ignored) { }
    }
}