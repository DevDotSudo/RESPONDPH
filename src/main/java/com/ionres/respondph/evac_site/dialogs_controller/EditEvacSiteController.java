package com.ionres.respondph.evac_site.dialogs_controller;

import com.ionres.respondph.common.controller.MappingDialogController;
import com.ionres.respondph.evac_site.EvacSiteController;
import com.ionres.respondph.evac_site.EvacSiteModel;
import com.ionres.respondph.evac_site.EvacSiteService;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.DialogManager;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class EditEvacSiteController {

    @FXML private VBox root;
    @FXML private Button exitBtn;
    @FXML private Button getLocationBtn;
    @FXML private Button editBtn;
    @FXML private TextField centerNameFld;
    @FXML private TextField capacityFld;
    @FXML private TextField latitudeFld;
    @FXML private TextField longitudeFld;
    @FXML private TextArea notesFld;
    @FXML private Label errorLabel;

    private Stage dialogStage;
    private EvacSiteService evacSiteService;
    private EvacSiteController evacSiteController;
    private EvacSiteModel currentEvacSite;

    @FXML
    private void initialize() {
        setupKeyHandlers();
        EventHandler<ActionEvent> handler = this::handleActions;
        editBtn.setOnAction(handler);
        exitBtn.setOnAction(handler);
        getLocationBtn.setOnAction(handler);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == editBtn) {
            updateEvacSite();
        } else if (src == exitBtn) {
            closeDialog();
        } else if (src == getLocationBtn) {
            handleGetLocationBtn();
        }
    }

    private void setupKeyHandlers() {
        root.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER: editBtn.fire(); break;
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

    private void updateEvacSite() {
        try {
            if (!validateInput()) {
                return;
            }

            currentEvacSite.setName(centerNameFld.getText().trim());
            currentEvacSite.setCapacity(capacityFld.getText().trim());
            currentEvacSite.setLat(latitudeFld.getText().trim());
            currentEvacSite.setLongi(longitudeFld.getText().trim());
            currentEvacSite.setNotes(notesFld.getText().trim());

            boolean success = evacSiteService.updateEvacSite(currentEvacSite);

            if (success) {
                AlertDialogManager.showSuccess("Success", "Evacuation site has been successfully updated.");
                evacSiteController.loadTable();
                closeDialog();
            } else {
                AlertDialogManager.showError("Error", "Failed to update evacuation site. Please try again.");
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

    public void setEvacSite(EvacSiteModel evacSite) {
        this.currentEvacSite = evacSite;
        populateFields();
    }

    private void populateFields() {
        if (currentEvacSite != null) {
            centerNameFld.setText(currentEvacSite.getName());
            capacityFld.setText(currentEvacSite.getCapacity());
            latitudeFld.setText(currentEvacSite.getLat());
            longitudeFld.setText(currentEvacSite.getLongi());
            notesFld.setText(currentEvacSite.getNotes());
        }
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.hide();
        }
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setEvacSiteService(EvacSiteService evacSiteService) {
        this.evacSiteService = evacSiteService;
    }

    public void setEvacSiteController(EvacSiteController evacSiteController) {
        this.evacSiteController = evacSiteController;
    }

    public void onShow() {
        root.requestFocus();
    }
}