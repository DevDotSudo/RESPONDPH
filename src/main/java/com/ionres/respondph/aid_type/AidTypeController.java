package com.ionres.respondph.aid_type;

import com.ionres.respondph.aid_type.dialogs_controller.AddAidTypeController;
import com.ionres.respondph.aid_type.dialogs_controller.EditAidTypeController;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.AppContext;
import com.ionres.respondph.util.DashboardRefresher;
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

import java.util.List;

public class AidTypeController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private TextField searchFld;

    @FXML
    private Button searchBtn;

    @FXML
    private Button addButton;

    @FXML
    private Button refreshButton;

    @FXML
    private TableView<AidTypeModel> aidTypeTable;

    @FXML
    private TableColumn<AidTypeModel, String> idColumn;

    @FXML
    private TableColumn<AidTypeModel, String> nameColumn;

    @FXML
    private TableColumn<AidTypeModel, String> adminNameColumn;

    @FXML
    private TableColumn<AidTypeModel, String> notesColumn;

    @FXML
    private TableColumn<AidTypeModel, String>  regDate;

    @FXML
    private  TableColumn<AidTypeModel, Void> actionsColumn;

    private ObservableList<AidTypeModel> aidTypeModelList;

    private final AidTypeService aidTypeService = AppContext.aidTypeService;

    @FXML
    private void initialize() {
        EventHandler<ActionEvent> handlers = this::handleActions;

        refreshButton.setOnAction(handlers);
        searchBtn.setOnAction(handlers);
        addButton.setOnAction(handlers);

        setupTableColumns();
        loadTable();
        setupActionButtons();
    }



    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == addButton) {
            showAddAidType();

        } else if (src == searchBtn) {
            handleSearch();
            setSearchFld();
        } else if (src == refreshButton) {
            loadTable();
        }
    }



    public void loadTable() {
        try {
            List<AidTypeModel> familyMembers = aidTypeService.getAllAidType();
            aidTypeModelList = FXCollections.observableArrayList(familyMembers);
            aidTypeTable.setItems(aidTypeModelList);

            if (familyMembers.isEmpty()) {
                aidTypeTable.setPlaceholder(new Label("No Aid Type found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error",
                    "Failed to load Aid Type: " + e.getMessage());
        }
    }

    private void deleteAidType(AidTypeModel aidTypeModel) {
        if (aidTypeModel == null || aidTypeModel.getAidTypeId() <= 0) {
            AlertDialogManager.showError("Invalid Selection",
                    "Family member ID is missing or invalid. Please select a valid family member.");
            return;
        }

        String aidTypeName = aidTypeModel.getAidTypeName();
        String adminName = aidTypeModel.getAdminName();

        boolean confirm = AlertDialogManager.showConfirmation(
                "Delete Aid Type",
                "Are you sure you want to delete Aid Type :\n" +
                        aidTypeName + "\n" +
                        "Admin: " + adminName + "\n\n" +
                        "This action cannot be undone."
        );

        if (confirm) {
            try {
                boolean success = aidTypeService.deleteAidType(aidTypeModel);

                if (success) {
                    aidTypeModelList.remove(aidTypeModel);
                    AlertDialogManager.showSuccess("Successful",
                            "Aid Type has been successfully deleted.");
                    loadTable();
                    DashboardRefresher.refreshComboBoxOfDNAndAN();
                    DashboardRefresher.refresh();
                } else {
                    AlertDialogManager.showError("Delete Failed",
                            "Failed to delete Aid Type. Please try again.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                AlertDialogManager.showError("Delete Error",
                        "An error occurred while deleting family member: " + e.getMessage());
            }
        }
    }

    private void showEditAidTypeController(AidTypeModel aidTypeModel) {
        try {
            EditAidTypeController controller = DialogManager.getController(
                    "editAidType", EditAidTypeController.class);
            controller.setAidTypeService(aidTypeService);
            controller.setAidTypeController(this);

            AidTypeModel aidType = aidTypeService.getAidTypeById(aidTypeModel.getAidTypeId());
            if (aidType != null) {
                controller.setAidType(aidType);
            } else {
                AlertDialogManager.showError("Data Error",
                        "Unable to load Aid Type  data. The record may have been deleted.");
                return;
            }
            DialogManager.show("editAidType");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load Edit Aid Type dialog: " + e.getMessage());
        }
    }

    private void showAddAidType() {
        try {
            AddAidTypeController controller = DialogManager.getController("addAidType", AddAidTypeController.class);
            controller.setAidTypeService(aidTypeService);
            controller.setAidTypeController(this);
            DialogManager.show("addAidType");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load the Add Beneficiary dialog: " + e.getMessage());
        }
    }


    private void handleSearch() {
        String searchText = searchFld.getText().trim();
        if (searchText.isEmpty()) {
            loadTable();
        } else {
            searchAdmins(searchText);
        }
    }

    private void searchAdmins(String searchText) {
        try {
            List<AidTypeModel> filteredBeneficiaries = aidTypeService.searchAidType(searchText);
            aidTypeModelList = FXCollections.observableArrayList(filteredBeneficiaries);
            aidTypeTable.setItems(aidTypeModelList);

            if (filteredBeneficiaries.isEmpty()) {
                aidTypeTable.setPlaceholder(new Label("No Aid Type found for: " + searchText));
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Search Error",
                    "Failed to search Aid type: " + e.getMessage());
        }
    }

    private void setSearchFld() {
        searchFld.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.trim().isEmpty()) {
                loadTable();
            }
        });
    }



    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("aidTypeId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("aidTypeName"));
        notesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));
        adminNameColumn.setCellValueFactory(new PropertyValueFactory<>("adminName"));
        regDate.setCellValueFactory(new PropertyValueFactory<>("regDate"));
    }



    private void setupActionButtons() {
        Callback<TableColumn<AidTypeModel, Void>, TableCell<AidTypeModel, Void>> cellFactory =
                new Callback<>() {
                    @Override
                    public TableCell<AidTypeModel, Void> call(TableColumn<AidTypeModel, Void> param) {
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
                                    AidTypeModel aidTypeModel = getTableView().getItems().get(getIndex());
                                    showEditAidTypeController(aidTypeModel);
                                });

                                deleteButton.setOnAction(event -> {
                                    AidTypeModel aidTypeModel = getTableView().getItems().get(getIndex());
                                    deleteAidType(aidTypeModel);
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