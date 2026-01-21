package com.ionres.respondph.common.controller;

import com.ionres.respondph.beneficiary.dialogs_controller.AddBeneficiariesDialogController;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.Mapping;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class MappingDialogController {
    @FXML private Pane mapContainer;
    @FXML private Button mapOkButton;
    @FXML private Button mapCloseButton;
    private AddBeneficiariesDialogController controller;
    private Stage dialogStage;
    private final Mapping mapping = new Mapping();
    private Mapping.LatLng selectedLatLng;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    public void setController(AddBeneficiariesDialogController controller) {
        this.controller = controller;
    }
    public void initialize() {
        Platform.runLater(() -> {
            mapping.init(mapContainer);
            mapping.getCanvas().setOnMouseClicked(e -> {
                selectedLatLng = mapping.screenToLatLon(e.getX(), e.getY());
                mapping.redraw();
            });
        });

        EventHandler<ActionEvent> handler = this::actionButtons;
        mapCloseButton.setOnAction(handler);
        mapOkButton.setOnAction(handler);
    }

    private void actionButtons(ActionEvent event) {
        Object src = event.getSource();

        if(src ==  mapCloseButton) {
            dialogStage.hide();
        }
        else if (src == mapOkButton) {
            handleOk();
        }
    }

    private void handleOk() {
        if (selectedLatLng == null) {
            AlertDialogManager.showError(
                    "Select Location",
                    "Please select a location on the map."
            );
            return;
        }
        controller.latitudeFld.setText(Double.toString(selectedLatLng.lat));
        controller.longitudeFld.setText(Double.toString(selectedLatLng.lon));
        dialogStage.hide();
    }
}
