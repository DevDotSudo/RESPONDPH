package com.ionres.respondph.beneficiary.dialogs_controller;

import com.ionres.respondph.beneficiary.BeneficiaryController;
import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.beneficiary.BeneficiaryService;
import com.ionres.respondph.util.CustomAlertDialog;
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

public class EditBeneficiariesDialogController {

    CustomAlertDialog customAlertDialog = new CustomAlertDialog();
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
        initializeBeneficiaryProfileDropdowns();
        initializeVulnerabilityIndicatorsDropdowns();
        initializeHousingAndInfrastructureDropdowns();
        initializeSocioEconomicStatusDropdowns();
        makeDraggable();
        EventHandler<ActionEvent> handlers = this::handleActions;
        exitBtn.setOnAction(handlers);
        updateBeneficiaryBtn.setOnAction(handlers);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == updateBeneficiaryBtn) {
            updateBeneficiary();
            beneficiaryController.loadTable();
        } else if (src == exitBtn) {
            closeDialog();
        }
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

    private void initializeBeneficiaryProfileDropdowns() {
        genderSelection.getItems().addAll("Male", "Female", "Other");

        maritalStatusSelection.getItems().addAll(
                "Single",
                "Married",
                "Widowed",
                "Separated",
                "Divorced"
        );

        soloParentStatusSelection.getItems().addAll("Yes", "No");
    }

    private void initializeVulnerabilityIndicatorsDropdowns() {
        disabilityTypeSelection.getItems().addAll(
                "None",
                "Physical",
                "Visual",
                "Hearing",
                "Speech",
                "Intellectual",
                "Mental/Psychosocial",
                "Due to Chronic Illness"
        );

        healthConditionSelection.getItems().addAll(
                "Healthy",
                "Temporarily ill",
                "Chronically ill",
                "Immunocompromised",
                "Terminally ill",
                "With History of Hospitalization/ Long-term Medical Equipment Dependency"
        );

        cleanWaterAccessSelection.getItems().addAll(
                "Yes",
                "Occasionally",
                "No"
        );

        sanitationFacilitiesSelection.getItems().addAll(
                "Safely managed private toilet",
                "Shared sanitation facility",
                "Unimproved sanitation facility",
                "No sanitation facility available"
        );
    }

    private void initializeHousingAndInfrastructureDropdowns() {
        houseConstructionTypeSelection.getItems().addAll(
                "Reinforced concrete or masonry",
                "Light materials (bamboo, nipa, thatch, cogon)",
                "Semi-concrete with light roofing (GI, asbestos)",
                "Makeshift shelter (wood, tarpaulin)"
        );

        ownershipStatusSelection.getItems().addAll(
                "Owned with formal title",
                "Owned without formal title",
                "Rented",
                "Informal settler",
                "Evicted or displaced"
        );
    }

    private void initializeSocioEconomicStatusDropdowns() {
        employmentStatusSelection.getItems().addAll(
                "Regular full-time employment",
                "Self-employed with stable income",
                "Self-employed with unstable income",
                "Irregular employment (odd jobs, seasonal work)",
                "Unemployed"
        );

        monthlyIncomeSelection.getItems().addAll(
                "12,030-30,000(Poor)",
                "12,030-24,480(Low-Income)",
                "24,061-84,120 (Lower Middle Income)",
                "84,121-144,210(Middle Class)",
                "144,211-244,350(Upper Middle Income)",
                "At least 244,350(Rich)"
        );

        educationLevelSelection.getItems().addAll(
                "No Formal Education",
                "Elementary",
                "High School",
                "Vocational or technical training",
                "College or university level",
                "Graduate education"
        );

        digitalAccessSelection.getItems().addAll(
                "Reliable Internet and Device Access",
                "Intermittent internet or device access",
                "Device only",
                "No digital access"
        );
    }

    private void updateBeneficiary() {
        try {
            String firstname = firstNameFld.getText().trim();
            String middlename = middleNameFld.getText().trim();
            String lastname = lastNameFld.getText().trim();
            String birthDate = birthDatePicker.getValue() != null
                    ? birthDatePicker.getValue().toString()
                    : "";
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
            String addedBy = com.ionres.respondph.util.SessionManager.getInstance().getCurrentAdminFirstName();
            String regDate = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            if (firstname.isEmpty()) {
                customAlertDialog.showWarning("First name is required");
                return;
            }
            if (middlename.isEmpty()) {
                customAlertDialog.showWarning("Middle name is required");
                return;
            }
            if (lastname.isEmpty()) {
                customAlertDialog.showWarning("Last name is required");
                return;
            }
            if (birthDate.isEmpty()) {
                customAlertDialog.showWarning("Birth date is required");
                return;
            }
            if (gender == null) {
                customAlertDialog.showWarning("Gender is required");
                return;
            }
            if (mobileNumber.isEmpty()) {
                customAlertDialog.showWarning("Mobile number is required");
                return;
            }
            if (maritalStatus == null) {
                customAlertDialog.showWarning("Marital status is required");
                return;
            }
            if (soloParentStatus == null) {
                customAlertDialog.showWarning("Solo parent status is required");
                return;
            }
            if (latitude.isEmpty()) {
                customAlertDialog.showWarning("Latitude is required");
                return;
            }
            if (longitude.isEmpty()) {
                customAlertDialog.showWarning("Longitude is required");
                return;
            }
            if (disabilityType == null) {
                customAlertDialog.showWarning("Disability type is required");
                return;
            }
            if (healthCondition == null) {
                customAlertDialog.showWarning("Health condition is required");
                return;
            }
            if (cleanWaterAccess == null) {
                customAlertDialog.showWarning("Clean water access is required");
                return;
            }
            if (sanitationFacility == null) {
                customAlertDialog.showWarning("Sanitation facility is required");
                return;
            }
            if (houseType == null) {
                customAlertDialog.showWarning("House type is required");
                return;
            }

            if (ownershipStatus == null) {
                customAlertDialog.showWarning("Ownership status is required");
                return;
            }

            if (employmentStatus == null) {
                customAlertDialog.showWarning("Employment status is required");
                return;
            }
            if (monthlyIncome == null) {
                customAlertDialog.showWarning("Monthly income is required");
                return;
            }
            if (educationalLevel == null) {
                customAlertDialog.showWarning("Educational level is required");
                return;
            }
            if (digitalAccess == null) {
                customAlertDialog.showWarning("Digital access is required");
                return;
            }

            BeneficiaryModel updatedBm = new BeneficiaryModel(
                    firstname, middlename, lastname, birthDate, gender,
                    maritalStatus, soloParentStatus, latitude, longitude,
                    mobileNumber, disabilityType, healthCondition, cleanWaterAccess,
                    sanitationFacility, houseType, ownershipStatus, employmentStatus,
                    monthlyIncome, educationalLevel, digitalAccess, addedBy,
                    regDate
            );

            updatedBm.setId(currentBeneficiary.getId());

            boolean success = beneficiaryService.updateBeneficiary(updatedBm);

            if (success) {
                customAlertDialog.showSuccess("Success", "Beneficiary updated successfully.");
                clearFields();
            } else {
                customAlertDialog.showErrorAlert("Error", "Failed to update beneficiary.");
            }
            dialogStage.hide();
        } catch (Exception e) {
            customAlertDialog.showErrorAlert("Error", e.getMessage());
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