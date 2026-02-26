package com.ionres.respondph.evac_site.dialogs_controller;

import com.ionres.respondph.common.interfaces.EvacSiteMappingService;
import com.ionres.respondph.common.model.EvacSiteMarker;
import com.ionres.respondph.evac_site.EvacSiteModel;
import com.ionres.respondph.util.AppContext;
import com.ionres.respondph.util.Mapping;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class EvacSiteMapViewController {

    private static final Logger LOGGER = Logger.getLogger(EvacSiteMapViewController.class.getName());
    private static final double MIN_ZOOM_FOR_MARKERS = 16.0;

    @FXML private Pane mappingContainer;
    @FXML private Label siteNameLabel;
    @FXML private Label capacityLabel;
    @FXML private Label coordsLabel;
    @FXML private Label notesLabel;
    @FXML private Button closeBtn;

    private Stage dialogStage;
    private final Mapping mapping = new Mapping();
    private Image markerImage;
    private EvacSiteModel currentSite;

    // Single-site marker (reuse EvacSiteMarker since it already holds what we need)
    private EvacSiteMarker siteMarker;

    private final double[][] boundary = {
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

    @FXML
    private void initialize() {
        loadMarkerImage();
        closeBtn.setOnAction(e -> closeDialog());
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setEvacSite(EvacSiteModel site) {
        if (site == null) return;

        this.currentSite = site;

        try {
            double lat = Double.parseDouble(site.getLat());
            double lon = Double.parseDouble(site.getLongi());
            int    cap = Integer.parseInt(site.getCapacity());

            // Populate info-card labels
            siteNameLabel.setText(site.getName());
            capacityLabel.setText("Capacity: " + cap + " persons");
            coordsLabel.setText(String.format("Lat: %.6f  Lon: %.6f", lat, lon));
            notesLabel.setText((site.getNotes() != null && !site.getNotes().isBlank())
                    ? site.getNotes() : "—");

            siteMarker = new EvacSiteMarker(
                    site.getEvacId(), lat, lon, site.getName(), cap
            );

            Platform.runLater(() -> {
                mapping.init(mappingContainer);
                mapping.setAfterRedraw(() -> {
                    drawBoundary();
                    drawSiteMarker();
                });
                Platform.runLater(() -> {
                    mapping.setCenter(lat, lon, 17.0);
                });
            });

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid coordinates/capacity for evac site", e);
        }
    }


    private void drawSiteMarker() {
        if (!mapping.isInitialized() || siteMarker == null) return;
        if (!Mapping.isValidCoordinate(siteMarker.getLat(), siteMarker.getLon())) return;

        GraphicsContext gc  = mapping.getGc();
        double zoom         = mapping.getZoom();
        Mapping.Point p     = mapping.latLonToScreen(siteMarker.getLat(), siteMarker.getLon());

        double cw = mapping.getCanvas().getWidth();
        double ch = mapping.getCanvas().getHeight();
        if (p.x < -100 || p.x > cw + 100 || p.y < -100 || p.y > ch + 100) return;

        if (zoom < MIN_ZOOM_FOR_MARKERS) {
            // Small dot at low zoom
            gc.setFill(Color.rgb(255, 215, 0));
            gc.fillOval(p.x - 6, p.y - 6, 12, 12);
        } else {
            // Full marker icon or fallback circle
            if (markerImage != null && !markerImage.isError()) {
                double w = markerImage.getWidth();
                double h = markerImage.getHeight();
                gc.drawImage(markerImage, p.x - w / 2, p.y - h, w, h);
            } else {
                // Fallback: green circle with white border
                gc.setFill(Color.rgb(16, 185, 129));
                gc.fillOval(p.x - 10, p.y - 10, 20, 20);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(2.5);
                gc.strokeOval(p.x - 10, p.y - 10, 20, 20);
            }

            // Name + capacity label beneath the marker
            String label = siteMarker.getName();
            if (siteMarker.getCapacity() > 0) {
                label += "  |  Cap: " + siteMarker.getCapacity();
            }

            gc.setFont(Font.font("Segoe UI", 11));
            double lw = textWidth(label, gc);

            // Label background
            gc.setFill(Color.rgb(255, 255, 255, 0.92));
            gc.fillRoundRect(p.x - lw / 2 - 6, p.y + 6, lw + 12, 18, 6, 6);

            // Label text
            gc.setFill(Color.rgb(30, 41, 59));
            gc.fillText(label, p.x - lw / 2, p.y + 18);
        }
    }

    private void drawBoundary() {
        if (!mapping.isInitialized()) return;

        GraphicsContext gc = mapping.getGc();
        gc.setStroke(Color.RED);
        gc.setLineWidth(2);
        gc.beginPath();

        boolean first = true;
        for (double[] c : boundary) {
            Mapping.Point p = mapping.latLonToScreen(c[0], c[1]);
            if (first) { gc.moveTo(p.x, p.y); first = false; }
            else        { gc.lineTo(p.x, p.y); }
        }
        gc.closePath();
        gc.stroke();
    }

    private double textWidth(String s, GraphicsContext gc) {
        Text t = new Text(s);
        t.setFont(gc.getFont());
        return t.getLayoutBounds().getWidth();
    }

    // ----------------------------------------------------------------
    // Marker image
    // ----------------------------------------------------------------
    private void loadMarkerImage() {
        try {
            markerImage = new Image(
                    getClass().getResourceAsStream("/images/location-pin.png"));
            if (markerImage.isError()) markerImage = null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load marker image", e);
            markerImage = null;
        }
    }

    private void closeDialog() {
        if (dialogStage != null) dialogStage.hide();
    }
}