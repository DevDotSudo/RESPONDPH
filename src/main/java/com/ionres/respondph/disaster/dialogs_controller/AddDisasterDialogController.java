package com.ionres.respondph.disaster.dialogs_controller;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

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
    private TextArea notesFld;

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
