package com.ionres.respondph.admin;

import com.ionres.respondph.admin.dialogs_controller.AddAdminDialogController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;

import java.awt.event.ActionEvent;
import java.beans.EventHandler;
import java.io.IOException;
import java.util.EventListener;
import java.util.List;
import com.ionres.respondph.admin.dialogs_controller.EditAdminDialogController;
import java.util.Optional;
import com.ionres.respondph.util.AlertDialog;

public class AdminController {
    AlertDialog alertDialog = new AlertDialog();
    private AdminService adminService = new AdminServiceImpl();
    ObservableList<AdminModel> adminList;
    @FXML
    private AnchorPane rootPane;

    @FXML
    private TableView<AdminModel> adminTable;

    @FXML
    private TableColumn<AdminModel, String> idColumn;

    @FXML
    private TableColumn<AdminModel, String> usernameColumn;

    @FXML
    private TableColumn<AdminModel, Integer> fNameColumn;

    @FXML
    private TableColumn<AdminModel, String> mNameColumn;

    @FXML
    private TableColumn<AdminModel, String> lNameColumn;

    @FXML
    private TableColumn<AdminModel, String> regDateColumn;

    @FXML
    private TableColumn<AdminModel, Void> actionsColumn;

    @FXML
    private TextField searchFld;

    @FXML
    private Button searchBtn;

    @FXML
    private Button addButton;

    @FXML
    private Button refreshButton;

    @FXML
    public void initialize() {
        adminTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTableColumns();
        setupButtons();
        loadTable();
        actionButtons();
        setSearchFld();

    }
    @FXML
    private void handleSearch() {
        String searchText = searchFld.getText().trim();
        if (searchText.isEmpty()) {
           loadTable();
        } else {
            searchAdmins(searchText);
        }
    }
    private void searchAdmins(String searchText) {
        List<AdminModel> filteredAdmins = adminService.searchAdmin(searchText);
        adminList = FXCollections.observableArrayList(filteredAdmins);
        adminTable.setItems(adminList);
    }

    private void setSearchFld(){
        searchFld.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.trim().isEmpty()) {
                loadTable();
            }
        });
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        fNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstname"));
        mNameColumn.setCellValueFactory(new PropertyValueFactory<>("middlename"));
        lNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastname"));
        regDateColumn.setCellValueFactory(new PropertyValueFactory<>("regDate"));
    }

    private void setupButtons() {
        addButton.setOnAction(event -> showAddAdminDialog());

        refreshButton.setOnAction(event -> loadTable());

        searchBtn.setOnAction(event -> handleSearch());
    }

    public void loadTable() {
        List<AdminModel> admins = adminService.getAllAdmins();

        adminList = FXCollections.observableArrayList(admins);
        adminTable.setItems(adminList);
    }

    public void refreshAdminTable() {
        loadTable();
    }

    private void showAddAdminDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dialogs/AddAdminDialog.fxml"));
            Parent dialogRoot = loader.load();

            AddAdminDialogController dialogController = loader.getController();

            dialogController.setAdminService(this.adminService);
            dialogController.setAdminController(this);

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UNDECORATED);
            dialogStage.setTitle("Add New Admin");

            dialogController.setDialogStage(dialogStage);

            Scene scene = new Scene(dialogRoot);
            dialogStage.setScene(scene);

            dialogStage.showAndWait();

            if (dialogController.isAdminAdded()) {
                loadTable();
            }

        } catch (IOException e) {
            e.printStackTrace();
            alertDialog.showErrorAlert("Error", "Unable to load the Add Admin dialog.");
        }
    }

    private void showEditAdminDialog(AdminModel selectedAdmin) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dialogs/EditAdminDialog.fxml"));
            Parent dialogRoot = loader.load();

            EditAdminDialogController dialogController = loader.getController();

            dialogController.setAdminService(this.adminService);
            dialogController.setAdminController(this);

            dialogController.setAdminData(selectedAdmin);

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UNDECORATED);
            dialogStage.setTitle("Edit Admin Info");

            dialogController.setDialogStage(dialogStage);

            Scene scene = new Scene(dialogRoot);
            dialogStage.setScene(scene);

            dialogStage.showAndWait();

            loadTable();

        } catch (IOException e) {
            e.printStackTrace();
            alertDialog.showErrorAlert("Error", "Unable to load the Edit Admin dialog.");
        }
    }


    private void deleteById(AdminModel am){
        if (am == null || am.getId() <= 0) {
            alertDialog.showErrorAlert("Invalid Selection", "Admin ID is missing or invalid.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Are you sure you want to delete this admin?");
        confirm.setContentText("Username: " + am.getUsername());

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {

            boolean success = adminService.deleteAdmin(am);

            if (success) {
                adminList.remove(am);
                adminTable.refresh();
                alertDialog.showSuccess("Success", "Admin deleted successfully.");
            } else {
                alertDialog.showErrorAlert("Failed", "Unable to delete admin.");
            }
        }

    }

    private void actionButtons() {
        Callback<TableColumn<AdminModel, Void>, TableCell<AdminModel, Void>> cellFactory = new Callback<TableColumn<AdminModel, Void>, TableCell<AdminModel, Void>>() {
            @Override
            public TableCell<AdminModel, Void> call(TableColumn<AdminModel, Void> adminModelVoidTableColumn) {
                return new TableCell<>() {
                    private final FontAwesomeIconView editIcon = new FontAwesomeIconView(FontAwesomeIcon.EDIT);
                    private final FontAwesomeIconView deleteIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
                    private final Button editButton = new Button("", editIcon);
                    private final Button deleteButton = new Button("", deleteIcon);

                    {
                        editButton.getStyleClass().add("edit-button");
                        deleteButton.getStyleClass().add("delete-button");

                        editButton.setOnAction(event -> {
                            AdminModel admin = getTableView().getItems().get(getIndex());
                            showEditAdminDialog(admin);
                        });

                        deleteButton.setOnAction(event -> {
                            AdminModel admin = getTableView().getItems().get(getIndex());
                            deleteById(admin);
                            loadTable();
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox box = new HBox(editButton, deleteButton);
                            box.getStyleClass().add("button-box");
                            setGraphic(box);
                        }
                    }
                };
            }
        };
        actionsColumn.setCellFactory(cellFactory);
    }
}

