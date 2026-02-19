package com.ionres.respondph.aid.dialogs_controller;
import com.ionres.respondph.aid.AidDAO;
import com.ionres.respondph.aid.AidDAOImpl;
import com.ionres.respondph.aid.AidModel;
import com.ionres.respondph.aid.AidPrintService;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.disaster.DisasterDAO;
import com.ionres.respondph.disaster.DisasterDAOImpl;
import com.ionres.respondph.disaster.DisasterModelComboBox;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.util.DashboardRefresher;
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
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PrintAidDialogController {

    // ── Disaster & Aid selection ─────────────────────────────────────────────
    @FXML private ComboBox<DisasterModelComboBox> disasterComboBox;
    @FXML private ComboBox<String> aidNameComboBox;

    // ── Barangay filter ──────────────────────────────────────────────────────
    @FXML private CheckBox         useBarangayFilterCheckBox;
    @FXML private ComboBox<String> barangayComboBox;

    // ── General-aid toggle ───────────────────────────────────────────────────
    @FXML private CheckBox generalAidCheckBox;

    // ── Report-type radios ───────────────────────────────────────────────────
    @FXML private RadioButton beneficiaryListRadio;
    @FXML private RadioButton distributionSummaryRadio;

    // ── Print settings ───────────────────────────────────────────────────────
    @FXML private ComboBox<String> bondPaperSizeComboBox;
    @FXML private RadioButton      portraitRadio;
    @FXML private RadioButton      landscapeRadio;
    @FXML private Spinner<Integer> copiesSpinner;
    @FXML private CheckBox         includeHeaderCheckbox;
    @FXML private CheckBox         includeFooterCheckbox;
    @FXML private CheckBox         includePageNumbersCheckbox;

    // ── Summary ──────────────────────────────────────────────────────────────
    @FXML private HBox  aidSummary;
    @FXML private Label beneficiaryCountLabel;

    // ── Buttons ──────────────────────────────────────────────────────────────
    @FXML private Button previewBtn;
    @FXML private Button cancelBtn;
    @FXML private Button printBtn;
    @FXML private Button closeBtn;

    // ── Services / DAOs ──────────────────────────────────────────────────────
    private final DisasterDAO     disasterDAO  = new DisasterDAOImpl(DBConnection.getInstance());
    private final AidDAO          aidDAO       = new AidDAOImpl(DBConnection.getInstance());
    private final AidPrintService printService = new AidPrintService();
    private final Cryptography    cs           = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

    // ── State ────────────────────────────────────────────────────────────────
    private List<AidModel> allAidRecords = new ArrayList<>();

    private static final String ALL_BARANGAYS = "All Barangays";

    private Stage progressStage;
    private ProgressBar progressBar;
    private Label progressLabel;

    private static class PrintDialogData {
        List<AidModel> filteredRecords;
        String disasterName;
        String aidName;
    }

    private final ToggleGroup reportTypeGroup  = new ToggleGroup();
    private final ToggleGroup orientationGroup = new ToggleGroup();

    // ── PDF color palette (mirrors AddAidController) ─────────────────────────
    private static final DeviceRgb PDF_HEADER_BG = new DeviceRgb(44,  62,  80);
    private static final DeviceRgb PDF_WHITE     = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb PDF_HIGH      = new DeviceRgb(41,  128, 185);
    private static final DeviceRgb PDF_MODERATE  = new DeviceRgb(243, 156,  18);
    private static final DeviceRgb PDF_LOW       = new DeviceRgb(149, 165, 166);
    private static final DeviceRgb PDF_ROW_ALT   = new DeviceRgb(248, 249, 250);

    // =========================================================================
    //  INIT
    // =========================================================================

    @FXML
    public void initialize() {
        wireToggleGroups();
        wirePaperSizeDefault();
        wireComboListeners();
        wireButtonHandlers();
        loadData();

        DashboardRefresher.registerDisasterNameAndAidtypeName(this);
    }

    private void wireToggleGroups() {
        beneficiaryListRadio   .setToggleGroup(reportTypeGroup);
        distributionSummaryRadio.setToggleGroup(reportTypeGroup);
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

    private void wireComboListeners() {
        generalAidCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            disasterComboBox.setDisable(newVal);
            if (newVal) disasterComboBox.setValue(null);
            loadBarangays();
            updateAidSummary();
        });

        disasterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && generalAidCheckBox.isSelected())
                generalAidCheckBox.setSelected(false);
            loadBarangays();
            updateAidSummary();
        });

        aidNameComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            loadBarangays();
            updateAidSummary();
        });

        useBarangayFilterCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            barangayComboBox.setDisable(!newVal);
            if (!newVal) barangayComboBox.setValue(ALL_BARANGAYS);
            updateAidSummary();
        });

        barangayComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateAidSummary());

        barangayComboBox.setDisable(true);
    }

    private void wireButtonHandlers() {
        closeBtn  .setOnAction(e -> closeDialog());
        cancelBtn .setOnAction(e -> closeDialog());
        previewBtn.setOnAction(e -> handlePreview());
        printBtn.setOnAction(e -> showProgressAndOpenDialog());
    }



    private void loadData() {
        Task<Void> task = new Task<>() {
            private List<DisasterModelComboBox> disasters;
            private List<String>                aidNames;
            private List<AidModel>              aidRecords;

            @Override
            protected Void call() {
                disasters  = disasterDAO.findAll();
                aidNames   = aidDAO.getDistinctAidNames();
                aidRecords = aidDAO.getAllAidForTable();
                return null;
            }

            @Override
            protected void succeeded() {
                allAidRecords = aidRecords;
                disasterComboBox.setItems(FXCollections.observableArrayList(disasters));
                aidNameComboBox .setItems(FXCollections.observableArrayList(aidNames));
            }

            @Override
            protected void failed() {
                AlertDialogManager.showError("Load Error",
                        "Failed to load data: " + getException().getMessage());
            }
        };
        new Thread(task).start();
    }

    private void loadBarangays() {
        if (barangayComboBox == null) return;

        int    disasterId  = getSelectedDisasterId();
        String selectedAid = aidNameComboBox.getValue();

        if (disasterId == 0 || selectedAid == null || selectedAid.isBlank()) {
            List<String> all = aidDAO.getAllBarangays();
            all.add(0, ALL_BARANGAYS);
            barangayComboBox.setItems(FXCollections.observableArrayList(all));
            barangayComboBox.setValue(ALL_BARANGAYS);
            return;
        }

        List<String> barangays = aidDAO.getBarangaysByAidNameAndDisaster(disasterId, selectedAid);
        barangays.add(0, ALL_BARANGAYS);
        barangayComboBox.setItems(FXCollections.observableArrayList(barangays));
        barangayComboBox.setValue(ALL_BARANGAYS);
    }


    private void showProgressAndOpenDialog() {
        if (!validate()) return;

        String selectedAid = aidNameComboBox.getValue();
        int    disasterId  = getSelectedDisasterId();
        String disasterName = resolveDisasterName();

        if (selectedAid == null || selectedAid.isBlank()) {
            AlertDialogManager.showWarning("No Selection", "Please select an Aid Name.");
            return;
        }

        createProgressDialog("Preparing data for printing/export...");

        Task<PrintDialogData> task = new Task<>() {
            @Override
            protected PrintDialogData call() throws Exception {
                PrintDialogData dialogData = new PrintDialogData();

                updateProgress(0, 100);
                updateMessage("Filtering aid records...");

                List<AidModel> records = getFilteredAndSortedAidRecords(disasterId, selectedAid);

                if (records.isEmpty()) {
                    throw new Exception("No records found for the selected criteria.");
                }

                dialogData.filteredRecords = records;
                dialogData.disasterName = disasterName;
                dialogData.aidName = selectedAid;

                updateProgress(30, 100);

                if (isBarangayFilterActive()) {
                    updateMessage("Loading barangay data...");
                    List<AidModel> filtered = filterByBarangay(records, barangayComboBox.getValue());
                    updateProgress(50, 100);
                } else {
                    updateProgress(50, 100);
                }

                updateMessage("Detecting connected printers...");
                Thread.sleep(300);

                ObservableList<PrinterEntry> activePrinters = buildPrinterEntries()
                        .filtered(PrinterEntry::isActive);

                updateProgress(80, 100);

                // Step 4: Prepare dialog data (80-95%)
                updateMessage("Preparing dialog components...");
                Thread.sleep(300);
                updateProgress(95, 100);

                // Step 5: Finalize (95-100%)
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
                    dialogData.filteredRecords,
                    dialogData.disasterName,
                    dialogData.aidName
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


    private void closeProgressDialog() {
        if (progressStage != null) {
            Platform.runLater(() -> {
                progressStage.close();
                progressStage = null;
            });
        }
    }

    private void updateAidSummary() {
        String selectedAid = aidNameComboBox.getValue();

        if (selectedAid == null || selectedAid.isBlank()) {
            beneficiaryCountLabel.setText("Beneficiaries: 0");
            hideAidSummary();
            printBtn  .setDisable(true);
            previewBtn.setDisable(true);
            return;
        }

        // ADD THIS GUARD — don't filter if records haven't loaded yet
        if (allAidRecords == null || allAidRecords.isEmpty()) {
            beneficiaryCountLabel.setText("Beneficiaries: loading...");
            showAidSummary();
            printBtn  .setDisable(true);
            previewBtn.setDisable(true);
            return;
        }

        int            disasterId = getSelectedDisasterId();
        List<AidModel> records    = getFilteredAndSortedAidRecords(disasterId, selectedAid);

        boolean isGeneralAid = generalAidCheckBox.isSelected() || disasterId == 0;
        String  contextInfo  = isGeneralAid ? " (General Aid)" : "";
        String  barangayInfo = isBarangayFilterActive()
                ? " | Barangay: " + barangayComboBox.getValue() : "";

        beneficiaryCountLabel.setText("Beneficiaries: " + records.size() + contextInfo + barangayInfo);
        showAidSummary();
        printBtn  .setDisable(records.isEmpty());
        previewBtn.setDisable(records.isEmpty());
    }

    private void showAidSummary() { aidSummary.setVisible(true);  aidSummary.setManaged(true);  }
    private void hideAidSummary() { aidSummary.setVisible(false); aidSummary.setManaged(false); }

    // =========================================================================
    //  VALIDATION
    // =========================================================================

    private boolean validate() {
        if (aidNameComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Validation", "Please select an Aid Name.");
            return false;
        }
        if (bondPaperSizeComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Validation", "Please select a paper size.");
            return false;
        }
        return true;
    }

    // =========================================================================
    //  PREVIEW
    // =========================================================================

    private void handlePreview() {
        if (!validate()) return;

        String         selectedAid  = aidNameComboBox.getValue();
        int            disasterId   = getSelectedDisasterId();
        String         disasterName = resolveDisasterName();
        List<AidModel> records      = getFilteredAndSortedAidRecords(disasterId, selectedAid);

        if (records.isEmpty()) {
            AlertDialogManager.showWarning("No Data", "No records found for the selected criteria.");
            return;
        }

        configurePrintService();

        VBox content = distributionSummaryRadio.isSelected()
                ? printService.buildDistributionSummary(disasterName, selectedAid, records)
                : printService.buildBeneficiaryList(disasterName, selectedAid, records);

        if (content == null) return;

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 20;");

        Stage previewStage = new Stage();
        previewStage.initModality(Modality.APPLICATION_MODAL);
        previewStage.setTitle("Preview – " + getSelectedReportType());
        previewStage.setScene(new Scene(scrollPane, 800, 900));
        previewStage.show();
    }

    // =========================================================================
    //  PRINT / EXPORT  —  output-format dialog (PDF or Printer)
    // =========================================================================

    /**
     * Opens the output-format dialog.  The user chooses:
     *   • Save as PDF  → FileChooser → iText 7 PDF
     *   • Send to Printer → ComboBox of ACTIVE (connected) printers only.
     *     If no active printers are found → AlertDialog "Not Connected in printer".
     */
    private void handlePrintOrExport() {
        if (!validate()) return;

        String         selectedAid  = aidNameComboBox.getValue();
        int            disasterId   = getSelectedDisasterId();
        String         disasterName = resolveDisasterName();
        List<AidModel> records      = getFilteredAndSortedAidRecords(disasterId, selectedAid);

        if (records.isEmpty()) {
            AlertDialogManager.showWarning("No Data", "No records found for the selected criteria.");
            return;
        }

        showOutputFormatDialog(records, disasterName, selectedAid);
    }

    /**
     * Output-format dialog:
     *
     * ┌─ Output Format ──────────────────────────────────────────────┐
     * │  ○ Save as PDF                                               │
     * │  ○ Send to Printer                                           │
     * │     ┌──────────────────────────────────────────────────────┐ │
     * │     │ Select Printer:  [ComboBox — active printers only]   │ │
     * │     └──────────────────────────────────────────────────────┘ │
     * └──────────────────────────────────────────────────────────────┘
     *
     * ComboBox rules (identical to AddAidController):
     *  • Virtual/software printers (PDF, FAX, XPS, OneNote…) are excluded.
     *  • Only ACTIVE (physically connected) printers appear.
     *  • Each item shows the printer name only — no badges, no icons.
     *  • If NO active printers exist when "Send to Printer" is selected:
     *      - Revert radio back to "Save as PDF"
     *      - Show Alert: title="Printer Not Connected",
     *                    header="Not Connected in printer",
     *                    body="No connected printer was detected…"
     */
    private void showOutputFormatDialog(List<AidModel> records,
                                        String disasterName,
                                        String aidName) {
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

        // Pre-select: default active printer → first active printer
        activeEntries.stream()
                .filter(PrinterEntry::isDefault)
                .findFirst()
                .or(() -> activeEntries.isEmpty()
                        ? Optional.empty()
                        : Optional.of(activeEntries.get(0)))
                .ifPresent(printerComboBox::setValue);

        // ── Printer section (hidden until "Send to Printer" chosen) ───────────
        Separator sep = new Separator();
        Label printerLbl = new Label("Select Printer:");
        printerLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        VBox printerSection = new VBox(7);
        printerSection.getChildren().addAll(sep, printerLbl, printerComboBox);
        printerSection.setVisible(false);
        printerSection.setManaged(false);

        // ── Radio toggle: show ComboBox OR alert if no active printers ─────────
        outputGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == printerRadio) {
                if (activeEntries.isEmpty()) {
                    // Revert to PDF and alert user
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
                generateAndSavePDF(records, disasterName, aidName);
            } else {
                PrinterEntry chosen = printerComboBox.getValue();
                if (chosen == null) {
                    AlertDialogManager.showWarning("No Printer Selected",
                            "Please select a printer from the list.");
                    return;
                }
                configurePrintService();
                sendToPrinterSilently(records, disasterName, aidName, chosen.getPrinter());
            }
        });
    }

    private void sendToPrinterSilently(List<AidModel> records,
                                       String disasterName,
                                       String aidName,
                                       Printer targetPrinter) {
        configurePrintService();

        // Build the JavaFX content node
        VBox content = distributionSummaryRadio.isSelected()
                ? printService.buildDistributionSummary(disasterName, aidName, records)
                : printService.buildBeneficiaryList(disasterName, aidName, records);

        if (content == null) {
            AlertDialogManager.showError("Print Error", "Failed to build report content.");
            return;
        }

        PrinterJob job = (targetPrinter != null)
                ? PrinterJob.createPrinterJob(targetPrinter)
                : PrinterJob.createPrinterJob();

        if (job == null) {
            AlertDialogManager.showError("Print Error",
                    "Could not create a print job for the selected printer.\n"
                            + "Please check that the printer is connected and try again.");
            return;
        }

        Printer         printer     = job.getPrinter();
        Paper           paper       = resolvePaper(printer);
        PageOrientation orientation = landscapeRadio.isSelected()
                ? PageOrientation.LANDSCAPE : PageOrientation.PORTRAIT;
        double margin = 36;
        PageLayout pageLayout = printer.createPageLayout(
                paper, orientation, margin, margin, margin, margin);
        job.getJobSettings().setPageLayout(pageLayout);

        int copies = copiesSpinner.getValue();
        job.getJobSettings().setCopies(copies);

        int actualPageCount = calculatePageCount(records, pageLayout);
        job.getJobSettings().setPageRanges(new PageRange(1, actualPageCount));

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

    private Paper resolvePaper(Printer printer) {
        String selected = bondPaperSizeComboBox.getValue();
        if (selected == null) return Paper.A4;
        if (selected.startsWith("Letter")) return Paper.NA_LETTER;
        if (selected.startsWith("Legal"))  return Paper.LEGAL;
        return Paper.A4;
    }

    private int calculatePageCount(List<AidModel> records, PageLayout pageLayout) {
        if (records == null || records.isEmpty()) return 1;

        double usableHeight = pageLayout.getPrintableHeight() - 150;
        int    rowHeight    = 22; // each beneficiary row ~22px
        int    rowsPerPage  = Math.max(1, (int) (usableHeight / rowHeight));

        return Math.max(1, (int) Math.ceil((double) records.size() / rowsPerPage));
    }


    private void generateAndSavePDF(List<AidModel> records,
                                    String disasterName,
                                    String aidName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Aid Report as PDF");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));
        chooser.setInitialFileName("AidReport_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + ".pdf");

        Stage owner;
        try { owner = (Stage) printBtn.getScene().getWindow(); }
        catch (Exception e) { owner = null; }

        File file = chooser.showSaveDialog(owner);
        if (file == null) return;

        try {
            buildPDF(file, records, disasterName, aidName);
            AlertDialogManager.showInfo("PDF Saved",
                    "PDF successfully saved to:\n" + file.getAbsolutePath());

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.OPEN))
                    desktop.open(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("PDF Error", "Failed to generate PDF:\n" + e.getMessage());
        }
    }

    private void buildPDF(File file,
                          List<AidModel> records,
                          String disasterName,
                          String aidName) throws IOException {

        PdfFont fontBold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont fontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontMono   = PdfFontFactory.createFont(StandardFonts.COURIER);

        // Resolve paper size from the ComboBox selection
        PageSize pageSize = resolvePageSize();

        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(file));
        Document    doc    = new Document(pdfDoc, pageSize);
        doc.setMargins(36, 36, 54, 36);

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy  hh:mm a"));
        String reportType = getSelectedReportType();

        // ═════════════════════════════════════════════════════════════════════
        //  HEADER BAR
        // ═════════════════════════════════════════════════════════════════════
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth()
                .setMarginBottom(4);

        Cell titleCell = new Cell()
                .add(new Paragraph("Aid Report — " + reportType)
                        .setFont(fontBold).setFontSize(18).setFontColor(PDF_WHITE))
                .add(new Paragraph(aidName)
                        .setFont(fontNormal).setFontSize(11)
                        .setFontColor(new DeviceRgb(189, 215, 238)))
                .setBackgroundColor(PDF_HEADER_BG)
                .setPadding(14)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);

        Cell metaCell = new Cell()
                .add(new Paragraph("RespondPH")
                        .setFont(fontBold).setFontSize(13).setFontColor(PDF_WHITE)
                        .setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph(timestamp)
                        .setFont(fontNormal).setFontSize(8)
                        .setFontColor(new DeviceRgb(189, 215, 238))
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(PDF_HEADER_BG)
                .setPadding(14)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        headerTable.addCell(titleCell).addCell(metaCell);
        doc.add(headerTable);

        // ═════════════════════════════════════════════════════════════════════
        //  METADATA BOX
        // ═════════════════════════════════════════════════════════════════════
        Table metaTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setBackgroundColor(new DeviceRgb(240, 244, 248))
                .setBorder(new SolidBorder(new DeviceRgb(190, 210, 230), 1))
                .setMarginTop(10).setMarginBottom(14);

        addMetaRow(metaTable, "Aid Name",     aidName,                           fontBold, fontNormal);
        addMetaRow(metaTable, "Disaster",     disasterName,                      fontBold, fontNormal);
        addMetaRow(metaTable, "Barangay",     resolveBarangayLabel(),            fontBold, fontNormal);
        addMetaRow(metaTable, "Total Records",records.size() + " beneficiaries", fontBold, fontNormal);
        addMetaRow(metaTable, "Generated On", timestamp,                         fontBold, fontNormal);
        doc.add(metaTable);

        // ═════════════════════════════════════════════════════════════════════
        //  BENEFICIARY TABLE
        // ═════════════════════════════════════════════════════════════════════
        Paragraph tableTitle = new Paragraph("Beneficiary List")
                .setFont(fontBold).setFontSize(13)
                .setFontColor(PDF_HEADER_BG)
                .setMarginBottom(4);
        doc.add(tableTitle);

        Table table = new Table(UnitValue.createPercentArray(new float[]{5, 28, 18, 17, 16, 16}))
                .useAllAvailableWidth()
                .setMarginBottom(12);

        // Column headers
        for (String col : new String[]{ "#", "Beneficiary Name", "Aid Distributed", "Quantity", "Cost (₱)", "Score / Priority" }) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(col).setFont(fontBold).setFontSize(9).setFontColor(PDF_WHITE))
                    .setBackgroundColor(PDF_HEADER_BG)
                    .setPadding(5)
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        }

        // Data rows
        boolean alt = false;
        int rank = 1;
        for (AidModel aid : records) {
            DeviceRgb rowBg = alt
                    ? new DeviceRgb(245, 248, 252)
                    : new DeviceRgb(255, 255, 255);
            alt = !alt;

            String scoreInfo = extractScoreInfo(aid.getNotes());
            DeviceRgb scoreColor = resolveScoreColor(aid.getNotes());

            table.addCell(pdfCell(String.valueOf(rank++), fontMono,   8, rowBg, TextAlignment.CENTER));
            table.addCell(pdfCell(safeStr(aid.getBeneficiaryName()), fontNormal, 9, rowBg, TextAlignment.LEFT));
            table.addCell(pdfCell(safeStr(aid.getName()),            fontNormal, 9, rowBg, TextAlignment.LEFT));
            table.addCell(pdfCell(String.valueOf(aid.getQuantity()), fontMono,   9, rowBg, TextAlignment.CENTER));
            table.addCell(pdfCell(String.format("%.2f", aid.getCost()), fontMono, 9, rowBg, TextAlignment.RIGHT));

            // Score cell with priority-colour accent
            table.addCell(new Cell()
                    .add(new Paragraph(scoreInfo).setFont(fontMono).setFontSize(8)
                            .setFontColor(scoreColor).setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(rowBg)
                    .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(5).setPaddingRight(5)
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(new DeviceRgb(220, 225, 230), 0.5f)));
        }
        doc.add(table);

        // ═════════════════════════════════════════════════════════════════════
        //  DISTRIBUTION SUMMARY FOOTER
        // ═════════════════════════════════════════════════════════════════════
        doc.add(new Paragraph(" ").setMarginTop(6).setMarginBottom(0));
        LineSeparator sep = new LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f));
        sep.setStrokeColor(new DeviceRgb(190, 200, 210));
        sep.setMarginBottom(8);
        doc.add(sep);

        doc.add(new Paragraph("Distribution Summary")
                .setFont(fontBold).setFontSize(13).setFontColor(PDF_HEADER_BG)
                .setMarginBottom(6));

        // Totals
        double   totalQty  = records.stream().mapToDouble(AidModel::getQuantity).sum();
        double totalCost = records.stream().mapToDouble(AidModel::getCost).sum();
        double avgScore  = records.stream()
                .mapToDouble(a -> extractScore(a.getNotes())).average().orElse(0.0);

        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{50, 25, 25}))
                .useAllAvailableWidth()
                .setMarginBottom(14);

        for (String col : new String[]{ "Metric", "Value", "" }) {
            summaryTable.addHeaderCell(new Cell()
                    .add(new Paragraph(col).setFont(fontBold).setFontSize(10).setFontColor(PDF_WHITE))
                    .setBackgroundColor(PDF_HEADER_BG).setPadding(6)
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        }

        DeviceRgb sumBg1 = new DeviceRgb(235, 242, 250);
        DeviceRgb sumBg2 = new DeviceRgb(245, 248, 252);
        addSummaryRow(summaryTable, "Total Beneficiaries",  String.valueOf(records.size()), "",  fontBold, fontNormal, sumBg1);
        addSummaryRow(summaryTable, "Total Quantity",        String.format("%.0f", totalQty)  ,       "",  fontNormal, fontNormal, sumBg2);
        addSummaryRow(summaryTable, "Total Cost",            String.format("₱%.2f", totalCost), "", fontNormal, fontNormal, sumBg1);
        addSummaryRow(summaryTable, "Average Priority Score",String.format("%.3f", avgScore),  "",  fontNormal, fontNormal, sumBg2);
        doc.add(summaryTable);

        // Closing note
        doc.add(new Paragraph(
                "This report was generated automatically by RespondPH. "
                        + "Beneficiary data reflects the latest aid distribution records.")
                .setFont(fontNormal).setFontSize(8)
                .setFontColor(new DeviceRgb(120, 130, 140))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(8));

        // Page numbers
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

    // =========================================================================
    //  PDF HELPER METHODS
    // =========================================================================

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

    private void addSummaryRow(Table table, String label, String val1, String val2,
                               PdfFont labelFont, PdfFont valFont, DeviceRgb bg) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(labelFont).setFontSize(9)
                        .setFontColor(new DeviceRgb(30, 50, 70))).setBackgroundColor(bg).setPadding(6)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(new DeviceRgb(210, 218, 226), 0.5f)));
        table.addCell(new Cell().add(new Paragraph(val1).setFont(valFont).setFontSize(9)
                        .setFontColor(new DeviceRgb(30, 50, 70))).setBackgroundColor(bg).setPadding(6)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(new DeviceRgb(210, 218, 226), 0.5f)));
        table.addCell(new Cell().add(new Paragraph(val2).setFont(valFont).setFontSize(9)
                        .setFontColor(new DeviceRgb(30, 50, 70))).setBackgroundColor(bg).setPadding(6)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(new DeviceRgb(210, 218, 226), 0.5f)));
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

    /** Resolve paper size from the ComboBox selection. */
    private PageSize resolvePageSize() {
        String sel = bondPaperSizeComboBox.getValue();
        if (sel == null) return PageSize.A4;
        if (sel.startsWith("Letter")) return PageSize.LETTER;
        if (sel.startsWith("Legal"))  return PageSize.LEGAL;
        return PageSize.A4;
    }

    /** Returns the barangay label for the metadata box. */
    private String resolveBarangayLabel() {
        if (isBarangayFilterActive()) return barangayComboBox.getValue();
        return "All Barangays";
    }

    /** Derives a priority colour from the notes field. */
    private DeviceRgb resolveScoreColor(String notes) {
        if (notes == null) return PDF_LOW;
        String lower = notes.toLowerCase();
        if (lower.contains("high"))     return PDF_HIGH;
        if (lower.contains("moderate")) return PDF_MODERATE;
        return PDF_LOW;
    }

    private String safeStr(String s) { return s != null ? s : ""; }

    // =========================================================================
    //  PRINTER DETECTION  (identical logic to AddAidController)
    // =========================================================================

    /** Inner class — wraps a Printer with active/default metadata. */
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

    /**
     * Builds the full printer list sorted: default-active first,
     * then other active (alphabetical), then inactive (alphabetical).
     */
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

    /**
     * Master active-check:
     *  1. Virtual/software printers → always FALSE (excluded from list).
     *  2. Windows → PowerShell IsOnline, fallback WMIC PrinterStatus.
     *  3. Linux/macOS → CUPS lpstat, fallback javax.print PrinterState.
     *  4. Any ambiguity → FALSE (fail-safe).
     */
    private boolean isPrinterActive(Printer printer) {
        if (printer == null) return false;
        if (isVirtualPrinter(printer.getName())) return false;
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win")
                ? isPrinterActiveWindows(printer.getName())
                : isPrinterActiveUnix(printer);
    }

    /**
     * Returns TRUE for any software / virtual printer (no physical hardware).
     * Matches: pdf, fax, xps, onenote, microsoft print/document,
     *          send to, snagit, cutepdf, bullzip, dopdf, nitro,
     *          foxit, pdfcreator, primopdf, pdf24, adobe pdf.
     */
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

    /** Windows: PowerShell Get-Printer IsOnline → WMIC fallback. */
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
            return isPrinterActiveWindowsWmic(printerName);

        } catch (Exception e) {
            return isPrinterActiveWindowsWmic(printerName);
        }
    }

    /**
     * Windows WMIC fallback — queries PrinterStatus + WorkOffline.
     * Only status 3 (Idle), 4 (Printing), 5 (Warmup) → ACTIVE.
     * All other statuses or WorkOffline=TRUE → FALSE (fail-safe).
     */
    private boolean isPrinterActiveWindowsWmic(String printerName) {
        try {
            String safeName = printerName.replace("'", "\\'");
            String[] cmd = {
                    "wmic", "printer",
                    "where", "Name='" + safeName + "'",
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

    /** Linux/macOS: CUPS lpstat → javax.print fallback. */
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
    //  UTILITY METHODS
    // =========================================================================

    private void configurePrintService() {
        printService.setPaperSize(bondPaperSizeComboBox.getValue());
        printService.setLandscape(landscapeRadio.isSelected());
        printService.setCopies(copiesSpinner.getValue());
        printService.setIncludeHeader(includeHeaderCheckbox.isSelected());
        printService.setIncludeFooter(includeFooterCheckbox.isSelected());
        printService.setIncludePageNumbers(includePageNumbersCheckbox.isSelected());
    }

    private String getSelectedReportType() {
        return distributionSummaryRadio.isSelected() ? "Distribution Summary" : "Beneficiary List";
    }

    private int getSelectedDisasterId() {
        if (generalAidCheckBox.isSelected()) return 0;
        DisasterModelComboBox d = disasterComboBox.getValue();
        return d != null ? d.getDisasterId() : 0;
    }

    private String resolveDisasterName() {
        if (generalAidCheckBox.isSelected()) return "General Aid";
        DisasterModelComboBox d = disasterComboBox.getValue();
        return d != null ? d.getDisasterName() : "General Aid";
    }

    private List<AidModel> getFilteredAndSortedAidRecords(int disasterId, String aidName) {
        List<AidModel> filtered = allAidRecords.stream()
                .filter(aid -> {
                    boolean disasterMatch = (disasterId == 0) || (aid.getDisasterId() == disasterId);
                    boolean nameMatch     = aid.getName() != null && aid.getName().equals(aidName);
                    return disasterMatch && nameMatch;
                })
                .collect(Collectors.toList());

        if (isBarangayFilterActive())
            filtered = filterByBarangay(filtered, barangayComboBox.getValue());

        return sortByKMeansScore(filtered);
    }

    private boolean isBarangayFilterActive() {
        return useBarangayFilterCheckBox.isSelected()
                && barangayComboBox.getValue() != null
                && !barangayComboBox.getValue().equals(ALL_BARANGAYS);
    }

    private List<AidModel> filterByBarangay(List<AidModel> records, String barangay) {
        Set<Integer> ids = getBeneficiaryIdsByBarangay(barangay);
        return records.stream()
                .filter(a -> ids.contains(a.getBeneficiaryId()))
                .collect(Collectors.toList());
    }

    private Set<Integer> getBeneficiaryIdsByBarangay(String barangay) {
        Set<Integer> ids = new HashSet<>();
        String sql = "SELECT beneficiary_id, barangay FROM beneficiary WHERE barangay IS NOT NULL";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    String decrypted = cs.decryptWithOneParameter(rs.getString("barangay"));
                    if (barangay.equals(decrypted)) ids.add(rs.getInt("beneficiary_id"));
                } catch (Exception ignored) { }
            }
        } catch (Exception e) {
            AlertDialogManager.showError("DB Error", "Error fetching barangay data: " + e.getMessage());
        }
        return ids;
    }

    private List<AidModel> sortByKMeansScore(List<AidModel> records) {
        return records.stream()
                .sorted((a, b) -> Double.compare(extractScore(b.getNotes()), extractScore(a.getNotes())))
                .collect(Collectors.toList());
    }

    private double extractScore(String notes) {
        if (notes == null || notes.isEmpty()) return 0.0;
        try {
            Matcher m = Pattern.compile("Score:\\s*([0-9]+\\.?[0-9]*)").matcher(notes);
            return m.find() ? Double.parseDouble(m.group(1)) : 0.0;
        } catch (Exception e) { return 0.0; }
    }

    private String extractScoreInfo(String notes) {
        if (notes == null || notes.isEmpty()) return "N/A";
        try {
            Matcher sm = Pattern.compile("Score:\\s*([0-9]+\\.?[0-9]*)").matcher(notes);
            Matcher cm = Pattern.compile("Cluster:\\s*([0-9]+)").matcher(notes);
            Matcher pm = Pattern.compile("Priority:\\s*([A-Za-z]+)").matcher(notes);
            String score    = sm.find() ? sm.group(1) : "N/A";
            String cluster  = cm.find() ? cm.group(1) : "N/A";
            String priority = pm.find() ? pm.group(1) : "N/A";
            return String.format("%s (C%s) - %s", score, cluster, priority);
        } catch (Exception e) { return "N/A"; }
    }

    private void closeDialog() {
        try { ((Stage) closeBtn.getScene().getWindow()).close(); }
        catch (Exception ignored) { }
    }



    public void refreshComboBoxes() {
        Task<Void> task = new Task<>() {
            private List<DisasterModelComboBox> disasters;
            private List<String>                aidNames;
            private List<AidModel>              freshAidRecords; // ADD THIS

            @Override
            protected Void call() {
                disasters        = disasterDAO.findAll();
                aidNames         = aidDAO.getDistinctAidNames();
                freshAidRecords  = aidDAO.getAllAidForTable(); // ADD THIS
                return null;
            }

            @Override
            protected void succeeded() {
                allAidRecords = freshAidRecords; // ADD THIS — replace stale cache

                DisasterModelComboBox prev    = disasterComboBox.getValue();
                String                prevAid = aidNameComboBox.getValue();

                disasterComboBox.setItems(FXCollections.observableArrayList(disasters));
                aidNameComboBox .setItems(FXCollections.observableArrayList(aidNames));

                if (prev != null) disasters.stream()
                        .filter(d -> d.getDisasterId() == prev.getDisasterId())
                        .findFirst().ifPresent(disasterComboBox::setValue);

                if (prevAid != null) aidNameComboBox.setValue(prevAid);

                loadBarangays();
                updateAidSummary();
            }

            @Override
            protected void failed() {
                AlertDialogManager.showError("Refresh Error",
                        "Failed to refresh: " + getException().getMessage());
            }
        };
        new Thread(task).start();
    }
}