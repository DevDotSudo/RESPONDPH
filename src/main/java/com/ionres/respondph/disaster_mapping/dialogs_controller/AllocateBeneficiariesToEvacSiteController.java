package com.ionres.respondph.disaster_mapping.dialogs_controller;
import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.common.model.DisasterCircleInfo;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.evacuation_plan.*;
import com.ionres.respondph.evac_site.EvacSiteModel;
import com.ionres.respondph.evac_site.EvacSiteService;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.ThemeManager;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    @FXML private TextField searchField;
    private double yOffset = 0;
    private double xOffset = 0;
    private FilteredList<BeneficiaryAllocationRow> filteredBeneficiaries;
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

        // Apply light mode to root if active
        if (ThemeManager.getInstance().isLightMode()) {
            if (root != null && !root.getStyleClass().contains("root-light")) {
                root.getStyleClass().add("root-light");
            }
        }

        setupTable();
        setupButtons();
        makeDraggable();
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

        filteredBeneficiaries = new FilteredList<>(allBeneficiaries, b -> true);

        // Wrap FilteredList in a SortedList and bind it to the table's comparator
        // so column-header sorting still works correctly
        SortedList<BeneficiaryAllocationRow> sortedBeneficiaries = new SortedList<>(filteredBeneficiaries);
        sortedBeneficiaries.comparatorProperty().bind(beneficiariesTable.comparatorProperty());

        beneficiariesTable.setItems(sortedBeneficiaries);
    }

    private void setupButtons() {
        closeBtn.setOnAction(e -> closeDialog());

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applySearch(newValue);
        });
    }

    private void applySearch(String keyword) {
        if (filteredBeneficiaries == null) return;

        if (keyword == null || keyword.isBlank()) {
            filteredBeneficiaries.setPredicate(b -> true);
            return;
        }

        String lower = keyword.toLowerCase().trim();

        filteredBeneficiaries.setPredicate(b ->
                String.valueOf(b.getBeneficiaryId()).contains(lower) ||
                        b.getName().toLowerCase().contains(lower) ||
                        b.getCategory().toLowerCase().contains(lower) ||
                        b.getAssignedSite().toLowerCase().contains(lower) ||
                        b.getStatus().toLowerCase().contains(lower)
        );
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
            // Get raw list of beneficiaries assigned to this site
            List<BeneficiaryModel> alreadyAssigned = evacPlanDAO.getBeneficiariesByEvacSiteAndDisaster(
                    evacSite.getEvacId(),
                    disasterId
            );

            LOGGER.info("Found " + alreadyAssigned.size() + " beneficiaries assigned to this site");

            // Track beneficiary IDs that are already assigned to this site
            Set<Integer> assignedBeneficiaryIds = new HashSet<>();

            // Get all ranked beneficiaries to fetch score data
            List<RankedBeneficiaryWithLocation> rankedBeneficiaries =
                    geoDAO.getRankedBeneficiariesWithLocation(disasterId, true);

            // Create a map for quick score lookup
            Map<Integer, RankedBeneficiaryWithLocation> scoreMap = new HashMap<>();
            for (RankedBeneficiaryWithLocation ranked : rankedBeneficiaries) {
                scoreMap.put(ranked.getBeneficiaryId(), ranked);
            }

            // Add already assigned beneficiaries to the table
            for (BeneficiaryModel beneficiary : alreadyAssigned) {
                assignedBeneficiaryIds.add(beneficiary.getId());

                int householdSize = evacPlanDAO.getHouseholdSizeForBeneficiary(
                        beneficiary.getId(),
                        disasterId
                );

                // Get score info from the map
                double score = 0.0;
                String category = "N/A";

                RankedBeneficiaryWithLocation rankedInfo = scoreMap.get(beneficiary.getId());
                if (rankedInfo != null) {
                    score = rankedInfo.getFinalScore();
                    category = rankedInfo.getScoreCategory();
                    LOGGER.info("Found score for beneficiary #" + beneficiary.getId() +
                            ": " + score + " (" + category + ")");
                } else {
                    LOGGER.warning("No score found for beneficiary #" + beneficiary.getId());
                }

                BeneficiaryAllocationRow row = new BeneficiaryAllocationRow(
                        beneficiary.getId(),
                        beneficiary.getFirstname() + " " + beneficiary.getLastname(),
                        score,
                        category,
                        householdSize,
                        evacSite.getName(),
                        "Assigned to This Site",
                        Double.parseDouble(beneficiary.getLatitude()),
                        Double.parseDouble(beneficiary.getLongitude())
                );
                allBeneficiaries.add(row);

                LOGGER.info("Added beneficiary #" + beneficiary.getId() +
                        " - Score: " + score + ", Household: " + householdSize);
            }

            totalBeneficiariesLabel.setText("Total Beneficiaries: " + allBeneficiaries.size() +
                    " (Assigned to this site: " + alreadyAssigned.size() + ")");

            LOGGER.info("=== BENEFICIARY LOADING COMPLETE ===");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading beneficiaries for disaster ID: " + disasterId, e);
            AlertDialogManager.showError("Error", "Failed to load beneficiaries: " + e.getMessage());
            totalBeneficiariesLabel.setText("Total Beneficiaries: 0 (Error)");
        }
    }


    // Add this helper method to debug missing beneficiaries
    private void debugMissingBeneficiaries(int evacSiteId, int disasterId) {
        try {
            DBConnection dbConnection = DBConnection.getInstance();
            Connection conn = dbConnection.getConnection();

            String sql = "SELECT ep.beneficiary_id, ep.evac_event_id, " +
                    "b.first_name, b.last_name, b.latitude, b.longitude, " +
                    "COALESCE(ahs.household_members, " +
                    "(SELECT COUNT(*) FROM family_member fm WHERE fm.beneficiary_id = ep.beneficiary_id) + 1) AS household " +
                    "FROM evac_plan ep " +
                    "INNER JOIN beneficiary b ON ep.beneficiary_id = b.beneficiary_id " +
                    "LEFT JOIN aid_and_household_score ahs ON ep.beneficiary_id = ahs.beneficiary_id AND ahs.disaster_id = ep.disaster_id " +
                    "WHERE ep.evac_site_id = ? AND ep.disaster_id = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, evacSiteId);
            ps.setInt(2, disasterId);
            ResultSet rs = ps.executeQuery();

            LOGGER.severe("=== RAW EVAC_PLAN ENTRIES ===");
            int count = 0;
            while (rs.next()) {
                count++;
                int benId = rs.getInt("beneficiary_id");
                String lat = rs.getString("latitude");
                String lon = rs.getString("longitude");
                int household = rs.getInt("household");

                LOGGER.severe("  Entry #" + count + ": Beneficiary ID = " + benId +
                        ", Household = " + household +
                        ", Lat = " + lat + ", Lon = " + lon);
            }
            LOGGER.severe("=== TOTAL RAW ENTRIES: " + count + " ===");

            rs.close();
            ps.close();
            conn.close();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in debugMissingBeneficiaries", e);
        }
    }


    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void makeDraggable() {
        root.setOnMousePressed(event -> {
            yOffset = event.getSceneY();
            xOffset = event.getSceneX();
        });
        root.setOnMouseDragged(event -> {
           if(dialogStage != null) {
               dialogStage.setY(event.getScreenY() - yOffset);
               dialogStage.setX(event.getScreenX() - xOffset);
           }
        });
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