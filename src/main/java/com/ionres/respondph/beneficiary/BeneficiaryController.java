package com.ionres.respondph.beneficiary;

import com.ionres.respondph.admin.AdminModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

public class BeneficiaryController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private TableView<BeneficiaryModel> beneficiaryTable;

    @FXML
    private TextField searchField;

    @FXML
    private Button addButton;

    @FXML
    private Button refreshButton;

    @FXML
    private TableColumn<BeneficiaryModel, String> idColumn;

    @FXML
    private TableColumn<BeneficiaryModel, String> nameColumn;

    @FXML
    private TableColumn<BeneficiaryModel, Integer> ageColumn;

    @FXML
    private TableColumn<BeneficiaryModel, String> contactColumn;

    @FXML
    private TableColumn<BeneficiaryModel, String> addressColumn;

    @FXML
    private TableColumn<BeneficiaryModel, String> statusColumn;

    @FXML
    private TableColumn<BeneficiaryModel, String> registeredDateColumn;

    @FXML
    private TableColumn<BeneficiaryModel, Void> actionsColumn;

    @FXML
    private void initialize() {
        beneficiaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    @FXML
    private void handleSearch() {
        System.out.println(searchField.getText());
    }

    @FXML
    private void handleAddBeneficiary() {
        System.out.println("Add Beneficiary");
    }

    @FXML
    private void handleRefresh() {
        System.out.println("Refresh Beneficiaries");
    }
}
