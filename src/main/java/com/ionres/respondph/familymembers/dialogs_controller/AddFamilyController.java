package com.ionres.respondph.familymembers.dialogs_controller;

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
import javafx.util.StringConverter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AddFamilyController {

    @FXML private VBox root;
    @FXML private TextField firstNameFld;
    @FXML private TextField middleNameFld;
    @FXML private TextField lastNameFld;
    @FXML private DatePicker birthDatePicker;
    @FXML private ComboBox<String> genderSelection;
    @FXML private ComboBox<String> maritalStatusSelection;
    @FXML private ComboBox<String> disabilityTypeSelection;
    @FXML private ComboBox<String> healthConditionSelection;
    @FXML private ComboBox<String> employmentStatusSelection;
    @FXML private ComboBox<String> educationLevelSelection;
    @FXML private ComboBox<BeneficiaryModel> beneficiaryNameFld;
    @FXML private TextArea notesFld;
    @FXML private ComboBox<String> relationshipSelection;
    @FXML private Button saveBtn;
    @FXML private Button exitBtn;

    private double xOffset = 0;
    private double yOffset = 0;
    private List<BeneficiaryModel> allBeneficiaries;
    private FamilyMemberService familyMemberService;
    private FamilyMembersController familyMembersController;

    public void setFamilyMemberService(FamilyMemberService familyMemberService) {
        this.familyMemberService = familyMemberService;
        loadBeneficiaries();
    }

    public void setFamilyMemberController(FamilyMembersController familyMembersController) {
        this.familyMembersController = familyMembersController;
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
                case ENTER: saveBtn.fire(); break;
                case ESCAPE: exitBtn.fire(); break;
            }
        });
        root.requestFocus();
    }

    private void setupEventHandlers() {
        saveBtn.setOnAction(this::handleSave);
        exitBtn.setOnAction(this::handleExit);
    }

    private void handleSave(ActionEvent event) {
        addFamilyMember();
    }

    private void handleExit(ActionEvent event) {
        closeDialog();
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
        beneficiaryNameFld.getSelectionModel().clearSelection();
        beneficiaryNameFld.getEditor().clear();
        notesFld.clear();
    }

    private void addFamilyMembers() {
        try {
            // Validate input
            if (!validateInput()) {
                return;
            }

            // Get values
            String firstName = firstNameFld.getText().trim();
            String middleName = middleNameFld.getText().trim();
            String lastName = lastNameFld.getText().trim();
            String relationship = relationshipSelection.getValue();
            String birthDate = birthDatePicker.getValue() != null ? birthDatePicker.getValue().toString() : "";
            String gender = genderSelection.getValue();
            String maritalStatus = maritalStatusSelection.getValue();
            String disabilityType = disabilityTypeSelection.getValue();
            String healthCondition = healthConditionSelection.getValue();
            String employmentStatus = employmentStatusSelection.getValue();
            String educationalLevel = educationLevelSelection.getValue();
            String notes = notesFld.getText().trim();

            String regDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            BeneficiaryModel selectedBeneficiary = beneficiaryNameFld.getValue();
            int beneficiaryId = selectedBeneficiary.getBeneficiaryId();

            // Create family member model
            FamilyMembersModel familyMember = new FamilyMembersModel(
                    firstName, middleName, lastName, relationship, birthDate, gender, maritalStatus,
                    disabilityType, healthCondition, employmentStatus, educationalLevel,
                    beneficiaryId, notes, regDate
            );

            // Save family member
            boolean success = familyMemberService.createfamilyMember(familyMember);

            if (success) {
                AlertDialogManager.showSuccess("Success",
                        "Family member has been successfully added.");
                familyMembersController.loadTable();
                clearFields();
                closeDialog();
            } else {
                AlertDialogManager.showError("Error",
                        "Failed to add family member. Please try again.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Error",
                    "An error occurred while adding family member: " + e.getMessage());
        }
    }

    private boolean validateInput() {
        if (beneficiaryNameFld.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error",
                    "Please select a beneficiary.");
            beneficiaryNameFld.requestFocus();
            return false;
        }

        if (firstNameFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error",
                    "First name is required.");
            firstNameFld.requestFocus();
            return false;
        }

        if (middleNameFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error",
                    "Middle name is required.");
            middleNameFld.requestFocus();
            return false;
        }

        if (lastNameFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error",
                    "Last name is required.");
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
            AlertDialogManager.showWarning("Validation Error",
                    "Birth date is required.");
            birthDatePicker.requestFocus();
            return false;
        }

        if (genderSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error",
                    "Gender is required.");
            genderSelection.requestFocus();
            return false;
        }

        if (maritalStatusSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error",
                    "Marital status is required.");
            maritalStatusSelection.requestFocus();
            return false;
        }

        if (disabilityTypeSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error",
                    "Disability type is required.");
            disabilityTypeSelection.requestFocus();
            return false;
        }

        if (healthConditionSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error",
                    "Health condition is required.");
            healthConditionSelection.requestFocus();
            return false;
        }

        if (employmentStatusSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error",
                    "Employment status is required.");
            employmentStatusSelection.requestFocus();
            return false;
        }

        if (educationLevelSelection.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error",
                    "Educational level is required.");
            educationLevelSelection.requestFocus();
            return false;
        }

        if (notesFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error",
                    "Notes are required.");
            notesFld.requestFocus();
            return false;
        }

        return true;
    }

    private void loadBeneficiaries() {
        try {
            allBeneficiaries = familyMemberService.getAllBeneficiaries();
            allBeneficiaries.sort(Comparator.comparing(b -> b.getFirstName().toLowerCase()));
            beneficiaryNameFld.getItems().setAll(allBeneficiaries);

            beneficiaryNameFld.setConverter(new StringConverter<>() {
                @Override
                public String toString(BeneficiaryModel beneficiary) {
                    return beneficiary != null ? beneficiary.getFirstName() : "";
                }

                @Override
                public BeneficiaryModel fromString(String string) {
                    return allBeneficiaries.stream()
                            .filter(b -> b.getFirstName().equalsIgnoreCase(string))
                            .findFirst()
                            .orElse(null);
                }
            });

            beneficiaryNameFld.setCellFactory(cb -> new ListCell<>() {
                @Override
                protected void updateItem(BeneficiaryModel item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getFirstName());
                }
            });

            beneficiaryNameFld.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(BeneficiaryModel item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getFirstName());
                }
            });

            beneficiaryNameFld.getEditor().textProperty().addListener((obs, oldText, newText) -> {
                String searchText = newText.toLowerCase().trim();

                List<BeneficiaryModel> filtered;
                if (searchText.isEmpty()) {
                    filtered = allBeneficiaries;
                } else {
                    filtered = allBeneficiaries.stream()
                            .filter(b -> b.getFirstName().toLowerCase().contains(searchText))
                            .sorted(Comparator.comparing(b -> b.getFirstName().toLowerCase()))
                            .collect(Collectors.toList());
                }

                beneficiaryNameFld.getItems().setAll(filtered);

                if (!beneficiaryNameFld.isShowing() && !filtered.isEmpty()) {
                    beneficiaryNameFld.show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error",
                    "Error loading beneficiaries: " + e.getMessage());
        }
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

    // Replace the addFamilyMember() method in AddFamilyController

    private void addFamilyMember() {
        try {
            // Validate input
            if (!validateInput()) {
                return;
            }

            // Get values
            String firstName = firstNameFld.getText().trim();
            String middleName = middleNameFld.getText().trim();
            String lastName = lastNameFld.getText().trim();
            String relationship = relationshipSelection.getValue();
            String birthDate = birthDatePicker.getValue() != null ? birthDatePicker.getValue().toString() : "";
            String gender = genderSelection.getValue();
            String maritalStatus = maritalStatusSelection.getValue();
            String disabilityType = disabilityTypeSelection.getValue();
            String healthCondition = healthConditionSelection.getValue();
            String employmentStatus = employmentStatusSelection.getValue();
            String educationalLevel = educationLevelSelection.getValue();
            String notes = notesFld.getText().trim();

            String regDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            BeneficiaryModel selectedBeneficiary = beneficiaryNameFld.getValue();
            int beneficiaryId = selectedBeneficiary.getBeneficiaryId();

            // Create family member model
            FamilyMembersModel familyMember = new FamilyMembersModel(
                    firstName, middleName, lastName, relationship, birthDate, gender, maritalStatus,
                    disabilityType, healthCondition, employmentStatus, educationalLevel,
                    beneficiaryId, notes, regDate
            );

            boolean success = familyMemberService.createfamilyMember(familyMember);

            if (success) {
                // Recalculate household scores for this beneficiary
                HouseholdScoreCalculator calculator = new HouseholdScoreCalculator(
                );
                boolean scoresCalculated = calculator.calculateAndSaveHouseholdScore(beneficiaryId);

                if (scoresCalculated) {
                    AlertDialogManager.showSuccess("Success",
                            "Family member added and household scores updated successfully.");
                } else {
                    AlertDialogManager.showWarning("Partial Success",
                            "Family member added, but household score update failed.");
                }

                familyMembersController.loadTable();
                clearFields();
                closeDialog();
            } else {
                AlertDialogManager.showError("Error",
                        "Failed to add family member. Please try again.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Error",
                    "An error occurred while adding family member: " + e.getMessage());
        }
    }
}