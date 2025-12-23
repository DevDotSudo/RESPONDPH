package com.ionres.respondph.disaster_damage.dialogs_controller;

import com.ionres.respondph.common.model.BeneficiaryModel;
import com.ionres.respondph.common.model.DisasterModel;
import com.ionres.respondph.disaster_damage.*;
import com.ionres.respondph.util.AlertDialog;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AddDisasterDamageDialogController {



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
    private DatePicker assessmentDatePicker;

    @FXML
    private TextField notesFld;

    @FXML
    private Button saveBtn, exitBtn;

    AlertDialog alertDialog = new AlertDialog();


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
        initializeDamageSeverityDropDowns();

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
            addDisasterDamage();
            disasterDamageController.loadTable();
            clearFields();
        }
        else if(src == exitBtn){
            closeDialog();
        }

    }

    private void initializeDamageSeverityDropDowns(){

        damageSeverityFld.getItems().addAll(
                "No visible damage",
                "Minor damage (non-structural)",
                "Moderate damage (partially unusable)",
                "Severe damage (unsafe for use)",
                "Destruction or collapse"

        );


    }

    private void closeDialog() {
        ((javafx.stage.Stage) exitBtn.getScene().getWindow()).close();
    }


    public void loadBeneficiaries() {
        try {
            allBeneficiaries = disasterDamageService.getAllBeneficiaries();

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

            beneficiaryNameFld.getEditor().textProperty().addListener((obs, oldText, newText) -> {
                String search = newText.toLowerCase().trim();

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

                if (!beneficiaryNameFld.isShowing() && !filtered.isEmpty()) {
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

            disasterFld.getEditor().textProperty().addListener((obs, oldText, newText) -> {
                String search = newText.toLowerCase().trim();

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

                if (!disasterFld.isShowing() && !filtered.isEmpty()) {
                    disasterFld.show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            alertDialog.showWarning("Error loading disasters: " + e.getMessage());
        }
    }


    private void addDisasterDamage() {
        try {
            BeneficiaryModel beneficiary = beneficiaryNameFld.getValue();
            DisasterModel disaster = disasterFld.getValue();
            String damageSeverity = damageSeverityFld.getValue();
            String verifiedBy = verifiedByFld.getText().trim();
            String notes = notesFld.getText().trim();

            String assessmentDate = assessmentDatePicker.getValue() != null
                    ? assessmentDatePicker.getValue().toString()
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

            if (assessmentDate == null) {
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


            boolean success = disasterDamageService.createDisasterDamage(ddm);

            if (success) {
                javax.swing.JOptionPane.showMessageDialog(
                        null,
                        "Disaster damage successfully added.",
                        "Success",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                javax.swing.JOptionPane.showMessageDialog(
                        null,
                        "Failed to add disaster damage.",
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



    private void clearFields() {
        beneficiaryNameFld.getSelectionModel().clearSelection();
        beneficiaryNameFld.setValue(null);
        beneficiaryNameFld.getEditor().clear();
        disasterFld.getSelectionModel().clearSelection();
        disasterFld.setValue(null);
        disasterFld.getEditor().clear();
        damageSeverityFld.getSelectionModel().clearSelection();
        damageSeverityFld.setValue(null);
        assessmentDatePicker.setValue(null);
        verifiedByFld.setText("");
        notesFld.setText("");
    }
}