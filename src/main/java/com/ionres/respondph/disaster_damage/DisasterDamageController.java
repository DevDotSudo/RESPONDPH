package com.ionres.respondph.disaster_damage;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.disaster_damage.dialogs_controller.AddDisasterDamageDialogController;
import com.ionres.respondph.disaster_damage.dialogs_controller.EditDisasterDamageDialogController;
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
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
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

import javax.print.attribute.standard.DialogOwner;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class DisasterDamageController {
    AlertDialog alertDialog = new AlertDialog();
    DisasterDamageService disasterDamageService = AppContext.disasterDamageService;
    ObservableList<DisasterDamageModel> disasterList;

    @FXML
    private AnchorPane root;

    @FXML
    private TextField searchField;

    @FXML
    private Button searchBtn, addBtn, refreshBtn;

    @FXML
    private TableView disastersDamageTbl;

    @FXML
    private TableColumn<DisasterDamageModel, String> damage_id;

    @FXML
    private TableColumn<DisasterDamageModel, String> beneficiaryNameColumn;

    @FXML
    private TableColumn<DisasterDamageModel, String> disasterColumn;

    @FXML
    private TableColumn<DisasterDamageModel, String> damageSeverityColumn;

    @FXML
    private TableColumn<DisasterDamageModel, String> assessmentDateColumn;

    @FXML
    private TableColumn<DisasterDamageModel, String> verifiedByColumn;

    @FXML
    private TableColumn<DisasterDamageModel, String> registeredDateColumn;

    @FXML
    private TableColumn<DisasterDamageModel, Void> actionsColumn;

    @FXML
    private void initialize() {
        disastersDamageTbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTableColumns();
        loadTable();
        actionButtons();


        EventHandler<ActionEvent> handlers = this::handleActions;
        refreshBtn.setOnAction(handlers);
        searchBtn.setOnAction(handlers);
        addBtn.setOnAction(handlers);
    }

    private void setupTableColumns() {

        damage_id.setCellValueFactory(
                new PropertyValueFactory<>("beneficiaryDisasterDamageId")
        );

        beneficiaryNameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getBeneficiaryId()
                                + " - "
                                + cellData.getValue().getBeneficiaryFirstname()
                )
        );
        disasterColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getDisasterId()
                                + " - "
                                + cellData.getValue().getDisasterType()
                )
        );
        damageSeverityColumn.setCellValueFactory(
                new PropertyValueFactory<>("houseDamageSeverity")
        );
        assessmentDateColumn.setCellValueFactory(
                new PropertyValueFactory<>("assessmentDate")
        );

        verifiedByColumn.setCellValueFactory(
                new PropertyValueFactory<>("verifiedBy")
        );

        registeredDateColumn.setCellValueFactory(
                new PropertyValueFactory<>("regDate")
        );
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == refreshBtn) {
        } else if (src == searchBtn) {
            handleSearch();
            setSearchFld();

        } else if(src == addBtn) {
            handleAddDisasterDamage();
        }
    }

    public void loadTable() {
        List<DisasterDamageModel> ddm = disasterDamageService.getAllDisasterDamage();

        disasterList = FXCollections.observableArrayList(ddm);
        disastersDamageTbl.setItems(disasterList);
    }


    private void deleteById(DisasterDamageModel ddm) {
        if (ddm == null || ddm.getDisasterId() <= 0) {
            alertDialog.showErrorAlert("Invalid Selection", "Disaster Damage ID is missing or invalid.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Are you sure you want to delete this Disaster Damage?");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {

            boolean success = disasterDamageService.deleteDisasterDamage(ddm);

            disasterList.remove(ddm);
        }
    }

    private void handleAddDisasterDamage() {
        try {
            AddDisasterDamageDialogController addDisasterDamageDialogController = DialogManager.getController("addDisasterDamage",  AddDisasterDamageDialogController.class);
            addDisasterDamageDialogController.setDisasterDamageService(disasterDamageService);
            addDisasterDamageDialogController.setDisasterDamageController(this);
            DialogManager.show("addDisasterDamage");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showEditDisasterDialog(DisasterDamageModel disasterModel) {
        try {
            EditDisasterDamageDialogController editDisasterDamageDialogController = DialogManager.getController("editDisasterDamage",  EditDisasterDamageDialogController.class);
            editDisasterDamageDialogController.setDisasterDamageService(this.disasterDamageService);
            editDisasterDamageDialogController.setDisasterDamageController(this);
            DisasterDamageModel fullDisasterDamage = disasterDamageService.getDisasterDamageId(
                    disasterModel.getBeneficiaryDisasterDamageId()
            );

            if (fullDisasterDamage != null) {

                editDisasterDamageDialogController.setDisasterDamage(fullDisasterDamage);
            } else {
                alertDialog.showErrorAlert("Error", "Unable to load disaster damage data.");
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
            alertDialog.showErrorAlert("Error", "Unable to load the Edit Disaster damage dialog: " + e.getMessage());
        }
    }

    private void handleSearch() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            loadTable();
        } else {
            searchDisasterDamage(searchText);
        }
    }

    private void searchDisasterDamage(String searchText) {
        try {
            List<DisasterDamageModel> filteredDisasterDamage =
                    disasterDamageService.searchDisasterDamage(searchText);

            disasterList = FXCollections.observableArrayList(filteredDisasterDamage);
            disastersDamageTbl.setItems(disasterList);

            if (filteredDisasterDamage.isEmpty()) {
                System.out.println("No results found for: \"" + searchText + "\"");
            } else {
                System.out.println("Found " + filteredDisasterDamage.size() + " result(s) for: \"" + searchText + "\"");
            }
        } catch (Exception e) {
            e.printStackTrace();
            alertDialog.showErrorAlert("Search Error", "Failed to search: " + e.getMessage());
        }
    }

    private void setSearchFld() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String searchText = newValue.trim();
            if (searchText.isEmpty()) {
                loadTable();
            }
        });

    }

    private void actionButtons() {
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
                                    DisasterDamageModel ddm = getTableView().getItems().get(getIndex());
                                    showEditDisasterDialog(ddm);
                                });

                                deleteButton.setOnAction(event -> {
                                    DisasterDamageModel ddm = getTableView().getItems().get(getIndex());
                                    deleteById(ddm);
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
