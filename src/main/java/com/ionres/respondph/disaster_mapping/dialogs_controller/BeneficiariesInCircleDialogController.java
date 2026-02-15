package com.ionres.respondph.disaster_mapping.dialogs_controller;

import com.ionres.respondph.common.model.BeneficiaryMarker;
import com.ionres.respondph.common.model.DisasterCircleInfo;
import com.ionres.respondph.disaster_mapping.DisasterMappingController;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.AppContext;
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
    private static final int INVALID_DISASTER_ID = -1;

    @FXML private Label titleLabel;
    @FXML private TableView<BeneficiaryMarker> beneficiariesTable;
    @FXML private TableColumn<BeneficiaryMarker, Integer> idColumn;
    @FXML private TableColumn<BeneficiaryMarker, String> nameColumn;
    @FXML private Button closeBtn;
    @FXML private Button evacuateBtn;
    @FXML private Button designatedSiteBtn;
    @FXML private VBox root;

    private Stage dialogStage;
    private double yOffset = 0;
    private double xOffset = 0;
    private ObservableList<BeneficiaryMarker> beneficiariesList = FXCollections.observableArrayList();
    private DisasterCircleInfo currentDisaster;
    private int currentDisasterId = INVALID_DISASTER_ID;
    private DisasterMappingController parentController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupButtons();
        makeDraggable();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setParentController(DisasterMappingController controller) {
        this.parentController = controller;
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

        if (designatedSiteBtn != null){
            designatedSiteBtn.setOnAction(e -> handleDesignatedSite() );
        }
    }

    private void handleDesignatedSite() {
        if (currentDisaster == null) {
            LOGGER.warning("Cannot evacuate: no disaster information available");
            AlertDialogManager.showError("Error", "Disaster information not available.");
            return;
        }

        final int disasterId = (currentDisasterId != INVALID_DISASTER_ID)
                ? currentDisasterId
                : AppContext.currentDisasterId;

        LOGGER.info("Attempting evacuation - Disaster ID: " + disasterId +
                " (source: " + (currentDisasterId != INVALID_DISASTER_ID ? "dialog" : "AppContext") + ")");

        if (disasterId == INVALID_DISASTER_ID || disasterId <= 0) {
            LOGGER.warning("Cannot evacuate: disaster ID not set (currentDisasterId=" +
                    currentDisasterId + ", AppContext=" + AppContext.currentDisasterId + ")");
            AlertDialogManager.showError("Error", "Unable to determine disaster ID. Please select a disaster first.");
            return;
        }

        try {
            LOGGER.info("Opening evacuation site mapping for disaster ID: " + disasterId);

            EvacuationSiteMappingController controller = DialogManager.getController(
                    "evacuationSiteMapping",
                    EvacuationSiteMappingController.class
            );

            if (controller != null) {
                controller.setDisasterId(disasterId);
                LOGGER.info("Set disaster ID on EvacuationSiteMappingController: " + disasterId);

                controller.setDisasterInfo(currentDisaster);
                LOGGER.info("Set disaster info on EvacuationSiteMappingController: " + currentDisaster.disasterName);

                DialogManager.show("evacuationSiteMapping");

                closeDialog();

                LOGGER.info("Evacuation site mapping opened successfully for disaster ID: " + disasterId);
            } else {
                LOGGER.severe("Could not get EvacuationSiteMappingController from DialogManager");
                AlertDialogManager.showError("Error", "Failed to open evacuation site mapping.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error opening evacuation site mapping for disaster ID: " + disasterId, e);
            AlertDialogManager.showError("Error", "Failed to open evacuation site mapping: " + e.getMessage());
        }
    }

    private void handleEvacuateNow() {
        if (currentDisaster == null) {
            LOGGER.warning("Cannot evacuate: no disaster information available");
            AlertDialogManager.showError("Error", "Disaster information not available.");
            return;
        }

        final int disasterId = (currentDisasterId != INVALID_DISASTER_ID)
                ? currentDisasterId
                : AppContext.currentDisasterId;

        LOGGER.info("Attempting evacuation - Disaster ID: " + disasterId);

        if (disasterId == INVALID_DISASTER_ID || disasterId <= 0) {
            LOGGER.warning("Cannot evacuate: disaster ID not set");
            AlertDialogManager.showError("Error", "Unable to determine disaster ID. Please select a disaster first.");
            return;
        }

        try {
            LOGGER.info("Opening evacuation allocation dialog for disaster ID: " + disasterId);

            EvacuationAllocationDialogController controller = DialogManager.getController(
                    "evacuationAllocation",
                    EvacuationAllocationDialogController.class
            );

            if (controller != null) {
                controller.setDisasterData(currentDisaster, disasterId);
                DialogManager.show("evacuationAllocation");
                closeDialog();
                LOGGER.info("Evacuation allocation dialog opened successfully");
            } else {
                LOGGER.severe("Could not get EvacuationAllocationDialogController from DialogManager");
                AlertDialogManager.showError("Error", "Failed to open evacuation allocation dialog.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error opening evacuation allocation dialog", e);
            AlertDialogManager.showError("Error", "Failed to open evacuation allocation dialog: " + e.getMessage());
        }
    }


    public void setData(DisasterCircleInfo circle, List<BeneficiaryMarker> beneficiaries, int disasterId) {
        this.currentDisaster = circle;
        this.currentDisasterId = disasterId;

        LOGGER.info("=== SETTING BENEFICIARIES DIALOG DATA ===");
        LOGGER.info("Disaster ID: " + disasterId);
        LOGGER.info("Disaster: " + (circle != null ? circle.disasterType + " - " + circle.disasterName : "null"));
        LOGGER.info("Beneficiary count: " + (beneficiaries != null ? beneficiaries.size() : 0));


        titleLabel.setText("Beneficiaries in Disaster Area");

        beneficiariesList.clear();
        if (beneficiaries != null && !beneficiaries.isEmpty()) {
            beneficiariesList.addAll(beneficiaries);
        }

        LOGGER.info("Dialog data set successfully");
    }


    @Deprecated
    public void setData(DisasterCircleInfo circle, List<BeneficiaryMarker> beneficiaries) {
        LOGGER.warning("=== DEPRECATED METHOD CALLED ===");
        LOGGER.warning("setData() called without disaster ID - allocation will not work properly");
        LOGGER.warning("Please update calling code to use setData(circle, beneficiaries, disasterId)");

        int fallbackDisasterId = AppContext.currentDisasterId;
        LOGGER.warning("Attempting to use disaster ID from AppContext: " + fallbackDisasterId);

        setData(circle, beneficiaries, fallbackDisasterId);
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