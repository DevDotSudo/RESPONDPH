package com.ionres.respondph.familymembers.dialogs_controller;

import com.ionres.respondph.common.model.BeneficiaryModel;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.ionres.respondph.familymembers.FamilyMemberService;
import com.ionres.respondph.familymembers.FamilyMembersController;
import com.ionres.respondph.familymembers.FamilyMembersModel;
import com.ionres.respondph.util.AlertDialog;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class AddFamilyController {
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
    private ComboBox<BeneficiaryModel> beneficiaryNameFld;

    @FXML
    private TextArea notesFld;
    @FXML
    private ComboBox<String> relationshipSelection;
    @FXML
    private Button saveBtn;
    @FXML
    private Button exitBtn;
    AlertDialog alertDialog = new AlertDialog();
    private double xOffset = 0;
    private  double yOffset = 0;
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

    public void initialize(){
        makeDraggable();
        initializeFamilyMemberProfileDropdowns();
        initializeVulnerabilityIndicatorsDropdowns();
        setupActionHandlers();
        setupKeyHandlers();
    }

    private void setupKeyHandlers() {
        root.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER: saveBtn.fire();
                case ESCAPE: exitBtn.fire();
            }
        });
        root.requestFocus();
    }

    private void setupActionHandlers() {
        EventHandler<ActionEvent> handlers = this::handleActions;
        saveBtn.setOnAction(handlers);
        exitBtn.setOnAction(handlers);
    }


    private void handleActions(ActionEvent event){
        Object src = event.getSource();

        if (src == saveBtn){
            addFamilyMembers();
            familyMembersController.loadTable();
            clearFields();
        }
        else if(src == exitBtn){
            closeDialog();
        }

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
        beneficiaryNameFld.getSelectionModel().clearSelection();
        beneficiaryNameFld.getEditor().setText("");
        notesFld.setText("");

    }

    private void addFamilyMembers(){
        String firstname        = firstNameFld.getText().trim();
        String middlename       = middleNameFld.getText().trim();
        String lastname         = lastNameFld.getText().trim();
        String relationshipToBene = relationshipSelection.getValue();
        String birthDate        = birthDatePicker.getValue() != null
                ? birthDatePicker.getValue().toString()
                : "";
        String gender           = genderSelection.getValue();
        String maritalStatus    = maritalStatusSelection.getValue();
        String disabilityType   = disabilityTypeSelection.getValue();
        String healthCondition  = healthConditionSelection.getValue();
        String employmentStatus = employmentStatusSelection.getValue();
        String educationalLevel = educationLevelSelection.getValue();
        String notes = notesFld.getText().trim();
        String regDate = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

        BeneficiaryModel selected = beneficiaryNameFld.getValue();

        if (selected == null) {
            alertDialog.showWarning("Please select a beneficiary");
            return;
        }
        int beneficiaryId = selected.getBeneficiaryId();

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
        if(relationshipToBene.isEmpty()){
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
        if(maritalStatus == null){
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
        if (notes.isEmpty()){
            alertDialog.showWarning("Notes is required");
            return;
        }

        FamilyMembersModel familyMembersModel = new FamilyMembersModel(firstname, middlename, lastname, relationshipToBene, birthDate, gender, maritalStatus,
                disabilityType, healthCondition, employmentStatus, educationalLevel, beneficiaryId, notes, regDate);

        boolean success = familyMemberService.createfamilyMember(familyMembersModel);

        if (success) {
            javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "Family Member successfully added.",
                    "Success",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "Failed to add Family Member.",
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );
        }
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
            alertDialog.showWarning("Error loading beneficiaries: " + e.getMessage());
        }
    }

    public void makeDraggable() {
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
