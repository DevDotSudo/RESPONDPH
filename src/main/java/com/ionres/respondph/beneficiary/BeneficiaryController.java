package com.ionres.respondph.beneficiary;

import com.ionres.respondph.beneficiary.dialogs_controller.AddBeneficiariesDialogController;
import com.ionres.respondph.beneficiary.dialogs_controller.EditBeneficiariesDialogController;
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

public class BeneficiaryController {

    BeneficiaryService beneficiaryService = AppContext.beneficiaryService;
    ObservableList<BeneficiaryModel> beneficiaryList;

    @FXML
    private AnchorPane rootPane;

    @FXML
    private TableColumn<BeneficiaryModel, String> idColumn;

    @FXML
    private TableColumn<BeneficiaryModel, String> firstnameColumn;

    @FXML
    private TableColumn<BeneficiaryModel, String> middlenameColumn;

    @FXML
    private TableColumn<BeneficiaryModel, String> lastnameColumn;

    @FXML
    private TableColumn<BeneficiaryModel, String> birthdateColumn;

    @FXML
    private TableColumn<BeneficiaryModel, String> genderColumn;

    @FXML
    private TableColumn<BeneficiaryModel, String> statusColumn;

    @FXML
    private TableColumn<BeneficiaryModel, String> mobileNumberColumn;

    @FXML
    private TableColumn<BeneficiaryModel, String> addedByColumn;

    @FXML
    private TableColumn<BeneficiaryModel, String> registeredDateColumn;

    @FXML
    private TableColumn<BeneficiaryModel, Void> actionsColumn;

    @FXML
    private TableView<BeneficiaryModel> beneficiaryTable;

    @FXML
    private Button refreshButton;

    @FXML
    private Button addButton;

    @FXML
    private Button searchBtn;

    @FXML
    private TextField searchField;

    @FXML
    private void initialize() {
        beneficiaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTableColumns();
        loadTable();
        actionButtons();
        EventHandler<ActionEvent> handlers = this::handleActions;

        refreshButton.setOnAction(handlers);
        searchBtn.setOnAction(handlers);
        addButton.setOnAction(handlers);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == refreshButton) {
            loadTable();
        } else if (src == searchBtn) {
            handleSearch();
            setSearchFld();
        } else if (src == addButton) {
            handleAddBeneficiary();
        }
    }

    private void handleAddBeneficiary() {
        try {
            AddBeneficiariesDialogController controller = DialogManager.getController("addBeneficiary", AddBeneficiariesDialogController.class);
            controller.setBeneficiaryService(beneficiaryService);
            controller.setBeneficiaryController(this);
            DialogManager.show("addBeneficiary");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load the Add Beneficiary dialog: " + e.getMessage());
        }
    }

    private void showEditBeneficiaryDialog(BeneficiaryModel bm) {
        try {
            EditBeneficiariesDialogController controller = DialogManager.getController("editBeneficiary", EditBeneficiariesDialogController.class);
            controller.setBeneficiaryService(beneficiaryService);
            controller.setBeneficiaryController(this);
            BeneficiaryModel beneficiaryModel = beneficiaryService.getBeneficiaryById(bm.getId());
            if (beneficiaryModel != null) {
                controller.setBeneficiary(beneficiaryModel);
            } else {
                AlertDialogManager.showError("Data Error",
                        "Unable to load beneficiary data. The record may have been deleted.");
                return;
            }
            DialogManager.show("editBeneficiary");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load the Edit Beneficiary dialog: " + e.getMessage());
        }
    }

    public void loadTable() {
        try {
            List<BeneficiaryModel> beneficiaries = beneficiaryService.getAllBeneficiary();
            beneficiaryList = FXCollections.observableArrayList(beneficiaries);
            beneficiaryTable.setItems(beneficiaryList);

            if (beneficiaries.isEmpty()) {
                beneficiaryTable.setPlaceholder(new Label("No beneficiaries found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error",
                    "Failed to load beneficiaries: " + e.getMessage());
        }
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        firstnameColumn.setCellValueFactory(new PropertyValueFactory<>("firstname"));
        middlenameColumn.setCellValueFactory(new PropertyValueFactory<>("middlename"));
        lastnameColumn.setCellValueFactory(new PropertyValueFactory<>("lastname"));
        birthdateColumn.setCellValueFactory(new PropertyValueFactory<>("birthDate"));
        genderColumn.setCellValueFactory(new PropertyValueFactory<>("gender"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("maritalStatus"));
        mobileNumberColumn.setCellValueFactory(new PropertyValueFactory<>("mobileNumber"));
        addedByColumn.setCellValueFactory(new PropertyValueFactory<>("addedBy"));
        registeredDateColumn.setCellValueFactory(new PropertyValueFactory<>("regDate"));
    }

    private void deleteById(BeneficiaryModel bm) {
        if (bm == null || bm.getId() <= 0) {
            AlertDialogManager.showError("Invalid Selection",
                    "Beneficiary ID is missing or invalid. Please select a valid beneficiary.");
            return;
        }

        boolean confirm = AlertDialogManager.showConfirmation("Delete Beneficiary",
                "Are you sure you want to delete beneficiary: " +
                        bm.getFirstname() + " " + bm.getLastname() + "?\n\n" +
                        "This action cannot be undone.");

        if (confirm) {
            try {
                boolean deleted = beneficiaryService.deleteBeneficiary(bm);
                if (deleted) {
                    beneficiaryList.remove(bm);
                    AlertDialogManager.showSuccess("Delete Successful",
                            "Beneficiary has been successfully deleted.");
                    DashboardRefresher.refresh();
                } else {
                    AlertDialogManager.showError("Delete Failed",
                            "Failed to delete beneficiary. Please try again.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                AlertDialogManager.showError("Delete Error",
                        "An error occurred while deleting beneficiary: " + e.getMessage());
            }
        }
    }

    private void handleSearch() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            loadTable();
        } else {
            searchAdmins(searchText);
        }
    }

    private void searchAdmins(String searchText) {
        try {
            List<BeneficiaryModel> filteredBeneficiaries = beneficiaryService.searchBeneficiary(searchText);
            beneficiaryList = FXCollections.observableArrayList(filteredBeneficiaries);
            beneficiaryTable.setItems(beneficiaryList);

            if (filteredBeneficiaries.isEmpty()) {
                beneficiaryTable.setPlaceholder(new Label("No beneficiaries found for: " + searchText));
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Search Error",
                    "Failed to search beneficiaries: " + e.getMessage());
        }
    }

    private void setSearchFld() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.trim().isEmpty()) {
                loadTable();
            }
        });
    }

    private void actionButtons() {
        Callback<TableColumn<BeneficiaryModel, Void>, TableCell<BeneficiaryModel, Void>> cellFactory =
                new Callback<>() {
                    @Override
                    public TableCell<BeneficiaryModel, Void> call(TableColumn<BeneficiaryModel, Void> param) {
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
                                    BeneficiaryModel bm = getTableView().getItems().get(getIndex());
                                    showEditBeneficiaryDialog(bm);
                                });

                                deleteButton.setOnAction(event -> {
                                    BeneficiaryModel bm = getTableView().getItems().get(getIndex());
                                    deleteById(bm);
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