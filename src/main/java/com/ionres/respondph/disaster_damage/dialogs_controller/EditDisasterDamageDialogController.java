package com.ionres.respondph.disaster_damage.dialogs_controller;

import com.ionres.respondph.common.model.BeneficiaryModel;
import com.ionres.respondph.common.model.DisasterModel;
import com.ionres.respondph.disaster_damage.*;
import com.ionres.respondph.util.AlertDialog;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EditDisasterDamageDialogController {

    @FXML
    private VBox root;

    @FXML
    private ComboBox<BeneficiaryModel> beneficiaryNameFld;
    private List<BeneficiaryModel> allBeneficiaries;

    @FXML
    private ComboBox<DisasterModel> disasterFld;
    private List<DisasterModel> allDisaster;

    @FXML
    private ComboBox<String> damageSeverityFld;

    @FXML
    private TextField verifiedByFld;

    @FXML
    private DatePicker birthDatePicker;

    @FXML
    private TextField notesFld;

    @FXML
    private Button updateBtn, exitBtn;

    private DisasterDamageService disasterDamageService;
    private DisasterDamageController disasterDamageController;
    private DisasterDamageModel currentDisaster;
    private Stage dialogStage;

    AlertDialog alertDialog = new AlertDialog();

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

    public void setDisasterDamage(DisasterDamageModel ddm) {
        this.currentDisaster = ddm;
        Platform.runLater(() -> {
            if (allBeneficiaries != null && !allBeneficiaries.isEmpty()
                    && allDisaster != null && !allDisaster.isEmpty()) {
                populateFields(ddm);
            } else {
                System.err.println("Warning: Data not loaded when trying to populate fields");
            }
        });
    }

    @FXML
    private void initialize() {
        initializeDamageSeverityDropDowns();
        setupActionHandlers();
        setupKeyHandlers();
    }

    private void setupKeyHandlers() {
        root.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER:
                    updateBtn.fire();
                    break;
                case ESCAPE:
                    closeDialog();
                    break;
            }
        });
        root.requestFocus();
    }

    private void setupActionHandlers() {
        EventHandler<ActionEvent> handler = this::handleActions;
        updateBtn.setOnAction(handler);
        exitBtn.setOnAction(handler);
    }

    private void handleActions(ActionEvent event) {
        if (event.getSource() == updateBtn) {
            updateDisasterDamage();
        } else {
            closeDialog();
        }
    }

    private void initializeDamageSeverityDropDowns() {
        damageSeverityFld.getItems().addAll(
                "No visible damage",
                "Minor damage (non-structural)",
                "Moderate damage (partially unusable)",
                "Severe damage (unsafe for use)",
                "Destruction or collapse"
        );
    }

    private boolean isPopulatingFields = false;
    private boolean isSelectingFromDropdown = false;

    public void loadBeneficiaries() {
        try {
            allBeneficiaries = disasterDamageService.getAllBeneficiaries();

            if (allBeneficiaries == null || allBeneficiaries.isEmpty()) {
                System.err.println("No beneficiaries loaded!");
                alertDialog.showWarning("No beneficiaries found in the system.");
                return;
            }

            System.out.println("Loaded " + allBeneficiaries.size() + " beneficiaries");

            allBeneficiaries.sort(
                    Comparator.comparing(b -> b.getFirstName().toLowerCase())
            );

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
                            .filter(b ->
                                    (b.getBeneficiaryId() + " - " + b.getFirstName())
                                            .equalsIgnoreCase(text)
                            )
                            .findFirst()
                            .orElse(null);
                }
            });

            beneficiaryNameFld.setCellFactory(cb -> new ListCell<>() {
                @Override
                protected void updateItem(BeneficiaryModel item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null
                            ? ""
                            : item.getBeneficiaryId() + " - " + item.getFirstName());
                }
            });

            beneficiaryNameFld.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(BeneficiaryModel item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null
                            ? ""
                            : item.getBeneficiaryId() + " - " + item.getFirstName());
                }
            });

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
                            .filter(b ->
                                    String.valueOf(b.getBeneficiaryId()).contains(search)
                                            || b.getFirstName().toLowerCase().contains(search)
                            )
                            .sorted(Comparator.comparing(b -> b.getFirstName().toLowerCase()))
                            .collect(Collectors.toList());
                }

                beneficiaryNameFld.getItems().setAll(filtered);

                if (!beneficiaryNameFld.isShowing() && !filtered.isEmpty() &&
                        beneficiaryNameFld.getEditor().isFocused()) {
                    beneficiaryNameFld.show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            alertDialog.showWarning("Error loading beneficiaries: " + e.getMessage());
        }
    }

    public void loadDisaster() {
        try {
            allDisaster = disasterDamageService.getALlDisaster();

            if (allDisaster == null || allDisaster.isEmpty()) {
                System.err.println("No disasters loaded!");
                alertDialog.showWarning("No disasters found in the system.");
                return;
            }

            System.out.println("Loaded " + allDisaster.size() + " disasters");

            allDisaster.sort(
                    Comparator.comparing(d -> d.getDisasterType().toLowerCase())
            );

            disasterFld.setEditable(true);
            disasterFld.getItems().setAll(allDisaster);

            disasterFld.setConverter(new StringConverter<>() {
                @Override
                public String toString(DisasterModel d) {
                    if (d == null) return "";
                    return d.getDisasterId() + " - "
                            + d.getDisasterType() + " - "
                            + d.getDisasterName();
                }

                @Override
                public DisasterModel fromString(String text) {
                    return allDisaster.stream()
                            .filter(d ->
                                    (d.getDisasterId() + " - "
                                            + d.getDisasterType() + " - "
                                            + d.getDisasterName())
                                            .equalsIgnoreCase(text)
                            )
                            .findFirst()
                            .orElse(null);
                }
            });

            disasterFld.setCellFactory(cb -> new ListCell<>() {
                @Override
                protected void updateItem(DisasterModel item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null
                            ? ""
                            : item.getDisasterId() + " - "
                            + item.getDisasterType() + " - "
                            + item.getDisasterName());
                }
            });

            disasterFld.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(DisasterModel item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null
                            ? ""
                            : item.getDisasterId() + " - "
                            + item.getDisasterType() + " - "
                            + item.getDisasterName());
                }
            });

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
                            .filter(d ->
                                    String.valueOf(d.getDisasterId()).contains(search)
                                            || d.getDisasterType().toLowerCase().contains(search)
                                            || d.getDisasterName().toLowerCase().contains(search)
                            )
                            .sorted(Comparator.comparing(d -> d.getDisasterType().toLowerCase()))
                            .collect(Collectors.toList());
                }

                disasterFld.getItems().setAll(filtered);

                if (!disasterFld.isShowing() && !filtered.isEmpty() &&
                        disasterFld.getEditor().isFocused()) {
                    disasterFld.show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            alertDialog.showWarning("Error loading disasters: " + e.getMessage());
        }
    }

    private void populateFields(DisasterDamageModel ddm) {
        if (allBeneficiaries == null || allBeneficiaries.isEmpty()) {
            System.err.println("ERROR: Beneficiaries list is null or empty!");
            alertDialog.showErrorAlert("Error", "Beneficiaries data not loaded.");
            return;
        }

        if (allDisaster == null || allDisaster.isEmpty()) {
            System.err.println("ERROR: Disasters list is null or empty!");
            alertDialog.showErrorAlert("Error", "Disasters data not loaded.");
            return;
        }

        isPopulatingFields = true;

        System.out.println("Populating fields for damage ID: " + ddm.getBeneficiaryDisasterDamageId());
        System.out.println("Looking for beneficiary ID: " + ddm.getBeneficiaryId());
        System.out.println("Looking for disaster ID: " + ddm.getDisasterId());

        BeneficiaryModel foundBeneficiary = allBeneficiaries.stream()
                .filter(b -> b.getBeneficiaryId() == ddm.getBeneficiaryId())
                .findFirst()
                .orElse(null);

        if (foundBeneficiary != null) {
            beneficiaryNameFld.setValue(foundBeneficiary);
            beneficiaryNameFld.getEditor().setText(
                    foundBeneficiary.getBeneficiaryId() + " - " + foundBeneficiary.getFirstName()
            );
            System.out.println("Beneficiary found and set: " + foundBeneficiary.getFirstName());
        } else {
            System.err.println("Beneficiary with ID " + ddm.getBeneficiaryId() + " not found!");
        }

        DisasterModel foundDisaster = allDisaster.stream()
                .filter(d -> d.getDisasterId() == ddm.getDisasterId())
                .findFirst()
                .orElse(null);

        if (foundDisaster != null) {
            disasterFld.setValue(foundDisaster);
            disasterFld.getEditor().setText(
                    foundDisaster.getDisasterId() + " - " +
                            foundDisaster.getDisasterType() + " - " +
                            foundDisaster.getDisasterName()
            );
            System.out.println("Disaster found and set: " + foundDisaster.getDisasterType());
        } else {
            System.err.println("Disaster with ID " + ddm.getDisasterId() + " not found!");
        }

        damageSeverityFld.setValue(ddm.getHouseDamageSeverity());
        verifiedByFld.setText(ddm.getVerifiedBy());
        notesFld.setText(ddm.getNotes());

        if (ddm.getAssessmentDate() != null && !ddm.getAssessmentDate().isEmpty()) {
            try {
                birthDatePicker.setValue(LocalDate.parse(ddm.getAssessmentDate()));
            } catch (Exception e) {
                System.err.println("Error parsing date: " + ddm.getAssessmentDate());
                e.printStackTrace();
            }
        }

        Platform.runLater(() -> isPopulatingFields = false);

        System.out.println("All fields populated successfully!");
    }

    private void updateDisasterDamage() {
        try {
            BeneficiaryModel beneficiary = beneficiaryNameFld.getValue();
            DisasterModel disaster = disasterFld.getValue();
            String damageSeverity = damageSeverityFld.getValue();
            String verifiedBy = verifiedByFld.getText().trim();
            String notes = notesFld.getText().trim();

            String assessmentDate = birthDatePicker.getValue() != null
                    ? birthDatePicker.getValue().toString()
                    : "";

            String regDate = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm a"));

            if (beneficiary == null) {
                alertDialog.showWarning("Beneficiary is required");
                return;
            }

            if (disaster == null) {
                alertDialog.showWarning("Disaster is required");
                return;
            }

            if (damageSeverity == null || damageSeverity.isEmpty()) {
                alertDialog.showWarning("Damage severity is required");
                return;
            }

            if (assessmentDate.isEmpty()) {
                alertDialog.showWarning("Assessment date is required");
                return;
            }

            if (verifiedBy.isEmpty()) {
                alertDialog.showWarning("Verified by is required");
                return;
            }

            if (notes.isEmpty()) {
                alertDialog.showWarning("Notes are required");
                return;
            }

            DisasterDamageModel ddm = new DisasterDamageModel(
                    beneficiary.getBeneficiaryId(),
                    disaster.getDisasterId(),
                    damageSeverity,
                    assessmentDate,
                    verifiedBy,
                    notes,
                    regDate
            );

            ddm.setBeneficiaryDisasterDamageId(currentDisaster.getBeneficiaryDisasterDamageId());

            boolean success = disasterDamageService.updateDisasterDamage(ddm);

            if (success) {
                alertDialog.showSuccess(
                        "Success",
                        "Disaster damage updated successfully."
                );

                if (disasterDamageController != null) {
                    disasterDamageController.loadTable();
                }

            } else {
                alertDialog.showErrorAlert(
                        "Error",
                        "Failed to update disaster damage."
                );
            }

        } catch (Exception e) {
            alertDialog.showErrorAlert("Error", e.getMessage());
            e.printStackTrace();
        }
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        } else {
            ((Stage) exitBtn.getScene().getWindow()).close();
        }
    }
}