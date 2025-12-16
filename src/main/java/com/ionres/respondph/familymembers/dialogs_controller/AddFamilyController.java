package com.ionres.respondph.familymembers.dialogs_controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

public class AddFamilyController {
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
    private Button saveBtn;
    @FXML
    private Button exitBtn;

    public void initialize(){

    }
}
