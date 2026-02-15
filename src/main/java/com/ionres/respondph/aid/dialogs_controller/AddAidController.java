package com.ionres.respondph.aid.dialogs_controller;

import com.ionres.respondph.aid.*;
import com.ionres.respondph.aid.KMeansAidDistribution.BeneficiaryCluster;
import com.ionres.respondph.aid_type.AidTypeModelComboBox;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.disaster.DisasterModelComboBox;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.DashboardRefresher;
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

    @FXML private VBox  root;
    @FXML private TextField nameFld;
    @FXML private TextField quantityFld;
    @FXML private TextField quantityPerBeneficiaryFld;
    @FXML private TextField costFld;
    @FXML private TextField providerFld;

    // ── Algorithm selection (RadioButtons replacing the old CheckBox) ─────────
    @FXML private RadioButton useKMeansRadio;   // ← NEW (was CheckBox useKMeansCheckbox)
    @FXML private RadioButton useFCMRadio;      // ← NEW

    // ── Info boxes toggled by algorithm selection ─────────────────────────────
    @FXML private VBox kmeansInfoBox;           // ← NEW
    @FXML private VBox fcmInfoBox;              // ← NEW

    @FXML private Button previewBtn;
    @FXML private Button printCustomBtn;
    @FXML private Button saveAidBtn;
    @FXML private Button cancelBtn;
    @FXML private Button closeBtn;
    @FXML private Label  infoLabel;
    @FXML private HBox   simpleDistributionWarning;
    @FXML private HBox   selectionSummaryBox;
    @FXML private Label  selectionSummaryLabel;

    @FXML private ComboBox<AidTypeModelComboBox>  aidTypeComboBox;
    @FXML private ComboBox<DisasterModelComboBox> disasterComboBox;
    @FXML private CheckBox   useBarangayFilterCheckbox;
    @FXML private ComboBox<String> barangayComboBox;
    @FXML private RadioButton singleBarangayRadio;
    @FXML private RadioButton allBarangaysRadio;
    @FXML private VBox barangaySelectionContainer;
    @FXML private VBox singleBarangayContainer;
    @FXML private CheckBox generalAidCheckbox;

    private AidService aidService;
    private AidDAO     aidDAO;
    private AidController aidController;
    private Stage dialogStage;

    private ToggleGroup algorithmGroup;
    private ToggleGroup barangayModeGroup;

    private static final int FIXED_CLUSTERS = 3;



    public void setAidService(AidService aidService)       { this.aidService = aidService; }
    public void setAidController(AidController c)          { this.aidController = c; }
    public void setDialogStage(Stage stage)                { this.dialogStage = stage; }

    public void setAidTypes(List<AidTypeModelComboBox> aidTypes) {
        aidTypeComboBox.setItems(FXCollections.observableArrayList(aidTypes));
    }

    public void setDisasters(List<DisasterModelComboBox> disasters) {
        disasterComboBox.setItems(FXCollections.observableArrayList(disasters));
    }

    public void setSelectedAidTypeAndDisaster(AidTypeModelComboBox aidType,
                                              DisasterModelComboBox disaster) {
        if (aidType  != null) aidTypeComboBox.setValue(aidType);
        if (disaster != null) {
            disasterComboBox.setValue(disaster);
            generalAidCheckbox.setSelected(false);
        }
        updateSelectionSummary();
        loadBarangays();
    }



    @FXML
    private void initialize() {
        if (aidService == null) aidService = new AidServiceImpl();
        aidDAO = new AidDAOImpl(DBConnection.getInstance());

        setupAlgorithmToggle();      // ← NEW
        setupGeneralAidOption();
        setupBarangayMode();
        setupEventHandlers();
        setupDefaultValues();
        setupComboBoxListeners();
        makeDraggable();
    }


    private void setupAlgorithmToggle() {
        algorithmGroup = new ToggleGroup();
        useKMeansRadio.setToggleGroup(algorithmGroup);
        useFCMRadio.setToggleGroup(algorithmGroup);
        useKMeansRadio.setSelected(true); // K-Means is default

        algorithmGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isFCM = (newVal == useFCMRadio);

            kmeansInfoBox.setVisible(!isFCM);
            kmeansInfoBox.setManaged(!isFCM);
            fcmInfoBox.setVisible(isFCM);
            fcmInfoBox.setManaged(isFCM);


            if (simpleDistributionWarning != null) {
                simpleDistributionWarning.setVisible(false);
                simpleDistributionWarning.setManaged(false);
            }
        });
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
            boolean single = (newVal == singleBarangayRadio);
            singleBarangayContainer.setVisible(single);
            singleBarangayContainer.setManaged(single);
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
        useKMeansRadio.setSelected(true);    // K-Means on by default
        useBarangayFilterCheckbox.setSelected(false);
        generalAidCheckbox.setSelected(false);
    }

    private void setupComboBoxListeners() {
        aidTypeComboBox.setOnAction(e -> updateSelectionSummary());
        disasterComboBox.setOnAction(e -> {
            if (disasterComboBox.getValue() != null) generalAidCheckbox.setSelected(false);
            updateSelectionSummary();
            loadBarangays();
        });
        barangayComboBox.setOnAction(e -> updateSelectionSummary());
    }

    private boolean isFCMSelected() {
        return useFCMRadio != null && useFCMRadio.isSelected();
    }

    private void loadBarangays() {
        int disasterId = getSelectedDisasterId();

        AidTypeModelComboBox selectedAidType = aidTypeComboBox.getValue();
        int aidTypeId = selectedAidType != null ? selectedAidType.getAidTypeId() : 0;

        if (aidTypeId == 0) {
            barangayComboBox.setItems(FXCollections.observableArrayList());
            return;
        }

        List<String> barangays = aidDAO.getBarangaysByDisaster(disasterId, aidTypeId);
        barangayComboBox.setItems(FXCollections.observableArrayList(barangays));

        if (!barangays.isEmpty()) {
            barangayComboBox.getSelectionModel().selectFirst();
        }
    }

    private int getSelectedDisasterId() {
        if (generalAidCheckbox != null && generalAidCheckbox.isSelected()) return 0;
        DisasterModelComboBox d = disasterComboBox.getValue();
        return d != null ? d.getDisasterId() : 0;
    }

    private void updateSelectionSummary() {
        AidTypeModelComboBox selectedAidType = aidTypeComboBox.getValue();
        boolean isGeneralAid   = generalAidCheckbox != null && generalAidCheckbox.isSelected();
        DisasterModelComboBox  selectedDisaster = disasterComboBox.getValue();

        if (selectedAidType != null && (isGeneralAid || selectedDisaster != null)) {
            selectionSummaryBox.setVisible(true);
            selectionSummaryBox.setManaged(true);

            int eligibleCount = getEligibleBeneficiaryCount(
                    selectedAidType.getAidTypeId(), getSelectedDisasterId());

            String barangayInfo = getBarangayInfoText();
            String disasterInfo = isGeneralAid
                    ? "General Aid (No Disaster)"
                    : "Disaster: " + selectedDisaster.getDisasterName();

            selectionSummaryLabel.setText(String.format(
                    "Aid Type: %s | %s%s | Eligible Beneficiaries: %d",
                    selectedAidType.getAidName(), disasterInfo, barangayInfo, eligibleCount));

            infoLabel.setText(String.format("Distributing %s%s%s",
                    selectedAidType.getAidName(),
                    isGeneralAid
                            ? " (General Aid)"
                            : " for " + selectedDisaster.getDisasterName() + " disaster",
                    barangayInfo));
        } else {
            selectionSummaryBox.setVisible(false);
            selectionSummaryBox.setManaged(false);
            infoLabel.setText("Select aid type and choose disaster or general aid option");
        }
    }

    private String getBarangayInfoText() {
        if (!useBarangayFilterCheckbox.isSelected()) return "";
        if (allBarangaysRadio.isSelected()) return " | All Barangays";
        if (singleBarangayRadio.isSelected()) {
            String b = barangayComboBox.getValue();
            return b != null ? " | Barangay: " + b : "";
        }
        return "";
    }

    private int getEligibleBeneficiaryCount(int aidTypeId, int disasterId) {
        try {
            int qtyPerBeneficiary = 1;
            try {
                String t = quantityPerBeneficiaryFld.getText().trim();
                if (!t.isEmpty()) {
                    int v = Integer.parseInt(t);
                    if (v > 0) qtyPerBeneficiary = v;
                }
            } catch (NumberFormatException ignored) { }

            List<BeneficiaryCluster> eligible;

            if (!useBarangayFilterCheckbox.isSelected() || allBarangaysRadio.isSelected()) {
                eligible = isFCMSelected()
                        ? aidService.previewAidDistributionFCM(
                        aidTypeId, disasterId, Integer.MAX_VALUE, qtyPerBeneficiary, FIXED_CLUSTERS)
                        : aidService.previewAidDistribution(
                        aidTypeId, disasterId, Integer.MAX_VALUE, qtyPerBeneficiary, FIXED_CLUSTERS);
            } else {
                String barangay = barangayComboBox.getValue();
                if (barangay == null) return 0;
                eligible = isFCMSelected()
                        ? aidService.previewAidDistributionFCMByBarangay(
                        aidTypeId, disasterId, Integer.MAX_VALUE, qtyPerBeneficiary, FIXED_CLUSTERS, barangay)
                        : aidService.previewAidDistributionByBarangay(
                        aidTypeId, disasterId, Integer.MAX_VALUE, qtyPerBeneficiary, FIXED_CLUSTERS, barangay);
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
        if (cancelBtn    != null) cancelBtn.setOnAction(this::handleClose);
        if (previewBtn   != null) previewBtn.setOnAction(this::handlePreview);
        if (printCustomBtn != null) printCustomBtn.setOnAction(this::handlePrintCustom);
    }

    @FXML
    private void handlePreview(ActionEvent event) {
        if (!validateSelection()) return;
        if (!validateInput())     return;

        try {
            int aidTypeId          = aidTypeComboBox.getValue().getAidTypeId();
            int disasterId         = getSelectedDisasterId();
            int quantity           = Integer.parseInt(quantityFld.getText().trim());
            int quantityPerBeneficiary = Integer.parseInt(quantityPerBeneficiaryFld.getText().trim());

            List<BeneficiaryCluster> preview = buildPreview(
                    aidTypeId, disasterId, quantity, quantityPerBeneficiary);

            if (preview.isEmpty()) {
                AlertDialogManager.showWarning("No Eligible Beneficiaries",
                        "No beneficiaries are currently eligible for this aid distribution.");
                return;
            }

            showPreviewDialog(preview, quantityPerBeneficiary);

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
        if (!validateInput())     return;

        try {
            int aidTypeId          = aidTypeComboBox.getValue().getAidTypeId();
            int disasterId         = getSelectedDisasterId();
            int quantity           = Integer.parseInt(quantityFld.getText().trim());
            int quantityPerBeneficiary = Integer.parseInt(quantityPerBeneficiaryFld.getText().trim());

            List<BeneficiaryCluster> preview = buildPreview(
                    aidTypeId, disasterId, quantity, quantityPerBeneficiary);

            if (preview.isEmpty()) {
                AlertDialogManager.showWarning("No Eligible Beneficiaries",
                        "No beneficiaries are currently eligible for this aid distribution.");
                return;
            }

            showPrintCustomDialog(preview, quantityPerBeneficiary);

        } catch (NumberFormatException e) {
            AlertDialogManager.showError("Invalid Input", "Please enter valid quantities.");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Print Error",
                    "Failed to generate print preview:\n" + e.getMessage());
        }
    }


    private List<BeneficiaryCluster> buildPreview(int aidTypeId, int disasterId,
                                                  int quantity, int quantityPerBeneficiary) {
        boolean fcm = isFCMSelected();

        if (!useBarangayFilterCheckbox.isSelected() || allBarangaysRadio.isSelected()) {
            return fcm
                    ? aidService.previewAidDistributionFCM(
                    aidTypeId, disasterId, quantity, quantityPerBeneficiary, FIXED_CLUSTERS)
                    : aidService.previewAidDistribution(
                    aidTypeId, disasterId, quantity, quantityPerBeneficiary, FIXED_CLUSTERS);
        } else {
            String barangay = barangayComboBox.getValue();
            if (barangay == null) return new ArrayList<>();
            return fcm
                    ? aidService.previewAidDistributionFCMByBarangay(
                    aidTypeId, disasterId, quantity, quantityPerBeneficiary, FIXED_CLUSTERS, barangay)
                    : aidService.previewAidDistributionByBarangay(
                    aidTypeId, disasterId, quantity, quantityPerBeneficiary, FIXED_CLUSTERS, barangay);
        }
    }



    private Map<Integer, String> buildClusterPriorityMap(List<BeneficiaryCluster> beneficiaries) {
        Map<Integer, Double> sumMap   = new HashMap<>();
        Map<Integer, Integer> countMap = new HashMap<>();

        for (BeneficiaryCluster b : beneficiaries) {
            int c = b.getCluster();
            sumMap.put(c, sumMap.getOrDefault(c, 0.0) + b.getFinalScore());
            countMap.put(c, countMap.getOrDefault(c, 0) + 1);
        }
        for (Integer c : sumMap.keySet()) {
            sumMap.put(c, sumMap.get(c) / countMap.get(c));
        }

        List<Map.Entry<Integer, Double>> sorted = new ArrayList<>(sumMap.entrySet());
        sorted.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        String[] labels = {"High Priority", "Moderate Priority", "Low Priority"};
        Map<Integer, String> labelMap = new HashMap<>();
        for (int i = 0; i < sorted.size() && i < labels.length; i++) {
            labelMap.put(sorted.get(i).getKey(), labels[i]);
        }
        return labelMap;
    }

    private Map<String, List<BeneficiaryCluster>> groupBeneficiariesByPriority(
            List<BeneficiaryCluster> beneficiaries) {

        Map<String, List<BeneficiaryCluster>> groups = new HashMap<>();
        groups.put("High Priority",   new ArrayList<>());
        groups.put("Moderate Priority", new ArrayList<>());
        groups.put("Low Priority",    new ArrayList<>());

        Map<Integer, String> clusterPriorityMap = buildClusterPriorityMap(beneficiaries);

        for (BeneficiaryCluster b : beneficiaries) {
            String label = clusterPriorityMap.getOrDefault(b.getCluster(), "Low Priority");
            groups.get(label).add(b);
        }
        return groups;
    }



    private void showPrintCustomDialog(List<BeneficiaryCluster> preview, int qtyPerBeneficiary) {
        Map<String, List<BeneficiaryCluster>> priorityGroups = groupBeneficiariesByPriority(preview);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Print Custom - Select Priority Levels");
        dialog.setHeaderText("Select which priority levels to print:");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        CheckBox highCheck = new CheckBox("High Priority ("
                + priorityGroups.getOrDefault("High Priority", new ArrayList<>()).size()
                + " beneficiaries)");
        highCheck.setSelected(true);

        CheckBox medCheck = new CheckBox("Moderate Priority ("
                + priorityGroups.getOrDefault("Moderate Priority", new ArrayList<>()).size()
                + " beneficiaries)");
        medCheck.setSelected(true);

        CheckBox lowCheck = new CheckBox("Low Priority ("
                + priorityGroups.getOrDefault("Low Priority", new ArrayList<>()).size()
                + " beneficiaries)");
        lowCheck.setSelected(true);

        content.getChildren().addAll(
                new Label("Select priority levels to include in the print:"),
                highCheck, medCheck, lowCheck);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                List<BeneficiaryCluster> selected = new ArrayList<>();
                if (highCheck.isSelected())
                    selected.addAll(priorityGroups.getOrDefault("High Priority", new ArrayList<>()));
                if (medCheck.isSelected())
                    selected.addAll(priorityGroups.getOrDefault("Moderate Priority", new ArrayList<>()));
                if (lowCheck.isSelected())
                    selected.addAll(priorityGroups.getOrDefault("Low Priority", new ArrayList<>()));

                if (selected.isEmpty()) {
                    AlertDialogManager.showWarning("No Selection",
                            "Please select at least one priority level to print.");
                    return;
                }
                printBeneficiaryList(selected, qtyPerBeneficiary);
            }
        });
    }

    private void printBeneficiaryList(List<BeneficiaryCluster> beneficiaries, int qtyPerBeneficiary) {
        try {
            PrinterJob printerJob = PrinterJob.createPrinterJob();
            if (printerJob == null) {
                AlertDialogManager.showError("Print Error", "No printer available.");
                return;
            }
            if (!printerJob.showPrintDialog(dialogStage)) return;

            VBox printContent = createPrintContent(beneficiaries, qtyPerBeneficiary);
            if (printerJob.printPage(printContent)) {
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

    private VBox createPrintContent(List<BeneficiaryCluster> beneficiaries, int qtyPerBeneficiary) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        Label title = new Label("Aid Distribution Beneficiary List");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setAlignment(Pos.CENTER);

        String algorithm = isFCMSelected()
                ? "Fuzzy C-Means (FCM) Clustering (3 Clusters)"
                : "K-Means Clustering (3 Clusters)";

        content.getChildren().addAll(
                title,
                new Label(""),
                new Label("Distribution Scope  : " + getDistributionScopeText()),
                new Label("Distribution Method : " + algorithm),
                new Label("Total Recipients   : " + beneficiaries.size() + " beneficiaries"),
                new Label("Quantity/Beneficiary: " + qtyPerBeneficiary + " units"),
                new Label(""));

        Map<String, List<BeneficiaryCluster>> priorityGroups = groupBeneficiariesByPriority(beneficiaries);

        for (String priority : List.of("High Priority", "Moderate Priority", "Low Priority")) {
            List<BeneficiaryCluster> group = priorityGroups.get(priority);
            if (group != null && !group.isEmpty()) {
                addPrioritySection(content, priority, group, qtyPerBeneficiary);
            }
        }
        return content;
    }

    private void addPrioritySection(VBox content, String priorityName,
                                    List<BeneficiaryCluster> beneficiaries, int qty) {
        Label header = new Label(priorityName + " (" + beneficiaries.size() + " beneficiaries)");
        header.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        header.setStyle("-fx-text-fill: #2c3e50;");
        content.getChildren().add(header);

        beneficiaries.sort((b1, b2) -> Double.compare(b2.getFinalScore(), b1.getFinalScore()));
        int count = 1;
        for (BeneficiaryCluster b : beneficiaries) {
            content.getChildren().add(new Label(String.format(
                    "%d. Beneficiary #%d | Score: %.3f | Receives: %d units",
                    count++, b.getBeneficiaryId(), b.getFinalScore(), qty)));
        }
        content.getChildren().add(new Label(""));
    }



    private void showPreviewDialog(List<BeneficiaryCluster> preview, int qtyPerBeneficiary) {
        boolean fcm = isFCMSelected();
        String algorithm = fcm
                ? "Fuzzy C-Means (FCM) Clustering (3 Clusters)"
                : "K-Means Clustering (3 Clusters)";

        StringBuilder message = new StringBuilder();
        message.append("Distribution Scope  : ").append(getDistributionScopeText()).append("\n");
        message.append("Distribution Method : ").append(algorithm).append("\n");
        message.append("Total Recipients    : ").append(preview.size()).append(" beneficiaries\n");
        message.append("Quantity/Beneficiary: ").append(qtyPerBeneficiary).append(" units\n");
        message.append("Total Units         : ").append(preview.size() * qtyPerBeneficiary).append(" units\n\n");

        message.append("=== Distribution by Cluster (3 Clusters) ===\n\n");

        Map<Integer, String> clusterToPriority = buildClusterPriorityMap(preview);

        // Compute averages for the header line
        Map<Integer, Double> clusterSums  = new HashMap<>();
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

        List<Integer> sortedKeys = new ArrayList<>(clusterAvg.keySet());
        sortedKeys.sort((a, b) -> Double.compare(clusterAvg.get(b), clusterAvg.get(a)));

        Map<Integer, List<BeneficiaryCluster>> byCluster = new HashMap<>();
        for (BeneficiaryCluster b : preview) {
            byCluster.computeIfAbsent(b.getCluster(), k -> new ArrayList<>()).add(b);
        }

        int overallCount = 1;
        for (Integer clusterNum : sortedKeys) {
            String priorityName = clusterToPriority.get(clusterNum);
            List<BeneficiaryCluster> members = byCluster.get(clusterNum);

            if (members != null && !members.isEmpty()) {
                message.append(String.format("--- %s (Cluster %d, Avg Score: %.3f) ---\n",
                        priorityName, clusterNum, clusterAvg.get(clusterNum)));

                members.sort((b1, b2) -> Double.compare(b2.getFinalScore(), b1.getFinalScore()));

                for (BeneficiaryCluster b : members) {
                    message.append(String.format(
                            "%d. Beneficiary #%d | Score: %.3f | %s | Receives: %d units\n",
                            overallCount++, b.getBeneficiaryId(), b.getFinalScore(),
                            priorityName, qtyPerBeneficiary));
                }
                message.append("\n");
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Distribution Preview");
        alert.setHeaderText("Aid Distribution Plan — " + algorithm);

        TextArea textArea = new TextArea(message.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        textArea.setPrefRowCount(20);
        textArea.setPrefColumnCount(50);

        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefWidth(620);
        alert.showAndWait();
    }



    @FXML
    private void handleSave(ActionEvent event) {
        if (!validateSelection()) return;
        if (!validateInput())     return;
        if (!showConfirmationDialog()) return;

        try {
            String aidName              = nameFld.getText().trim();
            int    aidTypeId            = aidTypeComboBox.getValue().getAidTypeId();
            int    disasterId           = getSelectedDisasterId();
            int    quantity             = Integer.parseInt(quantityFld.getText().trim());
            int    quantityPerBeneficiary = Integer.parseInt(quantityPerBeneficiaryFld.getText().trim());
            double costPerUnit          = Double.parseDouble(costFld.getText().trim());
            String provider             = providerFld.getText().trim();
            boolean fcm                 = isFCMSelected();

            int distributedCount;

            if (fcm) {
                if (!useBarangayFilterCheckbox.isSelected() || allBarangaysRadio.isSelected()) {
                    distributedCount = aidService.distributeAidWithFCM(
                            aidName, aidTypeId, disasterId, quantity,
                            quantityPerBeneficiary, costPerUnit, provider, FIXED_CLUSTERS);
                } else {
                    String barangay = barangayComboBox.getValue();
                    distributedCount = aidService.distributeAidWithFCMByBarangay(
                            aidName, aidTypeId, disasterId, quantity,
                            quantityPerBeneficiary, costPerUnit, provider, FIXED_CLUSTERS, barangay);
                }
            } else {
                if (!useBarangayFilterCheckbox.isSelected() || allBarangaysRadio.isSelected()) {
                    distributedCount = aidService.distributeAidWithKMeans(
                            aidName, aidTypeId, disasterId, quantity,
                            quantityPerBeneficiary, costPerUnit, provider, FIXED_CLUSTERS);
                } else {
                    String barangay = barangayComboBox.getValue();
                    distributedCount = aidService.distributeAidWithKMeansByBarangay(
                            aidName, aidTypeId, disasterId, quantity,
                            quantityPerBeneficiary, costPerUnit, provider, FIXED_CLUSTERS, barangay);
                }
            }

            if (distributedCount > 0) {
                showSuccessDialog(aidName, distributedCount, quantityPerBeneficiary, costPerUnit);
                if (aidController != null) aidController.loadAidData();
                clearFields();
            } else {
                showNoDistributionWarning();
            }

            DashboardRefresher.refresh();
            DashboardRefresher.refreshComboBoxOfDNAndAN();

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
        if (useBarangayFilterCheckbox.isSelected()
                && singleBarangayRadio.isSelected()
                && barangayComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Selection Required", "Please select a barangay.");
            return false;
        }
        return true;
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
            if (Integer.parseInt(quantityFld.getText().trim()) <= 0) {
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
            if (Double.parseDouble(costFld.getText().trim()) < 0) {
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

    private boolean showConfirmationDialog() {
        int    totalQuantity = Integer.parseInt(quantityFld.getText().trim());
        double costPerUnit   = Double.parseDouble(costFld.getText().trim());
        double totalCost     = totalQuantity * costPerUnit;

        String algorithm = isFCMSelected()
                ? "Fuzzy C-Means (FCM) Clustering"
                : "K-Means Clustering";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Distribution");
        alert.setHeaderText("Distribute Aid to Beneficiaries");
        alert.setContentText(String.format(
                "Are you sure you want to distribute this aid?\n\n"
                        + "Aid      : %s\n"
                        + "Aid Type : %s\n"
                        + "Scope    : %s\n"
                        + "Quantity : %s units\n"
                        + "Unit Cost: ₱%s\n"
                        + "Total    : ₱%.2f\n"
                        + "Provider : %s\n"
                        + "Method   : %s (3 Clusters)",
                nameFld.getText().trim(),
                aidTypeComboBox.getValue().getAidName(),
                getDistributionScopeText(),
                quantityFld.getText().trim(),
                costFld.getText().trim(),
                totalCost,
                providerFld.getText().trim(),
                algorithm));

        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showSuccessDialog(String aidName, int count,
                                   int qtyPerBeneficiary, double costPerUnit) {
        String algorithm = isFCMSelected()
                ? "Fuzzy C-Means (FCM) Clustering (3 Clusters)"
                : "K-Means Clustering (3 Clusters)";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Distribution Successful");
        alert.setHeaderText("✓ Aid Distribution Complete");
        alert.setContentText(String.format(
                "Successfully distributed aid!\n\n"
                        + "Aid Type              : %s\n"
                        + "Scope                 : %s\n"
                        + "Beneficiaries Served  : %d\n"
                        + "Total Quantity        : %d units\n"
                        + "Total Cost            : ₱%.2f\n"
                        + "Distribution Method  : %s",
                aidName,
                getDistributionScopeText(),
                count,
                count * qtyPerBeneficiary,
                (double) count * qtyPerBeneficiary * costPerUnit,
                algorithm));
        alert.showAndWait();
    }

    private void showNoDistributionWarning() {
        AlertDialogManager.showWarning("No Distribution",
                "No beneficiaries were eligible for this aid distribution.\n\n"
                        + "Please check:\n"
                        + "• Beneficiaries have been scored for this aid type\n"
                        + "• Beneficiaries haven't already received this aid\n"
                        + "• Selected barangay has eligible beneficiaries");
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
            String b = barangayComboBox.getValue();
            barangayContext = b != null ? "Barangay: " + b : "";
        } else {
            barangayContext = "";
        }
        return disasterContext + " | " + barangayContext;
    }



    @FXML
    private void handleClose(ActionEvent event) { closeDialog(); }

    private void clearFields() {
        nameFld.clear();
        quantityFld.clear();
        quantityPerBeneficiaryFld.clear();
        costFld.clear();
        providerFld.clear();
        aidTypeComboBox.setValue(null);
        disasterComboBox.setValue(null);
        barangayComboBox.setValue(null);

        useKMeansRadio.setSelected(true);        // reset to K-Means
        useBarangayFilterCheckbox.setSelected(false);
        generalAidCheckbox.setSelected(false);
        allBarangaysRadio.setSelected(true);

        // Reset info boxes
        kmeansInfoBox.setVisible(true);
        kmeansInfoBox.setManaged(true);
        fcmInfoBox.setVisible(false);
        fcmInfoBox.setManaged(false);

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
        root.setOnMousePressed(e -> { xOffset[0] = e.getSceneX(); yOffset[0] = e.getSceneY(); });
        root.setOnMouseDragged(e -> {
            if (dialogStage != null) {
                dialogStage.setX(e.getScreenX() - xOffset[0]);
                dialogStage.setY(e.getScreenY() - yOffset[0]);
            }
        });
    }
}