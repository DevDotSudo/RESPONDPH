package com.ionres.respondph.admin;

import com.ionres.respondph.admin.dialogs_controller.AddAdminDialogController;
import javafx.beans.value.ObservableValue;
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
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.io.IOException;
import java.util.List;

public class AdminController {

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
    }

    @FXML
    private void handleSearch() {
        System.out.println(searchFld.getText());
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

        searchBtn.setOnAction(event -> System.out.println(searchFld.getText()));
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
            showErrorAlert("Error", "Unable to load the Add Admin dialog.");
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
                        editButton.getStyleClass().add("action-button");
                        deleteButton.getStyleClass().add("delete-button");

                        editButton.setOnAction(event -> {
                            AdminModel admin = getTableView().getItems().get(getIndex());
                            System.out.println("Edit: " + admin.getId());
                        });

                        deleteButton.setOnAction(event -> {
                            AdminModel admin = getTableView().getItems().get(getIndex());
                            adminList.remove(admin);
                            System.out.println("Deleted: " + admin.getId());
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox box = new HBox(10, editButton, deleteButton);
                            setGraphic(box);
                        }
                    }
                };
            }
        };
        actionsColumn.setCellFactory(cellFactory);
    }
}

