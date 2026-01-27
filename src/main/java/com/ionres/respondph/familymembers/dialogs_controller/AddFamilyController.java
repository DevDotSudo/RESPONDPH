
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
    @FXML private Button calculateHouseholdScoresBtn;
    @FXML private ComboBox<BeneficiaryModel> selectBeneficiaryComboBox;

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
        calculateHouseholdScoresBtn.setOnAction(this::handleSaveCalculation);
    }

    private void handleSave(ActionEvent event) {
        addFamilyMembers();
    }

    private void handleSaveCalculation(ActionEvent event) {
        handleCalculateScores();
    }

    private void handleCalculateScores() {
        BeneficiaryModel selected = selectBeneficiaryComboBox.getValue();

        if (selected == null) {
            AlertDialogManager.showWarning("Warning", "Please select a beneficiary first.");
            return;
        }

        int beneficiaryId = selected.getBeneficiaryId();

        HouseholdScoreCalculator calculator = new HouseholdScoreCalculator();
        boolean success = calculator.calculateAndSaveHouseholdScore(beneficiaryId);

        if (success) {
            AlertDialogManager.showSuccess("Success",
                    "Household scores have been calculated and saved successfully!");
        } else {
            AlertDialogManager.showError("Error",
                    "Failed to calculate household scores. Please check:\n" +
                            "1. Beneficiary has disaster damage record\n" +
                            "2. Vulnerability indicator scores are configured\n" +
                            "3. All family members have been added");
        }
    }

    private void handleExit(ActionEvent event) {
        closeDialog();
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
            if (!validateInput()) {
                return;
            }

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

            BeneficiaryModel selectedBeneficiary = beneficiaryNameFld.getValue();
            int beneficiaryId = selectedBeneficiary.getBeneficiaryId();

            FamilyMembersModel familyMember = new FamilyMembersModel(
                    firstName, middleName, lastName, relationship, birthDate, ageScore, gender, maritalStatus,
                    disabilityType, healthCondition, employmentStatus, educationalLevel,
                    beneficiaryId, notes, regDate
            );

            boolean success = familyMemberService.createfamilyMember(familyMember);

            if (success) {
                HouseholdScoreCalculator calculator = new HouseholdScoreCalculator();

                 calculator.calculateAndSaveHouseholdScore(beneficiaryId);

                AlertDialogManager.showSuccess("Success",
                        "Family member has been successfully added.\n" +
                                "Add more family members or click 'Calculate Household Scores' when done.");
                familyMembersController.loadTable();
                clearFields();




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

            // ✅ POPULATE BOTH COMBO BOXES
            setupBeneficiaryNameComboBox();
            setupSelectBeneficiaryComboBox();

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error",
                    "Error loading beneficiaries: " + e.getMessage());
        }
    }

    // ✅ Setup for adding family members
    private void setupBeneficiaryNameComboBox() {
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
    }

    // ✅ Setup for calculating household scores
    private void setupSelectBeneficiaryComboBox() {
        selectBeneficiaryComboBox.getItems().setAll(allBeneficiaries);

        selectBeneficiaryComboBox.setConverter(new StringConverter<>() {
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

        selectBeneficiaryComboBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(BeneficiaryModel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getFirstName());
            }
        });

        selectBeneficiaryComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(BeneficiaryModel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getFirstName());
            }
        });
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