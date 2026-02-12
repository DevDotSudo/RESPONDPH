package com.ionres.respondph.disaster_mapping.dialogs_controller;
import com.ionres.respondph.common.model.DisasterCircleInfo;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.evacuation_plan.*;
import com.ionres.respondph.evac_site.EvacSiteModel;
import com.ionres.respondph.evac_site.EvacSiteService;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.DashboardRefresher;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AllocateBeneficiariesToEvacSiteController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(AllocateBeneficiariesToEvacSiteController.class.getName());

    @FXML private VBox root;
    @FXML private Label evacSiteNameLabel;
    @FXML private Label capacityLabel;
    @FXML private Label occupiedLabel;
    @FXML private Label remainingLabel;
    @FXML private Label disasterLabel;
    @FXML private Label totalBeneficiariesLabel;
    @FXML private TableView<BeneficiaryAllocationRow> beneficiariesTable;
    @FXML private TableColumn<BeneficiaryAllocationRow, Integer> idColumn;
    @FXML private TableColumn<BeneficiaryAllocationRow, String> nameColumn;
    @FXML private TableColumn<BeneficiaryAllocationRow, Double> scoreColumn;
    @FXML private TableColumn<BeneficiaryAllocationRow, String> categoryColumn;
    @FXML private TableColumn<BeneficiaryAllocationRow, Integer> householdColumn;
    @FXML private TableColumn<BeneficiaryAllocationRow, String> assignedSiteColumn;
    @FXML private TableColumn<BeneficiaryAllocationRow, String> statusColumn;
    //    @FXML private Button saveBtn;
    @FXML private Button closeBtn;

    private Stage dialogStage;
    private EvacSiteModel evacSite;
    private DisasterCircleInfo disaster;
    private int disasterId;
    private double evacSiteLat;
    private double evacSiteLon;

    private ObservableList<BeneficiaryAllocationRow> allBeneficiaries = FXCollections.observableArrayList();

    private GeoBasedEvacPlanDAO geoDAO;
    private EvacuationPlanDAO evacPlanDAO;
    private EvacSiteService evacSiteService;

    private int capacity;
    private int occupied;
    private int remaining;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("=== INITIALIZING AllocateBeneficiariesToEvacSiteController ===");

        DBConnection dbConnection = DBConnection.getInstance();
        geoDAO = new GeoBasedEvacPlanDAOImpl(dbConnection);
        evacPlanDAO = new EvacuationPlanDAOImpl(dbConnection);
        evacSiteService = new com.ionres.respondph.evac_site.EvacSiteServiceImpl(dbConnection);

        setupTable();
        setupButtons();

        LOGGER.info("Controller initialized successfully");
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setEvacSiteAndDisaster(EvacSiteModel evacSite, DisasterCircleInfo disaster, int disasterId) {
        LOGGER.info("=== SETTING EVAC SITE AND DISASTER DATA ===");
        LOGGER.info("Disaster ID: " + disasterId);
        LOGGER.info("Disaster: " + (disaster != null ? disaster.disasterType + " - " + disaster.disasterName : "null"));
        LOGGER.info("Evac Site: " + (evacSite != null ? evacSite.getName() : "null"));

        if (disasterId <= 0) {
            LOGGER.severe("CRITICAL: Invalid disaster ID passed to setEvacSiteAndDisaster: " + disasterId);
            AlertDialogManager.showError("Error", "Invalid disaster ID. Cannot load beneficiaries.");
            return;
        }

        this.evacSite = evacSite;
        this.disaster = disaster;
        this.disasterId = disasterId;

        if (evacSite != null) {
            try {
                this.evacSiteLat = Double.parseDouble(evacSite.getLat());
                this.evacSiteLon = Double.parseDouble(evacSite.getLongi());
                LOGGER.info("Evac site coordinates: " + evacSiteLat + ", " + evacSiteLon);
            } catch (NumberFormatException e) {
                LOGGER.log(Level.SEVERE, "Invalid evacuation site coordinates", e);
                AlertDialogManager.showError("Error", "Invalid evacuation site coordinates.");
                return;
            }
        }

        loadEvacSiteInfo();
        loadBeneficiaries();

        LOGGER.info("=== DATA SET COMPLETE ===");
    }

    private void setupTable() {
        idColumn.setCellValueFactory(cellData -> cellData.getValue().beneficiaryIdProperty().asObject());
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        scoreColumn.setCellValueFactory(cellData -> cellData.getValue().scoreProperty().asObject());
        categoryColumn.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());
        householdColumn.setCellValueFactory(cellData -> cellData.getValue().householdProperty().asObject());
        assignedSiteColumn.setCellValueFactory(cellData -> cellData.getValue().assignedSiteProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        beneficiariesTable.setItems(allBeneficiaries);
    }

    private void setupButtons() {
//        saveBtn.setOnAction(e -> saveAllocation());
        closeBtn.setOnAction(e -> closeDialog());
    }

    private void loadEvacSiteInfo() {
        LOGGER.info("Loading evacuation site info...");

        if (evacSite == null) {
            LOGGER.warning("Evacuation site is null");
            return;
        }

        evacSiteNameLabel.setText("Evacuation Site: " + evacSite.getName());

        try {
            capacity = Integer.parseInt(evacSite.getCapacity());
            occupied = evacPlanDAO.getOccupiedPersonCount(evacSite.getEvacId(), disasterId);
            remaining = capacity - occupied;

            capacityLabel.setText("Capacity: " + capacity + " persons");
            occupiedLabel.setText("Occupied: " + occupied + " persons");
            remainingLabel.setText("Remaining: " + remaining + " persons");

            if (disaster != null) {
                disasterLabel.setText("Disaster: " + disaster.disasterType + " - " + disaster.disasterName);
            } else {
                LOGGER.warning("Disaster info is null");
            }

            LOGGER.info("Evac site info loaded - Capacity: " + capacity + ", Occupied: " + occupied + ", Remaining: " + remaining);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading evacuation site info", e);
            AlertDialogManager.showError("Error", "Failed to load evacuation site information: " + e.getMessage());
        }
    }

    public void loadBeneficiaries() {
        LOGGER.info("=== LOADING BENEFICIARIES ===");
        LOGGER.info("Disaster ID for query: " + disasterId);
        LOGGER.info("Current Evac Site ID: " + (evacSite != null ? evacSite.getEvacId() : "null"));

        allBeneficiaries.clear();

        if (disasterId <= 0) {
            LOGGER.severe("CRITICAL: Cannot load beneficiaries - invalid disaster ID: " + disasterId);
            AlertDialogManager.showError("Error", "Invalid disaster ID. Cannot load beneficiaries.");
            totalBeneficiariesLabel.setText("Total Beneficiaries: 0 (Invalid disaster ID)");
            return;
        }

        try {
            LOGGER.info("Calling geoDAO.getRankedBeneficiariesWithLocation(" + disasterId + ")");
            List<RankedBeneficiaryWithLocation> rankedBeneficiaries =
                    geoDAO.getRankedBeneficiariesWithLocation(disasterId, true);

            LOGGER.info("Retrieved " + rankedBeneficiaries.size() + " ranked beneficiaries from database");

            if (rankedBeneficiaries.isEmpty()) {
                LOGGER.warning("No beneficiaries found for disaster ID: " + disasterId);
                totalBeneficiariesLabel.setText("Total Beneficiaries: 0");
                return;
            }

            List<EvacSiteWithDistance> availableSites = getAvailableEvacSitesWithCapacity();

            if (availableSites.isEmpty()) {
                LOGGER.warning("No evacuation sites with available capacity");
                totalBeneficiariesLabel.setText("Total Beneficiaries: " + rankedBeneficiaries.size() + " (No available sites)");

                for (RankedBeneficiaryWithLocation beneficiary : rankedBeneficiaries) {
                    BeneficiaryAllocationRow row = new BeneficiaryAllocationRow(
                            beneficiary.getBeneficiaryId(),
                            beneficiary.getFirstName() + " " + beneficiary.getLastName(),
                            beneficiary.getFinalScore(),
                            beneficiary.getScoreCategory(),
                            beneficiary.getHouseholdMembers(),
                            "No Capacity Available",
                            "Pending",
                            beneficiary.getLatitude(),
                            beneficiary.getLongitude()
                    );
                    allBeneficiaries.add(row);
                }
                return;
            }

            Map<Integer, Integer> siteRemainingCapacity = new HashMap<>();
            Map<Integer, String> siteNames = new HashMap<>();

            for (EvacSiteWithDistance site : availableSites) {
                siteRemainingCapacity.put(site.getEvacSiteId(), site.getRemainingCapacity());
                siteNames.put(site.getEvacSiteId(), site.getEvacSiteName());
            }

            Map<Integer, Integer> assignmentMap = new HashMap<>(); // beneficiary_id -> evac_site_id

            for (RankedBeneficiaryWithLocation beneficiary : rankedBeneficiaries) {
                if (evacPlanDAO.isAlreadyAssignedToDisaster(beneficiary.getBeneficiaryId(), disasterId)) {
                    Integer assignedEvacSiteId = evacPlanDAO.getAssignedEvacSiteId(beneficiary.getBeneficiaryId(), disasterId);

                    if (assignedEvacSiteId != null && assignedEvacSiteId == evacSite.getEvacId()) {
                        String assignedSiteName = evacSite.getName();

                        BeneficiaryAllocationRow row = new BeneficiaryAllocationRow(
                                beneficiary.getBeneficiaryId(),
                                beneficiary.getFirstName() + " " + beneficiary.getLastName(),
                                beneficiary.getFinalScore(),
                                beneficiary.getScoreCategory(),
                                beneficiary.getHouseholdMembers(),
                                assignedSiteName,
                                "Assigned to This Site",
                                beneficiary.getLatitude(),
                                beneficiary.getLongitude()
                        );
                        allBeneficiaries.add(row);
                        LOGGER.info("Beneficiary " + beneficiary.getBeneficiaryId() + " is assigned to this site - showing in table");
                    } else {
                        LOGGER.info("Beneficiary " + beneficiary.getBeneficiaryId() + " is assigned to a different site - skipping");
                    }
                    continue;
                }

                EvacSiteWithDistance nearestSite = findNearestSiteWithCapacity(
                        beneficiary.getLatitude(),
                        beneficiary.getLongitude(),
                        beneficiary.getHouseholdMembers(),
                        availableSites,
                        siteRemainingCapacity
                );

                String assignedSiteName = "No Capacity Available";
                String status = "Pending";

                if (nearestSite != null) {
                    assignedSiteName = siteNames.get(nearestSite.getEvacSiteId());
                    assignmentMap.put(beneficiary.getBeneficiaryId(), nearestSite.getEvacSiteId());

                    int remainingCap = siteRemainingCapacity.get(nearestSite.getEvacSiteId());
                    siteRemainingCapacity.put(nearestSite.getEvacSiteId(), remainingCap - beneficiary.getHouseholdMembers());
                }

                BeneficiaryAllocationRow row = new BeneficiaryAllocationRow(
                        beneficiary.getBeneficiaryId(),
                        beneficiary.getFirstName() + " " + beneficiary.getLastName(),
                        beneficiary.getFinalScore(),
                        beneficiary.getScoreCategory(),
                        beneficiary.getHouseholdMembers(),
                        assignedSiteName,
                        status,
                        beneficiary.getLatitude(),
                        beneficiary.getLongitude()
                );

                allBeneficiaries.add(row);
            }

            totalBeneficiariesLabel.setText("Total Beneficiaries: " + allBeneficiaries.size() +
                    " (Assigned to this site: " + allBeneficiaries.stream()
                    .filter(b -> "Assigned to This Site".equals(b.getStatus()))
                    .count() + ")");

            LOGGER.info("Loaded " + allBeneficiaries.size() + " beneficiaries for allocation");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading beneficiaries for disaster ID: " + disasterId, e);
            AlertDialogManager.showError("Error", "Failed to load beneficiaries: " + e.getMessage());
            totalBeneficiariesLabel.setText("Total Beneficiaries: 0 (Error)");
        }
    }

    private String getCurrentAssignmentSite(int beneficiaryId, int disasterId) {
        try {
            Integer evacSiteId = evacPlanDAO.getAssignedEvacSiteId(beneficiaryId, disasterId);

            if (evacSiteId != null && evacSiteId > 0) {
                EvacSiteModel site = evacSiteService.getEvacSiteById(evacSiteId);
                if (site != null) {
                    return site.getName();
                }
            }

            return "Already Assigned (Unknown Site)";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting current assignment site for beneficiary: " + beneficiaryId, e);
            return "Already Assigned";
        }
    }

    private void saveAllocation() {
        LOGGER.info("=== AUTOMATICALLY SAVING ALL ALLOCATIONS ===");

        List<EvacSiteWithDistance> availableSites = getAvailableEvacSitesWithCapacity();

        if (availableSites.isEmpty()) {
            AlertDialogManager.showError("No Capacity", "No evacuation sites have available capacity for this disaster.");
            return;
        }

        LOGGER.info("Available evacuation sites: " + availableSites.size());

        Map<Integer, Integer> siteRemainingCapacity = new HashMap<>();
        Map<Integer, String> siteNames = new HashMap<>();

        for (EvacSiteWithDistance site : availableSites) {
            siteRemainingCapacity.put(site.getEvacSiteId(), site.getRemainingCapacity());
            siteNames.put(site.getEvacSiteId(), site.getEvacSiteName());
        }

        int successCount = 0;
        int totalPersonsAssigned = 0;
        List<String> errors = new ArrayList<>();
        Map<Integer, Integer> allocationMap = new HashMap<>(); // beneficiary_id -> evac_site_id

        for (BeneficiaryAllocationRow row : allBeneficiaries) {
            if ("Already Assigned".equals(row.getStatus()) || "Assigned to This Site".equals(row.getStatus())) {
                LOGGER.info("Beneficiary ID " + row.getBeneficiaryId() + " already assigned - skipping");
                continue;
            }

            // Skip if no capacity available
            if ("No Capacity Available".equals(row.getAssignedSite())) {
                LOGGER.warning("No capacity available for beneficiary ID: " + row.getBeneficiaryId());
                continue;
            }

            // Check if already assigned (double-check)
            if (evacPlanDAO.isAlreadyAssignedToDisaster(row.getBeneficiaryId(), disasterId)) {
                LOGGER.warning("Beneficiary ID " + row.getBeneficiaryId() + " already assigned - skipping");
                continue;
            }

            // Find nearest site with capacity (re-calculate to ensure accuracy)
            EvacSiteWithDistance nearestSite = findNearestSiteWithCapacity(
                    row.getLatitude(),
                    row.getLongitude(),
                    row.getHousehold(),
                    availableSites,
                    siteRemainingCapacity
            );

            if (nearestSite == null) {
                String errorMsg = "No capacity available for beneficiary #" + row.getBeneficiaryId() +
                        " (needs " + row.getHousehold() + " persons)";
                errors.add(errorMsg);
                LOGGER.warning(errorMsg);
                continue;
            }

            // Create allocation notes
            String notes = String.format(
                    "Auto-allocated (Geo-Based) | Score: %.2f | Category: %s | Household: %d",
                    row.getScore(),
                    row.getCategory(),
                    row.getHousehold()
            );

            // Save allocation
            LOGGER.info("Allocating beneficiary ID: " + row.getBeneficiaryId() +
                    " to evac site ID: " + nearestSite.getEvacSiteId() +
                    " (" + siteNames.get(nearestSite.getEvacSiteId()) + ")");

            boolean success = evacPlanDAO.insertEvacPlan(
                    row.getBeneficiaryId(),
                    nearestSite.getEvacSiteId(),
                    disasterId,
                    notes
            );

            if (success) {
                // Update remaining capacity
                int remaining = siteRemainingCapacity.get(nearestSite.getEvacSiteId());
                int newRemaining = remaining - row.getHousehold();
                siteRemainingCapacity.put(nearestSite.getEvacSiteId(), newRemaining);

                successCount++;
                totalPersonsAssigned += row.getHousehold();
                allocationMap.put(row.getBeneficiaryId(), nearestSite.getEvacSiteId());

                LOGGER.info("Successfully allocated beneficiary ID: " + row.getBeneficiaryId() +
                        " to " + siteNames.get(nearestSite.getEvacSiteId()) +
                        " (remaining capacity: " + newRemaining + ")");
            } else {
                String errorMsg = "Failed to allocate beneficiary #" + row.getBeneficiaryId();
                errors.add(errorMsg);
                LOGGER.warning(errorMsg);
            }
        }

        // Show result
        if (successCount > 0) {
            StringBuilder message = new StringBuilder();
            message.append("Successfully allocated ").append(successCount)
                    .append(" beneficiaries (").append(totalPersonsAssigned).append(" persons) to nearest evacuation sites:\n\n");

            // Group allocations by evacuation site
            Map<Integer, List<Integer>> siteAllocations = new HashMap<>();
            for (Map.Entry<Integer, Integer> entry : allocationMap.entrySet()) {
                int beneficiaryId = entry.getKey();
                int evacSiteId = entry.getValue();
                siteAllocations.computeIfAbsent(evacSiteId, k -> new ArrayList<>()).add(beneficiaryId);
            }

            // Show allocation summary
            for (Map.Entry<Integer, List<Integer>> entry : siteAllocations.entrySet()) {
                int evacSiteId = entry.getKey();
                List<Integer> beneficiaryIds = entry.getValue();
                String siteName = siteNames.get(evacSiteId);

                message.append("• ").append(siteName).append(": ")
                        .append(beneficiaryIds.size()).append(" beneficiaries\n");
            }

            if (!errors.isEmpty()) {
                message.append("\nWarnings:\n").append(String.join("\n", errors));
                AlertDialogManager.showWarning("Allocation Complete", message.toString());
            } else {
                AlertDialogManager.showSuccess("Success", message.toString());
            }

            LOGGER.info("Allocation complete - Success: " + successCount +
                    ", Persons: " + totalPersonsAssigned +
                    ", Errors: " + errors.size());

            loadEvacSiteInfo();
            loadBeneficiaries();
        } else {
            if (allBeneficiaries.stream().anyMatch(b -> "Assigned to This Site".equals(b.getStatus()))) {
                AlertDialogManager.showInfo("Already Allocated",
                        "All beneficiaries shown are already assigned to their evacuation sites.");
            } else {
                AlertDialogManager.showError("Error", "Failed to allocate any beneficiaries:\n" + String.join("\n", errors));
            }
            LOGGER.info("No new allocations - all beneficiaries already assigned or no capacity");
        }
    }

    private List<EvacSiteWithDistance> getAvailableEvacSitesWithCapacity() {
        try {
            return geoDAO.getEvacSitesWithCapacity(disasterId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching available evac sites", e);
            return new ArrayList<>();
        }
    }

    private EvacSiteWithDistance findNearestSiteWithCapacity(
            double beneficiaryLat,
            double beneficiaryLon,
            int householdSize,
            List<EvacSiteWithDistance> availableSites,
            Map<Integer, Integer> siteRemainingCapacity) {

        EvacSiteWithDistance nearestSite = null;
        double minDistance = Double.MAX_VALUE;

        for (EvacSiteWithDistance site : availableSites) {
            int remainingCapacity = siteRemainingCapacity.getOrDefault(site.getEvacSiteId(), 0);

            // Check if site has enough capacity
            if (remainingCapacity < householdSize) {
                continue;
            }

            // Calculate distance between beneficiary and this evacuation site
            double distance = GeoDistanceCalculator.calculateDistance(
                    beneficiaryLat,
                    beneficiaryLon,
                    site.getLatitude(),
                    site.getLongitude()
            );

            // Give slight preference to the clicked site (10% distance reduction)
            if (site.getEvacSiteId() == evacSite.getEvacId()) {
                distance = distance * 0.9; // 10% preference for clicked site
            }

            // Update nearest site if this one is closer
            if (distance < minDistance) {
                minDistance = distance;
                nearestSite = site;
            }
        }

        return nearestSite;
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    // Inner class for table rows
    public static class BeneficiaryAllocationRow {
        private final SimpleIntegerProperty beneficiaryId;
        private final SimpleStringProperty name;
        private final SimpleDoubleProperty score;
        private final SimpleStringProperty category;
        private final SimpleIntegerProperty household;
        private final SimpleStringProperty assignedSite;
        private final SimpleStringProperty status;
        private final double latitude;
        private final double longitude;

        public BeneficiaryAllocationRow(int beneficiaryId, String name, double score, String category,
                                        int household, String assignedSite, String status) {
            this(beneficiaryId, name, score, category, household, assignedSite, status, 0.0, 0.0);
        }

        public BeneficiaryAllocationRow(int beneficiaryId, String name, double score, String category,
                                        int household, String assignedSite, String status,
                                        double latitude, double longitude) {
            this.beneficiaryId = new SimpleIntegerProperty(beneficiaryId);
            this.name = new SimpleStringProperty(name);
            this.score = new SimpleDoubleProperty(score);
            this.category = new SimpleStringProperty(category);
            this.household = new SimpleIntegerProperty(household);
            this.assignedSite = new SimpleStringProperty(assignedSite);
            this.status = new SimpleStringProperty(status);
            this.latitude = latitude;
            this.longitude = longitude;
        }

        // Property getters
        public SimpleIntegerProperty beneficiaryIdProperty() { return beneficiaryId; }
        public SimpleStringProperty nameProperty() { return name; }
        public SimpleDoubleProperty scoreProperty() { return score; }
        public SimpleStringProperty categoryProperty() { return category; }
        public SimpleIntegerProperty householdProperty() { return household; }
        public SimpleStringProperty assignedSiteProperty() { return assignedSite; }
        public SimpleStringProperty statusProperty() { return status; }

        // Value getters
        public int getBeneficiaryId() { return beneficiaryId.get(); }
        public String getName() { return name.get(); }
        public double getScore() { return score.get(); }
        public String getCategory() { return category.get(); }
        public int getHousehold() { return household.get(); }
        public String getAssignedSite() { return assignedSite.get(); }
        public String getStatus() { return status.get(); }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
    }
}