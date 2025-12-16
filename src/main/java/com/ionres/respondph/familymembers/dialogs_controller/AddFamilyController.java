package com.ionres.respondph.familymembers.dialogs_controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
    private Button saveBtn;
    @FXML
    private Button exitBtn;

    private double xOffset = 0;
    private  double yOffset = 0;

    public void initialize(){
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
