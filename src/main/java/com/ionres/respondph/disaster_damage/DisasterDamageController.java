package com.ionres.respondph.disaster_damage;


import com.ionres.respondph.disaster_damage.dialogs_controller.AddDisasterDamageDialogController;
import com.ionres.respondph.disaster_damage.dialogs_controller.EditDisasterDamageDialogController;
import com.ionres.respondph.util.*;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

// iText 7 PDF imports
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DisasterDamageController {

    private final DisasterDamageService disasterDamageService = AppContext.disasterDamageService;
    private ObservableList<DisasterDamageModel> disasterList;

    @FXML private AnchorPane root;
    @FXML private TextField searchField;
    @FXML private Button searchBtn, addBtn, refreshBtn, printListBtn, pdfBtn;
    @FXML private TableView<DisasterDamageModel> disastersDamageTbl;
    @FXML private TableColumn<DisasterDamageModel, String> damage_id;
    @FXML private TableColumn<DisasterDamageModel, String> beneficiaryNameColumn;
    @FXML private TableColumn<DisasterDamageModel, String> disasterColumn;
    @FXML private TableColumn<DisasterDamageModel, String> damageSeverityColumn;
    @FXML private TableColumn<DisasterDamageModel, String> assessmentDateColumn;
    @FXML private TableColumn<DisasterDamageModel, String> verifiedByColumn;
    @FXML private TableColumn<DisasterDamageModel, String> registeredDateColumn;
    @FXML private TableColumn<DisasterDamageModel, Void> actionsColumn;

    private Stage dialogStage;

    // Progress dialog components
    private Stage progressStage;
    private ProgressBar progressBar;
    private Label progressLabel;

    // ── PDF color palette ─────────────────────────────────────────────────────
    private static final DeviceRgb COLOR_HEADER_BG = new DeviceRgb(44, 62, 80);
    private static final DeviceRgb COLOR_WHITE      = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb COLOR_ALT_ROW    = new DeviceRgb(248, 249, 250);
    private static final DeviceRgb COLOR_ACCENT     = new DeviceRgb(52, 152, 219);

    // ── Printer entry ─────────────────────────────────────────────────────────
    private static class PrinterEntry {
        private final Printer printer;
        private final boolean active;
        private final boolean isDefault;

        PrinterEntry(Printer p, boolean active, boolean isDefault) {
            this.printer   = p;
            this.active    = active;
            this.isDefault = isDefault;
        }

        Printer getPrinter()    { return printer; }
        String  getPrinterName(){ return printer.getName(); }
        boolean isActive()      { return active; }
        boolean isDefault()     { return isDefault; }

        @Override public String toString() { return printer.getName(); }
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    // =========================================================================
    // INIT
    // =========================================================================

    @FXML
    private void initialize() {
        disastersDamageTbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTableColumns();
        loadTable();
        setupActionButtons();
        setupSearchListener();

        EventHandler<ActionEvent> handler = this::handleActions;
        searchBtn.setOnAction(handler);
        addBtn.setOnAction(handler);
        refreshBtn.setOnAction(handler);
        printListBtn.setOnAction(handler);
        pdfBtn.setOnAction(handler);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();
        if      (src == searchBtn)   handleSearch();
        else if (src == addBtn)      handleAddDisasterDamage();
        else if (src == refreshBtn)  handleRefresh();
        else if (src == printListBtn)showProgressAndOpenDialog("PRINT");
        else if (src == pdfBtn)      showProgressAndOpenDialog("PDF");
    }

    // =========================================================================
    // PROGRESS + BACKGROUND TASK
    // =========================================================================

    private void showProgressAndOpenDialog(String outputType) {
        createProgressDialog("Loading data for " +
                (outputType.equals("PRINT") ? "printing..." : "PDF export..."));

        Task<PrintDialogData> task = new Task<>() {
            @Override
            protected PrintDialogData call() throws Exception {
                PrintDialogData dialogData = new PrintDialogData();

                // Step 1: Disasters (0-30%)
                updateProgress(0, 100);
                updateMessage("Loading disaster list...");

                Set<DisasterItem> disasterSet = new HashSet<>();
                disasterSet.add(new DisasterItem(0, "All Disasters", "All"));

                if (disasterList != null) {
                    int total = disasterList.size();
                    int count = 0;
                    for (DisasterDamageModel d : disasterList) {
                        disasterSet.add(new DisasterItem(
                                d.getDisasterId(), d.getDisasterName(), d.getDisasterType()));
                        count++;
                        updateProgress((count * 30.0) / total, 100);
                    }
                }

                List<DisasterItem> disasters = new ArrayList<>(disasterSet);
                disasters.sort((a, b) -> {
                    if (a.getDisasterId() == 0) return -1;
                    if (b.getDisasterId() == 0) return  1;
                    return a.getDisasterName().compareToIgnoreCase(b.getDisasterName());
                });
                dialogData.disasters = disasters;

                // Step 2: Printers (30-70%)
                updateProgress(30, 100);
                updateMessage("Detecting connected printers...");

                ObservableList<PrinterEntry> activePrinters = FXCollections.observableArrayList();
                ObservableSet<Printer> printers = Printer.getAllPrinters();
                Printer defaultPrinter = Printer.getDefaultPrinter();

                if (printers != null && !printers.isEmpty()) {
                    List<Printer> printerList = new ArrayList<>(printers);
                    int total = printerList.size(), count = 0;
                    for (Printer p : printerList) {
                        boolean active = isPrinterActive(p);
                        boolean isDef  = p.equals(defaultPrinter);
                        activePrinters.add(new PrinterEntry(p, active, isDef));
                        count++;
                        updateProgress(30 + (count * 40.0) / total, 100);
                    }
                }

                ObservableList<PrinterEntry> filteredActive = FXCollections.observableArrayList(
                        activePrinters.filtered(PrinterEntry::isActive));
                filteredActive.sort((a, b) -> {
                    if (a.isDefault() != b.isDefault()) return a.isDefault() ? -1 : 1;
                    return a.getPrinterName().compareToIgnoreCase(b.getPrinterName());
                });
                dialogData.printers = filteredActive;

                // Step 3: Prepare (70-100%)
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
            showPrintDialog(outputType, data.disasters, data.printers);
            shutDownExecutor(executor);
        });

        task.setOnFailed(e -> {
            closeProgressDialog();
            AlertDialogManager.showError("Error",
                    "Failed to load data: " + task.getException().getMessage());
            task.getException().printStackTrace();
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

    private static class PrintDialogData {
        List<DisasterItem> disasters;
        ObservableList<PrinterEntry> printers;
    }

    // ── Progress dialog ───────────────────────────────────────────────────────

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

        // Header
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

        // Body
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
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        pctRow.getChildren().addAll(spacer, pctLabel);

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
    // PRINT DIALOG  (dark-card design)
    // =========================================================================

    private void showPrintDialog(String outputType,
                                 List<DisasterItem> disasters,
                                 ObservableList<PrinterEntry> activePrinters) {

        // ── Stage ────────────────────────────────────────────────────────────
        Stage printStage = new Stage();
        printStage.initModality(Modality.APPLICATION_MODAL);
        printStage.initStyle(StageStyle.UNDECORATED);
        printStage.setTitle(outputType.equals("PRINT")
                ? "Print Disaster Damage Report" : "Save Disaster Damage Report as PDF");

        // ── Root card ────────────────────────────────────────────────────────
        boolean light = ThemeManager.getInstance().isLightMode();
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

        FontAwesomeIconView headerIcon = new FontAwesomeIconView(
                outputType.equals("PRINT") ? FontAwesomeIcon.PRINT : FontAwesomeIcon.FILE_PDF_ALT);
        headerIcon.setSize("20");
        headerIcon.setGlyphStyle("-fx-fill: " + (light ? "rgba(255,255,255,0.92)" : "rgba(249,115,22,0.95)") + ";");

        VBox titleBlock = new VBox(3);
        Label titleLabel = new Label(outputType.equals("PRINT") ? "Print Report" : "Export as PDF");
        titleLabel.setStyle(
                "-fx-text-fill: " + (light ? "#FFFFFF" : "rgba(248,250,252,0.98)") + ";" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: 900;"
        );
        Label subtitleLabel = new Label("Disaster Damage Assessment Report");
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

        // ── SECTION: Disaster selection ───────────────────────────────────────
        VBox disasterSection = buildSection(
                FontAwesomeIcon.BOLT, "Select Disaster",
                "Choose which disaster records to include",
                light
        );

        ComboBox<DisasterItem> disasterCombo =
                new ComboBox<>(FXCollections.observableArrayList(disasters));
        disasterCombo.setMaxWidth(Double.MAX_VALUE);
        disasterCombo.setPromptText("Select disaster...");
        disasterCombo.getSelectionModel().selectFirst();
        disasterCombo.setStyle(
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
        String cellBorder= light ? "rgba(176,200,178,0.65)" : "rgba(148,163,184,0.22)";
        String cellText  = light ? "#1A1A1A" : "rgba(226,232,240,0.95)";
        String cellHover = light ? "rgba(193,216,195,0.35)" : "rgba(249,115,22,0.10)";
        String cellHoverText = light ? "#B85507" : "rgba(249,115,22,0.95)";

        Callback<ListView<DisasterItem>, ListCell<DisasterItem>> disasterCellFactory =
                lv -> {
                    if (lv != null) {
                        lv.setStyle(
                                "-fx-background-color: " + cellBg + ";" +
                                        "-fx-border-color: " + cellBorder + ";" +
                                        "-fx-border-width: 1;" +
                                        "-fx-border-radius: 6;" +
                                        "-fx-background-radius: 6;"
                        );
                    }
                    return new ListCell<>() {
                        @Override
                        protected void updateItem(DisasterItem item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                                setGraphic(null);
                                setStyle("-fx-background-color: transparent;");
                            } else {
                                setText(item.toString());
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
        disasterCombo.setCellFactory(disasterCellFactory);
        disasterCombo.setButtonCell(disasterCellFactory.call(null));
        disasterSection.getChildren().add(disasterCombo);

        body.getChildren().add(disasterSection);

        // ── SECTION: Printer (PRINT mode only) ───────────────────────────────
        ComboBox<PrinterEntry> printerCombo = new ComboBox<>(activePrinters);

        if (outputType.equals("PRINT")) {
            VBox printerSectionBox = new VBox(10);

            VBox printerCard = buildSection(
                    FontAwesomeIcon.DESKTOP, "Select Printer",
                    "Only connected printers are shown",
                    light
            );

            printerCombo.setMaxWidth(Double.MAX_VALUE);
            printerCombo.setPromptText("Choose printer...");
            printerCombo.setStyle(
                    "-fx-background-color: " + (light ? "#E8E2D8" : "rgba(255,255,255,0.04)") + ";" +
                            "-fx-border-color: " + (light ? "rgba(176,200,178,0.75)" : "rgba(148,163,184,0.20)") + ";" +
                            "-fx-border-width: 1;" +
                            "-fx-background-radius: 6;" +
                            "-fx-border-radius: 6;" +
                            "-fx-padding: 4 10 4 10;" +
                            "-fx-font-size: 13px;" +
                            "-fx-font-weight: 700;"
            );

            Callback<ListView<PrinterEntry>, ListCell<PrinterEntry>> printerCellFactory =
                    lv -> {
                        if (lv != null) {
                            lv.setStyle(
                                    "-fx-background-color: " + cellBg + ";" +
                                            "-fx-border-color: " + cellBorder + ";" +
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
            printerCombo.setCellFactory(printerCellFactory);
            printerCombo.setButtonCell(printerCellFactory.call(null));

            activePrinters.stream()
                    .filter(PrinterEntry::isDefault)
                    .findFirst()
                    .or(() -> activePrinters.isEmpty()
                            ? Optional.empty()
                            : Optional.of(activePrinters.get(0)))
                    .ifPresent(printerCombo::setValue);

            if (activePrinters.isEmpty()) {
                Label warning = new Label("⚠  No connected printers found. Please connect a printer and try again.");
                warning.setWrapText(true);
                warning.setStyle(
                        "-fx-text-fill: " + (light ? "#B85507" : "rgba(249,115,22,0.90)") + ";" +
                                "-fx-font-size: 12px;" +
                                "-fx-font-weight: 700;"
                );
                printerCombo.setDisable(true);
                printerCard.getChildren().addAll(printerCombo, warning);
            } else {
                printerCard.getChildren().add(printerCombo);
            }

            printerSectionBox.getChildren().add(printerCard);
            body.getChildren().add(printerSectionBox);
        }

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

        Button cancelBtn   = buildFooterButton("Cancel",   FontAwesomeIcon.TIMES,    false, light);
        Button generateBtn = buildFooterButton("Generate", FontAwesomeIcon.DOWNLOAD, true,  light);
        footer.getChildren().addAll(cancelBtn, generateBtn);

        card.getChildren().addAll(header, body, footer);

        // ── Wire actions ──────────────────────────────────────────────────────
        headerCloseBtn.setOnAction(e -> printStage.close());
        cancelBtn.setOnAction(e -> printStage.close());

        generateBtn.setOnAction(e -> {
            DisasterItem selected = disasterCombo.getValue();
            if (selected == null) {
                AlertDialogManager.showWarning("Selection Required", "Please select a disaster.");
                return;
            }

            List<DisasterDamageModel> records = getFilteredRecords(selected);
            if (records.isEmpty()) {
                AlertDialogManager.showWarning("No Records",
                        "No disaster damage records found for the selected disaster.");
                return;
            }

            printStage.close();

            if (outputType.equals("PRINT")) {
                PrinterEntry selectedPrinter = printerCombo.getValue();
                if (selectedPrinter == null) {
                    AlertDialogManager.showWarning("No Printer Selected",
                            "Please select a printer from the list.");
                    return;
                }
                printRecords(records, selected, selectedPrinter.getPrinter());
            } else {
                generateAndSavePDF(records, selected);
            }
        });

        Scene scene = new Scene(card);
        scene.setFill(null);
        printStage.setScene(scene);

        // Center on screen
        printStage.setOnShown(e -> {
            javafx.geometry.Rectangle2D screen =
                    javafx.stage.Screen.getPrimary().getVisualBounds();
            printStage.setX((screen.getWidth()  - card.getWidth())  / 2);
            printStage.setY((screen.getHeight() - card.getHeight()) / 2);
        });

        printStage.show();
    }

    // =========================================================================
    // SHARED DARK-CARD HELPERS
    // =========================================================================

    private VBox buildSection(FontAwesomeIcon icon, String title, String subtitle, boolean light) {
        VBox section = new VBox(10);
        section.setStyle(
                "-fx-background-color: " + (light ? "rgba(232,224,212,0.65)" : "rgba(255,255,255,0.03)") + ";" +
                        "-fx-border-color: " + (light ? "rgba(176,200,178,0.65)" : "rgba(148,163,184,0.14)") + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8;" +
                        "-fx-padding: 14 16 14 16;"
        );

        HBox sectionHeader = new HBox(10);
        sectionHeader.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView sectionIcon = new FontAwesomeIconView(icon);
        sectionIcon.setSize("14");
        sectionIcon.setGlyphStyle("-fx-fill: " + (light ? "#B85507" : "rgba(249,115,22,0.90)") + ";");

        VBox sectionTitleBlock = new VBox(2);
        Label sectionTitle = new Label(title);
        sectionTitle.setStyle(
                "-fx-text-fill: " + (light ? "#1A1A1A" : "rgba(248,250,252,0.95)") + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 800;"
        );
        Label sectionSubtitle = new Label(subtitle);
        sectionSubtitle.setStyle(
                "-fx-text-fill: " + (light ? "#4A7566" : "rgba(148,163,184,0.70)") + ";" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: 600;"
        );
        sectionTitleBlock.getChildren().addAll(sectionTitle, sectionSubtitle);
        sectionHeader.getChildren().addAll(sectionIcon, sectionTitleBlock);
        section.getChildren().add(sectionHeader);

        return section;
    }

    private RadioButton buildRadioCard(String label, FontAwesomeIcon icon, ToggleGroup group) {
        boolean light = ThemeManager.getInstance().isLightMode();
        RadioButton radio = new RadioButton(label);
        radio.setToggleGroup(group);

        FontAwesomeIconView radioIcon = new FontAwesomeIconView(icon);
        radioIcon.setSize("13");
        radioIcon.setGlyphStyle("-fx-fill: " + (light ? "#4A7566" : "rgba(226,232,240,0.80)") + ";");
        radio.setGraphic(radioIcon);

        String baseStyle =
                "-fx-background-color: " + (light ? "#E8E2D8" : "rgba(255,255,255,0.04)") + ";" +
                        "-fx-border-color: " + (light ? "rgba(176,200,178,0.75)" : "rgba(148,163,184,0.20)") + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 7;" +
                        "-fx-border-radius: 7;" +
                        "-fx-padding: 10 16 10 16;" +
                        "-fx-text-fill: " + (light ? "#1A1A1A" : "rgba(226,232,240,0.90)") + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: 700;" +
                        "-fx-cursor: hand;";

        String selectedStyle =
                "-fx-background-color: " + (light ? "rgba(184,85,7,0.10)" : "rgba(249,115,22,0.12)") + ";" +
                        "-fx-border-color: " + (light ? "#B85507" : "rgba(249,115,22,0.60)") + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 7;" +
                        "-fx-border-radius: 7;" +
                        "-fx-padding: 10 16 10 16;" +
                        "-fx-text-fill: " + (light ? "#B85507" : "rgba(249,115,22,0.95)") + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: 700;" +
                        "-fx-cursor: hand;";

        radio.setStyle(baseStyle);

        radio.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            radio.setStyle(isSelected ? selectedStyle : baseStyle);
            radioIcon.setStyle(isSelected
                    ? "-fx-fill: " + (light ? "#B85507" : "rgba(249,115,22,0.95)") + ";"
                    : "-fx-fill: " + (light ? "#4A7566" : "rgba(226,232,240,0.80)") + ";");
        });

        return radio;
    }

    private Button buildFooterButton(String label, FontAwesomeIcon icon, boolean isPrimary, boolean light) {
        FontAwesomeIconView btnIcon = new FontAwesomeIconView(icon);
        btnIcon.setSize("13");
        btnIcon.setGlyphStyle(isPrimary
                ? "-fx-fill: rgba(255,255,255,0.95);"
                : "-fx-fill: " + (light ? "#1A1A1A" : "rgba(226,232,240,0.96)") + ";");

        Button btn = new Button(label, btnIcon);
        btn.setMinWidth(126);
        btn.setMinHeight(40);

        String normalStyle = isPrimary
                ? "-fx-background-color: " + (light ? "#B85507" : "rgba(249,115,22,0.92)") + ";" +
                "-fx-border-color: " + (light ? "rgba(205,92,8,0.40)" : "rgba(249,115,22,0.70)") + ";" +
                "-fx-border-width: 1;" +
                "-fx-background-radius: 7;" +
                "-fx-border-radius: 7;" +
                "-fx-padding: 9 20 9 20;" +
                "-fx-text-fill: rgba(255,255,255,0.95);" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: 800;" +
                "-fx-cursor: hand;"
                : "-fx-background-color: " + (light ? "#EDE8DF" : "rgba(255,255,255,0.10)") + ";" +
                "-fx-border-color: " + (light ? "rgba(176,200,178,0.75)" : "rgba(148,163,184,0.38)") + ";" +
                "-fx-border-width: 1;" +
                "-fx-background-radius: 7;" +
                "-fx-border-radius: 7;" +
                "-fx-padding: 9 20 9 20;" +
                "-fx-text-fill: " + (light ? "#1A1A1A" : "rgba(226,232,240,0.96)") + ";" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: 700;" +
                "-fx-cursor: hand;";

        String hoverStyle = isPrimary
                ? "-fx-background-color: " + (light ? "#A34A06" : "rgba(249,115,22,1.0)") + ";" +
                "-fx-border-color: " + (light ? "rgba(205,92,8,0.60)" : "rgba(249,115,22,0.90)") + ";" +
                "-fx-border-width: 1;" +
                "-fx-background-radius: 7;" +
                "-fx-border-radius: 7;" +
                "-fx-padding: 9 20 9 20;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: 800;" +
                "-fx-cursor: hand;"
                : "-fx-background-color: " + (light ? "#E8E2D8" : "rgba(255,255,255,0.15)") + ";" +
                "-fx-border-color: " + (light ? "rgba(184,85,7,0.45)" : "rgba(148,163,184,0.50)") + ";" +
                "-fx-border-width: 1;" +
                "-fx-background-radius: 7;" +
                "-fx-border-radius: 7;" +
                "-fx-padding: 9 20 9 20;" +
                "-fx-text-fill: " + (light ? "#1A1A1A" : "rgba(226,232,240,0.95)") + ";" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: 700;" +
                "-fx-cursor: hand;";

        btn.setStyle(normalStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(normalStyle));

        return btn;
    }

    // =========================================================================
    // DATA / TABLE / SEARCH
    // =========================================================================

    private List<DisasterDamageModel> getFilteredRecords(DisasterItem selected) {
        if (selected.getDisasterId() == 0) {
            return new ArrayList<>(disasterList);
        }
        return disasterList.stream()
                .filter(d -> d.getDisasterId() == selected.getDisasterId())
                .collect(Collectors.toList());
    }

    private void handleRefresh() { loadTable(); }

    private void handleSearch() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) loadTable();
        else searchDisasterDamage(searchText);
    }

    private void handleAddDisasterDamage() { showAddDisasterDamageDialog(); }

    public void loadTable() {
        try {
            List<DisasterDamageModel> disasterDamage = disasterDamageService.getAllDisasterDamage();
            disasterList = FXCollections.observableArrayList(disasterDamage);
            disastersDamageTbl.setItems(disasterList);
            if (disasterDamage.isEmpty())
                disastersDamageTbl.setPlaceholder(new Label("No disaster damage records found"));
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error",
                    "Failed to load disaster damage records: " + e.getMessage());
        }
    }

    private void searchDisasterDamage(String searchText) {
        try {
            List<DisasterDamageModel> filtered =
                    disasterDamageService.searchDisasterDamage(searchText);
            disasterList = FXCollections.observableArrayList(filtered);
            disastersDamageTbl.setItems(disasterList);
            if (filtered.isEmpty())
                disastersDamageTbl.setPlaceholder(
                        new Label("No records found for: " + searchText));
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Search Error",
                    "Failed to search disaster damage records: " + e.getMessage());
        }
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.trim().isEmpty()) loadTable();
        });
    }

    private void printRecords(List<DisasterDamageModel> records, DisasterItem disaster, Printer printer) {
        try {
            PrinterJob job = PrinterJob.createPrinterJob(printer);
            if (job == null) {
                AlertDialogManager.showError("Print Error",
                        "Could not create a print job for the selected printer.");
                return;
            }

            PageLayout pageLayout = printer.createPageLayout(
                    Paper.A4, PageOrientation.LANDSCAPE, 36, 36, 36, 36);
            job.getJobSettings().setPageLayout(pageLayout);

            // ── Calculate actual page count ───────────────────────────────────
            int rowsPerPage = Math.max(1, (int)((pageLayout.getPrintableHeight() - 150) / 22));
            int actualPageCount = Math.max(1, (int) Math.ceil((double) records.size() / rowsPerPage));
            job.getJobSettings().setPageRanges(new PageRange(1, actualPageCount));

            VBox content = createPrintContent(records, disaster);

            // ── Silent print — no dialog ──────────────────────────────────────
            if (job.printPage(pageLayout, content)) {
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

    private VBox createPrintContent(List<DisasterDamageModel> records, DisasterItem disaster) {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: white;");
        root.setPrefWidth(900);

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy  hh:mm a"));
        String disasterInfo = disaster.getDisasterId() == 0
                ? "All Disasters"
                : disaster.getDisasterName() + " (" + disaster.getDisasterType() + ")";

        // ── Header bar (dark navy) ────────────────────────────────────────────
        HBox header = new HBox();
        header.setStyle("-fx-background-color: #2c3e50; -fx-padding: 14 18 14 18;");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox headerLeft = new VBox(3);
        Label reportTitle = new Label("DISASTER DAMAGE ASSESSMENT REPORT");
        reportTitle.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subTitle = new Label("Barangay Disaster Risk Reduction and Management");
        subTitle.setStyle("-fx-font-size: 10; -fx-text-fill: #bdd7ee;");
        headerLeft.getChildren().addAll(reportTitle, subTitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox headerRight = new VBox(3);
        headerRight.setAlignment(Pos.CENTER_RIGHT);
        Label systemName = new Label("RESPOND-PH");
        systemName.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: white;");
        Label tsLabel = new Label(timestamp);
        tsLabel.setStyle("-fx-font-size: 8; -fx-text-fill: #bdd7ee;");
        headerRight.getChildren().addAll(systemName, tsLabel);

        header.getChildren().addAll(headerLeft, spacer, headerRight);
        root.getChildren().add(header);

        // ── Metadata box ──────────────────────────────────────────────────────
        GridPane meta = new GridPane();
        meta.setStyle("-fx-background-color: #f0f4f8; -fx-border-color: #bed2e6; -fx-border-width: 1; -fx-padding: 0;");
        meta.setHgap(0);
        meta.setVgap(0);
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(25);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(25);
        ColumnConstraints col3 = new ColumnConstraints(); col3.setPercentWidth(25);
        ColumnConstraints col4 = new ColumnConstraints(); col4.setPercentWidth(25);
        meta.getColumnConstraints().addAll(col1, col2, col3, col4);

        addMetaCell(meta, "Disaster Event",  disasterInfo,                   0, 0);
        addMetaCell(meta, "Disaster Type",   disaster.getDisasterType(),      1, 0);
        addMetaCell(meta, "Total Records",   records.size() + " records",     2, 0);
        addMetaCell(meta, "Generated On",    timestamp,                       3, 0);

        VBox.setMargin(meta, new Insets(10, 0, 14, 0));
        root.getChildren().add(meta);

        // ── Table header row ──────────────────────────────────────────────────
        HBox tableHeader = new HBox(0);
        tableHeader.setStyle("-fx-background-color: #2c3e50; -fx-padding: 5 6 5 6;");
        tableHeader.getChildren().addAll(
                colHeaderCell("#",               50),
                colHeaderCell("Beneficiary",    200),
                colHeaderCell("Disaster",       160),
                colHeaderCell("Severity",       120),
                colHeaderCell("Assessment Date",120),
                colHeaderCell("Verified By",    120),
                colHeaderCell("Reg Date",       100)
        );
        root.getChildren().add(tableHeader);

        // ── Data rows ─────────────────────────────────────────────────────────
        boolean alt = false;
        int rowNum = 1;
        for (DisasterDamageModel d : records) {
            String rowBg = alt ? "#f5f8fc" : "#ffffff";
            alt = !alt;

            HBox row = new HBox(0);
            row.setStyle("-fx-background-color: " + rowBg + "; -fx-padding: 4 6 4 6; " +
                    "-fx-border-color: transparent transparent #dce1e6 transparent; -fx-border-width: 0.5;");

            row.getChildren().addAll(
                    colDataCell(String.valueOf(rowNum++),                          50,  true),
                    colDataCell(nvl(d.getBeneficiaryFirstname()),                  200, false),
                    colDataCell(nvl(d.getDisasterName()),                          160, false),
                    colDataCell(nvl(d.getHouseDamageSeverity()),                   120, false),
                    colDataCell(nvl(d.getAssessmentDate()),                        120, false),
                    colDataCell(nvl(d.getVerifiedBy()),                            120, false),
                    colDataCell(nvl(d.getRegDate()),                               100, false)
            );
            root.getChildren().add(row);
        }

        // ── Totals row ────────────────────────────────────────────────────────
        HBox totalRow = new HBox();
        totalRow.setStyle("-fx-background-color: #e8f0f8; -fx-padding: 5 6 5 6;");
        Label totalLabel = new Label("Total Records: " + records.size());
        totalLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        totalRow.getChildren().add(totalLabel);
        VBox.setMargin(totalRow, new Insets(0, 0, 10, 0));
        root.getChildren().add(totalRow);

        // ── Closing note ──────────────────────────────────────────────────────
        Label note = new Label("This report was generated automatically by RespondPH. " +
                "Data reflects the latest disaster damage assessment records.");
        note.setStyle("-fx-font-size: 8; -fx-text-fill: #888; -fx-padding: 8 0 0 0;");
        note.setAlignment(Pos.CENTER);
        note.setMaxWidth(Double.MAX_VALUE);
        root.getChildren().add(note);

        return root;
    }

// ── Print layout helper cells ─────────────────────────────────────────────────

    private void addMetaCell(GridPane grid, String label, String value, int col, int row) {
        VBox cell = new VBox(2);
        String labelBg = "#e1ebf5";
        String valueBg = "#f5f8fc";
        cell.setStyle("-fx-background-color: " + valueBg + "; -fx-padding: 6 8 6 8; " +
                "-fx-border-color: #bed2e6; -fx-border-width: 0.5;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 9; -fx-font-weight: bold; -fx-text-fill: #283c5a; " +
                "-fx-background-color: " + labelBg + "; -fx-padding: 1 3 1 3;");
        Label val = new Label(value != null ? value : "—");
        val.setStyle("-fx-font-size: 9; -fx-text-fill: #283c5a;");
        val.setWrapText(true);
        cell.getChildren().addAll(lbl, val);
        grid.add(cell, col, row);
    }

    private Label colHeaderCell(String text, double width) {
        Label lbl = new Label(text);
        lbl.setMinWidth(width); lbl.setMaxWidth(width);
        lbl.setStyle("-fx-font-size: 9; -fx-font-weight: bold; -fx-text-fill: white;");
        lbl.setWrapText(false);
        return lbl;
    }

    private Label colDataCell(String text, double width, boolean centered) {
        Label lbl = new Label(text);
        lbl.setMinWidth(width); lbl.setMaxWidth(width);
        lbl.setStyle("-fx-font-size: 9; -fx-text-fill: #283c3c;");
        lbl.setWrapText(false);
        if (centered) lbl.setAlignment(Pos.CENTER);
        return lbl;
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "—" : s; }

    private String truncate(String s, int maxLength) {
        if (s == null) return "";
        return s.length() <= maxLength ? s : s.substring(0, maxLength - 3) + "...";
    }

    private void generateAndSavePDF(List<DisasterDamageModel> records, DisasterItem disaster) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Disaster Damage Report PDF");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));

        String disasterName = disaster.getDisasterId() == 0
                ? "All_Disasters"
                : disaster.getDisasterName().replaceAll("\\s+", "_");
        chooser.setInitialFileName("DisasterDamage_" + disasterName + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + ".pdf");

        File file = chooser.showSaveDialog(null);
        if (file == null) return;

        PdfProgressRunner.run(
                dialogStage,

                progress -> buildPDF(file, records, disaster, progress),

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


    private void buildPDF(File file, List<DisasterDamageModel> records,
                          DisasterItem disaster,
                          PdfProgressRunner.PdfProgressCallback progress) throws Exception {

        // A4 landscape fixed
        PageSize pageSize = PageSize.A4.rotate();
        float    pageWidth = pageSize.getWidth();

        progress.update(8.0, "Creating fonts...");
        PdfFont fontBold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont fontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm a"));

        String disasterInfo = disaster.getDisasterId() == 0
                ? "All Disasters"
                : disaster.getDisasterName() + " (" + disaster.getDisasterType() + ")";


        progress.update(15.0, "Building document (Pass 1 of 2)...");
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        PdfWriter   writer1 = new PdfWriter(baos);
        PdfDocument pdfDoc1 = new PdfDocument(writer1);
        Document    doc1    = new Document(pdfDoc1, pageSize);
        doc1.setMargins(36, 36, 36, 36);

        progress.update(20.0, "Writing report header...");
        doc1.add(new Paragraph("DISASTER DAMAGE ASSESSMENT REPORT")
                .setFont(fontBold).setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));

        doc1.add(new Paragraph(
                "Disaster: " + disasterInfo
                        + " | Generated: " + timestamp
                        + " | Total Records: " + records.size())
                .setFont(fontNormal).setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

        progress.update(28.0, "Writing table header...");
        Table table = new Table(UnitValue.createPercentArray(
                new float[]{5, 20, 15, 12, 12, 12, 12})).useAllAvailableWidth();

        for (String h : new String[]{ "ID", "Beneficiary", "Disaster", "Severity",
                "Assessment Date", "Verified By", "Reg Date" }) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(fontBold).setFontSize(9)
                            .setFontColor(COLOR_WHITE))
                    .setBackgroundColor(COLOR_HEADER_BG).setPadding(5));
        }

        boolean alt   = false;
        int     rank  = 1;
        int     total = records.size();

        for (DisasterDamageModel d : records) {
            double rowPct = 28.0 + ((rank / (double) total) * 42.0);
            if (rank % 10 == 0)
                progress.update(rowPct, "Writing row " + rank + " of " + total + "...");

            DeviceRgb rowBg = alt ? COLOR_ALT_ROW : COLOR_WHITE;
            alt = !alt;

            table.addCell(cellOf(String.valueOf(d.getBeneficiaryDisasterDamageId()), fontNormal, 8, rowBg));
            table.addCell(cellOf(d.getBeneficiaryFirstname(), fontNormal, 8, rowBg));
            table.addCell(cellOf(d.getDisasterName(),         fontNormal, 8, rowBg));
            table.addCell(cellOf(d.getHouseDamageSeverity(),  fontNormal, 8, rowBg));
            table.addCell(cellOf(d.getAssessmentDate(),       fontNormal, 8, rowBg));
            table.addCell(cellOf(d.getVerifiedBy(),           fontNormal, 8, rowBg));
            table.addCell(cellOf(d.getRegDate(),              fontNormal, 8, rowBg));
            rank++;
        }
        doc1.add(table);

        progress.update(72.0, "Writing summary...");
        doc1.add(new Paragraph(
                "\nSummary:\nTotal Records: " + records.size()
                        + "\nReport generated by RespondPH System")
                .setFont(fontNormal).setFontSize(9).setMarginTop(20));

        progress.update(75.0, "Finalizing first pass...");
        doc1.close();


        progress.update(78.0, "Counting pages...");
        int totalPages;
        try (PdfDocument counter = new PdfDocument(
                new com.itextpdf.kernel.pdf.PdfReader(
                        new java.io.ByteArrayInputStream(baos.toByteArray())))) {
            totalPages = counter.getNumberOfPages();
        }


        progress.update(80.0, "Stamping page numbers (Pass 2 of 2)...");
        PdfDocument pdfDoc2 = new PdfDocument(
                new com.itextpdf.kernel.pdf.PdfReader(
                        new java.io.ByteArrayInputStream(baos.toByteArray())),
                new PdfWriter(file));

        PdfFont stampFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        for (int i = 1; i <= totalPages; i++) {
            double stampPct = 80.0 + (i / (double) totalPages) * 16.0;
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

    private Cell cellOf(String text, PdfFont font, float size, DeviceRgb bg) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "").setFont(font).setFontSize(size))
                .setBackgroundColor(bg).setPadding(4);
    }

    // =========================================================================
    // DIALOGS
    // =========================================================================

    private void showAddDisasterDamageDialog() {
        try {
            AddDisasterDamageDialogController controller = DialogManager.getController(
                    "addDisasterDamage", AddDisasterDamageDialogController.class);
            controller.setDisasterDamageService(disasterDamageService);
            controller.setDisasterDamageController(this);
            DialogManager.show("addDisasterDamage");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load Add Disaster Damage dialog: " + e.getMessage());
        }
    }

    private void showEditDisasterDamageDialog(DisasterDamageModel disasterDamage) {
        try {
            EditDisasterDamageDialogController controller = DialogManager.getController(
                    "editDisasterDamage", EditDisasterDamageDialogController.class);
            controller.setDisasterDamageService(this.disasterDamageService);
            controller.setDisasterDamageController(this);

            DisasterDamageModel full = disasterDamageService.getDisasterDamageId(
                    disasterDamage.getBeneficiaryDisasterDamageId());
            if (full != null) {
                controller.setDisasterDamage(full);
            } else {
                AlertDialogManager.showError("Data Error",
                        "Unable to load disaster damage data. The record may have been deleted.");
                return;
            }
            DialogManager.show("editDisasterDamage");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load Edit Disaster Damage dialog: " + e.getMessage());
        }
    }

    private void deleteDisasterDamage(DisasterDamageModel disasterDamage) {
        if (disasterDamage == null || disasterDamage.getBeneficiaryDisasterDamageId() <= 0) {
            AlertDialogManager.showError("Invalid Selection",
                    "Disaster damage ID is missing or invalid. Please select a valid record.");
            return;
        }

        boolean confirm = AlertDialogManager.showConfirmation(
                "Delete Disaster Damage Record",
                "Are you sure you want to delete this disaster damage record?");

        if (confirm) {
            try {
                boolean success = disasterDamageService.deleteDisasterDamage(disasterDamage);
                if (success) {
                    int beneficiaryId = disasterDamage.getBeneficiaryId();
                    UpdateTrigger trigger = new UpdateTrigger();
                    boolean cascadeSuccess = trigger.triggerCascadeUpdateAfterDisasterDamageDelete(
                            beneficiaryId, disasterDamage.getDisasterId());

                    if (cascadeSuccess) {
                        AlertDialogManager.showSuccess("Success",
                                "Disaster damage record has been successfully deleted.\n" +
                                        "Household and aid scores have been automatically recalculated.");
                    } else {
                        AlertDialogManager.showWarning("Partial Success",
                                "Disaster damage record has been deleted, but score recalculation encountered issues.\n" +
                                        "Please check the console for details.");
                    }
                    disasterList.remove(disasterDamage);
                } else {
                    AlertDialogManager.showError("Delete Failed",
                            "Failed to delete disaster damage record. Please try again.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                AlertDialogManager.showError("Delete Error",
                        "An error occurred while deleting disaster damage record: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // TABLE SETUP
    // =========================================================================

    private void setupTableColumns() {
        damage_id.setCellValueFactory(new PropertyValueFactory<>("beneficiaryDisasterDamageId"));

        beneficiaryNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getBeneficiaryId() + " - " +
                                cellData.getValue().getBeneficiaryFirstname()));

        disasterColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getDisasterId() + " - " +
                                cellData.getValue().getDisasterType() + " (" +
                                cellData.getValue().getDisasterName() + ")"));

        damageSeverityColumn.setCellValueFactory(new PropertyValueFactory<>("houseDamageSeverity"));
        assessmentDateColumn.setCellValueFactory(new PropertyValueFactory<>("assessmentDate"));
        verifiedByColumn.setCellValueFactory(new PropertyValueFactory<>("verifiedBy"));
        registeredDateColumn.setCellValueFactory(new PropertyValueFactory<>("regDate"));
    }

    private void setupActionButtons() {
        Callback<TableColumn<DisasterDamageModel, Void>,
                TableCell<DisasterDamageModel, Void>> cellFactory = param -> new TableCell<>() {

            private final FontAwesomeIconView editIcon   = new FontAwesomeIconView(FontAwesomeIcon.EDIT);
            private final FontAwesomeIconView deleteIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
            private final Button editButton   = new Button("", editIcon);
            private final Button deleteButton = new Button("", deleteIcon);

            {
                editIcon.getStyleClass().add("edit-icon");
                deleteIcon.getStyleClass().add("delete-icon");
                editButton.getStyleClass().add("edit-button");
                deleteButton.getStyleClass().add("delete-button");

                editButton.setOnAction(event ->
                        showEditDisasterDamageDialog(getTableView().getItems().get(getIndex())));
                deleteButton.setOnAction(event ->
                        deleteDisasterDamage(getTableView().getItems().get(getIndex())));
            }

            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(10, editButton, deleteButton);
                    box.setAlignment(Pos.CENTER);
                    box.getStyleClass().add("action-buttons-container");
                    setGraphic(box);
                    setAlignment(Pos.CENTER);
                }
            }
        };
        actionsColumn.setCellFactory(cellFactory);
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
        return l.contains("pdf")
                || l.contains("fax")
                || l.contains("xps")
                || l.contains("onenote")
                || l.contains("microsoft print")
                || l.contains("microsoft document")
                || l.contains("send to")
                || l.contains("snagit")
                || l.contains("cutepdf")
                || l.contains("cute pdf")
                || l.contains("bullzip")
                || l.contains("dopdf")
                || l.contains("nitro")
                || l.contains("foxit")
                || l.contains("pdfcreator")
                || l.contains("primopdf")
                || l.contains("pdf24")
                || l.contains("adobe pdf");
    }



    private boolean isPrinterActiveWindowsWmic(String printerName) {
        try {
            String[] cmd = {
                    "wmic", "printer",
                    "where", "Name='" + printerName.replace("'", "\\'") + "'",
                    "get", "PrinterStatus,WorkOffline",
                    "/format:csv"
            };
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
            Process proc = Runtime.getRuntime().exec(
                    new String[]{ "lpstat", "-p", printer.getName() });
            String output;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(proc.getInputStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append(" ");
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

    private boolean isPrinterActiveWindows(String printerName) {
        try {
            String safeName = printerName.replace("'", "''");
            String[] cmd = {
                    "powershell", "-NoProfile", "-NonInteractive", "-Command",
                    "(Get-Printer -Name '" + safeName + "').IsOnline"
            };
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
            return isPrinterActiveWindowsWmic(printerName); // fallback
        } catch (Exception e) {
            return isPrinterActiveWindowsWmic(printerName);
        }
    }

    // =========================================================================
    // DISASTER ITEM
    // =========================================================================

    public static class DisasterItem {
        private final int    disasterId;
        private final String disasterName;
        private final String disasterType;

        public DisasterItem(int disasterId, String disasterName, String disasterType) {
            this.disasterId   = disasterId;
            this.disasterName = disasterName;
            this.disasterType = disasterType;
        }

        public int    getDisasterId()   { return disasterId; }
        public String getDisasterName() { return disasterName; }
        public String getDisasterType() { return disasterType; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return disasterId == ((DisasterItem) o).disasterId;
        }

        @Override public int hashCode() { return Objects.hash(disasterId); }

        @Override
        public String toString() {
            return disasterId == 0 ? disasterName : disasterName + " (" + disasterType + ")";
        }
    }
}