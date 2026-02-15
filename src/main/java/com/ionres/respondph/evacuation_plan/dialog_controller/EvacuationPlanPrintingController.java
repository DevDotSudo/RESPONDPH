package com.ionres.respondph.evacuation_plan.dialog_controller;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.evacuation_plan.EvacuationPlanModel;
import com.ionres.respondph.evacuation_plan.EvacuationPlanService;
import com.ionres.respondph.evacuation_plan.EvacuationPlanServiceImpl;
import com.ionres.respondph.evac_site.EvacSiteDAO;
import com.ionres.respondph.evac_site.EvacSiteDAOServiceImpl;
import com.ionres.respondph.evac_site.EvacSiteModel;
import com.ionres.respondph.util.AlertDialogManager;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class EvacuationPlanPrintingController {

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

    // ── Footer buttons ───────────────────────────────────────────────────────
    @FXML private Button previewBtn;
    @FXML private Button cancelBtn;
    @FXML private Button printBtn;

    // ── Header close button ──────────────────────────────────────────────────
    @FXML private Button closeBtn;

    // ── Internal state ───────────────────────────────────────────────────────
    private final EvacuationPlanService service =
            new EvacuationPlanServiceImpl(DBConnection.getInstance());

    private final EvacSiteDAO evacSiteDAO =
            new EvacSiteDAOServiceImpl(DBConnection.getInstance());

    // All plans loaded once; filtered on demand
    private List<EvacuationPlanModel> allPlans = new ArrayList<>();

    // Maps populated alongside the combo-boxes
    private final Map<String, Integer> disasterNameToId   = new LinkedHashMap<>();
    private final Map<String, Integer> evacSiteNameToId   = new LinkedHashMap<>();
    private final Map<String, Integer> evacSiteCapacities = new LinkedHashMap<>();

    // ToggleGroups (FXML RadioButtons need to be in groups programmatically
    //  when they are declared without fx:id on the ToggleGroup itself)
    private final ToggleGroup reportTypeGroup   = new ToggleGroup();
    private final ToggleGroup orientationGroup  = new ToggleGroup();

    // ────────────────────────────────────────────────────────────────────────
    //  FXML INITIALIZE
    // ────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        wireToggleGroups();
        wirePaperSizeDefault();
        wireDisasterComboListener();
        wireEvacSiteComboListener();
        wireButtonHandlers();
        loadData();
    }

    // ── Toggle-group wiring ──────────────────────────────────────────────────

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

    // ── Reactive combo listeners ─────────────────────────────────────────────

    private void wireDisasterComboListener() {
        disasterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateEvacSiteComboForDisaster(newVal);
            } else {
                evacuationSiteComboBox.getItems().clear();
                hideCapacitySummary();
            }
        });
    }

    private void wireEvacSiteComboListener() {
        evacuationSiteComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateCapacitySummary(newVal);
            } else {
                hideCapacitySummary();
            }
        });
    }

    // ── Button handlers ──────────────────────────────────────────────────────

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
        Task<List<EvacuationPlanModel>> task = new Task<>() {
            @Override
            protected List<EvacuationPlanModel> call() {
                return service.getAllEvacuationPlans();
            }

            @Override
            protected void succeeded() {
                allPlans = getValue();
                populateDisasterCombo();
            }

            @Override
            protected void failed() {
                AlertDialogManager.showError("Load Error",
                        "Failed to load evacuation data: " + getException().getMessage());
            }
        };
        new Thread(task).start();
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

        if (!names.isEmpty()) {
            disasterComboBox.getSelectionModel().selectFirst();
        }
    }

    /**
     * Populate evacuation-site combo filtered to plans for {@code disasterName}.
     * Also pre-fetches capacity so the summary label can show it instantly.
     */
    private void populateEvacSiteComboForDisaster(String disasterName) {
        evacuationSiteComboBox.getItems().clear();
        evacSiteNameToId.clear();
        evacSiteCapacities.clear();
        hideCapacitySummary();

        Integer disasterId = disasterNameToId.get(disasterName);
        if (disasterId == null) return;

        Task<Map<String, Integer>> task = new Task<>() {
            @Override
            protected Map<String, Integer> call() {
                // Collect evac-site IDs used by this disaster
                Map<String, Integer> siteMap = allPlans.stream()
                        .filter(p -> disasterId.equals(p.getDisasterId()))
                        .filter(p -> p.getEvacSiteName() != null)
                        .collect(Collectors.toMap(
                                EvacuationPlanModel::getEvacSiteName,
                                EvacuationPlanModel::getEvacSiteId,
                                (a, b) -> a,
                                LinkedHashMap::new
                        ));
                return siteMap;
            }

            @Override
            protected void succeeded() {
                Map<String, Integer> siteMap = getValue();
                evacSiteNameToId.putAll(siteMap);

                // Fetch capacities for display
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

                ObservableList<String> sorted =
                        FXCollections.observableArrayList(siteMap.keySet()
                                .stream().sorted().collect(Collectors.toList()));
                evacuationSiteComboBox.setItems(sorted);

                if (!sorted.isEmpty()) {
                    evacuationSiteComboBox.getSelectionModel().selectFirst();
                }
            }

            @Override
            protected void failed() {
                AlertDialogManager.showError("Error",
                        "Failed to load evacuation sites: " + getException().getMessage());
            }
        };
        new Thread(task).start();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CAPACITY SUMMARY
    // ────────────────────────────────────────────────────────────────────────

    private void updateCapacitySummary(String siteName) {
        String disasterName = disasterComboBox.getValue();
        Integer disasterId  = disasterNameToId.get(disasterName);
        Integer siteId      = evacSiteNameToId.get(siteName);
        if (disasterId == null || siteId == null) { hideCapacitySummary(); return; }

        long occupied = allPlans.stream()
                .filter(p -> disasterId.equals(p.getDisasterId())
                        && siteId.equals(p.getEvacSiteId()))
                .count();

        Integer capacity = evacSiteCapacities.getOrDefault(siteName, 0);
        capacityLabel.setText(
                String.format("Capacity: %d / %d  (%.0f%% occupied)",
                        occupied, capacity,
                        capacity > 0 ? (occupied * 100.0 / capacity) : 0.0));

        capacitySummary.setVisible(true);
        capacitySummary.setManaged(true);
    }

    private void hideCapacitySummary() {
        capacitySummary.setVisible(false);
        capacitySummary.setManaged(false);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  VALIDATION
    // ────────────────────────────────────────────────────────────────────────

    private boolean validate() {
        if (disasterComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Validation",
                    "Please select a disaster event.");
            return false;
        }
        if (evacuationSiteComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Validation",
                    "Please select an evacuation site.");
            return false;
        }
        if (bondPaperSizeComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Validation",
                    "Please select a paper size.");
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
        VBox content = buildReportContent(reportType);
        if (content == null) return;

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            AlertDialogManager.showError("Print Error", "No printer is available.");
            return;
        }

        // Apply page layout from user selection
        applyPageLayout(job);

        boolean showDialog = job.showPrintDialog(getOwnerWindow());
        if (!showDialog) return;

        int copies = copiesSpinner.getValue();
        boolean allSuccess = true;

        for (int i = 0; i < copies; i++) {
            boolean printed = job.printPage(content);
            if (!printed) { allSuccess = false; break; }
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

    /** Applies the paper-size and orientation chosen by the user to the printer job. */
    private void applyPageLayout(PrinterJob job) {
        Printer printer = job.getPrinter();

        Paper paper = resolvePaper(printer);
        PageOrientation orientation = landscapeRadio.isSelected()
                ? PageOrientation.LANDSCAPE
                : PageOrientation.PORTRAIT;

        PageLayout layout = printer.createPageLayout(
                paper, orientation, Printer.MarginType.DEFAULT);
        job.getJobSettings().setPageLayout(layout);
    }

    /** Maps the UI paper-size string to the closest {@link Paper} constant. */
    private Paper resolvePaper(Printer printer) {
        String selected = bondPaperSizeComboBox.getValue();
        if (selected == null) return Paper.A4;

        if (selected.startsWith("Letter"))  return Paper.NA_LETTER;
        if (selected.startsWith("Legal"))   return Paper.LEGAL;
        return Paper.A4;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  REPORT BUILDERS  (one per report type)
    // ────────────────────────────────────────────────────────────────────────

    private String getSelectedReportType() {
        if (evacuationPlanRadio.isSelected())  return "Evacuation Plan";
        if (capacityReportRadio.isSelected())  return "Capacity Report";
        return "Beneficiary List";   // default / beneficiaryListRadio
    }

    /**
     * Entry point – delegates to the correct builder.
     * Returns {@code null} (with an error dialog) if no data is found.
     */
    private VBox buildReportContent(String reportType) {
        String disasterName = disasterComboBox.getValue();
        String siteName     = evacuationSiteComboBox.getValue();
        Integer disasterId  = disasterNameToId.get(disasterName);
        Integer siteId      = evacSiteNameToId.get(siteName);

        if (disasterId == null || siteId == null) {
            AlertDialogManager.showWarning("No Selection", "Please select a disaster and site.");
            return null;
        }

        List<EvacuationPlanModel> filtered = allPlans.stream()
                .filter(p -> disasterId.equals(p.getDisasterId())
                        && siteId.equals(p.getEvacSiteId()))
                .sorted(Comparator.comparing(EvacuationPlanModel::getBeneficiaryName))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            AlertDialogManager.showWarning("No Data",
                    "No records found for the selected disaster and evacuation site.");
            return null;
        }

        return switch (reportType) {
            case "Evacuation Plan"  -> buildEvacuationPlanContent(disasterName, siteName, filtered);
            case "Capacity Report"  -> buildCapacityReportContent(disasterName, siteName, siteId, disasterId, filtered);
            default                 -> buildBeneficiaryListContent(disasterName, siteName, filtered);
        };
    }

    // ── Beneficiary List ────────────────────────────────────────────────────

    private VBox buildBeneficiaryListContent(String disasterName, String siteName,
                                             List<EvacuationPlanModel> plans) {
        VBox root = printPage();

        if (includeHeaderCheckbox.isSelected())
            root.getChildren().add(buildHeader("BENEFICIARY LIST", disasterName, siteName));

        root.getChildren().add(separator());

        // Column header row
        HBox colHeader = styledRow(true);
        colHeader.getChildren().addAll(
                colCell("#",   50,  true),
                colCell("Beneficiary Name", 250, true),
                colCell("Date Assigned",    180, true),
                colCell("Notes",            200, true)
        );
        root.getChildren().add(colHeader);

        // Data rows
        for (int i = 0; i < plans.size(); i++) {
            EvacuationPlanModel p = plans.get(i);
            HBox row = styledRow(i % 2 == 0);
            row.getChildren().addAll(
                    colCell(String.valueOf(i + 1),        50,  false),
                    colCell(p.getBeneficiaryName(),        250, false),
                    colCell(formatDate(p.getDateCreated()), 180, false),
                    colCell(nvl(p.getNotes()),              200, false)
            );
            root.getChildren().add(row);
        }

        root.getChildren().add(separator());

        Label totalLabel = new Label("Total Beneficiaries: " + plans.size());
        totalLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold;");
        root.getChildren().add(totalLabel);

        if (includeFooterCheckbox.isSelected())
            root.getChildren().add(buildFooter(plans.size()));

        if (includePageNumbersCheckbox.isSelected())
            root.getChildren().add(pageNumber(1));

        return root;
    }

    // ── Evacuation Plan ─────────────────────────────────────────────────────

    private VBox buildEvacuationPlanContent(String disasterName, String siteName,
                                            List<EvacuationPlanModel> plans) {
        VBox root = printPage();

        if (includeHeaderCheckbox.isSelected())
            root.getChildren().add(buildHeader("EVACUATION PLAN", disasterName, siteName));

        root.getChildren().add(separator());

        // Group by first letter for visual structure
        Map<Character, List<EvacuationPlanModel>> byLetter = plans.stream()
                .collect(Collectors.groupingBy(
                        p -> Character.toUpperCase(p.getBeneficiaryName().charAt(0)),
                        TreeMap::new,
                        Collectors.toList()
                ));

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
        if (includeFooterCheckbox.isSelected())
            root.getChildren().add(buildFooter(plans.size()));
        if (includePageNumbersCheckbox.isSelected())
            root.getChildren().add(pageNumber(1));

        return root;
    }

    // ── Capacity Report ──────────────────────────────────────────────────────

    private VBox buildCapacityReportContent(String disasterName, String siteName,
                                            int siteId, int disasterId,
                                            List<EvacuationPlanModel> plans) {
        VBox root = printPage();

        if (includeHeaderCheckbox.isSelected())
            root.getChildren().add(buildHeader("CAPACITY REPORT", disasterName, siteName));

        root.getChildren().add(separator());

        int totalCapacity = evacSiteCapacities.getOrDefault(siteName, 0);
        int occupied      = plans.size();
        int available     = Math.max(0, totalCapacity - occupied);
        double pct        = totalCapacity > 0 ? (occupied * 100.0 / totalCapacity) : 0.0;

        root.getChildren().addAll(
                statRow("Evacuation Site",     siteName),
                statRow("Disaster Event",      disasterName),
                statRow("Total Capacity",      String.valueOf(totalCapacity) + " persons"),
                statRow("Currently Occupied",  occupied + " persons"),
                statRow("Available Slots",     available + " persons"),
                statRow("Occupancy Rate",      String.format("%.1f%%", pct))
        );

        root.getChildren().add(separator());

        // Visual occupancy bar
        double barWidth = 500.0;
        double filled   = barWidth * (pct / 100.0);

        StackPane bar = new StackPane();
        bar.setMaxWidth(barWidth);
        bar.setMinWidth(barWidth);
        bar.setMinHeight(22);
        bar.setMaxHeight(22);
        bar.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 4;");

        HBox fill = new HBox();
        fill.setMinWidth(filled);
        fill.setMaxWidth(filled);
        fill.setMinHeight(22);
        fill.setMaxHeight(22);
        fill.setStyle("-fx-background-color: " + capacityColor(pct) + "; " +
                "-fx-background-radius: 4;");

        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        bar.getChildren().addAll(fill,
                labelCentered(String.format("%.0f%% occupied", pct)));

        HBox barBox = new HBox(bar);
        barBox.setPadding(new Insets(10, 0, 10, 0));
        root.getChildren().add(barBox);

        root.getChildren().add(separator());

        // Summary note
        String note = pct >= 100 ? "⚠ Site is at full capacity."
                : pct >= 80  ? "⚠ Site is nearly full."
                :              "✔ Site has available space.";
        Label noteLabel = new Label(note);
        noteLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; " +
                "-fx-text-fill: " + (pct >= 80 ? "#c0392b" : "#1a7a4a") + ";");
        root.getChildren().add(noteLabel);

        if (includeFooterCheckbox.isSelected())
            root.getChildren().add(buildFooter(occupied));
        if (includePageNumbersCheckbox.isSelected())
            root.getChildren().add(pageNumber(1));

        return root;
    }

    private VBox printPage() {
        VBox page = new VBox(6);
        page.setPadding(new Insets(30));
        page.setStyle("-fx-background-color: white; -fx-min-width: 600;");
        return page;
    }

    private VBox buildHeader(String reportTitle, String disasterName, String siteName) {
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

        Label siteLabel = new Label("Evacuation Site: " + siteName);
        siteLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #333;");

        Label dateLabel = new Label("Generated: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy  HH:mm")));
        dateLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #777;");

        header.getChildren().addAll(republic, title, reportTypeLabel,
                disasterLabel, siteLabel, dateLabel);
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
        sep.setMinHeight(1);
        sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: #bdc3c7; -fx-margin: 6 0;");
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

    private String capacityColor(double pct) {
        if (pct >= 100) return "#c0392b";
        if (pct >= 80)  return "#e67e22";
        return "#1a7a4a";
    }

    private String formatDate(String raw) {
        if (raw == null || raw.isBlank()) return "—";
        try {
            String trimmed = raw.substring(0, Math.min(raw.length(), 10));
            return trimmed;
        } catch (Exception e) {
            return raw;
        }
    }

    private String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private javafx.stage.Window getOwnerWindow() {
        try {
            return printBtn.getScene().getWindow();
        } catch (Exception e) {
            return null;
        }
    }

    private void closeDialog() {
        try {
            Stage stage = (Stage) closeBtn.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            // fallback: hide via DialogManager if available
        }
    }
}