package com.ionres.respondph.disaster_damage.dialogs_controller;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class AddDisasterDamageDialogController {

    @FXML
    private VBox root;

    @FXML
    private ComboBox<String> beneficiaryNameFld;

    @FXML
    private ComboBox<String> disasterFld;

    @FXML
    private TextField damageSeverityFld;

    @FXML
    private TextField verifiedByFld;

    @FXML
    private DatePicker birthDatePicker;

    @FXML
    private TextField notesFld;

    @FXML
    private Button saveBtn, exitBtn;

    @FXML
    private void initialize() {
        EventHandler<ActionEvent> handlers = this::handleActions;

        exitBtn.setOnAction(handlers);
        saveBtn.setOnAction(handlers);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == exitBtn) {
            closeDialog();
        } else if (src == saveBtn) {

        }
    }

    private void closeDialog() {
        ((javafx.stage.Stage) exitBtn.getScene().getWindow()).close();
    }

}
