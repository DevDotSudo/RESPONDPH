package com.ionres.respondph.disaster_damage;

import com.ionres.respondph.disaster_damage.dialogs_controller.AddDisasterDamageDialogController;
import com.ionres.respondph.disaster_damage.dialogs_controller.EditDisasterDamageDialogController;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.AppContext;
import com.ionres.respondph.util.DialogManager;
import com.ionres.respondph.util.UpdateTrigger;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.property.SimpleStringProperty;
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

public class DisasterDamageController {

    private final DisasterDamageService disasterDamageService = AppContext.disasterDamageService;
    private ObservableList<DisasterDamageModel> disasterList;

    @FXML private AnchorPane root;
    @FXML private TextField searchField;
    @FXML private Button searchBtn, addBtn, refreshBtn;
    @FXML private TableView<DisasterDamageModel> disastersDamageTbl;
    @FXML private TableColumn<DisasterDamageModel, String> damage_id;
    @FXML private TableColumn<DisasterDamageModel, String> beneficiaryNameColumn;
    @FXML private TableColumn<DisasterDamageModel, String> disasterColumn;
    @FXML private TableColumn<DisasterDamageModel, String> damageSeverityColumn;
    @FXML private TableColumn<DisasterDamageModel, String> assessmentDateColumn;
    @FXML private TableColumn<DisasterDamageModel, String> verifiedByColumn;
    @FXML private TableColumn<DisasterDamageModel, String> registeredDateColumn;
    @FXML private TableColumn<DisasterDamageModel, Void> actionsColumn;

    @FXML
    private void initialize() {
        disastersDamageTbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTableColumns();
        loadTable();
        setupActionButtons();
        setupSearchListener();

        EventHandler<ActionEvent> handler = this::handleActions;
        searchBtn.setOnAction(handler);
        addBtn.setOnAction(handler);
        refreshBtn.setOnAction(handler);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == searchBtn) {
            handleSearch();
        }
        else if(src == addBtn){
            handleAddDisasterDamage();
        }
        else if(src == refreshBtn){
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
            searchDisasterDamage(searchText);
        }
    }

    private void handleAddDisasterDamage() {
        showAddDisasterDamageDialog();
    }

    private void setupTableColumns() {
        damage_id.setCellValueFactory(new PropertyValueFactory<>("beneficiaryDisasterDamageId"));

        beneficiaryNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getBeneficiaryId() + " - " +
                                cellData.getValue().getBeneficiaryFirstname()
                )
        );

        disasterColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getDisasterId() + " - " +
                                cellData.getValue().getDisasterType()
                )
        );

        damageSeverityColumn.setCellValueFactory(new PropertyValueFactory<>("houseDamageSeverity"));
        assessmentDateColumn.setCellValueFactory(new PropertyValueFactory<>("assessmentDate"));
        verifiedByColumn.setCellValueFactory(new PropertyValueFactory<>("verifiedBy"));
        registeredDateColumn.setCellValueFactory(new PropertyValueFactory<>("regDate"));
    }

    public void loadTable() {
        try {
            List<DisasterDamageModel> disasterDamage = disasterDamageService.getAllDisasterDamage();
            disasterList = FXCollections.observableArrayList(disasterDamage);
            disastersDamageTbl.setItems(disasterList);

            if (disasterDamage.isEmpty()) {
                disastersDamageTbl.setPlaceholder(new Label("No disaster damage records found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Load Error",
                    "Failed to load disaster damage records: " + e.getMessage());
        }
    }

    private void deleteDisasterDamage(DisasterDamageModel disasterDamage) {
        if (disasterDamage == null || disasterDamage.getBeneficiaryDisasterDamageId() <= 0) {
            AlertDialogManager.showError("Invalid Selection",
                    "Disaster damage ID is missing or invalid. Please select a valid record.");
            return;
        }

        boolean confirm = AlertDialogManager.showConfirmation(
                "Delete Disaster Damage Record",
                "Are you sure you want to delete this disaster damage record?"
        );

        if (confirm) {
            try {
                boolean success = disasterDamageService.deleteDisasterDamage(disasterDamage);

                if (success) {

                    int beneficiaryId = disasterDamage.getBeneficiaryId();
                    UpdateTrigger updateTrigger = new UpdateTrigger();
                    boolean cascadeSuccess = updateTrigger.triggerCascadeUpdate(beneficiaryId);

                    if (cascadeSuccess) {
                        AlertDialogManager.showSuccess("Success",
                                "Disaster damage record has been successfully Deleted.\n" +
                                        "Household and aid scores have been automatically recalculated.");
                    } else {
                        AlertDialogManager.showWarning("Partial Success",
                                "Disaster damage record has been added, but score recalculation encountered issues.\n" +
                                        "Please check the console for details.");
                    }

                    disasterList.remove(disasterDamage);
                } else {
                    AlertDialogManager.showError("Delete Failed",
                            "Failed to delete disaster damage record. Please try again.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                AlertDialogManager.showError("Delete Error",
                        "An error occurred while deleting disaster damage record: " + e.getMessage());
            }
        }
    }

    private void showAddDisasterDamageDialog() {
        try {
            AddDisasterDamageDialogController controller = DialogManager.getController(
                    "addDisasterDamage", AddDisasterDamageDialogController.class);
            controller.setDisasterDamageService(disasterDamageService);
            controller.setDisasterDamageController(this);
            DialogManager.show("addDisasterDamage");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load Add Disaster Damage dialog: " + e.getMessage());
        }
    }

    private void showEditDisasterDamageDialog(DisasterDamageModel disasterDamage) {
        try {
            EditDisasterDamageDialogController controller = DialogManager.getController(
                    "editDisasterDamage", EditDisasterDamageDialogController.class);
            controller.setDisasterDamageService(this.disasterDamageService);
            controller.setDisasterDamageController(this);

            DisasterDamageModel fullDisasterDamage = disasterDamageService.getDisasterDamageId(
                    disasterDamage.getBeneficiaryDisasterDamageId()
            );

            if (fullDisasterDamage != null) {
                controller.setDisasterDamage(fullDisasterDamage);
            } else {
                AlertDialogManager.showError("Data Error",
                        "Unable to load disaster damage data. The record may have been deleted.");
                return;
            }
            DialogManager.show("editDisasterDamage");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load Edit Disaster Damage dialog: " + e.getMessage());
        }
    }

    private void searchDisasterDamage(String searchText) {
        try {
            List<DisasterDamageModel> filteredDisasterDamage =
                    disasterDamageService.searchDisasterDamage(searchText);

            disasterList = FXCollections.observableArrayList(filteredDisasterDamage);
            disastersDamageTbl.setItems(disasterList);

            if (filteredDisasterDamage.isEmpty()) {
                disastersDamageTbl.setPlaceholder(new Label("No records found for: " + searchText));
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Search Error",
                    "Failed to search disaster damage records: " + e.getMessage());
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
        Callback<TableColumn<DisasterDamageModel, Void>, TableCell<DisasterDamageModel, Void>> cellFactory =
                new Callback<>() {
                    @Override
                    public TableCell<DisasterDamageModel, Void> call(TableColumn<DisasterDamageModel, Void> param) {
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
                                    DisasterDamageModel disasterDamage = getTableView().getItems().get(getIndex());
                                    showEditDisasterDamageDialog(disasterDamage);
                                });

                                deleteButton.setOnAction(event -> {
                                    DisasterDamageModel disasterDamage = getTableView().getItems().get(getIndex());
                                    deleteDisasterDamage(disasterDamage);
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