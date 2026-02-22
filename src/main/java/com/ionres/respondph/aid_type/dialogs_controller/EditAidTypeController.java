package com.ionres.respondph.aid_type.dialogs_controller;

import com.ionres.respondph.aidType_and_household_score.AidHouseholdScoreCalculate;
import com.ionres.respondph.aid_type.AidTypeController;
import com.ionres.respondph.aid_type.AidTypeModel;
import com.ionres.respondph.aid_type.AidTypeService;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.DashboardRefresher;
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

public class EditAidTypeController {

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
    @FXML private Button updateBtn;
    @FXML private Button exitBtn;

    private AidTypeService aidTypeService;
    private AidTypeController aidTypeController;
    private AidTypeModel currentAidType;
    private Stage dialogStage;

    private double xOffset = 0;
    private double yOffset = 0;

    public void setAidTypeService(AidTypeService aidTypeService) {
        this.aidTypeService = aidTypeService;
    }

    public void setAidTypeController(AidTypeController aidTypeController) {
        this.aidTypeController = aidTypeController;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setAidType(AidTypeModel aidTypeModel) {
        this.currentAidType = aidTypeModel;
        populateFields(aidTypeModel);
    }

    @FXML
    private void initialize() {
        makeDraggable();
        setupNumericValidation();
        EventHandler<ActionEvent> handlers = this::handleActions;
        updateBtn.setOnAction(handlers);
        exitBtn.setOnAction(handlers);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == updateBtn) {
            editAidType();
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

    private void editAidType() {
        if (!validateInput()) return;
        if (!validateWeightSum()) return;

        try {
            int adminId = SessionManager.getInstance().getCurrentAdminId();

            if (adminId <= 0) {
                AlertDialogManager.showWarning("Session Error", "No admin is currently logged in.");
                return;
            }

            AidTypeModel atm = new AidTypeModel();
            atm.setAidTypeId(currentAidType.getAidTypeId());
            atm.setAidTypeName(aidNameFld.getText().trim());
            atm.setNotes(notesFld.getText().trim());
            atm.setAgeWeight(Double.parseDouble(ageWeightFld.getText().trim()));
            atm.setGenderWeight(Double.parseDouble(genderWeightFld.getText().trim()));
            atm.setMaritalStatusWeight(Double.parseDouble(maritalStatusWeightFld.getText().trim()));
            atm.setSoloParentWeight(Double.parseDouble(soloParentWeightFld.getText().trim()));
            atm.setDisabilityWeight(Double.parseDouble(disabilityWeightFld.getText().trim()));
            atm.setHealthConditionWeight(Double.parseDouble(healthConditionWeightFld.getText().trim()));
            atm.setAccessToCleanWaterWeight(Double.parseDouble(waterAccessWeightFld.getText().trim()));
            atm.setSanitationFacilityWeight(Double.parseDouble(sanitationWeightFld.getText().trim()));
            atm.setHouseConstructionTypeWeight(Double.parseDouble(houseTypeWeightFld.getText().trim()));
            atm.setOwnershipWeight(Double.parseDouble(ownershipWeightFld.getText().trim()));
            atm.setDamageSeverityWeight(Double.parseDouble(damageSeverityWeightFld.getText().trim()));
            atm.setEmploymentStatusWeight(Double.parseDouble(employmentWeightFld.getText().trim()));
            atm.setMonthlyIncomeWeight(Double.parseDouble(monthlyIncomeWeightFld.getText().trim()));
            atm.setEducationalLevelWeight(Double.parseDouble(educationWeightFld.getText().trim()));
            atm.setDigitalAccessWeight(Double.parseDouble(digitalAccessWeightFld.getText().trim()));
            atm.setDependencyRatioWeight(Double.parseDouble(dependencyRatioWeightFld.getText().trim()));
            atm.setAdminId(adminId);
            atm.setRegDate(java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a")));

            boolean success = aidTypeService.updateAidType(atm);

            if (success) {
                showProgressAndRecalculate(atm.getAidTypeId(), adminId);
            } else {
                AlertDialogManager.showError("Update Failed", "Failed to update Aid Type.");
            }

        } catch (NumberFormatException e) {
            AlertDialogManager.showError("Invalid Input", "Please enter valid numeric values for all weights.");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Error", "Unexpected error occurred: " + e.getMessage());
        }
    }

    // ── Progress dialog + background Task ────────────────────────────────────
    private void showProgressAndRecalculate(int aidTypeId, int adminId) {

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
        Label subtitleLabel = new Label("Updating Aid Type");
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

        Label statusLabel = new Label("Recalculating household scores for all beneficiaries...");
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

        // Percentage label (right-aligned)
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
            pctLabel.setText(pct < 0 ? "…" : String.format("%.0f%%", pct * 100));
        });

        body.getChildren().addAll(statusLabel, barWrapper, pctRow);
        card.getChildren().addAll(header, body);

        Scene scene = new Scene(card);
        scene.setFill(null);
        progressStage.setScene(scene);

        // Center on owner stage
        Stage ownerStage = (dialogStage != null) ? dialogStage : (Stage) root.getScene().getWindow();
        if (ownerStage != null) {
            progressStage.initOwner(ownerStage);
            progressStage.setX(ownerStage.getX() + (ownerStage.getWidth()  - 420) / 2);
            progressStage.setY(ownerStage.getY() + (ownerStage.getHeight() - 160) / 2);
        }

        progressStage.show();

        // ── Disable update button while running ───────────────────────────────
        updateBtn.setDisable(true);

        // ── Run the heavy calculation off the FX thread ───────────────────────
        Task<int[]> task = new Task<>() {
            @Override
            protected int[] call() {
                updateMessage("Recalculating household scores for all beneficiaries...");

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
                            System.out.println("✓ Calculated score for beneficiary ID: " +
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
                System.out.println("Successfully calculated: " + successCount + " out of " + total);
                System.out.println("Failed: " + failCount);

                return new int[]{successCount, failCount, total};
            }
        };

        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            statusLabel.textProperty().unbind();
            progressStage.close();
            updateBtn.setDisable(false);

            int[] result = task.getValue();
            int successCount = result[0];
            int total = result[2];

            AlertDialogManager.showSuccess("Success",
                    "Aid Type updated successfully.\n" +
                            successCount + " of " + total +
                            " household score(s) recalculated.");

            aidTypeController.loadTable();
            DashboardRefresher.refreshComboBoxOfDNAndAN();
            clearFields();
        });

        task.setOnFailed(e -> {
            statusLabel.textProperty().unbind();
            progressStage.close();
            updateBtn.setDisable(false);

            Throwable ex = task.getException();
            AlertDialogManager.showWarning("Warning",
                    "Aid type updated, but there was an error recalculating household scores.\n" +
                            (ex != null ? ex.getMessage() : "Unknown error"));
            if (ex != null) ex.printStackTrace();

            aidTypeController.loadTable();
            DashboardRefresher.refreshComboBoxOfDNAndAN();
            clearFields();
        });

        Thread thread = new Thread(task, "EditAidTypeRecalculate-Thread");
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
                    "Aid type updated successfully, but there was an error recalculating household scores.\n" +
                            "You may need to recalculate manually.");
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

    private void populateFields(AidTypeModel aidType) {
        if (aidType == null) return;

        aidNameFld.setText(aidType.getAidTypeName());
        notesFld.setText(aidType.getNotes());
        ageWeightFld.setText(String.valueOf(aidType.getAgeWeight()));
        genderWeightFld.setText(String.valueOf(aidType.getGenderWeight()));
        maritalStatusWeightFld.setText(String.valueOf(aidType.getMaritalStatusWeight()));
        soloParentWeightFld.setText(String.valueOf(aidType.getSoloParentWeight()));
        disabilityWeightFld.setText(String.valueOf(aidType.getDisabilityWeight()));
        healthConditionWeightFld.setText(String.valueOf(aidType.getHealthConditionWeight()));
        waterAccessWeightFld.setText(String.valueOf(aidType.getAccessToCleanWaterWeight()));
        sanitationWeightFld.setText(String.valueOf(aidType.getSanitationFacilityWeight()));
        houseTypeWeightFld.setText(String.valueOf(aidType.getHouseConstructionTypeWeight()));
        ownershipWeightFld.setText(String.valueOf(aidType.getOwnershipWeight()));
        damageSeverityWeightFld.setText(String.valueOf(aidType.getDamageSeverityWeight()));
        employmentWeightFld.setText(String.valueOf(aidType.getEmploymentStatusWeight()));
        monthlyIncomeWeightFld.setText(String.valueOf(aidType.getMonthlyIncomeWeight()));
        educationWeightFld.setText(String.valueOf(aidType.getEducationalLevelWeight()));
        digitalAccessWeightFld.setText(String.valueOf(aidType.getDigitalAccessWeight()));
        dependencyRatioWeightFld.setText(String.valueOf(aidType.getDependencyRatioWeight()));
    }

    private boolean validateInput() {
        if (aidNameFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Aid type name is required.");
            aidNameFld.requestFocus();
            return false;
        }
        if (ageWeightFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Age weight is required.");
            ageWeightFld.requestFocus();
            return false;
        }
        if (genderWeightFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Gender weight is required.");
            genderWeightFld.requestFocus();
            return false;
        }
        if (maritalStatusWeightFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Marital status weight is required.");
            maritalStatusWeightFld.requestFocus();
            return false;
        }
        if (soloParentWeightFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Solo parent weight is required.");
            soloParentWeightFld.requestFocus();
            return false;
        }
        if (disabilityWeightFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Disability weight is required.");
            disabilityWeightFld.requestFocus();
            return false;
        }
        if (healthConditionWeightFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Health condition weight is required.");
            healthConditionWeightFld.requestFocus();
            return false;
        }
        if (waterAccessWeightFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Access to clean water weight is required.");
            waterAccessWeightFld.requestFocus();
            return false;
        }
        if (sanitationWeightFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Sanitation facility weight is required.");
            sanitationWeightFld.requestFocus();
            return false;
        }
        if (houseTypeWeightFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "House construction type weight is required.");
            houseTypeWeightFld.requestFocus();
            return false;
        }
        if (ownershipWeightFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Ownership weight is required.");
            ownershipWeightFld.requestFocus();
            return false;
        }
        if (damageSeverityWeightFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Damage severity weight is required.");
            damageSeverityWeightFld.requestFocus();
            return false;
        }
        if (employmentWeightFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Employment status weight is required.");
            employmentWeightFld.requestFocus();
            return false;
        }
        if (monthlyIncomeWeightFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Monthly income weight is required.");
            monthlyIncomeWeightFld.requestFocus();
            return false;
        }
        if (educationWeightFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Educational level weight is required.");
            educationWeightFld.requestFocus();
            return false;
        }
        if (digitalAccessWeightFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Digital access weight is required.");
            digitalAccessWeightFld.requestFocus();
            return false;
        }
        if (dependencyRatioWeightFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Dependency ratio weight is required.");
            dependencyRatioWeightFld.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validateWeightSum() {
        try {
            double totalWeight =
                    Double.parseDouble(ageWeightFld.getText().trim()) +
                            Double.parseDouble(genderWeightFld.getText().trim()) +
                            Double.parseDouble(maritalStatusWeightFld.getText().trim()) +
                            Double.parseDouble(soloParentWeightFld.getText().trim()) +
                            Double.parseDouble(disabilityWeightFld.getText().trim()) +
                            Double.parseDouble(healthConditionWeightFld.getText().trim()) +
                            Double.parseDouble(waterAccessWeightFld.getText().trim()) +
                            Double.parseDouble(sanitationWeightFld.getText().trim()) +
                            Double.parseDouble(houseTypeWeightFld.getText().trim()) +
                            Double.parseDouble(ownershipWeightFld.getText().trim()) +
                            Double.parseDouble(damageSeverityWeightFld.getText().trim()) +
                            Double.parseDouble(employmentWeightFld.getText().trim()) +
                            Double.parseDouble(monthlyIncomeWeightFld.getText().trim()) +
                            Double.parseDouble(educationWeightFld.getText().trim()) +
                            Double.parseDouble(digitalAccessWeightFld.getText().trim()) +
                            Double.parseDouble(dependencyRatioWeightFld.getText().trim());

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