package com.ionres.respondph.disaster_mapping.dialogs_controller;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.common.model.DisasterCircleInfo;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.evacuation_plan.*;
import com.ionres.respondph.evac_site.EvacSiteService;
import com.ionres.respondph.main.MainFrameController;
import com.ionres.respondph.sendsms.SmsService;
import com.ionres.respondph.sendsms.SmsServiceImpl;
import com.ionres.respondph.util.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EvacuationAllocationDialogController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(EvacuationAllocationDialogController.class.getName());

    @FXML private VBox root;
    @FXML private Label titleLabel;
    @FXML private Label disasterInfoLabel;
    @FXML private TabPane evacuationSitesTabPane;
    @FXML private Button saveAllocationBtn;
    @FXML private Button cancelBtn;
    @FXML private Button closeBtn;

    @FXML private RadioButton rbGsm;
    @FXML private RadioButton rbApi;
    @FXML private HBox gsmPortBox;
    @FXML private ComboBox<String> cbGsmPort;
    @FXML private Button btnRefreshPorts;
    @FXML private Label lblConnectionStatus;

    private Stage dialogStage;
    private DisasterCircleInfo disaster;
    private int disasterId;
    private GeoBasedEvacPlanDAO geoDAO;
    private EvacuationPlanDAO evacPlanDAO;
    private EvacSiteService evacSiteService;
    private SmsService smsService;
    private SMSSender smsSender;

    private Map<Integer, List<RankedBeneficiaryWithLocation>> siteAllocations = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("=== INITIALIZING EvacuationAllocationDialogController ===");

        DBConnection dbConnection = DBConnection.getInstance();
        geoDAO = new GeoBasedEvacPlanDAOImpl(dbConnection);
        evacPlanDAO = new EvacuationPlanDAOImpl(dbConnection);
        evacSiteService = new com.ionres.respondph.evac_site.EvacSiteServiceImpl(dbConnection);

        smsService = new SmsServiceImpl();
        smsSender = SMSSender.getInstance();

        setupButtons();
        setupRadioButtons();
        setupGsmPorts();
        LOGGER.info("Controller initialized successfully");
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setDisasterData(DisasterCircleInfo disaster, int disasterId) {
        LOGGER.info("=== SETTING DISASTER DATA ===");
        LOGGER.info("Disaster ID: " + disasterId);
        LOGGER.info("Disaster: " + (disaster != null ? disaster.disasterType + " - " + disaster.disasterName : "null"));

        this.disaster = disaster;
        this.disasterId = disasterId;

        if (disaster != null) {
            String disasterInfo = String.format("Disaster: %s - %s",
                    disaster.disasterType != null ? disaster.disasterType : "Unknown Type",
                    disaster.disasterName != null ? disaster.disasterName : "Unknown Disaster");
            disasterInfoLabel.setText(disasterInfo);
        }

        loadEvacuationSitesAndBeneficiaries();
    }
    private void setupButtons() {
        if (closeBtn != null) {
            closeBtn.setOnAction(e -> closeDialog());
        }
        if (cancelBtn != null) {
            cancelBtn.setOnAction(e -> closeDialog());
        }
        if (saveAllocationBtn != null) {
            saveAllocationBtn.setOnAction(e -> handleSaveAllocation());
        }
    }

    private void setupRadioButtons() {
        if (rbGsm == null || rbApi == null) {
            LOGGER.warning("Radio buttons not found in FXML");
            return;
        }

        ToggleGroup group = new ToggleGroup();
        rbGsm.setToggleGroup(group);
        rbApi.setToggleGroup(group);

        rbApi.setSelected(true);

        rbApi.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && gsmPortBox != null) {
                gsmPortBox.setVisible(false);
                gsmPortBox.setManaged(false);
            }
        });

        rbGsm.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && gsmPortBox != null) {
                gsmPortBox.setVisible(true);
                gsmPortBox.setManaged(true);
                populateGsmPorts();
            }
        });

        if (btnRefreshPorts != null) {
            btnRefreshPorts.setOnAction(e -> populateGsmPorts());
        }
    }

    private void setupGsmPorts() {
        if (rbGsm != null && rbGsm.isSelected()) {
            populateGsmPorts();
        }
    }

    private void populateGsmPorts() {
        if (cbGsmPort == null) return;

        Platform.runLater(() -> {
            try {
                LOGGER.info("Scanning for GSM ports...");
                List<String> availablePorts = smsSender.getAvailablePorts();

                if (availablePorts.isEmpty()) {
                    cbGsmPort.setItems(FXCollections.observableArrayList("No ports available"));
                    cbGsmPort.setDisable(true);
                    cbGsmPort.getSelectionModel().selectFirst();

                    if (lblConnectionStatus != null) {
                        lblConnectionStatus.setText("Disconnected - No ports found");
                        lblConnectionStatus.setStyle("-fx-text-fill: red;");
                    }
                    LOGGER.warning("No GSM ports found");
                } else {
                    ObservableList<String> portOptions = FXCollections.observableArrayList(availablePorts);
                    cbGsmPort.setItems(portOptions);
                    cbGsmPort.setDisable(false);

                    String connectedPort = smsSender.getConnectedPort();
                    if (connectedPort != null && portOptions.contains(connectedPort)) {
                        cbGsmPort.getSelectionModel().select(connectedPort);
                        if (lblConnectionStatus != null) {
                            lblConnectionStatus.setText("Connected to " + connectedPort);
                            lblConnectionStatus.setStyle("-fx-text-fill: green;");
                        }
                    } else {
                        cbGsmPort.getSelectionModel().selectFirst();
                        if (lblConnectionStatus != null) {
                            lblConnectionStatus.setText("Disconnected");
                            lblConnectionStatus.setStyle("-fx-text-fill: orange;");
                        }
                    }

                    cbGsmPort.valueProperty().addListener((obs, oldPort, newPort) -> {
                        if (newPort != null && !newPort.equals(oldPort) && rbGsm.isSelected()) {
                            connectToPort(newPort);
                        }
                    });

                    LOGGER.info("Found " + availablePorts.size() + " GSM ports");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error detecting GSM ports", e);
                cbGsmPort.setItems(FXCollections.observableArrayList("Error detecting ports"));
                cbGsmPort.setDisable(true);
                if (lblConnectionStatus != null) {
                    lblConnectionStatus.setText("Error: " + e.getMessage());
                    lblConnectionStatus.setStyle("-fx-text-fill: red;");
                }
            }
        });
    }

    private void connectToPort(String portName) {
        if (portName == null || portName.isEmpty() ||
                portName.contains("No ports") || portName.contains("Error")) {
            return;
        }

        LOGGER.info("Attempting to connect to port: " + portName);

        new Thread(() -> {
            boolean connected = smsSender.connectToPort(portName, 5000);

            Platform.runLater(() -> {
                if (connected) {
                    LOGGER.info("Successfully connected to " + portName);
                    if (lblConnectionStatus != null) {
                        lblConnectionStatus.setText("Connected to " + portName);
                        lblConnectionStatus.setStyle("-fx-text-fill: green;");
                    }
                } else {
                    LOGGER.warning("Failed to connect to " + portName);
                    if (lblConnectionStatus != null) {
                        lblConnectionStatus.setText("Failed to connect");
                        lblConnectionStatus.setStyle("-fx-text-fill: red;");
                    }
                }
            });
        }, "GSM-Connect-Thread").start();
    }

    public void loadEvacuationSitesAndBeneficiaries() {
        LOGGER.info("=== LOADING EVACUATION SITES AND BENEFICIARIES ===");
        LOGGER.info("Disaster ID: " + disasterId);

        try {
            List<RankedBeneficiaryWithLocation> allBeneficiaries =
                    geoDAO.getRankedBeneficiariesWithLocation(disasterId, true);

            LOGGER.info("Total beneficiaries retrieved: " + allBeneficiaries.size());

            List<EvacSiteWithDistance> availableSites = geoDAO.getEvacSitesWithCapacity(disasterId);

            LOGGER.info("Available evacuation sites: " + availableSites.size());

            if (availableSites.isEmpty()) {
                AlertDialogManager.showWarning("No Evacuation Sites",
                        "No evacuation sites with available capacity found for this disaster.");
                return;
            }

            if (allBeneficiaries.isEmpty()) {
                AlertDialogManager.showWarning("No Beneficiaries",
                        "No beneficiaries found inside the disaster area.");
                return;
            }

            evacuationSitesTabPane.getTabs().clear();
            siteAllocations.clear();

            Map<Integer, Integer> siteRemainingCapacity = new HashMap<>();
            for (EvacSiteWithDistance site : availableSites) {
                siteRemainingCapacity.put(site.getEvacSiteId(), site.getRemainingCapacity());
            }


            LOGGER.info("=== STARTING ALLOCATION PROCESS ===");
            LOGGER.info("Total beneficiaries to allocate: " + allBeneficiaries.size());
            LOGGER.info("Total evacuation sites available: " + availableSites.size());

            int successfulAllocations = 0;
            int failedAllocations = 0;
            List<String> allocationFailures = new ArrayList<>();

            for (RankedBeneficiaryWithLocation beneficiary : allBeneficiaries) {
                // Skip if already assigned to this disaster
                if (evacPlanDAO.isAlreadyAssignedToDisaster(beneficiary.getBeneficiaryId(), disasterId)) {
                    LOGGER.info("Beneficiary #" + beneficiary.getBeneficiaryId() +
                            " already assigned to this disaster - skipping");
                    continue;
                }

                String beneficiaryInfo = String.format(
                        "Beneficiary #%d (%s %s, %d persons, score: %.2f)",
                        beneficiary.getBeneficiaryId(),
                        beneficiary.getFirstName(),
                        beneficiary.getLastName(),
                        beneficiary.getHouseholdMembers(),
                        beneficiary.getFinalScore()
                );

                LOGGER.info("Processing: " + beneficiaryInfo);

                // Find nearest site with capacity for this beneficiary's household
                EvacSiteWithDistance nearestSite = findNearestSiteWithCapacity(
                        beneficiary.getLatitude(),
                        beneficiary.getLongitude(),
                        beneficiary.getHouseholdMembers(),
                        availableSites,
                        siteRemainingCapacity
                );

                if (nearestSite != null) {
                    // Add beneficiary to the site's allocation list
                    siteAllocations.computeIfAbsent(nearestSite.getEvacSiteId(), k -> new ArrayList<>())
                            .add(beneficiary);

                    // Update remaining capacity for the site
                    int previousCapacity = siteRemainingCapacity.get(nearestSite.getEvacSiteId());
                    int newCapacity = previousCapacity - beneficiary.getHouseholdMembers();
                    siteRemainingCapacity.put(nearestSite.getEvacSiteId(), newCapacity);

                    successfulAllocations++;

                    LOGGER.info(String.format(
                            "ALLOCATED: %s\n" +
                                    "   → Site: %s\n" +
                                    "   → Capacity update: %d → %d persons",
                            beneficiaryInfo,
                            nearestSite.getEvacSiteName(),
                            previousCapacity,
                            newCapacity
                    ));

                } else {
                    // No site found with sufficient capacity
                    failedAllocations++;
                    String failureMsg = String.format(
                            "No evacuation site available with capacity for %d persons (Beneficiary #%d)",
                            beneficiary.getHouseholdMembers(),
                            beneficiary.getBeneficiaryId()
                    );
                    allocationFailures.add(failureMsg);

                    LOGGER.warning(" ALLOCATION FAILED: " + failureMsg);

                    // Log current capacity status of all sites for debugging
                    LOGGER.warning("   Current capacity status of all sites:");
                    for (EvacSiteWithDistance site : availableSites) {
                        int remaining = siteRemainingCapacity.getOrDefault(site.getEvacSiteId(), 0);
                        LOGGER.warning(String.format(
                                "      - %s: %d persons remaining",
                                site.getEvacSiteName(),
                                remaining
                        ));
                    }
                }
            }

            // Log allocation summary
            LOGGER.info("=== ALLOCATION PROCESS COMPLETE ===");
            LOGGER.info("Successful allocations: " + successfulAllocations);
            LOGGER.info("Failed allocations: " + failedAllocations);

            if (!allocationFailures.isEmpty()) {
                LOGGER.warning("Allocation failures:");
                for (String failure : allocationFailures) {
                    LOGGER.warning("  - " + failure);
                }
            }

            // Log final capacity status for all sites
            LOGGER.info("=== FINAL EVACUATION SITE CAPACITY STATUS ===");
            for (EvacSiteWithDistance site : availableSites) {
                int allocated = siteAllocations.getOrDefault(site.getEvacSiteId(), new ArrayList<>()).size();
                int personsAllocated = siteAllocations.getOrDefault(site.getEvacSiteId(), new ArrayList<>())
                        .stream()
                        .mapToInt(RankedBeneficiaryWithLocation::getHouseholdMembers)
                        .sum();
                int remaining = siteRemainingCapacity.get(site.getEvacSiteId());

                LOGGER.info(String.format(
                        "%s: %d beneficiaries (%d persons), %d remaining capacity",
                        site.getEvacSiteName(),
                        allocated,
                        personsAllocated,
                        remaining
                ));
            }

            // ═══════════════════════════════════════════════════════════════════
            // CREATE TABS - Only for sites with allocated beneficiaries
            // ═══════════════════════════════════════════════════════════════════

            LOGGER.info("Creating tabs for evacuation sites...");
            int sitesWithBeneficiaries = 0;
            int sitesWithoutBeneficiaries = 0;

            for (EvacSiteWithDistance site : availableSites) {
                List<RankedBeneficiaryWithLocation> allocatedBeneficiaries =
                        siteAllocations.getOrDefault(site.getEvacSiteId(), new ArrayList<>());

                // Only create tab if there are beneficiaries allocated to this site
                if (allocatedBeneficiaries.isEmpty()) {
                    LOGGER.info("Skipping " + site.getEvacSiteName() + " - no beneficiaries allocated");
                    sitesWithoutBeneficiaries++;
                    continue;
                }

                LOGGER.info("Creating tab for " + site.getEvacSiteName() +
                        " with " + allocatedBeneficiaries.size() + " beneficiaries");

                Tab tab = createEvacuationSiteTab(site, allocatedBeneficiaries);
                evacuationSitesTabPane.getTabs().add(tab);
                sitesWithBeneficiaries++;
            }

            LOGGER.info(" Created " + evacuationSitesTabPane.getTabs().size() + " evacuation site tabs");
            LOGGER.info("   Sites with beneficiaries: " + sitesWithBeneficiaries);
            LOGGER.info("   Sites without beneficiaries: " + sitesWithoutBeneficiaries);

            // Show warning if no sites have beneficiaries
            if (evacuationSitesTabPane.getTabs().isEmpty()) {
                AlertDialogManager.showWarning("No Allocations",
                        "No beneficiaries were allocated to any evacuation site. " +
                                "This may be because all beneficiaries are already assigned or " +
                                "there is insufficient capacity at evacuation sites.");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading evacuation sites and beneficiaries", e);
            AlertDialogManager.showError("Error", "Failed to load data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Tab createEvacuationSiteTab(EvacSiteWithDistance site,
                                        List<RankedBeneficiaryWithLocation> beneficiaries) {
        Tab tab = new Tab();

        // Tab title with site name and beneficiary count
        int totalPersons = beneficiaries.stream()
                .mapToInt(RankedBeneficiaryWithLocation::getHouseholdMembers)
                .sum();

        tab.setText(String.format("%s (%d beneficiaries, %d persons)",
                site.getEvacSiteName(), beneficiaries.size(), totalPersons));

        // Create table for this site
        TableView<BeneficiaryRow> table = createBeneficiaryTable();

        // Populate table
        ObservableList<BeneficiaryRow> rows = FXCollections.observableArrayList();
        for (RankedBeneficiaryWithLocation beneficiary : beneficiaries) {
            double distance = GeoDistanceCalculator.calculateDistance(
                    beneficiary.getLatitude(),
                    beneficiary.getLongitude(),
                    site.getLatitude(),
                    site.getLongitude()
            ) * 1000; // Convert to meters

            BeneficiaryRow row = new BeneficiaryRow(
                    beneficiary.getBeneficiaryId(),
                    beneficiary.getFirstName() + " " + beneficiary.getLastName(),
                    beneficiary.getFinalScore(),
                    beneficiary.getScoreCategory(),
                    beneficiary.getHouseholdMembers(),
                    distance
            );
            rows.add(row);
        }

        table.setItems(rows);

        // Wrap table in VBox with padding
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().add(table);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        tab.setContent(content);

        return tab;
    }

    private TableView<BeneficiaryRow> createBeneficiaryTable() {
        TableView<BeneficiaryRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<BeneficiaryRow, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        idCol.setPrefWidth(60);

        TableColumn<BeneficiaryRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameCol.setPrefWidth(200);

        TableColumn<BeneficiaryRow, Double> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(cellData -> cellData.getValue().scoreProperty().asObject());
        scoreCol.setPrefWidth(80);

        TableColumn<BeneficiaryRow, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());
        categoryCol.setPrefWidth(120);

        TableColumn<BeneficiaryRow, Integer> householdCol = new TableColumn<>("Person(count)");
        householdCol.setCellValueFactory(cellData -> cellData.getValue().householdProperty().asObject());
        householdCol.setPrefWidth(100);

        TableColumn<BeneficiaryRow, Double> distanceCol = new TableColumn<>("Distance (m)");
        distanceCol.setCellValueFactory(cellData -> cellData.getValue().distanceProperty().asObject());
        distanceCol.setPrefWidth(120);

        table.getColumns().addAll(idCol, nameCol, scoreCol, categoryCol, householdCol, distanceCol);

        return table;
    }

    private EvacSiteWithDistance findNearestSiteWithCapacity(
            double beneficiaryLat,
            double beneficiaryLon,
            int householdSize,
            List<EvacSiteWithDistance> availableSites,
            Map<Integer, Integer> siteRemainingCapacity) {

        EvacSiteWithDistance nearestSite = null;
        double minDistance = Double.MAX_VALUE;

        // Track rejected sites for better logging
        List<String> rejectedSites = new ArrayList<>();
        List<String> eligibleSites = new ArrayList<>();

        for (EvacSiteWithDistance site : availableSites) {
            int remainingCapacity = siteRemainingCapacity.getOrDefault(site.getEvacSiteId(), 0);

            // Calculate distance first (for logging purposes)
            double distance = GeoDistanceCalculator.calculateDistance(
                    beneficiaryLat,
                    beneficiaryLon,
                    site.getLatitude(),
                    site.getLongitude()
            );

            // Check if site has enough capacity for the entire household
            if (remainingCapacity < householdSize) {
                rejectedSites.add(String.format(
                        "%s (%.2f km, needs %d, has %d)",
                        site.getEvacSiteName(),
                        distance,
                        householdSize,
                        remainingCapacity
                ));
                continue;
            }

            // Site has capacity - track it
            eligibleSites.add(String.format(
                    "%s (%.2f km, capacity: %d)",
                    site.getEvacSiteName(),
                    distance,
                    remainingCapacity
            ));

            // Update nearest site if this one is closer AND has capacity
            if (distance < minDistance) {
                minDistance = distance;
                nearestSite = site;
            }
        }

        // Enhanced logging for debugging and transparency
        if (nearestSite == null) {
            if (!rejectedSites.isEmpty()) {
                LOGGER.warning(String.format(
                        "No evacuation site found with capacity for household of %d persons.\n" +
                                "   Rejected sites (insufficient capacity): %s",
                        householdSize,
                        String.join(", ", rejectedSites)
                ));
            } else {
                LOGGER.warning(String.format(
                        "No evacuation sites available for household of %d persons.",
                        householdSize
                ));
            }
        } else {
            int selectedCapacity = siteRemainingCapacity.get(nearestSite.getEvacSiteId());

            LOGGER.info(String.format(
                    "Selected '%s' for %d-person household:\n" +
                            "   - Distance: %.2f km\n" +
                            "   - Remaining capacity: %d persons\n" +
                            "   - After allocation: %d persons remaining",
                    nearestSite.getEvacSiteName(),
                    householdSize,
                    minDistance,
                    selectedCapacity,
                    selectedCapacity - householdSize
            ));

            // Also log if there were closer sites that were rejected
            if (!rejectedSites.isEmpty()) {
                LOGGER.info(String.format(
                        "   Note: Skipped closer sites due to insufficient capacity: %s",
                        String.join(", ", rejectedSites)
                ));
            }

            // Log other eligible sites for reference
            if (eligibleSites.size() > 1) {
                LOGGER.fine(String.format(
                        "   Other eligible sites: %s",
                        String.join(", ", eligibleSites)
                ));
            }
        }

        return nearestSite;
    }

    private void handleSaveAllocation() {
        LOGGER.info("=== SAVING ALL ALLOCATIONS AND SENDING SMS ===");

        if (rbGsm != null && rbGsm.isSelected()) {
            String selectedPort = cbGsmPort != null ? cbGsmPort.getValue() : null;
            boolean validPort = selectedPort != null &&
                    !selectedPort.contains("No ports") &&
                    !selectedPort.contains("Error");

            if (!validPort || !smsSender.isConnected()) {
                AlertDialogManager.showError("GSM Not Connected",
                        "Please connect to a GSM port before sending SMS.");
                return;
            }
        }

        saveAllocationBtn.setDisable(true);
        cancelBtn.setDisable(true);

        Task<AllocationResult> task = new Task<>() {
            @Override
            protected AllocationResult call() {
                int successCount = 0;
                int totalPersonsAssigned = 0;
                int smsSentCount = 0;
                List<String> errors = new ArrayList<>();
                Map<Integer, String> siteNames = new HashMap<>();
                List<BeneficiaryWithEvacSite> allAllocatedBeneficiaries = new ArrayList<>();
                String sendMethod = (rbApi != null && rbApi.isSelected()) ? "API" : "GSM";

                for (Map.Entry<Integer, List<RankedBeneficiaryWithLocation>> entry : siteAllocations.entrySet()) {
                    int evacSiteId = entry.getKey();
                    List<RankedBeneficiaryWithLocation> beneficiaries = entry.getValue();

                    String evacSiteName = "";
                    try {
                        var site = evacSiteService.getEvacSiteById(evacSiteId);
                        if (site != null) {
                            evacSiteName = site.getName();
                            siteNames.put(evacSiteId, evacSiteName);
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error getting site name", e);
                    }

                    final String siteName = evacSiteName;

                    for (RankedBeneficiaryWithLocation beneficiary : beneficiaries) {
                        if (evacPlanDAO.isAlreadyAssignedToDisaster(beneficiary.getBeneficiaryId(), disasterId)) {
                            LOGGER.info("Beneficiary " + beneficiary.getBeneficiaryId() + " already assigned - skipping");
                            continue;
                        }

                        String notes = String.format(
                                "Auto-allocated (Geo-Based) | Score: %.2f | Category: %s | Household: %d",
                                beneficiary.getFinalScore(),
                                beneficiary.getScoreCategory(),
                                beneficiary.getHouseholdMembers()
                        );

                        boolean success = evacPlanDAO.insertEvacPlan(
                                beneficiary.getBeneficiaryId(),
                                evacSiteId,
                                disasterId,
                                notes
                        );

                        if (success) {
                            successCount++;
                            totalPersonsAssigned += beneficiary.getHouseholdMembers();

                            try {
                                BeneficiaryModel beneficiaryModel = AppContext.beneficiaryService
                                        .getBeneficiaryById(beneficiary.getBeneficiaryId());

                                if (beneficiaryModel != null && beneficiaryModel.getMobileNumber() != null) {
                                    allAllocatedBeneficiaries.add(
                                            new BeneficiaryWithEvacSite(beneficiaryModel, siteName)
                                    );
                                }
                            } catch (Exception e) {
                                LOGGER.log(Level.WARNING, "Error getting beneficiary for SMS", e);
                            }
                        } else {
                            errors.add("Failed to allocate beneficiary #" + beneficiary.getBeneficiaryId());
                        }
                    }
                }

                if (!allAllocatedBeneficiaries.isEmpty()) {
                    Platform.runLater(() -> showMainFrameSmsProgress(
                            "Sending SMS (" + sendMethod + ")",
                            allAllocatedBeneficiaries.size()
                    ));

                    int smsIndex = 0;
                    for (BeneficiaryWithEvacSite beneficiaryWithSite : allAllocatedBeneficiaries) {
                        smsIndex++;
                        final int currentSms = smsIndex;

                        Platform.runLater(() -> updateProgressUI("Sending SMS...", currentSms, allAllocatedBeneficiaries.size()));

                        BeneficiaryModel beneficiary = beneficiaryWithSite.beneficiary;
                        String evacSiteName = beneficiaryWithSite.evacSiteName;

                        String fullName = beneficiary.getFirstname() + " " + beneficiary.getLastname();
                        String message = CustomEvacMessageManager.getInstance()
                                .builtInMessage(fullName, evacSiteName);

                        String phoneNumber = beneficiary.getMobileNumber();

                        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                            boolean sent = smsService.sendSingleSMS(phoneNumber.trim(), fullName, message, sendMethod);
                            if (sent) {
                                smsSentCount++;
                            } else {
                                LOGGER.warning("Failed to send SMS to " + fullName + " (" + phoneNumber + ")");
                            }
                        } else {
                            LOGGER.warning("No phone number for beneficiary: " + fullName);
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                return new AllocationResult(successCount, totalPersonsAssigned, smsSentCount,
                        allAllocatedBeneficiaries.size(), errors, siteNames);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            AllocationResult result = task.getValue();
            hideMainFrameSmsProgress();
            saveAllocationBtn.setDisable(false);
            cancelBtn.setDisable(false);

            if (result.successCount > 0) {
                StringBuilder message = new StringBuilder();
                message.append(String.format(
                        "Successfully allocated %d beneficiaries (%d persons) to evacuation sites.\n\n",
                        result.successCount, result.totalPersonsAssigned
                ));

                message.append(String.format(
                        " SMS sent to %d out of %d beneficiaries.\n\n",
                        result.smsSentCount, result.totalBeneficiariesForSms
                ));

                // Show summary by site
                message.append("Allocation Summary:\n");
                for (Map.Entry<Integer, List<RankedBeneficiaryWithLocation>> entry : siteAllocations.entrySet()) {
                    int evacSiteId = entry.getKey();
                    List<RankedBeneficiaryWithLocation> beneficiaries = entry.getValue();

                    if (!beneficiaries.isEmpty()) {
                        String siteName = result.siteNames.getOrDefault(evacSiteId, "Site #" + evacSiteId);
                        int totalPersons = beneficiaries.stream()
                                .mapToInt(RankedBeneficiaryWithLocation::getHouseholdMembers)
                                .sum();

                        message.append(String.format("• %s: %d beneficiaries (%d persons)\n",
                                siteName, beneficiaries.size(), totalPersons));
                    }
                }

                if (!result.errors.isEmpty()) {
                    message.append("\n\nWarnings:\n").append(String.join("\n", result.errors));
                    AlertDialogManager.showWarning("Allocation Complete", message.toString());
                } else {
                    AlertDialogManager.showSuccess("Success", message.toString());
                }

                closeDialog();
            } else {
                AlertDialogManager.showError("Error", "Failed to allocate any beneficiaries:\n" +
                        String.join("\n", result.errors));
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            hideMainFrameSmsProgress();
            saveAllocationBtn.setDisable(false);
            cancelBtn.setDisable(false);

            LOGGER.log(Level.SEVERE, "Error in allocation task", task.getException());
            AlertDialogManager.showError("Error", "Failed to complete allocation: " +
                    task.getException().getMessage());
        }));

        new Thread(task, "Allocation-SMS-Task").start();
    }

    private void updateProgressUI(String label, int current, int total) {
        setMainFrameSmsCount(current, total);
    }

    private void showMainFrameSmsProgress(String title, int total) {
        MainFrameController main = MainFrameController.getInstance();
        if (main != null) {
            main.showSmsProgress(title, total);
            main.setSmsCount(0, total);
        }
    }

    private void setMainFrameSmsCount(int sent, int total) {
        MainFrameController main = MainFrameController.getInstance();
        if (main != null) {
            main.setSmsCount(sent, total);
        }
    }

    private void hideMainFrameSmsProgress() {
        MainFrameController main = MainFrameController.getInstance();
        if (main != null) {
            main.hideSmsProgress();
        }
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }


    public static class BeneficiaryRow {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty name;
        private final SimpleDoubleProperty score;
        private final SimpleStringProperty category;
        private final SimpleIntegerProperty household;
        private final SimpleDoubleProperty distance;

        public BeneficiaryRow(int id, String name, double score, String category, int household, double distance) {
            this.id = new SimpleIntegerProperty(id);
            this.name = new SimpleStringProperty(name);
            this.score = new SimpleDoubleProperty(score);
            this.category = new SimpleStringProperty(category);
            this.household = new SimpleIntegerProperty(household);
            this.distance = new SimpleDoubleProperty(distance);
        }

        public SimpleIntegerProperty idProperty() { return id; }
        public SimpleStringProperty nameProperty() { return name; }
        public SimpleDoubleProperty scoreProperty() { return score; }
        public SimpleStringProperty categoryProperty() { return category; }
        public SimpleIntegerProperty householdProperty() { return household; }
        public SimpleDoubleProperty distanceProperty() { return distance; }
    }


    private static class AllocationResult {
        final int successCount;
        final int totalPersonsAssigned;
        final int smsSentCount;
        final int totalBeneficiariesForSms;
        final List<String> errors;
        final Map<Integer, String> siteNames;

        AllocationResult(int successCount, int totalPersonsAssigned, int smsSentCount,
                         int totalBeneficiariesForSms, List<String> errors, Map<Integer, String> siteNames) {
            this.successCount = successCount;
            this.totalPersonsAssigned = totalPersonsAssigned;
            this.smsSentCount = smsSentCount;
            this.totalBeneficiariesForSms = totalBeneficiariesForSms;
            this.errors = errors;
            this.siteNames = siteNames;
        }
    }

    private static class BeneficiaryWithEvacSite {
        final BeneficiaryModel beneficiary;
        final String evacSiteName;

        BeneficiaryWithEvacSite(BeneficiaryModel beneficiary, String evacSiteName) {
            this.beneficiary = beneficiary;
            this.evacSiteName = evacSiteName;
        }
    }
}
