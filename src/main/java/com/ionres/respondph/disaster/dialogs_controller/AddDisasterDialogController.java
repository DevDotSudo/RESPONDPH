package com.ionres.respondph.disaster.dialogs_controller;

import com.ionres.respondph.common.controller.MappingDialogController;
import com.ionres.respondph.disaster.DisasterController;
import com.ionres.respondph.disaster.DisasterModel;
import com.ionres.respondph.disaster.DisasterService;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.DashboardRefresher;
import com.ionres.respondph.util.DialogManager;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AddDisasterDialogController {

    @FXML private VBox root;
    @FXML private Button exitBtn, saveBtn;
    @FXML private ComboBox<String> disasterType;
    @FXML private TextField disasterNameFld;
    @FXML private DatePicker datePicker;
    @FXML public TextField latitudeFld;
    @FXML public TextField longitudeFld;
    @FXML private TextField radiusFld;
    @FXML private TextArea notesFld;
    @FXML Button getLocationBtn;
    private Stage dialogStage;
    private DisasterService disasterService;
    private DisasterController disasterController;
    private double yOffset;
    private double xOffset;

    @FXML
    private void initialize() {
        setupKeyHandlers();
        makeDraggable();
        EventHandler<ActionEvent> handler = this::handleActions;
        saveBtn.setOnAction(handler);
        exitBtn.setOnAction(handler);
        getLocationBtn.setOnAction(handler);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if(src == saveBtn){
            addDisaster();
        }
        else if (src == exitBtn){
            closeDialog();
        }
        else if (src == getLocationBtn){
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

    private void handleGetLocationBtn(){
        MappingDialogController controller = DialogManager.getController("mapping", MappingDialogController.class);
        controller.setListener(latLng -> {
            latitudeFld.setText(String.valueOf(latLng.lat));
            longitudeFld.setText(String.valueOf(latLng.lon));
        });
        DialogManager.show("mapping");
    }

    private void addDisaster() {
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

            DisasterModel disaster = new DisasterModel(type, disasterName, date, latitude, longitude, radius, notes, regDate);

            boolean success = disasterService.createDisaster(disaster);

            if (success) {
                AlertDialogManager.showSuccess("Success", "Disaster has been successfully added.");
                disasterController.loadTable();
                clearFields();
                DashboardRefresher.refresh();
                DashboardRefresher.refreshDisasterInSend();
                DashboardRefresher.refreshComboBoxOfDNAndAN();
            } else {
                AlertDialogManager.showError("Error", "Failed to add disaster. Please try again.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Error", "An error occurred: " + e.getMessage());
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

    private void clearFields() {
        disasterType.getSelectionModel().clearSelection();
        disasterNameFld.clear();
        datePicker.setValue(null);
        latitudeFld.clear();
        longitudeFld.clear();
        radiusFld.clear();
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

    public void setDisasterService(DisasterService disasterService) {
        this.disasterService = disasterService;
    }

    public void setDisasterController(DisasterController disasterController) {
        this.disasterController = disasterController;
    }

    public void onShow() {
        clearFields();
        root.requestFocus();
    }

    private void makeDraggable() {
        root.setOnMousePressed(e -> {
            yOffset = e.getSceneY();
            xOffset = e.getSceneX();
        });
        root.setOnMouseDragged(e -> {
            if(dialogStage != null) {
                dialogStage.setY(e.getScreenY() - yOffset);
                dialogStage.setX(e.getScreenX() - xOffset);
            }
        });
    }
}