package com.ionres.respondph.disaster;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class DisasterController {

    @FXML
    private AnchorPane root;
    @FXML
    private TextField searchField;
    @FXML
    private Button searchBtn, addBtn, refreshBtn;
    @FXML
    private TableView disastersTbl;
    @FXML
    private TableColumn<BeneficiaryModel, String> disaster_id;
    @FXML
    private TableColumn<BeneficiaryModel, String> type;
    @FXML
    private TableColumn<BeneficiaryModel, String> disasterColumn;
    @FXML
    private TableColumn<BeneficiaryModel, String> dateColumn;
    @FXML
    private TableColumn<BeneficiaryModel, Void> actionsColumn;

    @FXML
    private void initialize() {
        disastersTbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTableColumns();

        EventHandler<ActionEvent> handlers = this::handleActions;

        refreshBtn.setOnAction(handlers);
        searchBtn.setOnAction(handlers);
        addBtn.setOnAction(handlers);
    }

    private void setupTableColumns() {
        disaster_id.setCellValueFactory(new PropertyValueFactory<>("damage_id"));
        type.setCellValueFactory(new PropertyValueFactory<>("type"));
        disasterColumn.setCellValueFactory(new PropertyValueFactory<>("disasterColumn"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("disasterColumn"));
        actionsColumn.setCellValueFactory(new PropertyValueFactory<>("dateColumn"));
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == refreshBtn) {
        } else if (src == searchBtn) {

        } else if(src == addBtn) {
            handleAddBeneficiary();
        }
    }

    private void handleAddBeneficiary() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dialogs/AddDisasterDialog.fxml"));
            Stage dialogStage = new Stage();

            Parent dialogRoot = loader.load();

            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UNDECORATED);
            dialogStage.setTitle("Add Disaster");


            Scene scene = new Scene(dialogRoot);

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
