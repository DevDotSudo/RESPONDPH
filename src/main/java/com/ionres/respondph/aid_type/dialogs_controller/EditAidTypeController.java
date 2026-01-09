package com.ionres.respondph.aid_type.dialogs_controller;


import com.ionres.respondph.aid_type.AidTypeController;
import com.ionres.respondph.aid_type.AidTypeModel;
import com.ionres.respondph.aid_type.AidTypeService;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class EditAidTypeController {
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
    private Button updateBtn;
    @FXML
    private Button exitBtn;

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
        EventHandler<ActionEvent> handlers = this::handleActions;

        updateBtn.setOnAction(handlers);
        exitBtn.setOnAction(handlers);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == updateBtn) {
            editAidType();
            aidTypeController.loadTable();
        }
        else if(src == exitBtn){
            closeDialog();
        }

    }

    private void editAidType() {

        if (!validateInput()) {
            return;
        }

        try {
            int adminId = SessionManager.getInstance().getCurrentAdminId();

            if (adminId <= 0) {
                AlertDialogManager.showWarning(
                        "Session Error",
                        "No admin is currently logged in."
                );
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
                AlertDialogManager.showSuccess(
                        "Success",
                        "Aid Type updated successfully."
                );
                aidTypeController.loadTable();
                clearFields();
            } else {
                AlertDialogManager.showError(
                        "Update Failed",
                        "Failed to update Aid Type."
                );
            }

        } catch (NumberFormatException e) {
            AlertDialogManager.showError(
                    "Invalid Input",
                    "Please enter valid numeric values for all weights."
            );
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError(
                    "Error",
                    "Unexpected error occurred: " + e.getMessage()
            );
        }
    }



    private  void closeDialog(){
        Stage stage = (Stage) exitBtn.getScene().getWindow();
        stage.hide();
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
