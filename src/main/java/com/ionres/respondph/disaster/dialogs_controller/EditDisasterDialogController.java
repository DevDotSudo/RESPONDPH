package com.ionres.respondph.disaster.dialogs_controller;

import com.ionres.respondph.common.controller.MappingDialogController;
import com.ionres.respondph.disaster.DisasterController;
import com.ionres.respondph.disaster.DisasterModel;
import com.ionres.respondph.disaster.DisasterService;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.Mapping;
import com.ionres.respondph.util.Refresher;
import com.ionres.respondph.util.DialogManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class EditDisasterDialogController {

    // ==================== FXML FIELDS ====================
    @FXML private VBox root;
    @FXML private Button exitBtn, updateBtn;
    @FXML private ComboBox<String> disasterType;
    @FXML private TextField disasterNameFld;
    @FXML private DatePicker datePicker;
    @FXML public TextField latitudeFld;
    @FXML public TextField longitudeFld;
    @FXML private TextField radiusFld;
    @FXML private TextArea notesFld;
    @FXML private CheckBox banateAreaCheckBox;
    @FXML Button getLocationBtn;

    @FXML private TabPane locationTabPane;
    @FXML private Tab circleTab;
    @FXML private Tab polygonTab;

    @FXML private Button selectOnMapBtn;
    @FXML private Label polygonPointCountLabel;
    @FXML private VBox polygonEmptyState;
    @FXML private ScrollPane polygonScrollPane;
    @FXML private VBox polygonPointsContainer;

    // ==================== STATE ====================
    private Stage dialogStage;
    private DisasterService disasterService;
    private DisasterController disasterController;
    private DisasterModel currentDisaster;
    private double yOffset;
    private double xOffset;

    private final List<double[]> polygonPoints = new ArrayList<>();

    // ==================== INIT ====================
    @FXML
    private void initialize() {
        setupKeyHandlers();
        makeDraggable();

        updateBtn.setOnAction(this::handleActions);
        exitBtn.setOnAction(this::handleActions);
        getLocationBtn.setOnAction(this::handleActions);
        selectOnMapBtn.setOnAction(this::handleActions);

        setupBanateAreaCheckBox();
        setupLocationTabPane();
    }

    // ==================== SETUP ====================

    private void setupBanateAreaCheckBox() {
        banateAreaCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            latitudeFld.setDisable(newVal);
            longitudeFld.setDisable(newVal);
            radiusFld.setDisable(newVal);
            getLocationBtn.setDisable(newVal);
            selectOnMapBtn.setDisable(newVal);

            if (newVal) {
                latitudeFld.clear();
                longitudeFld.clear();
                radiusFld.clear();
                clearPolygonPoints();
            }
        });
    }

    private void setupLocationTabPane() {
        locationTabPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                locationTabPane.layout();
                bindTabPaneHeightToContent();
            }
        });

        locationTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (banateAreaCheckBox.isSelected()) {
                getLocationBtn.setDisable(true);
                selectOnMapBtn.setDisable(true);
            }
            bindTabPaneHeightToContent();
        });
    }

    private void bindTabPaneHeightToContent() {
        Tab selected = locationTabPane.getSelectionModel().getSelectedItem();
        if (selected == null || !(selected.getContent() instanceof Region)) return;

        Region content = (Region) selected.getContent();

        Platform.runLater(() -> {
            content.applyCss();
            content.layout();

            double headerHeight = getTabHeaderHeight();
            double contentHeight = content.prefHeight(-1);
            locationTabPane.setPrefHeight(headerHeight + contentHeight);
            locationTabPane.setMinHeight(Region.USE_PREF_SIZE);
            locationTabPane.setMaxHeight(Region.USE_PREF_SIZE);
        });
    }

    private double getTabHeaderHeight() {
        javafx.scene.Node headerArea = locationTabPane.lookup(".tab-header-area");
        if (headerArea instanceof Region) {
            return ((Region) headerArea).prefHeight(-1);
        }
        return 44;
    }

    // ==================== ACTION HANDLING ====================

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == updateBtn) {
            updateDisaster();
        } else if (src == exitBtn) {
            closeDialog();
        } else if (src == getLocationBtn) {
            handleGetLocationBtn();
        } else if (src == selectOnMapBtn) {
            handleSelectOnMapBtn();
        }
    }

    // ==================== CIRCLE TAB ====================

    private void handleGetLocationBtn() {
        MappingDialogController controller = DialogManager.getController("mapping", MappingDialogController.class);
        controller.setMarkerType(MappingDialogController.MarkerType.DISASTER);
        controller.setListener(latLng -> {
            latitudeFld.setText(String.valueOf(latLng.lat));
            longitudeFld.setText(String.valueOf(latLng.lon));
        });
        DialogManager.show("mapping");
    }

    // ==================== POLYGON TAB ====================

    private void handleSelectOnMapBtn() {
        PolygonMappingController controller =
                DialogManager.getController("polygonMap", PolygonMappingController.class);

        List<Mapping.LatLng> existingLatLng = new ArrayList<>();
        for (double[] p : polygonPoints) {
            existingLatLng.add(new Mapping.LatLng(p[0], p[1]));
        }
        controller.setExistingPoints(existingLatLng);

        controller.setOnConfirm(confirmed -> {
            polygonPoints.clear();
            for (Mapping.LatLng ll : confirmed) {
                polygonPoints.add(new double[]{ll.lat, ll.lon});
            }
            refreshPolygonPointsList();
        });

        DialogManager.show("polygonMap");
    }

    private void refreshPolygonPointsList() {
        polygonPointsContainer.getChildren().clear();

        boolean hasPoints = !polygonPoints.isEmpty();
        polygonEmptyState.setVisible(!hasPoints);
        polygonEmptyState.setManaged(!hasPoints);
        polygonScrollPane.setVisible(hasPoints);
        polygonScrollPane.setManaged(hasPoints);

        int count = polygonPoints.size();
        polygonPointCountLabel.setText(count + (count == 1 ? " point" : " points"));

        for (int i = 0; i < polygonPoints.size(); i++) {
            double[] point = polygonPoints.get(i);
            HBox row = buildPolygonPointRow(i + 1, point[0], point[1]);
            polygonPointsContainer.getChildren().add(row);
        }

        bindTabPaneHeightToContent();
    }

    private HBox buildPolygonPointRow(int number, double lat, double lon) {
        HBox row = new HBox(10);
        row.getStyleClass().add("polygon-point-row");
        row.setAlignment(Pos.CENTER_LEFT);

        Label indexLabel = new Label(String.valueOf(number));
        indexLabel.getStyleClass().add("polygon-point-index");

        VBox coordsBox = new VBox(2);
        HBox latRow = new HBox(6);
        latRow.setAlignment(Pos.CENTER_LEFT);
        Label latLabel = new Label("Lat:");
        latLabel.getStyleClass().add("polygon-point-label");
        Label latValue = new Label(String.format("%.6f", lat));
        latValue.getStyleClass().add("polygon-point-value");
        latRow.getChildren().addAll(latLabel, latValue);

        HBox lonRow = new HBox(6);
        lonRow.setAlignment(Pos.CENTER_LEFT);
        Label lonLabel = new Label("Lon:");
        lonLabel.getStyleClass().add("polygon-point-label");
        Label lonValue = new Label(String.format("%.6f", lon));
        lonValue.getStyleClass().add("polygon-point-value");
        lonRow.getChildren().addAll(lonLabel, lonValue);

        coordsBox.getChildren().addAll(latRow, lonRow);

        row.getChildren().addAll(indexLabel, coordsBox);
        return row;
    }

    private void clearPolygonPoints() {
        polygonPoints.clear();
        refreshPolygonPointsList();
    }

    // ==================== HELPERS ====================

    private boolean isPolygonTabSelected() {
        return locationTabPane.getSelectionModel().getSelectedItem() == polygonTab;
    }

    private boolean isCircleTabSelected() {
        return locationTabPane.getSelectionModel().getSelectedItem() == circleTab;
    }

    // ==================== POPULATE ====================

    private void populateFields(DisasterModel disaster) {
        this.currentDisaster = disaster;

        disasterType.setValue(disaster.getDisasterType());
        disasterNameFld.setText(disaster.getDisasterName());

        if (disaster.getDate() != null && !disaster.getDate().isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(disaster.getDate());
                datePicker.setValue(date);
            } catch (DateTimeParseException e) {
                System.err.println("Error parsing date: " + e.getMessage());
            }
        }

        notesFld.setText(disaster.getNotes());

        // Determine location type and populate accordingly
        boolean isBanate = disaster.isBanateArea()
                || "BANATE".equals(disaster.getLocationType());
        boolean isPolygon = "POLYGON".equals(disaster.getLocationType())
                && disaster.getPolyLatLong() != null
                && !disaster.getPolyLatLong().trim().isEmpty();

        if (isBanate) {
            banateAreaCheckBox.setSelected(true);
            // Fields are auto-cleared/disabled by the checkbox listener
        } else if (isPolygon) {
            banateAreaCheckBox.setSelected(false);
            locationTabPane.getSelectionModel().select(polygonTab);

            // Parse polygon points from "lat,lon;lat,lon;..." format
            polygonPoints.clear();
            String[] parts = disaster.getPolyLatLong().split(";");
            for (String part : parts) {
                String[] coords = part.trim().split(",");
                if (coords.length == 2) {
                    try {
                        double lat = Double.parseDouble(coords[0].trim());
                        double lon = Double.parseDouble(coords[1].trim());
                        polygonPoints.add(new double[]{lat, lon});
                    } catch (NumberFormatException ignored) { }
                }
            }
            refreshPolygonPointsList();

            latitudeFld.clear();
            longitudeFld.clear();
            radiusFld.clear();
        } else {
            // Circle
            banateAreaCheckBox.setSelected(false);
            locationTabPane.getSelectionModel().select(circleTab);

            latitudeFld.setText(disaster.getLat());
            longitudeFld.setText(disaster.getLongi());
            radiusFld.setText(disaster.getRadius());
            clearPolygonPoints();
        }
    }

    // ==================== SAVE / VALIDATE ====================

    private void updateDisaster() {
        try {
            if (!validateInput()) return;

            String type    = disasterType.getValue();
            String name    = disasterNameFld.getText().trim();
            String date    = datePicker.getValue() != null ? datePicker.getValue().toString() : "";
            String notes   = notesFld.getText().trim();
            boolean isBanate = banateAreaCheckBox.isSelected();
            String regDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            DisasterModel updatedDisaster;

            if (isBanate) {
                // Banate area — no location data
                updatedDisaster = new DisasterModel(type, name, date, "", "", "", notes, regDate, true);
            } else if (isCircleTabSelected()) {
                // Circle — stores lat / lon / radius; poly_lat_long stays null
                String lat    = latitudeFld.getText().trim();
                String lon    = longitudeFld.getText().trim();
                String radius = radiusFld.getText().trim();
                updatedDisaster = new DisasterModel(type, name, date, lat, lon, radius, notes, regDate, false);
            } else {
                // Polygon
                String polyData = serializePolygonPoints();
                updatedDisaster = new DisasterModel(type, name, date, polyData, notes, regDate);
            }

            updatedDisaster.setDisasterId(currentDisaster.getDisasterId());

            boolean success = disasterService.updateDisaster(updatedDisaster);

            if (success) {
                AlertDialogManager.showSuccess("Update Successful",
                        "Disaster information has been successfully updated.");
                disasterController.loadTable();
                Refresher.refresh();
                Refresher.refreshDisasterInSend();
                Refresher.refreshComboBoxOfDNAndAN();
                Refresher.refreshComboAllTypesDisaster();
                Refresher.refreshDisasterCircle(currentDisaster.getDisasterId());
                closeDialog();
            } else {
                AlertDialogManager.showError("Update Failed",
                        "Failed to update disaster. Please try again.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Update Error",
                    "An error occurred while updating disaster: " + e.getMessage());
        }
    }

    private String serializePolygonPoints() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < polygonPoints.size(); i++) {
            if (i > 0) sb.append(";");
            sb.append(polygonPoints.get(i)[0]).append(",").append(polygonPoints.get(i)[1]);
        }
        return sb.toString();
    }

    private boolean validateInput() {
        if (disasterType.getValue() == null || disasterType.getValue().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Disaster type is required.");
            disasterType.requestFocus();
            return false;
        }
        if (disasterNameFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Disaster name is required.");
            disasterNameFld.requestFocus();
            return false;
        }
        if (datePicker.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Date is required.");
            datePicker.requestFocus();
            return false;
        }
        if (!banateAreaCheckBox.isSelected()) {
            if (isCircleTabSelected()) {
                if (latitudeFld.getText().trim().isEmpty()) {
                    AlertDialogManager.showWarning("Validation Error", "Latitude is required.");
                    latitudeFld.requestFocus();
                    return false;
                }
                if (longitudeFld.getText().trim().isEmpty()) {
                    AlertDialogManager.showWarning("Validation Error", "Longitude is required.");
                    longitudeFld.requestFocus();
                    return false;
                }
                if (radiusFld.getText().trim().isEmpty()) {
                    AlertDialogManager.showWarning("Validation Error", "Radius is required.");
                    radiusFld.requestFocus();
                    return false;
                }
            } else {
                if (polygonPoints.size() < 3) {
                    AlertDialogManager.showWarning("Validation Error",
                            "At least 3 points are required to define a polygon.");
                    return false;
                }
            }
        }
        return true;
    }

    // ==================== CLEAR / CLOSE ====================

    private void clearFields() {
        disasterType.getSelectionModel().clearSelection();
        disasterNameFld.clear();
        datePicker.setValue(null);
        latitudeFld.clear();
        longitudeFld.clear();
        radiusFld.clear();
        notesFld.clear();
        banateAreaCheckBox.setSelected(false);
        latitudeFld.setDisable(false);
        longitudeFld.setDisable(false);
        radiusFld.setDisable(false);
        getLocationBtn.setDisable(false);
        selectOnMapBtn.setDisable(false);
        clearPolygonPoints();
        locationTabPane.getSelectionModel().selectFirst();
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.hide();
        } else if (exitBtn.getScene() != null && exitBtn.getScene().getWindow() instanceof Stage) {
            ((Stage) exitBtn.getScene().getWindow()).hide();
        }
    }

    // ==================== KEY / DRAG ====================

    private void setupKeyHandlers() {
        root.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER: updateBtn.fire(); break;
                case ESCAPE: exitBtn.fire(); break;
            }
        });
        root.requestFocus();
    }

    private void makeDraggable() {
        root.setOnMousePressed(e -> {
            yOffset = e.getSceneY();
            xOffset = e.getSceneX();
        });
        root.setOnMouseDragged(e -> {
            if (dialogStage != null) {
                dialogStage.setY(e.getScreenY() - yOffset);
                dialogStage.setX(e.getScreenX() - xOffset);
            }
        });
    }

    // ==================== SETTERS / GETTERS ====================

    public void setDialogStage(Stage stage) { this.dialogStage = stage; }
    public Stage getDialogStage() { return dialogStage; }
    public void setDisasterService(DisasterService s) { this.disasterService = s; }
    public void setDisasterController(DisasterController c) { this.disasterController = c; }

    public void setDisaster(DisasterModel disaster) {
        populateFields(disaster);
    }
}

