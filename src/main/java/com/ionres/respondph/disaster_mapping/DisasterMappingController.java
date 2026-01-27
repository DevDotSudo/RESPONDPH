package com.ionres.respondph.disaster_mapping;

import com.ionres.respondph.common.model.BeneficiaryMarker;
import com.ionres.respondph.common.model.DisasterCircleInfo;
import com.ionres.respondph.common.model.DisasterModel;
import com.ionres.respondph.disaster_mapping.dialogs_controller.BeneficiariesInCircleDialogController;
import com.ionres.respondph.util.AppContext;
import com.ionres.respondph.util.DialogManager;
import com.ionres.respondph.util.GeographicUtils;
import com.ionres.respondph.util.Mapping;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

public class DisasterMappingController {
    private final DisasterMappingService disasterMappingService = AppContext.disasterMappingService;
    private final Mapping mapping = new Mapping();
    private final List<DisasterCircleInfo> disasterCircles = new ArrayList<>();
    private final List<BeneficiaryMarker> beneficiaries = new ArrayList<>();

    @FXML private Pane mapContainer;
    @FXML private ComboBox<DisasterModel> disasterComboBox;
    @FXML private ComboBox<String> disasterTypeComboBox;

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
        
        Platform.runLater(() -> {
            mapping.init(mapContainer);
            mapping.setAfterRedraw(() -> {
                drawBoundary();
                drawDisasterCircles();
                drawBeneficiaries();
            });

            // Add click handler to canvas for circle detection
            mapping.getCanvas().setOnMouseClicked(this::handleMapClick);

            loadDisasterTypes();
            loadBeneficiariesFromDb();
        });
    }

    private void setupDisasterComboBox() {
        // Setup StringConverter for DisasterModel
        disasterComboBox.setConverter(new StringConverter<DisasterModel>() {
            @Override
            public String toString(DisasterModel disaster) {
                if (disaster == null) {
                    return "";
                }
                String name = disaster.getDisasterName();
                return name != null ? name : "";
            }

            @Override
            public DisasterModel fromString(String string) {
                // This is typically not needed for non-editable comboboxes
                return null;
            }
        });

        // Setup cell factory for dropdown items
        disasterComboBox.setCellFactory(cb -> new ListCell<DisasterModel>() {
            @Override
            protected void updateItem(DisasterModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    String name = item.getDisasterName();
                    setText(name != null ? name : "");
                }
            }
        });

        // Setup button cell for selected item display
        disasterComboBox.setButtonCell(new ListCell<DisasterModel>() {
            @Override
            protected void updateItem(DisasterModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    String name = item.getDisasterName();
                    setText(name != null ? name : "");
                }
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
                filterByDisaster(selectedDisaster);
            }
        });
    }

    private void loadDisasterTypes() {
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

        List<DisasterModel> disastersToShow;
        if ("All Types".equals(type)) {
            disastersToShow = disasterMappingService.getDisasters();
        } else {
            disastersToShow = disasterMappingService.getDisastersByType(type);
        }
        
        // Clear and repopulate combo box
        disasterComboBox.getItems().clear();
        if (disastersToShow != null && !disastersToShow.isEmpty()) {
            disasterComboBox.getItems().addAll(disastersToShow);
        }
        
        // Force combo box UI to refresh
        Platform.runLater(() -> {
            disasterComboBox.getSelectionModel().clearSelection();
            // Trigger a layout update
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

        if (disasterId <= 0) {
            mapping.redraw();
            return;
        }

        DisasterModel disaster = disasterMappingService.getDisasterById(disasterId);
        if (disaster == null) {
            mapping.redraw();
            return;
        }

        List<DisasterCircleInfo> circles = disasterMappingService.getDisasterCirclesByDisasterId(disasterId);
        if (circles != null) {
            for (DisasterCircleInfo c : circles) {
                if (!Double.isNaN(c.lat) && !Double.isNaN(c.lon) &&
                    !Double.isNaN(c.radius) && c.radius > 0) {
                    disasterCircles.add(new DisasterCircleInfo(
                            c.lat,
                            c.lon,
                            c.radius,
                            c.disasterName != null ? c.disasterName : "",
                            c.disasterType != null ? c.disasterType : ""
                    ));
                }
            }
        }
        
        // Force redraw to show circles and beneficiaries
        mapping.redraw();
    }

    public void loadBeneficiariesFromDb() {
        beneficiaries.clear();
        List<BeneficiaryMarker> loadedBeneficiaries = disasterMappingService.getBeneficiaries();
        if (loadedBeneficiaries != null && !loadedBeneficiaries.isEmpty()) {
            beneficiaries.addAll(loadedBeneficiaries);
        }
        mapping.redraw();
    }

    private void drawBeneficiaries() {
        if (!mapping.isInitialized() || disasterCircles.isEmpty() || beneficiaries.isEmpty()) {
            return;
        }

        GraphicsContext gc = mapping.getGc();
        double canvasWidth = mapping.getCanvas().getWidth();
        double canvasHeight = mapping.getCanvas().getHeight();
        
        // Viewport bounds with padding for markers near edges
        double padding = 50;
        double minX = -padding;
        double maxX = canvasWidth + padding;
        double minY = -padding;
        double maxY = canvasHeight + padding;

        for (BeneficiaryMarker b : beneficiaries) {
            // Validate beneficiary coordinates
            if (!Mapping.isValidCoordinate(b.lat, b.lon)) {
                continue;
            }

            // Check if beneficiary is inside any disaster circle
            boolean isInsideDisaster = false;
            for (DisasterCircleInfo c : disasterCircles) {
                if (GeographicUtils.isInsideCircle(b.lat, b.lon, c.lat, c.lon, c.radius)) {
                    isInsideDisaster = true;
                    break;
                }
            }

            if (isInsideDisaster) {
                try {
                    Mapping.Point p = mapping.latLonToScreen(b.lat, b.lon);
                    
                    // Skip if point is invalid
                    if (p.x < 0 || p.y < 0) {
                        continue;
                    }

                    if (p.x >= minX && p.x <= maxX && p.y >= minY && p.y <= maxY) {
                        gc.setFill(Color.RED);
                        gc.fillOval(p.x - 5, p.y - 5, 10, 10);

                        gc.setStroke(Color.DARKRED);
                        gc.setLineWidth(1.5);
                        gc.strokeOval(p.x - 5, p.y - 5, 10, 10);
                    }
                } catch (Exception e) {
                    // Skip this beneficiary if coordinate conversion fails
                    java.util.logging.Logger.getLogger(DisasterMappingController.class.getName())
                        .log(java.util.logging.Level.FINE, "Error drawing beneficiary marker", e);
                    continue;
                }
            }
        }
    }

    private void drawDisasterCircles() {
        if (!mapping.isInitialized() || disasterCircles.isEmpty()) {
            return;
        }

        GraphicsContext gc = mapping.getGc();
        double canvasWidth = mapping.getCanvas().getWidth();
        double canvasHeight = mapping.getCanvas().getHeight();
        
        double padding = 2000;
        double minX = -padding;
        double maxX = canvasWidth + padding;
        double minY = -padding;
        double maxY = canvasHeight + padding;

        for (DisasterCircleInfo c : disasterCircles) {
            try {
                if (!Mapping.isValidCoordinate(c.lat, c.lon) ||
                    Double.isNaN(c.radius) || c.radius <= 0) {
                    continue;
                }

                Mapping.Point center = mapping.latLonToScreen(c.lat, c.lon);
                
                if (center.x < 0 || center.y < 0) {
                    continue;
                }

                double px = c.radius / mapping.metersPerPixel(c.lat);
                
                if (center.x < minX || center.x > maxX ||
                    center.y < minY || center.y > maxY) {
                    if (center.x + px < minX || center.x - px > maxX ||
                        center.y + px < minY || center.y - px > maxY) {
                        continue; // Circle is completely outside viewport
                    }
                }

                gc.setFill(Color.rgb(255, 0, 0, 0.25));
                gc.fillOval(center.x - px, center.y - px, px * 2, px * 2);

                gc.setStroke(Color.RED);
                gc.setLineWidth(2);
                gc.strokeOval(center.x - px, center.y - px, px * 2, px * 2);

                if (px > 20) {
                    String type = c.disasterType != null ? c.disasterType : "";
                    String name = c.disasterName != null ? c.disasterName : "";
                    String label = (type + " " + name).trim();
                    
                    if (!label.isEmpty()) {
                        gc.setFont(Font.font("Segoe UI", 10));
                        double labelWidth = textWidth(label, gc);

                        gc.setFill(Color.rgb(255, 255, 255, 0.95));
                        gc.fillRect(center.x - labelWidth / 2 - 4, center.y - 10, labelWidth + 8, 16);

                        gc.setFill(Color.DARKRED);
                        gc.fillText(label, center.x - labelWidth / 2, center.y + 2);
                    }
                }
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(DisasterMappingController.class.getName())
                    .log(java.util.logging.Level.FINE, "Error drawing disaster circle", e);
                continue;
            }
        }
    }

    private void drawBoundary() {
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
        if (disasterCircles.isEmpty()) {
            return;
        }

        double clickX = event.getX();
        double clickY = event.getY();

        Mapping.LatLng clickLatLon = mapping.screenToLatLon(clickX, clickY);

        for (DisasterCircleInfo circle : disasterCircles) {
            if (Double.isNaN(circle.lat) || Double.isNaN(circle.lon) || 
                Double.isNaN(circle.radius) || circle.radius <= 0) {
                continue;
            }

            double distance = GeographicUtils.calculateDistance(
                clickLatLon.lat, clickLatLon.lon,
                circle.lat, circle.lon
            );

            if (!Double.isNaN(distance) && distance <= circle.radius) {
                List<BeneficiaryMarker> beneficiariesInCircle =
                    disasterMappingService.getBeneficiariesInsideCircle(
                        circle.lat, circle.lon, circle.radius
                    );

                showBeneficiariesDialog(circle, beneficiariesInCircle);
                break;
            }
        }
    }


    private void showBeneficiariesDialog(DisasterCircleInfo circle, List<BeneficiaryMarker> beneficiaries) {
        try {
            BeneficiariesInCircleDialogController controller = DialogManager.getController(
                "beneficiariesInCircle", 
                BeneficiariesInCircleDialogController.class
            );
            
            if (controller != null) {
                controller.setData(circle, beneficiaries);
                DialogManager.show("beneficiariesInCircle");
            }
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(DisasterMappingController.class.getName())
                .log(java.util.logging.Level.SEVERE, "Error showing beneficiaries dialog", e);
        }
    }
}