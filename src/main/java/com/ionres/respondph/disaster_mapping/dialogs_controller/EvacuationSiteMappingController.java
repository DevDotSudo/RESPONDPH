package com.ionres.respondph.disaster_mapping.dialogs_controller;

import com.ionres.respondph.common.interfaces.EvacSiteMappingService;
import com.ionres.respondph.common.model.DisasterCircleInfo;
import com.ionres.respondph.common.model.EvacSiteMarker;
import com.ionres.respondph.evac_site.EvacSiteModel;
import com.ionres.respondph.evac_site.EvacSiteService;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.AppContext;
import com.ionres.respondph.util.DashboardRefresher;
import com.ionres.respondph.util.DialogManager;
import com.ionres.respondph.util.Mapping;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EvacuationSiteMappingController {
    private static final Logger LOGGER = Logger.getLogger(EvacuationSiteMappingController.class.getName());
    private static final int INVALID_DISASTER_ID = -1;
    private static final double MIN_ZOOM_FOR_MARKERS = 16.0;

    private final EvacSiteMappingService evacSiteMappingService = AppContext.evacSiteMappingService;
    private final EvacSiteService evacSiteService;
    private final Mapping mapping = new Mapping();
    private final List<EvacSiteMarker> evacSites = new ArrayList<>();
    private Image markerImage;
    private DisasterCircleInfo currentDisaster;
    private int currentDisasterId = INVALID_DISASTER_ID;
    private Stage dialogStage;
    @FXML private Pane mappingContainer;
    @FXML private Label disasterInfoLabel;
    @FXML private Button closeButton;

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

    public EvacuationSiteMappingController() {
        this.evacSiteService = new com.ionres.respondph.evac_site.EvacSiteServiceImpl(DBConnection.getInstance());
    }

    public void initialize() {
        LOGGER.info("=== INITIALIZING EvacuationSiteMappingController ===");

        loadMarkerImage();
        setupButtons();

        DashboardRefresher.registerEvacSiteInMap(this);

        Platform.runLater(() -> {
            mapping.init(mappingContainer);
            mapping.setAfterRedraw(() -> {
                drawBoundary();
                drawEvacSites();
            });

            mapping.getCanvas().setOnMouseClicked(this::handleMapClick);

            loadEvacSitesFromDb();
        });

        LOGGER.info("Controller initialized successfully");
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    private void setupButtons() {
        if (closeButton != null) {
            closeButton.setOnAction(e -> closeDialog());
        }
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.hide();
        }
    }

    public void setDisasterInfo(DisasterCircleInfo disaster) {
        LOGGER.info("=== SETTING DISASTER INFO ===");
        this.currentDisaster = disaster;

        if (disaster != null) {
            String disasterInfo = String.format("Disaster: %s - %s",
                    disaster.disasterType != null ? disaster.disasterType : "Unknown Type",
                    disaster.disasterName != null ? disaster.disasterName : "Unknown Disaster");
            disasterInfoLabel.setText(disasterInfo);
            LOGGER.info("Disaster info set: " + disasterInfo);

            // Redraw map to show evacuation sites
            if (mapping.isInitialized()) {
                mapping.redraw();
            }
        } else {
            LOGGER.warning("Disaster info is null");
        }
    }


    public void setDisasterId(int disasterId) {
        LOGGER.info("=== SETTING DISASTER ID ===");
        LOGGER.info("Disaster ID: " + disasterId);

        if (disasterId <= 0) {
            LOGGER.warning("Invalid disaster ID set: " + disasterId);
        }

        this.currentDisasterId = disasterId;
        LOGGER.info("Disaster ID set to: " + this.currentDisasterId);
    }

    private void loadMarkerImage() {
        try {
            markerImage = new Image(getClass().getResourceAsStream("/images/location-pin.png"));
            if (markerImage.isError()) {
                LOGGER.warning("Failed to load marker image from resources");
                markerImage = null;
            } else {
                LOGGER.info("Marker image loaded successfully");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading marker image", e);
            markerImage = null;
        }
    }

    public void loadEvacSitesFromDb() {
        LOGGER.info("Loading evacuation sites from database...");
        evacSites.clear();
        List<EvacSiteMarker> loadedSites = evacSiteMappingService.getAllEvacSites();
        if (loadedSites != null && !loadedSites.isEmpty()) {
            evacSites.addAll(loadedSites);
            LOGGER.info("Loaded " + evacSites.size() + " evacuation sites");
        } else {
            LOGGER.warning("No evacuation sites loaded from database");
        }
        if (mapping.isInitialized()) {
            mapping.redraw();
        }
    }

    private void drawEvacSites() {
        if (!mapping.isInitialized() || evacSites.isEmpty()) {
            return;
        }

        GraphicsContext gc = mapping.getGc();
        double canvasWidth = mapping.getCanvas().getWidth();
        double canvasHeight = mapping.getCanvas().getHeight();
        double currentZoom = mapping.getZoom();

        double padding = 100;
        double minX = -padding;
        double maxX = canvasWidth + padding;
        double minY = -padding;
        double maxY = canvasHeight + padding;

        for (EvacSiteMarker site : evacSites) {
            if (!Mapping.isValidCoordinate(site.getLat(), site.getLon())) {
                continue;
            }

            try {
                Mapping.Point p = mapping.latLonToScreen(site.getLat(), site.getLon());

                if (p.x >= minX && p.x <= maxX && p.y >= minY && p.y <= maxY) {

                    // Below zoom 16: show only small yellow circles
                    if (currentZoom < MIN_ZOOM_FOR_MARKERS) {
                        gc.setFill(Color.rgb(255, 215, 0)); // Yellow color
                        gc.fillOval(p.x - 4, p.y - 4, 8, 8);
                    }
                    // Zoom 16 and above: show full markers with labels
                    else {
                        if (markerImage != null && !markerImage.isError()) {
                            double w = markerImage.getWidth();
                            double h = markerImage.getHeight();
                            if (w > 0 && h > 0) {
                                gc.drawImage(markerImage, p.x - w/2, p.y - h, w, h);
                            }
                        } else {
                            gc.setFill(Color.rgb(16, 185, 129));
                            gc.fillOval(p.x - 8, p.y - 8, 16, 16);

                            gc.setStroke(Color.WHITE);
                            gc.setLineWidth(2);
                            gc.strokeOval(p.x - 8, p.y - 8, 16, 16);
                        }

                        if (site.getName() != null && !site.getName().isEmpty()) {
                            String label = site.getName();
                            if (site.getCapacity() > 0) {
                                label += " (Cap: " + site.getCapacity() + ")";
                            }

                            gc.setFont(Font.font("Segoe UI", 10));
                            double labelWidth = textWidth(label, gc);

                            gc.setFill(Color.rgb(255, 255, 255, 0.95));
                            gc.fillRect(p.x - labelWidth / 2 - 4, p.y + 5, labelWidth + 8, 16);

                            gc.setFill(Color.rgb(45, 55, 72));
                            gc.fillText(label, p.x - labelWidth / 2, p.y + 17);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Error drawing evacuation site marker for " + site.getName(), e);
            }
        }
    }

    private void drawBoundary() {
        if (!mapping.isInitialized()) {
            return;
        }

        GraphicsContext gc = mapping.getGc();
        gc.setStroke(Color.RED);
        gc.setLineWidth(2);
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
    }

    private double textWidth(String s, GraphicsContext gc) {
        Text t = new Text(s);
        t.setFont(gc.getFont());
        return t.getLayoutBounds().getWidth();
    }

    private void handleMapClick(MouseEvent event) {
        if (event.getClickCount() != 2) {
            return; // Only handle double-clicks
        }

        if (evacSites.isEmpty() || currentDisaster == null) {
            LOGGER.warning("Cannot handle map click - evacSites empty or disaster null");
            return;
        }

        if (currentDisasterId == INVALID_DISASTER_ID || currentDisasterId <= 0) {
            LOGGER.severe("Cannot handle map click - disaster ID not set: " + currentDisasterId);
            return;
        }

        double clickX = event.getX();
        double clickY = event.getY();

        final double CLICK_THRESHOLD_PIXELS = 25.0; // Increased threshold

        for (EvacSiteMarker evacSite : evacSites) {
            if (!Mapping.isValidCoordinate(evacSite.getLat(), evacSite.getLon())) {
                continue;
            }

            Mapping.Point sitePoint = mapping.latLonToScreen(evacSite.getLat(), evacSite.getLon());

            double markerWidth, markerHeight, markerLeft, markerTop;

            if (markerImage != null && !markerImage.isError()) {
                // Image marker dimensions
                markerWidth = markerImage.getWidth();
                markerHeight = markerImage.getHeight();
                markerLeft = sitePoint.x - markerWidth / 2;
                markerTop = sitePoint.y - markerHeight;
            } else {
                // Fallback circle marker dimensions
                markerWidth = 16;
                markerHeight = 16;
                markerLeft = sitePoint.x - 8;
                markerTop = sitePoint.y - 8;
            }

            // Check if click is within the marker bounds (with some padding)
            double padding = 5.0;
            boolean isWithinBounds = clickX >= (markerLeft - padding) &&
                    clickX <= (markerLeft + markerWidth + padding) &&
                    clickY >= (markerTop - padding) &&
                    clickY <= (markerTop + markerHeight + padding);

            if (isWithinBounds) {
                // Found a click on this evacuation site
                LOGGER.info("Evac site clicked: " + evacSite.getName() + " (ID: " + evacSite.getEvacId() + ")");
                showAllocateBeneficiariesDialog(evacSite);
                break;
            }
        }
    }

    /**
     * Show the dialog to allocate beneficiaries to the clicked evacuation site
     */
    private void showAllocateBeneficiariesDialog(EvacSiteMarker evacSiteMarker) {
        LOGGER.info("=== SHOWING ALLOCATE BENEFICIARIES DIALOG ===");
        LOGGER.info("Evac Site ID: " + evacSiteMarker.getEvacId());
        LOGGER.info("Disaster ID to pass: " + currentDisasterId);

        try {
            // Get the full EvacSiteModel with all details
            EvacSiteModel evacSite = evacSiteService.getEvacSiteById(evacSiteMarker.getEvacId());

            if (evacSite == null) {
                LOGGER.warning("Evacuation site not found: " + evacSiteMarker.getEvacId());
                return;
            }

            LOGGER.info("Retrieved evac site model: " + evacSite.getName());

            AllocateBeneficiariesToEvacSiteController controller = DialogManager.getController(
                    "allocateBeneficiariesToEvacSite",
                    AllocateBeneficiariesToEvacSiteController.class
            );

            if (controller != null) {
                LOGGER.info("Got AllocateBeneficiariesToEvacSiteController from DialogManager");
                LOGGER.info("Calling setEvacSiteAndDisaster with disaster ID: " + currentDisasterId);

                controller.setEvacSiteAndDisaster(evacSite, currentDisaster, currentDisasterId);

                LOGGER.info("Data set on controller, showing dialog...");
                DialogManager.show("allocateBeneficiariesToEvacSite");

                LOGGER.info("Dialog shown successfully");
            } else {
                LOGGER.severe("Failed to get AllocateBeneficiariesToEvacSiteController from DialogManager");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error showing allocate beneficiaries dialog for evac site: " +
                    evacSiteMarker.getEvacId(), e);
        }
    }
}