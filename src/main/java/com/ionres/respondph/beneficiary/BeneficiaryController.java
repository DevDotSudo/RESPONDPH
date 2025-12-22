package com.ionres.respondph.beneficiary;

import com.ionres.respondph.admin.AdminController;
import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.admin.AdminService;
import com.ionres.respondph.admin.dialogs_controller.AddAdminDialogController;
import com.ionres.respondph.beneficiary.dialogs_controller.AddBeneficiariesDialogController;
import com.ionres.respondph.beneficiary.dialogs_controller.EditBeneficiariesDialogController;
import com.ionres.respondph.util.AlertDialog;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class BeneficiaryController {

    BeneficiaryService beneficiaryService = new BeneficiaryServiceImpl();
    ObservableList<BeneficiaryModel> beneficiaryList;
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dialogs/AddBeneficiariesDialog.fxml"));
    Stage dialogStage = new Stage();
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);

    AlertDialog alertDialog = new AlertDialog();

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

            } else if(src == addButton) {
                handleAddBeneficiary();
            }
        }

        @FXML
        private void handleAddBeneficiary() {

            try {
                Parent dialogRoot = loader.load();


                AddBeneficiariesDialogController dialogController = loader.getController();


                dialogController.setBeneficiaryService(this.beneficiaryService);
                dialogController.setBeneficiaryController(this);


                dialogStage.initModality(Modality.APPLICATION_MODAL);
                dialogStage.initStyle(StageStyle.UNDECORATED);
                dialogStage.setTitle("Add New Beneficiary");


                Scene scene = new Scene(dialogRoot);

                dialogStage.setScene(scene);
                dialogStage.showAndWait();

            } catch (IOException e) {
                e.printStackTrace();
                alertDialog.showErrorAlert("Error", "Unable to load the Add Admin dialog.");
            }
        }

        private void showEditBeneficiaryDialog(BeneficiaryModel beneficiaryModel) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dialogs/EditBeneficiariesDialog.fxml"));
                Parent dialogRoot = loader.load();

                EditBeneficiariesDialogController dialogController = loader.getController();

                dialogController.setBeneficiaryService(this.beneficiaryService);
                dialogController.setBeneficiaryController(this);

                Stage dialogStage = new Stage();
                dialogController.setDialogStage(dialogStage);

                BeneficiaryModel fullBeneficiary = beneficiaryService.getBeneficiaryById(beneficiaryModel.getId());
                if (fullBeneficiary != null) {
                    dialogController.setBeneficiary(fullBeneficiary);
                } else {
                    alertDialog.showErrorAlert("Error", "Unable to load beneficiary data.");
                    return;
                }

                dialogStage.initModality(Modality.APPLICATION_MODAL);
                dialogStage.initStyle(StageStyle.UNDECORATED);
                dialogStage.setTitle("Update Beneficiary");

                Scene scene = new Scene(dialogRoot);
                dialogStage.setScene(scene);
                dialogStage.showAndWait();

            } catch (IOException e) {
                e.printStackTrace();
                alertDialog.showErrorAlert("Error", "Unable to load the Edit Beneficiary dialog.");
            }
        }


        public void loadTable() {
            List<BeneficiaryModel> admins = beneficiaryService.getAllBeneficiary();

            beneficiaryList = FXCollections.observableArrayList(admins);
            beneficiaryTable.setItems(beneficiaryList);
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
                alertDialog.showErrorAlert("Invalid Selection", "Admin ID is missing or invalid.");
                return;
            }

            confirm.setTitle("Confirm Delete");
            confirm.setHeaderText("Are you sure you want to delete this Beneficiary?");
            confirm.setContentText("Username: " + bm.getFirstname());

            Optional<ButtonType> result = confirm.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {

                boolean success = beneficiaryService.deleteBeneficiary(bm);

                beneficiaryList.remove(bm);
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
            List<BeneficiaryModel> filteredAdmins = beneficiaryService.searchBeneficiary(searchText);
            beneficiaryList = FXCollections.observableArrayList(filteredAdmins);
            beneficiaryTable.setItems(beneficiaryList);
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
                                    editButton.getStyleClass().add("action-button");
                                    deleteButton.getStyleClass().add("delete-button");

                                    editButton.setOnAction(event -> {
                                        BeneficiaryModel bm = getTableView().getItems().get(getIndex());

                                        showEditBeneficiaryDialog(bm);
                                    });

                                    deleteButton.setOnAction(event -> {
                                        BeneficiaryModel bm = getTableView().getItems().get(getIndex());
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
                                        HBox box = new HBox(5, editButton, deleteButton);
                                        setGraphic(box);
                                    }
                                }
                            };
                        }
                    };

            actionsColumn.setCellFactory(cellFactory);
        }
    }
