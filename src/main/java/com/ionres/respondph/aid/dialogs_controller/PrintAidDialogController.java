//package com.ionres.respondph.aid.dialogs_controller;
//
//import com.ionres.respondph.aid.AidDAO;
//import com.ionres.respondph.aid.AidDAOImpl;
//import com.ionres.respondph.aid.AidModel;
//import com.ionres.respondph.aid.AidPrintService;
//import com.ionres.respondph.aid_type.AidTypeDAO;
//import com.ionres.respondph.aid_type.AidTypeDAOImpl;
//import com.ionres.respondph.aid_type.AidTypeModelComboBox;
//import com.ionres.respondph.database.DBConnection;
//import com.ionres.respondph.disaster.DisasterDAO;
//import com.ionres.respondph.disaster.DisasterDAOImpl;
//import com.ionres.respondph.disaster.DisasterModelComboBox;
//import com.ionres.respondph.util.AlertDialogManager;
//import com.ionres.respondph.util.DashboardRefresher;
//import javafx.collections.FXCollections;
//import javafx.fxml.FXML;
//import javafx.scene.control.Button;
//import javafx.scene.control.ComboBox;
//import javafx.scene.control.Label;
//import javafx.stage.Stage;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//public class PrintAidDialogController {
//
//    @FXML private ComboBox<DisasterModelComboBox> disasterComboBox;
//    @FXML private ComboBox<AidTypeModelComboBox> aidTypeComboBox;
//    @FXML private Label beneficiaryCountLabel;
//    @FXML private Button printButton;
//    @FXML private Button cancelButton;
//    @FXML private Button previewButton;
//
//    private DisasterDAO disasterDAO;
//    private AidTypeDAO aidTypeDAO;
//    private AidDAO aidDAO;
//    private AidPrintService printService;
//
//    @FXML
//    private void initialize() {
//        disasterDAO = new DisasterDAOImpl(DBConnection.getInstance());
//        aidTypeDAO = new AidTypeDAOImpl(DBConnection.getInstance());
//        aidDAO = new AidDAOImpl(DBConnection.getInstance());
//        printService = new AidPrintService();
//
//        DashboardRefresher.registerDisasterNameAndAidtypeName(this);
//
//        setupComboBoxes();
//        setupListeners();
//        setupButtons();
//    }
//
//    private void setupComboBoxes() {
//        List<DisasterModelComboBox> disasters = disasterDAO.findAll();
//        disasterComboBox.setItems(FXCollections.observableArrayList(disasters));
//
//        List<AidTypeModelComboBox> aidTypes = aidTypeDAO.findAll();
//        aidTypeComboBox.setItems(FXCollections.observableArrayList(aidTypes));
//
//        disasterComboBox.setPromptText("Select Disaster");
//        aidTypeComboBox.setPromptText("Select Aid Type");
//    }
//
//    private void setupListeners() {
//        disasterComboBox.setOnAction(e -> updateBeneficiaryCount());
//        aidTypeComboBox.setOnAction(e -> updateBeneficiaryCount());
//    }
//
//    private void setupButtons() {
//        printButton.setOnAction(e -> handlePrint());
//        cancelButton.setOnAction(e -> handleCancel());
//        previewButton.setOnAction(e -> handlePreview());
//    }
//
//    private void updateBeneficiaryCount() {
//        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
//        AidTypeModelComboBox selectedAidType = aidTypeComboBox.getValue();
//
//        if (selectedDisaster == null || selectedAidType == null) {
//            beneficiaryCountLabel.setText("Beneficiaries: 0");
//            printButton.setDisable(true);
//            previewButton.setDisable(true);
//            return;
//        }
//
//        // Get beneficiaries for this disaster and aid type
//        List<AidModel> aidRecords = getAidRecords(selectedDisaster.getDisasterId(), selectedAidType.getAidTypeId());
//
//        beneficiaryCountLabel.setText("Beneficiaries: " + aidRecords.size());
//        printButton.setDisable(aidRecords.isEmpty());
//        previewButton.setDisable(aidRecords.isEmpty());
//    }
//
//    private void handlePrint() {
//        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
//        AidTypeModelComboBox selectedAidType = aidTypeComboBox.getValue();
//
//        if (selectedDisaster == null || selectedAidType == null) {
//            AlertDialogManager.showWarning("Selection Required", "Please select both Disaster and Aid Type.");
//            return;
//        }
//
//        // Get aid records
//        List<AidModel> aidRecords = getAidRecords(selectedDisaster.getDisasterId(), selectedAidType.getAidTypeId());
//
//        if (aidRecords.isEmpty()) {
//            AlertDialogManager.showWarning("No Data", "No beneficiaries found for the selected disaster and aid type.");
//            return;
//        }
//
//        // Print
//        boolean success = printService.printSpecificReport(
//                selectedDisaster.getDisasterName(),
//                selectedAidType.getAidName(),
//                aidRecords
//        );
//
//        if (success) {
//            closeDialog();
//        }
//    }
//
//    private void handlePreview() {
//        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
//        AidTypeModelComboBox selectedAidType = aidTypeComboBox.getValue();
//
//        if (selectedDisaster == null || selectedAidType == null) {
//            AlertDialogManager.showWarning("Selection Required", "Please select both Disaster and Aid Type.");
//            return;
//        }
//
//        List<AidModel> aidRecords = getAidRecords(selectedDisaster.getDisasterId(), selectedAidType.getAidTypeId());
//
//        if (aidRecords.isEmpty()) {
//            AlertDialogManager.showWarning("No Data", "No beneficiaries found.");
//            return;
//        }
//
//        // Show preview (list of names)
//        StringBuilder preview = new StringBuilder();
//        preview.append("Disaster: ").append(selectedDisaster.getDisasterName()).append("\n");
//        preview.append("Aid Type: ").append(selectedAidType.getAidName()).append("\n");
//        preview.append("Total Beneficiaries: ").append(aidRecords.size()).append("\n\n");
//        preview.append("Beneficiaries:\n");
//
//        int counter = 1;
//        for (AidModel aid : aidRecords) {
//            preview.append(counter++).append(". ").append(aid.getBeneficiaryName()).append("\n");
//        }
//
//        AlertDialogManager.showInfo("Preview", preview.toString());
//    }
//
//    private void handleCancel() {
//        closeDialog();
//    }
//
//    private List<AidModel> getAidRecords(int disasterId, int aidTypeId) {
//        List<AidModel> allAid = aidDAO.getAllAidForTable();
//
//        // Filter by disaster and aid type
//        return allAid.stream()
//                .filter(aid -> aid.getDisasterId() == disasterId && aid.getAidTypeId() == aidTypeId)
//                .collect(Collectors.toList());
//    }
//
//    private void closeDialog() {
//        Stage stage = (Stage) cancelButton.getScene().getWindow();
//        stage.close();
//    }
//
////    private void handlePrintAll() {
////        boolean confirmed = AlertDialogManager.showConfirmation(
////                "Print All Records",
////                "This will print ALL aid distribution records grouped by disaster and aid type.\n\n" +
////                        "This may generate multiple pages. Continue?"
////        );
////
////        if (!confirmed) {
////            return;
////        }
////
////        // Get all aid records
////        List<AidModel> allAidRecords = aidDAO.getAllAidForTable();
////
////        if (allAidRecords.isEmpty()) {
////            AlertDialogManager.showWarning("No Data", "No aid records found in the system.");
////            return;
////        }
////
////        // Print all records
////        boolean success = printService.printAidDistributionReport(allAidRecords);
////
////        if (success) {
////            closeDialog();
////        }
////    }
//
//    private void loadDisasters() {
//        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
//
//        List<DisasterModelComboBox> disasters = disasterDAO.findAll();
//        disasterComboBox.setItems(FXCollections.observableArrayList(disasters));
//
//        // Try to restore selection
//        if (selectedDisaster != null) {
//            disasterComboBox.getItems().stream()
//                    .filter(d -> d.getDisasterId() == selectedDisaster.getDisasterId())
//                    .findFirst()
//                    .ifPresent(disasterComboBox::setValue);
//        }
//    }
//
//    // ADD THIS METHOD
//    private void loadAidTypes() {
//        AidTypeModelComboBox selectedAidType = aidTypeComboBox.getValue();
//
//        List<AidTypeModelComboBox> aidTypes = aidTypeDAO.findAll();
//        aidTypeComboBox.setItems(FXCollections.observableArrayList(aidTypes));
//
//        // Try to restore selection
//        if (selectedAidType != null) {
//            aidTypeComboBox.getItems().stream()
//                    .filter(at -> at.getAidTypeId() == selectedAidType.getAidTypeId())
//                    .findFirst()
//                    .ifPresent(aidTypeComboBox::setValue);
//        }
//    }
//
//    // ADD THIS PUBLIC METHOD - Called by DashboardRefresher
//    public void refreshComboBoxes() {
//        loadDisasters();
//        loadAidTypes();
//        updateBeneficiaryCount();
//    }
//}
package com.ionres.respondph.aid.dialogs_controller;

import com.ionres.respondph.aid.AidDAO;
import com.ionres.respondph.aid.AidDAOImpl;
import com.ionres.respondph.aid.AidModel;
import com.ionres.respondph.aid.AidPrintService;
import com.ionres.respondph.aid_type.AidTypeDAO;
import com.ionres.respondph.aid_type.AidTypeDAOImpl;
import com.ionres.respondph.aid_type.AidTypeModelComboBox;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.disaster.DisasterDAO;
import com.ionres.respondph.disaster.DisasterDAOImpl;
import com.ionres.respondph.disaster.DisasterModelComboBox;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.util.DashboardRefresher;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * PrintAidDialogController with Barangay Filtering and K-Means Score Sorting
 *
 * Features:
 * - Filter beneficiaries by barangay
 * - Sort by K-means priority scores (highest first)
 * - Show disaster, aid type, and barangay information
 */
public class PrintAidDialogController {

    @FXML private ComboBox<DisasterModelComboBox> disasterComboBox;
    @FXML private ComboBox<AidTypeModelComboBox> aidTypeComboBox;
    @FXML private ComboBox<String> barangayComboBox;
    @FXML private CheckBox useBarangayFilterCheckBox;
    @FXML private Label beneficiaryCountLabel;
    @FXML private Button printButton;
    @FXML private Button cancelButton;
    @FXML private Button previewButton;

    private DisasterDAO disasterDAO;
    private AidTypeDAO aidTypeDAO;
    private AidDAO aidDAO;
    private AidPrintService printService;
    private Cryptography cs;

    private static final String ALL_BARANGAYS = "All Barangays";

    @FXML
    private void initialize() {
        disasterDAO = new DisasterDAOImpl(DBConnection.getInstance());
        aidTypeDAO = new AidTypeDAOImpl(DBConnection.getInstance());
        aidDAO = new AidDAOImpl(DBConnection.getInstance());
        printService = new AidPrintService();
        cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

        DashboardRefresher.registerDisasterNameAndAidtypeName(this);

        setupComboBoxes();
        setupListeners();
        setupButtons();
        setupBarangayFilter();
    }

    private void setupComboBoxes() {
        List<DisasterModelComboBox> disasters = disasterDAO.findAll();
        disasterComboBox.setItems(FXCollections.observableArrayList(disasters));

        List<AidTypeModelComboBox> aidTypes = aidTypeDAO.findAll();
        aidTypeComboBox.setItems(FXCollections.observableArrayList(aidTypes));

        disasterComboBox.setPromptText("Select Disaster");
        aidTypeComboBox.setPromptText("Select Aid Type");
        if (barangayComboBox != null) {
            barangayComboBox.setPromptText("Select Barangay");
        }
    }

    private void setupBarangayFilter() {
        // If barangay components exist in FXML, set them up
        if (barangayComboBox != null && useBarangayFilterCheckBox != null) {
            barangayComboBox.setDisable(true);
            useBarangayFilterCheckBox.setSelected(false);

            // Setup checkbox listener
            useBarangayFilterCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                barangayComboBox.setDisable(!newVal);
                if (!newVal) {
                    barangayComboBox.setValue(ALL_BARANGAYS);
                }
                updateBeneficiaryCount();
            });
        }
    }

    private void setupListeners() {
        disasterComboBox.setOnAction(e -> {
            loadBarangays();
            updateBeneficiaryCount();
        });
        aidTypeComboBox.setOnAction(e -> updateBeneficiaryCount());
        if (barangayComboBox != null) {
            barangayComboBox.setOnAction(e -> updateBeneficiaryCount());
        }
    }

    private void setupButtons() {
        printButton.setOnAction(e -> handlePrint());
        cancelButton.setOnAction(e -> handleCancel());
        previewButton.setOnAction(e -> handlePreview());
    }

    private void loadBarangays() {
        if (barangayComboBox == null) return;

        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();

        List<String> barangays;
        if (selectedDisaster != null) {
            barangays = aidDAO.getBarangaysByDisaster(selectedDisaster.getDisasterId());
        } else {
            barangays = aidDAO.getAllBarangays();
        }

        // Sort alphabetically
        barangays = barangays.stream().sorted().collect(Collectors.toList());

        // Add "All Barangays" option at the beginning
        barangays.add(0, ALL_BARANGAYS);

        barangayComboBox.setItems(FXCollections.observableArrayList(barangays));
        barangayComboBox.setValue(ALL_BARANGAYS);

        System.out.println("Loaded " + (barangays.size() - 1) + " barangays for printing");
    }

    private void updateBeneficiaryCount() {
        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
        AidTypeModelComboBox selectedAidType = aidTypeComboBox.getValue();

        if (selectedDisaster == null || selectedAidType == null) {
            beneficiaryCountLabel.setText("Beneficiaries: 0");
            printButton.setDisable(true);
            previewButton.setDisable(true);
            return;
        }

        // Get filtered and sorted aid records
        List<AidModel> aidRecords = getFilteredAndSortedAidRecords(
                selectedDisaster.getDisasterId(),
                selectedAidType.getAidTypeId()
        );

        String barangayInfo = getBarangayInfoText();
        beneficiaryCountLabel.setText("Beneficiaries: " + aidRecords.size() + barangayInfo);
        printButton.setDisable(aidRecords.isEmpty());
        previewButton.setDisable(aidRecords.isEmpty());
    }

    private String getBarangayInfoText() {
        if (!isBarangayFilterActive()) {
            return "";
        }
        return " (Barangay: " + barangayComboBox.getValue() + ")";
    }

    private void handlePrint() {
        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
        AidTypeModelComboBox selectedAidType = aidTypeComboBox.getValue();

        if (selectedDisaster == null || selectedAidType == null) {
            AlertDialogManager.showWarning("Selection Required",
                    "Please select both Disaster and Aid Type.");
            return;
        }

        // Get filtered and sorted records
        List<AidModel> aidRecords = getFilteredAndSortedAidRecords(
                selectedDisaster.getDisasterId(),
                selectedAidType.getAidTypeId()
        );

        if (aidRecords.isEmpty()) {
            AlertDialogManager.showWarning("No Data",
                    "No beneficiaries found for the selected criteria.");
            return;
        }

        // Build report title with barangay info
        String reportTitle = buildReportTitle(
                selectedDisaster.getDisasterName(),
                selectedAidType.getAidName()
        );

        // Print
        boolean success = printService.printSpecificReport(
                reportTitle,
                selectedAidType.getAidName(),
                aidRecords
        );

        if (success) {
            closeDialog();
        }
    }

    private void handlePreview() {
        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
        AidTypeModelComboBox selectedAidType = aidTypeComboBox.getValue();

        if (selectedDisaster == null || selectedAidType == null) {
            AlertDialogManager.showWarning("Selection Required",
                    "Please select both Disaster and Aid Type.");
            return;
        }

        List<AidModel> aidRecords = getFilteredAndSortedAidRecords(
                selectedDisaster.getDisasterId(),
                selectedAidType.getAidTypeId()
        );

        if (aidRecords.isEmpty()) {
            AlertDialogManager.showWarning("No Data", "No beneficiaries found.");
            return;
        }

        // Build preview with barangay and score information
        StringBuilder preview = new StringBuilder();
        preview.append("═".repeat(60)).append("\n");
        preview.append("          AID DISTRIBUTION PREVIEW          \n");
        preview.append("═".repeat(60)).append("\n\n");

        preview.append("Disaster: ").append(selectedDisaster.getDisasterName()).append("\n");
        preview.append("Aid Type: ").append(selectedAidType.getAidName()).append("\n");

        if (isBarangayFilterActive()) {
            preview.append("Barangay: ").append(barangayComboBox.getValue()).append("\n");
        } else {
            preview.append("Barangay: All Barangays\n");
        }

        preview.append("Total Beneficiaries: ").append(aidRecords.size()).append("\n");
        preview.append("Sorting: K-Means Priority (High to Low)\n");
        preview.append("\n").append("─".repeat(60)).append("\n");
        preview.append(String.format("%-4s %-35s %s\n", "No.", "Beneficiary Name", "Priority Score"));
        preview.append("─".repeat(60)).append("\n");

        int counter = 1;
        for (AidModel aid : aidRecords) {
            String scoreInfo = extractScoreInfo(aid.getNotes());
            preview.append(String.format("%-4d %-35s %s\n",
                    counter++,
                    truncateName(aid.getBeneficiaryName(), 35),
                    scoreInfo
            ));
        }

        preview.append("─".repeat(60)).append("\n");
        preview.append("\nNote: Beneficiaries are sorted by K-Means priority score.\n");
        preview.append("Higher scores indicate higher priority for aid distribution.\n");

        // Show in scrollable dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Distribution Preview");
        alert.setHeaderText("Barangay-Filtered Aid Distribution");

        TextArea textArea = new TextArea(preview.toString());
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setPrefRowCount(25);
        textArea.setPrefColumnCount(65);
        textArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px;");

        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefWidth(700);
        alert.showAndWait();
    }

    private void handleCancel() {
        closeDialog();
    }


    private List<AidModel> getFilteredAndSortedAidRecords(int disasterId, int aidTypeId) {
        // Step 1: Get all aid records for this disaster and aid type
        List<AidModel> allAid = aidDAO.getAllAidForTable();

        List<AidModel> filtered = allAid.stream()
                .filter(aid -> aid.getDisasterId() == disasterId &&
                        aid.getAidTypeId() == aidTypeId)
                .collect(Collectors.toList());

        // Step 2: Apply barangay filter if enabled
        if (isBarangayFilterActive()) {
            String selectedBarangay = barangayComboBox.getValue();
            filtered = filterByBarangay(filtered, selectedBarangay);
        }

        // Step 3: Sort by K-means score (highest priority first)
        filtered = sortByKMeansScore(filtered);

        return filtered;
    }

    /**
     * Check if barangay filter is active
     */
    private boolean isBarangayFilterActive() {
        return useBarangayFilterCheckBox != null &&
                useBarangayFilterCheckBox.isSelected() &&
                barangayComboBox != null &&
                barangayComboBox.getValue() != null &&
                !barangayComboBox.getValue().equals(ALL_BARANGAYS);
    }

    /**
     * Filter aid records by barangay
     * Matches beneficiaries belonging to the specified barangay
     */
    private List<AidModel> filterByBarangay(List<AidModel> aidRecords, String barangay) {
        // Get beneficiary IDs for this barangay
        Set<Integer> barangayBeneficiaryIds = getBeneficiaryIdsByBarangay(barangay);

        // Filter aid records
        return aidRecords.stream()
                .filter(aid -> barangayBeneficiaryIds.contains(aid.getBeneficiaryId()))
                .collect(Collectors.toList());
    }

    /**
     * Get all beneficiary IDs for a specific barangay
     */
    private Set<Integer> getBeneficiaryIdsByBarangay(String barangay) {
        Set<Integer> beneficiaryIds = new HashSet<>();

        String sql = "SELECT beneficiary_id, barangay FROM beneficiary WHERE barangay IS NOT NULL";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String encryptedBarangay = rs.getString("barangay");
                try {
                    String decryptedBarangay = cs.decryptWithOneParameter(encryptedBarangay);
                    if (decryptedBarangay != null && decryptedBarangay.equals(barangay)) {
                        beneficiaryIds.add(rs.getInt("beneficiary_id"));
                    }
                } catch (Exception e) {
                    System.err.println("Error decrypting barangay: " + e.getMessage());
                }
            }

            rs.close();
            ps.close();
            conn.close();

        } catch (Exception e) {
            System.err.println("Error fetching beneficiary IDs by barangay: " + e.getMessage());
            e.printStackTrace();
        }

        return beneficiaryIds;
    }

    /**
     * Sort aid records by K-means priority score (highest first)
     * Extracts score from notes field
     */
    private List<AidModel> sortByKMeansScore(List<AidModel> aidRecords) {
        return aidRecords.stream()
                .sorted((aid1, aid2) -> {
                    double score1 = extractScore(aid1.getNotes());
                    double score2 = extractScore(aid2.getNotes());
                    return Double.compare(score2, score1); // Descending order (highest first)
                })
                .collect(Collectors.toList());
    }

    /**
     * Extract numerical score from notes field
     * Format: "K-means Distribution | Priority: High | Score: 0.850 | Cluster: 2"
     */
    private double extractScore(String notes) {
        if (notes == null || notes.isEmpty()) {
            return 0.0;
        }

        try {
            // Pattern to match "Score: X.XXX"
            Pattern pattern = Pattern.compile("Score:\\s*([0-9]+\\.?[0-9]*)");
            Matcher matcher = pattern.matcher(notes);

            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {
            System.err.println("Error extracting score from notes: " + e.getMessage());
        }

        return 0.0;
    }

    /**
     * Extract score information for display
     */
    private String extractScoreInfo(String notes) {
        if (notes == null || notes.isEmpty()) {
            return "N/A";
        }

        try {
            // Extract score
            Pattern scorePattern = Pattern.compile("Score:\\s*([0-9]+\\.?[0-9]*)");
            Matcher scoreMatcher = scorePattern.matcher(notes);

            // Extract cluster
            Pattern clusterPattern = Pattern.compile("Cluster:\\s*([0-9]+)");
            Matcher clusterMatcher = clusterPattern.matcher(notes);

            // Extract priority category
            Pattern priorityPattern = Pattern.compile("Priority:\\s*([A-Za-z]+)");
            Matcher priorityMatcher = priorityPattern.matcher(notes);

            String score = scoreMatcher.find() ? scoreMatcher.group(1) : "N/A";
            String cluster = clusterMatcher.find() ? clusterMatcher.group(1) : "N/A";
            String priority = priorityMatcher.find() ? priorityMatcher.group(1) : "N/A";

            return String.format("%s (C%s) - %s", score, cluster, priority);
        } catch (Exception e) {
            return "N/A";
        }
    }

    /**
     * Build report title with barangay information
     */
    private String buildReportTitle(String disasterName, String aidName) {
        if (isBarangayFilterActive()) {
            return disasterName + " - Barangay " + barangayComboBox.getValue();
        }
        return disasterName;
    }

    /**
     * Truncate name to fit column width
     */
    private String truncateName(String name, int maxLength) {
        if (name == null) return "";
        if (name.length() <= maxLength) return name;
        return name.substring(0, maxLength - 3) + "...";
    }

    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    // Methods for DashboardRefresher
    private void loadDisasters() {
        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
        List<DisasterModelComboBox> disasters = disasterDAO.findAll();
        disasterComboBox.setItems(FXCollections.observableArrayList(disasters));

        if (selectedDisaster != null) {
            disasterComboBox.getItems().stream()
                    .filter(d -> d.getDisasterId() == selectedDisaster.getDisasterId())
                    .findFirst()
                    .ifPresent(disasterComboBox::setValue);
        }
    }

    private void loadAidTypes() {
        AidTypeModelComboBox selectedAidType = aidTypeComboBox.getValue();
        List<AidTypeModelComboBox> aidTypes = aidTypeDAO.findAll();
        aidTypeComboBox.setItems(FXCollections.observableArrayList(aidTypes));

        if (selectedAidType != null) {
            aidTypeComboBox.getItems().stream()
                    .filter(at -> at.getAidTypeId() == selectedAidType.getAidTypeId())
                    .findFirst()
                    .ifPresent(aidTypeComboBox::setValue);
        }
    }

    public void refreshComboBoxes() {
        loadDisasters();
        loadAidTypes();
        loadBarangays();
        updateBeneficiaryCount();
    }
}