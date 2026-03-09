package com.ionres.respondph.disaster_mapping.dialogs_controller;

import com.ionres.respondph.common.model.BeneficiaryMarker;
import com.ionres.respondph.common.model.DisasterCircleInfo;
import com.ionres.respondph.disaster_mapping.DisasterMappingController;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.AppContext;
import com.ionres.respondph.util.DialogManager;
import com.ionres.respondph.util.ThemeManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BeneficiariesInCircleDialogController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(BeneficiariesInCircleDialogController.class.getName());
    private static final int INVALID_DISASTER_ID = -1;

    @FXML private Label titleLabel;
    @FXML private Label infoLabel;
    @FXML private Label totalBeneficiariesLabel;
    @FXML private Label showingCountLabel;
    @FXML private Label totalCountLabel;
    @FXML private TextField searchField;
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
    private FilteredList<BeneficiaryMarker> filteredList;
    private DisasterCircleInfo currentDisaster;
    private int currentDisasterId = INVALID_DISASTER_ID;
    private DisasterMappingController parentController;

    // ── Progress dialog fields ─────────────────────────────────────────────────
    private Stage       progressStage;
    private ProgressBar progressBar;
    private Label       progressLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupSearch();
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

        filteredList = new FilteredList<>(beneficiariesList, p -> true);
        beneficiariesTable.setItems(filteredList);
    }

    private void setupSearch() {
        if (searchField == null) return;

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredList.setPredicate(beneficiary -> {
                if (newValue == null || newValue.trim().isEmpty()) {
                    updateCountLabels(beneficiariesList.size(), beneficiariesList.size());
                    return true;
                }

                String lowerCaseFilter = newValue.trim().toLowerCase();
                boolean matchesId   = String.valueOf(beneficiary.id).contains(lowerCaseFilter);
                boolean matchesName = beneficiary.name != null
                        && beneficiary.name.toLowerCase().contains(lowerCaseFilter);
                return matchesId || matchesName;
            });
            updateCountLabels(filteredList.size(), beneficiariesList.size());
        });
    }

    private void setupButtons() {
        if (closeBtn != null)         closeBtn.setOnAction(e -> closeDialog());
        if (evacuateBtn != null)      evacuateBtn.setOnAction(e -> handleEvacuateNow());
        if (designatedSiteBtn != null) designatedSiteBtn.setOnAction(e -> handleDesignatedSite());
    }

    private void updateCountLabels(int showing, int total) {
        if (totalBeneficiariesLabel != null) totalBeneficiariesLabel.setText("Total: " + total);
        if (showingCountLabel != null)       showingCountLabel.setText(String.valueOf(showing));
        if (totalCountLabel != null)         totalCountLabel.setText(String.valueOf(total));
    }

    private void updateDisasterInfoLabels() {
        if (infoLabel == null || currentDisaster == null) return;

        String type = currentDisaster.disasterType != null ? currentDisaster.disasterType : "";
        String name = currentDisaster.disasterName != null ? currentDisaster.disasterName : "";

        if (!type.isEmpty() && !name.isEmpty())  infoLabel.setText("Disaster: " + type + " — " + name);
        else if (!name.isEmpty())                infoLabel.setText("Disaster: " + name);
        else if (!type.isEmpty())                infoLabel.setText("Disaster Type: " + type);
        else                                     infoLabel.setText("Disaster: —");
    }

    // =========================================================================
    // PROGRESS DIALOG  (mirrors EvacuationPlanPrintingController)
    // =========================================================================

    private void createProgressDialog(String initialMessage) {
        progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.initStyle(StageStyle.UNDECORATED);
        progressStage.setAlwaysOnTop(true);

        boolean light = ThemeManager.getInstance().isLightMode();

        VBox card = new VBox(0);
        card.setPrefWidth(420);
        card.setStyle(
                "-fx-background-color: " + (light ? "#EDE8DF" : "#0b1220") + ";" +
                        "-fx-border-color: " + (light ? "rgba(176,200,178,0.80)" : "rgba(148,163,184,0.22)") + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0," + (light ? "0.18" : "0.45") + "), 28, 0.0, 0, 6);"
        );

        // ── Header ────────────────────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 22, 18, 22));
        header.setStyle(
                "-fx-background-color: " + (light ? "#5C8A79" : "rgba(255,255,255,0.025)") + ";" +
                        "-fx-border-color: " + (light ? "rgba(90,130,115,0.45)" : "rgba(148,163,184,0.12)") + ";" +
                        "-fx-border-width: 0 0 1 0;" +
                        "-fx-background-radius: 10 10 0 0;"
        );

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(22, 22);
        spinner.setMaxSize(22, 22);
        spinner.setMinSize(22, 22);
        spinner.setStyle("-fx-progress-color: " + (light ? "#B85507" : "rgba(249,115,22,0.95)") + ";");

        VBox titleBlock = new VBox(3);
        Label titleLabel = new Label("Please Wait");
        titleLabel.setFont(Font.font("Inter", FontWeight.BLACK, 16));
        titleLabel.setStyle(
                "-fx-text-fill: " + (light ? "#FFFFFF" : "rgba(248,250,252,0.98)") + ";" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: 900;"
        );
        Label subtitleLabel = new Label("Processing your request…");
        subtitleLabel.setStyle(
                "-fx-text-fill: " + (light ? "rgba(255,255,255,0.75)" : "rgba(148,163,184,0.80)") + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: 600;"
        );
        titleBlock.getChildren().addAll(titleLabel, subtitleLabel);
        header.getChildren().addAll(spinner, titleBlock);

        // ── Body ──────────────────────────────────────────────────────────────
        VBox body = new VBox(14);
        body.setPadding(new Insets(22, 22, 24, 22));
        body.setAlignment(Pos.CENTER_LEFT);
        body.setStyle("-fx-background-color: transparent;");

        progressLabel = new Label(initialMessage);
        progressLabel.setWrapText(true);
        progressLabel.setMaxWidth(Double.MAX_VALUE);
        progressLabel.setStyle(
                "-fx-text-fill: " + (light ? "#1A1A1A" : "rgba(226,232,240,0.85)") + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 600;"
        );

        VBox barWrapper = new VBox(0);
        barWrapper.setStyle(
                "-fx-background-color: " + (light ? "rgba(176,200,178,0.35)" : "rgba(255,255,255,0.06)") + ";" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: " + (light ? "rgba(176,200,178,0.70)" : "rgba(148,163,184,0.14)") + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-padding: 0;"
        );

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle(
                "-fx-accent: " + (light ? "#B85507" : "rgba(249,115,22,0.95)") + ";" +
                        "-fx-background-color: transparent;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-radius: 6;"
        );
        barWrapper.getChildren().add(progressBar);

        Label pctLabel = new Label("0%");
        pctLabel.setStyle(
                "-fx-text-fill: " + (light ? "#4A7566" : "rgba(148,163,184,0.70)") + ";" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: 700;"
        );
        HBox pctRow = new HBox();
        Region pctSpacer = new Region();
        HBox.setHgrow(pctSpacer, Priority.ALWAYS);
        pctRow.getChildren().addAll(pctSpacer, pctLabel);

        progressBar.progressProperty().addListener((obs, oldVal, newVal) -> {
            double pct = newVal.doubleValue();
            pctLabel.setText(pct < 0 ? "…" : String.format("%.0f%%", pct * 100));
        });

        body.getChildren().addAll(progressLabel, barWrapper, pctRow);
        card.getChildren().addAll(header, body);

        Scene scene = new Scene(card);
        scene.setFill(null);
        progressStage.setScene(scene);

        if (dialogStage != null) {
            progressStage.initOwner(dialogStage);
            progressStage.setX(dialogStage.getX() + (dialogStage.getWidth()  - 420) / 2);
            progressStage.setY(dialogStage.getY() + (dialogStage.getHeight() - 160) / 2);
        }

        progressStage.show();
    }

    private void closeProgressDialog() {
        if (progressStage != null) {
            Platform.runLater(() -> {
                progressStage.close();
                progressStage = null;
            });
        }
    }

    // =========================================================================
    // EVACUATE NOW — with progress
    // =========================================================================

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
            AlertDialogManager.showError("Error",
                    "Unable to determine disaster ID. Please select a disaster first.");
            return;
        }

        createProgressDialog("Preparing evacuation allocation dialog…");

        Task<EvacuationAllocationDialogController> task = new Task<>() {
            @Override
            protected EvacuationAllocationDialogController call() throws Exception {
                updateProgress(0, 100);
                updateMessage("Verifying disaster data…");
                Thread.sleep(250);

                updateProgress(30, 100);
                updateMessage("Loading beneficiary records…");
                Thread.sleep(300);

                updateProgress(60, 100);
                updateMessage("Fetching evacuation allocation dialog…");

                EvacuationAllocationDialogController controller =
                        DialogManager.getController(
                                "evacuationAllocation",
                                EvacuationAllocationDialogController.class);

                if (controller == null) {
                    throw new Exception("Failed to load evacuation allocation dialog.");
                }

                updateProgress(85, 100);
                updateMessage("Applying disaster context…");
                Thread.sleep(200);

                updateProgress(100, 100);
                updateMessage("Ready.");
                Thread.sleep(100);

                return controller;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());

        ExecutorService executor = Executors.newSingleThreadExecutor();

        task.setOnSucceeded(e -> {
            closeProgressDialog();
            EvacuationAllocationDialogController controller = task.getValue();
            controller.setDisasterData(currentDisaster, disasterId);
            DialogManager.show("evacuationAllocation");
            shutDownExecutor(executor);
            closeDialog();
            LOGGER.info("Evacuation allocation dialog opened successfully for disaster ID: " + disasterId);
        });

        task.setOnFailed(e -> {
            closeProgressDialog();
            Throwable ex = task.getException();
            String msg = (ex != null && ex.getMessage() != null)
                    ? ex.getMessage()
                    : "An unknown error occurred.";
            shutDownExecutor(executor);
            LOGGER.log(Level.SEVERE, "Error opening evacuation allocation dialog", ex);
            AlertDialogManager.showError("Error", "Failed to open evacuation allocation dialog: " + msg);
        });

        executor.submit(task);
    }

    private void shutDownExecutor(ExecutorService executor){
        try {
            System.out.println("attempt to shutdown executor");
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }catch (InterruptedException e) {
            System.err.println("tasks interrupted");
        }finally {
            if (!executor.isTerminated()) {
                System.err.println("cancel non-finished tasks");
            }
            executor.shutdownNow();
            System.out.println("shutdown finished");
        }
    }

    // =========================================================================
    // DESIGNATED SITE (unchanged)
    // =========================================================================

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
            AlertDialogManager.showError("Error",
                    "Unable to determine disaster ID. Please select a disaster first.");
            return;
        }

        try {
            LOGGER.info("Opening evacuation site mapping for disaster ID: " + disasterId);

            EvacuationSiteMappingController controller = DialogManager.getController(
                    "evacuationSiteMapping",
                    EvacuationSiteMappingController.class);

            if (controller != null) {
                controller.setDisasterId(disasterId);
                LOGGER.info("Set disaster ID on EvacuationSiteMappingController: " + disasterId);

                controller.setDisasterInfo(currentDisaster);
                LOGGER.info("Set disaster info on EvacuationSiteMappingController: "
                        + currentDisaster.disasterName);

                DialogManager.show("evacuationSiteMapping");
                closeDialog();
                LOGGER.info("Evacuation site mapping opened successfully for disaster ID: " + disasterId);
            } else {
                LOGGER.severe("Could not get EvacuationSiteMappingController from DialogManager");
                AlertDialogManager.showError("Error", "Failed to open evacuation site mapping.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Error opening evacuation site mapping for disaster ID: " + disasterId, e);
            AlertDialogManager.showError("Error",
                    "Failed to open evacuation site mapping: " + e.getMessage());
        }
    }

    // =========================================================================
    // DATA
    // =========================================================================

    public void setData(DisasterCircleInfo circle, List<BeneficiaryMarker> beneficiaries,
                        int disasterId) {
        this.currentDisaster   = circle;
        this.currentDisasterId = disasterId;

        LOGGER.info("=== SETTING BENEFICIARIES DIALOG DATA ===");
        LOGGER.info("Disaster ID: " + disasterId);
        LOGGER.info("Disaster: " + (circle != null
                ? circle.disasterType + " - " + circle.disasterName : "null"));
        LOGGER.info("Beneficiary count: "
                + (beneficiaries != null ? beneficiaries.size() : 0));

        titleLabel.setText("Beneficiaries in Disaster Area");

        if (searchField != null) searchField.clear();

        beneficiariesList.clear();
        if (beneficiaries != null && !beneficiaries.isEmpty()) {
            beneficiariesList.addAll(beneficiaries);
        }

        int total = beneficiariesList.size();
        updateCountLabels(total, total);
        updateDisasterInfoLabels();

        LOGGER.info("Dialog data set successfully");
    }

    @Deprecated
    public void setData(DisasterCircleInfo circle, List<BeneficiaryMarker> beneficiaries) {
        LOGGER.warning("=== DEPRECATED METHOD CALLED ===");
        LOGGER.warning("setData() called without disaster ID — allocation will not work properly");
        LOGGER.warning("Please update calling code to use setData(circle, beneficiaries, disasterId)");

        int fallbackDisasterId = AppContext.currentDisasterId;
        LOGGER.warning("Attempting to use disaster ID from AppContext: " + fallbackDisasterId);

        setData(circle, beneficiaries, fallbackDisasterId);
    }

    private void closeDialog() {
        if (dialogStage != null) dialogStage.hide();
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