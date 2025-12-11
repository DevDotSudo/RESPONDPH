package com.ionres.respondph.beneficiary.dialogs_controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.util.ArrayList;

public class AddBeneficiariesDialogController {
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
    private ComboBox<String> damageSeveritySelection;
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
    private Button addBeneficiaryBtn;

    @FXML
    public void initialize() {
        initializeBeneficiaryProfileDropdowns();
        initializeVulnerabilityIndicatorsDropdowns();
        initializeHousingAndInfrastructureDropdowns();
        initializeSocioEconomicStatusDropdowns();
        exitBtn.setOnAction(event -> closeDialog());
        addBeneficiaryBtn.setOnAction(event -> addBeneficiary());
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

        damageSeveritySelection.getItems().addAll(
            "No visible damage",
            "Minor damage (non-structural)",
            "Moderate damage (partially inhabitable)",
            "Severe damage or partially collapsed (unsafe for use)",
            "Destruction or collapse"
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
        System.out.println("Add Beneficiary button clicked");
    }

    private void closeDialog() {
        ((javafx.stage.Stage) exitBtn.getScene().getWindow()).close();
    }
}
