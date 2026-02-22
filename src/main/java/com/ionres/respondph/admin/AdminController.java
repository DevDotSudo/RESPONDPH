package com.ionres.respondph.admin;

import com.ionres.respondph.admin.dialogs_controller.AddAdminDialogController;
import com.ionres.respondph.admin.dialogs_controller.EditAdminDialogController;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.AppContext;
import com.ionres.respondph.util.DialogManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.util.List;

public class AdminController {

    private final AdminService adminService = AppContext.adminService;
    private Stage dialogStage;
    ObservableList<AdminModel> adminList;

    // ─── FXML ─────────────────────────────────────────────────────────────────

    @FXML private AnchorPane rootPane;

    @FXML private TableView<AdminModel> adminTable;

    @FXML private TableColumn<AdminModel, Integer> idColumn;
    @FXML private TableColumn<AdminModel, String>  usernameColumn;
    @FXML private TableColumn<AdminModel, String>  fNameColumn;
    @FXML private TableColumn<AdminModel, String>  mNameColumn;
    @FXML private TableColumn<AdminModel, String>  lNameColumn;
    @FXML private TableColumn<AdminModel, String>  regDateColumn;
    @FXML private TableColumn<AdminModel, String>  roleColumn;     // ← NEW
    @FXML private TableColumn<AdminModel, Void>    actionsColumn;

    @FXML private TextField searchFld;
    @FXML private Button    searchBtn;
    @FXML private Button    addButton;
    @FXML private Button    refreshButton;

    // ─── Init ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        adminTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTableColumns();
        setupButtons();
        loadTable();
        actionButtons();
        setSearchFld();
    }

    // ─── Dialog Stage ─────────────────────────────────────────────────────────

    public void setDialogStage(Stage stage) { this.dialogStage = stage; }
    public Stage getDialogStage()           { return dialogStage; }

    // ─── Search ───────────────────────────────────────────────────────────────

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
        try {
            List<AdminModel> filtered = adminService.searchAdmin(searchText);
            adminList = FXCollections.observableArrayList(filtered);
            adminTable.setItems(adminList);

            if (filtered.isEmpty()) {
                adminTable.setPlaceholder(new Label("No administrators found for: " + searchText));
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Search Error",
                    "Failed to search administrators: " + e.getMessage());
        }
    }

    private void setSearchFld() {
        searchFld.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) loadTable();
        });
    }

    // ─── Table Setup ──────────────────────────────────────────────────────────

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        fNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstname"));
        mNameColumn.setCellValueFactory(new PropertyValueFactory<>("middlename"));
        lNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastname"));
        regDateColumn.setCellValueFactory(new PropertyValueFactory<>("regDate"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role")); // ← NEW

        // Center-align all columns
        idColumn.setStyle("-fx-alignment: CENTER;");
        usernameColumn.setStyle("-fx-alignment: CENTER;");
        fNameColumn.setStyle("-fx-alignment: CENTER;");
        mNameColumn.setStyle("-fx-alignment: CENTER;");
        lNameColumn.setStyle("-fx-alignment: CENTER;");
        regDateColumn.setStyle("-fx-alignment: CENTER;");
        roleColumn.setStyle("-fx-alignment: CENTER;");    // ← NEW
        actionsColumn.setStyle("-fx-alignment: CENTER;");

        // Color-code the role badge in the role column
        roleColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(role);
                    // Apply a subtle badge color per role
                    String color = switch (role) {
                        case "Admin"     -> "#c0392b"; // red
                        case "Brgy_Sec" -> "#2980b9"; // blue
                        case "MSWDO"      -> "#27ae60"; // green
                        case "LDRRMO"    -> "#8e44ad"; // purple
                        default          -> "#7f8c8d"; // grey
                    };
                    setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
                }
            }
        });
    }

    private void setupButtons() {
        addButton.setOnAction(e -> showAddAdminDialog());
        refreshButton.setOnAction(e -> loadTable());
        searchBtn.setOnAction(e -> handleSearch());
    }

    // ─── Load Table ───────────────────────────────────────────────────────────

    public void loadTable() {
        adminTable.setPlaceholder(new Label("Loading administrators..."));

        Task<List<AdminModel>> task = new Task<>() {
            @Override
            protected List<AdminModel> call() {
                return adminService.getAllAdmins();
            }
        };

        task.setOnSucceeded(evt -> {
            List<AdminModel> admins = task.getValue();
            adminList = FXCollections.observableArrayList(admins);
            adminTable.setItems(adminList);
            if (admins == null || admins.isEmpty()) {
                adminTable.setPlaceholder(new Label("No administrators found"));
            }
        });

        task.setOnFailed(evt -> {
            Throwable e = task.getException();
            if (e != null) e.printStackTrace();
            AlertDialogManager.showError("Load Error",
                    "Failed to load administrators: " + (e != null ? e.getMessage() : "Unknown error"));
            adminTable.setPlaceholder(new Label("Failed to load administrators"));
        });

        Thread th = new Thread(task, "admin-load-table-thread");
        th.setDaemon(true);
        th.start();
    }

    public void refreshAdminTable() {
        loadTable();
    }

    // ─── Dialogs ──────────────────────────────────────────────────────────────

    private void showAddAdminDialog() {
        try {
            AddAdminDialogController controller =
                    DialogManager.getController("addAdmin", AddAdminDialogController.class);

            controller.setAdminService(adminService);
            controller.setAdminController(this);

            DialogManager.show("addAdmin");

            if (controller.isAdminAdded()) {
                AlertDialogManager.showSuccess("Success",
                        "New administrator has been successfully added.");
                loadTable();
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load the Add Admin dialog: " + e.getMessage());
        }
    }

    private void showEditAdminDialog(AdminModel selectedAdmin) {
        try {
            EditAdminDialogController editController =
                    DialogManager.getController("editAdmin", EditAdminDialogController.class);

            editController.setAdminService(adminService);
            editController.setAdminController(this);
            editController.setAdminData(selectedAdmin);

            DialogManager.show("editAdmin");

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load the Edit Admin dialog: " + e.getMessage());
        }
    }

    private void deleteById(AdminModel am) {
        if (am == null || am.getId() <= 0) {
            AlertDialogManager.showError("Invalid Selection",
                    "Admin ID is missing or invalid. Please select a valid administrator.");
            return;
        }

        boolean confirm = AlertDialogManager.showConfirmation("Delete Administrator",
                "Are you sure you want to delete this administrator?");

        if (confirm) {
            try {
                boolean success = adminService.deleteAdmin(am);

                if (success) {
                    adminList.remove(am);
                    adminTable.refresh();
                    AlertDialogManager.showSuccess("Delete Successful",
                            "Administrator has been successfully deleted.");
                } else {
                    AlertDialogManager.showError("Delete Failed",
                            "Unable to delete administrator. Please try again.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                AlertDialogManager.showError("Delete Error",
                        "An error occurred while deleting administrator: " + e.getMessage());
            }
        }
    }

    // ─── Action Buttons (Edit / Delete) ───────────────────────────────────────

    private void actionButtons() {
        Callback<TableColumn<AdminModel, Void>, TableCell<AdminModel, Void>> cellFactory =
                col -> new TableCell<>() {

                    private final FontAwesomeIconView editIcon   = new FontAwesomeIconView(FontAwesomeIcon.EDIT);
                    private final FontAwesomeIconView deleteIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
                    private final Button editButton   = new Button("", editIcon);
                    private final Button deleteButton = new Button("", deleteIcon);

                    {
                        editIcon.getStyleClass().add("edit-icon");
                        deleteIcon.getStyleClass().add("delete-icon");
                        editButton.getStyleClass().add("edit-button");
                        deleteButton.getStyleClass().add("delete-button");

                        editButton.setOnAction(e -> {
                            AdminModel admin = getTableView().getItems().get(getIndex());
                            showEditAdminDialog(admin);
                        });

                        deleteButton.setOnAction(e -> {
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

        actionsColumn.setCellFactory(cellFactory);
    }
}