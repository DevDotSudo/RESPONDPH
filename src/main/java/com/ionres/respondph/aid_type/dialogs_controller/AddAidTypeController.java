package com.ionres.respondph.aid_type.dialogs_controller;


import com.ionres.respondph.aid_type.AidTypeController;
import com.ionres.respondph.aid_type.AidTypeModel;
import com.ionres.respondph.aid_type.AidTypeService;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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


    private void addAidType() {
        try {
            if (!validateInput()) return;
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
                AlertDialogManager.showSuccess("Success", "Aid Type added successfully!");
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
