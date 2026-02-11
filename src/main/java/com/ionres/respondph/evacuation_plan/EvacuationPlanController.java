package com.ionres.respondph.evacuation_plan;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.AlertDialogManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EvacuationPlanController {

    @FXML private TextField searchField;
    @FXML private Button searchBtn;
    @FXML private Button refreshBtn;
    @FXML private Button printBtn;  // NEW

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
        refreshBtn.setOnAction(e -> loadTable());
        printBtn.setOnAction(e -> handlePrint());  // NEW
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


    private void handlePrint() {
        if (data.isEmpty()) {
            AlertDialogManager.showWarning("No Data",
                    "There are no evacuation plans to print.");
            return;
        }

        List<String> disasters = data.stream()
                .map(EvacuationPlanModel::getDisasterName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        if (disasters.isEmpty()) {
            AlertDialogManager.showWarning("No Data",
                    "No disaster data found.");
            return;
        }

        showDisasterSelectionDialog(disasters);
    }


    private void showDisasterSelectionDialog(List<String> disasters) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select Disaster");
        dialog.setHeaderText("Choose which disaster to print:");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label instruction = new Label("Select a disaster event:");
        instruction.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        ComboBox<String> disasterComboBox = new ComboBox<>();
        disasterComboBox.setItems(FXCollections.observableArrayList(disasters));
        disasterComboBox.setPromptText("Select disaster...");
        disasterComboBox.setPrefWidth(300);

        if (!disasters.isEmpty()) {
            disasterComboBox.getSelectionModel().selectFirst();
        }

        Label previewLabel = new Label("");
        previewLabel.setFont(Font.font("Arial", 11));
        previewLabel.setStyle("-fx-text-fill: #7f8c8d;");

        disasterComboBox.setOnAction(e -> {
            String selectedDisaster = disasterComboBox.getValue();
            if (selectedDisaster != null) {
                long count = data.stream()
                        .filter(plan -> plan.getDisasterName().equals(selectedDisaster))
                        .count();

                Map<String, Long> evacSiteCounts = data.stream()
                        .filter(plan -> plan.getDisasterName().equals(selectedDisaster))
                        .collect(Collectors.groupingBy(
                                EvacuationPlanModel::getEvacSiteName,
                                Collectors.counting()
                        ));

                previewLabel.setText(String.format(
                        "Total Beneficiaries: %d | Evacuation Sites: %d",
                        count,
                        evacSiteCounts.size()
                ));
            }
        });

        if (!disasters.isEmpty()) {
            String firstDisaster = disasters.get(0);
            long count = data.stream()
                    .filter(plan -> plan.getDisasterName().equals(firstDisaster))
                    .count();

            Map<String, Long> evacSiteCounts = data.stream()
                    .filter(plan -> plan.getDisasterName().equals(firstDisaster))
                    .collect(Collectors.groupingBy(
                            EvacuationPlanModel::getEvacSiteName,
                            Collectors.counting()
                    ));

            previewLabel.setText(String.format(
                    "Total Beneficiaries: %d | Evacuation Sites: %d",
                    count,
                    evacSiteCounts.size()
            ));
        }

        content.getChildren().addAll(instruction, disasterComboBox, previewLabel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(disasterComboBox.getValue() == null);

        disasterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            okButton.setDisable(newVal == null);
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return disasterComboBox.getValue();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(selectedDisaster -> {
            if (selectedDisaster != null) {
                printSelectedDisaster(selectedDisaster);
            }
        });
    }


    private void printSelectedDisaster(String disasterName) {
        // Filter data for selected disaster
        List<EvacuationPlanModel> disasterPlans = data.stream()
                .filter(plan -> plan.getDisasterName().equals(disasterName))
                .collect(Collectors.toList());

        if (disasterPlans.isEmpty()) {
            AlertDialogManager.showWarning("No Data",
                    "No evacuation plans found for " + disasterName);
            return;
        }

        Map<String, List<EvacuationPlanModel>> groupedByEvacSite = disasterPlans.stream()
                .collect(Collectors.groupingBy(EvacuationPlanModel::getEvacSiteName));

        printEvacuationList(disasterName, groupedByEvacSite);
    }


    private void printEvacuationList(String disasterName, Map<String, List<EvacuationPlanModel>> groupedByEvacSite) {
        try {
            PrinterJob printerJob = PrinterJob.createPrinterJob();

            if (printerJob == null) {
                AlertDialogManager.showError("Print Error", "No printer available.");
                return;
            }

            boolean proceed = printerJob.showPrintDialog(null);

            if (!proceed) {
                return;
            }

            VBox printContent = createPrintContent(disasterName, groupedByEvacSite);

            boolean success = printerJob.printPage(printContent);

            if (success) {
                printerJob.endJob();
                AlertDialogManager.showInfo("Print Success",
                        "Document sent to printer successfully.");
            } else {
                AlertDialogManager.showError("Print Error",
                        "Failed to print document.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Print Error",
                    "An error occurred during printing:\n" + e.getMessage());
        }
    }


    private VBox createPrintContent(String disasterName, Map<String, List<EvacuationPlanModel>> groupedByEvacSite) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        Label title = new Label("Evacuation Site Assignment");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setAlignment(Pos.CENTER);

        Label disasterLabel = new Label("Disaster: " + disasterName);
        disasterLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        disasterLabel.setStyle("-fx-text-fill: #c0392b;");
        disasterLabel.setAlignment(Pos.CENTER);

        int totalBeneficiaries = groupedByEvacSite.values().stream()
                .mapToInt(List::size)
                .sum();

        Label subtitle = new Label("Total Beneficiaries: " + totalBeneficiaries +
                " | Evacuation Sites: " + groupedByEvacSite.size());
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        subtitle.setAlignment(Pos.CENTER);

        content.getChildren().addAll(title, disasterLabel, subtitle, new Label(""));

        groupedByEvacSite.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String evacSite = entry.getKey();
                    List<EvacuationPlanModel> beneficiaries = entry.getValue();

                    Label siteHeader = new Label("Evacuation Site: " + evacSite);
                    siteHeader.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                    siteHeader.setStyle("-fx-text-fill: #2c3e50; -fx-padding: 10 0 5 0;");

                    Label siteCount = new Label("Total Beneficiaries: " + beneficiaries.size());
                    siteCount.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
                    siteCount.setStyle("-fx-text-fill: #7f8c8d;");

                    content.getChildren().addAll(siteHeader, siteCount);

                    beneficiaries.sort((b1, b2) ->
                            b1.getBeneficiaryName().compareTo(b2.getBeneficiaryName()));

                    int count = 1;
                    for (EvacuationPlanModel plan : beneficiaries) {
                        Label beneficiaryLabel = new Label(String.format(
                                "%d. %s",
                                count++,
                                plan.getBeneficiaryName()
                        ));
                        beneficiaryLabel.setFont(Font.font("Arial", 11));
                        beneficiaryLabel.setStyle("-fx-padding: 2 0 2 15;");
                        content.getChildren().add(beneficiaryLabel);
                    }

                    // Spacing between sites
                    content.getChildren().add(new Label(""));
                });

        return content;
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