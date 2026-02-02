package com.ionres.respondph.evac_site;

import com.ionres.respondph.evac_plan.dialogs_controller.AllocateEvacSiteController;
import com.ionres.respondph.evac_site.dialogs_controller.AddEvacSiteController;
import com.ionres.respondph.evac_site.dialogs_controller.EditEvacSiteController;
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
import java.util.List;

public class EvacSiteController {

    private final EvacSiteService evacSiteService = AppContext.evacSiteService;
    private ObservableList<EvacSiteModel> evacSiteList;

    @FXML private AnchorPane root;
    @FXML private TextField searchField;
    @FXML private Button searchBtn, addBtn, refreshBtn;
    @FXML private TableView<EvacSiteModel> evacSitesTbl;
    @FXML private TableColumn<EvacSiteModel, String> idColumn;
    @FXML private TableColumn<EvacSiteModel, String> capacityColumn;

    @FXML private TableColumn<EvacSiteModel, String> nameColumn;
    @FXML private TableColumn<EvacSiteModel, String> notesColumn;
    @FXML private TableColumn<EvacSiteModel, Void> actionsColumn;

    @FXML
    private void initialize() {
        evacSitesTbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTableColumns();
        setupActionButtons();
        setupSearchListener();
        loadTable();
        EventHandler<ActionEvent> handler = this::handleActions;
        searchBtn.setOnAction(handler);
        addBtn.setOnAction(handler);
        refreshBtn.setOnAction(handler);
    }

    private void handleActions(ActionEvent e) {
        Object src = e.getSource();

        if (src == searchBtn) {
            handleSearch();
        } else if (src == addBtn) {
            handleAddSite();
        } else if (src == refreshBtn) {
            handleRefresh();
        }
    }


    private void handleRefresh() {
        try {
            // Get the correct controller type for the allocateEvacSite dialog
            AllocateEvacSiteController controller = DialogManager.getController(
                    "allocateEvacSite", AllocateEvacSiteController.class);

            // Set required dependencies
            controller.setEvacSiteService(this.evacSiteService);
            controller.setEvacPlanController(AppContext.evacPlanController); // You'll need this from AppContext
            controller.setDisasterService(AppContext.disasterService); // You'll need this from AppContext

            // Show the dialog
            DialogManager.show("allocateEvacSite");

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load Allocate Evacuation Site dialog: " + e.getMessage());
        }
    }


    private void handleSearch() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            loadTable();
        } else {
            searchEvacSites(searchText);
        }
    }

    private void handleAddSite() {
        showAddEvacSiteDialog();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("evacId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        capacityColumn.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        notesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));
    }

    public void loadTable() {
        try {
            List<EvacSiteModel> evacSites = evacSiteService.getAllEvacSites();
            evacSiteList = FXCollections.observableArrayList(evacSites);
            evacSitesTbl.setItems(evacSiteList);

            if (evacSites.isEmpty()) {
                evacSitesTbl.setPlaceholder(new Label("No evacuation sites found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error",
                    "Failed to load evacuation sites: " + e.getMessage());
        }
    }

    private void deleteEvacSite(EvacSiteModel evacSite) {
        if (evacSite == null || evacSite.getEvacId() <= 0) {
            AlertDialogManager.showError("Invalid Selection",
                    "Evacuation Site ID is missing or invalid. Please select a valid site.");
            return;
        }

        boolean confirm = AlertDialogManager.showConfirmation(
                "Delete Evacuation Site",
                "Are you sure you want to delete this evacuation site?"
        );

        if (confirm) {
            try {
                boolean success = evacSiteService.deleteEvacSite(evacSite);

                if (success) {
                    evacSiteList.remove(evacSite);
                    AlertDialogManager.showSuccess("Delete Successful",
                            "Evacuation site has been successfully deleted.");
                } else {
                    AlertDialogManager.showError("Delete Failed",
                            "Failed to delete evacuation site. Please try again.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                AlertDialogManager.showError("Delete Error",
                        "An error occurred while deleting evacuation site: " + e.getMessage());
            }
        }
    }

    private void showAddEvacSiteDialog() {
        try {
            AddEvacSiteController controller = DialogManager.getController(
                    "evacSite", AddEvacSiteController.class);
            controller.setEvacSiteService(this.evacSiteService);
            controller.setEvacSiteController(this);
            DialogManager.show("evacSite");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load Add Evacuation Site dialog: " + e.getMessage());
        }
    }

    private void showEditEvacSiteDialog(EvacSiteModel evacSite) {
        try {
            EditEvacSiteController controller = DialogManager.getController(
                    "editEvacSite", EditEvacSiteController.class);
            controller.setEvacSiteService(evacSiteService);
            controller.setEvacSiteController(this);

            EvacSiteModel fullEvacSite = evacSiteService.getEvacSiteById(evacSite.getEvacId());
            if (fullEvacSite != null) {
                controller.setEvacSite(fullEvacSite);
            } else {
                AlertDialogManager.showError("Data Error",
                        "Unable to load evacuation site data. The record may have been deleted.");
                return;
            }
            DialogManager.show("editEvacSite");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load Edit Evacuation Site dialog: " + e.getMessage());
        }
    }

    private void searchEvacSites(String searchText) {
        try {
            List<EvacSiteModel> filteredEvacSites = evacSiteService.searchEvacSite(searchText);
            evacSiteList = FXCollections.observableArrayList(filteredEvacSites);
            evacSitesTbl.setItems(evacSiteList);

            if (filteredEvacSites.isEmpty()) {
                evacSitesTbl.setPlaceholder(new Label("No evacuation sites found for: " + searchText));
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Search Error",
                    "Failed to search evacuation sites: " + e.getMessage());
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
        Callback<TableColumn<EvacSiteModel, Void>, TableCell<EvacSiteModel, Void>> cellFactory =
                new Callback<>() {
                    @Override
                    public TableCell<EvacSiteModel, Void> call(TableColumn<EvacSiteModel, Void> param) {
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
                                    EvacSiteModel evacSite = getTableView().getItems().get(getIndex());
                                    showEditEvacSiteDialog(evacSite);
                                });

                                deleteButton.setOnAction(event -> {
                                    EvacSiteModel evacSite = getTableView().getItems().get(getIndex());
                                    deleteEvacSite(evacSite);
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