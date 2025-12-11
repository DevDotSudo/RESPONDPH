package com.ionres.respondph.beneficiary.dialogs_controller.pagination;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

public class BeneficiaryProfileController {
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
    private ComboBox<String> soloParentStatusSelection;

    @FXML
    private TextField mobileNumber;

    @FXML
    private TextField latitudeFld;

    @FXML
    private TextField longitudeFld;

    @FXML
    public void initialize() {

    }
}
