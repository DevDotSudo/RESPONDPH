package com.ionres.respondph.familymembers;

import com.ionres.respondph.util.AlertDialog;
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
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FamilyMembersController {
    @FXML private AnchorPane rootPane;
    @FXML private TableView<FamilyMembersModel> familyTable;
    @FXML private TableColumn<FamilyMembersModel, Integer> idColumn;
    @FXML private TableColumn<FamilyMembersModel, String> beneficiaryNameColumn;
    @FXML private TableColumn<FamilyMembersModel, String> firstnameColumn;
    @FXML private TableColumn<FamilyMembersModel, String> middlenameColumn;
    @FXML private TableColumn<FamilyMembersModel, String> lastnameColumn;
    @FXML private TableColumn<FamilyMembersModel, String> relationshipColumn;
    @FXML private TableColumn<FamilyMembersModel, LocalDate> birthdateColumn;
    @FXML private TableColumn<FamilyMembersModel, String> genderColumn;
    @FXML private TableColumn<FamilyMembersModel, String> maritalStatusColumn;
    @FXML private TableColumn<FamilyMembersModel, LocalDate> registeredDateColumn;
    @FXML private TableColumn<FamilyMembersModel, String> actionsColumn;
    @FXML private TextField searchField;
    @FXML private Button searchBtn;
    @FXML private Button addButton;
    @FXML private Button refreshButton;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private FamilyMemberService familyMembersService = new FamilyMemberService();
    AlertDialog alertDialog = new AlertDialog();

    public void initialize() {
        familyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        EventHandler<ActionEvent> handler = this::handleActions;

        addButton.setOnAction(handler);
    }

    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == searchBtn) {

        }else if (src == addButton) {
            showAddMembersDialog();
        }

        else if (src == refreshButton) {

        }
    }

    private void showAddMembersDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dialogs/AddFamilyMemberDialog.fxml"));
            Parent dialogRoot = loader.load();

            Stage dialogStage = new Stage();

            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UNDECORATED);
            dialogStage.setTitle("Add Family Members");

            Scene scene = new Scene(dialogRoot);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            alertDialog.showErrorAlert("Error", "Unable to load the Edit Beneficiary dialog.");
        }
    }
}
