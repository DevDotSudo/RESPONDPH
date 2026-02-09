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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AddAidController {

    @FXML private VBox addAidRoot;
    @FXML private TextField nameFld;
    @FXML private TextField quantityFld;
    @FXML private TextField quantityPerBeneficiaryFld;
    @FXML private TextField costFld;
    @FXML private TextField providerFld;
    @FXML private CheckBox useKMeansCheckbox;
    @FXML private Button previewBtn;
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

        setupBarangayMode();
        setupEventHandlers();
        setupDefaultValues();
        setupComboBoxListeners();
        makeDraggable();
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
    }

    private void setupComboBoxListeners() {
        aidTypeComboBox.setOnAction(e -> updateSelectionSummary());
        disasterComboBox.setOnAction(e -> {
            updateSelectionSummary();
            loadBarangays();
        });
        barangayComboBox.setOnAction(e -> updateSelectionSummary());
    }

    private void loadBarangays() {
        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();

        List<String> barangays;
        if (selectedDisaster != null) {
            barangays = aidDAO.getBarangaysByDisaster(selectedDisaster.getDisasterId());
        } else {
            barangays = aidDAO.getAllBarangays();
        }

        barangays = barangays.stream()
                .sorted()
                .collect(Collectors.toList());

        barangayComboBox.setItems(FXCollections.observableArrayList(barangays));
        if (!barangays.isEmpty()) {
            barangayComboBox.getSelectionModel().selectFirst();
        }
    }

    private void updateSelectionSummary() {
        AidTypeModelComboBox selectedAidType = aidTypeComboBox.getValue();
        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();

        if (selectedAidType != null && selectedDisaster != null) {
            selectionSummaryBox.setVisible(true);
            selectionSummaryBox.setManaged(true);

            int eligibleCount = getEligibleBeneficiaryCount(
                    selectedAidType.getAidTypeId(),
                    selectedDisaster.getDisasterId()
            );

            String barangayInfo = getBarangayInfoText();

            selectionSummaryLabel.setText(String.format(
                    "Aid Type: %s | Disaster: %s%s | Eligible Beneficiaries: %d",
                    selectedAidType.getAidName(),
                    selectedDisaster.getDisasterName(),
                    barangayInfo,
                    eligibleCount
            ));

            infoLabel.setText(String.format(
                    "Distributing %s for %s disaster%s",
                    selectedAidType.getAidName(),
                    selectedDisaster.getDisasterName(),
                    barangayInfo
            ));
        } else {
            selectionSummaryBox.setVisible(false);
            selectionSummaryBox.setManaged(false);
            infoLabel.setText("Select aid type and disaster to begin distribution");
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
        if (!validateSelection()) {
            return;
        }

        if (!validateInput()) {
            return;
        }

        try {
            int aidTypeId = aidTypeComboBox.getValue().getAidTypeId();
            int disasterId = disasterComboBox.getValue().getDisasterId();
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
                    AlertDialogManager.showWarning("Selection Required",
                            "Please select a barangay.");
                    return;
                }
                preview = aidService.previewAidDistributionByBarangay(
                        aidTypeId, disasterId, quantity, quantityPerBeneficiary, FIXED_CLUSTERS, barangay
                );
            }

            if (preview.isEmpty()) {
                AlertDialogManager.showWarning(
                        "No Eligible Beneficiaries",
                        "No beneficiaries are currently eligible for this aid distribution."
                );
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

    private void showPreviewDialog(List<BeneficiaryCluster> preview, boolean usedKMeans, int qtyPerBeneficiary) {
        StringBuilder message = new StringBuilder();

        String distributionScope = getDistributionScopeText();

        message.append(String.format("Distribution Scope: %s\n", distributionScope));
        message.append(String.format("Distribution Method: %s\n",
                usedKMeans ? "K-Means Clustering (3 Clusters)" : "Score-Based"));
        message.append(String.format("Total Recipients: %d beneficiaries\n", preview.size()));
        message.append(String.format("Quantity per Beneficiary: %d units\n", qtyPerBeneficiary));
        message.append(String.format("Total Units to Distribute: %d units\n\n", preview.size() * qtyPerBeneficiary));

        if (usedKMeans) {
            message.append("=== Distribution by Cluster (3 Clusters) ===\n\n");

            String[] clusterNames = {"Low Priority", "Medium Priority", "High Priority"};

            int currentCluster = -1;
            int count = 1;

            for (BeneficiaryCluster b : preview) {
                if (b.getCluster() != currentCluster) {
                    if (currentCluster != -1) {
                        message.append("\n");
                    }
                    currentCluster = b.getCluster();
                    String clusterName = (currentCluster >= 0 && currentCluster < 3) ?
                            clusterNames[currentCluster] : "Cluster " + currentCluster;
                    message.append(String.format("--- %s (Cluster %d) ---\n", clusterName, currentCluster));
                }

                message.append(String.format(
                        "%d. Beneficiary #%d | Score: %.3f | %s | Receives: %d units\n",
                        count++,
                        b.getBeneficiaryId(),
                        b.getFinalScore(),
                        b.getScoreCategory(),
                        qtyPerBeneficiary
                ));
            }
        } else {
            message.append("=== Top Beneficiaries by Score ===\n\n");

            for (int i = 0; i < preview.size(); i++) {
                BeneficiaryCluster b = preview.get(i);
                message.append(String.format(
                        "%d. Beneficiary #%d | Score: %.3f | %s | Receives: %d units\n",
                        i + 1,
                        b.getBeneficiaryId(),
                        b.getFinalScore(),
                        b.getScoreCategory(),
                        qtyPerBeneficiary
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
        if (!useBarangayFilterCheckbox.isSelected() || allBarangaysRadio.isSelected()) {
            return "All Barangays";
        } else if (singleBarangayRadio.isSelected()) {
            String barangay = barangayComboBox.getValue();
            return barangay != null ? "Barangay: " + barangay : "";
        }
        return "";
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (!validateSelection()) {
            return;
        }

        if (!validateInput()) {
            return;
        }

        if (!showConfirmationDialog()) {
            return;
        }

        try {
            String aidName = nameFld.getText().trim();
            int aidTypeId = aidTypeComboBox.getValue().getAidTypeId();
            int disasterId = disasterComboBox.getValue().getDisasterId();
            int quantity = Integer.parseInt(quantityFld.getText().trim());
            int quantityPerBeneficiary = Integer.parseInt(quantityPerBeneficiaryFld.getText().trim());
            double costPerUnit = Double.parseDouble(costFld.getText().trim());
            String provider = providerFld.getText().trim();

            boolean useKMeans = useKMeansCheckbox != null && useKMeansCheckbox.isSelected();

            int distributedCount;

            if (useKMeans) {
                if (!useBarangayFilterCheckbox.isSelected() || allBarangaysRadio.isSelected()) {
                    // Standard K-means distribution
                    distributedCount = aidService.distributeAidWithKMeans(
                            aidName, aidTypeId, disasterId, quantity, quantityPerBeneficiary,
                            costPerUnit, provider, FIXED_CLUSTERS
                    );
                } else {
                    // Single barangay K-means
                    String barangay = barangayComboBox.getValue();
                    distributedCount = aidService.distributeAidWithKMeansByBarangay(
                            aidName, aidTypeId, disasterId, quantity, quantityPerBeneficiary,
                            costPerUnit, provider, FIXED_CLUSTERS, barangay
                    );
                }
            } else {
                // Simple distribution (no barangay filtering for simple mode)
                distributedCount = aidService.distributeAidSimple(
                        aidName, aidTypeId, disasterId, quantity, quantityPerBeneficiary, costPerUnit, provider
                );
            }

            if (distributedCount > 0) {
                showSuccessDialog(aidName, distributedCount, costPerUnit, useKMeans);

                if (aidController != null) {
                    aidController.loadAidData();
                }

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

        if (disasterComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Selection Required",
                    "Please select a Disaster Event before distributing aid.");
            disasterComboBox.requestFocus();
            return false;
        }

        // Validate barangay selection if filter is enabled
        if (useBarangayFilterCheckbox.isSelected()) {
            if (singleBarangayRadio.isSelected() && barangayComboBox.getValue() == null) {
                AlertDialogManager.showWarning("Selection Required",
                        "Please select a barangay.");
                return false;
            }
        }

        return true;
    }

    private boolean showConfirmationDialog() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Distribution");
        confirmAlert.setHeaderText("Distribute Aid to Beneficiaries");

        double totalCost = Integer.parseInt(quantityFld.getText().trim()) *
                Double.parseDouble(costFld.getText().trim());

        String scopeText = getDistributionScopeText();

        confirmAlert.setContentText(
                String.format(
                        "Are you sure you want to distribute this aid?\n\n" +
                                "Aid: %s\n" +
                                "Aid Type: %s\n" +
                                "Disaster: %s\n" +
                                "Scope: %s\n" +
                                "Quantity: %s units\n" +
                                "Cost per unit: ₱%s\n" +
                                "Total cost: ₱%.2f\n" +
                                "Provider: %s\n" +
                                "Method: %s (3 Clusters)",
                        nameFld.getText().trim(),
                        aidTypeComboBox.getValue().getAidName(),
                        disasterComboBox.getValue().getDisasterName(),
                        scopeText,
                        quantityFld.getText().trim(),
                        costFld.getText().trim(),
                        totalCost,
                        providerFld.getText().trim(),
                        (useKMeansCheckbox != null && useKMeansCheckbox.isSelected()) ?
                                "K-means Clustering" : "Score-based"
                )
        );

        return confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showSuccessDialog(String aidName, int count, double costPerUnit, boolean usedKMeans) {
        double totalCost = count * costPerUnit;

        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
        successAlert.setTitle("Distribution Successful");
        successAlert.setHeaderText("✓ Aid Distribution Complete");

        String scopeText = getDistributionScopeText();

        successAlert.setContentText(
                String.format(
                        "Successfully distributed aid!\n\n" +
                                "Aid Type: %s\n" +
                                "Scope: %s\n" +
                                "Beneficiaries Served: %d\n" +
                                "Total Quantity: %d units\n" +
                                "Total Cost: ₱%.2f\n" +
                                "Distribution Method: %s",
                        aidName,
                        scopeText,
                        count,
                        count,
                        totalCost,
                        usedKMeans ? "K-means Clustering (3 Clusters)" : "Score-based"
                )
        );
        successAlert.showAndWait();
    }

    private void showNoDistributionWarning() {
        AlertDialogManager.showWarning(
                "No Distribution",
                "No beneficiaries were eligible for this aid distribution.\n\n" +
                        "Please check:\n" +
                        "• Beneficiaries have been scored for this aid type\n" +
                        "• Beneficiaries haven't already received this aid\n" +
                        "• Beneficiaries are affected by this disaster\n" +
                        "• Selected barangay has eligible beneficiaries"
        );
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

        if (useKMeansCheckbox != null) {
            useKMeansCheckbox.setSelected(true);
        }

        if (useBarangayFilterCheckbox != null) {
            useBarangayFilterCheckbox.setSelected(false);
        }

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

        addAidRoot.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });

        addAidRoot.setOnMouseDragged(event -> {
            Stage stage = (Stage) addAidRoot.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset[0]);
            stage.setY(event.getScreenY() - yOffset[0]);
        });
    }
}