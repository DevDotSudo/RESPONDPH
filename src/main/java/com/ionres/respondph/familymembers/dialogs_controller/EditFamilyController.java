package com.ionres.respondph.familymembers.dialogs_controller;

import com.ionres.respondph.beneficiary.AgeScoreCalculator;
import com.ionres.respondph.common.model.BeneficiaryModel;
import com.ionres.respondph.familymembers.FamilyMemberService;
import com.ionres.respondph.familymembers.FamilyMembersController;
import com.ionres.respondph.familymembers.FamilyMembersModel;
import com.ionres.respondph.household_score.HouseholdScoreCalculator;
import com.ionres.respondph.util.AlertDialogManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class EditFamilyController {

    @FXML private VBox root;
    @FXML private TextField firstNameFld;
    @FXML private TextField middleNameFld;
    @FXML private TextField lastNameFld;
    @FXML private TextArea notesFld;
    @FXML private ComboBox<String> relationshipSelection;
    @FXML private DatePicker birthDatePicker;
    @FXML private ComboBox<String> genderSelection;
    @FXML private ComboBox<String> maritalStatusSelection;
    @FXML private ComboBox<String> disabilityTypeSelection;
    @FXML private ComboBox<String> healthConditionSelection;
    @FXML private ComboBox<String> employmentStatusSelection;
    @FXML private ComboBox<String> educationLevelSelection;
    @FXML private Button updateBtn;
    @FXML private Button exitBtn;

    private FamilyMemberService familyMemberService;
    private FamilyMembersController familyMembersController;
    private FamilyMembersModel currentFamilyMember;
    private Stage dialogStage;

    private double xOffset = 0;
    private double yOffset = 0;

    public void setFamilyMemberService(FamilyMemberService familyMemberService) {
        this.familyMemberService = familyMemberService;
    }

    public void setFamilyMembersController(FamilyMembersController familyMembersController) {
        this.familyMembersController = familyMembersController;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setFamilyMember(FamilyMembersModel familyMember) {
        this.currentFamilyMember = familyMember;
        populateFields(familyMember);
    }

    @FXML
    public void initialize() {
        makeDraggable();
        initializeDropdowns();
        setupEventHandlers();
        setupKeyHandlers();
    }

    private void setupKeyHandlers() {
        root.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER: updateBtn.fire(); break;
                case ESCAPE: exitBtn.fire(); break;
            }
        });
        root.requestFocus();
    }

    private void setupEventHandlers() {
        updateBtn.setOnAction(this::handleUpdate);
        exitBtn.setOnAction(this::handleExit);
    }

    private void handleUpdate(ActionEvent event) {
        updateFamilyMember();
    }

    private void handleExit(ActionEvent event) {
        closeDialog();
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

    private void closeDialog() {
        Stage stage = (Stage) exitBtn.getScene().getWindow();
        stage.hide();
    }

    private void clearFields() {
        firstNameFld.clear();
        middleNameFld.clear();
        lastNameFld.clear();
        relationshipSelection.getSelectionModel().clearSelection();
        birthDatePicker.setValue(null);
        genderSelection.getSelectionModel().clearSelection();
        maritalStatusSelection.getSelectionModel().clearSelection();
        disabilityTypeSelection.getSelectionModel().clearSelection();
        healthConditionSelection.getSelectionModel().clearSelection();
        employmentStatusSelection.getSelectionModel().clearSelection();
        educationLevelSelection.getSelectionModel().clearSelection();
        notesFld.clear();
    }

    private void initializeDropdowns() {
        initializeFamilyMemberProfileDropdowns();
        initializeVulnerabilityIndicatorsDropdowns();
    }

    private void initializeFamilyMemberProfileDropdowns() {
        genderSelection.getItems().addAll("Male", "Female", "Other");

        maritalStatusSelection.getItems().addAll(
                "Single",
                "Married",
                "Widowed",
                "Separated",
                "Divorced"
        );

        relationshipSelection.getItems().addAll(
                "Son",
                "Daughter",
                "Grandchild",
                "Niece",
                "Nephew",
                "Brother",
                "Sister"
        );
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

        employmentStatusSelection.getItems().addAll(
                "Regular full-time employment",
                "Self-employed with stable income",
                "Self-employed with unstable income",
                "Irregular employment (odd jobs, seasonal work)",
                "Unemployed"
        );

        educationLevelSelection.getItems().addAll(
                "No Formal Education",
                "Elementary",
                "High School",
                "Vocational or technical training",
                "College or university level",
                "Graduate education"
        );
    }

    private void populateFields(FamilyMembersModel familyMember) {
        if (familyMember == null) return;

        firstNameFld.setText(familyMember.getFirstName());
        middleNameFld.setText(familyMember.getMiddleName());
        lastNameFld.setText(familyMember.getLastName());
        notesFld.setText(familyMember.getNotes());

        relationshipSelection.setValue(familyMember.getRelationshipToBeneficiary());
        genderSelection.setValue(familyMember.getGender());
        maritalStatusSelection.setValue(familyMember.getMaritalStatus());
        disabilityTypeSelection.setValue(familyMember.getDisabilityType());
        healthConditionSelection.setValue(familyMember.getHealthCondition());
        employmentStatusSelection.setValue(familyMember.getEmploymentStatus());
        educationLevelSelection.setValue(familyMember.getEducationalLevel());

        if (familyMember.getBirthDate() != null && !familyMember.getBirthDate().isEmpty()) {
            try {
                birthDatePicker.setValue(LocalDate.parse(familyMember.getBirthDate()));
            } catch (DateTimeParseException e) {
                System.err.println("Error parsing birth date: " + e.getMessage());
            }
        }
    }

    private void updateFamilyMembers() {
        try {
            // Validate input
            if (!validateInput()) {
                return;
            }

            // Get updated values
            String firstName = firstNameFld.getText().trim();
            String middleName = middleNameFld.getText().trim();
            String lastName = lastNameFld.getText().trim();
            String relationship = relationshipSelection.getValue();
            String birthDate = birthDatePicker.getValue() != null ? birthDatePicker.getValue().toString() : "";
            double ageScore = AgeScoreCalculator.calculateAgeScoreFromBirthdate(birthDate);
            String gender = genderSelection.getValue();
            String maritalStatus = maritalStatusSelection.getValue();
            String disabilityType = disabilityTypeSelection.getValue();
            String healthCondition = healthConditionSelection.getValue();
            String employmentStatus = employmentStatusSelection.getValue();
            String educationalLevel = educationLevelSelection.getValue();
            String notes = notesFld.getText().trim();

            String regDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            // Create updated family member model
            FamilyMembersModel updatedFamilyMember = new FamilyMembersModel(
                    firstName, middleName, lastName, relationship, birthDate, ageScore, gender, maritalStatus,
                    disabilityType, healthCondition, employmentStatus, educationalLevel,
                    currentFamilyMember.getBeneficiaryId(), notes, regDate
            );

            updatedFamilyMember.setFamilyId(currentFamilyMember.getFamilyId());

            // Update family member
            boolean success = familyMemberService.updatefamilyMember(updatedFamilyMember);

            if (success) {
                AlertDialogManager.showSuccess("Update Successful",
                        "Family member information has been successfully updated.");
                familyMembersController.loadTable();
                closeDialog();
            } else {
                AlertDialogManager.showError("Update Failed",
                        "Failed to update family member. Please try again.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Update Error",
                    "An error occurred while updating family member: " + e.getMessage());
        }
    }

    private void updateFamilyMember() {
        try {
            // Validate input
            if (!validateInput()) {
                return;
            }

            // Get updated values
            String firstName = firstNameFld.getText().trim();
            String middleName = middleNameFld.getText().trim();
            String lastName = lastNameFld.getText().trim();
            String relationship = relationshipSelection.getValue();
            String birthDate = birthDatePicker.getValue() != null ? birthDatePicker.getValue().toString() : "";
            double ageScore = AgeScoreCalculator.calculateAgeScoreFromBirthdate(birthDate);
            String gender = genderSelection.getValue();
            String maritalStatus = maritalStatusSelection.getValue();
            String disabilityType = disabilityTypeSelection.getValue();
            String healthCondition = healthConditionSelection.getValue();
            String employmentStatus = employmentStatusSelection.getValue();
            String educationalLevel = educationLevelSelection.getValue();
            String notes = notesFld.getText().trim();

            String regDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            // Create updated family member model
            FamilyMembersModel updatedFamilyMember = new FamilyMembersModel(
                    firstName, middleName, lastName, relationship, birthDate, ageScore, gender, maritalStatus,
                    disabilityType, healthCondition, employmentStatus, educationalLevel,
                    currentFamilyMember.getBeneficiaryId(), notes, regDate
            );

            updatedFamilyMember.setFamilyId(currentFamilyMember.getFamilyId());

            // Update family member
            boolean success = familyMemberService.updatefamilyMember(updatedFamilyMember);

            if (success) {
                // ✅ AUTO-RECALCULATE HOUSEHOLD SCORES AFTER FAMILY MEMBER UPDATE
                HouseholdScoreCalculator calculator =
                        new HouseholdScoreCalculator();
                calculator.autoRecalculateHouseholdScore(currentFamilyMember.getBeneficiaryId());

                AlertDialogManager.showSuccess("Update Successful",
                        "Family member information has been successfully updated.\nHousehold scores have been recalculated.");
                familyMembersController.loadTable();
                closeDialog();
            } else {
                AlertDialogManager.showError("Update Failed",
                        "Failed to update family member. Please try again.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Update Error",
                    "An error occurred while updating family member: " + e.getMessage());
        }
    }

    private boolean validateInput() {
        if (firstNameFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "First name is required.");
            firstNameFld.requestFocus();
            return false;
        }

        if (middleNameFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Middle name is required.");
            middleNameFld.requestFocus();
            return false;
        }

        if (lastNameFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Last name is required.");
            lastNameFld.requestFocus();
            return false;
        }

        if (relationshipSelection.getValue() == null || relationshipSelection.getValue().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error",
                    "Relationship to beneficiary is required.");
            relationshipSelection.requestFocus();
            return false;
        }

        if (birthDatePicker.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Birth date is required.");
            birthDatePicker.requestFocus();
            return false;
        }

        if (genderSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Gender is required.");
            genderSelection.requestFocus();
            return false;
        }

        if (maritalStatusSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Marital status is required.");
            maritalStatusSelection.requestFocus();
            return false;
        }

        if (disabilityTypeSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Disability type is required.");
            disabilityTypeSelection.requestFocus();
            return false;
        }

        if (healthConditionSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Health condition is required.");
            healthConditionSelection.requestFocus();
            return false;
        }

        if (employmentStatusSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Employment status is required.");
            employmentStatusSelection.requestFocus();
            return false;
        }

        if (educationLevelSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Educational level is required.");
            educationLevelSelection.requestFocus();
            return false;
        }

        if (notesFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Notes are required.");
            notesFld.requestFocus();
            return false;
        }

        return true;
    }
}