package com.ionres.respondph.disaster.dialogs_controller;

import com.ionres.respondph.util.Mapping;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PolygonMappingController {

    // ==================== FXML ====================
    @FXML private VBox root;
    @FXML private Button exitBtn;
    @FXML private Button cancelBtn;
    @FXML private Button confirmBtn;
    @FXML private Button clearAllBtn;
    @FXML private Button zoomInBtn;
    @FXML private Button zoomOutBtn;
    @FXML private Button centerMapBtn;
    @FXML private Button undoLastBtn;
    @FXML private Label pointCounterLabel;
    @FXML private VBox listEmptyState;
    @FXML private ScrollPane pointsScrollPane;
    @FXML private VBox pointsListContainer;
    @FXML private Pane mapContainer;
    @FXML private HBox validationHint;
    @FXML private Label validationHintLabel;

    // ==================== STATE ====================
    private Stage dialogStage;
    private final Mapping mapping = new Mapping();
    private final List<Mapping.LatLng> points = new ArrayList<>();

    /** Called when the user confirms — delivers the finalized point list. */
    private Consumer<List<Mapping.LatLng>> onConfirm;

    private double dragStartX;
    private double dragStartY;
    private boolean wasDragged;

    // ── Banate municipal boundary (matches DashboardController) ──────────────
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

    // ==================== INIT ====================
    @FXML
    private void initialize() {
        // Use a plain circle as the marker (no pin needed for polygon mode)
        mapping.setMarkerImage("/images/placeholder.png");

        // Draw boundary first, then polygon overlay on top
        mapping.setAfterRedraw(() -> {
            drawBoundary();
            drawPolygonOverlay();
        });

        // Wire buttons
        exitBtn.setOnAction(e -> closeDialog());
        cancelBtn.setOnAction(e -> closeDialog());
        confirmBtn.setOnAction(e -> handleConfirm());
        clearAllBtn.setOnAction(e -> clearAll());
        zoomInBtn.setOnAction(e -> zoomIn());
        zoomOutBtn.setOnAction(e -> zoomOut());
        centerMapBtn.setOnAction(e -> recenter());
        undoLastBtn.setOnAction(e -> undoLast());

        // Drag-vs-click detection on the map
        mapContainer.setOnMousePressed(e -> {
            dragStartX = e.getX();
            dragStartY = e.getY();
            wasDragged = false;
        });

        mapContainer.setOnMouseDragged(e -> {
            double dx = Math.abs(e.getX() - dragStartX);
            double dy = Math.abs(e.getY() - dragStartY);
            if (dx > 4 || dy > 4) wasDragged = true;
        });

        mapContainer.setOnMouseReleased(e -> {
            if (!wasDragged) {
                Mapping.LatLng latLng = mapping.screenToLatLon(e.getX(), e.getY());
                addPoint(latLng);
            }
        });

        // Init map after scene is attached
        mapContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    mapping.init(mapContainer);
                    mapping.redraw();
                });
            }
        });

        refreshUI();
    }

    // ==================== POINT MANAGEMENT ====================

    private void addPoint(Mapping.LatLng latLng) {
        points.add(latLng);
        refreshUI();
        mapping.redraw();
        // Scroll list to bottom so latest point is visible
        Platform.runLater(() -> pointsScrollPane.setVvalue(1.0));
    }

    private void removePoint(int index) {
        if (index >= 0 && index < points.size()) {
            points.remove(index);
            refreshUI();
            mapping.redraw();
        }
    }

    private void undoLast() {
        if (!points.isEmpty()) {
            points.remove(points.size() - 1);
            refreshUI();
            mapping.redraw();
        }
    }

    private void clearAll() {
        points.clear();
        refreshUI();
        mapping.redraw();
    }

    // ==================== MAP OVERLAY ====================

    /**
     * Draws the Banate municipal boundary on the canvas, identical to
     * the dashboard's drawBoundary() — outer dark glow + inner red line.
     */
    private void drawBoundary() {
        if (!mapping.isInitialized()) return;
        GraphicsContext gc = mapping.getGc();

        // ── Outer glow pass ──
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

        // ── Sharp inner line ──
        gc.setStroke(Color.rgb(255, 50, 50, 0.90));
        gc.setLineWidth(2.5);
        gc.beginPath();
        first = true;
        for (double[] c : BOUNDARY) {
            Mapping.Point p = mapping.latLonToScreen(c[0], c[1]);
            if (first) { gc.moveTo(p.x, p.y); first = false; } else gc.lineTo(p.x, p.y);
        }
        gc.closePath();
        gc.stroke();

        gc.setLineWidth(1.0); // reset
    }

    /**
     * Draws the polygon polyline and vertex dots directly onto the map canvas.
     * Called by {@link Mapping#setAfterRedraw} after each tile redraw.
     */
    private void drawPolygonOverlay() {
        if (points.isEmpty()) return;

        GraphicsContext gc = mapping.getGc();

        // ── Convert all points to screen coords ──
        double[] sx = new double[points.size()];
        double[] sy = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            Mapping.Point p = mapping.latLonToScreen(points.get(i).lat, points.get(i).lon);
            sx[i] = p.x;
            sy[i] = p.y;
        }

        // ── Draw filled semi-transparent polygon (if 3+ points) ──
        if (points.size() >= 3) {
            gc.setFill(Color.rgb(249, 115, 22, 0.10));
            gc.fillPolygon(sx, sy, points.size());
        }

        // ── Draw polyline edges ──
        gc.setStroke(Color.rgb(249, 115, 22, 0.90));
        gc.setLineWidth(2.0);
        gc.setLineDashes(null); // solid line

        for (int i = 0; i < points.size() - 1; i++) {
            gc.strokeLine(sx[i], sy[i], sx[i + 1], sy[i + 1]);
        }

        // Closing line (dashed) back to first point if 3+ points
        if (points.size() >= 3) {
            gc.setStroke(Color.rgb(249, 115, 22, 0.45));
            gc.setLineDashes(6, 4);
            gc.strokeLine(sx[points.size() - 1], sy[points.size() - 1], sx[0], sy[0]);
            gc.setLineDashes(null);
        }

        // ── Draw vertex circles ──
        for (int i = 0; i < points.size(); i++) {
            boolean isFirst = (i == 0);
            boolean isLast  = (i == points.size() - 1);

            double radius = isFirst ? 7.0 : 5.5;

            // Outer glow
            gc.setFill(Color.rgb(249, 115, 22, 0.25));
            gc.fillOval(sx[i] - radius - 3, sy[i] - radius - 3, (radius + 3) * 2, (radius + 3) * 2);

            // Fill
            if (isFirst) {
                gc.setFill(Color.rgb(59, 130, 246, 1.0)); // blue for first point
            } else if (isLast) {
                gc.setFill(Color.rgb(249, 115, 22, 1.0)); // orange for last point
            } else {
                gc.setFill(Color.rgb(248, 250, 252, 0.95));
            }
            gc.fillOval(sx[i] - radius, sy[i] - radius, radius * 2, radius * 2);

            // Border
            gc.setStroke(Color.rgb(249, 115, 22, 0.90));
            gc.setLineWidth(1.5);
            gc.strokeOval(sx[i] - radius, sy[i] - radius, radius * 2, radius * 2);

            // Index label next to vertex
            gc.setFill(Color.rgb(248, 250, 252, 0.95));
            gc.setFont(javafx.scene.text.Font.font("Inter", javafx.scene.text.FontWeight.BOLD, 10));
            gc.fillText(String.valueOf(i + 1), sx[i] + radius + 4, sy[i] + 4);
        }

        gc.setLineWidth(1.0); // reset
    }

    // ==================== UI REFRESH ====================

    private void refreshUI() {
        int count = points.size();

        // Counter badge
        pointCounterLabel.setText(count + (count == 1 ? " point" : " points"));

        // Empty state vs list
        boolean hasPoints = count > 0;
        listEmptyState.setVisible(!hasPoints);
        listEmptyState.setManaged(!hasPoints);
        pointsScrollPane.setVisible(hasPoints);
        pointsScrollPane.setManaged(hasPoints);

        // Rebuild list rows
        pointsListContainer.getChildren().clear();
        for (int i = 0; i < points.size(); i++) {
            final int idx = i;
            pointsListContainer.getChildren().add(
                    buildPointRow(i + 1, points.get(i).lat, points.get(i).lon, () -> removePoint(idx))
            );
        }

        // Validation hint
        boolean needsMore = count > 0 && count < 3;
        validationHint.setVisible(needsMore);
        validationHint.setManaged(needsMore);
        if (needsMore) {
            int remaining = 3 - count;
            validationHintLabel.setText("Add " + remaining + " more point" + (remaining > 1 ? "s" : "") + " to form a polygon.");
        }

        // Confirm button — only enabled with 3+ points
        confirmBtn.setDisable(count < 3);
    }

    /**
     * Builds a single point row: [badge] [lat/lon info] [spacer] [X button]
     */
    private HBox buildPointRow(int number, double lat, double lon, Runnable onDelete) {
        HBox row = new HBox(10);
        row.getStyleClass().add("point-row");
        row.setAlignment(Pos.CENTER_LEFT);

        // Index badge
        Label badge = new Label(String.valueOf(number));
        badge.getStyleClass().add("point-index-badge");

        // Coordinates block
        VBox coords = new VBox(3);

        HBox latRow = new HBox(5);
        latRow.setAlignment(Pos.CENTER_LEFT);
        Label latKey = new Label("Lat");
        latKey.getStyleClass().add("point-coord-key");
        Label latVal = new Label(String.format("%.6f", lat));
        latVal.getStyleClass().add("point-coord-value");
        latRow.getChildren().addAll(latKey, latVal);

        HBox lonRow = new HBox(5);
        lonRow.setAlignment(Pos.CENTER_LEFT);
        Label lonKey = new Label("Lon");
        lonKey.getStyleClass().add("point-coord-key");
        Label lonVal = new Label(String.format("%.6f", lon));
        lonVal.getStyleClass().add("point-coord-value");
        lonRow.getChildren().addAll(lonKey, lonVal);

        coords.getChildren().addAll(latRow, lonRow);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Delete button
        Button deleteBtn = new Button();
        deleteBtn.getStyleClass().add("point-delete-btn");
        FontAwesomeIconView deleteIcon = new FontAwesomeIconView();
        deleteIcon.setGlyphName("TIMES");
        deleteIcon.setSize("11");
        deleteIcon.getStyleClass().add("point-delete-icon");
        deleteBtn.setGraphic(deleteIcon);
        deleteBtn.setOnAction(e -> onDelete.run());

        row.getChildren().addAll(badge, coords, spacer, deleteBtn);
        return row;
    }

    // ==================== MAP CONTROLS ====================

    private void zoomIn() {
        double newZoom = Math.min(mapping.getZoom() + 0.5, Mapping.MAX_ZOOM);
        mapping.setZoom(newZoom);
    }

    private void zoomOut() {
        double newZoom = Math.max(mapping.getZoom() - 0.5, Mapping.MIN_ZOOM);
        mapping.setZoom(newZoom);
    }

    private void recenter() {
        // Re-center to Banate area default
        mapping.setCenter(11.052390, 122.786762, mapping.getZoom());
    }

    // ==================== CONFIRM / CLOSE ====================

    private void handleConfirm() {
        if (points.size() < 3) {
            validationHint.setVisible(true);
            validationHint.setManaged(true);
            validationHintLabel.setText("At least 3 points are required to form a polygon.");
            return;
        }
        if (onConfirm != null) {
            onConfirm.accept(new ArrayList<>(points));
        }
        closeDialog();
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.hide();
        }
    }

    // ==================== PUBLIC API ====================

    /**
     * Pre-populate with existing points (e.g. when re-opening the dialog to edit).
     */
    public void setExistingPoints(List<Mapping.LatLng> existingPoints) {
        points.clear();
        if (existingPoints != null) {
            points.addAll(existingPoints);
        }
        refreshUI();
        if (mapping.isInitialized()) {
            mapping.redraw();
        }
    }

    /**
     * Register a callback that receives the confirmed list of {@link Mapping.LatLng} points.
     */
    public void setOnConfirm(Consumer<List<Mapping.LatLng>> callback) {
        this.onConfirm = callback;
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public Stage getDialogStage() {
        return dialogStage;
    }

    /** Called each time the dialog is shown — refreshes map and list. */
    public void onShow() {
        refreshUI();
        if (mapping.isInitialized()) {
            mapping.redraw();
        }
    }
}