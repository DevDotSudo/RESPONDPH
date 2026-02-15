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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
    @FXML private ComboBox<String>                aidNameComboBox;

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
    private final DisasterDAO    disasterDAO  = new DisasterDAOImpl(DBConnection.getInstance());
    private final AidDAO         aidDAO       = new AidDAOImpl(DBConnection.getInstance());
    private final AidPrintService printService = new AidPrintService();
    private final Cryptography   cs           = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

    // ── State ────────────────────────────────────────────────────────────────
    private List<AidModel> allAidRecords = new ArrayList<>();

    private static final String ALL_BARANGAYS = "All Barangays";

    private final ToggleGroup reportTypeGroup  = new ToggleGroup();
    private final ToggleGroup orientationGroup = new ToggleGroup();

    // ────────────────────────────────────────────────────────────────────────
    //  INIT
    // ────────────────────────────────────────────────────────────────────────

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
        // General-aid toggle
        generalAidCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            disasterComboBox.setDisable(newVal);
            if (newVal) disasterComboBox.setValue(null);
            loadBarangays();
            updateAidSummary();
        });

        // Disaster selection
        disasterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && generalAidCheckBox.isSelected())
                generalAidCheckBox.setSelected(false);
            loadBarangays();
            updateAidSummary();
        });

        // Aid name selection
        aidNameComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            loadBarangays();
            updateAidSummary();
        });

        // Barangay filter checkbox
        useBarangayFilterCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            barangayComboBox.setDisable(!newVal);
            if (!newVal) barangayComboBox.setValue(ALL_BARANGAYS);
            updateAidSummary();
        });

        // Barangay selection
        barangayComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateAidSummary());

        // Disable barangay combo initially
        barangayComboBox.setDisable(true);
    }

    private void wireButtonHandlers() {
        closeBtn  .setOnAction(e -> closeDialog());
        cancelBtn .setOnAction(e -> closeDialog());
        previewBtn.setOnAction(e -> handlePreview());
        printBtn  .setOnAction(e -> handlePrint());
    }

    // ────────────────────────────────────────────────────────────────────────
    //  DATA LOADING
    // ────────────────────────────────────────────────────────────────────────

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

                ObservableList<DisasterModelComboBox> disasterList =
                        FXCollections.observableArrayList(disasters);
                disasterComboBox.setItems(disasterList);

                aidNameComboBox.setItems(FXCollections.observableArrayList(aidNames));

                if (!disasterList.isEmpty())
                    disasterComboBox.getSelectionModel().selectFirst();
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

        int    disasterId   = getSelectedDisasterId();
        String selectedAid  = aidNameComboBox.getValue();

        if (disasterId < 0) {
            barangayComboBox.setItems(FXCollections.observableArrayList(ALL_BARANGAYS));
            barangayComboBox.setValue(ALL_BARANGAYS);
            return;
        }

        List<String> barangays = (selectedAid != null && !selectedAid.isBlank())
                ? aidDAO.getBarangaysByAidNameAndDisaster(disasterId, selectedAid)
                : aidDAO.getBarangaysByDisaster(disasterId, 0);

        barangays.add(0, ALL_BARANGAYS);
        barangayComboBox.setItems(FXCollections.observableArrayList(barangays));
        barangayComboBox.setValue(ALL_BARANGAYS);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  AID SUMMARY
    // ────────────────────────────────────────────────────────────────────────

    private void updateAidSummary() {
        String selectedAid  = aidNameComboBox.getValue();
        int    disasterId   = getSelectedDisasterId();

        if (selectedAid == null || selectedAid.isBlank() || disasterId < 0) {
            beneficiaryCountLabel.setText("Beneficiaries: 0");
            hideAidSummary();
            printBtn  .setDisable(true);
            previewBtn.setDisable(true);
            return;
        }

        List<AidModel> records = getFilteredAndSortedAidRecords(disasterId, selectedAid);

        boolean isGeneralAid = generalAidCheckBox.isSelected();
        String contextInfo   = isGeneralAid ? " (General Aid)" : "";
        String barangayInfo  = isBarangayFilterActive()
                ? " | Barangay: " + barangayComboBox.getValue() : "";

        beneficiaryCountLabel.setText("Beneficiaries: " + records.size() + contextInfo + barangayInfo);

        showAidSummary();
        printBtn  .setDisable(records.isEmpty());
        previewBtn.setDisable(records.isEmpty());
    }

    private void showAidSummary() {
        aidSummary.setVisible(true);
        aidSummary.setManaged(true);
    }

    private void hideAidSummary() {
        aidSummary.setVisible(false);
        aidSummary.setManaged(false);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  VALIDATION
    // ────────────────────────────────────────────────────────────────────────

    private boolean validate() {
        if (aidNameComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Validation", "Please select an Aid Name.");
            return false;
        }
        if (getSelectedDisasterId() < 0) {
            AlertDialogManager.showWarning("Validation",
                    "Please select a Disaster or enable General Aid Distribution.");
            return false;
        }
        if (bondPaperSizeComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Validation", "Please select a paper size.");
            return false;
        }
        return true;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  PREVIEW
    // ────────────────────────────────────────────────────────────────────────

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
        previewStage.setScene(new Scene(scrollPane, 700, 850));
        previewStage.show();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  PRINT
    // ────────────────────────────────────────────────────────────────────────

    private void handlePrint() {
        if (!validate()) return;

        String reportType = getSelectedReportType();
        VBox   content    = buildReportContent(reportType);
        if (content == null) return;

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            AlertDialogManager.showError("Print Error", "No printer is available.");
            return;
        }

        applyPageLayout(job);

        boolean showDialog = job.showPrintDialog(getOwnerWindow());
        if (!showDialog) return;

        int     copies     = copiesSpinner.getValue();
        boolean allSuccess = true;

        for (int i = 0; i < copies; i++) {
            if (!job.printPage(content)) { allSuccess = false; break; }
        }

        if (allSuccess) {
            job.endJob();
            AlertDialogManager.showInfo("Success",
                    String.format("Document sent to printer.\n%d %s printed successfully.",
                            copies, copies == 1 ? "copy" : "copies"));
            closeDialog();
        } else {
            AlertDialogManager.showError("Print Error",
                    "An error occurred while printing. Please try again.");
        }
    }

    private void applyPageLayout(PrinterJob job) {
        Printer         printer     = job.getPrinter();
        Paper           paper       = resolvePaper(printer);
        PageOrientation orientation = landscapeRadio.isSelected()
                ? PageOrientation.LANDSCAPE : PageOrientation.PORTRAIT;
        PageLayout layout = printer.createPageLayout(
                paper, orientation, Printer.MarginType.DEFAULT);
        job.getJobSettings().setPageLayout(layout);
    }

    private Paper resolvePaper(Printer printer) {
        String selected = bondPaperSizeComboBox.getValue();
        if (selected == null) return Paper.A4;
        if (selected.startsWith("Letter")) return Paper.NA_LETTER;
        if (selected.startsWith("Legal"))  return Paper.LEGAL;
        return Paper.A4;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  REPORT BUILDERS
    // ────────────────────────────────────────────────────────────────────────

    private String getSelectedReportType() {
        if (distributionSummaryRadio.isSelected()) return "Distribution Summary";
        return "Beneficiary List";
    }

    private VBox buildReportContent(String reportType) {
        String selectedAid = aidNameComboBox.getValue();
        int    disasterId  = getSelectedDisasterId();

        if (selectedAid == null || disasterId < 0) {
            AlertDialogManager.showWarning("No Selection",
                    "Please select an Aid Name and a Disaster.");
            return null;
        }

        List<AidModel> records = getFilteredAndSortedAidRecords(disasterId, selectedAid);

        if (records.isEmpty()) {
            AlertDialogManager.showWarning("No Data",
                    "No records found for the selected criteria.");
            return null;
        }

        String disasterName = resolveDisasterName();

        return switch (reportType) {
            case "Distribution Summary" ->
                    buildDistributionSummaryContent(disasterName, selectedAid, records);
            default ->
                    buildBeneficiaryListContent(disasterName, selectedAid, records);
        };
    }

    // ── Beneficiary List ────────────────────────────────────────────────────

    private VBox buildBeneficiaryListContent(String disasterName, String aidName,
                                             List<AidModel> records) {
        VBox root = printPage();

        if (includeHeaderCheckbox.isSelected())
            root.getChildren().add(buildHeader("BENEFICIARY LIST", disasterName, aidName));

        root.getChildren().add(separator());

        // Column header row
        HBox colHeader = styledRow(true);
        colHeader.getChildren().addAll(
                colCell("#",                 50,  true),
                colCell("Beneficiary Name", 250,  true),
                colCell("Date Received",    150,  true),
                colCell("Amount (₱)",       110,  true),
                colCell("Priority Score",   100,  true)
        );
        root.getChildren().add(colHeader);

        // Data rows
        for (int i = 0; i < records.size(); i++) {
            AidModel aid = records.get(i);
            HBox row = styledRow(i % 2 == 0);
            row.getChildren().addAll(
                    colCell(String.valueOf(i + 1),                              50,  false),
                    colCell(nvl(aid.getBeneficiaryName()),                     250,  false),
                    colCell(aid.getDate() != null
                            ? aid.getDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                            : "—",                                             150,  false),
                    colCell(String.format("%.2f", aid.getCost()),              110,  false),
                    colCell(extractScoreInfo(aid.getNotes()),                  100,  false)
            );
            root.getChildren().add(row);
        }

        root.getChildren().add(separator());

        double totalCost = records.stream().mapToDouble(AidModel::getCost).sum();
        Label totalLabel = new Label(String.format(
                "Total Beneficiaries: %d     Total Cost: ₱ %,.2f", records.size(), totalCost));
        totalLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold;");
        root.getChildren().add(totalLabel);

        if (includeFooterCheckbox.isSelected())
            root.getChildren().add(buildFooter(records.size()));

        if (includePageNumbersCheckbox.isSelected())
            root.getChildren().add(pageNumber(1));

        return root;
    }

    // ── Distribution Summary ────────────────────────────────────────────────

    private VBox buildDistributionSummaryContent(String disasterName, String aidName,
                                                 List<AidModel> records) {
        VBox root = printPage();

        if (includeHeaderCheckbox.isSelected())
            root.getChildren().add(buildHeader("DISTRIBUTION SUMMARY", disasterName, aidName));

        root.getChildren().add(separator());

        double totalCost  = records.stream().mapToDouble(AidModel::getCost).sum();
        double avgCost    = records.isEmpty() ? 0 : totalCost / records.size();
        double maxCost    = records.stream().mapToDouble(AidModel::getCost).max().orElse(0);
        double minCost    = records.stream().mapToDouble(AidModel::getCost).min().orElse(0);

        root.getChildren().addAll(
                statRow("Aid Name",            aidName),
                statRow("Disaster Event",      disasterName),
                statRow("Total Beneficiaries", records.size() + " persons"),
                statRow("Total Cost",          String.format("₱ %,.2f", totalCost)),
                statRow("Average Cost",        String.format("₱ %,.2f", avgCost)),
                statRow("Highest Amount",      String.format("₱ %,.2f", maxCost)),
                statRow("Lowest Amount",       String.format("₱ %,.2f", minCost))
        );

        root.getChildren().add(separator());

        // Visual cost bar (scaled to total)
        double barWidth = 500.0;

        StackPane bar = new StackPane();
        bar.setMaxWidth(barWidth);
        bar.setMinWidth(barWidth);
        bar.setMinHeight(22);
        bar.setMaxHeight(22);
        bar.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 4;");

        HBox fill = new HBox();
        fill.setMinWidth(barWidth); // fully filled since this is total
        fill.setMaxWidth(barWidth);
        fill.setMinHeight(22);
        fill.setMaxHeight(22);
        fill.setStyle("-fx-background-color: #1a7a4a; -fx-background-radius: 4;");
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        bar.getChildren().addAll(fill,
                labelCentered(String.format("₱ %,.2f total distributed", totalCost)));

        HBox barBox = new HBox(bar);
        barBox.setPadding(new Insets(10, 0, 10, 0));
        root.getChildren().add(barBox);

        root.getChildren().add(separator());

        Label noteLabel = new Label("✔ Distribution completed successfully.");
        noteLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #1a7a4a;");
        root.getChildren().add(noteLabel);

        if (includeFooterCheckbox.isSelected())
            root.getChildren().add(buildFooter(records.size()));

        if (includePageNumbersCheckbox.isSelected())
            root.getChildren().add(pageNumber(1));

        return root;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  SHARED COMPONENT HELPERS
    // ────────────────────────────────────────────────────────────────────────

    private VBox printPage() {
        VBox page = new VBox(6);
        page.setPadding(new Insets(30));
        page.setStyle("-fx-background-color: white; -fx-min-width: 600;");
        return page;
    }

    private VBox buildHeader(String reportTitle, String disasterName, String aidName) {
        VBox header = new VBox(4);
        header.setAlignment(Pos.CENTER);

        Label republic = new Label("Republic of the Philippines");
        republic.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");

        Label title = new Label("BARANGAY DISASTER RISK REDUCTION AND MANAGEMENT");
        title.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");

        Label reportTypeLabel = new Label(reportTitle);
        reportTypeLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; " +
                "-fx-text-fill: #1a3a6b; -fx-padding: 4 0 0 0;");

        Label disasterLabel = new Label("Disaster: " + disasterName);
        disasterLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; " +
                "-fx-text-fill: #c0392b;");

        Label aidLabel = new Label("Aid Type: " + aidName);
        aidLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #333;");

        Label dateLabel = new Label("Generated: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy  HH:mm")));
        dateLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #777;");

        header.getChildren().addAll(republic, title, reportTypeLabel,
                disasterLabel, aidLabel, dateLabel);
        return header;
    }

    private HBox buildFooter(int totalRecords) {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        Label left = new Label("Total Records: " + totalRecords);
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
        sep.setMinHeight(1);
        sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: #bdc3c7;");
        VBox.setMargin(sep, new Insets(6, 0, 6, 0));
        return sep;
    }

    private HBox styledRow(boolean shaded) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 8, 4, 8));
        row.setStyle("-fx-background-color: " + (shaded ? "#f2f3f4" : "white") + ";");
        return row;
    }

    private Label colCell(String text, double width, boolean header) {
        Label lbl = new Label(text);
        lbl.setMinWidth(width);
        lbl.setMaxWidth(width);
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

    // ────────────────────────────────────────────────────────────────────────
    //  FILTERING / SORTING
    // ────────────────────────────────────────────────────────────────────────

    private List<AidModel> getFilteredAndSortedAidRecords(int disasterId, String aidName) {
        List<AidModel> filtered = allAidRecords.stream()
                .filter(aid -> {
                    boolean disasterMatch = (disasterId == 0 && aid.getDisasterId() == 0)
                            || (disasterId > 0 && aid.getDisasterId() == disasterId);
                    boolean nameMatch = aid.getName() != null && aid.getName().equals(aidName);
                    return disasterMatch && nameMatch;
                })
                .collect(Collectors.toList());

        if (isBarangayFilterActive()) {
            filtered = filterByBarangay(filtered, barangayComboBox.getValue());
        }

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

    // ────────────────────────────────────────────────────────────────────────
    //  UTILITIES
    // ────────────────────────────────────────────────────────────────────────

    private int getSelectedDisasterId() {
        if (generalAidCheckBox.isSelected()) return 0;
        DisasterModelComboBox d = disasterComboBox.getValue();
        return d != null ? d.getDisasterId() : -1;
    }

    private String resolveDisasterName() {
        if (generalAidCheckBox.isSelected()) return "General Aid";
        DisasterModelComboBox d = disasterComboBox.getValue();
        return d != null ? d.getDisasterName() : "—";
    }

    private String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private javafx.stage.Window getOwnerWindow() {
        try { return printBtn.getScene().getWindow(); }
        catch (Exception e) { return null; }
    }

    private void closeDialog() {
        try {
            Stage stage = (Stage) closeBtn.getScene().getWindow();
            stage.close();
        } catch (Exception ignored) { }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  DASHBOARD REFRESH
    // ────────────────────────────────────────────────────────────────────────

    public void refreshComboBoxes() {
        Task<Void> task = new Task<>() {
            private List<DisasterModelComboBox> disasters;
            private List<String>                aidNames;

            @Override
            protected Void call() {
                disasters = disasterDAO.findAll();
                aidNames  = aidDAO.getDistinctAidNames();
                return null;
            }

            @Override
            protected void succeeded() {
                DisasterModelComboBox prev = disasterComboBox.getValue();
                String prevAid = aidNameComboBox.getValue();

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