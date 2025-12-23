package com.ionres.respondph.familymembers.dialogs_controller;

import com.ionres.respondph.common.model.BeneficiaryModel;
import com.ionres.respondph.familymembers.FamilyMemberService;
import com.ionres.respondph.familymembers.FamilyMembersController;
import com.ionres.respondph.familymembers.FamilyMembersModel;
import com.ionres.respondph.util.AlertDialog;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class EditFamilyController {
    AlertDialog alertDialog = new AlertDialog();
    @FXML
    private VBox root;
    @FXML

    private List<BeneficiaryModel> allBeneficiaries;
    @FXML
    private TextField firstNameFld;
    @FXML
    private TextField middleNameFld;
    @FXML
    private TextField lastNameFld;
    @FXML
    private TextArea notesFld;
    @FXML
    private ComboBox<String> relationshipSelection;
    @FXML
    private DatePicker birthDatePicker;
    @FXML
    private ComboBox<String> genderSelection;
    @FXML
    private ComboBox<String> maritalStatusSelection;
    @FXML
    private ComboBox<String> disabilityTypeSelection;
    @FXML
    private ComboBox<String> healthConditionSelection;
    @FXML
    private ComboBox<String> employmentStatusSelection;
    @FXML
    private ComboBox<String> educationLevelSelection;
    @FXML
    private Button updateBtn;
    @FXML
    private Button exitBtn;
    private FamilyMemberService familyMemberService;
    private FamilyMembersController familyMembersController;
    private FamilyMembersModel currentFamilyMember;
    private Stage dialogStage;
    private FamilyMembersModel fm = new FamilyMembersModel();


    public void setFamilyMemberService(FamilyMemberService familyMemberService) {
        this.familyMemberService = familyMemberService;
    }

    public void setFamilyMembersController(FamilyMembersController familyMembersController) {
        this.familyMembersController = familyMembersController;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setFamilyMember(FamilyMembersModel fm) {
        this.currentFamilyMember = fm;
        populateFields(fm);
    }

    private double xOffset = 0;
    private  double yOffset = 0;

    @FXML
    public void initialize(){
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            Stage dialogStage = (Stage) root.getScene().getWindow();
            dialogStage.setX(event.getScreenX() - xOffset);
            dialogStage.setY(event.getScreenY() - yOffset);
        });
        initializeFamilyMemberProfileDropdowns();
        initializeVulnerabilityIndicatorsDropdowns();

        setupActionHandlers();
        setupKeyHandlers();
    }

    private void setupKeyHandlers() {
        root.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER: updateBtn.fire();
                case ESCAPE: exitBtn.fire();
            }
        });

        root.requestFocus();
    }


    private void setupActionHandlers() {
        EventHandler<ActionEvent> handlers = this::handleActions;
        updateBtn.setOnAction(handlers);
        exitBtn.setOnAction(handlers);
    }


    private void handleActions(ActionEvent event){
        Object src = event.getSource();

        if (src == updateBtn){
            updateFamilyMember();
            familyMembersController.loadTable();
            clearFields();
        }
        else if(src == exitBtn){
            closeDialog();
        }

    }
    private void closeDialog() {
        ((javafx.stage.Stage) exitBtn.getScene().getWindow()).close();
    }

    public void clearFields() {
        firstNameFld.setText("");
        middleNameFld.setText("");
        lastNameFld.setText("");
        relationshipSelection.getSelectionModel().clearSelection();
        birthDatePicker.setValue(null);
        genderSelection.getSelectionModel().clearSelection();
        maritalStatusSelection.getSelectionModel().clearSelection();
        disabilityTypeSelection.getSelectionModel().clearSelection();
        healthConditionSelection.getSelectionModel().clearSelection();
        employmentStatusSelection.getSelectionModel().clearSelection();
        educationLevelSelection.getSelectionModel().clearSelection();
        notesFld.setText("");

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

    private void populateFields(FamilyMembersModel fm) {
        if (fm == null) return;

        firstNameFld.setText(fm.getFirstName());
        middleNameFld.setText(fm.getMiddleName());
        lastNameFld.setText(fm.getLastName());
        notesFld.setText(fm.getNotes());

        relationshipSelection.setValue(fm.getRelationshipToBeneficiary());
        genderSelection.setValue(fm.getGender());
        maritalStatusSelection.setValue(fm.getMaritalStatus());
        disabilityTypeSelection.setValue(fm.getDisabilityType());
        healthConditionSelection.setValue(fm.getHealthCondition());
        employmentStatusSelection.setValue(fm.getEmploymentStatus());
        educationLevelSelection.setValue(fm.getEducationalLevel());

        if (fm.getBirthDate() != null && !fm.getBirthDate().isEmpty()) {
            try {
                birthDatePicker.setValue(java.time.LocalDate.parse(fm.getBirthDate()));
            } catch (Exception e) {
                System.out.println("Error parsing birthDate: " + e.getMessage());
            }
        }


    }

    private void updateFamilyMember(){
        try
        {
            String firstname = firstNameFld.getText().trim();
            String middlename = middleNameFld.getText().trim();
            String lastname = lastNameFld.getText().trim();
            String relationshipToBene = relationshipSelection.getValue();

            String birthDate = birthDatePicker.getValue() != null
                    ? birthDatePicker.getValue().toString()
                    : "";

            String gender = genderSelection.getValue();
            String maritalStatus = maritalStatusSelection.getValue();
            String disabilityType = disabilityTypeSelection.getValue();
            String healthCondition = healthConditionSelection.getValue();
            String employmentStatus = employmentStatusSelection.getValue();
            String educationalLevel = educationLevelSelection.getValue();
            String notes = notesFld.getText().trim();
            String regDate = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

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
            if (relationshipToBene.isEmpty()) {
                alertDialog.showWarning("Relationship to Beneficiary is required");
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
            if (maritalStatus == null) {
                alertDialog.showWarning("Marital Status is Required");
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
            if (employmentStatus == null) {
                alertDialog.showWarning("Employment status is required");
                return;
            }
            if (educationalLevel == null) {
                alertDialog.showWarning("Educational level is required");
                return;
            }
            if (notes.isEmpty()) {
                alertDialog.showWarning("Notes is required");
                return;
            }

            fm.setFirstName(firstname);
            fm.setMiddleName(middlename);
            fm.setLastName(lastname);
            fm.setRelationshipToBeneficiary(relationshipToBene);
            fm.setBirthDate(birthDate);
            fm.setGender(gender);
            fm.setMaritalStatus(maritalStatus);
            fm.setDisabilityType(disabilityType);
            fm.setHealthCondition(healthCondition);
            fm.setEmploymentStatus(employmentStatus);
            fm.setEducationalLevel(educationalLevel);
            fm.setNotes(notes);
            fm.setRegDate(currentFamilyMember.getRegDate());

            fm.setFamilyId(currentFamilyMember.getFamilyId());

            boolean success = familyMemberService.updatefamilyMember(fm);

            if (success) {
                alertDialog.showSuccess("Success", "Family Member updated successfully.");
            } else {
                alertDialog.showErrorAlert("Error", "Failed to update family Member.");
            }

        }catch (Exception e) {
            alertDialog.showErrorAlert("Error", e.getMessage());
            e.printStackTrace();
        }

    }
}
