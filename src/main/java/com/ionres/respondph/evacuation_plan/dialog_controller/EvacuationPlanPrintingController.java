package com.ionres.respondph.evacuation_plan.dialog_controller;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.evacuation_plan.EvacuationPlanModel;
import com.ionres.respondph.evacuation_plan.EvacuationPlanService;
import com.ionres.respondph.evacuation_plan.EvacuationPlanServiceImpl;
import com.ionres.respondph.evac_site.EvacSiteDAO;
import com.ionres.respondph.evac_site.EvacSiteDAOServiceImpl;
import com.ionres.respondph.evac_site.EvacSiteModel;
import com.ionres.respondph.util.AlertDialogManager;
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
import java.util.stream.Collectors;

public class EvacuationPlanPrintingController {

    // ── Page Layout Constants ───────────────────────────────────────────────
    private static final double PAGE_WIDTH    = 612;
    private static final double MARGIN        = 36;
    private static final double CONTENT_WIDTH = PAGE_WIDTH - (2 * MARGIN);

    // ── PDF color palette ─────────────────────────────────────────────────────
    private static final DeviceRgb PDF_HEADER_BG = new DeviceRgb(44,  62,  80);
    private static final DeviceRgb PDF_WHITE     = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb PDF_BLUE      = new DeviceRgb(41,  128, 185);
    private static final DeviceRgb PDF_GREEN     = new DeviceRgb(39,  174,  96);
    private static final DeviceRgb PDF_RED       = new DeviceRgb(192,  57,  43);
    private static final DeviceRgb PDF_ORANGE    = new DeviceRgb(230, 126,  34);

    // ── Disaster selection ──────────────────────────────────────────────────
    @FXML private ComboBox<String> disasterComboBox;

    // ── Evacuation site selection ───────────────────────────────────────────
    @FXML private ComboBox<String> evacuationSiteComboBox;
    @FXML private HBox             capacitySummary;
    @FXML private Label            capacityLabel;

    // ── Report type ─────────────────────────────────────────────────────────
    @FXML private RadioButton beneficiaryListRadio;
    @FXML private RadioButton evacuationPlanRadio;
    @FXML private RadioButton capacityReportRadio;

    // ── Print settings ───────────────────────────────────────────────────────
    @FXML private ComboBox<String> bondPaperSizeComboBox;
    @FXML private RadioButton      portraitRadio;
    @FXML private RadioButton      landscapeRadio;
    @FXML private Spinner<Integer> copiesSpinner;
    @FXML private CheckBox         includeHeaderCheckbox;
    @FXML private CheckBox         includeFooterCheckbox;
    @FXML private CheckBox         includePageNumbersCheckbox;

    @FXML private Button previewBtn;
    @FXML private Button cancelBtn;
    @FXML private Button printBtn;
    @FXML private Button closeBtn;

    private Stage progressStage;
    private ProgressBar progressBar;
    private Label progressLabel;


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

    private static class PrintDialogData {
        List<EvacuationPlanModel> filteredPlans;
        String disasterName;
        String siteName;
        int disasterId;
        int siteId;
    }

    // =========================================================================
    //  INIT
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
        // printBtn now opens the output-format dialog (PDF or Printer)
        printBtn.setOnAction(e -> showProgressAndOpenDialog());
    }

    // =========================================================================
    //  DATA LOADING  (unchanged)
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

    /**
     * Show progress bar while loading data for print/PDF dialog
     */
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

        // Create and show progress dialog
        createProgressDialog("Preparing data for printing/export...");

        Task<PrintDialogData> task = new Task<>() {
            @Override
            protected PrintDialogData call() throws Exception {
                PrintDialogData dialogData = new PrintDialogData();

                // Step 1: Filter plans (0-40%)
                updateProgress(0, 100);
                updateMessage("Filtering evacuation plans...");

                List<EvacuationPlanModel> filtered = allPlans.stream()
                        .filter(p -> disasterId.equals(p.getDisasterId()) && siteId.equals(p.getEvacSiteId()))
                        .sorted(Comparator.comparing(EvacuationPlanModel::getBeneficiaryName))
                        .collect(Collectors.toList());

                if (filtered.isEmpty()) {
                    throw new Exception("No records found for the selected disaster and evacuation site.");
                }

                dialogData.filteredPlans = filtered;
                dialogData.disasterName = disasterName;
                dialogData.siteName = siteName;
                dialogData.disasterId = disasterId;
                dialogData.siteId = siteId;

                updateProgress(40, 100);

                // Step 2: Detect printers (40-70%)
                updateMessage("Detecting connected printers...");
                Thread.sleep(300); // Small delay for visual feedback

                // Just trigger printer detection - will be used later
                ObservableList<PrinterEntry> activePrinters = buildPrinterEntries()
                        .filtered(PrinterEntry::isActive);

                updateProgress(70, 100);

                // Step 3: Prepare dialog data (70-90%)
                updateMessage("Preparing dialog components...");
                Thread.sleep(300);
                updateProgress(90, 100);

                // Step 4: Finalize (90-100%)
                updateMessage("Ready to open dialog...");
                Thread.sleep(200);
                updateProgress(100, 100);

                return dialogData;
            }
        };

        task.setOnSucceeded(e -> {
            closeProgressDialog();
            PrintDialogData dialogData = task.getValue();
            showOutputFormatDialog(
                    dialogData.filteredPlans,
                    dialogData.disasterName,
                    dialogData.siteName,
                    dialogData.disasterId,
                    dialogData.siteId
            );
        });

        task.setOnFailed(e -> {
            closeProgressDialog();
            Throwable exception = task.getException();
            if (exception != null && exception.getMessage() != null) {
                if (exception.getMessage().contains("No records found")) {
                    AlertDialogManager.showWarning("No Data", exception.getMessage());
                } else {
                    AlertDialogManager.showError("Error",
                            "Failed to load data: " + exception.getMessage());
                }
            } else {
                AlertDialogManager.showError("Error", "An unknown error occurred.");
            }
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

    /**
     * Close progress dialog
     */
    private void closeProgressDialog() {
        if (progressStage != null) {
            Platform.runLater(() -> {
                progressStage.close();
                progressStage = null;
            });
        }
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
                                (a, b) -> a,
                                LinkedHashMap::new));
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

    // =========================================================================
    //  VALIDATION  (unchanged)
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

    // =========================================================================
    //  PREVIEW  (unchanged)
    // =========================================================================

    private void handlePreview() {
        if (!validate()) return;
        VBox content = buildReportContent(getSelectedReportType());
        if (content == null) return;

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 20;");

        Stage previewStage = new Stage();
        previewStage.initModality(Modality.APPLICATION_MODAL);
        previewStage.setTitle("Preview – " + getSelectedReportType());
        previewStage.setScene(new Scene(scrollPane, 650, 900));
        previewStage.show();
    }


    private void handlePrintOrExport() {
        if (!validate()) return;

        String  disasterName = disasterComboBox.getValue();
        String  siteName     = evacuationSiteComboBox.getValue();
        Integer disasterId   = disasterNameToId.get(disasterName);
        Integer siteId       = evacSiteNameToId.get(siteName);

        if (disasterId == null || siteId == null) {
            AlertDialogManager.showWarning("No Selection", "Please select a disaster and site.");
            return;
        }

        List<EvacuationPlanModel> filtered = allPlans.stream()
                .filter(p -> disasterId.equals(p.getDisasterId()) && siteId.equals(p.getEvacSiteId()))
                .sorted(Comparator.comparing(EvacuationPlanModel::getBeneficiaryName))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            AlertDialogManager.showWarning("No Data",
                    "No records found for the selected disaster and evacuation site.");
            return;
        }

        showOutputFormatDialog(filtered, disasterName, siteName, disasterId, siteId);
    }

    private void showOutputFormatDialog(List<EvacuationPlanModel> filtered,
                                        String disasterName, String siteName,
                                        int disasterId, int siteId) {
        if (filtered.isEmpty()) {
            AlertDialogManager.showWarning("No Data",
                    "No records found for the selected disaster and evacuation site.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Export / Print");
        dialog.setHeaderText("Choose output format:");

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setPrefWidth(460);

        // ── Output format radios ──────────────────────────────────────────────
        Label outputLbl = new Label("Output Format:");
        outputLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        ToggleGroup outputGroup  = new ToggleGroup();
        RadioButton pdfRadio     = new RadioButton("Save as PDF");
        RadioButton printerRadio = new RadioButton("Send to Printer");
        pdfRadio.setToggleGroup(outputGroup);
        printerRadio.setToggleGroup(outputGroup);
        pdfRadio.setSelected(true);

        // ── Build active-only printer list ────────────────────────────────────
        ObservableList<PrinterEntry> allEntries    = buildPrinterEntries();
        ObservableList<PrinterEntry> activeEntries = FXCollections.observableArrayList(
                allEntries.filtered(PrinterEntry::isActive));

        // ── Printer ComboBox — name only, no badges ───────────────────────────
        ComboBox<PrinterEntry> printerComboBox = new ComboBox<>(activeEntries);
        printerComboBox.setPrefWidth(420);
        printerComboBox.setMaxWidth(Double.MAX_VALUE);

        Callback<ListView<PrinterEntry>, ListCell<PrinterEntry>> nameOnlyFactory =
                lv -> new ListCell<>() {
                    @Override
                    protected void updateItem(PrinterEntry item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) { setText(null); setGraphic(null); }
                        else {
                            setText(item.getPrinterName());
                            setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50;");
                            setGraphic(null);
                        }
                    }
                };
        printerComboBox.setCellFactory(nameOnlyFactory);
        printerComboBox.setButtonCell(nameOnlyFactory.call(null));

        // Pre-select: default active printer → first active printer
        activeEntries.stream()
                .filter(PrinterEntry::isDefault).findFirst()
                .or(() -> activeEntries.isEmpty()
                        ? Optional.empty() : Optional.of(activeEntries.get(0)))
                .ifPresent(printerComboBox::setValue);

        // ── Printer section (hidden until "Send to Printer" chosen) ───────────
        Separator  sep        = new Separator();
        Label      printerLbl = new Label("Select Printer:");
        printerLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        VBox printerSection = new VBox(7);
        printerSection.getChildren().addAll(sep, printerLbl, printerComboBox);
        printerSection.setVisible(false);
        printerSection.setManaged(false);

        // ── Radio toggle: show ComboBox or fire "Not Connected" alert ─────────
        outputGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == printerRadio) {
                if (activeEntries.isEmpty()) {
                    pdfRadio.setSelected(true);
                    Alert notConnected = new Alert(Alert.AlertType.ERROR);
                    notConnected.setTitle("Printer Not Connected");
                    notConnected.setHeaderText("Not Connected in printer");
                    notConnected.setContentText(
                            "No connected printer was detected on this system.\n\n"
                                    + "Please connect a printer and try again.");
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

        content.getChildren().addAll(outputLbl, pdfRadio, printerRadio, printerSection);
        dialog.getDialogPane().setContent(content);

        ButtonType okBtn = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result != okBtn) return;
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

        applyPageLayout(job);
        PageLayout pageLayout = job.getJobSettings().getPageLayout();

        if (!job.showPrintDialog(getOwnerWindow())) return;

        int     copies     = copiesSpinner.getValue();
        boolean allSuccess = true;
        for (int i = 0; i < copies; i++) {
            if (!job.printPage(pageLayout, content)) { allSuccess = false; break; }
        }

        if (allSuccess) {
            job.endJob();
            AlertDialogManager.showInfo("Success",
                    String.format("Document sent to printer: %s\n%d %s printed.",
                            job.getPrinter().getName(),
                            copies, copies == 1 ? "copy" : "copies"));
            closeDialog();
        } else {
            AlertDialogManager.showError("Print Error",
                    "An error occurred while printing. Please try again.");
        }
    }

    /** Applies the paper-size and orientation chosen by the user to the printer job. */
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

    // =========================================================================
    //  PDF GENERATION  ← NEW  (iText 7)
    // =========================================================================

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

        try {
            buildPDF(file, filtered, disasterName, siteName);
            AlertDialogManager.showInfo("PDF Saved",
                    "PDF successfully saved to:\n" + file.getAbsolutePath());
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.OPEN))
                    desktop.open(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("PDF Error",
                    "Failed to generate PDF:\n" + e.getMessage());
        }
    }

    /**
     * iText 7 PDF builder.
     *
     * Sections:
     *   1. Dark-navy header bar (report type | RespondPH + timestamp)
     *   2. Metadata box        (disaster, site, report type, total, generated on)
     *   3. Report body         (switches on getSelectedReportType())
     *   4. Closing note
     *   5. Page-number footer
     */
    private void buildPDF(File file,
                          List<EvacuationPlanModel> filtered,
                          String disasterName,
                          String siteName) throws IOException {

        PdfFont fontBold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont fontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontMono   = PdfFontFactory.createFont(StandardFonts.COURIER);

        PageSize pageSize = resolvePdfPageSize();
        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(file));
        Document    doc    = new Document(pdfDoc, pageSize);
        doc.setMargins(36, 36, 54, 36);

        String reportType = getSelectedReportType();
        String timestamp  = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy  hh:mm a"));

        // ── Header bar ────────────────────────────────────────────────────────
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth().setMarginBottom(4);

        headerTable.addCell(new Cell()
                .add(new Paragraph(reportType.toUpperCase())
                        .setFont(fontBold).setFontSize(18).setFontColor(PDF_WHITE))
                .add(new Paragraph("Barangay Disaster Risk Reduction and Management")
                        .setFont(fontNormal).setFontSize(10)
                        .setFontColor(new DeviceRgb(189, 215, 238)))
                .setBackgroundColor(PDF_HEADER_BG).setPadding(14)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));

        headerTable.addCell(new Cell()
                .add(new Paragraph("RespondPH")
                        .setFont(fontBold).setFontSize(13).setFontColor(PDF_WHITE)
                        .setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph(timestamp)
                        .setFont(fontNormal).setFontSize(8)
                        .setFontColor(new DeviceRgb(189, 215, 238))
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(PDF_HEADER_BG).setPadding(14)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE));

        doc.add(headerTable);

        // ── Metadata box ──────────────────────────────────────────────────────
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
        doc.add(metaTable);

        // ── Report body ───────────────────────────────────────────────────────
        switch (reportType) {
            case "Beneficiary List" ->
                    buildPdfBeneficiaryList(doc, filtered, disasterName, siteName, fontBold, fontNormal, fontMono);
            case "Evacuation Plan"  ->
                    buildPdfEvacuationPlan(doc, filtered, siteName, fontBold, fontNormal);
            case "Capacity Report"  ->
                    buildPdfCapacityReport(doc, filtered, siteName, fontBold, fontNormal);
        }

        // ── Closing note ──────────────────────────────────────────────────────
        doc.add(new Paragraph(
                "This report was generated automatically by RespondPH. "
                        + "Data reflects the latest evacuation plan records.")
                .setFont(fontNormal).setFontSize(8)
                .setFontColor(new DeviceRgb(120, 130, 140))
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(10));

        // ── Page numbers ──────────────────────────────────────────────────────
        int totalPages = pdfDoc.getNumberOfPages();
        for (int i = 1; i <= totalPages; i++) {
            doc.showTextAligned(
                    new Paragraph("Page " + i + " of " + totalPages)
                            .setFont(fontNormal).setFontSize(8)
                            .setFontColor(new DeviceRgb(150, 160, 170)),
                    pdfDoc.getPage(i).getPageSize().getWidth() / 2,
                    22, i, TextAlignment.CENTER, VerticalAlignment.BOTTOM, 0);
        }

        doc.close();
    }

    // ── PDF report-body builders ──────────────────────────────────────────────

    private void buildPdfBeneficiaryList(Document doc,
                                         List<EvacuationPlanModel> plans,
                                         String disasterName, String siteName,
                                         PdfFont fontBold, PdfFont fontNormal,
                                         PdfFont fontMono) {
        doc.add(new Paragraph("Beneficiary List")
                .setFont(fontBold).setFontSize(13).setFontColor(PDF_HEADER_BG)
                .setMarginBottom(4));

        Table table = new Table(UnitValue.createPercentArray(new float[]{6, 60, 34}))
                .useAllAvailableWidth().setMarginBottom(12);

        for (String col : new String[]{ "#", "Beneficiary Name", "Date Assigned" }) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(col).setFont(fontBold).setFontSize(9)
                            .setFontColor(PDF_WHITE))
                    .setBackgroundColor(PDF_HEADER_BG).setPadding(5)
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        }

        boolean alt = false;
        for (int i = 0; i < plans.size(); i++) {
            EvacuationPlanModel p = plans.get(i);
            DeviceRgb bg = alt ? new DeviceRgb(245, 248, 252) : new DeviceRgb(255, 255, 255);
            alt = !alt;
            table.addCell(pdfCell(String.valueOf(i + 1), fontMono,   8, bg, TextAlignment.CENTER));
            table.addCell(pdfCell(nvl(p.getBeneficiaryName()),  fontNormal, 9, bg, TextAlignment.LEFT));
            table.addCell(pdfCell(formatDate(p.getDateCreated()), fontNormal, 9, bg, TextAlignment.CENTER));
        }
        doc.add(table);
        doc.add(new Paragraph("Total Beneficiaries: " + plans.size())
                .setFont(fontBold).setFontSize(10).setFontColor(PDF_HEADER_BG).setMarginTop(4));
    }

    private void buildPdfEvacuationPlan(Document doc,
                                        List<EvacuationPlanModel> plans,
                                        String siteName,
                                        PdfFont fontBold, PdfFont fontNormal) {
        doc.add(new Paragraph("Evacuation Plan")
                .setFont(fontBold).setFontSize(13).setFontColor(PDF_HEADER_BG)
                .setMarginBottom(4));

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

    private void buildPdfCapacityReport(Document doc,
                                        List<EvacuationPlanModel> plans,
                                        String siteName,
                                        PdfFont fontBold, PdfFont fontNormal) {
        doc.add(new Paragraph("Capacity Report")
                .setFont(fontBold).setFontSize(13).setFontColor(PDF_HEADER_BG)
                .setMarginBottom(6));

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

        // Occupancy bar
        DeviceRgb barColor  = pct >= 100 ? PDF_RED : pct >= 80 ? PDF_ORANGE : PDF_GREEN;
        float     filledPct = (float) Math.min(pct, 100.0);

        Table barTable = new Table(UnitValue.createPercentArray(
                filledPct < 100 ? new float[]{ filledPct, 100 - filledPct }
                        : new float[]{ 100f }))
                .useAllAvailableWidth().setHeight(18).setMarginBottom(8);

        barTable.addCell(new Cell()
                .add(new Paragraph(String.format("%.0f%%", pct))
                        .setFont(fontBold).setFontSize(8).setFontColor(PDF_WHITE)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(barColor).setPadding(2)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));

        if (filledPct < 100) {
            barTable.addCell(new Cell()
                    .add(new Paragraph(""))
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
                .setFontColor(pct >= 80 ? PDF_RED : PDF_GREEN)
                .setMarginTop(4));
    }

    // ── PDF helper methods ────────────────────────────────────────────────────

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
    //  PRINTER DETECTION  ← NEW  (identical logic to AddAidController)
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

    /**
     * Returns TRUE only for real, physically connected hardware printers.
     * Virtual printers (PDF, FAX, XPS, OneNote…) always return FALSE.
     * Windows: PowerShell IsOnline → WMIC fallback.
     * Linux/macOS: CUPS lpstat → javax.print fallback.
     * Any ambiguity → FALSE (fail-safe).
     */
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
    //  EXISTING JAVAFX REPORT BUILDERS  (all unchanged from original)
    // =========================================================================

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
                colCell("#",                50,  true),
                colCell("Beneficiary Name", 340, true),
                colCell("Date Assigned",    150, true));
        root.getChildren().add(colHeader);

        for (int i = 0; i < plans.size(); i++) {
            EvacuationPlanModel p = plans.get(i);
            HBox row = styledRow(i % 2 == 0);
            row.getChildren().addAll(
                    colCell(String.valueOf(i + 1),         50,  false),
                    colCell(p.getBeneficiaryName(),         340, false),
                    colCell(formatDate(p.getDateCreated()), 150, false));
            root.getChildren().add(row);
        }

        root.getChildren().add(separator());
        Label totalLabel = new Label("Total Beneficiaries: " + plans.size());
        totalLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold;");
        root.getChildren().add(totalLabel);
        if (includeFooterCheckbox.isSelected())      root.getChildren().add(buildFooter(plans.size()));
        if (includePageNumbersCheckbox.isSelected())  root.getChildren().add(pageNumber(1));
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
        if (includeFooterCheckbox.isSelected())      root.getChildren().add(buildFooter(plans.size()));
        if (includePageNumbersCheckbox.isSelected())  root.getChildren().add(pageNumber(1));
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

        if (includeFooterCheckbox.isSelected())      root.getChildren().add(buildFooter(occupied));
        if (includePageNumbersCheckbox.isSelected())  root.getChildren().add(pageNumber(1));
        return root;
    }

    // ── JavaFX UI helpers (all unchanged) ─────────────────────────────────────

    private VBox printPage() {
        VBox page = new VBox(6);
        page.setPrefWidth(CONTENT_WIDTH); page.setMaxWidth(CONTENT_WIDTH);
        page.setAlignment(Pos.TOP_CENTER);
        page.setPadding(new Insets(20, 0, 20, 0));
        page.setStyle("-fx-background-color: white;");
        return page;
    }

    private VBox buildHeader(String reportTitle, String disasterName, String siteName) {
        VBox header = new VBox(4);
        header.setAlignment(Pos.CENTER);
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
        header.getChildren().addAll(republic, title, title2, rptLabel, disLabel, siteLabel, dateLabel);
        return header;
    }

    private HBox buildFooter(int totalRecords) {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 0, 0, 0));
        Label left  = new Label("Total Records: " + totalRecords);
        left.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label right = new Label("Prepared by BDRRMC  |  RespondPH System");
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