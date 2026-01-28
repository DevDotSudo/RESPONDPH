package com.ionres.respondph.beneficiary.dialogs_controller;

import com.ionres.respondph.beneficiary.AgeScoreCalculate;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.household_score.HouseholdScoreCalculate;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.DashboardRefresher;
import com.ionres.respondph.util.SessionManager;
import com.ionres.respondph.util.UpdateTrigger;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.ionres.respondph.beneficiary.BeneficiaryController;
import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.beneficiary.BeneficiaryService;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.function.UnaryOperator;

import static com.ionres.respondph.util.LatLongValidation.setNumericCoordinateFilter;


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
        onShow();
    }

    public Stage getDialogStage() {
        return dialogStage;
    }

    @FXML
    public void initialize() {
        makeDraggable();
        setupNumericFieldValidation();
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
        }
        else if(src == exitBtn){
            close();
        }
    }

    private void setupNumericFieldValidation() {
        setNumericPhoneNumberFilter(mobileNumberFld);

        setNumericCoordinateFilter(latitudeFld, 90.0, "Latitude");

        setNumericCoordinateFilter(longitudeFld, 180.0, "Longitude");
    }

    private void setNumericPhoneNumberFilter(TextField textField) {
        String phoneNumberPattern = "[-+()\\d\\s]*";

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();

            if (newText.matches(phoneNumberPattern)) {
                return change;
            }
            return null;
        };

        textField.setTextFormatter(new TextFormatter<>(filter));

        textField.setTooltip(new Tooltip("Enter phone number (digits, +, -, (, ), spaces allowed)"));
    }


    private void initializeBeneficiaryProfileDropdowns() {
        genderSelection.getItems().addAll("Male", "Female");

        maritalStatusSelection.getItems().addAll(
            "Single",
            "Married",
            "Widowed",
            "Separated"
        );

        soloParentStatusSelection.getItems().addAll("Not a Solo Parent", "Solo Parent (with support network)","Solo Parent (without support)");
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
            "Due to Chronic Illness",
                "Multiple Disabilities"
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
            "Daily access to clean and safe water",
            "Irregular or limited access to clean water",
            "No access to clean water"
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
            "Informal or irregular employment",
            "Unemployed"
        );

        monthlyIncomeSelection.getItems().addAll(
            "Less than 12,030 PHP (Poor)",
            "12,030 to 24,060 PHP (Low Income)",
            "24,061 to 48,120 PHP (Lower Middle Income)",
            "48,121 to 84,210 PHP (Middle Class)",
            "84,211 to 144,360 PHP (Upper Middle Income)",
            "144,361 to 240,600 PHP (Upper Income)",
            "At least 244,350(Rich)"
        );

        educationLevelSelection.getItems().addAll(
            "No Formal Education",
            "Elementary level completed",
            "High school level completed",
            "Vocational or technical training",
            "College or university level",
            "Graduate education"
        );

        digitalAccessSelection.getItems().addAll(
            "Reliable Internet and Device Access",
            "Intermittent internet or device access",
            "Limited or shared access only",
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
            double ageScore = AgeScoreCalculate.calculateAgeScoreFromBirthdate(birthDate);
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

            String regDate = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            String addedBy = com.ionres.respondph.util.SessionManager.getInstance().getCurrentAdminFirstName();


            BeneficiaryModel bm = new BeneficiaryModel(
                    firstname, middlename, lastname, birthDate,ageScore, gender,
                    maritalStatus, soloParentStatus, latitude, longitude,
                    mobileNumber, disabilityType, healthCondition, cleanWaterAccess,
                    sanitationFacility, houseType, ownershipStatus, employmentStatus,
                    monthlyIncome, educationalLevel, digitalAccess, addedBy,regDate
            );

            boolean success = beneficiaryService.createBeneficiary(bm);

            if (success) {
                int newBeneficiaryId = getLatestBeneficiaryId();

                if (newBeneficiaryId > 0) {
                    // Calculate and save household scores

                    UpdateTrigger trigger = new UpdateTrigger();
                    boolean cascadeSuccess = trigger.triggerCascadeUpdate(newBeneficiaryId);



                    if (cascadeSuccess) {
                        javax.swing.JOptionPane.showMessageDialog(
                                null,
                                "Beneficiary and household scores successfully saved.",
                                "Success",
                                javax.swing.JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(
                                null,
                                "Beneficiary saved, but household score calculation failed.",
                                "Warning",
                                javax.swing.JOptionPane.WARNING_MESSAGE
                        );
                    }
                }
                clearFields();
                DashboardRefresher.refresh();

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

    public void onShow() {
        clearFields();
    }



//    private void addBeneficiary() {
//        try {
//            String firstname        = firstNameFld.getText().trim();
//            String middlename       = middleNameFld.getText().trim();
//            String lastname         = lastNameFld.getText().trim();
//            String birthDate        = birthDatePicker.getValue() != null
//                    ? birthDatePicker.getValue().toString()
//                    : "";
//            String gender           = genderSelection.getValue();
//            String mobileNumber     = mobileNumberFld.getText().trim();
//            String maritalStatus    = maritalStatusSelection.getValue();
//            String soloParentStatus = soloParentStatusSelection.getValue();
//            String latitude         = latitudeFld.getText().trim();
//            String longitude        = longitudeFld.getText().trim();
//            String disabilityType   = disabilityTypeSelection.getValue();
//            String healthCondition  = healthConditionSelection.getValue();
//            String cleanWaterAccess = cleanWaterAccessSelection.getValue();
//            String sanitationFacility = sanitationFacilitiesSelection.getValue();
//            String houseType        = houseConstructionTypeSelection.getValue();
//            String ownershipStatus  = ownershipStatusSelection.getValue();
//            String employmentStatus = employmentStatusSelection.getValue();
//            String monthlyIncome    = monthlyIncomeSelection.getValue();
//            String educationalLevel = educationLevelSelection.getValue();
//            String digitalAccess    = digitalAccessSelection.getValue();
//
//            // Validation (keep your existing validation code)
//            if (firstname.isEmpty()) {
//                AlertDialogManager.showWarning("Warning","First name is required");
//                return;
//            }
//            // ... rest of validation ...
//
//            String regDate = java.time.LocalDateTime.now()
//                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));
//
//            String addedBy = com.ionres.respondph.util.SessionManager.getInstance().getCurrentAdminFirstName();
//
//            BeneficiaryModel bm = new BeneficiaryModel(
//                    firstname, middlename, lastname, birthDate, gender,
//                    maritalStatus, soloParentStatus, latitude, longitude,
//                    mobileNumber, disabilityType, healthCondition, cleanWaterAccess,
//                    sanitationFacility, houseType, ownershipStatus, employmentStatus,
//                    monthlyIncome, educationalLevel, digitalAccess, addedBy, regDate
//            );
//
//            boolean success = beneficiaryService.createBeneficiary(bm);
//
//            if (success) {
//
//                int newBeneficiaryId = getLatestBeneficiaryId();
//
//                if (newBeneficiaryId > 0) {
//                    // Calculate and save household scores
//                    HouseholdScoreCalculator calculator = new HouseholdScoreCalculator(
//                    );
//                    boolean scoresCalculated = calculator.calculateAndSaveHouseholdScore(newBeneficiaryId);
//
//                    if (scoresCalculated) {
//                        javax.swing.JOptionPane.showMessageDialog(
//                                null,
//                                "Beneficiary and household scores successfully saved.",
//                                "Success",
//                                javax.swing.JOptionPane.INFORMATION_MESSAGE
//                        );
//                    } else {
//                        javax.swing.JOptionPane.showMessageDialog(
//                                null,
//                                "Beneficiary saved, but household score calculation failed.",
//                                "Warning",
//                                javax.swing.JOptionPane.WARNING_MESSAGE
//                        );
//                    }
//                }
//
//                clearFields();
//                DashboardRefresher.refresh();
//
//            } else {
//                javax.swing.JOptionPane.showMessageDialog(
//                        null,
//                        "Failed to add beneficiary.",
//                        "Error",
//                        javax.swing.JOptionPane.ERROR_MESSAGE
//                );
//            }
//
//        } catch (Exception e) {
//            javax.swing.JOptionPane.showMessageDialog(
//                    null,
//                    e.getMessage(),
//                    "Error",
//                    javax.swing.JOptionPane.ERROR_MESSAGE
//            );
//            e.printStackTrace();
//        }
//    }

    private int getLatestBeneficiaryId() {
        try {
            String sql = "SELECT beneficiary_id FROM beneficiary ORDER BY beneficiary_id DESC LIMIT 1";
            java.sql.Connection conn = DBConnection.getInstance().getConnection();
            java.sql.PreparedStatement ps = conn.prepareStatement(sql);
            java.sql.ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("beneficiary_id");
                conn.close();
                return id;
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}
