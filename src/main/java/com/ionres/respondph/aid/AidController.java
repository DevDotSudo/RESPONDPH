package com.ionres.respondph.aid;

import com.ionres.respondph.aid.dialogs_controller.AddAidController;
import com.ionres.respondph.aid.dialogs_controller.PrintAidDialogController;
import com.ionres.respondph.aid_type.AidTypeDAO;
import com.ionres.respondph.aid_type.AidTypeDAOImpl;
import com.ionres.respondph.aid_type.AidTypeModelComboBox;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.disaster.DisasterDAO;
import com.ionres.respondph.disaster.DisasterDAOImpl;
import com.ionres.respondph.disaster.DisasterModelComboBox;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.DialogManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class AidController {
    @FXML private AnchorPane aidRootPane;
    @FXML private TextField aidSearchField;
    @FXML private Button aidSearchButton, addAidButton, refreshAidButton, printButton;
    @FXML private TableColumn<AidModel, Integer> aidIdColumn;
    @FXML private TableColumn<AidModel, String> beneficiaryColumn;
    @FXML private TableColumn<AidModel, String> disasterColumn;
    @FXML private TableColumn<AidModel, String> nameColumn;
    @FXML private TableColumn<AidModel, LocalDate> dateColumn;
    @FXML private TableColumn<AidModel, Double> qtyColumn;
    @FXML private TableColumn<AidModel, Double> costColumn;
    @FXML private TableColumn<AidModel, String> providerColumn;
    @FXML private TableColumn<AidModel, String> notesColumn;
    @FXML private TableView<AidModel> aidTable;

    private AidTypeDAO aidTypeDAO;
    private DisasterDAO disasterDAO;
    private AidDAO aidDAO;
    private ObservableList<AidModel> aidData;

    @FXML
    private void initialize() {
        aidTypeDAO = new AidTypeDAOImpl(DBConnection.getInstance());
        disasterDAO = new DisasterDAOImpl(DBConnection.getInstance());
        aidDAO = new AidDAOImpl(DBConnection.getInstance());

        setupTableColumns();
        loadAidData();

        EventHandler<ActionEvent> handler = this::handleActions;
        addAidButton.setOnAction(handler);
        refreshAidButton.setOnAction(handler);
        aidSearchButton.setOnAction(handler);
        printButton.setOnAction(handler);
    }

    private void setupTableColumns() {
        aidIdColumn.setCellValueFactory(new PropertyValueFactory<>("aidId"));
        beneficiaryColumn.setCellValueFactory(new PropertyValueFactory<>("beneficiaryName"));
        disasterColumn.setCellValueFactory(new PropertyValueFactory<>("disasterName"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        qtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        costColumn.setCellValueFactory(new PropertyValueFactory<>("cost"));
        providerColumn.setCellValueFactory(new PropertyValueFactory<>("provider"));
        notesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));

        dateColumn.setCellFactory(column -> new TableCell<AidModel, LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(formatter.format(date));
                }
            }
        });

        qtyColumn.setCellFactory(column -> new TableCell<AidModel, Double>() {
            @Override
            protected void updateItem(Double qty, boolean empty) {
                super.updateItem(qty, empty);
                if (empty || qty == null) {
                    setText(null);
                } else {
                    setText(String.format("%.0f", qty));
                }
            }
        });

        costColumn.setCellFactory(column -> new TableCell<AidModel, Double>() {
            @Override
            protected void updateItem(Double cost, boolean empty) {
                super.updateItem(cost, empty);
                if (empty || cost == null) {
                    setText(null);
                } else {
                    setText(String.format("₱%.2f", cost));
                }
            }
        });

//        addActionButtons();
    }


    public void loadAidData() {
        try {
            List<AidModel> aidList = aidDAO.getAllAidForTable();
            aidData = FXCollections.observableArrayList(aidList);
            aidTable.setItems(aidData);

            System.out.println("Loaded " + aidList.size() + " aid records into table");
        } catch (Exception e) {
            System.err.println("Error loading aid data: " + e.getMessage());
            e.printStackTrace();
            AlertDialogManager.showError(
                    "Error Loading Data",
                    "Failed to load aid records: " + e.getMessage()
            );
        }
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == aidSearchButton) {
            handleSearch();
        }
        else if (src == addAidButton) {
            handleAddAid();
        }
        else if (src == refreshAidButton) {
            refreshAidButtonAction(event);
        }
        else if (src == printButton){
            handlePrint();
        }
    }

    private void handleSearch() {
        String searchText = aidSearchField.getText();

        if (searchText == null || searchText.trim().isEmpty()) {
            loadAidData();
            return;
        }

        Task<List<AidModel>> task = new Task<>() {
            @Override
            protected List<AidModel> call() throws Exception {
                return searchAidRecords(searchText);
            }

            @Override
            protected void succeeded() {
                aidData = FXCollections.observableArrayList(getValue());
                aidTable.setItems(aidData);
                System.out.println("Search found " + aidData.size() + " results for: " + searchText);
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

    private List<AidModel> searchAidRecords(String searchText) {
        List<AidModel> allAidList = aidDAO.getAllAidForTable();
        String searchTerm = searchText.trim().toLowerCase();

        return allAidList.stream()
                .filter(aid -> {
                    if (String.valueOf(aid.getAidId()).contains(searchTerm)) {
                        return true;
                    }
                    if (aid.getBeneficiaryName() != null &&
                            aid.getBeneficiaryName().toLowerCase().contains(searchTerm)) {
                        return true;
                    }

                    if (aid.getDisasterName() != null &&
                            aid.getDisasterName().toLowerCase().contains(searchTerm)) {
                        return true;
                    }

                    if (aid.getName() != null &&
                            aid.getName().toLowerCase().contains(searchTerm)) {
                        return true;
                    }
                    if (aid.getProvider() != null &&
                            aid.getProvider().toLowerCase().contains(searchTerm)) {
                        return true;
                    }

                    if (aid.getNotes() != null &&
                            aid.getNotes().toLowerCase().contains(searchTerm)) {
                        return true;
                    }

                    if (aid.getDate() != null) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
                        String formattedDate = formatter.format(aid.getDate());
                        if (formattedDate.toLowerCase().contains(searchTerm)) {
                            return true;
                        }
                    }

                    if (String.format("%.0f", aid.getQuantity()).contains(searchTerm)) {
                        return true;
                    }

                    if (String.format("%.2f", aid.getCost()).contains(searchTerm)) {
                        return true;
                    }

                    return false;
                })
                .collect(Collectors.toList());
    }

    private void handleAddAid() {
        try {
            List<AidTypeModelComboBox> aidTypes = aidTypeDAO.findAll();
            List<DisasterModelComboBox> disasters = disasterDAO.findAll();

            if (aidTypes.isEmpty()) {
                AlertDialogManager.showWarning(
                        "No Aid Types",
                        "Please create at least one Aid Type before distributing aid."
                );
                return;
            }

            if (disasters.isEmpty()) {
                AlertDialogManager.showWarning(
                        "No Disasters",
                        "Please register at least one Disaster before distributing aid."
                );
                return;
            }

            AddAidController addAidController = DialogManager.getController("addAid", AddAidController.class);

            addAidController.setAidTypes(aidTypes);
            addAidController.setDisasters(disasters);
            addAidController.setAidController(this);

            DialogManager.show("addAid");

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError(
                    "Error",
                    "An unexpected error occurred: " + e.getMessage()
            );
        }
    }

    public void refreshAidTable() {
        loadAidData();
        aidSearchField.clear();
    }

    private void refreshAidButtonAction(ActionEvent event) {
        refreshAidTable();
    }

    private void handlePrint() {
        try {
            PrintAidDialogController printAidDialogController = DialogManager.getController("printAidDialog", PrintAidDialogController.class);
            DialogManager.show("printAidDialog");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError(
                    "Error",
                    "Failed to open print dialog: " + e.getMessage()
            );
        }
    }
}