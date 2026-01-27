package com.ionres.respondph.common.controller;

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

    private Stage dialogStage;
    private final Mapping mapping = new Mapping();
    private Mapping.LatLng selectedLatLng;
    private ControllerListener listener;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setListener(ControllerListener listener) {
        this.listener = listener;
    }

    /**
     * Initializes the mapping dialog.
     * Sets up the map and event handlers.
     */
    public void initialize() {
        Platform.runLater(() -> {
            try {
                mapping.init(mapContainer);
                
                // Wait for mapping to be fully initialized
                mapping.getCanvas().setOnMouseClicked(e -> {
                    if (!mapping.isDragging() && mapping.isInitialized()) {
                        try {
                            Mapping.LatLng latLng = mapping.screenToLatLon(e.getX(), e.getY());
                            
                            // Validate coordinates
                            if (Mapping.isValidCoordinate(latLng.lat, latLng.lon)) {
                                selectedLatLng = latLng;
                                Mapping.Point screenPoint = mapping.latLonToScreen(latLng.lat, latLng.lon);
                                mapping.markerPosition = screenPoint;
                                mapping.redraw();
                            }
                        } catch (Exception ex) {
                            java.util.logging.Logger.getLogger(MappingDialogController.class.getName())
                                .log(java.util.logging.Level.WARNING, "Error handling map click", ex);
                        }
                    }
                });
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(MappingDialogController.class.getName())
                    .log(java.util.logging.Level.SEVERE, "Failed to initialize mapping dialog", e);
            }
        });

        EventHandler<ActionEvent> handler = this::actionButtons;
        if (mapCloseButton != null) {
            mapCloseButton.setOnAction(handler);
        }
        if (mapOkButton != null) {
            mapOkButton.setOnAction(handler);
        }
    }

    private void actionButtons(ActionEvent event) {
        if (event.getSource() == mapCloseButton) {
            dialogStage.hide();
            return;
        }

        if (event.getSource() == mapOkButton) {
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

        if (listener != null) {
            listener.onLocationSelected(selectedLatLng);
        }
        dialogStage.hide();
    }
}
