package com.ionres.respondph.aid_type.dialogs_controller;

import com.ionres.respondph.aidType_and_household_score.AidHouseholdScoreCalculate;
import com.ionres.respondph.aid_type.AidTypeController;
import com.ionres.respondph.aid_type.AidTypeModel;
import com.ionres.respondph.aid_type.AidTypeService;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.Refresher;
import com.ionres.respondph.util.SessionManager;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;
import java.util.function.UnaryOperator;

public class AddAidTypeController {

    @FXML private VBox root;
    @FXML private TextField aidNameFld;
    @FXML private TextField ageWeightFld;
    @FXML private TextField genderWeightFld;
    @FXML private TextField maritalStatusWeightFld;
    @FXML private TextField soloParentWeightFld;
    @FXML private TextField disabilityWeightFld;
    @FXML private TextField healthConditionWeightFld;
    @FXML private TextField waterAccessWeightFld;
    @FXML private TextField sanitationWeightFld;
    @FXML private TextField houseTypeWeightFld;
    @FXML private TextField ownershipWeightFld;
    @FXML private TextField damageSeverityWeightFld;
    @FXML private TextField employmentWeightFld;
    @FXML private TextField monthlyIncomeWeightFld;
    @FXML private TextField educationWeightFld;
    @FXML private TextField digitalAccessWeightFld;
    @FXML private TextField dependencyRatioWeightFld;
    @FXML private TextArea notesFld;
    @FXML private Button saveBtn;
    @FXML private Button exitBtn;

    private double xOffset = 0;
    private double yOffset = 0;
    private AidTypeService aidTypeService;
    private AidTypeController aidTypeController;

    public void setAidTypeService(AidTypeService aidTypeService) {
        this.aidTypeService = aidTypeService;
    }

    public void setAidTypeController(AidTypeController aidTypeController) {
        this.aidTypeController = aidTypeController;
    }

    @FXML
    private void initialize() {
        makeDraggable();
        setupNumericValidation();
        EventHandler<ActionEvent> handlers = this::handleActions;

        saveBtn.setOnAction(handlers);
        exitBtn.setOnAction(handlers);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == saveBtn) {
            addAidType();
        } else if (src == exitBtn) {
            closeDialog();
        }
    }

    private void setupNumericValidation() {
        TextField[] weightFields = {
                ageWeightFld, genderWeightFld, maritalStatusWeightFld, soloParentWeightFld,
                disabilityWeightFld, healthConditionWeightFld, waterAccessWeightFld, sanitationWeightFld,
                houseTypeWeightFld, ownershipWeightFld, damageSeverityWeightFld, employmentWeightFld,
                monthlyIncomeWeightFld, educationWeightFld, digitalAccessWeightFld, dependencyRatioWeightFld
        };

        for (TextField field : weightFields) {
            if (field != null) {
                setNumericWeightFilter(field);
            }
        }
    }

    private void setNumericWeightFilter(TextField textField) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();

            if (newText.isEmpty()) {
                return change;
            }

            if (newText.matches("-?\\d*(\\.\\d*)?")) {
                int decimalCount = newText.length() - newText.replace(".", "").length();
                if (decimalCount <= 1) {
                    return change;
                }
            }

            return null;
        };

        textField.setTextFormatter(new TextFormatter<>(filter));
    }

    private void addAidType() {
        try {
            if (!validateWeightSum()) return;

            double ageWeight = parseWeight(ageWeightFld);
            double genderWeight = parseWeight(genderWeightFld);
            double maritalStatusWeight = parseWeight(maritalStatusWeightFld);
            double soloParentWeight = parseWeight(soloParentWeightFld);
            double disabilityWeight = parseWeight(disabilityWeightFld);
            double healthConditionWeight = parseWeight(healthConditionWeightFld);
            double accessToCleanWaterWeight = parseWeight(waterAccessWeightFld);
            double sanitationFacilityWeight = parseWeight(sanitationWeightFld);
            double houseConstructionTypeWeight = parseWeight(houseTypeWeightFld);
            double ownershipWeight = parseWeight(ownershipWeightFld);
            double damageSeverityWeight = parseWeight(damageSeverityWeightFld);
            double employmentStatusWeight = parseWeight(employmentWeightFld);
            double monthlyIncomeWeight = parseWeight(monthlyIncomeWeightFld);
            double educationalLevelWeight = parseWeight(educationWeightFld);
            double digitalAccessWeight = parseWeight(digitalAccessWeightFld);
            double dependencyRatioWeight = parseWeight(dependencyRatioWeightFld);

            String aidTypeName = aidNameFld.getText().trim();
            String notes = notesFld.getText().trim();

            int adminId = SessionManager.getInstance().getCurrentAdminId();
            if (adminId <= 0) {
                AlertDialogManager.showWarning("Warning", "No admin logged in");
                return;
            }

            String regDate = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            AidTypeModel aidTypeModel = new AidTypeModel(
                    aidTypeName,
                    ageWeight,
                    genderWeight,
                    maritalStatusWeight,
                    soloParentWeight,
                    disabilityWeight,
                    healthConditionWeight,
                    accessToCleanWaterWeight,
                    sanitationFacilityWeight,
                    houseConstructionTypeWeight,
                    ownershipWeight,
                    damageSeverityWeight,
                    employmentStatusWeight,
                    monthlyIncomeWeight,
                    educationalLevelWeight,
                    digitalAccessWeight,
                    dependencyRatioWeight,
                    notes,
                    adminId,
                    regDate
            );

            boolean success = aidTypeService.createAidType(aidTypeModel);

            if (success) {
                int newAidTypeId = getLatestAidTypeId();

                if (newAidTypeId > 0) {
                    showProgressAndRecalculate(newAidTypeId, adminId);
                } else {
                    AlertDialogManager.showWarning("Warning",
                            "Aid type created, but could not determine new Aid Type ID for score calculation.");
                    Refresher.refreshComboBoxOfDNAndAN();
                    Refresher.refresh();
                    clearFields();
                    aidTypeController.loadTable();
                }
            } else {
                AlertDialogManager.showWarning("Error", "Failed to add Aid Type.");
            }

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(
                    null,
                    e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
        }
    }

    // ── Progress dialog + background Task (mirrors VulnerabilityIndicatorController) ──
    private void showProgressAndRecalculate(int newAidTypeId, int adminId) {

        // ── Build the progress dialog ─────────────────────────────────────────
        Stage progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.initStyle(StageStyle.UNDECORATED);
        progressStage.setResizable(false);
        progressStage.setAlwaysOnTop(true);

        // ── Outer wrapper (dark card) ─────────────────────────────────────────
        VBox card = new VBox(0);
        card.setPrefWidth(420);
        card.setStyle(
                "-fx-background-color: #0b1220;" +
                        "-fx-border-color: rgba(148,163,184,0.22);" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 28, 0.0, 0, 6);"
        );

        // ── Header ────────────────────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 22, 18, 22));
        header.setStyle(
                "-fx-background-color: rgba(255,255,255,0.025);" +
                        "-fx-border-color: rgba(148,163,184,0.12);" +
                        "-fx-border-width: 0 0 1 0;" +
                        "-fx-background-radius: 10 10 0 0;"
        );

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(22, 22);
        spinner.setMaxSize(22, 22);
        spinner.setMinSize(22, 22);
        spinner.setStyle("-fx-progress-color: rgba(249,115,22,0.95);");

        VBox titleBlock = new VBox(3);
        Label titleLabel = new Label("Please Wait");
        titleLabel.setFont(Font.font("Inter", FontWeight.BLACK, 16));
        titleLabel.setStyle(
                "-fx-text-fill: rgba(248,250,252,0.98);" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: 900;"
        );
        Label subtitleLabel = new Label("Saving Aid Type");
        subtitleLabel.setStyle(
                "-fx-text-fill: rgba(148,163,184,0.80);" +
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

        Label statusLabel = new Label("Calculating household scores for all beneficiaries...");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setStyle(
                "-fx-text-fill: rgba(226,232,240,0.85);" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 600;"
        );

        // Progress bar track wrapper
        VBox barWrapper = new VBox(0);
        barWrapper.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: rgba(148,163,184,0.14);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-padding: 0;"
        );

        ProgressBar progressBar = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle(
                "-fx-accent: rgba(249,115,22,0.95);" +
                        "-fx-background-color: transparent;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-radius: 6;"
        );
        barWrapper.getChildren().add(progressBar);

        // Percentage / indeterminate label (right-aligned)
        Label pctLabel = new Label("…");
        pctLabel.setStyle(
                "-fx-text-fill: rgba(148,163,184,0.70);" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: 700;"
        );
        HBox pctRow = new HBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        pctRow.getChildren().addAll(spacer, pctLabel);

        progressBar.progressProperty().addListener((obs, oldVal, newVal) -> {
            double pct = newVal.doubleValue();
            if (pct < 0) {
                pctLabel.setText("…");
            } else {
                pctLabel.setText(String.format("%.0f%%", pct * 100));
            }
        });

        body.getChildren().addAll(statusLabel, barWrapper, pctRow);
        card.getChildren().addAll(header, body);

        Scene scene = new Scene(card);
        scene.setFill(null);
        progressStage.setScene(scene);

        // Center on owner stage if available
        Stage ownerStage = (Stage) root.getScene().getWindow();
        if (ownerStage != null) {
            progressStage.initOwner(ownerStage);
            progressStage.setX(ownerStage.getX() + (ownerStage.getWidth()  - 420) / 2);
            progressStage.setY(ownerStage.getY() + (ownerStage.getHeight() - 160) / 2);
        }

        progressStage.show();

        // ── Disable save button while running ─────────────────────────────────
        saveBtn.setDisable(true);

        // ── Run the heavy calculation off the FX thread ───────────────────────
        Task<int[]> task = new Task<>() {
            @Override
            protected int[] call() {
                updateMessage("Calculating household scores for all beneficiaries...");

                List<BeneficiaryDisasterPair> pairs = getAllBeneficiaryDisasterPairsWithHouseholdScores();
                int total = pairs.size();
                int successCount = 0;
                int failCount = 0;

                AidHouseholdScoreCalculate calculator = new AidHouseholdScoreCalculate();

                for (int i = 0; i < total; i++) {
                    BeneficiaryDisasterPair pair = pairs.get(i);
                    updateMessage("Processing beneficiary " + (i + 1) + " of " + total + "…");
                    updateProgress(i + 1, total);

                    try {
                        boolean success;
                        if (pair.disasterId != null) {
                            success = calculator.calculateAndSaveAidHouseholdScoreWithDisaster(
                                    pair.beneficiaryId, newAidTypeId, adminId, pair.disasterId);
                        } else {
                            success = calculator.calculateAndSaveAidHouseholdScore(
                                    pair.beneficiaryId, newAidTypeId, adminId);
                        }

                        if (success) successCount++;
                        else failCount++;

                    } catch (Exception e) {
                        failCount++;
                        System.err.println("✗ Error for beneficiary ID " + pair.beneficiaryId +
                                ", disaster ID " + pair.disasterId + ": " + e.getMessage());
                    }
                }

                return new int[]{successCount, failCount, total};
            }
        };

        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            statusLabel.textProperty().unbind();
            progressStage.close();
            saveBtn.setDisable(false);

            int[] result = task.getValue();
            int successCount = result[0];
            int total = result[2];

            AlertDialogManager.showSuccess("Success",
                    "Aid Type added successfully!\n" +
                            successCount + " of " + total +
                            " household score(s) calculated for all beneficiaries.");

            // Close the add-aid-type dialog (owner of the progress dialog)
            try {
                java.awt.Window w = null;
                if (root.getScene() != null && root.getScene().getWindow() != null) {
                    javafx.stage.Window win = root.getScene().getWindow();
                    if (win instanceof Stage) ((Stage) win).hide();
                }
            } catch (Exception ignore) {}

            Refresher.refreshComboBoxOfDNAndAN();
            Refresher.refresh();
            clearFields();
            aidTypeController.loadTable();
        });

        task.setOnFailed(e -> {
            statusLabel.textProperty().unbind();
            progressStage.close();
            saveBtn.setDisable(false);

            Throwable ex = task.getException();
            AlertDialogManager.showWarning("Warning",
                    "Aid type created, but there was an error calculating household scores.\n" +
                            (ex != null ? ex.getMessage() : "Unknown error"));
            if (ex != null) ex.printStackTrace();

            // Close the add-aid-type dialog (owner of the progress dialog)
            try {
                if (root.getScene() != null && root.getScene().getWindow() != null) {
                    javafx.stage.Window win = root.getScene().getWindow();
                    if (win instanceof Stage) ((Stage) win).hide();
                }
            } catch (Exception ignore) {}

            Refresher.refreshComboBoxOfDNAndAN();
            Refresher.refresh();
            clearFields();
            aidTypeController.loadTable();
        });

        Thread thread = new Thread(task, "AidTypeRecalculate-Thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void recalculateAllBeneficiaryScores(int aidTypeId, int adminId) {
        try {
            System.out.println("========== RECALCULATING AID-HOUSEHOLD SCORES ==========");
            System.out.println("Aid Type ID: " + aidTypeId);
            System.out.println("Admin ID: " + adminId);

            List<BeneficiaryDisasterPair> beneficiaryDisasterPairs =
                    getAllBeneficiaryDisasterPairsWithHouseholdScores();

            System.out.println("Found " + beneficiaryDisasterPairs.size() +
                    " beneficiary-disaster pairs with household scores");

            AidHouseholdScoreCalculate calculator = new AidHouseholdScoreCalculate();
            int successCount = 0;
            int failCount = 0;

            for (BeneficiaryDisasterPair pair : beneficiaryDisasterPairs) {
                try {
                    boolean success;

                    if (pair.disasterId != null) {
                        System.out.println("→ With disaster (ID: " + pair.disasterId +
                                ") for beneficiary ID: " + pair.beneficiaryId);
                        success = calculator.calculateAndSaveAidHouseholdScoreWithDisaster(
                                pair.beneficiaryId, aidTypeId, adminId, pair.disasterId);
                    } else {
                        System.out.println("→ No disaster (NULL) for beneficiary ID: " + pair.beneficiaryId);
                        success = calculator.calculateAndSaveAidHouseholdScore(
                                pair.beneficiaryId, aidTypeId, adminId);
                    }

                    if (success) {
                        successCount++;
                        System.out.println("✓ Calculated aid-household score for beneficiary ID: " +
                                pair.beneficiaryId + ", disaster ID: " + pair.disasterId);
                    } else {
                        failCount++;
                        System.err.println("✗ Failed for beneficiary ID: " +
                                pair.beneficiaryId + ", disaster ID: " + pair.disasterId);
                    }
                } catch (Exception e) {
                    failCount++;
                    System.err.println("✗ Error for beneficiary ID " + pair.beneficiaryId +
                            ", disaster ID " + pair.disasterId + ": " + e.getMessage());
                }
            }

            System.out.println("========== RECALCULATION COMPLETE ==========");
            System.out.println("Successfully calculated: " + successCount + " out of " +
                    beneficiaryDisasterPairs.size());
            System.out.println("Failed: " + failCount);

        } catch (Exception e) {
            System.err.println("Error recalculating all beneficiary scores: " + e.getMessage());
            e.printStackTrace();
            AlertDialogManager.showWarning("Warning",
                    "Aid type created successfully, but there was an error calculating household scores.\n" +
                            "You may need to recalculate manually.");
        }
    }

    private double parseWeight(TextField field) {
        String text = field.getText().trim();
        return text.isEmpty() ? 0.0 : Double.parseDouble(text);
    }

    private int getLatestAidTypeId() {
        try {
            return aidTypeService.getLatestAidTypeId();
        } catch (Exception e) {
            System.err.println("Error getting latest aid type ID: " + e.getMessage());
            return -1;
        }
    }

    private static class BeneficiaryDisasterPair {
        int beneficiaryId;
        Integer disasterId;

        BeneficiaryDisasterPair(int beneficiaryId, Integer disasterId) {
            this.beneficiaryId = beneficiaryId;
            this.disasterId = disasterId;
        }
    }

    private List<BeneficiaryDisasterPair> getAllBeneficiaryDisasterPairsWithHouseholdScores() {
        List<BeneficiaryDisasterPair> pairs = new java.util.ArrayList<>();
        String sql = "SELECT DISTINCT beneficiary_id, disaster_id FROM household_score";

        java.sql.Connection conn = null;
        try {
            conn = com.ionres.respondph.database.DBConnection.getInstance().getConnection();
            java.sql.PreparedStatement ps = conn.prepareStatement(sql);
            java.sql.ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int beneficiaryId = rs.getInt("beneficiary_id");
                Object disasterObj = rs.getObject("disaster_id");
                Integer disasterId = (disasterObj != null) ? ((Number) disasterObj).intValue() : null;
                pairs.add(new BeneficiaryDisasterPair(beneficiaryId, disasterId));
            }

            rs.close();
            ps.close();

        } catch (java.sql.SQLException e) {
            System.err.println("Error fetching beneficiary-disaster pairs: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (conn != null && !conn.isClosed()) conn.close();
            } catch (java.sql.SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }

        return pairs;
    }

    private boolean validateWeightSum() {
        try {
            double totalWeight =
                    parseWeight(ageWeightFld) + parseWeight(genderWeightFld) +
                            parseWeight(maritalStatusWeightFld) + parseWeight(soloParentWeightFld) +
                            parseWeight(disabilityWeightFld) + parseWeight(healthConditionWeightFld) +
                            parseWeight(waterAccessWeightFld) + parseWeight(sanitationWeightFld) +
                            parseWeight(houseTypeWeightFld) + parseWeight(ownershipWeightFld) +
                            parseWeight(damageSeverityWeightFld) + parseWeight(employmentWeightFld) +
                            parseWeight(monthlyIncomeWeightFld) + parseWeight(educationWeightFld) +
                            parseWeight(digitalAccessWeightFld) + parseWeight(dependencyRatioWeightFld);

            totalWeight = Math.round(totalWeight * 100.0) / 100.0;

            if (totalWeight != 1.0) {
                AlertDialogManager.showWarning("Validation Error",
                        "The sum of all weights must equal exactly 1.0" +
                                " Current total: " + String.format("%.2f", totalWeight) +
                                " Required total: 1.0" +
                                " Please adjust the weight values.");
                return false;
            }

            return true;

        } catch (NumberFormatException e) {
            AlertDialogManager.showWarning("Validation Error",
                    "All weight fields must contain valid numeric values.");
            return false;
        }
    }

    public void clearFields() {
        aidNameFld.setText("");
        ageWeightFld.setText("");
        genderWeightFld.setText("");
        maritalStatusWeightFld.setText("");
        soloParentWeightFld.setText("");
        disabilityWeightFld.setText("");
        healthConditionWeightFld.setText("");
        waterAccessWeightFld.setText("");
        sanitationWeightFld.setText("");
        houseTypeWeightFld.setText("");
        ownershipWeightFld.setText("");
        damageSeverityWeightFld.setText("");
        employmentWeightFld.setText("");
        monthlyIncomeWeightFld.setText("");
        educationWeightFld.setText("");
        digitalAccessWeightFld.setText("");
        dependencyRatioWeightFld.setText("");
        notesFld.setText("");
    }

    private void closeDialog() {
        Stage stage = (Stage) exitBtn.getScene().getWindow();
        stage.hide();
    }

    private void makeDraggable() {
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            Stage dialogStage = (Stage) root.getScene().getWindow();
            dialogStage.setX(event.getScreenX() - xOffset);
            dialogStage.setY(event.getScreenY() - yOffset);
        });
    }
}