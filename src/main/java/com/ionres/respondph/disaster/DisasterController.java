package com.ionres.respondph.disaster;

import com.ionres.respondph.disaster.dialogs_controller.AddDisasterDialogController;
import com.ionres.respondph.disaster.dialogs_controller.EditDisasterDialogController;
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

public class DisasterController {

    private final DisasterService disasterService = AppContext.disasterService;
    private ObservableList<DisasterModel> disasterList;

    @FXML private AnchorPane root;
    @FXML private TextField searchField;
    @FXML private Button searchBtn, addBtn, refreshBtn;
    @FXML private TableView<DisasterModel> disastersTbl;
    @FXML private TableColumn<DisasterModel, String> disaster_id;
    @FXML private TableColumn<DisasterModel, String> type;
    @FXML private TableColumn<DisasterModel, String> disasterColumn;
    @FXML private TableColumn<DisasterModel, String> dateColumn;
    @FXML private TableColumn<DisasterModel, String> regDateColumn;
    @FXML private TableColumn<DisasterModel, String> notesColumn;
    @FXML private TableColumn<DisasterModel, Void> actionsColumn;

    @FXML
    private void initialize() {
        disastersTbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTableColumns();
        setupActionButtons();
        setupSearchListener();
        loadTable();
        EventHandler<ActionEvent> handler = this::handleActions;
        searchBtn.setOnAction(handler);
        addBtn.setOnAction(handler);
        refreshBtn.setOnAction(handler);
    }

    private void handleActions (ActionEvent e) {
        Object src = e.getSource();

        if (src == searchBtn) {
            handleSearch();
        }
        else if (src == addBtn) {
            handleAddDisaster();
        }
        else if (src == refreshBtn) {
            handleRefresh();
        }
    }

    private void handleRefresh() {
        loadTable();
    }

    private void handleSearch() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            loadTable();
        } else {
            searchDisasters(searchText);
        }
    }

    private void handleAddDisaster() {
        showAddDisasterDialog();
    }

    private void setupTableColumns() {
        disaster_id.setCellValueFactory(new PropertyValueFactory<>("disasterId"));
        type.setCellValueFactory(new PropertyValueFactory<>("disasterType"));
        disasterColumn.setCellValueFactory(new PropertyValueFactory<>("disasterName"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        notesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));
        regDateColumn.setCellValueFactory(new PropertyValueFactory<>("regDate"));
    }

    public void loadTable() {
        try {
            List<DisasterModel> disasters = disasterService.getAllDisaster();
            disasterList = FXCollections.observableArrayList(disasters);
            disastersTbl.setItems(disasterList);

            if (disasters.isEmpty()) {
                disastersTbl.setPlaceholder(new Label("No disasters found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error",
                    "Failed to load disasters: " + e.getMessage());
        }
    }

    private void deleteDisaster(DisasterModel disaster) {
        if (disaster == null || disaster.getDisasterId() <= 0) {
            AlertDialogManager.showError("Invalid Selection",
                    "Disaster ID is missing or invalid. Please select a valid disaster.");
            return;
        }

        boolean confirm = AlertDialogManager.showConfirmation(
                "Delete Disaster",
                "Are you sure you want to delete this disaster?"
        );

        if (confirm) {
            try {
                boolean success = disasterService.deleteDisaster(disaster);

                if (success) {
                    disasterList.remove(disaster);
                    AlertDialogManager.showSuccess("Delete Successful",
                            "Disaster has been successfully deleted.");
                    DashboardRefresher.refresh();
                    DashboardRefresher.refreshComboBoxOfDNAndAN();
                } else {
                    AlertDialogManager.showError("Delete Failed",
                            "Failed to delete disaster. Please try again.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                AlertDialogManager.showError("Delete Error",
                        "An error occurred while deleting disaster: " + e.getMessage());
            }
        }
    }

    private void showAddDisasterDialog() {
        try {
            AddDisasterDialogController controller = DialogManager.getController(
                    "addDisaster", AddDisasterDialogController.class);
            controller.setDisasterService(this.disasterService);
            controller.setDisasterController(this);
            DialogManager.show("addDisaster");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load Add Disaster dialog: " + e.getMessage());
        }
    }

    private void showEditDisasterDialog(DisasterModel disaster) {
        try {
            EditDisasterDialogController controller = DialogManager.getController(
                    "editDisaster", EditDisasterDialogController.class);
            controller.setDisasterService(disasterService);
            controller.setDisasterController(this);

            DisasterModel fullDisaster = disasterService.getDisasterById(disaster.getDisasterId());
            if (fullDisaster != null) {
                controller.setDisaster(fullDisaster);
            } else {
                AlertDialogManager.showError("Data Error",
                        "Unable to load disaster data. The record may have been deleted.");
                return;
            }
            DialogManager.show("editDisaster");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load Edit Disaster dialog: " + e.getMessage());
        }
    }

    private void searchDisasters(String searchText) {
        try {
            List<DisasterModel> filteredDisasters = disasterService.searchDisaster(searchText);
            disasterList = FXCollections.observableArrayList(filteredDisasters);
            disastersTbl.setItems(disasterList);

            if (filteredDisasters.isEmpty()) {
                disastersTbl.setPlaceholder(new Label("No disasters found for: " + searchText));
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Search Error",
                    "Failed to search disasters: " + e.getMessage());
        }
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.trim().isEmpty()) {
                loadTable();
            }
        });
    }

    private void setupActionButtons() {
        Callback<TableColumn<DisasterModel, Void>, TableCell<DisasterModel, Void>> cellFactory =
                new Callback<>() {
                    @Override
                    public TableCell<DisasterModel, Void> call(TableColumn<DisasterModel, Void> param) {
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
                                    DisasterModel disaster = getTableView().getItems().get(getIndex());
                                    showEditDisasterDialog(disaster);
                                });

                                deleteButton.setOnAction(event -> {
                                    DisasterModel disaster = getTableView().getItems().get(getIndex());
                                    deleteDisaster(disaster);
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