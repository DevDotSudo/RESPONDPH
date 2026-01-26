package com.ionres.respondph.disaster.dialogs_controller;

import com.ionres.respondph.disaster.DisasterController;
import com.ionres.respondph.disaster.DisasterModel;
import com.ionres.respondph.disaster.DisasterService;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.DashboardRefresher;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static com.ionres.respondph.util.LatLongValidation.setNumericCoordinateFilter;
import static com.ionres.respondph.util.LatLongValidation.setNumericNumberFilter;

public class EditDisasterDialogController {

    @FXML private VBox root;
    @FXML private Button exitBtn, updateBtn;
    @FXML private ComboBox<String> disasterType;
    @FXML private TextField disasterNameFld;
    @FXML private DatePicker datePicker;
    @FXML private TextField latitudeFld;
    @FXML private TextField longitudeFld;
    @FXML private TextField radiusFld;
    @FXML private TextField notesFld;

    private DisasterService disasterService;
    private DisasterController disasterController;
    private DisasterModel currentDisaster;
    private Stage dialogStage;

    @FXML
    private void initialize() {
        setNumberValidation();
        initializeDisasterTypeDropdowns();
        setupEventHandlers();
        setupKeyHandlers();
    }

    private void setupEventHandlers() {
        exitBtn.setOnAction(this::handleExit);
        updateBtn.setOnAction(this::handleUpdate);
    }

    private void setupKeyHandlers() {
        root.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER: updateBtn.fire(); break;
                case ESCAPE: exitBtn.fire(); break;
            }
        });
        root.requestFocus();
    }

    private void setNumberValidation(){
        setNumericCoordinateFilter(latitudeFld, 90.0, "Latitude");

        setNumericCoordinateFilter(longitudeFld, 180.0, "Longitude");

        setNumericNumberFilter(radiusFld);
    }

    private void handleUpdate(ActionEvent event) {
        updateDisaster();
    }

    private void handleExit(ActionEvent event) {
        closeDialog();
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

    private void populateFields(DisasterModel disaster) {
        this.currentDisaster = disaster;

        disasterType.setValue(disaster.getDisasterType());
        disasterNameFld.setText(disaster.getDisasterName());

        if (disaster.getDate() != null && !disaster.getDate().isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(disaster.getDate());
                datePicker.setValue(date);
            } catch (DateTimeParseException e) {
                System.err.println("Error parsing date: " + e.getMessage());
            }
        }

        latitudeFld.setText(disaster.getLat());
        longitudeFld.setText(disaster.getLongi());
        radiusFld.setText(disaster.getRadius());
        notesFld.setText(disaster.getNotes());
    }

    private void updateDisaster() {
        try {
            if (!validateInput()) {
                return;
            }

            String type = disasterType.getValue();
            String disasterName = disasterNameFld.getText().trim();
            String date = datePicker.getValue() != null ? datePicker.getValue().toString() : "";
            String latitude = latitudeFld.getText().trim();
            String longitude = longitudeFld.getText().trim();
            String radius = radiusFld.getText().trim();
            String notes = notesFld.getText().trim();

            String regDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            DisasterModel updatedDisaster = new DisasterModel(
                    type, disasterName, date, latitude, longitude, radius, notes, regDate
            );
            updatedDisaster.setDisasterId(currentDisaster.getDisasterId());

            boolean success = disasterService.updateDisaster(updatedDisaster);

            if (success) {
                AlertDialogManager.showSuccess("Update Successful",
                        "Disaster information has been successfully updated.");
                disasterController.loadTable();
                DashboardRefresher.refresh();
                DashboardRefresher.refreshComboBoxOfDNAndAN();
                closeDialog();
            } else {
                AlertDialogManager.showError("Update Failed",
                        "Failed to update disaster. Please try again.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Update Error",
                    "An error occurred while updating disaster: " + e.getMessage());
        }
    }

    private boolean validateInput() {
        if (disasterType.getValue() == null || disasterType.getValue().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Disaster type is required.");
            disasterType.requestFocus();
            return false;
        }

        if (disasterNameFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Disaster name is required.");
            disasterNameFld.requestFocus();
            return false;
        }

        if (datePicker.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Date is required.");
            datePicker.requestFocus();
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

        if (radiusFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Radius is required.");
            radiusFld.requestFocus();
            return false;
        }

        if (notesFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Notes are required.");
            notesFld.requestFocus();
            return false;
        }

        return true;
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.hide();
        } else if (exitBtn.getScene() != null && exitBtn.getScene().getWindow() instanceof Stage) {
            ((Stage) exitBtn.getScene().getWindow()).hide();
        }
    }

    private void clearFields() {
        disasterType.getSelectionModel().clearSelection();
        disasterNameFld.clear();
        datePicker.setValue(null);
        latitudeFld.clear();
        longitudeFld.clear();
        radiusFld.clear();
        notesFld.clear();
    }

    // Getters and Setters
    public void setDisasterService(DisasterService disasterService) {
        this.disasterService = disasterService;
    }

    public void setDisasterController(DisasterController disasterController) {
        this.disasterController = disasterController;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setDisaster(DisasterModel disaster) {
        populateFields(disaster);
    }
}