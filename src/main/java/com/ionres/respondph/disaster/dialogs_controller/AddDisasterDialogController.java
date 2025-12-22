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

public class AddDisasterDialogController {

    @FXML
    private VBox root;

    @FXML
    private Button exitBtn, saveBtn;

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

    private Stage dialogStage;

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
        onShow();
    }

    public Stage getDialogStage() {
        return dialogStage;
    }

    AlertDialog alertDialog = new AlertDialog();

    private DisasterService disasterService;
    private DisasterController disasterController;

    public void setDisasterService(DisasterService disasterService) {
        this.disasterService = disasterService;
    }
    public void setDisasterController(DisasterController disasterController) {
        this.disasterController = disasterController;
    }

    @FXML
    private void initialize() {
        initializeDisasterTypeDropdowns();

        setupKeyHandlers();
        setupActionHandlers();

    }
    private void setupKeyHandlers() {
        root.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER: saveBtn.fire();
                case ESCAPE: exitBtn.fire();
            }
        });

        root.requestFocus();
    }
    private void setupActionHandlers() {
        EventHandler<ActionEvent> handlers = this::handleActions;
        saveBtn.setOnAction(handlers);
        exitBtn.setOnAction(handlers);
    }


    private void handleActions(ActionEvent event){
        Object src = event.getSource();

        if (src == saveBtn){
            addDisaster();
            disasterController.loadTable();
            clearFields();

        }
        else if(src == exitBtn){
            closeDialog();
        }

    }

    private void initializeDisasterTypeDropdowns(){

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

    private void addDisaster(){
        try {
            String type = disasterType.getValue().trim();
            String disasterName       = disasterNameFld.getText().trim();
            String date        = datePicker.getValue() != null
                    ? datePicker.getValue().toString()
                    : "";
            String latitude         = latitudeFld.getText().trim();
            String longitude         = longitudeFld.getText().trim();
            String radius         = radiusFld.getText().trim();
            String notes = notesFld.getText().trim();

            String regDate = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));


            if (type.isEmpty()) {
                alertDialog.showWarning("Disaster Type is required");
                return;
            }
            if (disasterName.isEmpty()) {
                alertDialog.showWarning("Disaster Name is required");
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
                alertDialog.showWarning("Notes is required");
                return;
            }

            boolean success = disasterService.createDisaster(new DisasterModel(type, disasterName, date, latitude, longitude, radius, notes, regDate));

            if (success) {
                javax.swing.JOptionPane.showMessageDialog(
                        null,
                        "Disaster successfully added.",
                        "Success",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                javax.swing.JOptionPane.showMessageDialog(
                        null,
                        "Disaster to add beneficiary.",
                        "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE
                );
            }

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(
                    null,
                    e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
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

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.hide();
        }
    }

    public void onShow() {

    }
}
