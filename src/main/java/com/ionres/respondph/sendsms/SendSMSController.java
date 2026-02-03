package com.ionres.respondph.sendsms;

import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.AppContext;
import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.beneficiary.BeneficiaryServiceImpl;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class SendSMSController implements Initializable {

    @FXML private ComboBox<String> cbSelectBeneficiary;
    @FXML private ComboBox<String> cbSelectPorts;
    @FXML private TextArea txtMessage;
    @FXML private Label charCount;
    @FXML private Button btnSendSMS;
    @FXML private TableView<SmsModel> adminTable;
    @FXML private TableColumn<SmsModel, String> dateSentColumn;
    @FXML private TableColumn<SmsModel, String> fullNameColumn;
    @FXML private TableColumn<SmsModel, String> msgColumn;
    @FXML private TableColumn<SmsModel, String> statusColumn;
    @FXML private TableColumn<SmsModel, String> phoneColumn;
    @FXML private TableColumn<SmsModel, Void> actionsColumn;
    @FXML private RadioButton rbGsm;
    @FXML private RadioButton rbApi;

    private final ObservableList<SmsModel> logRows = FXCollections.observableArrayList();
    private SmsService smsService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        smsService = new SmsServiceImpl();

        setupRadioButtons();
        setupControls();
        loadSMSLogs();

        if (btnSendSMS != null) btnSendSMS.setDisable(true);
        if (charCount != null) charCount.setText("0/160 characters");
    }

    private void setupRadioButtons() {
        ToggleGroup group = new ToggleGroup();
        rbGsm.setToggleGroup(group);
        rbApi.setToggleGroup(group);

        // Default to GSM
        rbGsm.setSelected(true);

        rbApi.selectedProperty().addListener((o, oldV, api) -> {
            if (cbSelectPorts != null) {
                cbSelectPorts.setDisable(api);
            }
            updateSendButtonState();
        });
    }

    private void loadSMSLogs() {
        try {
            List<SmsModel> saved = smsService.getAllSMSLogs();
            if (saved != null) {
                logRows.clear();
                logRows.addAll(saved);
            }
            if (adminTable != null) {
                adminTable.setItems(logRows);
            }
        } catch (Exception e) {
            System.err.println("Failed to load SMS logs: " + e.getMessage());
        }
    }

    private void setupControls() {
        setupTableColumns();
        setupEventHandlers();
        populateComboBoxes();
    }

    private void setupTableColumns() {
        if (dateSentColumn != null) {
            dateSentColumn.setCellValueFactory(cell -> {
                if (cell.getValue() == null || cell.getValue().getDateSent() == null)
                    return new SimpleStringProperty("");
                return new SimpleStringProperty(cell.getValue().getDateSent().toString());
            });
        }

        if (fullNameColumn != null) {
            fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullname"));
        }

        if (msgColumn != null) {
            msgColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        }

        if (statusColumn != null) {
            statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        }

        if (phoneColumn != null) {
            phoneColumn.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue() == null ? "" : cell.getValue().getPhoneString())
            );
        }

        if (actionsColumn != null) {
            actionsColumn.setCellFactory(col -> new TableCell<>() {
                private final Button resendBtn = new Button("Resend");

                {
                    resendBtn.setOnAction(evt -> handleResend());
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        return;
                    }

                    SmsModel row = getTableRow().getItem();
                    String status = row.getStatus();
                    resendBtn.setDisable("SENT".equalsIgnoreCase(status));
                    setGraphic(resendBtn);
                }

                private void handleResend() {
                    SmsModel row = getTableRow().getItem();
                    if (row == null) return;

                    resendBtn.setDisable(true);
                    String method = rbApi.isSelected() ? "API" : "GSM";

                    javafx.concurrent.Task<Boolean> task = new javafx.concurrent.Task<>() {
                        @Override
                        protected Boolean call() {
                            return smsService.resendSMS(row, method);
                        }
                    };

                    task.setOnSucceeded(e -> Platform.runLater(() -> {
                        boolean success = task.getValue();
                        row.setStatus(success ? "SENT" : "FAILED");
                        adminTable.refresh();
                        resendBtn.setDisable(success);
                    }));

                    task.setOnFailed(e -> Platform.runLater(() -> {
                        row.setStatus("FAILED");
                        adminTable.refresh();
                        resendBtn.setDisable(false);
                    }));

                    new Thread(task, "Resend-SMS-Task").start();
                }
            });
        }
    }

    private void setupEventHandlers() {
        if (btnSendSMS != null) {
            btnSendSMS.setOnAction(e -> onSendSMS());
        }

        if (txtMessage != null) {
            txtMessage.textProperty().addListener((obs, oldText, newText) -> {
                int len = newText == null ? 0 : newText.length();
                if (charCount != null) {
                    charCount.setText(len + "/160 characters");
                }
            });
        }
    }

    private void populateComboBoxes() {
        if (cbSelectBeneficiary != null) {
            cbSelectBeneficiary.setItems(FXCollections.observableArrayList(
                    "All Beneficiaries",
                    "By Barangay",
                    "Selected Beneficiaries",
                    "By Disaster Area",
                    "Custom List"
            ));
            cbSelectBeneficiary.getSelectionModel().selectFirst();
            cbSelectBeneficiary.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldVal, newVal) -> updateSendButtonState());
        }

        if (cbSelectPorts != null) {
            // Populate ports if using GSM
            cbSelectPorts.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldVal, newVal) -> updateSendButtonState());
        }
    }

    private void updateSendButtonState() {
        boolean canSend = false;

        if (rbApi != null && rbApi.isSelected()) {
            // For API, no port needed
            canSend = true;
        } else if (rbGsm != null && rbGsm.isSelected()) {
            // For GSM, port is required
            canSend = cbSelectPorts != null &&
                    cbSelectPorts.getSelectionModel().getSelectedItem() != null;
        }

        // Check if there are beneficiaries
        String sel = cbSelectBeneficiary == null ? null :
                cbSelectBeneficiary.getSelectionModel().getSelectedItem();
        if ("All Beneficiaries".equals(sel)) {
            try {
                if (AppContext.beneficiaryService == null && AppContext.db != null) {
                    AppContext.beneficiaryService = new BeneficiaryServiceImpl(AppContext.db);
                }
                List<BeneficiaryModel> all = AppContext.beneficiaryService == null ?
                        List.of() : AppContext.beneficiaryService.getAllBeneficiary();
                canSend = canSend && all != null && !all.isEmpty();
            } catch (Exception e) {
                canSend = false;
            }
        }

        if (btnSendSMS != null) {
            btnSendSMS.setDisable(!canSend);
        }
    }

    @FXML
    private void onSendSMS() {
        String message = txtMessage == null ? null : txtMessage.getText();
        if (message == null || message.trim().isEmpty()) {
            AlertDialogManager.showWarning("Empty Message", "Please enter a message to send.");
            return;
        }

        String recipientGroup = cbSelectBeneficiary == null ? null :
                cbSelectBeneficiary.getSelectionModel().getSelectedItem();
        String method = rbApi.isSelected() ? "API" : "GSM";

        if ("All Beneficiaries".equals(recipientGroup)) {
            sendToAllBeneficiaries(message, method);
        } else {
            AlertDialogManager.showError("Not Implemented", "This recipient group is not yet implemented.");
        }
    }

    private void sendToAllBeneficiaries(String message, String method) {
        if (AppContext.beneficiaryService == null) {
            if (AppContext.db != null) {
                AppContext.beneficiaryService = new BeneficiaryServiceImpl(AppContext.db);
            }
        }

        List<BeneficiaryModel> beneficiaries = AppContext.beneficiaryService == null ?
                List.of() : AppContext.beneficiaryService.getAllBeneficiary();

        int recipientCount = beneficiaries == null ? 0 : beneficiaries.size();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Bulk Send");
        confirm.setHeaderText("Send SMS to All Beneficiaries");
        confirm.setContentText(String.format(
                "You are about to send this message to %d recipients via %s. Continue?",
                recipientCount, method
        ));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        javafx.concurrent.Task<Integer> task = new javafx.concurrent.Task<>() {
            @Override
            protected Integer call() {
                updateMessage("Sending to all beneficiaries via " + method + "...");
                return smsService.sendBulkSMS(beneficiaries, message, method);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            int successCount = task.getValue();
            AlertDialogManager.showSuccess("Send Complete",
                    String.format("Successfully sent %d out of %d messages.",
                            successCount, recipientCount));
            loadSMSLogs();
        }));

        task.setOnFailed(e -> Platform.runLater(() ->
                AlertDialogManager.showError("Send Failed", "An error occurred while sending messages.")
        ));

        Thread t = new Thread(task, "SMSSender-UI-Task");
        t.setDaemon(true);
        t.start();
    }
}