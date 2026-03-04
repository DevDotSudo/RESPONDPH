package com.ionres.respondph.disaster_mapping;

import com.ionres.respondph.common.model.BeneficiaryMarker;
import com.ionres.respondph.common.model.DisasterCircleInfo;
import com.ionres.respondph.common.model.DisasterModel;
import com.ionres.respondph.disaster_mapping.dialogs_controller.BeneficiariesInCircleDialogController;
import com.ionres.respondph.util.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DisasterMappingController {
    private final DisasterMappingService disasterMappingService = AppContext.disasterMappingService;
    private final Mapping mapping = new Mapping();
    private final List<DisasterCircleInfo> disasterCircles = new ArrayList<>();
    private final List<BeneficiaryMarker> beneficiaries = new ArrayList<>();

    @FXML private Pane mapContainer;
    @FXML private ComboBox<DisasterModel> disasterComboBox;
    @FXML private ComboBox<String> disasterTypeComboBox;

    private static final double MIN_ZOOM_FOR_PERSON_MARKER = 16.0;
    private Image personMarker;

    private static final double PERSON_MARKER_W = 28;
    private static final double PERSON_MARKER_H = 28;

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
        setupDisasterComboBox();
        setupComboBoxListeners();

        Refresher.registerBeneficiaryMapInDisasterMapping(this);

        Platform.runLater(() -> {
            mapping.init(mapContainer);

            try {
                personMarker = new Image(
                        Objects.requireNonNull(getClass().getResourceAsStream("/images/person_marker.png"))
                );
            } catch (Exception e) {
                personMarker = null;
                java.util.logging.Logger.getLogger(DisasterMappingController.class.getName())
                        .log(java.util.logging.Level.WARNING,
                                "Missing marker resource: /images/person_marker.png", e);
            }

            mapping.setAfterRedraw(() -> {
                drawBoundary();
                drawDisasterCircles();
                drawBeneficiaries();
            });

            mapping.getCanvas().setOnMouseClicked(this::handleMapClick);

            loadDisasterTypes();
            loadBeneficiariesFromDb();
        });
    }

    private void setupDisasterComboBox() {
        disasterComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(DisasterModel d) {
                if (d == null) return "";
                return d.getDisasterName() != null ? d.getDisasterName() : "";
            }
            @Override public DisasterModel fromString(String s) { return null; }
        });

        disasterComboBox.setCellFactory(cb -> new ListCell<>() {
            @Override protected void updateItem(DisasterModel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" :
                        item.getDisasterName() != null ? item.getDisasterName() : "");
            }
        });

        disasterComboBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(DisasterModel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" :
                        item.getDisasterName() != null ? item.getDisasterName() : "");
            }
        });
    }

    private void setupComboBoxListeners() {
        disasterTypeComboBox.setOnAction(event -> {
            String selectedType = disasterTypeComboBox.getValue();
            if (selectedType != null && !selectedType.isEmpty()) {
                filterByDisasterType(selectedType);
            }
        });

        disasterComboBox.setOnAction(event -> {
            DisasterModel selectedDisaster = disasterComboBox.getValue();
            if (selectedDisaster != null) {
                AppContext.currentDisasterId = selectedDisaster.getDisasterId();
                filterByDisaster(selectedDisaster);
            }
        });
    }

    public void loadDisasterTypes() {
        List<String> types = disasterMappingService.getDisasterTypes();
        disasterTypeComboBox.getItems().clear();
        disasterTypeComboBox.getItems().add("All Types");
        if (types != null && !types.isEmpty()) {
            disasterTypeComboBox.getItems().addAll(types);
        }
    }

    private void filterByDisasterType(String type) {
        disasterComboBox.setValue(null);
        disasterCircles.clear();

        List<DisasterModel> disastersToShow = "All Types".equals(type)
                ? disasterMappingService.getDisasters()
                : disasterMappingService.getDisastersByType(type);

        disasterComboBox.getItems().clear();
        if (disastersToShow != null && !disastersToShow.isEmpty()) {
            disasterComboBox.getItems().addAll(disastersToShow);
        }

        Platform.runLater(() -> {
            disasterComboBox.getSelectionModel().clearSelection();
            disasterComboBox.requestLayout();
        });

        mapping.redraw();
    }

    private void filterByDisaster(DisasterModel disaster) {
        if (disaster != null && disaster.getDisasterId() > 0) {
            loadDisasterCirclesFromDb(disaster.getDisasterId());
        }
    }

    private void loadDisasterCirclesFromDb(int disasterId) {
        disasterCircles.clear();

        if (disasterId <= 0) { mapping.redraw(); return; }

        DisasterModel disaster = disasterMappingService.getDisasterById(disasterId);
        if (disaster == null) { mapping.redraw(); return; }

        List<DisasterCircleInfo> circles = disasterMappingService.getDisasterCirclesByDisasterId(disasterId);
        if (circles != null) {
            for (DisasterCircleInfo c : circles) {
                if (!Double.isNaN(c.lat) && !Double.isNaN(c.lon) &&
                        !Double.isNaN(c.radius) && c.radius > 0) {
                    disasterCircles.add(new DisasterCircleInfo(
                            c.lat, c.lon, c.radius,
                            c.disasterName != null ? c.disasterName : "",
                            c.disasterType != null ? c.disasterType : ""
                    ));
                }
            }
        }

        mapping.redraw();
    }

    public void loadBeneficiariesFromDb() {
        beneficiaries.clear();
        List<BeneficiaryMarker> loaded = disasterMappingService.getBeneficiaries();
        if (loaded != null && !loaded.isEmpty()) {
            beneficiaries.addAll(loaded);
        }
        mapping.redraw();
    }

    // ═══════════════════════════════════════════════════════════════════
    // DRAW DISASTER CIRCLES
    // ─────────────────────────────────────────────────────────────────
    // FIX: Removed the broken `center.x < 0 || center.y < 0` guard that
    // caused circles to disappear whenever the user panned so that the
    // circle center moved off the top-left of the canvas.
    //
    // The correct culling test is AABB (Axis-Aligned Bounding Box):
    //   skip only when the circle's bounding square does not intersect
    //   the canvas rectangle at all.  Because offsetX/Y can be negative
    //   (pan right/down), center.x or center.y can legitimately be very
    //   negative (center off left/top) while the circle still overlaps
    //   the visible area on the right/bottom.
    // ═══════════════════════════════════════════════════════════════════
    private void drawDisasterCircles() {
        if (!mapping.isInitialized() || disasterCircles.isEmpty()) return;

        GraphicsContext gc   = mapping.getGc();
        double canvasW       = mapping.getCanvas().getWidth();
        double canvasH       = mapping.getCanvas().getHeight();

        for (DisasterCircleInfo c : disasterCircles) {
            try {
                if (!Mapping.isValidCoordinate(c.lat, c.lon) ||
                        Double.isNaN(c.radius) || c.radius <= 0) continue;

                Mapping.Point center = mapping.latLonToScreen(c.lat, c.lon);
                double px = c.radius / mapping.metersPerPixel(c.lat);

                // ── AABB visibility cull ──────────────────────────────────────
                // Skip only when the circle's bounding box is entirely outside
                // the canvas. center.x/y may be negative (left/top of canvas)
                // while the circle still overlaps the visible area.
                if (center.x + px < 0 || center.x - px > canvasW ||
                        center.y + px < 0 || center.y - px > canvasH) {
                    continue;
                }

                // ── Draw filled circle ────────────────────────────────────────
                gc.setFill(Color.rgb(255, 0, 0, 0.25));
                gc.fillOval(center.x - px, center.y - px, px * 2, px * 2);

                // ── Draw border ───────────────────────────────────────────────
                gc.setStroke(Color.RED);
                gc.setLineWidth(2);
                gc.strokeOval(center.x - px, center.y - px, px * 2, px * 2);

                // ── Label (only when circle is large enough to read) ──────────
                if (px > 20) {
                    String type  = c.disasterType  != null ? c.disasterType  : "";
                    String name  = c.disasterName  != null ? c.disasterName  : "";
                    String label = (type + " " + name).trim();

                    if (!label.isEmpty()) {
                        gc.setFont(Font.font("Segoe UI", 10));
                        double labelW = textWidth(label, gc);

                        gc.setFill(Color.rgb(255, 255, 255, 0.95));
                        gc.fillRect(center.x - labelW / 2 - 4, center.y - 10, labelW + 8, 16);

                        gc.setFill(Color.DARKRED);
                        gc.fillText(label, center.x - labelW / 2, center.y + 2);
                    }
                }

            } catch (Exception e) {
                java.util.logging.Logger.getLogger(DisasterMappingController.class.getName())
                        .log(java.util.logging.Level.FINE, "Error drawing disaster circle", e);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // DRAW BENEFICIARIES
    // ─────────────────────────────────────────────────────────────────
    // FIX: Same AABB fix applied here — the old check used a fixed 50px
    // padding which was too small for person markers near the edge and
    // would skip markers whose position calculation returned a slightly
    // negative screen coordinate even though they were still visible.
    // ═══════════════════════════════════════════════════════════════════
    private void drawBeneficiaries() {
        if (!mapping.isInitialized() || disasterCircles.isEmpty() || beneficiaries.isEmpty()) return;

        GraphicsContext gc  = mapping.getGc();
        double canvasW      = mapping.getCanvas().getWidth();
        double canvasH      = mapping.getCanvas().getHeight();

        // Expand clip by half marker size so we never clip a visible marker
        double halfW = PERSON_MARKER_W / 2.0;
        double halfH = PERSON_MARKER_H / 2.0;

        double zoom = mapping.getZoom();
        boolean usePersonMarker = zoom >= MIN_ZOOM_FOR_PERSON_MARKER && personMarker != null;

        Color dotFill   = Color.rgb(0, 120, 255, 0.85);
        Color dotStroke = Color.rgb(0,  70, 180, 0.95);

        for (BeneficiaryMarker b : beneficiaries) {
            if (!Mapping.isValidCoordinate(b.lat, b.lon)) continue;

            // Only draw beneficiaries that fall inside at least one disaster circle
            boolean isInsideDisaster = false;
            for (DisasterCircleInfo c : disasterCircles) {
                if (GeographicUtils.isInsideCircle(b.lat, b.lon, c.lat, c.lon, c.radius)) {
                    isInsideDisaster = true;
                    break;
                }
            }
            if (!isInsideDisaster) continue;

            try {
                Mapping.Point p = mapping.latLonToScreen(b.lat, b.lon);

                // AABB cull — allow marker dimensions as margin
                if (p.x + halfW < 0 || p.x - halfW > canvasW ||
                        p.y + halfH < 0 || p.y - halfH > canvasH) continue;

                if (usePersonMarker) {
                    gc.drawImage(personMarker,
                            p.x - halfW, p.y - halfH,
                            PERSON_MARKER_W, PERSON_MARKER_H);
                } else {
                    gc.setFill(dotFill);
                    gc.fillOval(p.x - 5, p.y - 5, 10, 10);
                    gc.setStroke(dotStroke);
                    gc.setLineWidth(1.5);
                    gc.strokeOval(p.x - 5, p.y - 5, 10, 10);
                }
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(DisasterMappingController.class.getName())
                        .log(java.util.logging.Level.FINE, "Error drawing beneficiary marker", e);
            }
        }
    }

    private void drawBoundary() {
        GraphicsContext gc = mapping.getGc();

        gc.setStroke(Color.rgb(120, 0, 0, 0.35));
        gc.setLineWidth(6);
        gc.beginPath();
        boolean first = true;
        for (double[] coord : boundary) {
            Mapping.Point p = mapping.latLonToScreen(coord[0], coord[1]);
            if (first) { gc.moveTo(p.x, p.y); first = false; }
            else gc.lineTo(p.x, p.y);
        }
        gc.closePath();
        gc.stroke();

        gc.setStroke(Color.rgb(255, 50, 50, 0.9));
        gc.setLineWidth(2.5);
        gc.beginPath();
        first = true;
        for (double[] coord : boundary) {
            Mapping.Point p = mapping.latLonToScreen(coord[0], coord[1]);
            if (first) { gc.moveTo(p.x, p.y); first = false; }
            else gc.lineTo(p.x, p.y);
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
        if (event.getClickCount() != 2 || disasterCircles.isEmpty()) return;

        Mapping.LatLng clickLatLon = mapping.screenToLatLon(event.getX(), event.getY());

        for (DisasterCircleInfo circle : disasterCircles) {
            if (Double.isNaN(circle.lat) || Double.isNaN(circle.lon) ||
                    Double.isNaN(circle.radius) || circle.radius <= 0) continue;

            double distance = GeographicUtils.calculateDistance(
                    clickLatLon.lat, clickLatLon.lon, circle.lat, circle.lon);

            if (!Double.isNaN(distance) && distance <= circle.radius) {
                List<BeneficiaryMarker> inCircle =
                        disasterMappingService.getBeneficiariesInsideCircle(
                                circle.lat, circle.lon, circle.radius);
                showBeneficiariesDialog(circle, inCircle);
                break;
            }
        }
    }

    private void showBeneficiariesDialog(DisasterCircleInfo circle, List<BeneficiaryMarker> beneficiaries) {
        try {
            BeneficiariesInCircleDialogController controller = DialogManager.getController(
                    "beneficiariesInCircle", BeneficiariesInCircleDialogController.class);

            if (controller != null) {
                int disasterId = (disasterComboBox.getValue() != null)
                        ? disasterComboBox.getValue().getDisasterId()
                        : AppContext.currentDisasterId;

                controller.setData(circle, beneficiaries, disasterId);
                DialogManager.show("beneficiariesInCircle");
            }
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(DisasterMappingController.class.getName())
                    .log(java.util.logging.Level.SEVERE, "Error showing beneficiaries dialog", e);
        }
    }

    public void reloadCircleIfSelected(int updatedDisasterId) {
        DisasterModel selected = disasterComboBox.getValue();
        if (selected != null && selected.getDisasterId() == updatedDisasterId) {
            loadDisasterCirclesFromDb(updatedDisasterId);
        }
    }
}