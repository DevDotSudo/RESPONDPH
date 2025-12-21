package com.ionres.respondph.disaster_damage;

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

public class DisasterDamageController {
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
    private TableColumn<BeneficiaryModel, Void> actionsColumn;

    @FXML
    private void initialize() {
        disastersDamageTbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTableColumns();
        EventHandler<ActionEvent> handlers = this::handleActions;

        refreshBtn.setOnAction(handlers);
        searchBtn.setOnAction(handlers);
        addBtn.setOnAction(handlers);
    }

    private void setupTableColumns() {
        damage_id.setCellValueFactory(new PropertyValueFactory<>("damage_id"));
        beneficiaryNameColumn.setCellValueFactory(new PropertyValueFactory<>("beneficiaryNameColumn"));
        disasterColumn.setCellValueFactory(new PropertyValueFactory<>("disasterColumn"));
        damageSeverityColumn.setCellValueFactory(new PropertyValueFactory<>("damageSeverityColumn"));
        assessmentDateColumn.setCellValueFactory(new PropertyValueFactory<>("assessmentDateColumn"));
        verifiedByColumn.setCellValueFactory(new PropertyValueFactory<>("verifiedByColumn"));
        registeredDateColumn.setCellValueFactory(new PropertyValueFactory<>("registeredDateColumn"));
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == refreshBtn) {
        } else if (src == searchBtn) {

        } else if(src == addBtn) {
            handleAddDisasterDamage();
        }
    }

    private void handleAddDisasterDamage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dialogs/AddDisasterDamageDialog.fxml"));
            Stage dialogStage = new Stage();

            Parent dialogRoot = loader.load();

            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UNDECORATED);
            dialogStage.setTitle("Add Disaster Damage");


            Scene scene = new Scene(dialogRoot);

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
