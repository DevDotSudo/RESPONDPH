package com.ionres.respondph.disaster;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.disaster.dialogs_controller.AddDisasterDialogController;
import com.ionres.respondph.disaster.dialogs_controller.EditDisasterDialogController;
import com.ionres.respondph.util.AlertDialog;
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
import java.util.Optional;

public class DisasterController {
    AlertDialog alertDialog = new AlertDialog();
    DisasterService disasterService = AppContext.disasterService;
    ObservableList<DisasterModel> disasterList;

    @FXML
    private AnchorPane root;
    @FXML
    private TextField searchField;
    @FXML
    private Button searchBtn, addBtn, refreshBtn;
    @FXML
    private TableView disastersTbl;
    @FXML
    private TableColumn<DisasterModel, String> disaster_id;
    @FXML
    private TableColumn<DisasterModel, String> type;
    @FXML
    private TableColumn<DisasterModel, String> disasterColumn;
    @FXML
    private TableColumn<DisasterModel, String> dateColumn;
    @FXML
    private  TableColumn<DisasterModel, String> regDateColumn;
    @FXML
    private  TableColumn<DisasterModel, String> notesColumn;
    @FXML
    private TableColumn<DisasterModel, Void> actionsColumn;

    @FXML
    private void initialize() {
        disastersTbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTableColumns();
        actionButtons();
        setSearchFld();
        loadTable();
        EventHandler<ActionEvent> handlers = this::handleActions;
        refreshBtn.setOnAction(handlers);
        searchBtn.setOnAction(handlers);
        addBtn.setOnAction(handlers);
    }

    private void setupTableColumns() {
        disaster_id.setCellValueFactory(
                new PropertyValueFactory<>("disasterId")
        );

        type.setCellValueFactory(
                new PropertyValueFactory<>("disasterType")
        );

        disasterColumn.setCellValueFactory(
                new PropertyValueFactory<>("disasterName")
        );

        dateColumn.setCellValueFactory(
                new PropertyValueFactory<>("date")
        );

        notesColumn.setCellValueFactory(
                new PropertyValueFactory<>("notes")
        );

        regDateColumn.setCellValueFactory(
                new PropertyValueFactory<>("regDate")
        );
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == refreshBtn) {
            loadTable();
        } else if (src == searchBtn) {
            handleSearch();
        } else if(src == addBtn) {
            handleAddBeneficiary();
        }
    }

    public void loadTable() {
        List<DisasterModel> disaster = disasterService.getAllDisaster();

        disasterList = FXCollections.observableArrayList(disaster);
        disastersTbl.setItems(disasterList);
    }

    private void deleteById(DisasterModel dm) {
        if (dm == null || dm.getDisasterId() <= 0) {
            alertDialog.showErrorAlert("Invalid Selection", "Disaster ID is missing or invalid.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Are you sure you want to delete this Disaster?");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {

            boolean success = disasterService.deleteDisaster(dm);

            disasterList.remove(dm);
        }
    }

    private void handleAddBeneficiary() {
        try {
            AddDisasterDialogController disasterDialogController = DialogManager.getController("addDisaster", AddDisasterDialogController.class);
            disasterDialogController.setDisasterService(this.disasterService);
            disasterDialogController.setDisasterController(this);
            DialogManager.show("addDisaster");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showEditDisasterDialog(DisasterModel disasterModel) {
        try {
            EditDisasterDialogController editDisasterDialogController = DialogManager.getController("editDisaster", EditDisasterDialogController.class);
            editDisasterDialogController.setDisasterService(disasterService);
            editDisasterDialogController.setDisasterController(this);
            DisasterModel fullDisaster = disasterService.getDisasterById(disasterModel.getDisasterId());
            if (fullDisaster != null) {
                editDisasterDialogController.setDisaster(fullDisaster);
            } else {
                alertDialog.showErrorAlert("Error", "Unable to load disaster data.");
                return;
            }
            DialogManager.show("editDisaster");
        } catch (Exception e) {
            e.printStackTrace();
            alertDialog.showErrorAlert("Error", "Unable to load the Edit Disaster dialog.");
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
        List<DisasterModel> filteredDisaster = disasterService.searchDisaster(searchText);
        disasterList = FXCollections.observableArrayList(filteredDisaster);
        disastersTbl.setItems(disasterList);
    }

    private void setSearchFld() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.trim().isEmpty()) {
                loadTable();
            }
        });
    }




    private void actionButtons() {
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
                                    DisasterModel dm = getTableView().getItems().get(getIndex());

                                    showEditDisasterDialog(dm);
                                });

                                deleteButton.setOnAction(event -> {
                                    DisasterModel dm = getTableView().getItems().get(getIndex());
                                    deleteById(dm);
                                    loadTable();
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
