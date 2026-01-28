//
//package com.ionres.respondph.aid_type.dialogs_controller;
//
//import com.ionres.respondph.aidType_and_household_score.AidHouseholdScoreCalculate;
//import com.ionres.respondph.aid_type.AidTypeController;
//import com.ionres.respondph.aid_type.AidTypeModel;
//import com.ionres.respondph.aid_type.AidTypeModelComboBox;
//import com.ionres.respondph.aid_type.AidTypeService;
//import com.ionres.respondph.util.AlertDialogManager;
//import com.ionres.respondph.util.DashboardRefresher;
//import com.ionres.respondph.util.SessionManager;
//import javafx.collections.FXCollections;
//import javafx.event.ActionEvent;
//import javafx.event.EventHandler;
//import javafx.fxml.FXML;
//import javafx.scene.control.*;
//import javafx.scene.layout.VBox;
//import javafx.stage.Stage;
//
//import java.util.List;
//import java.util.function.UnaryOperator;
//
//public class AddAidTypeController {
//
//    @FXML
//    private VBox root;
//    @FXML
//    private TextField aidNameFld;
//    @FXML
//    private TextField ageWeightFld;
//    @FXML
//    private TextField genderWeightFld;
//    @FXML
//    private TextField maritalStatusWeightFld;
//    @FXML
//    private TextField soloParentWeightFld;
//    @FXML
//    private TextField disabilityWeightFld;
//    @FXML
//    private TextField healthConditionWeightFld;
//    @FXML
//    private TextField waterAccessWeightFld;
//    @FXML
//    private TextField sanitationWeightFld;
//    @FXML
//    private TextField houseTypeWeightFld;
//    @FXML
//    private TextField ownershipWeightFld;
//    @FXML
//    private TextField damageSeverityWeightFld;
//    @FXML
//    private TextField employmentWeightFld;
//    @FXML
//    private TextField monthlyIncomeWeightFld;
//    @FXML
//    private TextField educationWeightFld;
//    @FXML
//    private TextField digitalAccessWeightFld;
//    @FXML
//    private TextField dependencyRatioWeightFld;
//    @FXML
//    private TextArea notesFld;
//
//    @FXML
//    private Button saveBtn;
//    @FXML
//    private Button exitBtn;
//    private double xOffset = 0;
//    private double yOffset = 0;
//
//    private AidTypeService aidTypeService;
//    private AidTypeController aidTypeController;
//
//    public void setAidTypeService(AidTypeService aidTypeService) {
//        this.aidTypeService = aidTypeService;
//    }
//
//    public void setAidTypeController(AidTypeController aidTypeController) {
//        this.aidTypeController = aidTypeController;
//    }
//
//    @FXML
//    private void initialize() {
//        makeDraggable();
//        setupNumericValidation();
//        EventHandler<ActionEvent> handlers = this::handleActions;
//
//        saveBtn.setOnAction(handlers);
//        exitBtn.setOnAction(handlers);
//    }
//
//    private void handleActions(ActionEvent event) {
//        Object src = event.getSource();
//
//        if (src == saveBtn) {
//            addAidType();
//            aidTypeController.loadTable();
//        }
//        else if(src == exitBtn){
//            closeDialog();
//        }
//    }
//
//    private void setupNumericValidation() {
//        TextField[] weightFields = {
//                ageWeightFld, genderWeightFld, maritalStatusWeightFld, soloParentWeightFld,
//                disabilityWeightFld, healthConditionWeightFld, waterAccessWeightFld, sanitationWeightFld,
//                houseTypeWeightFld, ownershipWeightFld, damageSeverityWeightFld, employmentWeightFld,
//                monthlyIncomeWeightFld, educationWeightFld, digitalAccessWeightFld, dependencyRatioWeightFld
//        };
//
//        for (TextField field : weightFields) {
//            if (field != null) {
//                setNumericWeightFilter(field);
//            }
//        }
//    }
//
//    private void setNumericWeightFilter(TextField textField) {
//        UnaryOperator<TextFormatter.Change> filter = change -> {
//            String newText = change.getControlNewText();
//
//            if (newText.isEmpty()) {
//                return change;
//            }
//
//            if (newText.matches("-?\\d*(\\.\\d*)?")) {
//                int decimalCount = newText.length() - newText.replace(".", "").length();
//                if (decimalCount <= 1) {
//                    return change;
//                }
//            }
//
//            return null;
//        };
//
//        textField.setTextFormatter(new TextFormatter<>(filter));
//    }
//
//
//
//
//    private void addAidType() {
//        try {
//            if (!validateInput()) return;
//
//            double ageWeight = Double.parseDouble(ageWeightFld.getText().trim());
//            double genderWeight = Double.parseDouble(genderWeightFld.getText().trim());
//            double maritalStatusWeight = Double.parseDouble(maritalStatusWeightFld.getText().trim());
//            double soloParentWeight = Double.parseDouble(soloParentWeightFld.getText().trim());
//            double disabilityWeight = Double.parseDouble(disabilityWeightFld.getText().trim());
//            double healthConditionWeight = Double.parseDouble(healthConditionWeightFld.getText().trim());
//            double accessToCleanWaterWeight = Double.parseDouble(waterAccessWeightFld.getText().trim());
//            double sanitationFacilityWeight = Double.parseDouble(sanitationWeightFld.getText().trim());
//            double houseConstructionTypeWeight = Double.parseDouble(houseTypeWeightFld.getText().trim());
//            double ownershipWeight = Double.parseDouble(ownershipWeightFld.getText().trim());
//            double damageSeverityWeight = Double.parseDouble(damageSeverityWeightFld.getText().trim());
//            double employmentStatusWeight = Double.parseDouble(employmentWeightFld.getText().trim());
//            double monthlyIncomeWeight = Double.parseDouble(monthlyIncomeWeightFld.getText().trim());
//            double educationalLevelWeight = Double.parseDouble(educationWeightFld.getText().trim());
//            double digitalAccessWeight = Double.parseDouble(digitalAccessWeightFld.getText().trim());
//            double dependencyRatioWeight = Double.parseDouble(dependencyRatioWeightFld.getText().trim());
//
//            String aidTypeName = aidNameFld.getText().trim();
//            String notes = notesFld.getText().trim();
//
//            int adminId = SessionManager.getInstance().getCurrentAdminId();
//            if (adminId <= 0) {
//                AlertDialogManager.showWarning("Warning","No admin logged in");
//                return;
//            }
//
//            String regDate = java.time.LocalDateTime.now()
//                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));
//
//            AidTypeModel aidTypeModel = new AidTypeModel(
//                    aidTypeName,
//                    ageWeight,
//                    genderWeight,
//                    maritalStatusWeight,
//                    soloParentWeight,
//                    disabilityWeight,
//                    healthConditionWeight,
//                    accessToCleanWaterWeight,
//                    sanitationFacilityWeight,
//                    houseConstructionTypeWeight,
//                    ownershipWeight,
//                    damageSeverityWeight,
//                    employmentStatusWeight,
//                    monthlyIncomeWeight,
//                    educationalLevelWeight,
//                    digitalAccessWeight,
//                    dependencyRatioWeight,
//                    notes,
//                    adminId,
//                    regDate
//            );
//
//            boolean success = aidTypeService.createAidType(aidTypeModel);
//
//            if (success) {
//                // Get the newly created aid type ID
//                int newAidTypeId = getLatestAidTypeId();
//
//                if (newAidTypeId > 0) {
//                    // Automatically calculate aid-household scores for all beneficiaries
//                    recalculateAllBeneficiaryScores(newAidTypeId, adminId);
//                }
//
//                AlertDialogManager.showSuccess("Success",
//                        "Aid Type added successfully!\n" +
//                                "Household scores have been calculated for all beneficiaries.");
//
//                DashboardRefresher.refreshComboBoxOfDNAndAN();
//                clearFields();
//            } else {
//                AlertDialogManager.showWarning("Error", "Failed to add Aid Type.");
//            }
//
//        } catch (Exception e) {
//            javax.swing.JOptionPane.showMessageDialog(
//                    null,
//                    e.getMessage(),
//                    "Error",
//                    javax.swing.JOptionPane.ERROR_MESSAGE
//            );
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Gets the ID of the most recently created aid type
//     */
//    private int getLatestAidTypeId() {
//        try {
//            java.util.List<AidTypeModel> allAidTypes = aidTypeService.getAllAidType();
//            if (allAidTypes != null && !allAidTypes.isEmpty()) {
//                // Get the last aid type (most recently added)
//                return allAidTypes.get(allAidTypes.size() - 1).getAidTypeId();
//            }
//        } catch (Exception e) {
//            System.err.println("Error getting latest aid type ID: " + e.getMessage());
//            e.printStackTrace();
//        }
//        return -1;
//    }
//
//
//    private void recalculateAllBeneficiaryScores(int aidTypeId, int adminId) {
//        try {
//            System.out.println("========== RECALCULATING AID-HOUSEHOLD SCORES ==========");
//            System.out.println("Aid Type ID: " + aidTypeId);
//            System.out.println("Admin ID: " + adminId);
//
//            // Get all beneficiary IDs that have household scores
//            java.util.List<Integer> beneficiaryIds = getAllBeneficiaryIdsWithHouseholdScores();
//
//            System.out.println("Found " + beneficiaryIds.size() + " beneficiaries with household scores");
//
//            AidHouseholdScoreCalculate calculator = new AidHouseholdScoreCalculate();
//            int successCount = 0;
//            int failCount = 0;
//
//            for (Integer beneficiaryId : beneficiaryIds) {
//                try {
//                    boolean success = calculator.calculateAndSaveAidHouseholdScore(
//                            beneficiaryId,
//                            aidTypeId,
//                            adminId
//                    );
//
//                    if (success) {
//                        successCount++;
//                        System.out.println("✓ Calculated aid-household score for beneficiary ID: " + beneficiaryId);
//                    } else {
//                        failCount++;
//                        System.err.println("✗ Failed to calculate aid-household score for beneficiary ID: " + beneficiaryId);
//                    }
//                } catch (Exception e) {
//                    failCount++;
//                    System.err.println("✗ Error calculating aid-household score for beneficiary ID " +
//                            beneficiaryId + ": " + e.getMessage());
//                }
//            }
//
//            System.out.println("========== RECALCULATION COMPLETE ==========");
//            System.out.println("Successfully calculated: " + successCount + " out of " + beneficiaryIds.size());
//            System.out.println("Failed: " + failCount);
//
//            if (failCount > 0) {
//                AlertDialogManager.showWarning("Partial Success",
//                        "Aid type created successfully.\n" +
//                                "Calculated scores for " + successCount + " beneficiaries.\n" +
//                                "Failed to calculate scores for " + failCount + " beneficiaries.");
//            }
//
//        } catch (Exception e) {
//            System.err.println("Error recalculating all beneficiary scores: " + e.getMessage());
//            e.printStackTrace();
//            AlertDialogManager.showWarning("Warning",
//                    "Aid type created successfully, but there was an error calculating household scores.\n" +
//                            "You may need to recalculate manually.");
//        }
//    }
//
//
//    private java.util.List<Integer> getAllBeneficiaryIdsWithHouseholdScores() {
//        java.util.List<Integer> beneficiaryIds = new java.util.ArrayList<>();
//        String sql = "SELECT DISTINCT beneficiary_id FROM household_score";
//
//        java.sql.Connection conn = null;
//        try {
//            conn = com.ionres.respondph.database.DBConnection.getInstance().getConnection();
//            java.sql.PreparedStatement ps = conn.prepareStatement(sql);
//            java.sql.ResultSet rs = ps.executeQuery();
//
//            while (rs.next()) {
//                beneficiaryIds.add(rs.getInt("beneficiary_id"));
//            }
//
//            rs.close();
//            ps.close();
//
//        } catch (java.sql.SQLException e) {
//            System.err.println("Error fetching beneficiary IDs: " + e.getMessage());
//            e.printStackTrace();
//        } finally {
//            try {
//                if (conn != null && !conn.isClosed()) {
//                    conn.close();
//                }
//            } catch (java.sql.SQLException e) {
//                System.err.println("Error closing connection: " + e.getMessage());
//            }
//        }
//
//        return beneficiaryIds;
//    }
//
//    private boolean validateInput() {
//        if (aidNameFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "Aid type name is required.");
//            aidNameFld.requestFocus();
//            return false;
//        }
//
//        if (ageWeightFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "Age weight is required.");
//            ageWeightFld.requestFocus();
//            return false;
//        }
//
//        if (genderWeightFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "Gender weight is required.");
//            genderWeightFld.requestFocus();
//            return false;
//        }
//
//        if (maritalStatusWeightFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "Marital status weight is required.");
//            maritalStatusWeightFld.requestFocus();
//            return false;
//        }
//
//        if (soloParentWeightFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "Solo parent weight is required.");
//            soloParentWeightFld.requestFocus();
//            return false;
//        }
//
//        if (disabilityWeightFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "Disability weight is required.");
//            disabilityWeightFld.requestFocus();
//            return false;
//        }
//
//        if (healthConditionWeightFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "Health condition weight is required.");
//            healthConditionWeightFld.requestFocus();
//            return false;
//        }
//
//        if (waterAccessWeightFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "Access to clean water weight is required.");
//            waterAccessWeightFld.requestFocus();
//            return false;
//        }
//
//        if (sanitationWeightFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "Sanitation facility weight is required.");
//            sanitationWeightFld.requestFocus();
//            return false;
//        }
//
//        if (houseTypeWeightFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "House construction type weight is required.");
//            houseTypeWeightFld.requestFocus();
//            return false;
//        }
//
//        if (ownershipWeightFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "Ownership weight is required.");
//            ownershipWeightFld.requestFocus();
//            return false;
//        }
//
//        if (damageSeverityWeightFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "Damage severity weight is required.");
//            damageSeverityWeightFld.requestFocus();
//            return false;
//        }
//
//        if (employmentWeightFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "Employment status weight is required.");
//            employmentWeightFld.requestFocus();
//            return false;
//        }
//
//        if (monthlyIncomeWeightFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "Monthly income weight is required.");
//            monthlyIncomeWeightFld.requestFocus();
//            return false;
//        }
//
//        if (educationWeightFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "Educational level weight is required.");
//            educationWeightFld.requestFocus();
//            return false;
//        }
//
//        if (digitalAccessWeightFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "Digital access weight is required.");
//            digitalAccessWeightFld.requestFocus();
//            return false;
//        }
//
//        if (dependencyRatioWeightFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "Dependency ratio weight is required.");
//            dependencyRatioWeightFld.requestFocus();
//            return false;
//        }
//
//        if (notesFld.getText().trim().isEmpty()) {
//            AlertDialogManager.showWarning("Validation Error", "Notes are required.");
//            notesFld.requestFocus();
//            return false;
//        }
//
//        return true;
//    }
//
//    public void clearFields() {
//        aidNameFld.setText("");
//        ageWeightFld.setText("");
//        genderWeightFld.setText("");
//        maritalStatusWeightFld.setText("");
//        soloParentWeightFld.setText("");
//        disabilityWeightFld.setText("");
//        healthConditionWeightFld.setText("");
//        waterAccessWeightFld.setText("");
//        sanitationWeightFld.setText("");
//        houseTypeWeightFld.setText("");
//        ownershipWeightFld.setText("");
//        damageSeverityWeightFld.setText("");
//        employmentWeightFld.setText("");
//        monthlyIncomeWeightFld.setText("");
//        educationWeightFld.setText("");
//        digitalAccessWeightFld.setText("");
//        dependencyRatioWeightFld.setText("");
//        notesFld.setText("");
//    }
//
//    private void closeDialog() {
//        Stage stage = (Stage) exitBtn.getScene().getWindow();
//        stage.hide();
//    }
//
//    private void makeDraggable() {
//        root.setOnMousePressed(event -> {
//            xOffset = event.getSceneX();
//            yOffset = event.getSceneY();
//        });
//
//        root.setOnMouseDragged(event -> {
//            Stage dialogStage = (Stage) root.getScene().getWindow();
//            dialogStage.setX(event.getScreenX() - xOffset);
//            dialogStage.setY(event.getScreenY() - yOffset);
//        });
//    }
//}


package com.ionres.respondph.aid_type.dialogs_controller;

import com.ionres.respondph.aidType_and_household_score.AidHouseholdScoreCalculate;
import com.ionres.respondph.aid_type.AidTypeController;
import com.ionres.respondph.aid_type.AidTypeModel;
import com.ionres.respondph.aid_type.AidTypeService;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.DashboardRefresher;
import com.ionres.respondph.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.UnaryOperator;

public class AddAidTypeController {

    @FXML
    private VBox root;
    @FXML
    private TextField aidNameFld;
    @FXML
    private TextField ageWeightFld;
    @FXML
    private TextField genderWeightFld;
    @FXML
    private TextField maritalStatusWeightFld;
    @FXML
    private TextField soloParentWeightFld;
    @FXML
    private TextField disabilityWeightFld;
    @FXML
    private TextField healthConditionWeightFld;
    @FXML
    private TextField waterAccessWeightFld;
    @FXML
    private TextField sanitationWeightFld;
    @FXML
    private TextField houseTypeWeightFld;
    @FXML
    private TextField ownershipWeightFld;
    @FXML
    private TextField damageSeverityWeightFld;
    @FXML
    private TextField employmentWeightFld;
    @FXML
    private TextField monthlyIncomeWeightFld;
    @FXML
    private TextField educationWeightFld;
    @FXML
    private TextField digitalAccessWeightFld;
    @FXML
    private TextField dependencyRatioWeightFld;
    @FXML
    private TextArea notesFld;

    @FXML
    private Button saveBtn;
    @FXML
    private Button exitBtn;
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
            aidTypeController.loadTable();
        }
        else if(src == exitBtn){
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
            if (!validateInput()) return;
            if (!validateWeightSum()) return;

            double ageWeight = Double.parseDouble(ageWeightFld.getText().trim());
            double genderWeight = Double.parseDouble(genderWeightFld.getText().trim());
            double maritalStatusWeight = Double.parseDouble(maritalStatusWeightFld.getText().trim());
            double soloParentWeight = Double.parseDouble(soloParentWeightFld.getText().trim());
            double disabilityWeight = Double.parseDouble(disabilityWeightFld.getText().trim());
            double healthConditionWeight = Double.parseDouble(healthConditionWeightFld.getText().trim());
            double accessToCleanWaterWeight = Double.parseDouble(waterAccessWeightFld.getText().trim());
            double sanitationFacilityWeight = Double.parseDouble(sanitationWeightFld.getText().trim());
            double houseConstructionTypeWeight = Double.parseDouble(houseTypeWeightFld.getText().trim());
            double ownershipWeight = Double.parseDouble(ownershipWeightFld.getText().trim());
            double damageSeverityWeight = Double.parseDouble(damageSeverityWeightFld.getText().trim());
            double employmentStatusWeight = Double.parseDouble(employmentWeightFld.getText().trim());
            double monthlyIncomeWeight = Double.parseDouble(monthlyIncomeWeightFld.getText().trim());
            double educationalLevelWeight = Double.parseDouble(educationWeightFld.getText().trim());
            double digitalAccessWeight = Double.parseDouble(digitalAccessWeightFld.getText().trim());
            double dependencyRatioWeight = Double.parseDouble(dependencyRatioWeightFld.getText().trim());

            String aidTypeName = aidNameFld.getText().trim();
            String notes = notesFld.getText().trim();

            int adminId = SessionManager.getInstance().getCurrentAdminId();
            if (adminId <= 0) {
                AlertDialogManager.showWarning("Warning","No admin logged in");
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
                    recalculateAllBeneficiaryScores(newAidTypeId, adminId);
                }

                AlertDialogManager.showSuccess("Success",
                        "Aid Type added successfully!\n" +
                                "Household scores have been calculated for all beneficiaries.");

                DashboardRefresher.refreshComboBoxOfDNAndAN();
                clearFields();
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


    private int getLatestAidTypeId() {
        try {
            java.util.List<AidTypeModel> allAidTypes = aidTypeService.getAllAidType();
            if (allAidTypes != null && !allAidTypes.isEmpty()) {
                // Get the last aid type (most recently added)
                return allAidTypes.get(allAidTypes.size() - 1).getAidTypeId();
            }
        } catch (Exception e) {
            System.err.println("Error getting latest aid type ID: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    private void recalculateAllBeneficiaryScores(int aidTypeId, int adminId) {
        try {
            System.out.println("========== RECALCULATING AID-HOUSEHOLD SCORES ==========");
            System.out.println("Aid Type ID: " + aidTypeId);
            System.out.println("Admin ID: " + adminId);

            // ✅ NEW: Get all beneficiary-disaster pairs that have household scores
            List<BeneficiaryDisasterPair> beneficiaryDisasterPairs =
                    getAllBeneficiaryDisasterPairsWithHouseholdScores();

            System.out.println("Found " + beneficiaryDisasterPairs.size() +
                    " beneficiary-disaster pairs with household scores");

            AidHouseholdScoreCalculate calculator = new AidHouseholdScoreCalculate();
            int successCount = 0;
            int failCount = 0;

            for (BeneficiaryDisasterPair pair : beneficiaryDisasterPairs) {
                try {
                    boolean success = calculator.calculateAndSaveAidHouseholdScoreWithDisaster(
                            pair.beneficiaryId,
                            aidTypeId,
                            adminId,
                            pair.disasterId  // ✅ NEW
                    );

                    if (success) {
                        successCount++;
                        System.out.println("✓ Calculated aid-household score for beneficiary ID: " +
                                pair.beneficiaryId + ", disaster ID: " + pair.disasterId);
                    } else {
                        failCount++;
                        System.err.println("✗ Failed to calculate aid-household score for beneficiary ID: " +
                                pair.beneficiaryId + ", disaster ID: " + pair.disasterId);
                    }
                } catch (Exception e) {
                    failCount++;
                    System.err.println("✗ Error calculating aid-household score for beneficiary ID " +
                            pair.beneficiaryId + ", disaster ID " + pair.disasterId + ": " + e.getMessage());
                }
            }

            System.out.println("========== RECALCULATION COMPLETE ==========");
            System.out.println("Successfully calculated: " + successCount + " out of " +
                    beneficiaryDisasterPairs.size());
            System.out.println("Failed: " + failCount);

            if (failCount > 0) {
                AlertDialogManager.showWarning("Partial Success",
                        "Aid type created successfully.\n" +
                                "Calculated scores for " + successCount + " beneficiary-disaster pairs.\n" +
                                "Failed to calculate scores for " + failCount + " pairs.");
            }

        } catch (Exception e) {
            System.err.println("Error recalculating all beneficiary scores: " + e.getMessage());
            e.printStackTrace();
            AlertDialogManager.showWarning("Warning",
                    "Aid type created successfully, but there was an error calculating household scores.\n" +
                            "You may need to recalculate manually.");
        }
    }

    private static class BeneficiaryDisasterPair {
        int beneficiaryId;
        int disasterId;

        BeneficiaryDisasterPair(int beneficiaryId, int disasterId) {
            this.beneficiaryId = beneficiaryId;
            this.disasterId = disasterId;
        }
    }

    private java.util.List<BeneficiaryDisasterPair> getAllBeneficiaryDisasterPairsWithHouseholdScores() {
        java.util.List<BeneficiaryDisasterPair> pairs = new java.util.ArrayList<>();
        // ✅ MODIFIED: Select both beneficiary_id and disaster_id
        String sql = "SELECT DISTINCT beneficiary_id, disaster_id FROM household_score";

        java.sql.Connection conn = null;
        try {
            conn = com.ionres.respondph.database.DBConnection.getInstance().getConnection();
            java.sql.PreparedStatement ps = conn.prepareStatement(sql);
            java.sql.ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int beneficiaryId = rs.getInt("beneficiary_id");
                int disasterId = rs.getInt("disaster_id");
                pairs.add(new BeneficiaryDisasterPair(beneficiaryId, disasterId));
            }

            rs.close();
            ps.close();

        } catch (java.sql.SQLException e) {
            System.err.println("Error fetching beneficiary-disaster pairs: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (java.sql.SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }

        return pairs;
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

        if (notesFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Notes are required.");
            notesFld.requestFocus();
            return false;
        }

        return true;
    }
    private boolean validateWeightSum() {
        try {
            double ageWeight = Double.parseDouble(ageWeightFld.getText().trim());
            double genderWeight = Double.parseDouble(genderWeightFld.getText().trim());
            double maritalStatusWeight = Double.parseDouble(maritalStatusWeightFld.getText().trim());
            double soloParentWeight = Double.parseDouble(soloParentWeightFld.getText().trim());
            double disabilityWeight = Double.parseDouble(disabilityWeightFld.getText().trim());
            double healthConditionWeight = Double.parseDouble(healthConditionWeightFld.getText().trim());
            double accessToCleanWaterWeight = Double.parseDouble(waterAccessWeightFld.getText().trim());
            double sanitationFacilityWeight = Double.parseDouble(sanitationWeightFld.getText().trim());
            double houseConstructionTypeWeight = Double.parseDouble(houseTypeWeightFld.getText().trim());
            double ownershipWeight = Double.parseDouble(ownershipWeightFld.getText().trim());
            double damageSeverityWeight = Double.parseDouble(damageSeverityWeightFld.getText().trim());
            double employmentStatusWeight = Double.parseDouble(employmentWeightFld.getText().trim());
            double monthlyIncomeWeight = Double.parseDouble(monthlyIncomeWeightFld.getText().trim());
            double educationalLevelWeight = Double.parseDouble(educationWeightFld.getText().trim());
            double digitalAccessWeight = Double.parseDouble(digitalAccessWeightFld.getText().trim());
            double dependencyRatioWeight = Double.parseDouble(dependencyRatioWeightFld.getText().trim());

            double totalWeight = ageWeight + genderWeight + maritalStatusWeight + soloParentWeight +
                    disabilityWeight + healthConditionWeight + accessToCleanWaterWeight + sanitationFacilityWeight +
                    houseConstructionTypeWeight + ownershipWeight + damageSeverityWeight + employmentStatusWeight +
                    monthlyIncomeWeight + educationalLevelWeight + digitalAccessWeight + dependencyRatioWeight;

            totalWeight = Math.round(totalWeight * 100.0) / 100.0;

            if (totalWeight != 1.0) {
                AlertDialogManager.showWarning("Validation Error",
                        "The sum of all weights must equal exactly 1.0" +
                                " Current total: " + String.format("%.2f", totalWeight)+
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