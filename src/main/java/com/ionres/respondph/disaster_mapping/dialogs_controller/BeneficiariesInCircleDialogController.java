package com.ionres.respondph.disaster_mapping.dialogs_controller;

import com.ionres.respondph.common.model.BeneficiaryMarker;
import com.ionres.respondph.common.model.DisasterCircleInfo;
import com.ionres.respondph.util.DialogManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BeneficiariesInCircleDialogController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(BeneficiariesInCircleDialogController.class.getName());

    @FXML private Label titleLabel;
    @FXML private Label infoLabel;
    @FXML private TableView<BeneficiaryMarker> beneficiariesTable;
    @FXML private TableColumn<BeneficiaryMarker, Integer> idColumn;
    @FXML private TableColumn<BeneficiaryMarker, String> nameColumn;
    @FXML private Button closeBtn;
    @FXML private Button evacuateBtn;
    private Stage dialogStage;
    @FXML private VBox root;
    private double yOffset = 0;
    private double xOffset = 0;
    private ObservableList<BeneficiaryMarker> beneficiariesList = FXCollections.observableArrayList();
    private DisasterCircleInfo currentDisaster;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupButtons();
        makeDraggable();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    private void setupTable() {
        idColumn.setCellValueFactory(cellData -> {
            BeneficiaryMarker marker = cellData.getValue();
            return new javafx.beans.property.SimpleIntegerProperty(marker.id).asObject();
        });
        nameColumn.setCellValueFactory(cellData -> {
            BeneficiaryMarker marker = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(marker.name);
        });

        beneficiariesTable.setItems(beneficiariesList);
    }

    private void setupButtons() {
        if (closeBtn != null) {
            closeBtn.setOnAction(e -> closeDialog());
        }
        if (evacuateBtn != null) {
            evacuateBtn.setOnAction(e -> handleEvacuateNow());
        }
    }

    private void handleEvacuateNow() {
        if (currentDisaster == null) {
            LOGGER.warning("Cannot evacuate: no disaster information available");
            return;
        }

        try {
            EvacuationSiteMappingController controller = DialogManager.getController(
                    "evacuationSiteMapping",
                    EvacuationSiteMappingController.class
            );

            if (controller != null) {
                controller.setDisasterInfo(currentDisaster);

                DialogManager.show("evacuationSiteMapping");

                closeDialog();
            } else {
                LOGGER.warning("Could not get evacuation site mapping controller");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error opening evacuation site mapping", e);
        }
    }

    public void setData(DisasterCircleInfo circle, List<BeneficiaryMarker> beneficiaries) {
        this.currentDisaster = circle;

        String disasterInfo = String.format("%s - %s",
                circle.disasterType != null ? circle.disasterType : "Unknown Type",
                circle.disasterName != null ? circle.disasterName : "Unknown Disaster");
        infoLabel.setText("Disaster: " + disasterInfo);

        titleLabel.setText("Beneficiaries in Disaster Area");

        beneficiariesList.clear();
        if (beneficiaries != null && !beneficiaries.isEmpty()) {
            beneficiariesList.addAll(beneficiaries);
        }
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.hide();
        }
    }

    private void makeDraggable() {
        root.setOnMousePressed(event -> {
            yOffset = event.getSceneY();
            xOffset = event.getSceneX();
        });
        root.setOnMouseDragged(event -> {
            if (dialogStage != null) {
                dialogStage.setX(event.getScreenX() - xOffset);
                dialogStage.setY(event.getScreenY() - yOffset);
            }
        });
    }
}