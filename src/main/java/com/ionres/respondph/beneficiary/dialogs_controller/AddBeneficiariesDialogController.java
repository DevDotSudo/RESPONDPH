package com.ionres.respondph.beneficiary.dialogs_controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import com.ionres.respondph.beneficiary.BeneficiaryController;
import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.beneficiary.BeneficiaryService;
import com.ionres.respondph.util.AlertDialog;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AddBeneficiariesDialogController {
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
    private ComboBox<String> educationLevelSelection;
    @FXML
    private ComboBox<String> monthlyIncomeSelection;

    @FXML
    private ComboBox<String> digitalAccessSelection;
    @FXML
    private Button exitBtn;
    @FXML
    private Button addBeneficiaryBtn;
    private double yOffset = 0;
    private double xOffset = 0;
    AlertDialog alertDialog = new AlertDialog();
    private BeneficiaryService beneficiaryService;
    private BeneficiaryController beneficiaryController;
    private Stage dialogStage;

    public void setBeneficiaryService(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }
    public void setBeneficiaryController(BeneficiaryController beneficiaryController) {
        this.beneficiaryController = beneficiaryController;
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public Stage getDialogStage() {
        return dialogStage;
    }

    @FXML
    public void initialize() {
        makeDraggable();
        initializeBeneficiaryProfileDropdowns();
        initializeVulnerabilityIndicatorsDropdowns();
        initializeHousingAndInfrastructureDropdowns();
        initializeSocioEconomicStatusDropdowns();
        EventHandler<ActionEvent> handlers = this::handleActions;
        exitBtn.setOnAction(handlers);
        addBeneficiaryBtn.setOnAction(handlers);
    }

    private void handleActions(ActionEvent event){
        Object src = event.getSource();

        if(src == addBeneficiaryBtn){
            addBeneficiary();
            beneficiaryController.loadTable();
            clearFields();
        }
        else if(src == exitBtn){
            close();
        }
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

    private void addBeneficiary() {
        try {
            String firstname        = firstNameFld.getText().trim();
            String middlename       = middleNameFld.getText().trim();
            String lastname         = lastNameFld.getText().trim();
            String birthDate        = birthDatePicker.getValue() != null
                    ? birthDatePicker.getValue().toString()
                    : "";
            String gender           = genderSelection.getValue();
            String mobileNumber     = mobileNumberFld.getText().trim();
            String maritalStatus    = maritalStatusSelection.getValue();
            String soloParentStatus = soloParentStatusSelection.getValue();
            String latitude         = latitudeFld.getText().trim();
            String longitude        = longitudeFld.getText().trim();
            String disabilityType   = disabilityTypeSelection.getValue();
            String healthCondition  = healthConditionSelection.getValue();
            String cleanWaterAccess = cleanWaterAccessSelection.getValue();
            String sanitationFacility = sanitationFacilitiesSelection.getValue();
            String houseType        = houseConstructionTypeSelection.getValue();
            String ownershipStatus  = ownershipStatusSelection.getValue();
            String employmentStatus = employmentStatusSelection.getValue();
            String monthlyIncome    = monthlyIncomeSelection.getValue();
            String educationalLevel = educationLevelSelection.getValue();
            String digitalAccess    = digitalAccessSelection.getValue();


            if (firstname.isEmpty()) {
                alertDialog.showWarning("First name is required");
                return;
            }
            if (middlename.isEmpty()) {
                alertDialog.showWarning("Middle name is required");
                return;
            }
            if (lastname.isEmpty()) {
                alertDialog.showWarning("Last name is required");
                return;
            }
            if (birthDate.isEmpty()) {
                alertDialog.showWarning("Birth date is required");
                return;
            }
            if (gender == null) {
                alertDialog.showWarning("Gender is required");
                return;
            }
            if (mobileNumber.isEmpty()) {
                alertDialog.showWarning("Mobile number is required");
                return;
            }
            if (maritalStatus == null) {
                alertDialog.showWarning("Marital status is required");
                return;
            }
            if (soloParentStatus == null) {
                alertDialog.showWarning("Solo parent status is required");
                return;
            }
            if (latitude.isEmpty()) {
                alertDialog.showWarning("Latitude is required");
                return;
            }
            if (longitude.isEmpty()) {
                alertDialog.showWarning("Longitude is required");
                return;
            }
            if (disabilityType == null) {
                alertDialog.showWarning("Disability type is required");
                return;
            }
            if (healthCondition == null) {
                alertDialog.showWarning("Health condition is required");
                return;
            }
            if (cleanWaterAccess == null) {
                alertDialog.showWarning("Clean water access is required");
                return;
            }
            if (sanitationFacility == null) {
                alertDialog.showWarning("Sanitation facility is required");
                return;
            }
            if (houseType == null) {
                alertDialog.showWarning("House type is required");
                return;
            }
            if (ownershipStatus == null) {
                alertDialog.showWarning("Ownership status is required");
                return;
            }
            if (employmentStatus == null) {
                alertDialog.showWarning("Employment status is required");
                return;
            }
            if (monthlyIncome == null) {
                alertDialog.showWarning("Monthly income is required");
                return;
            }
            if (educationalLevel == null) {
                alertDialog.showWarning("Educational level is required");
                return;
            }
            if (digitalAccess == null) {
                alertDialog.showWarning("Digital access is required");
                return;
            }

            String regDate = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            String addedBy = com.ionres.respondph.util.SessionManager.getInstance().getCurrentAdminFirstName();


            BeneficiaryModel bm = new BeneficiaryModel(
                    firstname, middlename, lastname, birthDate, gender,
                    maritalStatus, soloParentStatus, latitude, longitude,
                    mobileNumber, disabilityType, healthCondition, cleanWaterAccess,
                    sanitationFacility, houseType, ownershipStatus, employmentStatus,
                    monthlyIncome, educationalLevel, digitalAccess, addedBy,regDate
            );

            boolean success = beneficiaryService.createBeneficiary(bm);

            if (success) {
                javax.swing.JOptionPane.showMessageDialog(
                        null,
                        "Beneficiary successfully added.",
                        "Success",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE
                );
                System.out.println("Firstname " + addedBy);
                close();
            } else {
                javax.swing.JOptionPane.showMessageDialog(
                        null,
                        "Failed to add beneficiary.",
                        "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE
                );
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

    private void close() {
        if (dialogStage != null) {
            dialogStage.hide();
        }
    }
}
