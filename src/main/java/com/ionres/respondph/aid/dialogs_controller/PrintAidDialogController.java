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

public class PrintAidDialogController {

    @FXML private ComboBox<DisasterModelComboBox> disasterComboBox;
    @FXML private ComboBox<String> aidNameComboBox;
    @FXML private ComboBox<String> barangayComboBox;
    @FXML private CheckBox useBarangayFilterCheckBox;
    @FXML private CheckBox generalAidCheckBox;
    @FXML private Label beneficiaryCountLabel;
    @FXML private Button printButton;
    @FXML private Button cancelButton;
    @FXML private Button previewButton;

    private DisasterDAO disasterDAO;
    private AidDAO aidDAO;
    private AidPrintService printService;
    private Cryptography cs;

    private static final String ALL_BARANGAYS = "All Barangays";

    @FXML
    private void initialize() {
        disasterDAO = new DisasterDAOImpl(DBConnection.getInstance());
        aidDAO = new AidDAOImpl(DBConnection.getInstance());
        printService = new AidPrintService();
        cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

        DashboardRefresher.registerDisasterNameAndAidtypeName(this);

        setupComboBoxes();
        setupGeneralAidOption();
        setupListeners();
        setupButtons();
        setupBarangayFilter();
    }

    private void setupComboBoxes() {
        List<DisasterModelComboBox> disasters = disasterDAO.findAll();
        disasterComboBox.setItems(FXCollections.observableArrayList(disasters));

        // Get distinct aid names from aid table
        List<String> aidNames = aidDAO.getDistinctAidNames();
        aidNameComboBox.setItems(FXCollections.observableArrayList(aidNames));

        disasterComboBox.setPromptText("Select Disaster");
        aidNameComboBox.setPromptText("Select Aid Name");
        if (barangayComboBox != null) {
            barangayComboBox.setPromptText("Select Barangay");
        }
    }

    private void setupGeneralAidOption() {
        if (generalAidCheckBox != null) {
            generalAidCheckBox.setSelected(false);

            generalAidCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    disasterComboBox.setValue(null);
                    disasterComboBox.setDisable(true);
                } else {
                    disasterComboBox.setDisable(false);
                }
                loadBarangays();
                updateBeneficiaryCount();
            });
        }
    }

    private void setupBarangayFilter() {
        if (barangayComboBox != null && useBarangayFilterCheckBox != null) {
            barangayComboBox.setDisable(true);
            useBarangayFilterCheckBox.setSelected(false);

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
            if (disasterComboBox.getValue() != null && generalAidCheckBox != null) {
                generalAidCheckBox.setSelected(false);
            }
            loadBarangays();
            updateBeneficiaryCount();
        });

        aidNameComboBox.setOnAction(e -> updateBeneficiaryCount());

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

        int disasterId = getSelectedDisasterId();

        List<String> barangays = aidDAO.getBarangaysByDisaster(disasterId);

        barangays = barangays.stream().sorted().collect(Collectors.toList());
        barangays.add(0, ALL_BARANGAYS);

        barangayComboBox.setItems(FXCollections.observableArrayList(barangays));
        barangayComboBox.setValue(ALL_BARANGAYS);

        String context = disasterId > 0 ? "disaster #" + disasterId : "general aid";
        System.out.println("Loaded " + (barangays.size() - 1) + " barangays for " + context);
    }

    private int getSelectedDisasterId() {
        if (generalAidCheckBox != null && generalAidCheckBox.isSelected()) {
            return 0; // General aids
        }

        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
        return selectedDisaster != null ? selectedDisaster.getDisasterId() : -1;
    }

    private void updateBeneficiaryCount() {
        String selectedAidName = aidNameComboBox.getValue();
        int disasterId = getSelectedDisasterId();

        if (selectedAidName == null || selectedAidName.trim().isEmpty() || disasterId < 0) {
            beneficiaryCountLabel.setText("Beneficiaries: 0");
            printButton.setDisable(true);
            previewButton.setDisable(true);
            return;
        }

        List<AidModel> aidRecords = getFilteredAndSortedAidRecords(
                disasterId,
                selectedAidName
        );

        String contextInfo = getContextInfoText();
        String barangayInfo = getBarangayInfoText();
        beneficiaryCountLabel.setText("Beneficiaries: " + aidRecords.size() + contextInfo + barangayInfo);
        printButton.setDisable(aidRecords.isEmpty());
        previewButton.setDisable(aidRecords.isEmpty());
    }

    private String getContextInfoText() {
        boolean isGeneralAid = generalAidCheckBox != null && generalAidCheckBox.isSelected();
        return isGeneralAid ? " (General Aid)" : "";
    }

    private String getBarangayInfoText() {
        if (!isBarangayFilterActive()) {
            return "";
        }
        return " | Barangay: " + barangayComboBox.getValue();
    }

    private void handlePrint() {
        String selectedAidName = aidNameComboBox.getValue();
        int disasterId = getSelectedDisasterId();

        if (selectedAidName == null || selectedAidName.trim().isEmpty() || disasterId < 0) {
            AlertDialogManager.showWarning("Selection Required",
                    "Please select an Aid Name and either a Disaster or check General Aid option.");
            return;
        }

        List<AidModel> aidRecords = getFilteredAndSortedAidRecords(
                disasterId,
                selectedAidName
        );

        if (aidRecords.isEmpty()) {
            AlertDialogManager.showWarning("No Data",
                    "No beneficiaries found for the selected criteria.");
            return;
        }

        String reportTitle = buildReportTitle(selectedAidName);

        boolean success = printService.printSpecificReport(
                reportTitle,
                selectedAidName,
                aidRecords
        );

        if (success) {
            closeDialog();
        }
    }

    private void handlePreview() {
        String selectedAidName = aidNameComboBox.getValue();
        int disasterId = getSelectedDisasterId();

        if (selectedAidName == null || selectedAidName.trim().isEmpty() || disasterId < 0) {
            AlertDialogManager.showWarning("Selection Required",
                    "Please select an Aid Name and either a Disaster or check General Aid option.");
            return;
        }

        List<AidModel> aidRecords = getFilteredAndSortedAidRecords(
                disasterId,
                selectedAidName
        );

        if (aidRecords.isEmpty()) {
            AlertDialogManager.showWarning("No Data", "No beneficiaries found.");
            return;
        }

        StringBuilder preview = new StringBuilder();
        preview.append("═".repeat(60)).append("\n");
        preview.append("          AID DISTRIBUTION PREVIEW          \n");
        preview.append("═".repeat(60)).append("\n\n");

        boolean isGeneralAid = generalAidCheckBox != null && generalAidCheckBox.isSelected();
        if (isGeneralAid) {
            preview.append("Distribution Type: General Aid (No Disaster)\n");
        } else {
            DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
            preview.append("Disaster: ").append(selectedDisaster.getDisasterName()).append("\n");
        }

        preview.append("Aid Name: ").append(selectedAidName).append("\n");

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

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Distribution Preview");
        alert.setHeaderText(isGeneralAid ? "General Aid Distribution" : "Disaster Aid Distribution");

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

    private List<AidModel> getFilteredAndSortedAidRecords(int disasterId, String aidName) {
        List<AidModel> allAid = aidDAO.getAllAidForTable();

        List<AidModel> filtered = allAid.stream()
                .filter(aid -> {
                    boolean disasterMatch = (disasterId == 0 && aid.getDisasterId() == 0) ||
                            (disasterId > 0 && aid.getDisasterId() == disasterId);

                    boolean aidNameMatch = aid.getName() != null &&
                            aid.getName().equals(aidName);

                    return disasterMatch && aidNameMatch;
                })
                .collect(Collectors.toList());

        if (isBarangayFilterActive()) {
            String selectedBarangay = barangayComboBox.getValue();
            filtered = filterByBarangay(filtered, selectedBarangay);
        }

        // Sort by K-means score (highest priority first)
        filtered = sortByKMeansScore(filtered);

        return filtered;
    }

    private boolean isBarangayFilterActive() {
        return useBarangayFilterCheckBox != null &&
                useBarangayFilterCheckBox.isSelected() &&
                barangayComboBox != null &&
                barangayComboBox.getValue() != null &&
                !barangayComboBox.getValue().equals(ALL_BARANGAYS);
    }

    private List<AidModel> filterByBarangay(List<AidModel> aidRecords, String barangay) {
        Set<Integer> barangayBeneficiaryIds = getBeneficiaryIdsByBarangay(barangay);

        return aidRecords.stream()
                .filter(aid -> barangayBeneficiaryIds.contains(aid.getBeneficiaryId()))
                .collect(Collectors.toList());
    }

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

    private List<AidModel> sortByKMeansScore(List<AidModel> aidRecords) {
        return aidRecords.stream()
                .sorted((aid1, aid2) -> {
                    double score1 = extractScore(aid1.getNotes());
                    double score2 = extractScore(aid2.getNotes());
                    return Double.compare(score2, score1);
                })
                .collect(Collectors.toList());
    }

    private double extractScore(String notes) {
        if (notes == null || notes.isEmpty()) {
            return 0.0;
        }

        try {
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

    private String extractScoreInfo(String notes) {
        if (notes == null || notes.isEmpty()) {
            return "N/A";
        }

        try {
            Pattern scorePattern = Pattern.compile("Score:\\s*([0-9]+\\.?[0-9]*)");
            Matcher scoreMatcher = scorePattern.matcher(notes);

            Pattern clusterPattern = Pattern.compile("Cluster:\\s*([0-9]+)");
            Matcher clusterMatcher = clusterPattern.matcher(notes);

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

    private String buildReportTitle(String aidName) {
        boolean isGeneralAid = generalAidCheckBox != null && generalAidCheckBox.isSelected();

        StringBuilder title = new StringBuilder();

        if (isGeneralAid) {
            title.append("General Aid Distribution");
        } else {
            DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
            title.append(selectedDisaster.getDisasterName());
        }

        if (isBarangayFilterActive()) {
            title.append(" - Barangay ").append(barangayComboBox.getValue());
        }

        return title.toString();
    }

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

    private void loadAidNames() {
        String selectedAidName = aidNameComboBox.getValue();
        List<String> aidNames = aidDAO.getDistinctAidNames();
        aidNameComboBox.setItems(FXCollections.observableArrayList(aidNames));

        if (selectedAidName != null) {
            aidNameComboBox.setValue(selectedAidName);
        }
    }

    public void refreshComboBoxes() {
        loadDisasters();
        loadAidNames();
        loadBarangays();
        updateBeneficiaryCount();
    }
}


//package com.ionres.respondph.aid.dialogs_controller;
//
//import com.ionres.respondph.aid.AidDAO;
//import com.ionres.respondph.aid.AidDAOImpl;
//import com.ionres.respondph.aid.AidModel;
//import com.ionres.respondph.aid.AidPrintService;
//import com.ionres.respondph.database.DBConnection;
//import com.ionres.respondph.disaster.DisasterDAO;
//import com.ionres.respondph.disaster.DisasterDAOImpl;
//import com.ionres.respondph.disaster.DisasterModelComboBox;
//import com.ionres.respondph.util.AlertDialogManager;
//import com.ionres.respondph.util.Cryptography;
//import com.ionres.respondph.util.DashboardRefresher;
//import javafx.collections.FXCollections;
//import javafx.fxml.FXML;
//import javafx.scene.control.*;
//import javafx.stage.Stage;
//
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.util.*;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.util.stream.Collectors;
//
//public class PrintAidDialogController {
//
//    @FXML private ComboBox<DisasterModelComboBox> disasterComboBox;
//    @FXML private ComboBox<String> aidNameComboBox;
//    @FXML private ComboBox<String> barangayComboBox;
//    @FXML private CheckBox useBarangayFilterCheckBox;
//    @FXML private CheckBox generalAidCheckBox;
//    @FXML private Label beneficiaryCountLabel;
//    @FXML private Button printButton;
//    @FXML private Button cancelButton;
//    @FXML private Button previewButton;
//
//    private DisasterDAO disasterDAO;
//    private AidDAO aidDAO;
//    private AidPrintService printService;
//    private Cryptography cs;
//
//    private static final String ALL_BARANGAYS = "All Barangays";
//
//    @FXML
//    private void initialize() {
//        disasterDAO = new DisasterDAOImpl(DBConnection.getInstance());
//        aidDAO = new AidDAOImpl(DBConnection.getInstance());
//        printService = new AidPrintService();
//        cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");
//
//        DashboardRefresher.registerDisasterNameAndAidtypeName(this);
//
//        setupComboBoxes();
//        setupGeneralAidOption();
//        setupListeners();
//        setupButtons();
//        setupBarangayFilter();
//    }
//
//    private void setupComboBoxes() {
//        List<DisasterModelComboBox> disasters = disasterDAO.findAll();
//        disasterComboBox.setItems(FXCollections.observableArrayList(disasters));
//
//        // Get distinct aid names from aid table
//        List<String> aidNames = aidDAO.getDistinctAidNames();
//        aidNameComboBox.setItems(FXCollections.observableArrayList(aidNames));
//
//        disasterComboBox.setPromptText("Select Disaster");
//        aidNameComboBox.setPromptText("Select Aid Name");
//        if (barangayComboBox != null) {
//            barangayComboBox.setPromptText("Select Barangay");
//        }
//    }
//
//    private void setupGeneralAidOption() {
//        if (generalAidCheckBox != null) {
//            generalAidCheckBox.setSelected(false);
//
//            generalAidCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
//                if (newVal) {
//                    disasterComboBox.setValue(null);
//                    disasterComboBox.setDisable(true);
//                } else {
//                    disasterComboBox.setDisable(false);
//                }
//                loadBarangays();
//                updateBeneficiaryCount();
//            });
//        }
//    }
//
//    private void setupBarangayFilter() {
//        if (barangayComboBox != null && useBarangayFilterCheckBox != null) {
//            barangayComboBox.setDisable(true);
//            useBarangayFilterCheckBox.setSelected(false);
//
//            useBarangayFilterCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
//                barangayComboBox.setDisable(!newVal);
//                if (!newVal) {
//                    barangayComboBox.setValue(ALL_BARANGAYS);
//                }
//                updateBeneficiaryCount();
//            });
//        }
//    }
//
//    private void setupListeners() {
//        disasterComboBox.setOnAction(e -> {
//            if (disasterComboBox.getValue() != null && generalAidCheckBox != null) {
//                generalAidCheckBox.setSelected(false);
//            }
//            loadBarangays();
//            updateBeneficiaryCount();
//        });
//
//        aidNameComboBox.setOnAction(e -> updateBeneficiaryCount());
//
//        if (barangayComboBox != null) {
//            barangayComboBox.setOnAction(e -> updateBeneficiaryCount());
//        }
//    }
//
//    private void setupButtons() {
//        printButton.setOnAction(e -> handlePrint());
//        cancelButton.setOnAction(e -> handleCancel());
//        previewButton.setOnAction(e -> handlePreview());
//    }
//
//    private void loadBarangays() {
//        if (barangayComboBox == null) return;
//
//        int disasterId = getSelectedDisasterId();
//
//        List<String> barangays = aidDAO.getBarangaysByDisaster(disasterId);
//
//        barangays = barangays.stream().sorted().collect(Collectors.toList());
//        barangays.add(0, ALL_BARANGAYS);
//
//        barangayComboBox.setItems(FXCollections.observableArrayList(barangays));
//        barangayComboBox.setValue(ALL_BARANGAYS);
//
//        String context = disasterId > 0 ? "disaster #" + disasterId : "general aid";
//        System.out.println("Loaded " + (barangays.size() - 1) + " barangays for " + context);
//    }
//
//    private int getSelectedDisasterId() {
//        if (generalAidCheckBox != null && generalAidCheckBox.isSelected()) {
//            return 0; // General aid
//        }
//
//        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
//        return selectedDisaster != null ? selectedDisaster.getDisasterId() : -1;
//    }
//
//    private void updateBeneficiaryCount() {
//        String selectedAidName = aidNameComboBox.getValue();
//        int disasterId = getSelectedDisasterId();
//
//        if (selectedAidName == null || selectedAidName.trim().isEmpty() || disasterId < 0) {
//            beneficiaryCountLabel.setText("Beneficiaries: 0");
//            printButton.setDisable(true);
//            previewButton.setDisable(true);
//            return;
//        }
//
//        List<AidModel> aidRecords = getFilteredAndSortedAidRecords(
//                disasterId,
//                selectedAidName
//        );
//
//        String contextInfo = getContextInfoText();
//        String barangayInfo = getBarangayInfoText();
//        beneficiaryCountLabel.setText("Beneficiaries: " + aidRecords.size() + contextInfo + barangayInfo);
//        printButton.setDisable(aidRecords.isEmpty());
//        previewButton.setDisable(aidRecords.isEmpty());
//    }
//
//    private String getContextInfoText() {
//        boolean isGeneralAid = generalAidCheckBox != null && generalAidCheckBox.isSelected();
//        return isGeneralAid ? " (General Aid)" : "";
//    }
//
//    private String getBarangayInfoText() {
//        if (!isBarangayFilterActive()) {
//            return "";
//        }
//        return " | Barangay: " + barangayComboBox.getValue();
//    }
//
//    private void handlePrint() {
//        String selectedAidName = aidNameComboBox.getValue();
//        int disasterId = getSelectedDisasterId();
//
//        if (selectedAidName == null || selectedAidName.trim().isEmpty() || disasterId < 0) {
//            AlertDialogManager.showWarning("Selection Required",
//                    "Please select an Aid Name and either a Disaster or check General Aid option.");
//            return;
//        }
//
//        List<AidModel> aidRecords = getFilteredAndSortedAidRecords(
//                disasterId,
//                selectedAidName
//        );
//
//        if (aidRecords.isEmpty()) {
//            AlertDialogManager.showWarning("No Data",
//                    "No beneficiaries found for the selected criteria.");
//            return;
//        }
//
//        String reportTitle = buildReportTitle(selectedAidName);
//
//        boolean success = printService.printSpecificReport(
//                reportTitle,
//                selectedAidName,
//                aidRecords
//        );
//
//        if (success) {
//            closeDialog();
//        }
//    }
//
//    private void handlePreview() {
//        String selectedAidName = aidNameComboBox.getValue();
//        int disasterId = getSelectedDisasterId();
//
//        if (selectedAidName == null || selectedAidName.trim().isEmpty() || disasterId < 0) {
//            AlertDialogManager.showWarning("Selection Required",
//                    "Please select an Aid Name and either a Disaster or check General Aid option.");
//            return;
//        }
//
//        List<AidModel> aidRecords = getFilteredAndSortedAidRecords(
//                disasterId,
//                selectedAidName
//        );
//
//        if (aidRecords.isEmpty()) {
//            AlertDialogManager.showWarning("No Data", "No beneficiaries found.");
//            return;
//        }
//
//        // ── Build cluster-aware priority labels from scores ──────────────────
//        // Derive the same cluster groupings used at distribution time so the
//        // preview priority labels always match the cluster header labels.
//        Map<Integer, String> clusterPriorityMap = buildClusterPriorityMap(aidRecords);
//
//        StringBuilder preview = new StringBuilder();
//        preview.append("═".repeat(60)).append("\n");
//        preview.append("          AID DISTRIBUTION PREVIEW          \n");
//        preview.append("═".repeat(60)).append("\n\n");
//
//        boolean isGeneralAid = generalAidCheckBox != null && generalAidCheckBox.isSelected();
//        if (isGeneralAid) {
//            preview.append("Distribution Type: General Aid (No Disaster)\n");
//        } else {
//            DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
//            preview.append("Disaster: ").append(selectedDisaster.getDisasterName()).append("\n");
//        }
//
//        preview.append("Aid Name: ").append(selectedAidName).append("\n");
//
//        if (isBarangayFilterActive()) {
//            preview.append("Barangay: ").append(barangayComboBox.getValue()).append("\n");
//        } else {
//            preview.append("Barangay: All Barangays\n");
//        }
//
//        preview.append("Total Beneficiaries: ").append(aidRecords.size()).append("\n");
//        preview.append("Sorting: K-Means Priority (High to Low)\n");
//        preview.append("\n").append("─".repeat(60)).append("\n");
//        preview.append(String.format("%-4s %-35s %s\n", "No.", "Beneficiary Name", "Priority Score"));
//        preview.append("─".repeat(60)).append("\n");
//
//        int counter = 1;
//        String currentClusterLabel = null;
//
//        for (AidModel aid : aidRecords) {
//            int clusterNum = extractClusterNumber(aid.getNotes());
//            // ── Use the cluster-derived label, not the stale per-beneficiary label ──
//            String resolvedPriority = clusterPriorityMap.getOrDefault(clusterNum,
//                    extractRawPriority(aid.getNotes()));
//
//            // Print cluster header whenever we enter a new cluster group
//            if (!resolvedPriority.equals(currentClusterLabel)) {
//                currentClusterLabel = resolvedPriority;
//                double clusterAvg = getClusterAverage(aidRecords, clusterNum);
//                preview.append(String.format("\n--- %s (Cluster %d, Avg Score: %.3f) ---\n",
//                        resolvedPriority, clusterNum, clusterAvg));
//            }
//
//            double score = extractScore(aid.getNotes());
//            preview.append(String.format("%-4d %-35s %.3f (%s)\n",
//                    counter++,
//                    truncateName(aid.getBeneficiaryName(), 35),
//                    score,
//                    resolvedPriority   // ← now matches the cluster header
//            ));
//        }
//
//        preview.append("\n").append("─".repeat(60)).append("\n");
//        preview.append("\nNote: Beneficiaries are sorted by K-Means priority score.\n");
//        preview.append("Higher scores indicate higher priority for aid distribution.\n");
//
//        Alert alert = new Alert(Alert.AlertType.INFORMATION);
//        alert.setTitle("Distribution Preview");
//        alert.setHeaderText(isGeneralAid ? "General Aid Distribution" : "Disaster Aid Distribution");
//
//        TextArea textArea = new TextArea(preview.toString());
//        textArea.setEditable(false);
//        textArea.setWrapText(false);
//        textArea.setPrefRowCount(25);
//        textArea.setPrefColumnCount(65);
//        textArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px;");
//
//        alert.getDialogPane().setContent(textArea);
//        alert.getDialogPane().setPrefWidth(700);
//        alert.showAndWait();
//    }
//
//    /**
//     * Groups the aid records by their stored cluster number, ranks the groups by
//     * average score (descending), and returns a map of cluster# → priority label.
//     *
//     * This is the single source of truth for priority labels in the preview, so
//     * both the cluster headers AND the per-row labels are always consistent.
//     */
//    private Map<Integer, String> buildClusterPriorityMap(List<AidModel> aidRecords) {
//        // Accumulate score sums / counts per cluster
//        Map<Integer, Double> sumMap   = new LinkedHashMap<>();
//        Map<Integer, Integer> countMap = new LinkedHashMap<>();
//
//        for (AidModel aid : aidRecords) {
//            int cluster = extractClusterNumber(aid.getNotes());
//            double score = extractScore(aid.getNotes());
//            sumMap.merge(cluster, score, Double::sum);
//            countMap.merge(cluster, 1, Integer::sum);
//        }
//
//        // Sort cluster numbers by average score descending
//        List<Integer> sortedClusters = new ArrayList<>(sumMap.keySet());
//        sortedClusters.sort((a, b) -> {
//            double avgA = sumMap.get(a) / countMap.get(a);
//            double avgB = sumMap.get(b) / countMap.get(b);
//            return Double.compare(avgB, avgA);
//        });
//
//        // Assign labels: highest avg = High Priority, lowest avg = Low Priority, rest = Medium
//        Map<Integer, String> labelMap = new LinkedHashMap<>();
//        for (int rank = 0; rank < sortedClusters.size(); rank++) {
//            int clusterIdx = sortedClusters.get(rank);
//            if (rank == 0) {
//                labelMap.put(clusterIdx, "High Priority");
//            } else if (rank == sortedClusters.size() - 1 && sortedClusters.size() > 1) {
//                labelMap.put(clusterIdx, "Low Priority");
//            } else {
//                labelMap.put(clusterIdx, "Medium Priority");
//            }
//        }
//        return labelMap;
//    }
//
//    /** Computes the average score of all aid records belonging to the given cluster. */
//    private double getClusterAverage(List<AidModel> aidRecords, int clusterNum) {
//        return aidRecords.stream()
//                .filter(a -> extractClusterNumber(a.getNotes()) == clusterNum)
//                .mapToDouble(a -> extractScore(a.getNotes()))
//                .average()
//                .orElse(0.0);
//    }
//
//    private void handleCancel() {
//        closeDialog();
//    }
//
//    private List<AidModel> getFilteredAndSortedAidRecords(int disasterId, String aidName) {
//        List<AidModel> allAid = aidDAO.getAllAidForTable();
//
//        List<AidModel> filtered = allAid.stream()
//                .filter(aid -> {
//                    boolean disasterMatch = (disasterId == 0 && aid.getDisasterId() == 0) ||
//                            (disasterId > 0 && aid.getDisasterId() == disasterId);
//
//                    boolean aidNameMatch = aid.getName() != null &&
//                            aid.getName().equals(aidName);
//
//                    return disasterMatch && aidNameMatch;
//                })
//                .collect(Collectors.toList());
//
//        if (isBarangayFilterActive()) {
//            String selectedBarangay = barangayComboBox.getValue();
//            filtered = filterByBarangay(filtered, selectedBarangay);
//        }
//
//        // Sort by K-means score (highest priority first)
//        filtered = sortByKMeansScore(filtered);
//
//        return filtered;
//    }
//
//    private boolean isBarangayFilterActive() {
//        return useBarangayFilterCheckBox != null &&
//                useBarangayFilterCheckBox.isSelected() &&
//                barangayComboBox != null &&
//                barangayComboBox.getValue() != null &&
//                !barangayComboBox.getValue().equals(ALL_BARANGAYS);
//    }
//
//    private List<AidModel> filterByBarangay(List<AidModel> aidRecords, String barangay) {
//        Set<Integer> barangayBeneficiaryIds = getBeneficiaryIdsByBarangay(barangay);
//
//        return aidRecords.stream()
//                .filter(aid -> barangayBeneficiaryIds.contains(aid.getBeneficiaryId()))
//                .collect(Collectors.toList());
//    }
//
//    private Set<Integer> getBeneficiaryIdsByBarangay(String barangay) {
//        Set<Integer> beneficiaryIds = new HashSet<>();
//
//        String sql = "SELECT beneficiary_id, barangay FROM beneficiary WHERE barangay IS NOT NULL";
//
//        try {
//            Connection conn = DBConnection.getInstance().getConnection();
//            PreparedStatement ps = conn.prepareStatement(sql);
//            ResultSet rs = ps.executeQuery();
//
//            while (rs.next()) {
//                String encryptedBarangay = rs.getString("barangay");
//                try {
//                    String decryptedBarangay = cs.decryptWithOneParameter(encryptedBarangay);
//                    if (decryptedBarangay != null && decryptedBarangay.equals(barangay)) {
//                        beneficiaryIds.add(rs.getInt("beneficiary_id"));
//                    }
//                } catch (Exception e) {
//                    System.err.println("Error decrypting barangay: " + e.getMessage());
//                }
//            }
//
//            rs.close();
//            ps.close();
//            conn.close();
//
//        } catch (Exception e) {
//            System.err.println("Error fetching beneficiary IDs by barangay: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        return beneficiaryIds;
//    }
//
//    private List<AidModel> sortByKMeansScore(List<AidModel> aidRecords) {
//        return aidRecords.stream()
//                .sorted((aid1, aid2) -> {
//                    double score1 = extractScore(aid1.getNotes());
//                    double score2 = extractScore(aid2.getNotes());
//                    return Double.compare(score2, score1);
//                })
//                .collect(Collectors.toList());
//    }
//
//    private double extractScore(String notes) {
//        if (notes == null || notes.isEmpty()) {
//            return 0.0;
//        }
//
//        try {
//            Pattern pattern = Pattern.compile("Score:\\s*([0-9]+\\.?[0-9]*)");
//            Matcher matcher = pattern.matcher(notes);
//
//            if (matcher.find()) {
//                return Double.parseDouble(matcher.group(1));
//            }
//        } catch (Exception e) {
//            System.err.println("Error extracting score from notes: " + e.getMessage());
//        }
//
//        return 0.0;
//    }
//
//    /** Extracts the cluster number stored in the notes string. Returns -1 if not found. */
//    private int extractClusterNumber(String notes) {
//        if (notes == null || notes.isEmpty()) return -1;
//        try {
//            Pattern p = Pattern.compile("Cluster:\\s*(-?[0-9]+)");
//            Matcher m = p.matcher(notes);
//            if (m.find()) return Integer.parseInt(m.group(1));
//        } catch (Exception ignored) { }
//        return -1;
//    }
//
//    /**
//     * Fallback: reads the raw Priority field from notes.
//     * Only used when no cluster mapping is available.
//     */
//    private String extractRawPriority(String notes) {
//        if (notes == null || notes.isEmpty()) return "N/A";
//        try {
//            Pattern p = Pattern.compile("Priority:\\s*([A-Za-z]+)");
//            Matcher m = p.matcher(notes);
//            if (m.find()) return m.group(1) + " Priority";
//        } catch (Exception ignored) { }
//        return "N/A";
//    }
//
//    /** Used by extractScoreInfo for non-preview display (e.g. legacy callers). */
//    private String extractScoreInfo(String notes) {
//        if (notes == null || notes.isEmpty()) {
//            return "N/A";
//        }
//
//        try {
//            Pattern scorePattern = Pattern.compile("Score:\\s*([0-9]+\\.?[0-9]*)");
//            Matcher scoreMatcher = scorePattern.matcher(notes);
//
//            Pattern clusterPattern = Pattern.compile("Cluster:\\s*(-?[0-9]+)");
//            Matcher clusterMatcher = clusterPattern.matcher(notes);
//
//            String score   = scoreMatcher.find()  ? scoreMatcher.group(1)  : "N/A";
//            String cluster = clusterMatcher.find() ? clusterMatcher.group(1) : "N/A";
//
//            // NOTE: Priority label is intentionally omitted here because the
//            // correct label requires knowing the full cluster ranking context.
//            // Use buildClusterPriorityMap() + extractClusterNumber() in preview instead.
//            return String.format("%s (C%s)", score, cluster);
//        } catch (Exception e) {
//            return "N/A";
//        }
//    }
//
//    private String buildReportTitle(String aidName) {
//        boolean isGeneralAid = generalAidCheckBox != null && generalAidCheckBox.isSelected();
//
//        StringBuilder title = new StringBuilder();
//
//        if (isGeneralAid) {
//            title.append("General Aid Distribution");
//        } else {
//            DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
//            title.append(selectedDisaster.getDisasterName());
//        }
//
//        if (isBarangayFilterActive()) {
//            title.append(" - Barangay ").append(barangayComboBox.getValue());
//        }
//
//        return title.toString();
//    }
//
//    private String truncateName(String name, int maxLength) {
//        if (name == null) return "";
//        if (name.length() <= maxLength) return name;
//        return name.substring(0, maxLength - 3) + "...";
//    }
//
//    private void closeDialog() {
//        Stage stage = (Stage) cancelButton.getScene().getWindow();
//        stage.close();
//    }
//
//    // Methods for DashboardRefresher
//    private void loadDisasters() {
//        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
//        List<DisasterModelComboBox> disasters = disasterDAO.findAll();
//        disasterComboBox.setItems(FXCollections.observableArrayList(disasters));
//
//        if (selectedDisaster != null) {
//            disasterComboBox.getItems().stream()
//                    .filter(d -> d.getDisasterId() == selectedDisaster.getDisasterId())
//                    .findFirst()
//                    .ifPresent(disasterComboBox::setValue);
//        }
//    }
//
//    private void loadAidNames() {
//        String selectedAidName = aidNameComboBox.getValue();
//        List<String> aidNames = aidDAO.getDistinctAidNames();
//        aidNameComboBox.setItems(FXCollections.observableArrayList(aidNames));
//
//        if (selectedAidName != null) {
//            aidNameComboBox.setValue(selectedAidName);
//        }
//    }
//
//    public void refreshComboBoxes() {
//        loadDisasters();
//        loadAidNames();
//        loadBarangays();
//        updateBeneficiaryCount();
//    }
//}