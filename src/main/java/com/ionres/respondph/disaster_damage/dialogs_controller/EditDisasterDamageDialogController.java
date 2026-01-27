package com.ionres.respondph.disaster_damage.dialogs_controller;

import com.ionres.respondph.common.model.BeneficiaryModel;
import com.ionres.respondph.common.model.DisasterModel;
import com.ionres.respondph.disaster_damage.DisasterDamageController;
import com.ionres.respondph.disaster_damage.DisasterDamageModel;
import com.ionres.respondph.disaster_damage.DisasterDamageService;
import com.ionres.respondph.util.AlertDialogManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EditDisasterDamageDialogController {

    @FXML private VBox root;
    @FXML private ComboBox<BeneficiaryModel> beneficiaryNameFld;
    @FXML private ComboBox<DisasterModel> disasterFld;
    @FXML private ComboBox<String> damageSeverityFld;
    @FXML private TextField verifiedByFld;
    @FXML private DatePicker assessmentDatePicker;
    @FXML private TextField notesFld;
    @FXML private Button updateBtn, exitBtn;

    private List<BeneficiaryModel> allBeneficiaries;
    private List<DisasterModel> allDisaster;
    private DisasterDamageService disasterDamageService;
    private DisasterDamageController disasterDamageController;
    private DisasterDamageModel currentDisasterDamage;
    private Stage dialogStage;

    private boolean isPopulatingFields = false;
    private boolean isSelectingFromDropdown = false;

    public void setDisasterDamageService(DisasterDamageService service) {
        this.disasterDamageService = service;
        loadBeneficiaries();
        loadDisaster();
    }

    public void setDisasterDamageController(DisasterDamageController controller) {
        this.disasterDamageController = controller;
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setDisasterDamage(DisasterDamageModel disasterDamage) {
        this.currentDisasterDamage = disasterDamage;
        Platform.runLater(() -> {
            if (allBeneficiaries != null && !allBeneficiaries.isEmpty() &&
                    allDisaster != null && !allDisaster.isEmpty()) {
                populateFields(disasterDamage);
            }
        });
    }

    @FXML
    private void initialize() {
        setupKeyHandlers();
        EventHandler<ActionEvent> handler = this::handleActions;
        updateBtn.setOnAction(handler);
        exitBtn.setOnAction(handler);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == exitBtn) {
            closeDialog();
        }
        else if (src == updateBtn) {
            updateDisasterDamage();
        }
    }

    private void setupKeyHandlers() {
        root.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER: updateBtn.fire(); break;
                case ESCAPE: closeDialog(); break;
            }
        });
        root.requestFocus();
    }

    private void loadBeneficiaries() {
        try {
            allBeneficiaries = disasterDamageService.getAllBeneficiaries();

            if (allBeneficiaries == null || allBeneficiaries.isEmpty()) {
                AlertDialogManager.showWarning("Data Warning",
                        "No beneficiaries found in the system.");
                return;
            }

            allBeneficiaries.sort(Comparator.comparing(b -> b.getFirstName().toLowerCase()));

            beneficiaryNameFld.setEditable(true);
            beneficiaryNameFld.getItems().setAll(allBeneficiaries);

            beneficiaryNameFld.setConverter(new StringConverter<>() {
                @Override
                public String toString(BeneficiaryModel b) {
                    if (b == null) return "";
                    return b.getBeneficiaryId() + " - " + b.getFirstName();
                }

                @Override
                public BeneficiaryModel fromString(String text) {
                    return allBeneficiaries.stream()
                            .filter(b -> (b.getBeneficiaryId() + " - " + b.getFirstName())
                                    .equalsIgnoreCase(text))
                            .findFirst()
                            .orElse(null);
                }
            });

            beneficiaryNameFld.setCellFactory(cb -> new ListCell<>() {
                @Override
                protected void updateItem(BeneficiaryModel item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" :
                            item.getBeneficiaryId() + " - " + item.getFirstName());
                }
            });

            beneficiaryNameFld.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(BeneficiaryModel item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" :
                            item.getBeneficiaryId() + " - " + item.getFirstName());
                }
            });

            setupBeneficiaryListeners();

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error",
                    "Error loading beneficiaries: " + e.getMessage());
        }
    }

    private void setupBeneficiaryListeners() {
        beneficiaryNameFld.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                isSelectingFromDropdown = true;
                Platform.runLater(() -> isSelectingFromDropdown = false);
            }
        });

        beneficiaryNameFld.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (isPopulatingFields || isSelectingFromDropdown) {
                return;
            }

            String search = newText == null ? "" : newText.toLowerCase().trim();

            List<BeneficiaryModel> filtered;
            if (search.isEmpty()) {
                filtered = allBeneficiaries;
            } else {
                filtered = allBeneficiaries.stream()
                        .filter(b -> String.valueOf(b.getBeneficiaryId()).contains(search) ||
                                b.getFirstName().toLowerCase().contains(search))
                        .sorted(Comparator.comparing(b -> b.getFirstName().toLowerCase()))
                        .collect(Collectors.toList());
            }

            beneficiaryNameFld.getItems().setAll(filtered);

            if (!beneficiaryNameFld.isShowing() && !filtered.isEmpty() &&
                    beneficiaryNameFld.getEditor().isFocused()) {
                beneficiaryNameFld.show();
            }
        });
    }

    private void loadDisaster() {
        try {
            allDisaster = disasterDamageService.getALlDisaster();

            if (allDisaster == null || allDisaster.isEmpty()) {
                AlertDialogManager.showWarning("Data Warning",
                        "No disasters found in the system.");
                return;
            }

            allDisaster.sort(Comparator.comparing(d -> d.getDisasterType().toLowerCase()));

            disasterFld.setEditable(true);
            disasterFld.getItems().setAll(allDisaster);

            disasterFld.setConverter(new StringConverter<>() {
                @Override
                public String toString(DisasterModel d) {
                    if (d == null) return "";
                    return d.getDisasterId() + " - " +
                            d.getDisasterType() + " - " +
                            d.getDisasterName();
                }

                @Override
                public DisasterModel fromString(String text) {
                    return allDisaster.stream()
                            .filter(d -> (d.getDisasterId() + " - " +
                                    d.getDisasterType() + " - " +
                                    d.getDisasterName()).equalsIgnoreCase(text))
                            .findFirst()
                            .orElse(null);
                }
            });

            disasterFld.setCellFactory(cb -> new ListCell<>() {
                @Override
                protected void updateItem(DisasterModel item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" :
                            item.getDisasterId() + " - " +
                                    item.getDisasterType() + " - " +
                                    item.getDisasterName());
                }
            });

            disasterFld.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(DisasterModel item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" :
                            item.getDisasterId() + " - " +
                                    item.getDisasterType() + " - " +
                                    item.getDisasterName());
                }
            });

            setupDisasterListeners();

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error",
                    "Error loading disasters: " + e.getMessage());
        }
    }

    private void setupDisasterListeners() {
        disasterFld.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                isSelectingFromDropdown = true;
                Platform.runLater(() -> isSelectingFromDropdown = false);
            }
        });

        disasterFld.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (isPopulatingFields || isSelectingFromDropdown) {
                return;
            }

            String search = newText == null ? "" : newText.toLowerCase().trim();

            List<DisasterModel> filtered;
            if (search.isEmpty()) {
                filtered = allDisaster;
            } else {
                filtered = allDisaster.stream()
                        .filter(d -> String.valueOf(d.getDisasterId()).contains(search) ||
                                d.getDisasterType().toLowerCase().contains(search) ||
                                d.getDisasterName().toLowerCase().contains(search))
                        .sorted(Comparator.comparing(d -> d.getDisasterType().toLowerCase()))
                        .collect(Collectors.toList());
            }

            disasterFld.getItems().setAll(filtered);

            if (!disasterFld.isShowing() && !filtered.isEmpty() &&
                    disasterFld.getEditor().isFocused()) {
                disasterFld.show();
            }
        });
    }

    private void populateFields(DisasterDamageModel disasterDamage) {
        if (allBeneficiaries == null || allBeneficiaries.isEmpty()) {
            AlertDialogManager.showError("Data Error", "Beneficiaries data not loaded.");
            return;
        }

        if (allDisaster == null || allDisaster.isEmpty()) {
            AlertDialogManager.showError("Data Error", "Disasters data not loaded.");
            return;
        }

        isPopulatingFields = true;

        BeneficiaryModel foundBeneficiary = allBeneficiaries.stream()
                .filter(b -> b.getBeneficiaryId() == disasterDamage.getBeneficiaryId())
                .findFirst()
                .orElse(null);

        if (foundBeneficiary != null) {
            beneficiaryNameFld.setValue(foundBeneficiary);
            beneficiaryNameFld.getEditor().setText(
                    foundBeneficiary.getBeneficiaryId() + " - " + foundBeneficiary.getFirstName()
            );
        } else {
            AlertDialogManager.showWarning("Data Warning",
                    "Beneficiary with ID " + disasterDamage.getBeneficiaryId() + " not found.");
        }

        DisasterModel foundDisaster = allDisaster.stream()
                .filter(d -> d.getDisasterId() == disasterDamage.getDisasterId())
                .findFirst()
                .orElse(null);

        if (foundDisaster != null) {
            disasterFld.setValue(foundDisaster);
            disasterFld.getEditor().setText(
                    foundDisaster.getDisasterId() + " - " +
                            foundDisaster.getDisasterType() + " - " +
                            foundDisaster.getDisasterName()
            );
        } else {
            AlertDialogManager.showWarning("Data Warning",
                    "Disaster with ID " + disasterDamage.getDisasterId() + " not found.");
        }

        damageSeverityFld.setValue(disasterDamage.getHouseDamageSeverity());
        verifiedByFld.setText(disasterDamage.getVerifiedBy());
        notesFld.setText(disasterDamage.getNotes());

        if (disasterDamage.getAssessmentDate() != null && !disasterDamage.getAssessmentDate().isEmpty()) {
            try {
                assessmentDatePicker.setValue(LocalDate.parse(disasterDamage.getAssessmentDate()));
            } catch (DateTimeParseException e) {
                System.err.println("Error parsing date: " + disasterDamage.getAssessmentDate());
            }
        }

        Platform.runLater(() -> isPopulatingFields = false);
    }

    private void updateDisasterDamage() {
        try {
            if (!validateInput()) {
                return;
            }

            BeneficiaryModel beneficiary = beneficiaryNameFld.getValue();
            DisasterModel disaster = disasterFld.getValue();
            String damageSeverity = damageSeverityFld.getValue();
            String verifiedBy = verifiedByFld.getText().trim();
            String notes = notesFld.getText().trim();

            String assessmentDate = assessmentDatePicker.getValue() != null ?
                    assessmentDatePicker.getValue().toString() : "";

            String regDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            DisasterDamageModel updatedDisasterDamage = new DisasterDamageModel(
                    beneficiary.getBeneficiaryId(),
                    disaster.getDisasterId(),
                    damageSeverity,
                    assessmentDate,
                    verifiedBy,
                    notes,
                    regDate
            );

            updatedDisasterDamage.setBeneficiaryDisasterDamageId(
                    currentDisasterDamage.getBeneficiaryDisasterDamageId());

            boolean success = disasterDamageService.updateDisasterDamage(updatedDisasterDamage);

            if (success) {
                AlertDialogManager.showSuccess("Update Successful",
                        "Disaster damage record has been successfully updated.");

                if (disasterDamageController != null) {
                    disasterDamageController.loadTable();
                }
                closeDialog();
            } else {
                AlertDialogManager.showError("Update Failed",
                        "Failed to update disaster damage record. Please try again.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Update Error",
                    "An error occurred while updating disaster damage: " + e.getMessage());
        }
    }

    private boolean validateInput() {
        if (beneficiaryNameFld.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Beneficiary is required.");
            beneficiaryNameFld.requestFocus();
            return false;
        }

        if (disasterFld.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Disaster is required.");
            disasterFld.requestFocus();
            return false;
        }

        if (damageSeverityFld.getValue() == null || damageSeverityFld.getValue().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Damage severity is required.");
            damageSeverityFld.requestFocus();
            return false;
        }

        if (assessmentDatePicker.getValue() == null) {
            AlertDialogManager.showWarning("Validation Error", "Assessment date is required.");
            assessmentDatePicker.requestFocus();
            return false;
        }

        if (verifiedByFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Verified by is required.");
            verifiedByFld.requestFocus();
            return false;
        }

        if (notesFld.getText().trim().isEmpty()) {
            AlertDialogManager.showWarning("Validation Error", "Notes are required.");
            notesFld.requestFocus();
            return false;
        }

        return true;
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.hide();
        } else if (exitBtn.getScene() != null && exitBtn.getScene().getWindow() instanceof Stage) {
            ((Stage) exitBtn.getScene().getWindow()).hide();
        }
    }
}