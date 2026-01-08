package com.ionres.respondph.aid_type;

import com.ionres.respondph.aid_type.dialogs_controller.AddAidTypeController;
import com.ionres.respondph.beneficiary.dialogs_controller.AddBeneficiariesDialogController;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.DialogManager;
import com.sun.javafx.scene.control.MultipleAdditionAndRemovedChange;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

public class AidTypeController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private TextField searchFld;

    @FXML
    private Button searchBtn;

    @FXML
    private Button addButton;

    @FXML
    private Button refreshButton;

    @FXML
    private TableView<AidTypeModel> adminTable;

    @FXML
    private TableColumn<AidTypeModel, String> idColumn;

    @FXML
    private TableColumn<AidTypeModel, String> nameColumn;

    @FXML
    private TableColumn<AidTypeModel, String> adminNameColumn;

    @FXML
    private TableColumn<AidTypeModel, String> notesColumn;

    @FXML
    private void initialize() {
        EventHandler<ActionEvent> handlers = this::handleActions;

        refreshButton.setOnAction(handlers);
        searchBtn.setOnAction(handlers);
        addButton.setOnAction(handlers);
    }



    private void handleActions(ActionEvent event) {
        Object src = event.getSource();

        if (src == refreshButton) {
        } else if (src == searchBtn) {
        } else if (src == addButton) {
            handleAddBeneficiary();
        }
    }

    private void handleAddBeneficiary() {
        try {
            AddAidTypeController controller = DialogManager.getController("addAidType", AddAidTypeController.class);
            DialogManager.show("addAidType");
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error",
                    "Unable to load the Add Beneficiary dialog: " + e.getMessage());
        }
    }
}
