package com.ionres.respondph.aid.dialogs_controller;

import com.ionres.respondph.aid.*;
import com.ionres.respondph.aid.KMeansAidDistribution.BeneficiaryCluster;
import com.ionres.respondph.aid_type.AidTypeModelComboBox;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.disaster.DisasterModelComboBox;
import com.ionres.respondph.util.AlertDialogManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.print.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AddAidController {

    @FXML private VBox root;
    @FXML private TextField nameFld;
    @FXML private TextField quantityFld;
    @FXML private TextField quantityPerBeneficiaryFld;
    @FXML private TextField costFld;
    @FXML private TextField providerFld;
    @FXML private CheckBox useKMeansCheckbox;
    @FXML private Button previewBtn;
    @FXML private Button printCustomBtn;
    @FXML private Button saveAidBtn;
    @FXML private Button cancelBtn;
    @FXML private Button closeBtn;
    @FXML private Label infoLabel;
    @FXML private HBox simpleDistributionWarning;
    @FXML private HBox selectionSummaryBox;
    @FXML private Label selectionSummaryLabel;
    @FXML private ComboBox<AidTypeModelComboBox> aidTypeComboBox;
    @FXML private ComboBox<DisasterModelComboBox> disasterComboBox;
    @FXML private CheckBox useBarangayFilterCheckbox;
    @FXML private ComboBox<String> barangayComboBox;
    @FXML private RadioButton singleBarangayRadio;
    @FXML private RadioButton allBarangaysRadio;
    @FXML private VBox barangaySelectionContainer;
    @FXML private VBox singleBarangayContainer;
    @FXML private CheckBox generalAidCheckbox;
    @FXML private VBox disasterSelectionContainer;

    private AidService aidService;
    private AidDAO aidDAO;
    private AidController aidController;
    private Stage dialogStage;
    private ToggleGroup barangayModeGroup;

    private static final int FIXED_CLUSTERS = 3;

    public void setAidService(AidService aidService) {
        this.aidService = aidService;
    }

    public void setAidController(AidController aidController) {
        this.aidController = aidController;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setAidTypes(List<AidTypeModelComboBox> aidTypes) {
        aidTypeComboBox.setItems(FXCollections.observableArrayList(aidTypes));
    }

    public void setDisasters(List<DisasterModelComboBox> disasters) {
        disasterComboBox.setItems(FXCollections.observableArrayList(disasters));
    }

    public void setSelectedAidTypeAndDisaster(AidTypeModelComboBox aidType, DisasterModelComboBox disaster) {
        if (aidType != null) {
            aidTypeComboBox.setValue(aidType);
        }
        if (disaster != null) {
            disasterComboBox.setValue(disaster);
            generalAidCheckbox.setSelected(false);
        }
        updateSelectionSummary();
        loadBarangays();
    }

    @FXML
    private void initialize() {
        if (aidService == null) {
            aidService = new AidServiceImpl();
        }

        aidDAO = new AidDAOImpl(DBConnection.getInstance());

        setupGeneralAidOption();
        setupBarangayMode();
        setupEventHandlers();
        setupDefaultValues();
        setupComboBoxListeners();
        makeDraggable();
    }

    private void setupGeneralAidOption() {
        if (generalAidCheckbox != null) {
            generalAidCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    disasterComboBox.setValue(null);
                    disasterComboBox.setDisable(true);
                } else {
                    disasterComboBox.setDisable(false);
                }
                updateSelectionSummary();
                loadBarangays();
            });
        }
    }

    private void setupBarangayMode() {
        barangayModeGroup = new ToggleGroup();
        singleBarangayRadio.setToggleGroup(barangayModeGroup);
        allBarangaysRadio.setToggleGroup(barangayModeGroup);

        allBarangaysRadio.setSelected(true);

        barangayModeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == singleBarangayRadio) {
                singleBarangayContainer.setVisible(true);
                singleBarangayContainer.setManaged(true);
            } else {
                singleBarangayContainer.setVisible(false);
                singleBarangayContainer.setManaged(false);
            }
            updateSelectionSummary();
        });

        useBarangayFilterCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            barangaySelectionContainer.setVisible(newVal);
            barangaySelectionContainer.setManaged(newVal);
            updateSelectionSummary();
        });

        barangaySelectionContainer.setVisible(false);
        barangaySelectionContainer.setManaged(false);
    }

    private void setupDefaultValues() {
        if (useKMeansCheckbox != null) {
            useKMeansCheckbox.setSelected(true);
        }
        if (useBarangayFilterCheckbox != null) {
            useBarangayFilterCheckbox.setSelected(false);
        }
        if (generalAidCheckbox != null) {
            generalAidCheckbox.setSelected(false);
        }
    }

    private void setupComboBoxListeners() {
        aidTypeComboBox.setOnAction(e -> updateSelectionSummary());
        disasterComboBox.setOnAction(e -> {
            if (disasterComboBox.getValue() != null) {
                generalAidCheckbox.setSelected(false);
            }
            updateSelectionSummary();
            loadBarangays();
        });
        barangayComboBox.setOnAction(e -> updateSelectionSummary());
    }

    private void loadBarangays() {
        int disasterId = getSelectedDisasterId();

        List<String> barangays = aidDAO.getBarangaysByDisaster(disasterId);

        barangays = barangays.stream()
                .sorted()
                .collect(Collectors.toList());

        barangayComboBox.setItems(FXCollections.observableArrayList(barangays));
        if (!barangays.isEmpty()) {
            barangayComboBox.getSelectionModel().selectFirst();
        }
    }

    private int getSelectedDisasterId() {
        if (generalAidCheckbox != null && generalAidCheckbox.isSelected()) {
            return 0;
        }

        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
        return selectedDisaster != null ? selectedDisaster.getDisasterId() : 0;
    }

    private void updateSelectionSummary() {
        AidTypeModelComboBox selectedAidType = aidTypeComboBox.getValue();
        boolean isGeneralAid = generalAidCheckbox != null && generalAidCheckbox.isSelected();
        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();

        if (selectedAidType != null && (isGeneralAid || selectedDisaster != null)) {
            selectionSummaryBox.setVisible(true);
            selectionSummaryBox.setManaged(true);

            int disasterId = getSelectedDisasterId();
            int eligibleCount = getEligibleBeneficiaryCount(
                    selectedAidType.getAidTypeId(),
                    disasterId
            );

            String barangayInfo = getBarangayInfoText();
            String disasterInfo = isGeneralAid
                    ? "General Aid (No Disaster)"
                    : "Disaster: " + selectedDisaster.getDisasterName();

            selectionSummaryLabel.setText(String.format(
                    "Aid Type: %s | %s%s | Eligible Beneficiaries: %d",
                    selectedAidType.getAidName(),
                    disasterInfo,
                    barangayInfo,
                    eligibleCount
            ));

            infoLabel.setText(String.format(
                    "Distributing %s%s%s",
                    selectedAidType.getAidName(),
                    isGeneralAid ? " (General Aid)" : " for " + selectedDisaster.getDisasterName() + " disaster",
                    barangayInfo
            ));
        } else {
            selectionSummaryBox.setVisible(false);
            selectionSummaryBox.setManaged(false);
            infoLabel.setText("Select aid type and choose disaster or general aid option");
        }
    }

    private String getBarangayInfoText() {
        if (!useBarangayFilterCheckbox.isSelected()) {
            return "";
        }

        if (allBarangaysRadio.isSelected()) {
            return " | All Barangays";
        } else if (singleBarangayRadio.isSelected()) {
            String barangay = barangayComboBox.getValue();
            return barangay != null ? " | Barangay: " + barangay : "";
        }
        return "";
    }

    private int getEligibleBeneficiaryCount(int aidTypeId, int disasterId) {
        try {
            int qtyPerBeneficiary = 1;
            try {
                String qtyText = quantityPerBeneficiaryFld.getText().trim();
                if (!qtyText.isEmpty()) {
                    qtyPerBeneficiary = Integer.parseInt(qtyText);
                    if (qtyPerBeneficiary <= 0) {
                        qtyPerBeneficiary = 1;
                    }
                }
            } catch (NumberFormatException e) {
                qtyPerBeneficiary = 1;
            }

            List<BeneficiaryCluster> eligible;

            if (!useBarangayFilterCheckbox.isSelected() || allBarangaysRadio.isSelected()) {
                eligible = aidService.previewAidDistribution(
                        aidTypeId, disasterId, Integer.MAX_VALUE, qtyPerBeneficiary, FIXED_CLUSTERS
                );
            } else {
                String barangay = barangayComboBox.getValue();
                if (barangay != null) {
                    eligible = aidService.previewAidDistributionByBarangay(
                            aidTypeId, disasterId, Integer.MAX_VALUE, qtyPerBeneficiary, FIXED_CLUSTERS, barangay
                    );
                } else {
                    eligible = new ArrayList<>();
                }
            }

            return eligible.size();
        } catch (Exception e) {
            System.err.println("Error getting eligible count: " + e.getMessage());
            return 0;
        }
    }

    private void setupEventHandlers() {
        saveAidBtn.setOnAction(this::handleSave);
        closeBtn.setOnAction(this::handleClose);

        if (cancelBtn != null) {
            cancelBtn.setOnAction(this::handleClose);
        }

        if (previewBtn != null) {
            previewBtn.setOnAction(this::handlePreview);
        }

        if (printCustomBtn != null) {
            printCustomBtn.setOnAction(this::handlePrintCustom);
        }

        if (useKMeansCheckbox != null) {
            useKMeansCheckbox.setOnAction(e -> {
                boolean useKMeans = useKMeansCheckbox.isSelected();
                if (simpleDistributionWarning != null) {
                    simpleDistributionWarning.setVisible(!useKMeans);
                    simpleDistributionWarning.setManaged(!useKMeans);
                }
            });
        }
    }

    @FXML
    private void handlePreview(ActionEvent event) {
        if (!validateSelection()) return;
        if (!validateInput()) return;

        try {
            int aidTypeId = aidTypeComboBox.getValue().getAidTypeId();
            int disasterId = getSelectedDisasterId();
            int quantity = Integer.parseInt(quantityFld.getText().trim());
            int quantityPerBeneficiary = Integer.parseInt(quantityPerBeneficiaryFld.getText().trim());
            boolean useKMeans = useKMeansCheckbox != null && useKMeansCheckbox.isSelected();

            List<BeneficiaryCluster> preview;

            if (!useBarangayFilterCheckbox.isSelected() || allBarangaysRadio.isSelected()) {
                preview = aidService.previewAidDistribution(
                        aidTypeId, disasterId, quantity, quantityPerBeneficiary, FIXED_CLUSTERS
                );
            } else {
                String barangay = barangayComboBox.getValue();
                if (barangay == null) {
                    AlertDialogManager.showWarning("Selection Required", "Please select a barangay.");
                    return;
                }
                preview = aidService.previewAidDistributionByBarangay(
                        aidTypeId, disasterId, quantity, quantityPerBeneficiary, FIXED_CLUSTERS, barangay
                );
            }

            if (preview.isEmpty()) {
                AlertDialogManager.showWarning("No Eligible Beneficiaries",
                        "No beneficiaries are currently eligible for this aid distribution.");
                return;
            }

            showPreviewDialog(preview, useKMeans, quantityPerBeneficiary);

        } catch (NumberFormatException e) {
            AlertDialogManager.showError("Invalid Input", "Please enter valid quantities.");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Preview Error",
                    "Failed to generate distribution preview:\n" + e.getMessage());
        }
    }

    @FXML
    private void handlePrintCustom(ActionEvent event) {
        if (!validateSelection()) return;
        if (!validateInput()) return;

        try {
            int aidTypeId = aidTypeComboBox.getValue().getAidTypeId();
            int disasterId = getSelectedDisasterId();
            int quantity = Integer.parseInt(quantityFld.getText().trim());
            int quantityPerBeneficiary = Integer.parseInt(quantityPerBeneficiaryFld.getText().trim());
            boolean useKMeans = useKMeansCheckbox != null && useKMeansCheckbox.isSelected();

            List<BeneficiaryCluster> preview;

            if (!useBarangayFilterCheckbox.isSelected() || allBarangaysRadio.isSelected()) {
                preview = aidService.previewAidDistribution(
                        aidTypeId, disasterId, quantity, quantityPerBeneficiary, FIXED_CLUSTERS
                );
            } else {
                String barangay = barangayComboBox.getValue();
                if (barangay == null) {
                    AlertDialogManager.showWarning("Selection Required", "Please select a barangay.");
                    return;
                }
                preview = aidService.previewAidDistributionByBarangay(
                        aidTypeId, disasterId, quantity, quantityPerBeneficiary, FIXED_CLUSTERS, barangay
                );
            }

            if (preview.isEmpty()) {
                AlertDialogManager.showWarning("No Eligible Beneficiaries",
                        "No beneficiaries are currently eligible for this aid distribution.");
                return;
            }

            showPrintCustomDialog(preview, useKMeans, quantityPerBeneficiary);

        } catch (NumberFormatException e) {
            AlertDialogManager.showError("Invalid Input", "Please enter valid quantities.");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Print Error",
                    "Failed to generate print preview:\n" + e.getMessage());
        }
    }

    /**
     * Builds a cluster-rank-based priority map from the given beneficiary list.
     * rank 0 (highest avg score) → "High Priority"
     * rank 1                     → "Medium Priority"
     * rank 2 (lowest avg score)  → "Low Priority"
     *
     * This is the single source of truth so cluster headers and per-row labels
     * are always the same.
     */
    private Map<Integer, String> buildClusterPriorityMap(List<BeneficiaryCluster> beneficiaries) {
        Map<Integer, Double> sumMap   = new HashMap<>();
        Map<Integer, Integer> countMap = new HashMap<>();

        for (BeneficiaryCluster b : beneficiaries) {
            int cluster = b.getCluster();
            sumMap.put(cluster, sumMap.getOrDefault(cluster, 0.0) + b.getFinalScore());
            countMap.put(cluster, countMap.getOrDefault(cluster, 0) + 1);
        }

        // Average score per cluster
        for (Integer cluster : sumMap.keySet()) {
            sumMap.put(cluster, sumMap.get(cluster) / countMap.get(cluster));
        }

        // Sort by average score descending
        List<Map.Entry<Integer, Double>> sorted = new ArrayList<>(sumMap.entrySet());
        sorted.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        // Assign rank-based labels
        String[] labels = {"High Priority", "Medium Priority", "Low Priority"};
        Map<Integer, String> labelMap = new HashMap<>();
        for (int i = 0; i < sorted.size() && i < labels.length; i++) {
            labelMap.put(sorted.get(i).getKey(), labels[i]);
        }
        return labelMap;
    }

    // ── FIX 1: groupBeneficiariesByPriority ──────────────────────────────────
    // OLD: used b.getScoreCategory() → stale SAW-calculated label
    // NEW: uses buildClusterPriorityMap() → correct K-Means cluster rank label
    private Map<String, List<BeneficiaryCluster>> groupBeneficiariesByPriority(
            List<BeneficiaryCluster> beneficiaries) {

        Map<String, List<BeneficiaryCluster>> groups = new HashMap<>();
        groups.put("High Priority",   new ArrayList<>());
        groups.put("Medium Priority", new ArrayList<>());
        groups.put("Low Priority",    new ArrayList<>());

        // Build the correct cluster→label mapping from the actual data
        Map<Integer, String> clusterPriorityMap = buildClusterPriorityMap(beneficiaries);

        for (BeneficiaryCluster b : beneficiaries) {
            // Use cluster rank label instead of the original SAW scoreCategory
            String label = clusterPriorityMap.getOrDefault(b.getCluster(), "Low Priority");
            groups.get(label).add(b);
        }

        return groups;
    }

    private void showPrintCustomDialog(List<BeneficiaryCluster> preview,
                                       boolean usedKMeans, int qtyPerBeneficiary) {
        Map<String, List<BeneficiaryCluster>> priorityGroups = groupBeneficiariesByPriority(preview);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Print Custom - Select Priority Levels");
        dialog.setHeaderText("Select which priority levels to print:");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        CheckBox highPriorityCheck = new CheckBox("High Priority (" +
                priorityGroups.getOrDefault("High Priority", new ArrayList<>()).size() + " beneficiaries)");
        highPriorityCheck.setSelected(true);

        CheckBox mediumPriorityCheck = new CheckBox("Medium Priority (" +
                priorityGroups.getOrDefault("Medium Priority", new ArrayList<>()).size() + " beneficiaries)");
        mediumPriorityCheck.setSelected(true);

        CheckBox lowPriorityCheck = new CheckBox("Low Priority (" +
                priorityGroups.getOrDefault("Low Priority", new ArrayList<>()).size() + " beneficiaries)");
        lowPriorityCheck.setSelected(true);

        content.getChildren().addAll(
                new Label("Select priority levels to include in the print:"),
                highPriorityCheck,
                mediumPriorityCheck,
                lowPriorityCheck
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                List<BeneficiaryCluster> selectedBeneficiaries = new ArrayList<>();

                if (highPriorityCheck.isSelected()) {
                    selectedBeneficiaries.addAll(priorityGroups.getOrDefault("High Priority", new ArrayList<>()));
                }
                if (mediumPriorityCheck.isSelected()) {
                    selectedBeneficiaries.addAll(priorityGroups.getOrDefault("Medium Priority", new ArrayList<>()));
                }
                if (lowPriorityCheck.isSelected()) {
                    selectedBeneficiaries.addAll(priorityGroups.getOrDefault("Low Priority", new ArrayList<>()));
                }

                if (selectedBeneficiaries.isEmpty()) {
                    AlertDialogManager.showWarning("No Selection",
                            "Please select at least one priority level to print.");
                    return;
                }

                printBeneficiaryList(selectedBeneficiaries, usedKMeans, qtyPerBeneficiary);
            }
        });
    }

    private void printBeneficiaryList(List<BeneficiaryCluster> beneficiaries,
                                      boolean usedKMeans, int qtyPerBeneficiary) {
        try {
            PrinterJob printerJob = PrinterJob.createPrinterJob();

            if (printerJob == null) {
                AlertDialogManager.showError("Print Error", "No printer available.");
                return;
            }

            boolean proceed = printerJob.showPrintDialog(dialogStage);
            if (!proceed) return;

            VBox printContent = createPrintContent(beneficiaries, usedKMeans, qtyPerBeneficiary);
            boolean success = printerJob.printPage(printContent);

            if (success) {
                printerJob.endJob();
                AlertDialogManager.showInfo("Print Success", "Document sent to printer successfully.");
            } else {
                AlertDialogManager.showError("Print Error", "Failed to print document.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Print Error",
                    "An error occurred during printing:\n" + e.getMessage());
        }
    }

    private VBox createPrintContent(List<BeneficiaryCluster> beneficiaries,
                                    boolean usedKMeans, int qtyPerBeneficiary) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        Label title = new Label("Aid Distribution Beneficiary List");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setAlignment(Pos.CENTER);

        Label scopeLabel  = new Label("Distribution Scope: " + getDistributionScopeText());
        scopeLabel.setFont(Font.font("Arial", 12));

        Label methodLabel = new Label("Distribution Method: " +
                (usedKMeans ? "K-Means Clustering (3 Clusters)" : "Score-Based"));
        methodLabel.setFont(Font.font("Arial", 12));

        Label countLabel = new Label("Total Recipients: " + beneficiaries.size() + " beneficiaries");
        countLabel.setFont(Font.font("Arial", 12));

        Label qtyLabel = new Label("Quantity per Beneficiary: " + qtyPerBeneficiary + " units");
        qtyLabel.setFont(Font.font("Arial", 12));

        content.getChildren().addAll(title, new Label(""),
                scopeLabel, methodLabel, countLabel, qtyLabel, new Label(""));

        Map<String, List<BeneficiaryCluster>> priorityGroups = groupBeneficiariesByPriority(beneficiaries);

        if (!priorityGroups.get("High Priority").isEmpty()) {
            addPrioritySection(content, "High Priority",
                    priorityGroups.get("High Priority"), qtyPerBeneficiary);
        }
        if (!priorityGroups.get("Medium Priority").isEmpty()) {
            addPrioritySection(content, "Medium Priority",
                    priorityGroups.get("Medium Priority"), qtyPerBeneficiary);
        }
        if (!priorityGroups.get("Low Priority").isEmpty()) {
            addPrioritySection(content, "Low Priority",
                    priorityGroups.get("Low Priority"), qtyPerBeneficiary);
        }

        return content;
    }

    private void addPrioritySection(VBox content, String priorityName,
                                    List<BeneficiaryCluster> beneficiaries, int qtyPerBeneficiary) {
        Label sectionHeader = new Label(priorityName + " (" + beneficiaries.size() + " beneficiaries)");
        sectionHeader.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        sectionHeader.setStyle("-fx-text-fill: #2c3e50;");
        content.getChildren().add(sectionHeader);

        beneficiaries.sort((b1, b2) -> Double.compare(b2.getFinalScore(), b1.getFinalScore()));

        int count = 1;
        for (BeneficiaryCluster b : beneficiaries) {
            Label beneficiaryLabel = new Label(String.format(
                    "%d. Beneficiary #%d | Score: %.3f | Receives: %d units",
                    count++, b.getBeneficiaryId(), b.getFinalScore(), qtyPerBeneficiary
            ));
            beneficiaryLabel.setFont(Font.font("Arial", 11));
            content.getChildren().add(beneficiaryLabel);
        }

        content.getChildren().add(new Label(""));
    }

    private void showPreviewDialog(List<BeneficiaryCluster> preview,
                                   boolean usedKMeans, int qtyPerBeneficiary) {
        StringBuilder message = new StringBuilder();

        message.append(String.format("Distribution Scope: %s\n", getDistributionScopeText()));
        message.append(String.format("Distribution Method: %s\n",
                usedKMeans ? "K-Means Clustering (3 Clusters)" : "Score-Based"));
        message.append(String.format("Total Recipients: %d beneficiaries\n", preview.size()));
        message.append(String.format("Quantity per Beneficiary: %d units\n", qtyPerBeneficiary));
        message.append(String.format("Total Units to Distribute: %d units\n\n",
                preview.size() * qtyPerBeneficiary));

        if (usedKMeans) {
            message.append("=== Distribution by Cluster (3 Clusters) ===\n\n");

            // ── FIX 2: build priority map by cluster RANK (highest avg = High) ──
            Map<Integer, String> clusterToPriority = buildClusterPriorityMap(preview);

            // Compute averages again for display in the header line
            Map<Integer, Double> clusterSums   = new HashMap<>();
            Map<Integer, Integer> clusterCounts = new HashMap<>();
            for (BeneficiaryCluster b : preview) {
                int c = b.getCluster();
                clusterSums.put(c, clusterSums.getOrDefault(c, 0.0) + b.getFinalScore());
                clusterCounts.put(c, clusterCounts.getOrDefault(c, 0) + 1);
            }
            Map<Integer, Double> clusterAvg = new HashMap<>();
            for (Integer c : clusterSums.keySet()) {
                clusterAvg.put(c, clusterSums.get(c) / clusterCounts.get(c));
            }

            // Sort clusters by average descending (High → Medium → Low)
            List<Integer> sortedClusterKeys = new ArrayList<>(clusterAvg.keySet());
            sortedClusterKeys.sort((a, b) -> Double.compare(clusterAvg.get(b), clusterAvg.get(a)));

            // Group beneficiaries by cluster
            Map<Integer, List<BeneficiaryCluster>> byCluster = new HashMap<>();
            for (BeneficiaryCluster b : preview) {
                byCluster.computeIfAbsent(b.getCluster(), k -> new ArrayList<>()).add(b);
            }

            int overallCount = 1;
            for (Integer clusterNum : sortedClusterKeys) {
                // priorityName comes from the rank map → always matches header
                String priorityName = clusterToPriority.get(clusterNum);
                List<BeneficiaryCluster> clusterBeneficiaries = byCluster.get(clusterNum);

                if (clusterBeneficiaries != null && !clusterBeneficiaries.isEmpty()) {
                    message.append(String.format("--- %s (Cluster %d, Avg Score: %.3f) ---\n",
                            priorityName, clusterNum, clusterAvg.get(clusterNum)));

                    clusterBeneficiaries.sort((b1, b2) ->
                            Double.compare(b2.getFinalScore(), b1.getFinalScore()));

                    for (BeneficiaryCluster b : clusterBeneficiaries) {
                        message.append(String.format(
                                "%d. Beneficiary #%d | Score: %.3f | %s | Receives: %d units\n",
                                overallCount++,
                                b.getBeneficiaryId(),
                                b.getFinalScore(),
                                // ── FIX 3: priorityName from rank map, NOT b.getScoreCategory() ──
                                priorityName,
                                qtyPerBeneficiary
                        ));
                    }
                    message.append("\n");
                }
            }
        } else {
            message.append("=== Top Beneficiaries by Score ===\n\n");
            for (int i = 0; i < preview.size(); i++) {
                BeneficiaryCluster b = preview.get(i);
                // Non-K-Means: scoreCategory is fine, no cluster ranking involved
                message.append(String.format(
                        "%d. Beneficiary #%d | Score: %.3f | %s | Receives: %d units\n",
                        i + 1, b.getBeneficiaryId(), b.getFinalScore(),
                        b.getScoreCategory(), qtyPerBeneficiary
                ));
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Distribution Preview");
        alert.setHeaderText("Aid Distribution Plan");

        TextArea textArea = new TextArea(message.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        textArea.setPrefRowCount(20);
        textArea.setPrefColumnCount(50);

        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefWidth(600);
        alert.showAndWait();
    }

    private String getDistributionScopeText() {
        boolean isGeneralAid = generalAidCheckbox != null && generalAidCheckbox.isSelected();
        String disasterContext = isGeneralAid
                ? "General Aid (No Disaster)"
                : "Disaster: " + disasterComboBox.getValue().getDisasterName();

        String barangayContext;
        if (!useBarangayFilterCheckbox.isSelected() || allBarangaysRadio.isSelected()) {
            barangayContext = "All Barangays";
        } else if (singleBarangayRadio.isSelected()) {
            String barangay = barangayComboBox.getValue();
            barangayContext = barangay != null ? "Barangay: " + barangay : "";
        } else {
            barangayContext = "";
        }

        return disasterContext + " | " + barangayContext;
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (!validateSelection()) return;
        if (!validateInput()) return;
        if (!showConfirmationDialog()) return;

        try {
            String aidName            = nameFld.getText().trim();
            int aidTypeId             = aidTypeComboBox.getValue().getAidTypeId();
            int disasterId            = getSelectedDisasterId();
            int quantity              = Integer.parseInt(quantityFld.getText().trim());
            int quantityPerBeneficiary = Integer.parseInt(quantityPerBeneficiaryFld.getText().trim());
            double costPerUnit        = Double.parseDouble(costFld.getText().trim());
            String provider           = providerFld.getText().trim();
            boolean useKMeans         = useKMeansCheckbox != null && useKMeansCheckbox.isSelected();

            int distributedCount;

            if (useKMeans) {
                if (!useBarangayFilterCheckbox.isSelected() || allBarangaysRadio.isSelected()) {
                    distributedCount = aidService.distributeAidWithKMeans(
                            aidName, aidTypeId, disasterId, quantity, quantityPerBeneficiary,
                            costPerUnit, provider, FIXED_CLUSTERS
                    );
                } else {
                    String barangay = barangayComboBox.getValue();
                    distributedCount = aidService.distributeAidWithKMeansByBarangay(
                            aidName, aidTypeId, disasterId, quantity, quantityPerBeneficiary,
                            costPerUnit, provider, FIXED_CLUSTERS, barangay
                    );
                }
            } else {
                distributedCount = aidService.distributeAidSimple(
                        aidName, aidTypeId, disasterId, quantity, quantityPerBeneficiary,
                        costPerUnit, provider
                );
            }

            if (distributedCount > 0) {
                showSuccessDialog(aidName, distributedCount, quantityPerBeneficiary,
                        costPerUnit, useKMeans);
                if (aidController != null) aidController.loadAidData();
                clearFields();
            } else {
                showNoDistributionWarning();
            }

        } catch (NumberFormatException e) {
            AlertDialogManager.showError("Invalid Input",
                    "Please check that quantity and cost are valid numbers.");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Distribution Error",
                    "An error occurred during aid distribution:\n" + e.getMessage());
        }
    }

    private boolean validateSelection() {
        if (aidTypeComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Selection Required",
                    "Please select an Aid Type before distributing aid.");
            aidTypeComboBox.requestFocus();
            return false;
        }

        boolean isGeneralAid = generalAidCheckbox != null && generalAidCheckbox.isSelected();
        if (!isGeneralAid && disasterComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Selection Required",
                    "Please either select a Disaster Event or check the 'General Aid' option.");
            return false;
        }

        if (useBarangayFilterCheckbox.isSelected()) {
            if (singleBarangayRadio.isSelected() && barangayComboBox.getValue() == null) {
                AlertDialogManager.showWarning("Selection Required", "Please select a barangay.");
                return false;
            }
        }

        return true;
    }

    private boolean showConfirmationDialog() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Distribution");
        confirmAlert.setHeaderText("Distribute Aid to Beneficiaries");

        int totalQuantity  = Integer.parseInt(quantityFld.getText().trim());
        double costPerUnit = Double.parseDouble(costFld.getText().trim());
        double totalCost   = totalQuantity * costPerUnit;

        confirmAlert.setContentText(String.format(
                "Are you sure you want to distribute this aid?\n\n" +
                        "Aid: %s\n" +
                        "Aid Type: %s\n" +
                        "Scope: %s\n" +
                        "Quantity: %s units\n" +
                        "Cost per unit: ₱%s\n" +
                        "Total cost: ₱%.2f\n" +
                        "Provider: %s\n" +
                        "Method: %s (3 Clusters)",
                nameFld.getText().trim(),
                aidTypeComboBox.getValue().getAidName(),
                getDistributionScopeText(),
                quantityFld.getText().trim(),
                costFld.getText().trim(),
                totalCost,
                providerFld.getText().trim(),
                (useKMeansCheckbox != null && useKMeansCheckbox.isSelected()) ?
                        "K-means Clustering" : "Score-based"
        ));

        return confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showSuccessDialog(String aidName, int count, int qtyPerBeneficiary,
                                   double costPerUnit, boolean usedKMeans) {
        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
        successAlert.setTitle("Distribution Successful");
        successAlert.setHeaderText("✓ Aid Distribution Complete");

        successAlert.setContentText(String.format(
                "Successfully distributed aid!\n\n" +
                        "Aid Type: %s\n" +
                        "Scope: %s\n" +
                        "Beneficiaries Served: %d\n" +
                        "Total Quantity: %d units\n" +
                        "Total Cost: ₱%.2f\n" +
                        "Distribution Method: %s",
                aidName,
                getDistributionScopeText(),
                count,
                count * qtyPerBeneficiary,
                (double) count * qtyPerBeneficiary * costPerUnit,
                usedKMeans ? "K-means Clustering (3 Clusters)" : "Score-based"
        ));
        successAlert.showAndWait();
    }

    private void showNoDistributionWarning() {
        AlertDialogManager.showWarning("No Distribution",
                "No beneficiaries were eligible for this aid distribution.\n\n" +
                        "Please check:\n" +
                        "• Beneficiaries have been scored for this aid type\n" +
                        "• Beneficiaries haven't already received this aid\n" +
                        "• Selected barangay has eligible beneficiaries");
    }

    @FXML
    private void handleClose(ActionEvent event) {
        closeDialog();
    }

    private boolean validateInput() {
        if (nameFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Aid name is required.");
            nameFld.requestFocus();
            return false;
        }

        if (quantityFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Quantity is required.");
            quantityFld.requestFocus();
            return false;
        }

        try {
            int quantity = Integer.parseInt(quantityFld.getText().trim());
            if (quantity <= 0) {
                AlertDialogManager.showWarning("Validation Error",
                        "Quantity must be greater than zero.");
                quantityFld.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            AlertDialogManager.showWarning("Validation Error",
                    "Please enter a valid quantity (whole number).");
            quantityFld.requestFocus();
            return false;
        }

        if (costFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Cost is required.");
            costFld.requestFocus();
            return false;
        }

        try {
            double cost = Double.parseDouble(costFld.getText().trim());
            if (cost < 0) {
                AlertDialogManager.showWarning("Validation Error", "Cost cannot be negative.");
                costFld.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            AlertDialogManager.showWarning("Validation Error",
                    "Please enter a valid cost (number).");
            costFld.requestFocus();
            return false;
        }

        if (providerFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Provider is required.");
            providerFld.requestFocus();
            return false;
        }

        return true;
    }

    private void clearFields() {
        nameFld.clear();
        quantityFld.clear();
        quantityPerBeneficiaryFld.clear();
        costFld.clear();
        providerFld.clear();
        aidTypeComboBox.setValue(null);
        disasterComboBox.setValue(null);
        barangayComboBox.setValue(null);

        if (useKMeansCheckbox != null)        useKMeansCheckbox.setSelected(true);
        if (useBarangayFilterCheckbox != null) useBarangayFilterCheckbox.setSelected(false);
        if (generalAidCheckbox != null)        generalAidCheckbox.setSelected(false);

        allBarangaysRadio.setSelected(true);
        updateSelectionSummary();
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        } else {
            Stage stage = (Stage) closeBtn.getScene().getWindow();
            stage.close();
        }
    }

    private void makeDraggable() {
        final double[] xOffset = {0};
        final double[] yOffset = {0};

        root.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            if(dialogStage != null) {
                dialogStage.setX(event.getScreenX() - xOffset[0]);
                dialogStage.setY(event.getScreenY() - yOffset[0]);
            }
        });
    }
}