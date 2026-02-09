package com.ionres.respondph.sendsms.dialogs_controller;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class BeneficiarySelectionDialogController {

    @FXML private TableView<BeneficiarySelectionItem> tblBeneficiaries;
    @FXML private TableColumn<BeneficiarySelectionItem, Boolean> colCheckbox;
    @FXML private TableColumn<BeneficiarySelectionItem, String> colName;
    @FXML private TableColumn<BeneficiarySelectionItem, String> colBarangay;
    @FXML private TableColumn<BeneficiarySelectionItem, String> colPhone;
    @FXML private TableColumn<BeneficiarySelectionItem, String> colAddress;
    @FXML private TextField txtSearch;
    @FXML private Label lblSelectedCount;
    @FXML private Label lblTotalCount;
    @FXML private Button btnOk;
    @FXML private Button btnCancel;

    private ObservableList<BeneficiarySelectionItem> allItems;
    private FilteredList<BeneficiarySelectionItem> filteredItems;

    private List<BeneficiaryModel> selectedBeneficiaries;
    private boolean okClicked = false;
    private boolean isClosing = false;

    @FXML
    private void initialize() {
        allItems = FXCollections.observableArrayList();
        filteredItems = new FilteredList<>(allItems, p -> true);

        // Setup table columns
        colCheckbox.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        colCheckbox.setCellFactory(CheckBoxTableCell.forTableColumn(colCheckbox));
        colCheckbox.setEditable(true);

        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colBarangay.setCellValueFactory(new PropertyValueFactory<>("barangay"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("mobileNumber"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("coordinates"));

        tblBeneficiaries.setEditable(true);
        tblBeneficiaries.setItems(filteredItems);

        // Setup search
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterBeneficiaries(newValue));

        updateCountLabels();

        // Cancel button triggers by ESC
        if (btnCancel != null) {
            btnCancel.setCancelButton(true);
        }
    }

    /** ✅ Used by SendSMSController to avoid wiping selections on reopen */
    public boolean isLoaded() {
        return allItems != null && !allItems.isEmpty();
    }

    public void setBeneficiaries(List<BeneficiaryModel> beneficiaries) {
        allItems.clear();

        for (BeneficiaryModel beneficiary : beneficiaries) {
            BeneficiarySelectionItem item = new BeneficiarySelectionItem(beneficiary);

            // Listen for selection changes
            item.selectedProperty().addListener((obs, wasSelected, isSelected) -> updateCountLabels());

            allItems.add(item);
        }

        updateCountLabels();
        System.out.println("DEBUG: Loaded " + allItems.size() + " beneficiaries for selection");
    }

    public void setPreselectedBeneficiaries(List<BeneficiaryModel> preselected) {
        if (preselected == null || preselected.isEmpty()) return;

        for (BeneficiarySelectionItem item : allItems) {
            for (BeneficiaryModel beneficiary : preselected) {
                if (item.getBeneficiary().getId() == beneficiary.getId()) {
                    item.setSelected(true);
                    break;
                }
            }
        }

        updateCountLabels();
    }

    @FXML
    private void onSelectAll() {
        for (BeneficiarySelectionItem item : filteredItems) item.setSelected(true);
        updateCountLabels();
    }

    @FXML
    private void onDeselectAll() {
        for (BeneficiarySelectionItem item : filteredItems) item.setSelected(false);
        updateCountLabels();
    }

    @FXML
    private void onClearSearch() {
        txtSearch.clear();
    }

    @FXML
    private void onOk() {
        if (isClosing) {
            System.out.println("DEBUG: Already closing, ignoring duplicate OK click");
            return;
        }

        isClosing = true;

        btnOk.setDisable(true);
        btnCancel.setDisable(true);

        selectedBeneficiaries = new ArrayList<>();
        for (BeneficiarySelectionItem item : allItems) {
            if (item.isSelected()) {
                selectedBeneficiaries.add(item.getBeneficiary());
            }
        }

        okClicked = true;
        closeDialog();
    }

    @FXML
    private void onCancel() {
        if (isClosing) {
            System.out.println("DEBUG: Already closing, ignoring duplicate Cancel click");
            return;
        }

        isClosing = true;

        btnOk.setDisable(true);
        btnCancel.setDisable(true);

        okClicked = false;
        closeDialog();
    }

    private void filterBeneficiaries(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredItems.setPredicate(p -> true);
        } else {
            String lowerCaseFilter = searchText.toLowerCase().trim();
            filteredItems.setPredicate(item -> {
                String fullName = item.getFullName().toLowerCase();
                String barangay = item.getBarangay() != null ? item.getBarangay().toLowerCase() : "";
                String phone = item.getMobileNumber() != null ? item.getMobileNumber().toLowerCase() : "";
                return fullName.contains(lowerCaseFilter) || barangay.contains(lowerCaseFilter) || phone.contains(lowerCaseFilter);
            });
        }
        updateCountLabels();
    }

    /** ✅ Reset dialog behavior WITHOUT clearing checkboxes */
    public void resetState() {
        okClicked = false;
        isClosing = false;

        if (btnOk != null) btnOk.setDisable(false);
        if (btnCancel != null) btnCancel.setDisable(false);

        // clear only the returned result list (NOT the checkbox states)
        if (selectedBeneficiaries != null) selectedBeneficiaries.clear();
        selectedBeneficiaries = null;

        // reset search to show all items, keep checks
        if (txtSearch != null) txtSearch.clear();
        if (filteredItems != null) filteredItems.setPredicate(p -> true);

        if (tblBeneficiaries != null) {
            tblBeneficiaries.getSelectionModel().clearSelection();
        }

        updateCountLabels();
    }

    private void updateCountLabels() {
        int selectedCount = 0;
        for (BeneficiarySelectionItem item : allItems) {
            if (item.isSelected()) selectedCount++;
        }

        if (lblSelectedCount != null) lblSelectedCount.setText("Selected: " + selectedCount);
        if (lblTotalCount != null) lblTotalCount.setText("Total: " + allItems.size() + " | Showing: " + filteredItems.size());
    }

    public void setupCloseHandler(Stage stage) {
        stage.setOnCloseRequest(event -> {
            if (!isClosing) {
                event.consume();
                onCancel();
            }
        });
    }

    private void closeDialog() {
        try {
            Stage stage = (Stage) btnOk.getScene().getWindow();
            if (stage != null) stage.close();
        } catch (Exception e) {
            System.err.println("Error closing dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    public List<BeneficiaryModel> getSelectedBeneficiaries() {
        return selectedBeneficiaries != null ? selectedBeneficiaries : new ArrayList<>();
    }

    public static class BeneficiarySelectionItem {
        private final BeneficiaryModel beneficiary;
        private final BooleanProperty selected;
        private final String fullName;
        private final String barangay;
        private final String mobileNumber;
        private final String coordinates;

        public BeneficiarySelectionItem(BeneficiaryModel beneficiary) {
            this.beneficiary = beneficiary;
            this.selected = new SimpleBooleanProperty(false);

            String fn = beneficiary.getFirstname() != null ? beneficiary.getFirstname() : "";
            String mn = beneficiary.getMiddlename() != null ? beneficiary.getMiddlename() : "";
            String ln = beneficiary.getLastname() != null ? beneficiary.getLastname() : "";
            this.fullName = (fn + " " + (mn.isEmpty() ? "" : mn + " ") + ln).trim();

            this.barangay = beneficiary.getBarangay() != null ? beneficiary.getBarangay() : "N/A";
            this.mobileNumber = beneficiary.getMobileNumber() != null ? beneficiary.getMobileNumber() : "N/A";

            String lat = beneficiary.getLatitude() != null ? beneficiary.getLatitude() : "0";
            String lon = beneficiary.getLongitude() != null ? beneficiary.getLongitude() : "0";
            this.coordinates = lat + ", " + lon;
        }

        public BeneficiaryModel getBeneficiary() { return beneficiary; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean selected) { this.selected.set(selected); }
        public BooleanProperty selectedProperty() { return selected; }

        public String getFullName() { return fullName; }
        public String getBarangay() { return barangay; }
        public String getMobileNumber() { return mobileNumber; }
        public String getCoordinates() { return coordinates; }
    }
}
