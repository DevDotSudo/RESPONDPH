package com.ionres.respondph.common.controller;

import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.Mapping;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MappingDialogController {

    @FXML VBox mappingDialogRoot;
    @FXML private Pane   mapContainer;
    @FXML private Button mapOkButton;
    @FXML private Button mapCloseButton;

    private Stage            dialogStage;
    private final Mapping    mapping = new Mapping();
    private Mapping.LatLng   selectedLatLng;
    private ControllerListener listener;

    // ── Pending marker coordinates (source of truth for marker position) ──
    private double pendingLat = Double.NaN;
    private double pendingLon = Double.NaN;

    // ── Banate, Iloilo boundary — same coordinates as DashboardController ──
    private static final double[][] BOUNDARY = {
            {11.0775,122.7315},{11.1031,122.7581},{11.0925,122.7618},
            {11.0912,122.7648},{11.0897,122.7662},{11.0896,122.7796},
            {11.0756,122.7942},{11.0674,122.7957},{11.0584,122.7991},
            {11.0533,122.8023},{11.0416,122.8200},{10.9914,122.8514},
            {10.9907,122.8483},{10.9899,122.8462},{10.9904,122.8449},
            {10.9920,122.8447},{10.9951,122.8433},{10.9968,122.8443},
            {10.9966,122.8417},{10.9963,122.8340},{10.9988,122.8287},
            {10.9976,122.8156},{10.9909,122.7957},{10.9919,122.7865},
            {11.0034,122.7861},{11.0480,122.7722},{11.0613,122.7499},
            {11.0681,122.7489},{11.0719,122.7453},{11.0761,122.7454}
    };

    // ── Boundary center / initial zoom (matches DashboardController) ───────
    private static final double BOUNDARY_CENTER_LAT = 11.0470;
    private static final double BOUNDARY_CENTER_LON = 122.7900;
    private static final double BOUNDARY_ZOOM       = 13.0;

    // =========================================================================
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setListener(ControllerListener listener) {
        this.listener = listener;
    }

    // =========================================================================
    public void initialize() {
        Platform.runLater(() -> {
            try {
                mapping.init(mapContainer);

                // ── afterRedraw: draw boundary + recompute marker every frame ──
                mapping.setAfterRedraw(() -> {
                    drawBoundary();
                    if (!Double.isNaN(pendingLat) && !Double.isNaN(pendingLon)) {
                        mapping.markerPosition = mapping.latLonToScreen(pendingLat, pendingLon);
                    }
                });

                // ── Center map on Banate boundary ──────────────────────────────
                mapping.setCenter(BOUNDARY_CENTER_LAT, BOUNDARY_CENTER_LON, BOUNDARY_ZOOM);

                mapping.getCanvas().setOnMouseClicked(e -> {
                    if (!mapping.isDragging() && mapping.isInitialized()) {
                        try {
                            Mapping.LatLng latLng = mapping.screenToLatLon(e.getX(), e.getY());
                            if (Mapping.isValidCoordinate(latLng.lat, latLng.lon)) {
                                selectedLatLng = latLng;
                                pendingLat = latLng.lat;
                                pendingLon = latLng.lon;
                                // Do NOT set markerPosition here — afterRedraw handles it
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
        if (mapCloseButton != null) mapCloseButton.setOnAction(handler);
        if (mapOkButton    != null) mapOkButton.setOnAction(handler);
    }

    public void resetForNewSession() {
        pendingLat = Double.NaN;
        pendingLon = Double.NaN;
        selectedLatLng = null;
        mapping.markerPosition = null;
        mapping.setCenter(BOUNDARY_CENTER_LAT, BOUNDARY_CENTER_LON, BOUNDARY_ZOOM);
    }

    public void preSelectLocation(double lat, double lon) {
        if (!Mapping.isValidCoordinate(lat, lon)) return;

        selectedLatLng = new Mapping.LatLng(lat, lon);
        pendingLat = lat;
        pendingLon = lon;

        if (mapping.isInitialized()) {
            mapping.setCenter(lat, lon, Mapping.MAX_ZOOM);
        } else {
            // Not ready yet — keep polling on the FX thread until initialized
            waitThenCenter(lat, lon);
        }
    }

    private void waitThenCenter(double lat, double lon) {
        Platform.runLater(() -> {
            if (mapping.isInitialized()) {
                mapping.setCenter(lat, lon, Mapping.MAX_ZOOM);
                // afterRedraw will compute markerPosition correctly post-setCenter
            } else {
                waitThenCenter(lat, lon);
            }
        });
    }

    // =========================================================================
    //  Draw Banate boundary — mirrors DashboardController.drawBoundary()
    // =========================================================================
    private void drawBoundary() {
        if (!mapping.isInitialized()) return;
        GraphicsContext gc = mapping.getGc();

        // Outer glow
        gc.setStroke(Color.rgb(120, 0, 0, 0.35));
        gc.setLineWidth(6);
        gc.beginPath();
        boolean first = true;
        for (double[] c : BOUNDARY) {
            Mapping.Point p = mapping.latLonToScreen(c[0], c[1]);
            if (first) { gc.moveTo(p.x, p.y); first = false; } else gc.lineTo(p.x, p.y);
        }
        gc.closePath();
        gc.stroke();

        // Inner crisp line
        gc.setStroke(Color.rgb(255, 50, 50, 0.9));
        gc.setLineWidth(2.5);
        gc.beginPath();
        first = true;
        for (double[] c : BOUNDARY) {
            Mapping.Point p = mapping.latLonToScreen(c[0], c[1]);
            if (first) { gc.moveTo(p.x, p.y); first = false; } else gc.lineTo(p.x, p.y);
        }
        gc.closePath();
        gc.stroke();
    }

    // =========================================================================
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

    public enum MarkerType {
        BENEFICIARY,
        EVAC_SITE,
        DISASTER
    }

    public void setMarkerType(MarkerType type) {
        if (type == MarkerType.EVAC_SITE) {
            mapping.setMarkerImage("/images/location-pin.png");
        } else if (type == MarkerType.BENEFICIARY) {
            mapping.setMarkerImage("/images/placeholder.png");
        }
        else if (type == MarkerType.DISASTER) {
            mapping.setMarkerImage("/images/hazzard-sign.png");
        }
    }
}