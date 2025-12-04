package com.ionres.respondph.beneficiary;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.admin.dialogs_controller.AddAdminDialogController;
import com.ionres.respondph.beneficiary.dialogs_controller.AddBeneficiariesDialogController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class BeneficiaryController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private TableView<BeneficiaryModel> beneficiaryTable;

    @FXML
    private TextField searchField;

    @FXML
    private  Button searchBtn;

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
        loadTable();
    }

    @FXML
    private void handleSearch() {
        System.out.println(searchField.getText());
    }

    @FXML
    private void handleAddBeneficiary() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dialogs/AddBeneficiariesDialog.fxml"));
            Parent dialogRoot = loader.load();

            AddBeneficiariesDialogController dialogController = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UNDECORATED);
            dialogStage.setTitle("Add New Beneficiary");

            Scene scene = new Scene(dialogRoot);
            dialogStage.setScene(scene);

            dialogController.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            if (dialogController.isAdminAdded()) {
                loadTable();
            }

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Error", "Unable to load the Add Admin dialog.");
        }
    }

    @FXML
    private void handleRefresh() {
        System.out.println("Refresh Beneficiaries");
    }

    private void loadTable() {
        System.out.println("Loading Table");
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
