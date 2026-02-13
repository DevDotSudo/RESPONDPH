package com.ionres.respondph.sendsms;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.common.interfaces.BulkProgressListener;
import com.ionres.respondph.common.services.NewsGeneratorService;
import com.ionres.respondph.main.MainFrameController;
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
import javafx.concurrent.Task;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;

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
    @FXML private VBox aiLoadingBox;
    @FXML private ProgressIndicator piAi;
    @FXML private HBox smsLoadingBox;
    @FXML private ProgressIndicator piSms;
    @FXML private RadioButton rbNewsSlot1;
    @FXML private RadioButton rbNewsSlot2;
    @FXML private RadioButton rbNewsSlot3;
    @FXML private RadioButton rbNewsSlot4;
    @FXML private RadioButton rbNewsSlot5;
    @FXML private TextArea txtCustomEvacMessage;
    @FXML private Button btnSaveEvacMessage;
    @FXML private Label lblMessageStatus;

    private List<BeneficiaryModel> selectedBeneficiariesList = new ArrayList<>();
    private final List<String> storedNewsItems = new ArrayList<>();
    private RadioButton[] newsSlots;
    private final ObservableList<SmsModel> logRows = FXCollections.observableArrayList();
    private CustomEvacMessageManager evacMessageManager;
    private SmsService smsService;
    private SMSSender smsSender;
    private NewsGeneratorService newsGeneratorService;
    private BeneficiaryDAO beneficiaryDAO;
    private DisasterDAO disasterDAO;

    private boolean isOpeningDialog = false;
    private boolean isUpdatingComboBox = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        smsService = new SmsServiceImpl();
        smsSender = SMSSender.getInstance();

        try {
            newsGeneratorService = new NewsGeneratorService();
        } catch (Exception e) {
            newsGeneratorService = null;
            System.err.println("[AI] Disabled: " + e.getMessage());
        }

        beneficiaryDAO = new BeneficiaryDAOImpl();
        disasterDAO = new DisasterDAOImpl();

        newsSlots = new RadioButton[]{rbNewsSlot1, rbNewsSlot2, rbNewsSlot3, rbNewsSlot4, rbNewsSlot5};

        Platform.runLater(this::bindAiSlotWidths);

        setupRadioButtons();
        setupControls();
        loadSMSLogs();
        populateAvailablePorts();
        setupNewsControls();
        setupNewsSlots();
        checkNetworkStatus();

        DashboardRefresher.registerDisasterAndBeneficiaryCombo(this);

        evacMessageManager = CustomEvacMessageManager.getInstance();

        if (charCount != null) {
            charCount.setText("0/160 characters");
        }

        updateConnectionStatus();

        System.out.println("SendSMSController initialized successfully");
    }

    private void bindAiSlotWidths() {
        if (aiResponseContainer == null) return;

        aiResponseContainer.setFillWidth(true); // ✅ important

        double paddingLR = 32;

        for (RadioButton rb : newsSlots) {
            if (rb == null) continue;
            rb.setMaxWidth(Double.MAX_VALUE);
            rb.setMinWidth(0);
            rb.prefWidthProperty().bind(aiResponseContainer.widthProperty().subtract(paddingLR));
        }
    }

    private void setupNewsSlots() {
        if (newsAiResponse == null) newsAiResponse = new ToggleGroup();

        for (int i = 0; i < newsSlots.length; i++) {
            final int index = i;
            RadioButton slot = newsSlots[i];
            if (slot == null) continue;

            slot.setToggleGroup(newsAiResponse);

            slot.getStyleClass().add("ai-news-slot");

            // ✅ WRAP + GROW HEIGHT
            slot.setWrapText(true);
            slot.setAlignment(Pos.TOP_LEFT);
            slot.setTextOverrun(OverrunStyle.CLIP);   // (or ELLIPSIS if you want)
            slot.setMinHeight(Region.USE_PREF_SIZE);
            slot.setPrefHeight(Region.USE_COMPUTED_SIZE);
            slot.setMaxHeight(Double.MAX_VALUE);

            slot.setDisable(true);

            slot.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected && index < storedNewsItems.size()) {
                    loadNewsToMessage(index);
                }
            });
        }
    }

    private void loadNewsToMessage(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= storedNewsItems.size() || txtMessage == null) return;

        String newsText = storedNewsItems.get(slotIndex);
        if (newsText == null) return;

        String cleaned = newsText
                .replaceAll("(?i)\\s*\\(\\s*Source\\s*:\\s*https?://[^\\s)]+\\s*\\)\\s*$", "")
                .trim();

        txtMessage.setText(cleaned);
    }

    private void updateNewsSlot(int slotIndex, String newsText) {
        if (slotIndex < 0 || slotIndex >= newsSlots.length) return;
        RadioButton slot = newsSlots[slotIndex];
        if (slot == null) return;

        String displayText = (newsText != null) ? newsText.trim() : "";

        slot.setText(displayText);
        slot.setDisable(false);

        if (slotIndex == 0) slot.setSelected(true);
    }

    private void clearNewsSlots() {
        for (int i = 0; i < newsSlots.length; i++) {
            RadioButton slot = newsSlots[i];
            if (slot != null) {
                slot.setText("Slot " + (i + 1) + " (Empty)");
                slot.setDisable(true);
                slot.setSelected(false);
            }
        }
    }

    private void setupRadioButtons() {
        if (rbApi != null) {
            rbApi.selectedProperty().addListener((obs, old, selected) -> {
                if (cbSelectPorts != null) cbSelectPorts.setDisable(selected);
                if (btnRefreshPorts != null) btnRefreshPorts.setDisable(selected);
                updateSendButtonState();
                updateConnectionStatus();
            });
        }

        if (rbGsm != null) {
            rbGsm.selectedProperty().addListener((obs, old, selected) -> {
                if (!selected) return;
                if (cbSelectPorts != null) {
                    String port = cbSelectPorts.getSelectionModel().getSelectedItem();
                    if (port != null && !port.contains("No") && !port.contains("Error")) {
                        connectToSelectedPort(port);
                    }
                }
                updateSendButtonState();
            });
        }
    }

    private void populateAvailablePorts() {
        if (cbSelectPorts == null) return;

        Platform.runLater(() -> {
            try {
                List<String> ports = smsSender.getAvailablePorts();

                if (ports.isEmpty()) {
                    cbSelectPorts.setItems(FXCollections.observableArrayList("No ports available"));
                    cbSelectPorts.setDisable(true);
                    cbSelectPorts.getSelectionModel().selectFirst();
                    updateConnectionLabel("Disconnected - No ports found", "red");
                } else {
                    ObservableList<String> items = FXCollections.observableArrayList(ports);
                    cbSelectPorts.setItems(items);
                    cbSelectPorts.setDisable(false);

                    String connected = smsSender.getConnectedPort();
                    if (connected != null && items.contains(connected)) {
                        cbSelectPorts.getSelectionModel().select(connected);
                        updateConnectionLabel("Connected to " + connected, "green");
                    } else {
                        cbSelectPorts.getSelectionModel().selectFirst();
                        updateConnectionLabel("Disconnected", "orange");
                    }

                    // Re-attach listener (safe even if already attached)
                    cbSelectPorts.getSelectionModel().selectedItemProperty().addListener(portChangeListener);
                }
            } catch (Exception e) {
                cbSelectPorts.setItems(FXCollections.observableArrayList("Error detecting ports"));
                cbSelectPorts.setDisable(true);
                cbSelectPorts.getSelectionModel().selectFirst();
                updateConnectionLabel("Error: " + e.getMessage(), "red");
                e.printStackTrace();
            }
        });
    }

    private final javafx.beans.value.ChangeListener<String> portChangeListener =
            (obs, oldPort, newPort) -> {
                if (newPort != null && !newPort.equals(oldPort) && rbGsm != null && rbGsm.isSelected()) {
                    connectToSelectedPort(newPort);
                }
                updateSendButtonState();
            };

    private void connectToSelectedPort(String portName) {
        if (portName == null || portName.isEmpty() || portName.contains("No") || portName.contains("Error")) {
            return;
        }

        new Thread(() -> {
            boolean success = smsSender.connectToPort(portName, 5000);

            Platform.runLater(() -> {
                if (success) {
                    updateConnectionLabel("Connected to " + portName, "green");
                    AlertDialogManager.showSuccess("Success", "Connected to GSM modem on " + portName);
                } else {
                    updateConnectionLabel("Failed to connect to " + portName, "red");
                    AlertDialogManager.showError("Connection Failed",
                            "Could not connect to GSM modem on " + portName);
                }
                updateSendButtonState();
            });
        }, "GSM-Connect-Thread").start();
    }

    private void updateConnectionLabel(String text, String color) {
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText(text);
            connectionStatusLabel.setStyle("-fx-text-fill: " + color + ";");
        }
    }

    private void updateConnectionStatus() {
        if (connectionStatusLabel == null) return;

        if (rbApi != null && rbApi.isSelected()) {
            connectionStatusLabel.setText("Using SMS API");
            connectionStatusLabel.setStyle("-fx-text-fill: blue;");
        } else if (rbGsm != null && rbGsm.isSelected()) {
            if (smsSender.isConnected()) {
                String port = smsSender.getConnectedPort();
                connectionStatusLabel.setText("Connected to " + (port != null ? port : "unknown port"));
                connectionStatusLabel.setStyle("-fx-text-fill: green;");
            } else {
                connectionStatusLabel.setText("Disconnected - Select a port");
                connectionStatusLabel.setStyle("-fx-text-fill: orange;");
            }
        }
    }

    private void loadSMSLogs() {
        try {
            List<SmsModel> logs = smsService.getAllSMSLogs();
            if (logs != null) {
                logRows.setAll(logs);
                if (adminTable != null) adminTable.setItems(logRows);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupControls() {
        setupTableColumns();
        setupEventHandlers();
        populateComboBoxes();
    }

    private void setupNewsControls() {
        if (newsAiResponse == null) newsAiResponse = new ToggleGroup();

        boolean aiEnabled = newsGeneratorService != null;

        if (aiResponseContainer != null) {
            aiResponseContainer.setVisible(false);
            aiResponseContainer.setManaged(false);
        }

        if (btnUseSelectedNews != null) {
            btnUseSelectedNews.setVisible(false);
            btnUseSelectedNews.setManaged(false);
            btnUseSelectedNews.setDisable(true);
            btnUseSelectedNews.setOnAction(e -> onUseSelectedNews());
        }

        if (btnGenerateNews != null) {
            btnGenerateNews.setDisable(!aiEnabled);
            btnGenerateNews.setOnAction(e -> onGenerateNews());
        }

        if (cbNewsTopic != null) {
            cbNewsTopic.setDisable(!aiEnabled);
        }

        if(btnSaveEvacMessage != null) {
            btnSaveEvacMessage.setOnAction(e -> onSaveEvacMessage());
        }

        updateAiResponseVisibility(false);
    }

    private void setupTableColumns() {
        if (dateSentColumn != null) {
            dateSentColumn.setCellValueFactory(cell -> {
                SmsModel m = cell.getValue();
                return new SimpleStringProperty(m != null && m.getDateSent() != null ? m.getDateSent().toString() : "");
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
            phoneColumn.setCellValueFactory(cell -> {
                SmsModel m = cell.getValue();
                return new SimpleStringProperty(m != null ? m.getPhoneString() : "");
            });
        }

        if (actionsColumn != null) {
            actionsColumn.setCellFactory(col -> new TableCell<SmsModel, Void>() {
                private final Button resendBtn = new Button("Resend");

                {
                    resendBtn.setOnAction(e -> {
                        SmsModel row = getTableRow().getItem();
                        if (row == null) return;

                        resendBtn.setDisable(true);
                        String method = (rbApi != null && rbApi.isSelected()) ? "API" : "GSM";

                        javafx.concurrent.Task<Boolean> task = new javafx.concurrent.Task<>() {
                            @Override
                            protected Boolean call() {
                                return smsService.resendSMS(row, method);
                            }
                        };

                        task.setOnSucceeded(ev -> Platform.runLater(() -> {
                            boolean ok = task.getValue();
                            row.setStatus(ok ? "SENT" : "FAILED");
                            adminTable.refresh();
                            resendBtn.setDisable(ok);
                            AlertDialogManager.showInfo(
                                    ok ? "Success" : "Failed",
                                    ok ? "SMS resent successfully." : "Failed to resend SMS.");
                        }));

                        task.setOnFailed(ev -> Platform.runLater(() -> {
                            row.setStatus("FAILED");
                            adminTable.refresh();
                            resendBtn.setDisable(false);
                            AlertDialogManager.showError("Error", "Resend task failed.");
                        }));

                        new Thread(task).start();
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        return;
                    }

                    SmsModel row = getTableRow().getItem();
                    resendBtn.setDisable("SENT".equalsIgnoreCase(row.getStatus()));
                    setGraphic(resendBtn);
                }
            });
        }
    }

    private void setupEventHandlers() {
        if (btnSendSMS != null) btnSendSMS.setOnAction(e -> onSendSMS());
        if (txtMessage != null) {
            txtMessage.textProperty().addListener((obs, old, newVal) -> {
                int len = (newVal != null) ? newVal.length() : 0;
                if (charCount != null) charCount.setText(len + "/160 characters");
            });
        }
        if (btnRefreshPorts != null) btnRefreshPorts.setOnAction(e -> populateAvailablePorts());
        if (btnRefreshNetwork != null) btnRefreshNetwork.setOnAction(e -> checkNetworkStatus());
    }

    private void checkNetworkStatus() {
        if (lblNetworkStatus == null) return;

        javafx.concurrent.Task<Boolean> task = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() throws Exception {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(5))
                        .build();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.google.com"))
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .timeout(java.time.Duration.ofSeconds(5))
                        .build();
                HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
                return resp.statusCode() >= 200 && resp.statusCode() < 400;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() ->
                updateNetworkLabel(Boolean.TRUE.equals(task.getValue()))));

        task.setOnFailed(e -> Platform.runLater(() -> updateNetworkLabel(false)));

        new Thread(task, "Network-Check-Thread").start();
    }

    private void updateNetworkLabel(boolean online) {
        if (lblNetworkStatus == null) return;
        lblNetworkStatus.setText(online ? "Online" : "Offline - Internet required for AI");
        lblNetworkStatus.setStyle("-fx-text-fill: " + (online ? "#22c55e;" : "#ef4444;"));
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
                    .addListener((obs, old, val) -> onRecipientGroupChanged());
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
            boolean proceed = AlertDialogManager.showConfirmation(
                    "Missing Placeholders",
                    "Your message doesn't contain placeholders\n\n"
                            + "Recommended placeholders: {name}, {evacSite}\n\n"
                            + "Continue saving anyway?",
                    ButtonType.OK,
                    ButtonType.CANCEL
            );
            if (!proceed) {
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

    private void onRecipientGroupChanged() {
        if (isUpdatingComboBox || isOpeningDialog) return;

        String group = (cbSelectBeneficiary != null) ? cbSelectBeneficiary.getValue() : null;
        if (group == null) return;

        if ("By Barangay".equals(group)) {
            showBarangaySelection(true);
            showDisasterSelection(false);
        } else if ("By Disaster Area".equals(group)) {
            showBarangaySelection(false);
            showDisasterSelection(true);
            if (cbSelectDisaster != null && cbSelectDisaster.getItems().isEmpty()) {
                loadDisasters();
            }
        } else if (group.startsWith("Selected Beneficiaries")) {
            showBarangaySelection(false);
            showDisasterSelection(false);
            openBeneficiarySelectionDialog();
        } else {
            showBarangaySelection(false);
            showDisasterSelection(false);
        }

        updateSendButtonState();
    }

    private void showBarangaySelection(boolean show) {
        if (barangaySelectionBox != null) {
            barangaySelectionBox.setVisible(show);
            barangaySelectionBox.setManaged(show);
        }
        if (show && cbSelectBarangay != null && cbSelectBarangay.getItems().isEmpty()) {
            loadBarangayList();
        }
    }

    private void updateEvacMessageStatus(boolean saved, int length) {
        if (lblMessageStatus != null) {
            if (saved) {
                lblMessageStatus.setText("✓ Custom evacuation message saved (" + length + " characters)");
                lblMessageStatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            } else {
                lblMessageStatus.setText("No custom evacuation message set (using default format)");
                lblMessageStatus.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
            }
        }
    }

    private void showDisasterSelection(boolean show) {
        if (disasterSelectionBox != null) {
            disasterSelectionBox.setVisible(show);
            disasterSelectionBox.setManaged(show);
        }
    }

    public void loadDisasters() {
        try {
            List<DisasterModel> disasters = disasterDAO.getAllDisasters();
            if (cbSelectDisaster != null) {
                cbSelectDisaster.getItems().setAll(disasters);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadBarangayList() {
        new Thread(() -> {
            try {
                List<String> barangays = beneficiaryDAO.getAllBarangays();
                Platform.runLater(() -> {
                    if (cbSelectBarangay != null) {
                        cbSelectBarangay.setItems(FXCollections.observableArrayList(barangays));
                        if (barangays.isEmpty()) {
                            AlertDialogManager.showError("No Barangays", "No barangays found.");
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        AlertDialogManager.showError("Error", "Failed to load barangays: " + e.getMessage()));
                e.printStackTrace();
            }
        }, "LoadBarangayThread").start();
    }

    // ────────────────────────────────────────────────
    // Beneficiary Selection Dialog
    // ────────────────────────────────────────────────

    private void openBeneficiarySelectionDialog() {
        if (isOpeningDialog) return;
        isOpeningDialog = true;

        try {
            List<BeneficiaryModel> all = beneficiaryDAO.getAllBeneficiaries();
            if (all == null || all.isEmpty()) {
                AlertDialogManager.showWarning("No Beneficiaries", "Add beneficiaries first.");
                return;
            }

            BeneficiarySelectionDialogController ctrl =
                    DialogManager.getController("selection", BeneficiarySelectionDialogController.class);

            ctrl.resetState();

            if (!ctrl.isLoaded()) {
                ctrl.setBeneficiaries(all);
            }

            if (!selectedBeneficiariesList.isEmpty()) {
                ctrl.setPreselectedBeneficiaries(selectedBeneficiariesList);
            }

            DialogManager.show("selection");

            if (ctrl.isOkClicked()) {
                updateSelectedBeneficiaries(ctrl.getSelectedBeneficiaries());
            } else {
                handleDialogCancelled();
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Dialog Error", "Failed to open selection dialog.");
        } finally {
            isOpeningDialog = false;
            Platform.runLater(() -> isUpdatingComboBox = false);
            updateSendButtonState();
        }
    }

    private void updateSelectedBeneficiaries(List<BeneficiaryModel> selected) {
        isUpdatingComboBox = true;
        try {
            selectedBeneficiariesList = (selected != null) ? new ArrayList<>(selected) : new ArrayList<>();

            String text = String.format("Selected Beneficiaries (%d selected)", selectedBeneficiariesList.size());

            if (cbSelectBeneficiary != null) {
                ObservableList<String> items = cbSelectBeneficiary.getItems();
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i).startsWith("Selected Beneficiaries")) {
                        items.set(i, text);
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

    private void handleDialogCancelled() {
        isUpdatingComboBox = true;
        try {
            if (cbSelectBeneficiary != null) {
                if (selectedBeneficiariesList.isEmpty()) {
                    cbSelectBeneficiary.getSelectionModel().selectFirst();
                } else {
                    String text = String.format("Selected Beneficiaries (%d selected)", selectedBeneficiariesList.size());
                    ObservableList<String> items = cbSelectBeneficiary.getItems();
                    for (int i = 0; i < items.size(); i++) {
                        if (items.get(i).startsWith("Selected Beneficiaries")) {
                            items.set(i, text);
                            cbSelectBeneficiary.getSelectionModel().select(i);
                            break;
                        }
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

    @FXML
    private void onGenerateNews() {
        if (cbNewsTopic == null || btnGenerateNews == null) return;

        if (newsGeneratorService == null) {
            AlertDialogManager.showError(
                    "AI Disabled",
                    "Missing GEMINI_API_KEY environment variable.\nSet GEMINI_API_KEY then restart the app."
            );
            return;
        }

        if (!InternetConnectionChecker.isOnline()) {
            AlertDialogManager.showError("Offline", "Internet required for AI news generation.");
            return;
        }

        String topic = cbNewsTopic.getSelectionModel().getSelectedItem();
        if (topic == null || topic.trim().isEmpty()) {
            AlertDialogManager.showWarning("Topic Required", "Please select a news topic.");
            return;
        }

        storedNewsItems.clear();
        clearNewsSlots();
        updateAiResponseVisibility(false);
        setAiLoading(true);

        newsGeneratorService.generateNewsHeadlines(topic, 5)
                .whenComplete((result, ex) -> Platform.runLater(() -> {
                    setAiLoading(false);

                    if (ex != null) {
                        ex.printStackTrace();
                        AlertDialogManager.showError("AI Error", "News generation failed: " + ex.getMessage());
                        return;
                    }

                    if (result == null || result.isEmpty()) {
                        AlertDialogManager.showWarning("No News", "No headlines generated.");
                        return;
                    }

                    storedNewsItems.clear();
                    storedNewsItems.addAll(result);

                    for (int i = 0; i < Math.min(storedNewsItems.size(), newsSlots.length); i++) {
                        updateNewsSlot(i, storedNewsItems.get(i));
                    }

                    updateAiResponseVisibility(true);

                    AlertDialogManager.showSuccess("Success",
                            storedNewsItems.size() + " news headline(s) generated.");
                }));
    }

    @FXML
    private void onUseSelectedNews() {
        if (newsAiResponse == null || txtMessage == null) return;

        Toggle toggle = newsAiResponse.getSelectedToggle();
        if (toggle == null) return;

        for (int i = 0; i < newsSlots.length; i++) {
            if (newsSlots[i] == toggle && i < storedNewsItems.size()) {
                loadNewsToMessage(i);
                break;
            }
        }
    }

    private void setAiLoading(boolean loading) {
        if (aiLoadingBox != null) {
            aiLoadingBox.setVisible(loading);
            aiLoadingBox.setManaged(loading);
        }
        if (btnGenerateNews != null) btnGenerateNews.setDisable(loading);
        if (cbNewsTopic != null) cbNewsTopic.setDisable(loading);
    }

    private void updateAiResponseVisibility(boolean visible) {
        if (aiResponseContainer != null) {
            aiResponseContainer.setVisible(visible);
            aiResponseContainer.setManaged(visible);
        }
        if (btnUseSelectedNews != null) {
            btnUseSelectedNews.setVisible(visible);
            btnUseSelectedNews.setManaged(visible);
            btnUseSelectedNews.setDisable(!visible);
        }
        for (RadioButton slot : newsSlots) {
            if (slot != null) {
                slot.setVisible(visible);
                slot.setManaged(visible);
            }
        }
        if (!visible && newsAiResponse != null) {
            newsAiResponse.selectToggle(null);
        }
    }

    private void setSmsLoading(boolean loading) {
        if (smsLoadingBox != null) {
            smsLoadingBox.setVisible(loading);
            smsLoadingBox.setManaged(loading);
        }
        if (btnSendSMS != null) btnSendSMS.setDisable(loading);
        if (txtMessage != null) txtMessage.setDisable(loading);
    }

    // ────────────────────────────────────────────────
    // Send SMS Logic
    // ────────────────────────────────────────────────

    @FXML
    private void onSendSMS() {
        if (txtMessage == null || txtMessage.getText().trim().isEmpty()) {
            AlertDialogManager.showError("Empty Message", "Please enter message content.");
            return;
        }

        String group = (cbSelectBeneficiary != null) ? cbSelectBeneficiary.getValue() : null;
        if (group == null) {
            AlertDialogManager.showError("No Group", "Select recipient group.");
            return;
        }

        String method = (rbGsm != null && rbGsm.isSelected()) ? "GSM" : "API";

        List<BeneficiaryModel> recipients = getRecipients(group);
        if (recipients.isEmpty()) {
            AlertDialogManager.showError("No Recipients", "No valid recipients found for this selection.");
            return;
        }

        String msgPreview = txtMessage.getText().substring(0, Math.min(50, txtMessage.getText().length())) + "...";

        boolean confirmed = AlertDialogManager.showConfirmation(
                "Confirm Send",
                "Send to " + recipients.size() + " recipient(s)?",
                ButtonType.OK,
                ButtonType.CANCEL
        );

        if (confirmed) {
            sendSMSToRecipients(recipients, txtMessage.getText(), method);
        }
    }

    private List<BeneficiaryModel> getRecipients(String group) {
        if (group == null) return List.of();

        String base = group;
        if (group.contains("(")) {
            base = group.substring(0, group.indexOf("(")).trim();
        }

        switch (base) {
            case "All Beneficiaries":
                return beneficiaryDAO.getAllBeneficiaries();

            case "By Barangay":
                String brgy = (cbSelectBarangay != null) ? cbSelectBarangay.getValue() : null;
                return (brgy != null) ? beneficiaryDAO.getBeneficiariesByBarangay(brgy) : List.of();

            case "By Disaster Area":
                DisasterModel d = (cbSelectDisaster != null) ? cbSelectDisaster.getValue() : null;
                return (d != null) ? beneficiaryDAO.getBeneficiariesByDisaster(d.getDisasterId()) : List.of();

            default:
                if (base.startsWith("Selected Beneficiaries")) {
                    return new ArrayList<>(selectedBeneficiariesList);
                }
                return List.of();
        }
    }

    private void sendSMSToRecipients(List<BeneficiaryModel> recipients, String message, String method) {
        setSmsLoading(true);

        MainFrameController main = MainFrameController.getInstance();
        int total = recipients.size();

        if (main != null) {
            main.showSmsProgress("Sending SMS (" + method + ")", total);
            main.setSmsCount(0, total);
        }

        SmsServiceImpl impl = (smsService instanceof SmsServiceImpl) ? (SmsServiceImpl) smsService : null;

        if (impl != null) {
            impl.setBulkProgressListener(new BulkProgressListener() {
                @Override
                public void onProgress(int done, int total, int successCount, String m) {
                    Platform.runLater(() -> {
                        MainFrameController mf = MainFrameController.getInstance();
                        if (mf != null) mf.setSmsCount(done, total);
                    });
                }

                @Override
                public void onFinished(int total, int successCount, String m) {
                    Platform.runLater(() -> {
                        setSmsLoading(false);

                        MainFrameController mf = MainFrameController.getInstance();
                        if (mf != null) mf.hideSmsProgress();

                        AlertDialogManager.showSuccess(
                                "Send Complete",
                                successCount + " of " + total + " messages sent. (" + m + ")"
                        );
                        loadSMSLogs();
                    });
                }
            });
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                smsService.sendBulkSMS(recipients, message, method);
                return null;
            }
        };

        task.setOnFailed(ev -> Platform.runLater(() -> {
            setSmsLoading(false);

            MainFrameController mf = MainFrameController.getInstance();
            if (mf != null) mf.hideSmsProgress();

            Throwable ex = task.getException();
            AlertDialogManager.showError("Send Failed", ex != null ? ex.getMessage() : "Unknown error");
            loadSMSLogs();
        }));

        Thread t = new Thread(task, "SMS-Bulk-Thread");
        t.setDaemon(true);
        t.start();
    }

    private void updateSendButtonState() {
        boolean enabled = false;

        if (rbApi != null && rbApi.isSelected()) {
            enabled = true;
        } else if (rbGsm != null && rbGsm.isSelected()) {
            String port = (cbSelectPorts != null) ? cbSelectPorts.getSelectionModel().getSelectedItem() : null;
            boolean validPort = port != null && !port.contains("No") && !port.contains("Error");
            enabled = validPort && smsSender.isConnected();
        }

        if (btnSendSMS != null) {
            btnSendSMS.setDisable(!enabled);
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
            updateConnectionLabel("Disconnected", "orange");
            AlertDialogManager.showInfo("Disconnected", "GSM modem disconnected.");
            updateSendButtonState();
        }
    }
}
