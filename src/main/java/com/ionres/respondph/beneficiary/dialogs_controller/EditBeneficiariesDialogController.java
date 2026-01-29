package com.ionres.respondph.beneficiary.dialogs_controller;

import com.ionres.respondph.beneficiary.AgeScoreCalculate;
import com.ionres.respondph.beneficiary.BeneficiaryController;
import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.beneficiary.BeneficiaryService;
import com.ionres.respondph.common.controller.MappingDialogController;
import com.ionres.respondph.util.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EditBeneficiariesDialogController {

    @FXML
    private VBox root;

    @FXML
    private TextField firstNameFld;
    @FXML
    private TextField middleNameFld;
    @FXML
    private TextField lastNameFld;
    @FXML
    private DatePicker birthDatePicker;
    @FXML
    private ComboBox<String> genderSelection;
    @FXML
    private TextField mobileNumberFld;
    @FXML
    private ComboBox<String> maritalStatusSelection;
    @FXML
    private ComboBox<String> soloParentStatusSelection;
    @FXML
    private TextField latitudeFld;
    @FXML
    private TextField longitudeFld;
    @FXML
    private Button getLocationBtn;

    @FXML
    private ComboBox<String> disabilityTypeSelection;
    @FXML
    private ComboBox<String> healthConditionSelection;
    @FXML
    private ComboBox<String> cleanWaterAccessSelection;
    @FXML
    private ComboBox<String> sanitationFacilitiesSelection;
    @FXML
    private ComboBox<String> houseConstructionTypeSelection;
    @FXML
    private ComboBox<String> ownershipStatusSelection;
    @FXML
    private ComboBox<String> employmentStatusSelection;
    @FXML
    private ComboBox<String> monthlyIncomeSelection;
    @FXML
    private ComboBox<String> educationLevelSelection;
    @FXML
    private ComboBox<String> digitalAccessSelection;

    @FXML
    private Button exitBtn;
    @FXML
    private Button updateBeneficiaryBtn;
    private double yOffset = 0;
    private double xOffset = 0;
    private BeneficiaryService beneficiaryService;
    private BeneficiaryController beneficiaryController;
    private BeneficiaryModel currentBeneficiary;
    private Stage dialogStage;

    public void setBeneficiaryService(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    public void setBeneficiaryController(BeneficiaryController beneficiaryController) {
        this.beneficiaryController = beneficiaryController;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setBeneficiary(BeneficiaryModel bm) {
        this.currentBeneficiary = bm;
        populateFields(bm);
    }

    @FXML
    public void initialize() {
        makeDraggable();
        EventHandler<ActionEvent> handlers = this::handleActions;
        exitBtn.setOnAction(handlers);
        updateBeneficiaryBtn.setOnAction(handlers);
        getLocationBtn.setOnAction(handlers);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == updateBeneficiaryBtn) {
            updateBeneficiary();
            beneficiaryController.loadTable();
        } else if (src == exitBtn) {
            closeDialog();
        }
        else if(src == getLocationBtn) {
            handleGetLocationBtn();
        }
    }

    private void handleGetLocationBtn() {
        MappingDialogController controller = DialogManager.getController("mapping", MappingDialogController.class);
        controller.setListener(latLng -> {
            latitudeFld.setText(String.valueOf(latLng.lat));
            longitudeFld.setText(String.valueOf(latLng.lon));
        });
        DialogManager.show("mapping");
    }

    private void populateFields(BeneficiaryModel bm) {
        firstNameFld.setText(bm.getFirstname());
        middleNameFld.setText(bm.getMiddlename());
        lastNameFld.setText(bm.getLastname());
        mobileNumberFld.setText(bm.getMobileNumber());
        latitudeFld.setText(bm.getLatitude());
        longitudeFld.setText(bm.getLongitude());

        if (bm.getBirthDate() != null && !bm.getBirthDate().isEmpty()) {
            try {
                LocalDate birthDate = LocalDate.parse(bm.getBirthDate());
                birthDatePicker.setValue(birthDate);
            } catch (Exception e) {
                System.out.println("Error parsing date: " + e.getMessage());
            }
        }

        genderSelection.setValue(bm.getGender());
        maritalStatusSelection.setValue(bm.getMaritalStatus());
        soloParentStatusSelection.setValue(bm.getSoloParentStatus());
        disabilityTypeSelection.setValue(bm.getDisabilityType());
        healthConditionSelection.setValue(bm.getHealthCondition());
        cleanWaterAccessSelection.setValue(bm.getCleanWaterAccess());
        sanitationFacilitiesSelection.setValue(bm.getSanitationFacility());
        houseConstructionTypeSelection.setValue(bm.getHouseType());
        ownershipStatusSelection.setValue(bm.getOwnerShipStatus());
        employmentStatusSelection.setValue(bm.getEmploymentStatus());
        monthlyIncomeSelection.setValue(bm.getMonthlyIncome());
        educationLevelSelection.setValue(bm.getEducationalLevel());
        digitalAccessSelection.setValue(bm.getDigitalAccess());
    }

    private void updateBeneficiary() {
        try {
            String firstname = firstNameFld.getText().trim();
            String middlename = middleNameFld.getText().trim();
            String lastname = lastNameFld.getText().trim();
            String birthDate = birthDatePicker.getValue() != null
                    ? birthDatePicker.getValue().toString()
                    : "";
            double ageScore = AgeScoreCalculate.calculateAgeScoreFromBirthdate(birthDate);
            String gender = genderSelection.getValue();
            String mobileNumber = mobileNumberFld.getText().trim();
            String maritalStatus = maritalStatusSelection.getValue();
            String soloParentStatus = soloParentStatusSelection.getValue();
            String latitude = latitudeFld.getText().trim();
            String longitude = longitudeFld.getText().trim();
            String disabilityType = disabilityTypeSelection.getValue();
            String healthCondition = healthConditionSelection.getValue();
            String cleanWaterAccess = cleanWaterAccessSelection.getValue();
            String sanitationFacility = sanitationFacilitiesSelection.getValue();
            String houseType = houseConstructionTypeSelection.getValue();
            String ownershipStatus = ownershipStatusSelection.getValue();
            String employmentStatus = employmentStatusSelection.getValue();
            String monthlyIncome = monthlyIncomeSelection.getValue();
            String educationalLevel = educationLevelSelection.getValue();
            String digitalAccess = digitalAccessSelection.getValue();
            String addedBy = SessionManager.getInstance().getCurrentAdminFirstName();
            String regDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            if (firstname.isEmpty()) {
                AlertDialogManager.showWarning("Warning","First name is required");
                return;
            }
            if (middlename.isEmpty()) {
                AlertDialogManager.showWarning("Warning","Middle name is required");
                return;
            }
            if (lastname.isEmpty()) {
                AlertDialogManager.showWarning("Warning","Last name is required");
                return;
            }
            if (birthDate.isEmpty()) {
                AlertDialogManager.showWarning("Warning","Birth date is required");
                return;
            }
            if (gender == null) {
                AlertDialogManager.showWarning("Warning","Gender is required");
                return;
            }
            if (mobileNumber.isEmpty()) {
                AlertDialogManager.showWarning("Warning","Mobile number is required");
                return;
            }
            if (maritalStatus == null) {
                AlertDialogManager.showWarning("Warning","Marital status is required");
                return;
            }
            if (soloParentStatus == null) {
                AlertDialogManager.showWarning("Warning","Solo parent status is required");
                return;
            }
            if (latitude.isEmpty()) {
                AlertDialogManager.showWarning("Warning","Latitude is required");
                return;
            }
            if (longitude.isEmpty()) {
                AlertDialogManager.showWarning("Warning","Longitude is required");
                return;
            }
            if (disabilityType == null) {
                AlertDialogManager.showWarning("Warning","Disability type is required");
                return;
            }
            if (healthCondition == null) {
                AlertDialogManager.showWarning("Warning","Health condition is required");
                return;
            }
            if (cleanWaterAccess == null) {
                AlertDialogManager.showWarning("Warning","Clean water access is required");
                return;
            }
            if (sanitationFacility == null) {
                AlertDialogManager.showWarning("Warning","Sanitation facility is required");
                return;
            }
            if (houseType == null) {
                AlertDialogManager.showWarning("Warning","House type is required");
                return;
            }
            if (ownershipStatus == null) {
                AlertDialogManager.showWarning("Warning","Ownership status is required");
                return;
            }
            if (employmentStatus == null) {
                AlertDialogManager.showWarning("Warning","Employment status is required");
                return;
            }
            if (monthlyIncome == null) {
                AlertDialogManager.showWarning("Warning","Monthly income is required");
                return;
            }
            if (educationalLevel == null) {
                AlertDialogManager.showWarning("Warning","Educational level is required");
                return;
            }
            if (digitalAccess == null) {
                AlertDialogManager.showWarning("Warning","Digital access is required");
                return;
            }

            BeneficiaryModel updatedBm = new BeneficiaryModel(
                    firstname, middlename, lastname, birthDate,ageScore, gender,
                    maritalStatus, soloParentStatus, latitude, longitude,
                    mobileNumber, disabilityType, healthCondition, cleanWaterAccess,
                    sanitationFacility, houseType, ownershipStatus, employmentStatus,
                    monthlyIncome, educationalLevel, digitalAccess, addedBy,
                    regDate
            );

            updatedBm.setId(currentBeneficiary.getId());

            boolean success = beneficiaryService.updateBeneficiary(updatedBm);

            if (success) {
                UpdateTrigger trigger = new UpdateTrigger();
                boolean cascadeSuccess = trigger.triggerCascadeUpdate(currentBeneficiary.getId());

                if (cascadeSuccess) {
                    AlertDialogManager.showSuccess("Success",
                            "Beneficiary updated successfully!\n" +
                                    "Household scores and aid scores have been recalculated.");
                } else {
                    AlertDialogManager.showWarning("Partial Success",
                            "Beneficiary updated, but some scores failed to recalculate.");
                }
            } else {
                AlertDialogManager.showError("Error", "Failed to update beneficiary.");
            }
            dialogStage.hide();
        } catch (Exception e) {
            AlertDialogManager.showError("Error", e.getMessage());
            e.printStackTrace();
        }
    }

    private void makeDraggable() {
        root.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });

        root.setOnMouseDragged(e -> {
            if (dialogStage != null) {
                dialogStage.setX(e.getScreenX() - xOffset);
                dialogStage.setY(e.getScreenY() - yOffset);
            }
        });
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.hide();
        }
    }

    public void clearFields() {
        firstNameFld.setText("");
        middleNameFld.setText("");
        lastNameFld.setText("");
        mobileNumberFld.setText("");
        latitudeFld.setText("");
        longitudeFld.setText("");
        birthDatePicker.setValue(null);
        genderSelection.getSelectionModel().clearSelection();
        maritalStatusSelection.getSelectionModel().clearSelection();
        soloParentStatusSelection.getSelectionModel().clearSelection();
        disabilityTypeSelection.getSelectionModel().clearSelection();
        healthConditionSelection.getSelectionModel().clearSelection();
        cleanWaterAccessSelection.getSelectionModel().clearSelection();
        sanitationFacilitiesSelection.getSelectionModel().clearSelection();
        houseConstructionTypeSelection.getSelectionModel().clearSelection();
        ownershipStatusSelection.getSelectionModel().clearSelection();
        employmentStatusSelection.getSelectionModel().clearSelection();
        monthlyIncomeSelection.getSelectionModel().clearSelection();
        educationLevelSelection.getSelectionModel().clearSelection();
        digitalAccessSelection.getSelectionModel().clearSelection();
    }
}