package com.ionres.respondph.disaster.dialogs_controller;

import com.ionres.respondph.disaster.DisasterController;
import com.ionres.respondph.disaster.DisasterModel;
import com.ionres.respondph.disaster.DisasterService;
import com.ionres.respondph.util.AlertDialog;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;

public class EditDisasterDialogController {

    @FXML
    private VBox root;

    @FXML
    private Button exitBtn, updateBtn;

    @FXML
    private ComboBox<String> disasterType;

    @FXML
    private TextField disasterNameFld;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField latitudeFld;

    @FXML
    private TextField longitudeFld;

    @FXML
    private TextField radiusFld;

    @FXML
    private TextField notesFld;

    private DisasterService disasterService;
    private DisasterController disasterController;
    private DisasterModel currentDisaster;
    private Stage dialogStage;

    AlertDialog alertDialog = new AlertDialog();

    public void setDisasterService(DisasterService disasterService) {
        this.disasterService = disasterService;
    }

    public void setDisasterController(DisasterController disasterController) {
        this.disasterController = disasterController;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setDisaster(DisasterModel dm) {
        this.currentDisaster = dm;
        populateFields(dm);
    }

    @FXML
    private void initialize() {
        initializeDisasterTypeDropdowns();

        EventHandler<ActionEvent> handlers = this::handleActions;
        exitBtn.setOnAction(handlers);
        updateBtn.setOnAction(handlers);

        root.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER: updateBtn.fire();
                case ESCAPE: exitBtn.fire();
            }
        });

        root.requestFocus();
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == updateBtn) {
            updateDisaster();
            disasterController.loadTable();
            clearFields();
        } else if (src == exitBtn) {
            closeDialog();
        }
    }

    private void initializeDisasterTypeDropdowns() {
        disasterType.getItems().addAll(
                "Earthquake",
                "Tsunami",
                "Volcanic eruption",
                "Typhoon / Storm",
                "Landslide",
                "Storm surge",
                "Drought",
                "Wildfire",
                "Heat wave"
        );
    }

    private void populateFields(DisasterModel dm) {
        disasterType.setValue(dm.getDisasterType());
        disasterNameFld.setText(dm.getDisasterName());

        if (dm.getDate() != null && !dm.getDate().isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(dm.getDate());
                datePicker.setValue(date);
            } catch (Exception e) {
                System.out.println("Error parsing date: " + e.getMessage());
            }
        }

        latitudeFld.setText(dm.getLat());
        longitudeFld.setText(dm.getLongi());
        radiusFld.setText(dm.getRadius());
        notesFld.setText(dm.getNotes());
    }

    private void updateDisaster() {
        try {
            String type = disasterType.getValue();
            String disasterName = disasterNameFld.getText().trim();
            String date = datePicker.getValue() != null ? datePicker.getValue().toString() : "";
            String latitude = latitudeFld.getText().trim();
            String longitude = longitudeFld.getText().trim();
            String radius = radiusFld.getText().trim();
            String notes = notesFld.getText().trim();

            String regDate = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            if (type == null || type.isEmpty()) {
                alertDialog.showWarning("Disaster type is required");
                return;
            }
            if (disasterName.isEmpty()) {
                alertDialog.showWarning("Disaster name is required");
                return;
            }
            if (date.isEmpty()) {
                alertDialog.showWarning("Date is required");
                return;
            }
            if (latitude.isEmpty()) {
                alertDialog.showWarning("Latitude is required");
                return;
            }
            if (longitude.isEmpty()) {
                alertDialog.showWarning("Longitude is required");
                return;
            }
            if (radius.isEmpty()) {
                alertDialog.showWarning("Radius is required");
                return;
            }
            if (notes.isEmpty()) {
                alertDialog.showWarning("Notes are required");
                return;
            }

            DisasterModel updatedDm = new DisasterModel(
                    type, disasterName, date, latitude, longitude, radius, notes, regDate
            );
            updatedDm.setDisasterId(currentDisaster.getDisasterId());

            boolean success = disasterService.updateDisaster(updatedDm);

            if (success) {
                alertDialog.showSuccess("Success", "Disaster updated successfully.");
            } else {
                alertDialog.showErrorAlert("Error", "Failed to update disaster.");
            }

        } catch (Exception e) {
            alertDialog.showErrorAlert("Error", e.getMessage());
            e.printStackTrace();
        }
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        } else {
            ((Stage) exitBtn.getScene().getWindow()).close();
        }
    }

    private void clearFields() {
        disasterType.getSelectionModel().clearSelection();
        disasterType.setValue(null);

        disasterNameFld.setText("");
        datePicker.setValue(null);

        latitudeFld.setText("");
        longitudeFld.setText("");
        radiusFld.setText("");
        notesFld.setText("");
    }
}