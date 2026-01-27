package com.ionres.respondph.disaster_damage.dialogs_controller;

import com.ionres.respondph.common.model.BeneficiaryModel;
import com.ionres.respondph.common.model.DisasterModel;
import com.ionres.respondph.disaster_damage.DisasterDamageController;
import com.ionres.respondph.disaster_damage.DisasterDamageModel;
import com.ionres.respondph.disaster_damage.DisasterDamageService;
import com.ionres.respondph.util.AlertDialogManager;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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

public class AddDisasterDamageDialogController {

    @FXML private VBox root;
    @FXML private ComboBox<BeneficiaryModel> beneficiaryNameFld;
    @FXML private ComboBox<DisasterModel> disasterFld;
    @FXML private ComboBox<String> damageSeverityFld;
    @FXML private TextField verifiedByFld;
    @FXML private DatePicker assessmentDatePicker;
    @FXML private TextField notesFld;
    @FXML private Button saveBtn, exitBtn;

    private List<BeneficiaryModel> allBeneficiaries;
    private List<DisasterModel> allDisaster;
    private DisasterDamageService disasterDamageService;
    private DisasterDamageController disasterDamageController;

    public void setDisasterDamageService(DisasterDamageService disasterDamageService) {
        this.disasterDamageService = disasterDamageService;
        loadBeneficiaries();
        loadDisaster();
    }

    public void setDisasterDamageController(DisasterDamageController disasterDamageController) {
        this.disasterDamageController = disasterDamageController;
    }

    @FXML
    private void initialize() {
        setupKeyHandlers();
        EventHandler<ActionEvent> handler = this::handleActions;
        saveBtn.setOnAction(handler);
        exitBtn.setOnAction(handler);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == saveBtn) {
            addDisasterDamage();
        }
        else if (src == exitBtn) {
            closeDialog();
        }
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

    private void closeDialog() {
        Stage stage = (Stage) exitBtn.getScene().getWindow();
        stage.hide();
    }

    private void loadBeneficiaries() {
        try {
            allBeneficiaries = disasterDamageService.getAllBeneficiaries();
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

            setupBeneficiarySearchFilter();

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error",
                    "Error loading beneficiaries: " + e.getMessage());
        }
    }

    private void setupBeneficiarySearchFilter() {
        beneficiaryNameFld.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            String search = newText.toLowerCase().trim();

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

            if (!beneficiaryNameFld.isShowing() && !filtered.isEmpty()) {
                beneficiaryNameFld.show();
            }
        });
    }

    private void loadDisaster() {
        try {
            allDisaster = disasterDamageService.getALlDisaster();
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

            setupDisasterSearchFilter();

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error",
                    "Error loading disasters: " + e.getMessage());
        }
    }

    private void setupDisasterSearchFilter() {
        disasterFld.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            String search = newText.toLowerCase().trim();

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

            if (!disasterFld.isShowing() && !filtered.isEmpty()) {
                disasterFld.show();
            }
        });
    }

    private void addDisasterDamage() {
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

            DisasterDamageModel disasterDamage = new DisasterDamageModel(
                    beneficiary.getBeneficiaryId(),
                    disaster.getDisasterId(),
                    damageSeverity,
                    assessmentDate,
                    verifiedBy,
                    notes,
                    regDate
            );

            boolean success = disasterDamageService.createDisasterDamage(disasterDamage);

            if (success) {
                AlertDialogManager.showSuccess("Success",
                        "Disaster damage record has been successfully added.");
                disasterDamageController.loadTable();
                clearFields();
                closeDialog();
            } else {
                AlertDialogManager.showError("Error",
                        "Failed to add disaster damage record. Please try again.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Error",
                    "An error occurred while adding disaster damage: " + e.getMessage());
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

    private void clearFields() {
        beneficiaryNameFld.getSelectionModel().clearSelection();
        beneficiaryNameFld.getEditor().clear();
        disasterFld.getSelectionModel().clearSelection();
        disasterFld.getEditor().clear();
        damageSeverityFld.getSelectionModel().clearSelection();
        assessmentDatePicker.setValue(null);
        verifiedByFld.clear();
        notesFld.clear();
    }
}