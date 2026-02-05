package com.ionres.respondph.evacuation_plan;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.evacuation_plan.dialog_controller.AllocateEvacSiteDialogController;
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
import javafx.scene.layout.HBox;
import javafx.util.Callback;
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
    @FXML private TableColumn<EvacuationPlanModel, String> noteColumn;
    @FXML private TableColumn<EvacuationPlanModel, Void> actionColumn;



    private final EvacuationPlanService service = new EvacuationPlanServiceImpl(DBConnection.getInstance());
    private ObservableList<EvacuationPlanModel> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        setupActionButtons();
        setupHandlers();
        loadTable();
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("planId"));
        beneficiaryColumn.setCellValueFactory(new PropertyValueFactory<>("beneficiaryName"));
        evacSiteColumn.setCellValueFactory(new PropertyValueFactory<>("evacSiteName"));
        disasterColumn.setCellValueFactory(new PropertyValueFactory<>("disasterName"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreated"));
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));

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

    public void loadTable() {
        Task<List<EvacuationPlanModel>> task = new Task<>() {
            @Override
            protected List<EvacuationPlanModel> call() throws Exception {
                return service.getAllEvacuationPlans();
            }

            @Override
            protected void succeeded() {
                data.clear();
                data.addAll(getValue());
                System.out.println("Loaded " + data.size() + " evacuation plans");
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                exception.printStackTrace();
                AlertDialogManager.showError("Error",
                        "Failed to load evacuation plans: " + exception.getMessage());
            }
        };

        new Thread(task).start();
    }

    private void deleteEvacPlan(EvacuationPlanModel plan) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Evacuation Plan");
        confirmAlert.setContentText("Are you sure you want to delete the evacuation plan for " +
                plan.getBeneficiaryName() + "?\n\n" +
                "Evacuation Site: " + plan.getEvacSiteName() + "\n" +
                "Disaster: " + plan.getDisasterName());

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        return service.deleteEvacuationPlan(plan.getPlanId());
                    }

                    @Override
                    protected void succeeded() {
                        if (getValue()) {
                            AlertDialogManager.showConfirmation("Success",
                                    "Evacuation plan deleted successfully.\nAvailable capacity updated automatically.");
                            loadTable();
                        } else {
                            AlertDialogManager.showError("Error", "Failed to delete evacuation plan.");
                        }
                    }

                    @Override
                    protected void failed() {
                        Throwable exception = getException();
                        exception.printStackTrace();
                        AlertDialogManager.showError("Error",
                                "Failed to delete evacuation plan: " + exception.getMessage());
                    }
                };

                new Thread(task).start();
            }
        });
    }


    private void setupHandlers() {
        searchBtn.setOnAction(e -> handleSearch());
        allocateBtn.setOnAction(e -> handleAllocate());
        refreshBtn.setOnAction(e-> loadTable());
    }

    private void handleSearch() {
        String searchText = searchField.getText();

        if (searchText == null || searchText.trim().isEmpty()) {
            loadTable();
            return;
        }

        Task<List<EvacuationPlanModel>> task = new Task<>() {
            @Override
            protected List<EvacuationPlanModel> call() throws Exception {
                return service.searchEvacuationPlans(searchText);
            }

            @Override
            protected void succeeded() {
                data.clear();
                data.addAll(getValue());
                System.out.println("Search found " + data.size() + " results for: " + searchText);
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                exception.printStackTrace();
                AlertDialogManager.showError("Error",
                        "Search failed: " + exception.getMessage());
            }
        };

        new Thread(task).start();
    }

    private void handleAllocate() {
        AllocateEvacSiteDialogController controller = DialogManager.getController("allocate", AllocateEvacSiteDialogController.class);
        controller.setEvacPlanController(this);
        controller.setDisasterService(AppContext.disasterService);
        controller.setEvacSiteService(AppContext.evacSiteService);
        DialogManager.show("allocate");
    }

    private void setupActionButtons() {
        Callback<TableColumn<EvacuationPlanModel, Void>, TableCell<EvacuationPlanModel, Void>> cellFactory =
                new Callback<>() {
                    @Override
                    public TableCell<EvacuationPlanModel, Void> call(TableColumn<EvacuationPlanModel, Void> param) {
                        return new TableCell<>() {
                            private final FontAwesomeIconView deleteIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
                            private final Button deleteButton = new Button("", deleteIcon);

                            {
                                deleteIcon.getStyleClass().add("delete-icon");
                                deleteButton.getStyleClass().add("delete-button");

                                deleteButton.setOnAction(event -> {
                                    EvacuationPlanModel evacSite = getTableView().getItems().get(getIndex());
                                    deleteEvacPlan(evacSite);

                                });
                            }

                            @Override
                            public void updateItem(Void item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty) {
                                    setGraphic(null);
                                } else {
                                    HBox box = new HBox(10, deleteButton);
                                    box.setAlignment(Pos.CENTER);
                                    box.getStyleClass().add("action-buttons-container");
                                    setGraphic(box);
                                    setAlignment(Pos.CENTER);
                                }
                            }
                        };
                    }
                };
        actionColumn.setCellFactory(cellFactory);
    }
}