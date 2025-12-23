package com.ionres.respondph.admin;

import com.ionres.respondph.admin.dialogs_controller.AddAdminDialogController;
import com.ionres.respondph.util.AppContext;
import com.ionres.respondph.util.DialogManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.util.List;
import com.ionres.respondph.admin.dialogs_controller.EditAdminDialogController;
import java.util.Optional;
import com.ionres.respondph.util.AlertDialog;

public class AdminController {
    AlertDialog alertDialog = new AlertDialog();
    private final AdminService adminService = AppContext.adminService;
    private Stage dialogStage;
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

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public Stage getDialogStage() {
        return dialogStage;
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

        // Center align all columns
        idColumn.setStyle("-fx-alignment: CENTER;");
        usernameColumn.setStyle("-fx-alignment: CENTER;");
        fNameColumn.setStyle("-fx-alignment: CENTER;");
        mNameColumn.setStyle("-fx-alignment: CENTER;");
        lNameColumn.setStyle("-fx-alignment: CENTER;");
        regDateColumn.setStyle("-fx-alignment: CENTER;");
        actionsColumn.setStyle("-fx-alignment: CENTER;");
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
            AddAdminDialogController controller =
                    DialogManager.getController("addAdmin", AddAdminDialogController.class);

            controller.setAdminService(adminService);
            controller.setAdminController(this);

            DialogManager.show("addAdmin");

            if (controller.isAdminAdded()) {
                loadTable();
            }

        } catch (Exception e) {
            e.printStackTrace();
            alertDialog.showErrorAlert("Error", "Unable to load the Add Admin dialog.");
        }
    }

    private void showEditAdminDialog(AdminModel selectedAdmin) {
        try {
            EditAdminDialogController editAdminDialogController = DialogManager.getController("editAdmin", EditAdminDialogController.class);
            editAdminDialogController.setAdminService(adminService);
            editAdminDialogController.setAdminController(this);
            editAdminDialogController.setAdminData(selectedAdmin);
            DialogManager.show("editAdmin");
        } catch (Exception e) {
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
                        editIcon.getStyleClass().add("edit-icon");
                        deleteIcon.getStyleClass().add("delete-icon");
                        editButton.getStyleClass().add("edit-button");
                        deleteButton.getStyleClass().add("delete-button");

                        editButton.setOnAction(event -> {
                            AdminModel admin = getTableView().getItems().get(getIndex());
                            showEditAdminDialog(admin);
                        });

                        deleteButton.setOnAction(event -> {
                            AdminModel admin = getTableView().getItems().get(getIndex());
                            deleteById(admin);
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