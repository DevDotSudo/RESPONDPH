package com.ionres.respondph.familymembers;

import com.ionres.respondph.familymembers.dialogs_controller.AddFamilyController;
import com.ionres.respondph.familymembers.dialogs_controller.EditFamilyController;
import com.ionres.respondph.util.AlertDialog;
import com.ionres.respondph.util.AppContext;
import com.ionres.respondph.util.DialogManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    @FXML private TableColumn<FamilyMembersModel, Void> actionsColumn;
    @FXML private TextField searchField;
    @FXML private Button searchBtn;
    @FXML private Button addButton;
    @FXML private Button refreshButton;
    private final FamilyMemberService familyMembersService = AppContext.familyMemberService;
    ObservableList<FamilyMembersModel> familyMembersList;
    AlertDialog alertDialog = new AlertDialog();

    public void initialize() {
        familyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTableColumns();
        loadTable();
        actionButtons();
        EventHandler<ActionEvent> handler = this::handleActions;

        addButton.setOnAction(handler);
        refreshButton.setOnAction(handler);
        searchBtn.setOnAction(handler);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == addButton) {
            showAddMembersDialog();
        }
        else if (src == refreshButton) {
            loadTable();
        }
        else if (src == searchBtn) {
            handleSearch();
            setSearchFld();

        }
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(
                new PropertyValueFactory<>("familyId"));

        beneficiaryNameColumn.setCellValueFactory(
                new PropertyValueFactory<>("beneficiaryName"));

        firstnameColumn.setCellValueFactory(
                new PropertyValueFactory<>("firstName"));

        middlenameColumn.setCellValueFactory(
                new PropertyValueFactory<>("middleName"));

        lastnameColumn.setCellValueFactory(
                new PropertyValueFactory<>("lastName"));

        relationshipColumn.setCellValueFactory(
                new PropertyValueFactory<>("relationshipToBeneficiary"));
        birthdateColumn.setCellValueFactory(
                new PropertyValueFactory<>("birthDate"));

        genderColumn.setCellValueFactory(
                new PropertyValueFactory<>("gender"));
        maritalStatusColumn.setCellValueFactory(
                new PropertyValueFactory<>("maritalStatus"));

        registeredDateColumn.setCellValueFactory(
                new PropertyValueFactory<>("regDate"));

    }

    public void loadTable() {
        List<FamilyMembersModel> familyMembers = familyMembersService.getAllFamilyMembers();

        familyMembersList = FXCollections.observableArrayList(familyMembers);
        familyTable.setItems(familyMembersList);
    }

    private void deleteById(FamilyMembersModel fm) {
        if (fm == null || fm.getFamilyId() <= 0) {
            alertDialog.showErrorAlert("Invalid Selection", "Family Member ID is missing or invalid.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Are you sure you want to delete this Family Member?");
        confirm.setContentText("Firstname : " + fm.getFirstName());

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {

            boolean success = familyMembersService.deletefamilyMember(fm);

            familyMembersList.remove(fm);
        }
    }

    private void handleSearch() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            loadTable();
        } else {
            searchFamilyMember(searchText);
        }
    }

    private void searchFamilyMember(String searchText) {
        List<FamilyMembersModel> filteredFamilyMembers = familyMembersService.searchFamilyMember(searchText);
        familyMembersList = FXCollections.observableArrayList(filteredFamilyMembers);
        familyTable.setItems(familyMembersList);
    }

    private void setSearchFld() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.trim().isEmpty()) {
                loadTable();
            }
        });
    }

    private void showAddMembersDialog() {
        try {
            AddFamilyController addFamilyController = DialogManager.getController("addFamilyMember", AddFamilyController.class);
            addFamilyController.setFamilyMemberService(familyMembersService);
            addFamilyController.setFamilyMemberController(this);
            DialogManager.show("addFamilyMember");
        } catch(Exception e){
            e.printStackTrace();
            alertDialog.showErrorAlert("Error", "Unable to load the Add Family Member dialog.");
        }
    }

        private void showEditFamilyMemberDialog (FamilyMembersModel fm){
            try {
                EditFamilyController editFamilyController = DialogManager.getController("editFamilyMember", EditFamilyController.class);
                editFamilyController.setFamilyMemberService(familyMembersService);
                editFamilyController.setFamilyMembersController(this);
                FamilyMembersModel familyMembersModel = familyMembersService.getfamilyMemberId(fm.getFamilyId());
                if (familyMembersModel != null) {
                    editFamilyController.setFamilyMember(familyMembersModel);
                } else {
                    alertDialog.showErrorAlert("Error", "Unable to load Family Member data.");
                    return;
                }
                DialogManager.show("editFamilyMember");
            } catch (Exception e) {
                e.printStackTrace();
                alertDialog.showErrorAlert("Error", "Unable to load the Edit Family Member  dialog.");
            }
        }

        private void actionButtons () {
            Callback<TableColumn<FamilyMembersModel, Void>, TableCell<FamilyMembersModel, Void>> cellFactory =
                    new Callback<>() {
                        @Override
                        public TableCell<FamilyMembersModel, Void> call(TableColumn<FamilyMembersModel, Void> param) {
                            return new TableCell<>() {
                                private final FontAwesomeIconView editIcon = new FontAwesomeIconView(FontAwesomeIcon.EDIT);
                                private final FontAwesomeIconView deleteIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
                                private final Button editButton = new Button("", editIcon);
                                private final Button deleteButton = new Button("", deleteIcon);

                                {
                                    editIcon.getStyleClass().add("edit-icon");
                                    deleteIcon.getStyleClass().add("delete-icon");
                                    editButton.getStyleClass().add("edit-button");
                                    deleteButton.getStyleClass().add("delete-button");

                                    editButton.setOnAction(event -> {
                                        FamilyMembersModel fm = getTableView().getItems().get(getIndex());
                                        showEditFamilyMemberDialog(fm);
                                    });

                                    deleteButton.setOnAction(event -> {
                                        FamilyMembersModel bm = getTableView().getItems().get(getIndex());
                                        deleteById(bm);
                                        loadTable();
                                    });
                                }

                                @Override
                                public void updateItem(Void item, boolean empty) {
                                    super.updateItem(item, empty);
                                    if (empty) {
                                        setGraphic(null);
                                    } else {
                                        HBox box = new HBox(10, editButton, deleteButton);
                                        box.setAlignment(Pos.CENTER);
                                        box.getStyleClass().add("action-buttons-container");
                                        setGraphic(box);
                                        setAlignment(Pos.CENTER);
                                    }
                                }
                            };
                        }
                    };

            actionsColumn.setCellFactory(cellFactory);
        }
    }
