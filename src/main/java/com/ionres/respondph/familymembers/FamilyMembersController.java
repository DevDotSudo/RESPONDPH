package com.ionres.respondph.familymembers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FamilyMembersController {
    @FXML private AnchorPane rootPane;

    @FXML
    private TableView<FamilyMembersModel> familyTable;
    @FXML private TableColumn<FamilyMembersModel, Integer> idColumn;
    @FXML private TableColumn<FamilyMembersModel, String> beneficiaryNameColumn;
    @FXML private TableColumn<FamilyMembersModel, String> firstnameColumn;
    @FXML private TableColumn<FamilyMembersModel, String> middlenameColumn;
    @FXML private TableColumn<FamilyMembersModel, String> lastnameColumn;
    @FXML private TableColumn<FamilyMembersModel, String> relationshipColumn;
    @FXML private TableColumn<FamilyMembersModel, LocalDate> birthdateColumn;
    @FXML private TableColumn<FamilyMembersModel, String> genderColumn;
    @FXML private TableColumn<FamilyMembersModel, String> maritalStatusColumn;
    @FXML private TableColumn<FamilyMembersModel, LocalDate> registeredDateColumn;
    @FXML private TableColumn<FamilyMembersModel, String> actionsColumn;

    @FXML private TextField searchField;
    @FXML private Button searchBtn;
    @FXML private Button addButton;
    @FXML private Button refreshButton;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public void initialize() {
        familyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
}
