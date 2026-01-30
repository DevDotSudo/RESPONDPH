package com.ionres.respondph.evac_site;

import com.ionres.respondph.evac_site.dialogs_controller.AddEvacSiteController;
import com.ionres.respondph.util.DialogManager;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

public class EvacSiteController {
    @FXML private AnchorPane root;
    @FXML private TextField searchField;
    @FXML private Button searchBtn, addBtn, refreshBtn;
    @FXML private TableColumn<EvacSiteModel, String> idColumn, nameColumn, capacityColumn, notesColumn, actionsColumn;

    @FXML private void initialize() {
        EventHandler<ActionEvent> handler = this::handleActions;
        addBtn.setOnAction(handler);
        refreshBtn.setOnAction(handler);
        searchBtn.setOnAction(handler);
    }

    private void handleActions(ActionEvent event) {
        Object src =  event.getSource();

        if (src == searchBtn) {
            handleSearch();
        }
        else if (src == addBtn) {
            handleAddSite();
        }
        else if (src == refreshBtn) {
            handleRefresh();
        }
    }

    private void handleSearch() {

    }

    private void handleAddSite() {
        AddEvacSiteController controller = DialogManager.getController("evacSite", AddEvacSiteController.class);
        DialogManager.show("evacSite");
    }

    private void handleRefresh() {

    }
}
