package com.ionres.respondph.aid.dialogs_controller;

import com.ionres.respondph.aid.AidDAO;
import com.ionres.respondph.aid.AidDAOServiceImpl;
import com.ionres.respondph.aid.AidModel;
import com.ionres.respondph.aid.AidPrintService;
import com.ionres.respondph.aid_type.AidTypeDAO;
import com.ionres.respondph.aid_type.AidTypeDAOImpl;
import com.ionres.respondph.aid_type.AidTypeModelComboBox;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.disaster.DisasterDAO;
import com.ionres.respondph.disaster.DisasterDAOImpl;
import com.ionres.respondph.disaster.DisasterModelComboBox;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.DashboardRefresher;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;

public class PrintAidDialogController {

    @FXML private ComboBox<DisasterModelComboBox> disasterComboBox;
    @FXML private ComboBox<AidTypeModelComboBox> aidTypeComboBox;
    @FXML private Label beneficiaryCountLabel;
    @FXML private Button printButton;
    @FXML private Button cancelButton;
    @FXML private Button previewButton;
    @FXML private Button printAllButton;

    private DisasterDAO disasterDAO;
    private AidTypeDAO aidTypeDAO;
    private AidDAO aidDAO;
    private AidPrintService printService;

    @FXML
    private void initialize() {
        disasterDAO = new DisasterDAOImpl(DBConnection.getInstance());
        aidTypeDAO = new AidTypeDAOImpl(DBConnection.getInstance());
        aidDAO = new AidDAOServiceImpl(DBConnection.getInstance());
        printService = new AidPrintService();

        DashboardRefresher.registerDisasterNameAndAidtypeName(this);

        setupComboBoxes();
        setupListeners();
        setupButtons();
    }

    private void setupComboBoxes() {
        List<DisasterModelComboBox> disasters = disasterDAO.findAll();
        disasterComboBox.setItems(FXCollections.observableArrayList(disasters));

        List<AidTypeModelComboBox> aidTypes = aidTypeDAO.findAll();
        aidTypeComboBox.setItems(FXCollections.observableArrayList(aidTypes));

        disasterComboBox.setPromptText("Select Disaster");
        aidTypeComboBox.setPromptText("Select Aid Type");
    }

    private void setupListeners() {
        disasterComboBox.setOnAction(e -> updateBeneficiaryCount());
        aidTypeComboBox.setOnAction(e -> updateBeneficiaryCount());
    }

    private void setupButtons() {
        printButton.setOnAction(e -> handlePrint());
        cancelButton.setOnAction(e -> handleCancel());
        previewButton.setOnAction(e -> handlePreview());
        printAllButton.setOnAction(e -> handlePrintAll());
    }

    private void updateBeneficiaryCount() {
        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
        AidTypeModelComboBox selectedAidType = aidTypeComboBox.getValue();

        if (selectedDisaster == null || selectedAidType == null) {
            beneficiaryCountLabel.setText("Beneficiaries: 0");
            printButton.setDisable(true);
            previewButton.setDisable(true);
            return;
        }

        // Get beneficiaries for this disaster and aid type
        List<AidModel> aidRecords = getAidRecords(selectedDisaster.getDisasterId(), selectedAidType.getAidTypeId());

        beneficiaryCountLabel.setText("Beneficiaries: " + aidRecords.size());
        printButton.setDisable(aidRecords.isEmpty());
        previewButton.setDisable(aidRecords.isEmpty());
    }

    private void handlePrint() {
        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
        AidTypeModelComboBox selectedAidType = aidTypeComboBox.getValue();

        if (selectedDisaster == null || selectedAidType == null) {
            AlertDialogManager.showWarning("Selection Required", "Please select both Disaster and Aid Type.");
            return;
        }

        // Get aid records
        List<AidModel> aidRecords = getAidRecords(selectedDisaster.getDisasterId(), selectedAidType.getAidTypeId());

        if (aidRecords.isEmpty()) {
            AlertDialogManager.showWarning("No Data", "No beneficiaries found for the selected disaster and aid type.");
            return;
        }

        // Print
        boolean success = printService.printSpecificReport(
                selectedDisaster.getDisasterName(),
                selectedAidType.getAidName(),
                aidRecords
        );

        if (success) {
            closeDialog();
        }
    }

    private void handlePreview() {
        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();
        AidTypeModelComboBox selectedAidType = aidTypeComboBox.getValue();

        if (selectedDisaster == null || selectedAidType == null) {
            AlertDialogManager.showWarning("Selection Required", "Please select both Disaster and Aid Type.");
            return;
        }

        List<AidModel> aidRecords = getAidRecords(selectedDisaster.getDisasterId(), selectedAidType.getAidTypeId());

        if (aidRecords.isEmpty()) {
            AlertDialogManager.showWarning("No Data", "No beneficiaries found.");
            return;
        }

        // Show preview (list of names)
        StringBuilder preview = new StringBuilder();
        preview.append("Disaster: ").append(selectedDisaster.getDisasterName()).append("\n");
        preview.append("Aid Type: ").append(selectedAidType.getAidName()).append("\n");
        preview.append("Total Beneficiaries: ").append(aidRecords.size()).append("\n\n");
        preview.append("Beneficiaries:\n");

        int counter = 1;
        for (AidModel aid : aidRecords) {
            preview.append(counter++).append(". ").append(aid.getBeneficiaryName()).append("\n");
        }

        AlertDialogManager.showInfo("Preview", preview.toString());
    }

    private void handleCancel() {
        closeDialog();
    }

    private List<AidModel> getAidRecords(int disasterId, int aidTypeId) {
        List<AidModel> allAid = aidDAO.getAllAidForTable();

        // Filter by disaster and aid type
        return allAid.stream()
                .filter(aid -> aid.getDisasterId() == disasterId && aid.getAidTypeId() == aidTypeId)
                .collect(Collectors.toList());
    }

    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void handlePrintAll() {
        boolean confirmed = AlertDialogManager.showConfirmation(
                "Print All Records",
                "This will print ALL aid distribution records grouped by disaster and aid type.\n\n" +
                        "This may generate multiple pages. Continue?"
        );

        if (!confirmed) {
            return;
        }

        // Get all aid records
        List<AidModel> allAidRecords = aidDAO.getAllAidForTable();

        if (allAidRecords.isEmpty()) {
            AlertDialogManager.showWarning("No Data", "No aid records found in the system.");
            return;
        }

        // Print all records
        boolean success = printService.printAidDistributionReport(allAidRecords);

        if (success) {
            closeDialog();
        }
    }

    // ADD THIS METHOD
    private void loadDisasters() {
        DisasterModelComboBox selectedDisaster = disasterComboBox.getValue();

        List<DisasterModelComboBox> disasters = disasterDAO.findAll();
        disasterComboBox.setItems(FXCollections.observableArrayList(disasters));

        // Try to restore selection
        if (selectedDisaster != null) {
            disasterComboBox.getItems().stream()
                    .filter(d -> d.getDisasterId() == selectedDisaster.getDisasterId())
                    .findFirst()
                    .ifPresent(disasterComboBox::setValue);
        }
    }

    // ADD THIS METHOD
    private void loadAidTypes() {
        AidTypeModelComboBox selectedAidType = aidTypeComboBox.getValue();

        List<AidTypeModelComboBox> aidTypes = aidTypeDAO.findAll();
        aidTypeComboBox.setItems(FXCollections.observableArrayList(aidTypes));

        // Try to restore selection
        if (selectedAidType != null) {
            aidTypeComboBox.getItems().stream()
                    .filter(at -> at.getAidTypeId() == selectedAidType.getAidTypeId())
                    .findFirst()
                    .ifPresent(aidTypeComboBox::setValue);
        }
    }

    // ADD THIS PUBLIC METHOD - Called by DashboardRefresher
    public void refreshComboBoxes() {
        loadDisasters();
        loadAidTypes();
        updateBeneficiaryCount();
    }
}