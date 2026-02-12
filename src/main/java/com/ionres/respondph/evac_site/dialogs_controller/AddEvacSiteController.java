package com.ionres.respondph.evac_site.dialogs_controller;

import com.ionres.respondph.common.controller.MappingDialogController;
import com.ionres.respondph.evac_site.EvacSiteController;
import com.ionres.respondph.evac_site.EvacSiteModel;
import com.ionres.respondph.evac_site.EvacSiteService;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.DashboardRefresher;
import com.ionres.respondph.util.DialogManager;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AddEvacSiteController {

    @FXML private VBox root;
    @FXML private Button exitBtn;
    @FXML private Button getLocationBtn;
    @FXML private Button saveBtn;
    @FXML private TextField centerNameFld;
    @FXML private TextField capacityFld;
    @FXML private TextField latitudeFld;
    @FXML private TextField longitudeFld;
    @FXML private TextArea notesFld;
    @FXML private Label errorLabel;

    private Stage dialogStage;
    private EvacSiteService evacSiteService;
    private EvacSiteController evacSiteController;

    @FXML
    private void initialize() {
        setupKeyHandlers();
        EventHandler<ActionEvent> handler = this::handleActions;
        saveBtn.setOnAction(handler);
        exitBtn.setOnAction(handler);
        getLocationBtn.setOnAction(handler);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == saveBtn) {
            addEvacSite();
        } else if (src == exitBtn) {
            closeDialog();
        } else if (src == getLocationBtn) {
            handleGetLocationBtn();
        }
    }

    private void setupKeyHandlers() {
        root.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER: saveBtn.fire(); break;
                case ESCAPE: exitBtn.fire(); break;
            }
        });
        root.requestFocus();
    }

    private void handleGetLocationBtn() {
        MappingDialogController controller = DialogManager.getController("mapping", MappingDialogController.class);
        controller.setListener(latLng -> {
            latitudeFld.setText(String.valueOf(latLng.lat));
            longitudeFld.setText(String.valueOf(latLng.lon));
        });
        DialogManager.show("mapping");
    }

    private void addEvacSite() {
        try {
            if (!validateInput()) {
                return;
            }

            String name = centerNameFld.getText().trim();
            String capacity = capacityFld.getText().trim();
            String latitude = latitudeFld.getText().trim();
            String longitude = longitudeFld.getText().trim();
            String notes = notesFld.getText().trim();

            EvacSiteModel evacSite = new EvacSiteModel(name, capacity, latitude, longitude, notes);

            boolean success = evacSiteService.createEvacSite(evacSite);

            if (success) {
                AlertDialogManager.showSuccess("Success", "Evacuation site has been successfully added.");
                evacSiteController.loadTable();
                clearFields();
                DashboardRefresher.refreshEvacSiteMap();
            } else {
                AlertDialogManager.showError("Error", "Failed to add evacuation site. Please try again.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Error", "An error occurred: " + e.getMessage());
        }
    }

    private boolean validateInput() {
        if (centerNameFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Center name is required.");
            centerNameFld.requestFocus();
            return false;
        }

        if (capacityFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Capacity is required.");
            capacityFld.requestFocus();
            return false;
        }

        try {
            Integer.parseInt(capacityFld.getText().trim());
        } catch (NumberFormatException e) {
            AlertDialogManager.showWarning("Validation Error", "Capacity must be a valid number.");
            capacityFld.requestFocus();
            return false;
        }

        if (latitudeFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Latitude is required.");
            latitudeFld.requestFocus();
            return false;
        }

        if (longitudeFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Longitude is required.");
            longitudeFld.requestFocus();
            return false;
        }

        return true;
    }

    private void clearFields() {
        centerNameFld.clear();
        capacityFld.clear();
        latitudeFld.clear();
        longitudeFld.clear();
        notesFld.clear();
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.hide();
        }
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public Stage getDialogStage() {
        return dialogStage;
    }

    public void setEvacSiteService(EvacSiteService evacSiteService) {
        this.evacSiteService = evacSiteService;
    }

    public void setEvacSiteController(EvacSiteController evacSiteController) {
        this.evacSiteController = evacSiteController;
    }

    public void onShow() {
        clearFields();
        root.requestFocus();
    }
}