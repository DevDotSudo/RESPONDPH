package com.ionres.respondph.familymembers;

import com.ionres.respondph.familymembers.dialogs_controller.AddFamilyController;
import com.ionres.respondph.familymembers.dialogs_controller.EditFamilyController;
import com.ionres.respondph.util.AlertDialogManager;
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

public class FamilyMembersController {

    @FXML private AnchorPane rootPane;
    @FXML private TableView<FamilyMembersModel> familyTable;
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
    private ObservableList<FamilyMembersModel> familyMembersList;

    @FXML
    public void initialize() {
        familyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTableColumns();
        loadTable();
        setupActionButtons();
        setupSearchListener();
        EventHandler<ActionEvent> handler = this::handleActions;
        refreshButton.setOnAction(handler);
        addButton.setOnAction(handler);
        searchBtn.setOnAction(handler);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if(src == searchBtn){
            handleSearch();
        }
        else if(src == addButton){
            handleAdd();
        }
        else if (src == refreshButton){
            handleRefresh();
        }
    }

    private void handleAdd() {
        showAddMembersDialog();
    }

    private void handleRefresh() {
        loadTable();
    }

    private void handleSearch() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            loadTable();
        } else {
            searchFamilyMembers(searchText);
        }
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("familyId"));
        beneficiaryNameColumn.setCellValueFactory(new PropertyValueFactory<>("beneficiaryName"));
        firstnameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        middlenameColumn.setCellValueFactory(new PropertyValueFactory<>("middleName"));
        lastnameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        relationshipColumn.setCellValueFactory(new PropertyValueFactory<>("relationshipToBeneficiary"));
        birthdateColumn.setCellValueFactory(new PropertyValueFactory<>("birthDate"));
        genderColumn.setCellValueFactory(new PropertyValueFactory<>("gender"));
        maritalStatusColumn.setCellValueFactory(new PropertyValueFactory<>("maritalStatus"));
        registeredDateColumn.setCellValueFactory(new PropertyValueFactory<>("regDate"));
    }

    public void loadTable() {
        try {
            List<FamilyMembersModel> familyMembers = familyMembersService.getAllFamilyMembers();
            familyMembersList = FXCollections.observableArrayList(familyMembers);
            familyTable.setItems(familyMembersList);

            if (familyMembers.isEmpty()) {
                familyTable.setPlaceholder(new Label("No family members found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error",
                    "Failed to load family members: " + e.getMessage());
        }
    }

    private void deleteFamilyMember(FamilyMembersModel familyMember) {
        if (familyMember == null || familyMember.getFamilyId() <= 0) {
            AlertDialogManager.showError("Invalid Selection",
                    "Family member ID is missing or invalid. Please select a valid family member.");
            return;
        }

        String memberName = familyMember.getFirstName() + " " + familyMember.getLastName();
        String beneficiaryName = familyMember.getBeneficiaryName();

        boolean confirm = AlertDialogManager.showConfirmation(
                "Delete Family Member",
                "Are you sure you want to delete family member:\n" +
                        memberName + "\n" +
                        "Relationship: " + familyMember.getRelationshipToBeneficiary() + "\n" +
                        "Beneficiary: " + beneficiaryName + "\n\n" +
                        "This action cannot be undone."
        );

        if (confirm) {
            try {
                boolean success = familyMembersService.deletefamilyMember(familyMember);

                if (success) {
                    familyMembersList.remove(familyMember);
                    AlertDialogManager.showSuccess("Delete Successful",
                            "Family member has been successfully deleted.");
                } else {
                    AlertDialogManager.showError("Delete Failed",
                            "Failed to delete family member. Please try again.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                AlertDialogManager.showError("Delete Error",
                        "An error occurred while deleting family member: " + e.getMessage());
            }
        }
    }

    private void searchFamilyMembers(String searchText) {
        try {
            List<FamilyMembersModel> filteredFamilyMembers = familyMembersService.searchFamilyMember(searchText);
            familyMembersList = FXCollections.observableArrayList(filteredFamilyMembers);
            familyTable.setItems(familyMembersList);

            if (filteredFamilyMembers.isEmpty()) {
                familyTable.setPlaceholder(new Label("No family members found for: " + searchText));
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Search Error",
                    "Failed to search family members: " + e.getMessage());
        }
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.trim().isEmpty()) {
                loadTable();
            }
        });
    }

    private void showAddMembersDialog() {
        try {
            AddFamilyController controller = DialogManager.getController(
                    "addFamilyMember", AddFamilyController.class);
            controller.setFamilyMemberService(familyMembersService);
            controller.setFamilyMemberController(this);
            DialogManager.show("addFamilyMember");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load Add Family Member dialog: " + e.getMessage());
        }
    }

    private void showEditFamilyMemberDialog(FamilyMembersModel familyMember) {
        try {
            EditFamilyController controller = DialogManager.getController(
                    "editFamilyMember", EditFamilyController.class);
            controller.setFamilyMemberService(familyMembersService);
            controller.setFamilyMembersController(this);

            FamilyMembersModel fullFamilyMember = familyMembersService.getfamilyMemberId(familyMember.getFamilyId());
            if (fullFamilyMember != null) {
                controller.setFamilyMember(fullFamilyMember);
            } else {
                AlertDialogManager.showError("Data Error",
                        "Unable to load family member data. The record may have been deleted.");
                return;
            }
            DialogManager.show("editFamilyMember");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load Edit Family Member dialog: " + e.getMessage());
        }
    }

    private void setupActionButtons() {
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
                                    FamilyMembersModel familyMember = getTableView().getItems().get(getIndex());
                                    showEditFamilyMemberDialog(familyMember);
                                });

                                deleteButton.setOnAction(event -> {
                                    FamilyMembersModel familyMember = getTableView().getItems().get(getIndex());
                                    deleteFamilyMember(familyMember);
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