package com.ionres.respondph.beneficiary.dialogs_controller;

import com.ionres.respondph.beneficiary.AgeScoreCalculate;
import com.ionres.respondph.common.controller.MappingDialogController;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.DashboardRefresher;
import com.ionres.respondph.util.DialogManager;
import com.ionres.respondph.util.UpdateTrigger;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import com.ionres.respondph.beneficiary.BeneficiaryController;
import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.beneficiary.BeneficiaryService;
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
    private ComboBox<String> barangaySelection;

    @FXML
    private ComboBox<String> genderSelection;
    @FXML
    private TextField mobileNumberFld;
    @FXML
    private ComboBox<String> maritalStatusSelection;
    @FXML
    private ComboBox<String> soloParentStatusSelection;
    @FXML
    public TextField latitudeFld;
    @FXML
    public TextField longitudeFld;
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
    private BeneficiaryModel beneficiaryModel;
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
        EventHandler<ActionEvent> handlers = this::handleActions;
        exitBtn.setOnAction(handlers);
        addBeneficiaryBtn.setOnAction(handlers);
        getLocationBtn.setOnAction(handlers);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == addBeneficiaryBtn) {
            addBeneficiary();
            beneficiaryController.loadTable();
        } else if (src == exitBtn) {
            close();
        } else if (src == getLocationBtn) {
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

    private void addBeneficiary() {
        try {
            String firstname = firstNameFld.getText().trim();
            String middlename = middleNameFld.getText().trim();
            String lastname = lastNameFld.getText().trim();
            String birthDate = birthDatePicker.getValue() != null
                    ? birthDatePicker.getValue().toString()
                    : "";
            String barangay = barangaySelection.getValue();
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

            if (firstname.isEmpty()) {
                AlertDialogManager.showWarning("Warning", "First name is required");
                return;
            }
            if (middlename.isEmpty()) {
                AlertDialogManager.showWarning("Warning", "Middle name is required");
                return;
            }
            if (lastname.isEmpty()) {
                AlertDialogManager.showWarning("Warning", "Last name is required");
                return;
            }
            if (birthDate.isEmpty()) {
                AlertDialogManager.showWarning("Warning", "Birth date is required");
                return;
            }
            if (barangay.isEmpty()) {
                AlertDialogManager.showWarning("Warning", "Barangay is required");
                return;
            }
            if (gender == null) {
                AlertDialogManager.showWarning("Warning", "Gender is required");
                return;
            }
            if (mobileNumber.isEmpty()) {
                AlertDialogManager.showWarning("Warning", "Mobile number is required");
                return;
            }
            if (maritalStatus == null) {
                AlertDialogManager.showWarning("Warning", "Marital status is required");
                return;
            }
            if (soloParentStatus == null) {
                AlertDialogManager.showWarning("Warning", "Solo parent status is required");
                return;
            }
            if (latitude.isEmpty()) {
                AlertDialogManager.showWarning("Warning", "Latitude is required");
                return;
            }
            if (longitude.isEmpty()) {
                AlertDialogManager.showWarning("Warning", "Longitude is required");
                return;
            }
            if (disabilityType == null) {
                AlertDialogManager.showWarning("Warning", "Disability type is required");
                return;
            }
            if (healthCondition == null) {
                AlertDialogManager.showWarning("Warning", "Health condition is required");
                return;
            }
            if (cleanWaterAccess == null) {
                AlertDialogManager.showWarning("Warning", "Clean water access is required");
                return;
            }
            if (sanitationFacility == null) {
                AlertDialogManager.showWarning("Warning", "Sanitation facility is required");
                return;
            }
            if (houseType == null) {
                AlertDialogManager.showWarning("Warning", "House type is required");
                return;
            }
            if (ownershipStatus == null) {
                AlertDialogManager.showWarning("Warning", "Ownership status is required");
                return;
            }
            if (employmentStatus == null) {
                AlertDialogManager.showWarning("Warning", "Employment status is required");
                return;
            }
            if (monthlyIncome == null) {
                AlertDialogManager.showWarning("Warning", "Monthly income is required");
                return;
            }

            if (educationalLevel == null) {
                AlertDialogManager.showWarning("Warning", "Educational level is required");
                return;
            }

            if (digitalAccess == null) {
                AlertDialogManager.showWarning("Warning", "Digital access is required");
                return;
            }

            String regDate = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            String addedBy = com.ionres.respondph.util.SessionManager.getInstance().getCurrentAdminFirstName();


            BeneficiaryModel bm = new BeneficiaryModel(
                    firstname, middlename, lastname, birthDate, barangay, ageScore, gender,
                    maritalStatus, soloParentStatus, latitude, longitude,
                    mobileNumber, disabilityType, healthCondition, cleanWaterAccess,
                    sanitationFacility, houseType, ownershipStatus, employmentStatus,
                    monthlyIncome, educationalLevel, digitalAccess, addedBy, regDate
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
                DashboardRefresher.refreshBeneInSend();

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
        barangaySelection.getSelectionModel().clearSelection();
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
