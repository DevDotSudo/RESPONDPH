package com.ionres.respondph.dashboard;

import com.ionres.respondph.common.model.BeneficiaryMarker;
import com.ionres.respondph.util.AppContext;
import com.ionres.respondph.util.DashboardRefresher;
import com.ionres.respondph.util.Mapping;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class DashboardController {
    private final DashBoardService dashBoardService = AppContext.dashBoardService;
    private final Mapping mapping = new Mapping();
    private final List<BeneficiaryMarker> beneficiaries = new ArrayList<>();
    @FXML private Pane mapContainer;
    @FXML private Label totalBeneficiaryLabel;
    @FXML private Label totalDisastersLabel;
    @FXML private Label totalAidsLabel;
    private Image personMarker;
    private static final double MIN_ZOOM_FOR_MARKERS = 16.0;
    private static final double MARKER_WIDTH = 32;
    private static final double MARKER_HEIGHT = 32;
    private static final double MARKER_OFFSET_Y = MARKER_HEIGHT; // Offset to place the point at the bottom

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

    public void initialize() {
        Platform.runLater(() -> {
            // Load the marker image
            try {
                personMarker = new Image(getClass().getResourceAsStream("/images/person_marker.png"));
                if (personMarker.isError()) {
                    System.err.println("Failed to load marker image: " + personMarker.getException().getMessage());
                    personMarker = null;
                }
            } catch (Exception e) {
                System.err.println("Error loading marker image: " + e.getMessage());
                personMarker = null;
            }

            mapping.init(mapContainer);
            mapping.setAfterRedraw(() -> {
                drawBoundary();
                drawBeneficiaries();
            });
            DashboardRefresher.register(this);
            loadDashBoardData();
            loadBeneficiariesFromDb();
        });
    }

    public void loadBeneficiariesFromDb() {
        beneficiaries.clear();
        beneficiaries.addAll(dashBoardService.getBeneficiaries());
        mapping.redraw();
    }

    private boolean isPointInPolygon(double x, double y, double[][] polygon) {
        boolean inside = false;
        for (int i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
            double xi = polygon[i][1]; // lon
            double yi = polygon[i][0]; // lat
            double xj = polygon[j][1]; // lon
            double yj = polygon[j][0]; // lat

            boolean intersect = ((yi > y) != (yj > y))
                    && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    private void drawBeneficiaries() {
        if (!mapping.isInitialized() || beneficiaries.isEmpty()) {
            return;
        }

        // ────────────────────────────────────────────────
        // Only draw markers when zoomed in enough
        if (mapping.getZoom() < MIN_ZOOM_FOR_MARKERS) {
            return;
        }
        // ────────────────────────────────────────────────

        GraphicsContext gc = mapping.getGc();
        double canvasWidth  = mapping.getCanvas().getWidth();
        double canvasHeight = mapping.getCanvas().getHeight();

        double padding = 50;
        double minX = -padding;
        double maxX = canvasWidth + padding;
        double minY = -padding;
        double maxY = canvasHeight + padding;

        for (BeneficiaryMarker b : beneficiaries) {
            if (!Mapping.isValidCoordinate(b.lat, b.lon)) {
                continue;
            }

            // Optional: still filter by polygon (most apps keep this)
            if (!isPointInPolygon(b.lon, b.lat, boundary)) {
                continue;
            }

            try {
                Mapping.Point p = mapping.latLonToScreen(b.lat, b.lon);

                if (p.x < 0 || p.y < 0) {
                    continue;
                }

                if (p.x >= minX && p.x <= maxX && p.y >= minY && p.y <= maxY) {
                    if (personMarker != null) {
                        double markerX = p.x - (MARKER_WIDTH / 2);
                        double markerY = p.y - MARKER_OFFSET_Y;

                        gc.drawImage(personMarker, markerX, markerY, MARKER_WIDTH, MARKER_HEIGHT);
                    } else {
                        // fallback
                        gc.setFill(Color.RED);
                        gc.fillOval(p.x - 5, p.y - 5, 10, 10);
                        gc.setStroke(Color.DARKRED);
                        gc.setLineWidth(1.5);
                        gc.strokeOval(p.x - 5, p.y - 5, 10, 10);
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
    }

    private void drawBoundary() {
        GraphicsContext gc = mapping.getGc();

        // ===== Shadow Layer (dark red glow) =====
        gc.setStroke(Color.rgb(120, 0, 0, 0.35));
        gc.setLineWidth(6); // thicker shadow
        gc.beginPath();

        boolean first = true;
        for (double[] c : boundary) {
            Mapping.Point p = mapping.latLonToScreen(c[0], c[1]);
            if (first) {
                gc.moveTo(p.x, p.y);
                first = false;
            } else {
                gc.lineTo(p.x, p.y);
            }
        }
        gc.closePath();
        gc.stroke();


        gc.setStroke(Color.rgb(255, 50, 50, 0.9));
        gc.setLineWidth(2.5); // normal line thickness
        gc.beginPath();

        first = true;
        for (double[] c : boundary) {
            Mapping.Point p = mapping.latLonToScreen(c[0], c[1]);
            if (first) {
                gc.moveTo(p.x, p.y);
                first = false;
            } else {
                gc.lineTo(p.x, p.y);
            }
        }
        gc.closePath();
        gc.stroke();
    }

    public void loadDashBoardData() {
        totalBeneficiaryLabel.setText(String.valueOf(dashBoardService.fetchTotalBeneficiary()));
        totalDisastersLabel.setText(String.valueOf(dashBoardService.fetchTotalDisasters()));
        totalAidsLabel.setText(String.valueOf(dashBoardService.fetchTotalAids()));
    }
}