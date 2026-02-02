package com.ionres.respondph.evac_plan.dialogs_controller;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.evac_plan.EvacPlanController;
import com.ionres.respondph.evac_plan.GeoBasedEvacPlanService;
import com.ionres.respondph.evac_plan.RankedBeneficiaryModel;
import com.ionres.respondph.evac_plan.RankedBeneficiaryWithLocation;
import com.ionres.respondph.evac_site.EvacSiteModel;
import com.ionres.respondph.evac_site.EvacSiteService;
import com.ionres.respondph.disaster.DisasterModel;
import com.ionres.respondph.disaster.DisasterService;
import com.ionres.respondph.util.AlertDialogManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class AllocateEvacSiteController {

    @FXML private VBox root;
    @FXML private ComboBox<EvacSiteModel> evacSiteComboBox;
    @FXML private ComboBox<DisasterModel> disasterComboBox;
    @FXML private Label capacityLabel;
    @FXML private Label occupiedLabel;
    @FXML private Label remainingLabel;
    @FXML private TextArea resultTextArea;
    @FXML private Button allocateBtn;
    @FXML private Button closeBtn;

    private Stage dialogStage;
    private EvacPlanController evacPlanController;
    private EvacSiteService evacSiteService;
    private DisasterService disasterService;

    @FXML
    private void initialize() {
        setupEventHandlers();
        setupComboBoxes();
    }

    private void setupEventHandlers() {
        EventHandler<ActionEvent> handler = this::handleActions;
        allocateBtn.setOnAction(handler);
        closeBtn.setOnAction(handler);

        // Update capacity info when evac site is selected
        evacSiteComboBox.setOnAction(e -> updateCapacityInfo());

        root.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER: allocateBtn.fire(); break;
                case ESCAPE: closeBtn.fire(); break;
            }
        });
    }

    private void setupComboBoxes() {
        evacSiteComboBox.setCellFactory(lv -> new ListCell<EvacSiteModel>() {
            @Override
            protected void updateItem(EvacSiteModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (Capacity: " + item.getCapacity() + ")");
                }
            }
        });
        evacSiteComboBox.setButtonCell(new ListCell<EvacSiteModel>() {
            @Override
            protected void updateItem(EvacSiteModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (Capacity: " + item.getCapacity() + ")");
                }
            }
        });

        // Custom cell factory to display disaster name and date
        disasterComboBox.setCellFactory(lv -> new ListCell<DisasterModel>() {
            @Override
            protected void updateItem(DisasterModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisasterName() + " (" + item.getDate() + ")");
                }
            }
        });
        disasterComboBox.setButtonCell(new ListCell<DisasterModel>() {
            @Override
            protected void updateItem(DisasterModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisasterName() + " (" + item.getDate() + ")");
                }
            }
        });
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == allocateBtn) {
//            performAllocation();
        } else if (src == closeBtn) {
            closeDialog();
        }
    }

    private void updateCapacityInfo() {
        EvacSiteModel selectedSite = evacSiteComboBox.getValue();
        if (selectedSite != null) {
            capacityLabel.setText(selectedSite.getCapacity() + " persons");
            // Note: occupied/remaining would need to be fetched from database
            // For now, just show capacity
            occupiedLabel.setText("N/A");
            remainingLabel.setText("N/A");
        }
    }

    private void showAllocationResults(List<RankedBeneficiaryWithLocation> assigned) {
        StringBuilder results = new StringBuilder();
        results.append("ALLOCATION RESULTS\n");
        results.append("==================\n\n");

        // Group by evacuation site
        Map<String, List<RankedBeneficiaryWithLocation>> groupedBySite = assigned.stream()
                .collect(Collectors.groupingBy(RankedBeneficiaryWithLocation::getAssignedEvacSiteName));

        for (Map.Entry<String, List<RankedBeneficiaryWithLocation>> entry : groupedBySite.entrySet()) {
            String siteName = entry.getKey();
            List<RankedBeneficiaryWithLocation> beneficiaries = entry.getValue();

            int totalPersons = beneficiaries.stream()
                    .mapToInt(RankedBeneficiaryWithLocation::getHouseholdMembers)
                    .sum();

            results.append(String.format("%s (%d beneficiaries, %d persons):\n",
                    siteName, beneficiaries.size(), totalPersons));

            for (RankedBeneficiaryWithLocation ben : beneficiaries) {
                results.append(String.format("  - %s %s (%.2f km, %d persons)\n",
                        ben.getFirstName(),
                        ben.getLastName(),
                        ben.getDistanceToEvacSite(),
                        ben.getHouseholdMembers()));
            }
            results.append("\n");
        }

        // Show in a dialog or text area
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Allocation Results");
        alert.setHeaderText("Beneficiaries assigned to evacuation sites:");

        TextArea textArea = new TextArea(results.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    private void performAllocation() {
        if (evacSiteComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Please select an evacuation site.");
            evacSiteComboBox.requestFocus();
            return;
        }

        if (disasterComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Please select a disaster.");
            disasterComboBox.requestFocus();
            return;
        }

        int evacSiteId = evacSiteComboBox.getValue().getEvacId();
        int disasterId = disasterComboBox.getValue().getDisasterId();

        try {
            // Call the controller to perform allocation
//            List<RankedBeneficiaryModel> assigned = evacPlanController.allocateEvacSite(evacSiteId, disasterId);
            GeoBasedEvacPlanService geoBasedEvacPlanService = new GeoBasedEvacPlanService(DBConnection.getInstance());
            geoBasedEvacPlanService.autoAllocateByProximity(disasterId);

//            if (assigned.isEmpty()) {
//                AlertDialogManager.showInfo("No Allocation",
//                        "No beneficiaries were allocated. The site may be full or there are no eligible beneficiaries.");
//                resultTextArea.setText("No beneficiaries allocated.");
//            } else {
//                // Build result message
//                StringBuilder result = new StringBuilder();
//                result.append("Successfully allocated ").append(assigned.size()).append(" beneficiaries:\n\n");
//
//                int totalPersons = 0;
//                for (int i = 0; i < assigned.size(); i++) {
//                    RankedBeneficiaryModel ben = assigned.get(i);
//                    totalPersons += ben.getHouseholdMembers();
//                    result.append(String.format("%d. %s %s | Score: %.2f | Household: %d persons\n",
//                            i + 1,
//                            ben.getFirstName(),
//                            ben.getLastName(),
//                            ben.getFinalScore(),
//                            ben.getHouseholdMembers()));
//                }
//
//                result.append("\nTotal persons allocated: ").append(totalPersons);
//
//                resultTextArea.setText(result.toString());
//
//                AlertDialogManager.showSuccess("Allocation Complete",
//                        "Successfully allocated " + assigned.size() + " beneficiaries (" + totalPersons + " persons).");
//            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Error", "An error occurred during allocation: " + e.getMessage());
        }
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setEvacPlanController(EvacPlanController evacPlanController) {
        this.evacPlanController = evacPlanController;
    }

    public void setEvacSiteService(EvacSiteService evacSiteService) {
        this.evacSiteService = evacSiteService;
        loadEvacSites();
    }

    public void setDisasterService(DisasterService disasterService) {
        this.disasterService = disasterService;
        loadDisasters();
    }

    private void loadEvacSites() {
        if (evacSiteService != null) {
            List<EvacSiteModel> sites = evacSiteService.getAllEvacSites();
            evacSiteComboBox.setItems(FXCollections.observableArrayList(sites));
        }
    }

    private void loadDisasters() {
        if (disasterService != null) {
            List<DisasterModel> disasters = disasterService.getAllDisaster();
            disasterComboBox.setItems(FXCollections.observableArrayList(disasters));
        }
    }

    public void onShow() {
        resultTextArea.clear();
        evacSiteComboBox.setValue(null);
        disasterComboBox.setValue(null);
        capacityLabel.setText("N/A");
        occupiedLabel.setText("N/A");
        remainingLabel.setText("N/A");
        root.requestFocus();
    }
}