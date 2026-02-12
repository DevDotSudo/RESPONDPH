package com.ionres.respondph.evacuation_plan.dialog_controller;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.evacuation_plan.EvacuationPlanController;
import com.ionres.respondph.evacuation_plan.GeoBasedEvacPlanService;
import com.ionres.respondph.evacuation_plan.RankedBeneficiaryWithLocation;
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

public class AllocateEvacSiteDialogController {

    @FXML private VBox root;
    @FXML private ComboBox<DisasterModel> disasterComboBox;
    @FXML private Button allocateBtn;
    @FXML private Button closeBtn;

    private Stage dialogStage;
    private EvacuationPlanController evacPlanController;
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

        root.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER: allocateBtn.fire(); break;
                case ESCAPE: closeBtn.fire(); break;
            }
        });
    }

    private void setupComboBoxes() {

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
            performAllocation();
            evacPlanController.loadTable();
        } else if (src == closeBtn) {
            closeDialog();
        }
    }

    private void performAllocation() {

        if (disasterComboBox.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Please select a disaster.");
            disasterComboBox.requestFocus();
            return;
        }

        int disasterId = disasterComboBox.getValue().getDisasterId();

        try {
            GeoBasedEvacPlanService geoBasedEvacPlanService = new GeoBasedEvacPlanService(DBConnection.getInstance());
            List<RankedBeneficiaryWithLocation> allocate = geoBasedEvacPlanService.autoAllocateByProximity(disasterId);

            if(allocate != null) {
                AlertDialogManager.showSuccess("Allocation Success", "Successfully Allocated Beneficiaries");
            }
            else {
                AlertDialogManager.showWarning("Allocation Error", "Please select a disaster.");
            }
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

    public void setEvacPlanController(EvacuationPlanController evacPlanController) {
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
        }
    }

    private void loadDisasters() {
        if (disasterService != null) {
            List<DisasterModel> disasters = disasterService.getAllDisaster();
            disasterComboBox.setItems(FXCollections.observableArrayList(disasters));
        }
    }
}