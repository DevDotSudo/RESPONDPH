package com.ionres.respondph.disaster.dialogs_controller;

import com.ionres.respondph.common.controller.MappingDialogController;
import com.ionres.respondph.disaster.DisasterController;
import com.ionres.respondph.disaster.DisasterModel;
import com.ionres.respondph.disaster.DisasterService;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.Mapping;
import com.ionres.respondph.util.Refresher;
import com.ionres.respondph.util.DialogManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AddDisasterDialogController {

    // ==================== FXML FIELDS ====================
    @FXML private VBox root;
    @FXML private Button exitBtn, saveBtn;
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
    private double yOffset;
    private double xOffset;

    private final List<double[]> polygonPoints = new ArrayList<>();

    // ==================== INIT ====================
    @FXML
    private void initialize() {
        setupKeyHandlers();
        makeDraggable();

        saveBtn.setOnAction(this::handleActions);
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

        if (src == saveBtn) {
            addDisaster();
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

    public void addPolygonPoint(double lat, double lon) {
        polygonPoints.add(new double[]{lat, lon});
        refreshPolygonPointsList();
    }

    private void removePolygonPoint(int index) {
        if (index >= 0 && index < polygonPoints.size()) {
            polygonPoints.remove(index);
            refreshPolygonPointsList();
        }
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
            final int index = i;
            double[] point = polygonPoints.get(i);
            HBox row = buildPolygonPointRow(index + 1, point[0], point[1], () -> removePolygonPoint(index));
            polygonPointsContainer.getChildren().add(row);
        }

        bindTabPaneHeightToContent();
    }

    private HBox buildPolygonPointRow(int number, double lat, double lon, Runnable onDelete) {
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button deleteBtn = new Button();
        deleteBtn.getStyleClass().add("polygon-delete-btn");
        FontAwesomeIconView deleteIcon = new FontAwesomeIconView();
        deleteIcon.setGlyphName("TIMES");
        deleteIcon.setSize("12");
        deleteIcon.getStyleClass().add("polygon-delete-icon");
        deleteBtn.setGraphic(deleteIcon);
        deleteBtn.setOnAction(e -> onDelete.run());

        row.getChildren().addAll(indexLabel, coordsBox, spacer, deleteBtn);
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

    // ==================== SAVE / VALIDATE ====================

    private void addDisaster() {
        try {
            if (!validateInput()) return;

            String type    = disasterType.getValue();
            String name    = disasterNameFld.getText().trim();
            String date    = datePicker.getValue() != null ? datePicker.getValue().toString() : "";
            String notes   = notesFld.getText().trim();
            boolean isBanate = banateAreaCheckBox.isSelected();
            String regDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            DisasterModel disaster;

            if (isBanate) {
                // Banate area — no location data
                disaster = new DisasterModel(type, name, date, "", "", "", notes, regDate, true);

            } else if (isCircleTabSelected()) {
                // Circle — stores lat / lon / radius; poly_lat_long stays null
                String lat    = latitudeFld.getText().trim();
                String lon    = longitudeFld.getText().trim();
                String radius = radiusFld.getText().trim();
                disaster = new DisasterModel(type, name, date, lat, lon, radius, notes, regDate, false);

            } else {
                // ── POLYGON ──────────────────────────────────────────────────
                // Uses dedicated constructor: sets locationType="POLYGON",
                // lat/lon/radius = null, polyLatLong = serialized points string
                String polyData = serializePolygonPoints();
                disaster = new DisasterModel(type, name, date, polyData, notes, regDate);
            }

            boolean success = disasterService.createDisaster(disaster);

            if (success) {
                AlertDialogManager.showSuccess("Success", "Disaster has been successfully added.");
                disasterController.loadTable();
                clearFields();
                Refresher.refresh();
                Refresher.refreshDisasterInSend();
                Refresher.refreshComboBoxOfDNAndAN();
                Refresher.refreshComboAllTypesDisaster();
            } else {
                AlertDialogManager.showError("Error", "Failed to add disaster. Please try again.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Error", "An error occurred: " + e.getMessage());
        }
    }

    /**
     * Serializes polygon points to "lat,lon;lat,lon;..." format.
     * Encrypted by DisasterServiceImpl before DB storage.
     */
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
        if (dialogStage != null) dialogStage.hide();
    }

    // ==================== KEY / DRAG ====================

    private void setupKeyHandlers() {
        root.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER: saveBtn.fire(); break;
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

    public void onShow() {
        clearFields();
        root.requestFocus();
    }

    public List<double[]> getPolygonPoints() {
        return new ArrayList<>(polygonPoints);
    }
}