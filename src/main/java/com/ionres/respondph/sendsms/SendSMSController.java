package com.ionres.respondph.sendsms;

import com.ionres.respondph.util.SMSSender;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class SendSMSController implements Initializable {

    @FXML
    private ComboBox<String> cbSelectBeneficiary;

    @FXML
    private ComboBox<String> cbSelectPorts;

    @FXML
    private TextArea txtMessage;

    @FXML
    private Label charCount;

    @FXML
    private Button btnSendSMS;

    @FXML
    private TableView<smsModel> adminTable;

    @FXML
    private TableColumn<smsModel, String> dateSentColumn;

    @FXML
    private TableColumn<smsModel, String> fullNameColumn;

    @FXML
    private TableColumn<smsModel, String> msgColumn;

    @FXML
    private TableColumn<smsModel, String> statusColumn;

    @FXML
    private TableColumn<smsModel, String> phoneColumn;

    @FXML
    private TableColumn<smsModel, Void> actionsColumn;

    private final ObservableList<smsModel> logRows = FXCollections.observableArrayList();


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            populatePorts();
            setupControls();

            if (btnSendSMS != null) btnSendSMS.setDisable(true);
            if (charCount != null) charCount.setText("0/160 characters");

            try {
                List<smsModel> saved = new smsDAOImpl().getAllSMS();
                if (saved != null) {
                    logRows.clear();
                    logRows.addAll(saved);
                }
                if (adminTable != null) adminTable.setItems(logRows);
            } catch (Throwable t) {
                System.err.println("Failed to load SMS logs: " + t.getMessage());
            }

            SMSSender.getInstance().addSmsLogListener(log -> {
                Platform.runLater(() -> {
                    logRows.add(0, log);
                    if (adminTable != null) adminTable.refresh();
                });
            });

        } catch (Throwable t) {
            System.err.println("Exception during SendSMSController.initialize(): " + t.getMessage());
        }
    }

    private void populatePorts() {
        if (cbSelectPorts == null) {
            System.err.println("populatePorts: cbSelectPorts is null; skipping port population.");
            return;
        }
        try {
            List<String> portNames = SMSSender.getInstance().getAvailablePorts();
            ObservableList<String> items = FXCollections.observableArrayList(portNames);
            cbSelectPorts.setItems(items);
            if (!items.isEmpty()) cbSelectPorts.getSelectionModel().selectFirst();
            cbSelectPorts.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateSendButtonState());

        } catch (Throwable t) {
            System.err.println("Failed to enumerate serial ports: " + t.getMessage());
        }
    }

    private void setupControls() {
        try {
            if (dateSentColumn != null) {
                dateSentColumn.setCellValueFactory(cell -> {
                    if (cell.getValue() == null || cell.getValue().getDateSent() == null) return new SimpleStringProperty("");
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
                phoneColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue() == null ? "" : cell.getValue().getPhoneString()));
            }

            if (actionsColumn != null) {
                actionsColumn.setCellFactory(col -> new TableCell<smsModel, Void>() {
                    private final Button resendBtn = new Button("Resend");

                    {
                        resendBtn.setOnAction(evt -> {
                            smsModel row = getTableRow() == null ? null : getTableRow().getItem();
                            if (row == null) return;
                            resendBtn.setDisable(true);

                            javafx.concurrent.Task<Boolean> sendTask = new javafx.concurrent.Task<>() {
                                @Override
                                protected Boolean call() throws Exception {
                                    SMSSender sender = SMSSender.getInstance();
                                    String port = cbSelectPorts == null ? null : cbSelectPorts.getSelectionModel().getSelectedItem();
                                    return sender.resendSMS(row, port, 3000);
                                }
                            };

                            sendTask.setOnSucceeded(workerStateEvent -> {
                                boolean success = sendTask.getValue() != null && sendTask.getValue();
                                Platform.runLater(() -> {
                                    row.setStatus(success ? "SENT" : "FAILED");
                                    if (adminTable != null) adminTable.refresh();
                                    resendBtn.setDisable(success);
                                });
                             });

                             sendTask.setOnFailed(workerStateEvent -> {
                                 Platform.runLater(() -> {
                                     row.setStatus("FAILED");
                                     if (adminTable != null) adminTable.refresh();
                                     resendBtn.setDisable(false);
                                 });
                             });

                             Thread t = new Thread(sendTask, "Resend-SMS-Task");
                             t.setDaemon(true);
                             t.start();
                         });
                     }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                            return;
                        }
                        smsModel row = getTableView().getItems().get(getIndex());
                        if (row == null) {
                            setGraphic(null);
                            return;
                        }
                        String status = row.getStatus();
                        boolean disabled = status != null && status.equalsIgnoreCase("SENT");
                        resendBtn.setDisable(disabled);
                        setGraphic(resendBtn);
                    }
                });
            }
        } catch (Throwable t) {
            System.err.println("Failed to configure table columns: " + t.getMessage());
        }

        if (btnSendSMS != null) {
            btnSendSMS.setOnAction(e -> onSendSMS());
        }

        if (txtMessage != null) {
            txtMessage.textProperty().addListener((obs, oldText, newText) -> {
                int len = newText == null ? 0 : newText.length();
                String text = len + "/160 characters";
                if (charCount != null) charCount.setText(text);
            });
        }

        if (cbSelectBeneficiary != null && (cbSelectBeneficiary.getItems() == null || cbSelectBeneficiary.getItems().isEmpty())) {
            cbSelectBeneficiary.setItems(FXCollections.observableArrayList(
                    "All Beneficiaries",
                    "By Barangay",
                    "Selected Beneficiaries",
                    "By Disaster Area",
                    "Custom List"
            ));
            cbSelectBeneficiary.getSelectionModel().selectFirst();
        }

        if (cbSelectBeneficiary != null) {
            cbSelectBeneficiary.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateSendButtonState());
        }
    }

    private void updateSendButtonState() {
        boolean hasPort = cbSelectPorts != null && cbSelectPorts.getSelectionModel().getSelectedItem() != null;
        boolean hasRows = true;
        String sel = cbSelectBeneficiary == null ? null : cbSelectBeneficiary.getSelectionModel().getSelectedItem();
        if ("All Beneficiaries".equals(sel)) {
            try {
                if (AppContext.beneficiaryService == null && AppContext.db != null) {
                    AppContext.beneficiaryService = new BeneficiaryServiceImpl(AppContext.db);
                }
                List<BeneficiaryModel> all = AppContext.beneficiaryService == null ? List.of() : AppContext.beneficiaryService.getAllBeneficiary();
                hasRows = all != null && !all.isEmpty();
            } catch (Throwable t) {
                hasRows = false;
            }
        }
        if (btnSendSMS != null) btnSendSMS.setDisable(!(hasPort && hasRows));
    }

    @FXML
    private void onSendSMS() {
        String port = cbSelectPorts == null ? null : cbSelectPorts.getSelectionModel().getSelectedItem();
        String recipientGroup = cbSelectBeneficiary == null ? null : cbSelectBeneficiary.getSelectionModel().getSelectedItem();
        String message = txtMessage == null ? null : txtMessage.getText();

        if (message == null || message.trim().isEmpty()) {
            return;
        }

        if (port == null) {
            return;
        }

        if ("All Beneficiaries".equals(recipientGroup)) {
            if (AppContext.beneficiaryService == null) {
                if (AppContext.db != null) AppContext.beneficiaryService = new BeneficiaryServiceImpl(AppContext.db);
            }
            List<BeneficiaryModel> beneficiaries = AppContext.beneficiaryService == null ? List.of() : AppContext.beneficiaryService.getAllBeneficiary();
            int recipientCount = beneficiaries == null ? 0 : beneficiaries.size();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Bulk Send");
            confirm.setHeaderText("Send SMS to All Beneficiaries");
            confirm.setContentText("You are about to send this message to " + recipientCount + " recipients. Continue?");
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isEmpty() || res.get() != ButtonType.OK) return;
        }

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                SMSSender sms = SMSSender.getInstance();
                try {
                    if (!sms.isConnected()) {
                        updateMessage("Connecting to " + port);
                        boolean ok = sms.connectToPort(port, 3000);
                        if (!ok) {
                            updateMessage("Failed to connect to port " + port);
                            return null;
                        }
                    }

                    if ("All Beneficiaries".equals(recipientGroup)) {
                        updateMessage("Sending to all beneficiaries...");
                        if (AppContext.beneficiaryService == null) {
                            if (AppContext.db != null) AppContext.beneficiaryService = new BeneficiaryServiceImpl(AppContext.db);
                        }
                        List<BeneficiaryModel> beneficiaries = AppContext.beneficiaryService == null ? List.of() : AppContext.beneficiaryService.getAllBeneficiary();
                        List<String> numbers = new ArrayList<>();
                        List<String> names = new ArrayList<>();
                        List<Integer> ids = new ArrayList<>();
                        for (BeneficiaryModel b : beneficiaries) {
                            if (b == null) continue;
                            String phone = b.getMobileNumber();
                            if (phone == null || phone.trim().isEmpty()) continue;
                            numbers.add(phone.trim());
                            String fn = b.getFirstname() == null ? "" : b.getFirstname();
                            String mn = b.getMiddlename() == null ? "" : b.getMiddlename();
                            String ln = b.getLastname() == null ? "" : b.getLastname();
                            String fullname = (fn + " " + (mn.isEmpty() ? "" : mn + " ") + ln).trim();
                            names.add(fullname);
                            ids.add(b.getId());
                        }

                        int successes = sms.sendSMS(numbers, names, ids, message, 1000, 3);
                        updateMessage("Bulk send complete. Successes: " + successes);
                    } else {
                        updateMessage("Recipient group not supported: " + recipientGroup);
                    }

                } catch (Exception ex) {
                    updateMessage("Error during send: " + ex.getMessage());
                }
                return null;
            }
        };


        Thread t = new Thread(task, "SMSSender-UI-Task");
        t.setDaemon(true);
        t.start();
    }

    public static FXMLLoader loadFXML() throws IOException {
        FXMLLoader loader = new FXMLLoader(SendSMSController.class.getResource("/view/send_sms/SendSMS.fxml"));
        loader.load();
        return loader;
    }

    public static Parent reloadView() throws IOException {
        FXMLLoader loader = loadFXML();
        return loader.getRoot();
    }

}
