package com.ionres.respondph.sendsms;

import com.ionres.respondph.common.services.NewsGeneratorService;
import com.ionres.respondph.util.AlertDialogManager;
import com.ionres.respondph.util.AppContext;
import com.ionres.respondph.util.SMSSender;
import com.ionres.respondph.util.InternetConnectionChecker;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.beneficiary.BeneficiaryServiceImpl;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    @FXML private Label connectionStatusLabel; // Optional: Add this to your FXML if you want to show connection status
    @FXML private Button btnRefreshPorts; // Optional: Add refresh button

    @FXML private ComboBox<String> cbNewsTopic;
    @FXML private Button btnGenerateNews;
    @FXML private VBox aiResponseContainer;
    @FXML private Label aiResponseLabel;
    @FXML private HBox aiResponseActions;
    @FXML private Button btnUseSelectedNews;
    @FXML private ToggleGroup newsAiResponse;
    @FXML private Label lblNetworkStatus;
    @FXML private Button btnRefreshNetwork;

    private final ObservableList<SmsModel> logRows = FXCollections.observableArrayList();
    private SmsService smsService;
    private SMSSender smsSender; // Add this field
    private NewsGeneratorService newsGeneratorService;
    private NewsGeneratorService.NewsResult lastNewsResult;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        smsService = new SmsServiceImpl();
        smsSender = SMSSender.getInstance(); // Get the SMSSender instance
        newsGeneratorService = new NewsGeneratorService();

        setupRadioButtons();
        setupControls();
        loadSMSLogs();
        populateAvailablePorts();
        setupNewsControls();
        checkNetworkStatus();

        if (charCount != null) charCount.setText("0/160 characters");

        updateConnectionStatus();
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
            if (btnRefreshPorts != null) {
                btnRefreshPorts.setDisable(api);
            }
            updateSendButtonState();
            updateConnectionStatus();
        });

        rbGsm.selectedProperty().addListener((o, oldV, gsm) -> {
            if (!gsm) return;

            // When GSM is selected, auto-connect if a port is selected
            if (cbSelectPorts != null && cbSelectPorts.getSelectionModel().getSelectedItem() != null) {
                String selectedPort = cbSelectPorts.getSelectionModel().getSelectedItem();
                connectToSelectedPort(selectedPort);
            }
        });
    }

    private void populateAvailablePorts() {
        if (cbSelectPorts == null) return;

        Platform.runLater(() -> {
            try {
                // Use SMSSender utility to get available ports
                List<String> availablePorts = smsSender.getAvailablePorts();

                if (availablePorts.isEmpty()) {
                    cbSelectPorts.setItems(FXCollections.observableArrayList("No ports available"));
                    cbSelectPorts.setDisable(true);
                    cbSelectPorts.getSelectionModel().selectFirst();

                    if (connectionStatusLabel != null) {
                        connectionStatusLabel.setText("Disconnected - No ports found");
                        connectionStatusLabel.setStyle("-fx-text-fill: red;");
                    }
                } else {
                    ObservableList<String> portOptions = FXCollections.observableArrayList(availablePorts);
                    cbSelectPorts.setItems(portOptions);
                    cbSelectPorts.setDisable(false);

                    // Try to select the currently connected port if any
                    String connectedPort = smsSender.getConnectedPort();
                    if (connectedPort != null && portOptions.contains(connectedPort)) {
                        cbSelectPorts.getSelectionModel().select(connectedPort);
                        if (connectionStatusLabel != null) {
                            connectionStatusLabel.setText("Connected to " + connectedPort);
                            connectionStatusLabel.setStyle("-fx-text-fill: green;");
                        }
                    } else {
                        cbSelectPorts.getSelectionModel().selectFirst();
                        if (connectionStatusLabel != null) {
                            connectionStatusLabel.setText("Disconnected");
                            connectionStatusLabel.setStyle("-fx-text-fill: orange;");
                        }
                    }

                    // Add listener for port selection changes
                    cbSelectPorts.getSelectionModel().selectedItemProperty().addListener(
                            (obs, oldPort, newPort) -> {
                                if (newPort != null && !newPort.equals(oldPort) && rbGsm.isSelected()) {
                                    connectToSelectedPort(newPort);
                                }
                                updateSendButtonState();
                            }
                    );
                }
            } catch (Exception e) {
                System.err.println("Error detecting serial ports: " + e.getMessage());
                e.printStackTrace();
                cbSelectPorts.setItems(FXCollections.observableArrayList("Error detecting ports"));
                cbSelectPorts.setDisable(true);
                cbSelectPorts.getSelectionModel().selectFirst();

                if (connectionStatusLabel != null) {
                    connectionStatusLabel.setText("Error: " + e.getMessage());
                    connectionStatusLabel.setStyle("-fx-text-fill: red;");
                }
            }
        });
    }

    private void connectToSelectedPort(String portName) {
        if (portName == null || portName.isEmpty() ||
                portName.contains("No ports") || portName.contains("Error")) {
            return;
        }

        new Thread(() -> {
            boolean connected = smsSender.connectToPort(portName, 5000);

            Platform.runLater(() -> {
                if (connected) {
                    System.out.println("Successfully connected to port: " + portName);
                    if (connectionStatusLabel != null) {
                        connectionStatusLabel.setText("Connected to " + portName);
                        connectionStatusLabel.setStyle("-fx-text-fill: green;");
                    }
                    AlertDialogManager.showSuccess("Connection Successful",
                            "Connected to GSM modem on port: " + portName);
                } else {
                    System.err.println("Failed to connect to port: " + portName);
                    if (connectionStatusLabel != null) {
                        connectionStatusLabel.setText("Failed to connect to " + portName);
                        connectionStatusLabel.setStyle("-fx-text-fill: red;");
                    }
                    AlertDialogManager.showError("Connection Failed",
                            "Failed to connect to GSM modem on port: " + portName +
                                    "\nPlease check if the modem is properly connected.");
                }
                updateSendButtonState();
            });
        }, "GSM-Connect-Thread").start();
    }

    private void updateConnectionStatus() {
        if (connectionStatusLabel == null) return;

        if (rbApi.isSelected()) {
            connectionStatusLabel.setText("Using SMS API");
            connectionStatusLabel.setStyle("-fx-text-fill: blue;");
        } else if (rbGsm.isSelected()) {
            if (smsSender.isConnected()) {
                String port = smsSender.getConnectedPort();
                connectionStatusLabel.setText("Connected to " + port);
                connectionStatusLabel.setStyle("-fx-text-fill: green;");
            } else {
                connectionStatusLabel.setText("Disconnected - Select a port");
                connectionStatusLabel.setStyle("-fx-text-fill: orange;");
            }
        }
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
            e.printStackTrace();
        }
    }

    private void setupControls() {
        setupTableColumns();
        setupEventHandlers();
        populateComboBoxes();
    }

    private void setupNewsControls() {
        if (aiResponseContainer != null) {
            aiResponseContainer.setVisible(false);
            aiResponseContainer.setManaged(false);
        }
        if (btnUseSelectedNews != null) {
            btnUseSelectedNews.setVisible(false);
            btnUseSelectedNews.setManaged(false);
            btnUseSelectedNews.setOnAction(e -> onUseSelectedNews());
        }
        if (btnGenerateNews != null) {
            btnGenerateNews.setOnAction(e -> onGenerateNews());
        }
        if (newsAiResponse == null) {
            newsAiResponse = new ToggleGroup();
        }
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

        if (btnRefreshPorts != null) {
            btnRefreshPorts.setOnAction(e -> populateAvailablePorts());
        }
        if (btnRefreshNetwork != null) {
            btnRefreshNetwork.setOnAction(e -> onRefreshNetwork());
        }
    }

    @FXML
    private void onRefreshNetwork() {
        checkNetworkStatus();
    }

    private void checkNetworkStatus() {
        if (lblNetworkStatus == null) return;

        javafx.concurrent.Task<Boolean> task = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() {
                try {
                    HttpClient client = HttpClient.newBuilder()
                            .connectTimeout(java.time.Duration.ofSeconds(5))
                            .build();
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create("https://www.google.com"))
                            .method("HEAD", HttpRequest.BodyPublishers.noBody())
                            .timeout(java.time.Duration.ofSeconds(5))
                            .build();
                    HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
                    return resp.statusCode() / 100 == 2;
                } catch (Exception ex) {
                    return false;
                }
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            boolean online = Boolean.TRUE.equals(task.getValue());
            updateNetworkLabel(online);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> updateNetworkLabel(false)));

        Thread t = new Thread(task, "network-check");
        t.setDaemon(true);
        t.start();
    }

    private void updateNetworkLabel(boolean online) {
        if (lblNetworkStatus == null) return;
        lblNetworkStatus.setText(online ? "Online" : "Offline - Internet required for AI APIs");
        lblNetworkStatus.setStyle(online ? "-fx-text-fill: #22c55e;" : "-fx-text-fill: #ef4444;");
        if (!online) {
            AlertDialogManager.showWarning("No Internet", "Internet connection is required to use AI news APIs.");
        }
    }

    @FXML
    private void onGenerateNews() {
        if (cbNewsTopic == null || btnGenerateNews == null) return;

        if (!InternetConnectionChecker.isOnline()) {
            AlertDialogManager.showError("Offline", "No internet connection. AI news generation requires internet.");
            return;
        }

        String topic = cbNewsTopic.getSelectionModel().getSelectedItem();
        if (topic == null || topic.isBlank()) {
            AlertDialogManager.showWarning("Select Topic", "Please select a news topic first.");
            return;
        }

        if ("Custom Topic".equalsIgnoreCase(topic)) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Custom Topic");
            dialog.setHeaderText("Enter a custom news topic");
            dialog.setContentText("Topic:");
            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty() || result.get().trim().isEmpty()) {
                return;
            }
            topic = result.get().trim();
        }

        final String requestTopic = topic;

        setAiBusy(true, "Generating AI news...");

        javafx.concurrent.Task<NewsGeneratorService.NewsResult> task = new javafx.concurrent.Task<>() {
            @Override
            protected NewsGeneratorService.NewsResult call() throws Exception {
                return newsGeneratorService.generateLatestNews(requestTopic);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            lastNewsResult = task.getValue();
            populateAiOptions(lastNewsResult == null ? List.of() : lastNewsResult.getOptions());
            setAiBusy(false, null);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            setAiBusy(false, null);
            String msg = task.getException() == null ? "Unknown error" : task.getException().getMessage();
            AlertDialogManager.showError("AI News Error", msg);
        }));

        Thread t = new Thread(task, "AI-News-Generator");
        t.setDaemon(true);
        t.start();
    }

    private void populateAiOptions(List<String> options) {
        if (aiResponseContainer == null) return;

        clearAiOptions();
        if (options == null || options.isEmpty()) {
            showAiContainer(false);
            AlertDialogManager.showWarning("No News", "No generated news was returned.");
            return;
        }

        int index = 1;
        for (String option : options) {
            String text = option == null ? "" : option.trim();
            if (text.isEmpty()) continue;

            RadioButton rb = new RadioButton(index + ") " + text);
            rb.getStyleClass().add("ai-news-option");
            rb.setToggleGroup(newsAiResponse);
            rb.setWrapText(true);
            rb.setMaxWidth(Double.MAX_VALUE);
            insertOptionNode(rb);
            if (index == 1) {
                rb.setSelected(true);
            }
            index++;
        }

        showAiContainer(true);
        aiResponseContainer.applyCss();
        aiResponseContainer.layout();
    }

    @FXML
    private void onUseSelectedNews() {
        if (newsAiResponse == null || txtMessage == null) return;
        Toggle selected = newsAiResponse.getSelectedToggle();
        if (selected instanceof RadioButton) {
            String text = ((RadioButton) selected).getText();
            if (text != null) {
                String cleaned = text.replaceFirst("^\\d+\\)\\s*", "");
                txtMessage.setText(cleaned);
            }
        }
    }

    private void setAiBusy(boolean busy, String placeholder) {
        if (btnGenerateNews != null) {
            btnGenerateNews.setDisable(busy);
        }
        if (busy) {
            clearAiOptions();
            showAiContainer(false);
        }
    }

    private void clearAiOptions() {
        if (aiResponseContainer == null) return;
        int fromIndex = aiResponseLabel == null ? 0 : aiResponseContainer.getChildren().indexOf(aiResponseLabel) + 1;
        int toIndex = aiResponseActions == null ? aiResponseContainer.getChildren().size() :
                aiResponseContainer.getChildren().indexOf(aiResponseActions);

        if (fromIndex < 0 || toIndex < 0 || fromIndex >= toIndex) {
            return;
        }
        aiResponseContainer.getChildren().remove(fromIndex, toIndex);
    }

    private void insertOptionNode(javafx.scene.Node node) {
        if (aiResponseContainer == null) return;
        int insertIndex = aiResponseActions == null ? aiResponseContainer.getChildren().size() :
                aiResponseContainer.getChildren().indexOf(aiResponseActions);
        if (insertIndex < 0) {
            insertIndex = aiResponseContainer.getChildren().size();
        }
        aiResponseContainer.getChildren().add(insertIndex, node);
    }

    private void showAiContainer(boolean show) {
        if (aiResponseContainer != null) {
            aiResponseContainer.setVisible(show);
            aiResponseContainer.setManaged(show);
        }
        if (btnUseSelectedNews != null) {
            btnUseSelectedNews.setVisible(show);
            btnUseSelectedNews.setManaged(show);
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
    }

    private void updateSendButtonState() {
        boolean canSend = false;

        if (rbApi != null && rbApi.isSelected()) {
            canSend = true;
        } else if (rbGsm != null && rbGsm.isSelected()) {
            String selectedPort = cbSelectPorts == null ? null :
                    cbSelectPorts.getSelectionModel().getSelectedItem();

            boolean validPort = selectedPort != null &&
                    !selectedPort.contains("No ports") &&
                    !selectedPort.contains("Error");

            boolean isConnected = smsSender != null && smsSender.isConnected();

            canSend = validPort && isConnected;
        }

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
        String selectedPort = null;

        if ("API".equals(method) && !InternetConnectionChecker.isOnline()) {
            AlertDialogManager.showError("Offline", "No internet connection. SMS API sending requires internet.");
            return;
        }

        if ("GSM".equals(method)) {
            selectedPort = cbSelectPorts == null ? null :
                    cbSelectPorts.getSelectionModel().getSelectedItem();
            if (selectedPort == null || selectedPort.contains("No ports") ||
                    selectedPort.contains("Error")) {
                AlertDialogManager.showError("No Port Selected",
                        "Please select a valid GSM port.");
                return;
            }

            if (!smsSender.isConnected()) {
                AlertDialogManager.showError("GSM Modem Not Connected",
                        "Please connect to a GSM modem before sending messages.");
                return;
            }
        }

        if ("All Beneficiaries".equals(recipientGroup)) {
            sendToAllBeneficiaries(message, method, selectedPort);
        } else {
            AlertDialogManager.showError("Not Implemented", "This recipient group is not yet implemented.");
        }
    }

    private void sendToAllBeneficiaries(String message, String method, String port) {
        if (AppContext.beneficiaryService == null) {
            if (AppContext.db != null) {
                AppContext.beneficiaryService = new BeneficiaryServiceImpl(AppContext.db);
            }
        }

        List<BeneficiaryModel> beneficiaries = AppContext.beneficiaryService == null ?
                List.of() : AppContext.beneficiaryService.getAllBeneficiary();

        int recipientCount = beneficiaries == null ? 0 : beneficiaries.size();

        boolean confirm = AlertDialogManager.showConfirmation("Confirm Bulk Send", "Send SMS to All Beneficiaries");

        if (confirm) {
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

            task.setOnFailed(e -> {
                Platform.runLater(() -> {
                    AlertDialogManager.showError("Send Failed",
                            "An error occurred while sending messages.");
                    if (e.getSource().getException() != null) {
                        e.getSource().getException().printStackTrace();
                    }
                });
            });

            Thread t = new Thread(task, "SMSSender-UI-Task");
            t.setDaemon(true);
            t.start();
        }
    }

    @FXML
    private void onRefreshPorts() {
        populateAvailablePorts();
    }

    @FXML
    private void onDisconnect() {
        if (smsSender != null && smsSender.isConnected()) {
            smsSender.disconnect();
            if (connectionStatusLabel != null) {
                connectionStatusLabel.setText("Disconnected");
                connectionStatusLabel.setStyle("-fx-text-fill: orange;");
            }
            AlertDialogManager.showInfo("Disconnected", "Disconnected from GSM modem.");
            updateSendButtonState();
        }
    }
}
