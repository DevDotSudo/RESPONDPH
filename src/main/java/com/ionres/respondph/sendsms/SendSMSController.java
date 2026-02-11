package com.ionres.respondph.sendsms;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.beneficiary.BeneficiaryServiceImpl;
import com.ionres.respondph.common.services.NewsGeneratorService;
import com.ionres.respondph.sendsms.dialogs_controller.BeneficiarySelectionDialogController;
import com.ionres.respondph.util.*;

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

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class SendSMSController implements Initializable {
    @FXML private ComboBox<String> cbSelectBeneficiary;
    @FXML private ComboBox<String> cbSelectPorts;
    @FXML private ComboBox<String> cbSelectBarangay;
    @FXML private HBox barangaySelectionBox;

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

    @FXML private Label connectionStatusLabel;
    @FXML private Button btnRefreshPorts;

    @FXML private HBox disasterSelectionBox;
    @FXML private ComboBox<DisasterModel> cbSelectDisaster;

    @FXML private ComboBox<String> cbNewsTopic;
    @FXML private Button btnGenerateNews;
    @FXML private VBox aiResponseContainer;
    @FXML private Label aiResponseLabel;
    @FXML private HBox aiResponseActions;
    @FXML private Button btnUseSelectedNews;
    @FXML private ToggleGroup newsAiResponse;

    @FXML private Label lblNetworkStatus;
    @FXML private Button btnRefreshNetwork;

    @FXML private TextArea txtCustomEvacMessage;
    @FXML private Button btnSaveEvacMessage;
    @FXML private Label lblEvacMessageStatus;

    private CustomEvacMessageManager evacMessageManager;

    private List<BeneficiaryModel> selectedBeneficiariesList = new ArrayList<>();

    private final ObservableList<SmsModel> logRows = FXCollections.observableArrayList();
    private SmsService smsService;
    private SMSSender smsSender;
    private NewsGeneratorService newsGeneratorService;
    private NewsGeneratorService.NewsResult lastNewsResult;
    private BeneficiaryDAO beneficiaryDAO;
    private DisasterDAO disasterDAO;

    private boolean isOpeningDialog = false;
    private boolean isUpdatingComboBox = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        smsService = new SmsServiceImpl();
        smsSender = SMSSender.getInstance();
        newsGeneratorService = new NewsGeneratorService();
        beneficiaryDAO = new BeneficiaryDAOImpl();
        disasterDAO = new DisasterDAOImpl();
        setupRadioButtons();
        setupControls();
        loadSMSLogs();
        populateAvailablePorts();
        setupNewsControls();
        checkNetworkStatus();
        DashboardRefresher.registerDisasterAndBeneficiaryCombo(this);

        evacMessageManager = CustomEvacMessageManager.getInstance();

        loadExistingEvacMessage();

        if (charCount != null) charCount.setText("0/160 characters");

        updateConnectionStatus();

        System.out.println("SendSMSController initialized successfully");
    }

    @FXML
    private void onRecipientGroupChanged() {
        System.out.println("DEBUG: onRecipientGroupChanged() called, isUpdatingComboBox=" + isUpdatingComboBox + ", isOpeningDialog=" + isOpeningDialog);

        if (isUpdatingComboBox || isOpeningDialog) {
            System.out.println("DEBUG: Skipping onRecipientGroupChanged - flags are set");
            return;
        }

        String selectedGroup = cbSelectBeneficiary.getValue();

        if (selectedGroup == null) {
            System.out.println("DEBUG: selectedGroup is null, returning");
            return;
        }

        System.out.println("DEBUG: Processing selectedGroup: " + selectedGroup);

        if ("By Barangay".equals(selectedGroup)) {
            disasterSelectionBox.setVisible(false);
            disasterSelectionBox.setManaged(false);

            barangaySelectionBox.setVisible(true);
            barangaySelectionBox.setManaged(true);

            if (cbSelectBarangay.getItems().isEmpty()) {
                loadBarangayList();
            }
        }
        else if ("By Disaster Area".equals(selectedGroup)) {
            barangaySelectionBox.setVisible(false);
            barangaySelectionBox.setManaged(false);

            disasterSelectionBox.setVisible(true);
            disasterSelectionBox.setManaged(true);

            if (cbSelectDisaster.getItems().isEmpty()) {
                loadDisasters();
            }
        }
        else if (selectedGroup.startsWith("Selected Beneficiaries")) {
            barangaySelectionBox.setVisible(false);
            barangaySelectionBox.setManaged(false);

            disasterSelectionBox.setVisible(false);
            disasterSelectionBox.setManaged(false);

            openBeneficiarySelectionDialog();
        }
        else {
            barangaySelectionBox.setVisible(false);
            barangaySelectionBox.setManaged(false);
            cbSelectBarangay.setValue(null);

            disasterSelectionBox.setVisible(false);
            disasterSelectionBox.setManaged(false);
            if (cbSelectDisaster != null) cbSelectDisaster.setValue(null);
        }

        updateSendButtonState();
    }

    public void loadDisasters() {
        try {
            System.out.println("DEBUG: Loading disaster list...");
            List<DisasterModel> disasters = disasterDAO.getAllDisasters();
            cbSelectDisaster.getItems().clear();
            cbSelectDisaster.getItems().addAll(disasters);
            System.out.println("DEBUG: Loaded " + disasters.size() + " disasters");
        } catch (Exception e) {
            System.err.println("Error loading disasters: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadBarangayList() {
        new Thread(() -> {
            try {
                System.out.println("DEBUG: Loading barangay list...");
                List<String> barangays = beneficiaryDAO.getAllBarangays();

                Platform.runLater(() -> {
                    ObservableList<String> barangayList = FXCollections.observableArrayList(barangays);
                    cbSelectBarangay.setItems(barangayList);

                    if (barangays.isEmpty()) {
                        AlertDialogManager.showError("No Barangays Found",
                                "No barangays were found. Make sure beneficiaries have valid coordinates."
                        );
                    } else {
                        System.out.println("DEBUG: Loaded " + barangays.size() + " barangays");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    AlertDialogManager.showError("Error Loading Barangays",
                            "Failed to load barangay list: " + e.getMessage()
                    );
                });
                e.printStackTrace();
            }
        }, "Load-Barangays-Thread").start();
    }

    private void openBeneficiarySelectionDialog() {
        if (isOpeningDialog) return;

        try {
            isOpeningDialog = true;

            List<BeneficiaryModel> allBeneficiaries = beneficiaryDAO.getAllBeneficiaries();

            if (allBeneficiaries == null || allBeneficiaries.isEmpty()) {
                AlertDialogManager.showWarning(
                        "No Beneficiaries Available",
                        "There are no beneficiaries in the system to select from.\nPlease add beneficiaries first."
                );
                return;
            }

            BeneficiarySelectionDialogController controller =
                    DialogManager.getController("selection", BeneficiarySelectionDialogController.class);

            // ✅ reset button/flags + clear search but KEEP checkbox selections
            controller.resetState();

            // ✅ IMPORTANT: do not call setBeneficiaries every time (it clears selections)
            if (!controller.isLoaded()) {
                controller.setBeneficiaries(allBeneficiaries);
            }

            // ✅ keep checks synced with last selected list (safe)
            if (!selectedBeneficiariesList.isEmpty()) {
                controller.setPreselectedBeneficiaries(selectedBeneficiariesList);
            }

            DialogManager.show("selection");

            if (controller.isOkClicked()) {
                updateSelectedBeneficiaries(controller.getSelectedBeneficiaries());
            } else {
                handleDialogCancelled();
            }

            updateSendButtonState();

        } catch (Exception e) {
            System.err.println("Error opening beneficiary selection dialog: " + e.getMessage());
            e.printStackTrace();
            AlertDialogManager.showError("Error", "Failed to open beneficiary selection dialog.");
            isUpdatingComboBox = false;

        } finally {
            isOpeningDialog = false;
            Platform.runLater(() -> isUpdatingComboBox = false);
        }
    }

    private void updateSelectedBeneficiaries(List<BeneficiaryModel> beneficiaries) {
        isUpdatingComboBox = true;

        try {
            selectedBeneficiariesList = beneficiaries;

            String displayText = String.format("Selected Beneficiaries (%d selected)", beneficiaries.size());
            ObservableList<String> items = cbSelectBeneficiary.getItems();

            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).startsWith("Selected Beneficiaries")) {
                    items.set(i, displayText);
                    cbSelectBeneficiary.getSelectionModel().select(i);
                    break;
                }
            }
        } finally {
            Platform.runLater(() -> {
                isUpdatingComboBox = false;
                updateSendButtonState();
            });
        }
    }


    private void handleDialogCancelled() {
        isUpdatingComboBox = true;

        try {
            if (selectedBeneficiariesList.isEmpty()) {
                Platform.runLater(() -> cbSelectBeneficiary.getSelectionModel().selectFirst());
            } else {
                String displayText = String.format("Selected Beneficiaries (%d selected)", selectedBeneficiariesList.size());
                ObservableList<String> items = cbSelectBeneficiary.getItems();

                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i).startsWith("Selected Beneficiaries")) {
                        items.set(i, displayText);
                        cbSelectBeneficiary.getSelectionModel().select(i);
                        break;
                    }
                }
            }
        } finally {
            Platform.runLater(() -> {
                isUpdatingComboBox = false;
                updateSendButtonState();
            });
        }
    }


    private void setupRadioButtons() {
        ToggleGroup group = new ToggleGroup();
        rbGsm.setToggleGroup(group);
        rbApi.setToggleGroup(group);

        rbApi.setSelected(true);

        rbApi.selectedProperty().addListener((o, oldV, api) -> {
            if (cbSelectPorts != null) cbSelectPorts.setDisable(api);
            if (btnRefreshPorts != null) btnRefreshPorts.setDisable(api);
            updateSendButtonState();
            updateConnectionStatus();
        });

        rbGsm.selectedProperty().addListener((o, oldV, gsm) -> {
            if (!gsm) return;

            if (cbSelectPorts != null && cbSelectPorts.getSelectionModel().getSelectedItem() != null) {
                String selectedPort = cbSelectPorts.getSelectionModel().getSelectedItem();
                connectToSelectedPort(selectedPort);
            }
            updateSendButtonState();
        });
    }

    private void populateAvailablePorts() {
        if (cbSelectPorts == null) return;

        Platform.runLater(() -> {
            try {
                System.out.println("DEBUG: Scanning for serial ports...");
                List<String> availablePorts = smsSender.getAvailablePorts();

                if (availablePorts.isEmpty()) {
                    cbSelectPorts.setItems(FXCollections.observableArrayList("No ports available"));
                    cbSelectPorts.setDisable(true);
                    cbSelectPorts.getSelectionModel().selectFirst();

                    if (connectionStatusLabel != null) {
                        connectionStatusLabel.setText("Disconnected - No ports found");
                        connectionStatusLabel.setStyle("-fx-text-fill: red;");
                    }
                    System.out.println("DEBUG: No serial ports found");
                } else {
                    ObservableList<String> portOptions = FXCollections.observableArrayList(availablePorts);
                    cbSelectPorts.setItems(portOptions);
                    cbSelectPorts.setDisable(false);

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

                    System.out.println("DEBUG: Found " + availablePorts.size() + " serial ports");

                    cbSelectPorts.getSelectionModel().selectedItemProperty().addListener((obs, oldPort, newPort) -> {
                        if (newPort != null && !newPort.equals(oldPort) && rbGsm.isSelected()) {
                            connectToSelectedPort(newPort);
                        }
                        updateSendButtonState();
                    });
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
        if (portName == null || portName.isEmpty() || portName.contains("No ports") || portName.contains("Error")) return;

        System.out.println("DEBUG: Attempting to connect to port: " + portName);

        new Thread(() -> {
            boolean connected = smsSender.connectToPort(portName, 5000);

            Platform.runLater(() -> {
                if (connected) {
                    System.out.println("Successfully connected to port: " + portName);
                    if (connectionStatusLabel != null) {
                        connectionStatusLabel.setText("Connected to " + portName);
                        connectionStatusLabel.setStyle("-fx-text-fill: green;");
                    }
                    AlertDialogManager.showSuccess("Connection Successful", "Connected to GSM modem on port: " + portName);
                } else {
                    System.err.println("Failed to connect to port: " + portName);
                    if (connectionStatusLabel != null) {
                        connectionStatusLabel.setText("Failed to connect to " + portName);
                        connectionStatusLabel.setStyle("-fx-text-fill: red;");
                    }
                    AlertDialogManager.showError("Connection Failed",
                            "Failed to connect to GSM modem on port: " + portName + "\nPlease check if the modem is properly connected.");
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
            System.out.println("DEBUG: Loading SMS logs...");
            List<SmsModel> saved = smsService.getAllSMSLogs();
            if (saved != null) {
                logRows.clear();
                logRows.addAll(saved);
                System.out.println("DEBUG: Loaded " + saved.size() + " SMS logs");
            }
            if (adminTable != null) adminTable.setItems(logRows);
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
        if (btnSaveEvacMessage != null) {
            btnSaveEvacMessage.setOnAction(e -> onSaveEvacMessage());
        }
    }

    private void loadExistingEvacMessage() {
        if (evacMessageManager.hasCustomMessage()) {
            String savedMessage = evacMessageManager.getCustomEvacuationMessage();
            if (txtCustomEvacMessage != null) {
                txtCustomEvacMessage.setText(savedMessage);
            }
            updateEvacMessageStatus(true, savedMessage.length());
        }
    }

    @FXML
    private void onSaveEvacMessage() {
        String message = txtCustomEvacMessage != null ? txtCustomEvacMessage.getText() : null;

        if (message == null || message.trim().isEmpty()) {
            AlertDialogManager.showWarning("Empty Message",
                    "Please enter a custom evacuation message before saving.");
            return;
        }

        // Validate message length (typical SMS is 160 characters)
        if (message.length() > 160) {
            AlertDialogManager.showWarning("Message Too Long",
                    "Message is " + message.length() + " characters. SMS messages are typically limited to 160 characters.\n\n" +
                            "Your message may be split into multiple SMS or truncated.");
        }

        // Validate that placeholders are present
        if (!message.contains("{name}") && !message.contains("{evacSite}") && !message.contains("{site}")) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Missing Placeholders");
            confirm.setHeaderText("Your message doesn't contain placeholders");
            confirm.setContentText("Recommended placeholders: {name}, {evacSite}\n\n" +
                    "Continue saving anyway?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }

        // Save the custom message to the singleton manager
        evacMessageManager.setCustomEvacuationMessage(message.trim());

        // Update status label
        updateEvacMessageStatus(true, message.trim().length());

        System.out.println("Custom evacuation SMS message saved: " + message.trim());
        AlertDialogManager.showSuccess("Message Saved",
                "Custom evacuation message template has been saved successfully.\n\n" +
                        "This message will be used when allocating beneficiaries to evacuation sites.");
    }

    /**
     * Update the evacuation message status label
     */
    private void updateEvacMessageStatus(boolean saved, int length) {
        if (lblEvacMessageStatus != null) {
            if (saved) {
                lblEvacMessageStatus.setText("✓ Custom evacuation message saved (" + length + " characters)");
                lblEvacMessageStatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            } else {
                lblEvacMessageStatus.setText("No custom evacuation message set (using default format)");
                lblEvacMessageStatus.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
            }
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

                        if (success) {
                            AlertDialogManager.showSuccess("Resend Successful", "SMS has been resent successfully.");
                        } else {
                            AlertDialogManager.showError("Resend Failed", "Failed to resend SMS. Check console for details.");
                        }
                    }));

                    task.setOnFailed(e -> Platform.runLater(() -> {
                        row.setStatus("FAILED");
                        adminTable.refresh();
                        resendBtn.setDisable(false);
                        AlertDialogManager.showError("Resend Error", "An error occurred while resending.");
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
                if (charCount != null) charCount.setText(len + "/160 characters");
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

        task.setOnSucceeded(e -> Platform.runLater(() -> updateNetworkLabel(Boolean.TRUE.equals(task.getValue()))));
        task.setOnFailed(e -> Platform.runLater(() -> updateNetworkLabel(false)));

        Thread t = new Thread(task, "network-check");
        t.setDaemon(true);
        t.start();
    }

    private void updateNetworkLabel(boolean online) {
        if (lblNetworkStatus == null) return;
        lblNetworkStatus.setText(online ? "Online" : "Offline - Internet required for AI APIs");
        lblNetworkStatus.setStyle(online ? "-fx-text-fill: #22c55e;" : "-fx-text-fill: #ef4444;");
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
            if (result.isEmpty() || result.get().trim().isEmpty()) return;
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

            if (index == 1) rb.setSelected(true);
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
        if (btnGenerateNews != null) btnGenerateNews.setDisable(busy);
        if (busy) {
            clearAiOptions();
            showAiContainer(false);
        }
    }

    private void clearAiOptions() {
        if (aiResponseContainer == null) return;
        int fromIndex = aiResponseLabel == null ? 0 : aiResponseContainer.getChildren().indexOf(aiResponseLabel) + 1;
        int toIndex = aiResponseActions == null ? aiResponseContainer.getChildren().size()
                : aiResponseContainer.getChildren().indexOf(aiResponseActions);

        if (fromIndex < 0 || toIndex < 0 || fromIndex >= toIndex) return;
        aiResponseContainer.getChildren().remove(fromIndex, toIndex);
    }

    private void insertOptionNode(javafx.scene.Node node) {
        if (aiResponseContainer == null) return;
        int insertIndex = aiResponseActions == null ? aiResponseContainer.getChildren().size()
                : aiResponseContainer.getChildren().indexOf(aiResponseActions);
        if (insertIndex < 0) insertIndex = aiResponseContainer.getChildren().size();
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
                    .addListener((obs, oldVal, newVal) -> onRecipientGroupChanged());
        }
    }

    private void updateSendButtonState() {
        boolean canSend = false;

        if (rbApi != null && rbApi.isSelected()) {
            canSend = true;
        } else if (rbGsm != null && rbGsm.isSelected()) {
            String selectedPort = cbSelectPorts == null ? null : cbSelectPorts.getSelectionModel().getSelectedItem();

            boolean validPort = selectedPort != null &&
                    !selectedPort.contains("No ports") &&
                    !selectedPort.contains("Error");

            boolean isConnected = smsSender != null && smsSender.isConnected();

            canSend = validPort && isConnected;
        }

        String sel = cbSelectBeneficiary == null ? null : cbSelectBeneficiary.getSelectionModel().getSelectedItem();
        if ("All Beneficiaries".equals(sel)) {
            try {
                if (AppContext.beneficiaryService == null && AppContext.db != null) {
                    AppContext.beneficiaryService = new BeneficiaryServiceImpl(AppContext.db);
                }
                List<BeneficiaryModel> all = AppContext.beneficiaryService == null
                        ? List.of()
                        : AppContext.beneficiaryService.getAllBeneficiary();
                canSend = canSend && all != null && !all.isEmpty();
            } catch (Exception e) {
                canSend = false;
            }
        }

        if (btnSendSMS != null) btnSendSMS.setDisable(!canSend);

        System.out.println("DEBUG: Send button state - Enabled: " + canSend);
    }

    @FXML
    private void onSendSMS() {
        String message = txtMessage.getText();
        if (message == null || message.trim().isEmpty()) {
            AlertDialogManager.showError("Empty Message", "Please enter a message to send.");
            return;
        }

        String recipientGroup = cbSelectBeneficiary.getValue();
        if (recipientGroup == null) {
            AlertDialogManager.showError("No Recipients", "Please select a recipient group.");
            return;
        }

        String sendMethod = rbGsm.isSelected() ? "GSM" : "API";

        System.out.println("DEBUG: Preparing to send SMS");
        System.out.println("DEBUG: Recipient group: " + recipientGroup);
        System.out.println("DEBUG: Send method: " + sendMethod);

        List<BeneficiaryModel> recipients = getRecipients(recipientGroup);

        if (recipients.isEmpty()) {
            AlertDialogManager.showError("No Recipients Found",
                    "No beneficiaries found for the selected criteria.\n" +
                            "Please check that beneficiaries have valid phone numbers."
            );
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Send");
        confirm.setHeaderText("Send SMS to " + recipients.size() + " recipient(s)?");
        confirm.setContentText("Method: " + sendMethod + "\nMessage: " +
                message.substring(0, Math.min(50, message.length())) + "...");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                sendSMSToRecipients(recipients, message, sendMethod);
            }
        });
    }

    private List<BeneficiaryModel> getRecipients(String recipientGroup) {
        System.out.println("DEBUG: Getting recipients for group: " + recipientGroup);

        String baseGroup = recipientGroup;
        if (recipientGroup.contains("(") && recipientGroup.contains("selected)")) {
            baseGroup = recipientGroup.substring(0, recipientGroup.indexOf("(")).trim();
        }

        switch (baseGroup) {
            case "All Beneficiaries": {
                List<BeneficiaryModel> all = beneficiaryDAO.getAllBeneficiaries();
                System.out.println("DEBUG: Found " + all.size() + " beneficiaries");
                return all;
            }

            case "By Barangay": {
                String selectedBarangay = cbSelectBarangay.getValue();
                if (selectedBarangay == null) {
                    Platform.runLater(() ->
                            AlertDialogManager.showError("No Barangay Selected", "Please select a barangay.")
                    );
                    return List.of();
                }
                List<BeneficiaryModel> byBarangay = beneficiaryDAO.getBeneficiariesByBarangay(selectedBarangay);
                System.out.println("DEBUG: Found " + byBarangay.size() + " beneficiaries in " + selectedBarangay);
                return byBarangay;
            }

            case "Selected Beneficiaries": {
                if (selectedBeneficiariesList.isEmpty()) {
                    Platform.runLater(() ->
                            AlertDialogManager.showError("No Beneficiaries Selected", "Please select beneficiaries first.")
                    );
                    return List.of();
                }
                System.out.println("DEBUG: Using " + selectedBeneficiariesList.size() + " pre-selected beneficiaries");
                return new ArrayList<>(selectedBeneficiariesList);
            }

            case "By Disaster Area": {
                DisasterModel selectedDisaster = cbSelectDisaster.getValue();
                if (selectedDisaster == null) {
                    Platform.runLater(() ->
                            AlertDialogManager.showError("No Disaster Selected", "Please select a disaster.")
                    );
                    return List.of();
                }
                System.out.println("DEBUG: Selected disaster: " + selectedDisaster.getName() +
                        " (ID: " + selectedDisaster.getDisasterId() + ")");
                List<BeneficiaryModel> byDisaster =
                        beneficiaryDAO.getBeneficiariesByDisaster(selectedDisaster.getDisasterId());
                System.out.println("DEBUG: Found " + byDisaster.size() + " beneficiaries affected by " + selectedDisaster.getName());
                return byDisaster;
            }

            case "Custom List":
                Platform.runLater(() ->
                        AlertDialogManager.showError("Not Implemented", "This feature will be implemented soon.")
                );
                return List.of();

            default:
                return List.of();
        }
    }

    private void sendSMSToRecipients(List<BeneficiaryModel> recipients, String message, String method) {
        btnSendSMS.setDisable(true);

        System.out.println("DEBUG: Starting send process for " + recipients.size() + " recipients");

        Alert progress = new Alert(Alert.AlertType.INFORMATION);
        progress.setTitle("Sending SMS");
        progress.setHeaderText("Sending messages...");
        progress.setContentText("Please wait... This may take a few moments.");
        progress.show();

        new Thread(() -> {
            int sent = smsService.sendBulkSMS(recipients, message, method);

            Platform.runLater(() -> {
                progress.close();
                btnSendSMS.setDisable(false);

                AlertDialogManager.showSuccess("SMS Send Complete",
                        "Successfully sent " + sent + " out of " + recipients.size() + " messages.\n" +
                                "Check the SMS History table below for details."
                );

                loadSMSLogs();
            });
        }, "SMS-Send-Thread").start();
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
