package com.ionres.respondph.evacuation_plan;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.evacuation_plan.dialog_controller.AllocateEvacSiteDialogController;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.AppContext;
import com.ionres.respondph.util.DialogManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class EvacuationPlanController {

    @FXML private TextField searchField;
    @FXML private Button searchBtn;
    @FXML private Button allocateBtn;
    @FXML private Button refreshBtn;

    @FXML private TableView<EvacuationPlanModel> planTable;
    @FXML private TableColumn<EvacuationPlanModel, Integer> idColumn;
    @FXML private TableColumn<EvacuationPlanModel, String> beneficiaryColumn;
    @FXML private TableColumn<EvacuationPlanModel, String> evacSiteColumn;
    @FXML private TableColumn<EvacuationPlanModel, String> disasterColumn;
    @FXML private TableColumn<EvacuationPlanModel, String> dateColumn;

    private final EvacuationPlanService service = new EvacuationPlanServiceImpl(DBConnection.getInstance());
    private ObservableList<EvacuationPlanModel> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        setupHandlers();
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("planId"));
        beneficiaryColumn.setCellValueFactory(new PropertyValueFactory<>("beneficiaryName"));
        evacSiteColumn.setCellValueFactory(new PropertyValueFactory<>("evacSiteName"));
        disasterColumn.setCellValueFactory(new PropertyValueFactory<>("disasterName"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreated"));

        disasterColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().remove("disaster-badge");
                } else {
                    setText(item);
                    if (!getStyleClass().contains("disaster-badge")) {
                        getStyleClass().add("disaster-badge");
                    }
                }
            }
        });

        planTable.setItems(data);
        planTable.setPlaceholder(new Label("No evacuation plans found"));
    }

    private void setupHandlers() {
        searchBtn.setOnAction(e -> handleSearch());
        allocateBtn.setOnAction(e -> handleAllocate());
    }


    private void handleSearch() {

    }

    private void handleAllocate() {
        AllocateEvacSiteDialogController controller = DialogManager.getController("allocate", AllocateEvacSiteDialogController.class);
        controller.setEvacPlanController(this);
        controller.setDisasterService(AppContext.disasterService);
        controller.setEvacSiteService(AppContext.evacSiteService);
        DialogManager.show("allocate");
    }
}
