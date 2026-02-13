package com.ionres.respondph.dashboard;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.common.model.BeneficiaryMarker;
import com.ionres.respondph.common.model.EvacSiteMarker;
import com.ionres.respondph.util.AppContext;
import com.ionres.respondph.util.DashboardRefresher;
import com.ionres.respondph.util.Mapping;
import com.ionres.respondph.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    @FXML private Label currentDateLabel;
    @FXML private Label currentTimeLabel;
    @FXML private Label totalEvacutaionSiteLabel;
    @FXML private Label adminNameLabel;
    private Image personMarker;
    private static final double MIN_ZOOM_FOR_MARKERS = 16.0;
    private static final double MARKER_WIDTH = 32;
    private static final double MARKER_HEIGHT = 32;
    private static final double MARKER_OFFSET_Y = MARKER_HEIGHT;
    private final List<EvacSiteMarker> evacSites = new ArrayList<>();
    private Image evacSiteMarker;
    private static final double EVAC_MARKER_WIDTH = 32;
    private static final double EVAC_MARKER_HEIGHT = 32;
    private static final double EVAC_MARKER_OFFSET_Y = EVAC_MARKER_HEIGHT;

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
        AdminModel admin1 = SessionManager.getInstance().getCurrentAdmin();
        System.out.println("=== DashboardController.initialize() ===");
        System.out.println("Admin from session: " + admin1);
        if (admin1 != null) {
            System.out.println("ID: " + admin1.getId());
            System.out.println("Username: " + admin1.getUsername());
            System.out.println("Firstname: " + admin1.getFirstname());
            System.out.println("Lastname: " + admin1.getLastname());
        }

        Platform.runLater(() -> {
            try {
                personMarker = new Image(getClass().getResourceAsStream("/images/person_marker.png"));
                if (personMarker.isError()) {
                    System.err.println("Failed to load marker image: " + personMarker.getException().getMessage());
                    personMarker = null;
                }
                // Load evacuation site marker
                evacSiteMarker = new Image(getClass().getResourceAsStream("/images/location-pin.png"));
                if (evacSiteMarker.isError()) {
                    System.err.println("Failed to load evac site marker image: " + evacSiteMarker.getException().getMessage());
                    evacSiteMarker = null;
                }
            } catch (Exception e) {
                System.err.println("Error loading marker image: " + e.getMessage());
                personMarker = null;
            }

            mapping.init(mapContainer);
            mapping.setAfterRedraw(() -> {
                drawBoundary();
                drawEvacSites();
                drawBeneficiaries();
            });
            DashboardRefresher.register(this);
            loadDashBoardData();
            loadBeneficiariesFromDb();
            loadEvacSitesFromDb();

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a");

            Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
                LocalDateTime now = LocalDateTime.now();
                currentTimeLabel.setText(now.format(timeFormatter));
                currentDateLabel.setText(now.format(dateFormatter));
            }), new KeyFrame(Duration.seconds(1)));

            clock.setCycleCount(Timeline.INDEFINITE);
            clock.play();

            AdminModel currentAdmin = SessionManager.getInstance().getCurrentAdmin();
            System.out.println("Admin inside runLater: " + currentAdmin);

            SessionManager.getInstance().setOnSessionChanged(() -> {
                AdminModel admin = SessionManager.getInstance().getCurrentAdmin();
                if (admin != null) {
                    String display = (admin.getFirstname() != null && !admin.getFirstname().isEmpty())
                            ? admin.getFirstname() + " " + admin.getLastname()
                            : admin.getUsername();
                    adminNameLabel.setText("Loggen in : " + display);
                }
            });
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
        if (!mapping.isInitialized() || beneficiaries.isEmpty()) return;

        GraphicsContext gc = mapping.getGc();
        double canvasWidth  = mapping.getCanvas().getWidth();
        double canvasHeight = mapping.getCanvas().getHeight();

        double padding = 50;
        double minX = -padding, maxX = canvasWidth + padding;
        double minY = -padding, maxY = canvasHeight + padding;

        boolean useMarkerImage = mapping.getZoom() >= MIN_ZOOM_FOR_MARKERS;

        double dotRadius = 4; // adjust if you want bigger/smaller
        Color dotFill   = Color.rgb(0, 120, 255, 0.85);   // bright blue fill
        Color dotStroke = Color.rgb(0, 70, 180, 0.95);    // deeper blue stroke

        for (BeneficiaryMarker b : beneficiaries) {
            if (!Mapping.isValidCoordinate(b.lat, b.lon)) continue;

            // keep polygon filtering
            if (!isPointInPolygon(b.lon, b.lat, boundary)) continue;

            try {
                Mapping.Point p = mapping.latLonToScreen(b.lat, b.lon);

                if (p.x < 0 || p.y < 0) continue;

                if (p.x < minX || p.x > maxX || p.y < minY || p.y > maxY) continue;

                if (useMarkerImage && personMarker != null) {
                    double markerX = p.x - (MARKER_WIDTH / 2);
                    double markerY = p.y - MARKER_OFFSET_Y;
                    gc.drawImage(personMarker, markerX, markerY, MARKER_WIDTH, MARKER_HEIGHT);

                } else if (useMarkerImage) {
                    gc.setFill(Color.RED);
                    gc.fillOval(p.x - 5, p.y - 5, 10, 10);
                    gc.setStroke(Color.DARKRED);
                    gc.setLineWidth(1.5);
                    gc.strokeOval(p.x - 5, p.y - 5, 10, 10);

                } else {
                    gc.setFill(dotFill);
                    gc.fillOval(p.x - dotRadius, p.y - dotRadius, dotRadius * 2, dotRadius * 2);

                    gc.setStroke(dotStroke);
                    gc.setLineWidth(1.2);
                    gc.strokeOval(p.x - dotRadius, p.y - dotRadius, dotRadius * 2, dotRadius * 2);
                }

            } catch (Exception ignored) {
                // keep rendering others even if one fails
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

    private void drawEvacSites() {
        if (!mapping.isInitialized() || evacSites.isEmpty()) return;

        GraphicsContext gc = mapping.getGc();
        double canvasWidth = mapping.getCanvas().getWidth();
        double canvasHeight = mapping.getCanvas().getHeight();

        double padding = 60;
        double minX = -padding, maxX = canvasWidth + padding;
        double minY = -padding, maxY = canvasHeight + padding;

        boolean useMarkerImage = mapping.getZoom() >= MIN_ZOOM_FOR_MARKERS;

        double dotRadius = 4; // same size as beneficiaries
        Color dotFill = Color.rgb(234, 179, 8, 0.85);   // bright green fill
        Color dotStroke = Color.rgb(234, 179, 8, 0.85);

        for (EvacSiteMarker site : evacSites) {
            if (!Mapping.isValidCoordinate(site.lat, site.lon)) continue;

            if (!isPointInPolygon(site.lon, site.lat, boundary)) continue;

            try {
                Mapping.Point p = mapping.latLonToScreen(site.lat, site.lon);

                if (p.x < 0 || p.y < 0) continue;

                if (p.x < minX || p.x > maxX || p.y < minY || p.y > maxY) continue;

                if (useMarkerImage && evacSiteMarker != null) {
                    double markerX = p.x - (EVAC_MARKER_WIDTH / 2);
                    double markerY = p.y - EVAC_MARKER_OFFSET_Y;
                    gc.drawImage(evacSiteMarker, markerX, markerY, EVAC_MARKER_WIDTH, EVAC_MARKER_HEIGHT);

                } else if (useMarkerImage) {
                    // Fallback: use GREEN color instead of red
                    gc.setFill(Color.rgb(234, 179, 8, 0.85));
                    gc.fillOval(p.x - 5, p.y - 5, 10, 10);
                    gc.setStroke(Color.rgb(234, 179, 8, 0.85));
                    gc.setLineWidth(1.5);
                    gc.strokeOval(p.x - 5, p.y - 5, 10, 10);

                } else {
                    gc.setFill(dotFill);
                    gc.fillOval(p.x - dotRadius, p.y - dotRadius, dotRadius * 2, dotRadius * 2);

                    gc.setStroke(dotStroke);
                    gc.setLineWidth(1.2);
                    gc.strokeOval(p.x - dotRadius, p.y - dotRadius, dotRadius * 2, dotRadius * 2);
                }

            } catch (Exception ignored) {
                // keep rendering others even if one fails
            }
        }
    }
    public void loadEvacSitesFromDb() {
        evacSites.clear();
        evacSites.addAll(dashBoardService.getEvacSites());
        mapping.redraw();
    }

    public void loadDashBoardData() {
        totalBeneficiaryLabel.setText(String.valueOf(dashBoardService.fetchTotalBeneficiary()));
        totalDisastersLabel.setText(String.valueOf(dashBoardService.fetchTotalDisasters()));
        totalAidsLabel.setText(String.valueOf(dashBoardService.fetchTotalAids()));
        totalEvacutaionSiteLabel.setText(String.valueOf(dashBoardService.fetchTotalEvacuationSites()));
        loadEvacSitesFromDb();
    }
}