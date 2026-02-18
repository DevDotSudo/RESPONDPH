package com.ionres.respondph.sendsms;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.common.interfaces.BulkProgressListener;
import com.ionres.respondph.common.services.NewsGeneratorService;
import com.ionres.respondph.common.services.NewsGeneratorService.NewsItem;
import com.ionres.respondph.main.MainFrameController;
import com.ionres.respondph.sendsms.dialogs_controller.BeneficiarySelectionDialogController;
import com.ionres.respondph.util.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

public class SendSMSController implements Initializable {

    @FXML private ComboBox<String> cbSelectBeneficiary;
    @FXML private ComboBox<String> cbSelectPorts;
    @FXML private ComboBox<String> cbSelectBarangay;
    @FXML private HBox             barangaySelectionBox;
    @FXML private TextArea         txtMessage;
    @FXML private Label            charCount;
    @FXML private Button           btnSendSMS;

    @FXML private TableView<SmsModel>            adminTable;
    @FXML private TableColumn<SmsModel, String>  dateSentColumn;
    @FXML private TableColumn<SmsModel, String>  fullNameColumn;
    @FXML private TableColumn<SmsModel, String>  msgColumn;
    @FXML private TableColumn<SmsModel, String>  statusColumn;
    @FXML private TableColumn<SmsModel, String>  phoneColumn;
    @FXML private TableColumn<SmsModel, Void>    actionsColumn;

    @FXML private RadioButton rbGsm;
    @FXML private RadioButton rbApi;
    @FXML private Label       connectionStatusLabel;
    @FXML private Button      btnRefreshPorts;

    @FXML private HBox                    disasterSelectionBox;
    @FXML private ComboBox<DisasterModel> cbSelectDisaster;

    @FXML private ComboBox<String> cbNewsTopic;
    @FXML private Button           btnGenerateNews;
    @FXML private VBox             aiResponseContainer;
    @FXML private HBox             aiResponseActions;
    @FXML private Button           btnUseSelectedNews;
    @FXML private ToggleGroup      newsAiResponse;

    @FXML private Label  lblNetworkStatus;
    @FXML private Button btnRefreshNetwork;

    @FXML private HBox              smsLoadingBox;
    @FXML private ProgressIndicator piSms;

    @FXML private RadioButton rbNewsSlot1;
    @FXML private RadioButton rbNewsSlot2;
    @FXML private RadioButton rbNewsSlot3;
    @FXML private RadioButton rbNewsSlot4;
    @FXML private RadioButton rbNewsSlot5;

    @FXML private TextArea txtCustomEvacMessage;
    @FXML private Button   btnSaveEvacMessage;
    @FXML private Label    lblMessageStatus;

    // ── State ─────────────────────────────────────────────────────────────────
    private List<BeneficiaryModel> selectedBeneficiariesList = new ArrayList<>();
    private final List<NewsItem>   storedNewsItems           = new ArrayList<>();
    private RadioButton[]          newsSlots;

    /** Guard: prevents starting a second generation while one is in flight. */
    private final AtomicBoolean generationInProgress = new AtomicBoolean(false);

    private final ObservableList<SmsModel> logRows = FXCollections.observableArrayList();

    // ── Services / DAOs ───────────────────────────────────────────────────────
    private CustomEvacMessageManager evacMessageManager;
    private SmsService               smsService;
    private SMSSender                smsSender;
    private NewsGeneratorService     newsGeneratorService;
    private BeneficiaryDAO           beneficiaryDAO;
    private DisasterDAO              disasterDAO;

    // ── Dialog-open guard ─────────────────────────────────────────────────────
    private boolean isOpeningDialog    = false;
    private boolean isUpdatingComboBox = false;

    private final javafx.beans.value.ChangeListener<String> portChangeListener =
            (obs, oldPort, newPort) -> {
                if (newPort != null && !newPort.equals(oldPort) && rbGsm != null && rbGsm.isSelected())
                    connectToSelectedPort(newPort);
                updateSendButtonState();
            };

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        smsService = new SmsServiceImpl();
        smsSender  = SMSSender.getInstance();

        try {
            newsGeneratorService = new NewsGeneratorService();
        } catch (Exception e) {
            newsGeneratorService = null;
            System.err.println("[AI] Disabled: " + e.getMessage());
        }

        beneficiaryDAO = new BeneficiaryDAOImpl();
        disasterDAO    = new DisasterDAOImpl();

        newsSlots = new RadioButton[]{
                rbNewsSlot1, rbNewsSlot2, rbNewsSlot3, rbNewsSlot4, rbNewsSlot5
        };

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

        if (charCount != null) charCount.setText("0/320 characters");

        updateConnectionStatus();
        loadExistingEvacMessage();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AI SLOT LAYOUT
    // ─────────────────────────────────────────────────────────────────────────

    private void bindAiSlotWidths() {
        if (aiResponseContainer == null) return;
        aiResponseContainer.setFillWidth(true);
        double paddingLR = 40;
        for (RadioButton rb : newsSlots) {
            if (rb == null) continue;
            rb.setMaxWidth(Double.MAX_VALUE);
            rb.setMinWidth(0);
            rb.prefWidthProperty().bind(aiResponseContainer.widthProperty().subtract(paddingLR));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NEWS SLOTS
    // ─────────────────────────────────────────────────────────────────────────

    private void setupNewsSlots() {
        if (newsAiResponse == null) newsAiResponse = new ToggleGroup();

        for (int i = 0; i < newsSlots.length; i++) {
            final int index = i;
            RadioButton slot = newsSlots[i];
            if (slot == null) continue;

            slot.setToggleGroup(newsAiResponse);
            slot.getStyleClass().add("ai-news-slot");
            slot.setWrapText(true);
            slot.setAlignment(Pos.TOP_LEFT);
            slot.setTextOverrun(OverrunStyle.CLIP);
            slot.setMinHeight(Region.USE_PREF_SIZE);
            slot.setPrefHeight(Region.USE_COMPUTED_SIZE);
            slot.setMaxHeight(Double.MAX_VALUE);
            slot.setDisable(true);

            slot.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected && index < storedNewsItems.size())
                    loadNewsToMessage(index);
            });
        }
    }

    /**
     * Populates slot {@code slotIndex} with the given news item.
     * The SMS text is shown as a label and the URL as a double-click hyperlink.
     */
    private void updateNewsSlot(int slotIndex, NewsItem item) {
        if (slotIndex < 0 || slotIndex >= newsSlots.length) return;
        RadioButton slot = newsSlots[slotIndex];
        if (slot == null || item == null) return;

        double leftIndent = 38;

        Label sms = new Label(item.smsText());
        sms.setWrapText(true);
        sms.setMaxWidth(Double.MAX_VALUE);

        Hyperlink link = new Hyperlink(item.url());
        link.setWrapText(true);
        link.setMaxWidth(Double.MAX_VALUE);
        link.setOnMouseClicked(ev -> { if (ev.getClickCount() == 2) openInBrowser(item.url()); });

        VBox box = new VBox(6, sms, link);
        box.setFillWidth(true);
        box.setMaxWidth(Double.MAX_VALUE);
        box.prefWidthProperty().bind(slot.widthProperty().subtract(leftIndent));
        sms.prefWidthProperty().bind(box.prefWidthProperty());
        link.prefWidthProperty().bind(box.prefWidthProperty());

        slot.setText("");
        slot.setGraphic(box);
        slot.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        slot.setDisable(false);

        // Auto-select the first slot so its text pre-fills the message box
        if (slotIndex == 0) slot.setSelected(true);
    }

    /** Resets all 5 radio-button slots to their empty placeholder state. */
    private void clearNewsSlots() {
        for (int i = 0; i < newsSlots.length; i++) {
            RadioButton slot = newsSlots[i];
            if (slot != null) {
                slot.setGraphic(null);
                slot.setContentDisplay(ContentDisplay.LEFT);
                slot.setText("Slot " + (i + 1) + " (Empty)");
                slot.setDisable(true);
                slot.setSelected(false);
            }
        }
    }

    private void loadNewsToMessage(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= storedNewsItems.size() || txtMessage == null) return;
        String smsText = storedNewsItems.get(slotIndex).smsText();
        if (smsText != null) txtMessage.setText(smsText.trim());
    }

    private void openInBrowser(String url) {
        if (url == null || url.isBlank()) return;
        try {
            if (Desktop.isDesktopSupported())
                Desktop.getDesktop().browse(URI.create(url));
            else
                AlertDialogManager.showWarning("Not Supported", "Cannot open browser on this system.");
        } catch (Exception e) {
            AlertDialogManager.showError("Open Link Failed",
                    "Could not open:\n" + url + "\n\n" + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RADIO BUTTONS / GSM CONNECTION
    // ─────────────────────────────────────────────────────────────────────────

    private void setupRadioButtons() {
        if (rbApi != null) {
            rbApi.selectedProperty().addListener((obs, old, selected) -> {
                if (cbSelectPorts   != null) cbSelectPorts.setDisable(selected);
                if (btnRefreshPorts != null) btnRefreshPorts.setDisable(selected);
                updateSendButtonState();
                updateConnectionStatus();
            });
        }
        if (rbGsm != null) {
            rbGsm.selectedProperty().addListener((obs, old, selected) -> {
                if (!selected) return;
                String port = (cbSelectPorts != null)
                        ? cbSelectPorts.getSelectionModel().getSelectedItem() : null;
                if (port != null && !port.contains("No") && !port.contains("Error"))
                    connectToSelectedPort(port);
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
                    cbSelectPorts.getSelectionModel().selectedItemProperty()
                            .addListener(portChangeListener);
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

    private void connectToSelectedPort(String portName) {
        if (portName == null || portName.isEmpty()
                || portName.contains("No") || portName.contains("Error")) return;

        new Thread(() -> {
            boolean ok = smsSender.connectToPort(portName, 5000);
            Platform.runLater(() -> {
                if (ok) {
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

    // ─────────────────────────────────────────────────────────────────────────
    // TABLE / SMS LOGS
    // ─────────────────────────────────────────────────────────────────────────

    private void loadSMSLogs() {
        try {
            List<SmsModel> logs = smsService.getAllSMSLogs();
            if (logs != null) {
                logRows.setAll(logs);
                if (adminTable != null) adminTable.setItems(logRows);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupControls() {
        setupTableColumns();
        setupEventHandlers();
        populateComboBoxes();
    }

    private void setupTableColumns() {
        if (dateSentColumn != null)
            dateSentColumn.setCellValueFactory(cell -> {
                SmsModel m = cell.getValue();
                return new SimpleStringProperty(
                        m != null && m.getDateSent() != null ? m.getDateSent().toString() : "");
            });
        if (fullNameColumn != null) fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullname"));
        if (msgColumn      != null) msgColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        if (statusColumn   != null) statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        if (phoneColumn    != null)
            phoneColumn.setCellValueFactory(cell -> {
                SmsModel m = cell.getValue();
                return new SimpleStringProperty(m != null ? m.getPhoneString() : "");
            });

        if (actionsColumn != null) {
            actionsColumn.setCellFactory(col -> new TableCell<>() {
                private final Button resendBtn = new Button("Resend");
                {
                    resendBtn.setOnAction(e -> {
                        SmsModel row = getTableRow().getItem();
                        if (row == null) return;
                        resendBtn.setDisable(true);
                        String method = (rbApi != null && rbApi.isSelected()) ? "API" : "GSM";

                        Task<Boolean> task = new Task<>() {
                            @Override protected Boolean call() {
                                return smsService.resendSMS(row, method);
                            }
                        };
                        task.setOnSucceeded(ev -> Platform.runLater(() -> {
                            boolean ok = task.getValue();
                            row.setStatus(ok ? "SENT" : "FAILED");
                            adminTable.refresh();
                            resendBtn.setDisable(ok);
                            AlertDialogManager.showInfo(ok ? "Success" : "Failed",
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
                        setGraphic(null); return;
                    }
                    SmsModel row = getTableRow().getItem();
                    resendBtn.setDisable("SENT".equalsIgnoreCase(row.getStatus()));
                    setGraphic(resendBtn);
                }
            });
        }
    }

    private void setupEventHandlers() {
        if (btnSendSMS  != null) btnSendSMS.setOnAction(e -> onSendSMS());
        if (txtMessage  != null) txtMessage.textProperty().addListener((obs, old, val) -> {
            int len = (val != null) ? val.length() : 0;
            if (charCount != null) charCount.setText(len + "/320 characters");
        });
        if (btnRefreshPorts   != null) btnRefreshPorts.setOnAction(e -> populateAvailablePorts());
        if (btnRefreshNetwork != null) btnRefreshNetwork.setOnAction(e -> checkNetworkStatus());
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

    // ─────────────────────────────────────────────────────────────────────────
    // NEWS CONTROLS SETUP
    // ─────────────────────────────────────────────────────────────────────────

    private void setupNewsControls() {
        if (newsAiResponse == null) newsAiResponse = new ToggleGroup();

        boolean aiEnabled = newsGeneratorService != null;

        // Start collapsed — shown after successful generation
        updateAiResponseVisibility(false);

        if (btnUseSelectedNews != null) {
            btnUseSelectedNews.setOnAction(e -> onUseSelectedNews());
        }
        if (btnGenerateNews != null) {
            btnGenerateNews.setDisable(!aiEnabled);
            btnGenerateNews.setOnAction(e -> onGenerateNews());
        }
        if (cbNewsTopic != null) {
            cbNewsTopic.setDisable(!aiEnabled);
        }
        if (btnSaveEvacMessage != null) {
            btnSaveEvacMessage.setOnAction(e -> onSaveEvacMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NETWORK STATUS
    // ─────────────────────────────────────────────────────────────────────────

    private void checkNetworkStatus() {
        if (lblNetworkStatus == null) return;
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() throws Exception {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(5)).build();
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

    // ─────────────────────────────────────────────────────────────────────────
    // CUSTOM EVAC MESSAGE
    // ─────────────────────────────────────────────────────────────────────────

    private void loadExistingEvacMessage() {
        if (txtCustomEvacMessage == null) return;
        if (evacMessageManager.hasCustomMessage()) {
            String msg = evacMessageManager.getCustomEvacuationMessage();
            txtCustomEvacMessage.setText(msg);
            updateEvacMessageStatus(true, msg.length());
        } else {
            txtCustomEvacMessage.clear();
            updateEvacMessageStatus(false, 0);
        }
    }

    @FXML private void onSaveEvacMessage() {
        String message = txtCustomEvacMessage != null ? txtCustomEvacMessage.getText() : null;
        if (message == null || message.trim().isEmpty()) {
            AlertDialogManager.showWarning("Empty Message",
                    "Please enter a custom evacuation message before saving.");
            return;
        }
        if (message.length() > 320) {
            AlertDialogManager.showWarning("Message Too Long",
                    "Message is " + message.length() + " characters. "
                            + "SMS messages are limited to 320 characters and may be split.");
        }
        if (!message.contains("{name}") && !message.contains("{evacSite}") && !message.contains("{site}")) {
            boolean proceed = AlertDialogManager.showConfirmation(
                    "Missing Placeholders",
                    "Your message doesn't contain placeholders.\n\n"
                            + "Recommended: {name}, {evacSite}\n\nContinue saving anyway?",
                    ButtonType.OK, ButtonType.CANCEL
            );
            if (!proceed) return;
        }
        evacMessageManager.setCustomEvacuationMessage(message.trim());
        updateEvacMessageStatus(true, message.trim().length());
        AlertDialogManager.showSuccess("Message Saved",
                "Custom evacuation message saved successfully.\n"
                        + "It will be used when allocating beneficiaries to evacuation sites.");
    }

    private void updateEvacMessageStatus(boolean saved, int length) {
        if (lblMessageStatus == null) return;
        if (saved) {
            lblMessageStatus.setText("✓ Custom evacuation message saved (" + length + " characters)");
            lblMessageStatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else {
            lblMessageStatus.setText("No custom evacuation message set (using default format)");
            lblMessageStatus.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RECIPIENT GROUP SELECTION
    // ─────────────────────────────────────────────────────────────────────────

    private void onRecipientGroupChanged() {
        if (isUpdatingComboBox || isOpeningDialog) return;
        String group = (cbSelectBeneficiary != null) ? cbSelectBeneficiary.getValue() : null;
        if (group == null) return;

        switch (group) {
            case "By Barangay" -> {
                showBarangaySelection(true);
                showDisasterSelection(false);
            }
            case "By Disaster Area" -> {
                showBarangaySelection(false);
                showDisasterSelection(true);
                if (cbSelectDisaster != null && cbSelectDisaster.getItems().isEmpty()) loadDisasters();
            }
            default -> {
                showBarangaySelection(false);
                showDisasterSelection(false);
                if (group.startsWith("Selected Beneficiaries")) openBeneficiarySelectionDialog();
            }
        }
        updateSendButtonState();
    }

    private void showBarangaySelection(boolean show) {
        if (barangaySelectionBox != null) {
            barangaySelectionBox.setVisible(show);
            barangaySelectionBox.setManaged(show);
        }
        if (show && cbSelectBarangay != null && cbSelectBarangay.getItems().isEmpty())
            loadBarangayList();
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
            if (cbSelectDisaster != null) cbSelectDisaster.getItems().setAll(disasters);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void loadBarangayList() {
        new Thread(() -> {
            try {
                List<String> barangays = beneficiaryDAO.getAllBarangays();
                Platform.runLater(() -> {
                    if (cbSelectBarangay != null) {
                        cbSelectBarangay.setItems(FXCollections.observableArrayList(barangays));
                        if (barangays.isEmpty())
                            AlertDialogManager.showError("No Barangays", "No barangays found.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        AlertDialogManager.showError("Error", "Failed to load barangays: " + e.getMessage()));
                e.printStackTrace();
            }
        }, "LoadBarangayThread").start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BENEFICIARY SELECTION DIALOG
    // ─────────────────────────────────────────────────────────────────────────

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
            if (!ctrl.isLoaded()) ctrl.setBeneficiaries(all);
            if (!selectedBeneficiariesList.isEmpty())
                ctrl.setPreselectedBeneficiaries(selectedBeneficiariesList);

            DialogManager.show("selection");

            if (ctrl.isOkClicked()) updateSelectedBeneficiaries(ctrl.getSelectedBeneficiaries());
            else                    handleDialogCancelled();
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
            String text = "Selected Beneficiaries (%d selected)".formatted(selectedBeneficiariesList.size());
            updateComboEntry(text);
        } finally {
            Platform.runLater(() -> { isUpdatingComboBox = false; updateSendButtonState(); });
        }
    }

    private void handleDialogCancelled() {
        isUpdatingComboBox = true;
        try {
            if (selectedBeneficiariesList.isEmpty()) {
                if (cbSelectBeneficiary != null) cbSelectBeneficiary.getSelectionModel().selectFirst();
            } else {
                updateComboEntry("Selected Beneficiaries (%d selected)".formatted(selectedBeneficiariesList.size()));
            }
        } finally {
            Platform.runLater(() -> { isUpdatingComboBox = false; updateSendButtonState(); });
        }
    }

    /** Updates the "Selected Beneficiaries" entry text in the combo box. */
    private void updateComboEntry(String text) {
        if (cbSelectBeneficiary == null) return;
        ObservableList<String> items = cbSelectBeneficiary.getItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).startsWith("Selected Beneficiaries")) {
                items.set(i, text);
                cbSelectBeneficiary.getSelectionModel().select(i);
                return;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AI NEWS GENERATION
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void onGenerateNews() {
        if (cbNewsTopic == null || btnGenerateNews == null) return;

        // Guard against double-clicks / re-entry
        if (!generationInProgress.compareAndSet(false, true)) return;

        if (newsGeneratorService == null) {
            generationInProgress.set(false);
            AlertDialogManager.showError("AI Disabled",
                    "Missing GEMINI_API_KEY.\nSet the environment variable and restart.");
            return;
        }
        if (!InternetConnectionChecker.isOnline()) {
            generationInProgress.set(false);
            AlertDialogManager.showError("Offline", "Internet connection required for AI news generation.");
            return;
        }

        String topic = cbNewsTopic.getSelectionModel().getSelectedItem();
        if (topic == null || topic.trim().isEmpty()) {
            generationInProgress.set(false);
            AlertDialogManager.showWarning("Topic Required", "Please select a news topic.");
            return;
        }

        // ── Reset UI ──────────────────────────────────────────────────────────
        storedNewsItems.clear();
        clearNewsSlots();
        updateAiResponseVisibility(false);

        MainFrameController main = MainFrameController.getInstance();
        if (main != null) {
            main.setNewsCancelAction(() -> newsGeneratorService.cancelCurrentGeneration());
            main.showNewsProgress(topic);
        }

        newsGeneratorService
                .generateNewsHeadlines(topic, (progress, status) ->
                        Platform.runLater(() -> { if (main != null) main.setNewsProgress(progress, status); }))
                .whenComplete((result, ex) -> Platform.runLater(() -> {

                    // ── Always restore UI ─────────────────────────────────────────
                    generationInProgress.set(false);

                    if (main != null) {
                        main.setNewsCancelAction(null);
                        main.hideNewsProgress();
                    }

                    // ── Error path ────────────────────────────────────────────────
                    if (ex != null) {
                        ex.printStackTrace();
                        AlertDialogManager.showError("AI Error",
                                "News generation failed:\n" + ex.getCause() != null
                                        ? ex.getCause().getMessage()
                                        : ex.getMessage());
                        return;
                    }

                    // ── Empty = cancelled or zero results ─────────────────────────
                    // (toast already showed "Cancelled." — no extra dialog needed)
                    if (result == null || result.isEmpty()) return;

                    // ── Populate slots ────────────────────────────────────────────
                    storedNewsItems.addAll(result);
                    int n = Math.min(storedNewsItems.size(), newsSlots.length);
                    for (int i = 0; i < n; i++) updateNewsSlot(i, storedNewsItems.get(i));

                    updateAiResponseVisibility(true);

                    // Only alert if we got fewer than TARGET (5)
                    if (n < 5) {
                        AlertDialogManager.showInfo("Partial Results",
                                "Generated " + n + " of 5 news items.\n"
                                        + "Some articles could not be verified or found.");
                    } else {
                        AlertDialogManager.showSuccess("Success", "5 verified news items generated.");
                    }
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
        // Show/hide individual slots so the layout collapses cleanly
        for (RadioButton slot : newsSlots) {
            if (slot == null) continue;
            slot.setVisible(visible);
            slot.setManaged(visible);
        }
        if (!visible && newsAiResponse != null) newsAiResponse.selectToggle(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SMS LOADING UI
    // ─────────────────────────────────────────────────────────────────────────

    private void setSmsLoading(boolean loading) {
        if (smsLoadingBox != null) {
            smsLoadingBox.setVisible(loading);
            smsLoadingBox.setManaged(loading);
        }
        if (btnSendSMS  != null) btnSendSMS.setDisable(loading);
        if (txtMessage  != null) txtMessage.setDisable(loading);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEND SMS
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void onSendSMS() {
        if (txtMessage == null || txtMessage.getText().trim().isEmpty()) {
            AlertDialogManager.showError("Empty Message", "Please enter message content.");
            return;
        }
        String group = (cbSelectBeneficiary != null) ? cbSelectBeneficiary.getValue() : null;
        if (group == null) {
            AlertDialogManager.showError("No Group", "Select a recipient group.");
            return;
        }
        String method = (rbGsm != null && rbGsm.isSelected()) ? "GSM" : "API";
        List<BeneficiaryModel> recipients = getRecipients(group);
        if (recipients.isEmpty()) {
            AlertDialogManager.showError("No Recipients",
                    "No valid recipients found for this selection.");
            return;
        }
        boolean confirmed = AlertDialogManager.showConfirmation(
                "Confirm Send",
                "Send to " + recipients.size() + " recipient(s) via " + method + "?",
                ButtonType.OK, ButtonType.CANCEL
        );
        if (confirmed) sendSMSToRecipients(recipients, txtMessage.getText(), method);
    }

    private List<BeneficiaryModel> getRecipients(String group) {
        if (group == null) return List.of();
        // Strip the count suffix added to "Selected Beneficiaries (N selected)"
        String base = group.contains("(") ? group.substring(0, group.indexOf("(")).trim() : group;
        return switch (base) {
            case "All Beneficiaries"       -> beneficiaryDAO.getAllBeneficiaries();
            case "By Barangay" -> {
                String brgy = (cbSelectBarangay != null) ? cbSelectBarangay.getValue() : null;
                yield brgy != null ? beneficiaryDAO.getBeneficiariesByBarangay(brgy) : List.of();
            }
            case "By Disaster Area" -> {
                DisasterModel d = (cbSelectDisaster != null) ? cbSelectDisaster.getValue() : null;
                yield d != null ? beneficiaryDAO.getBeneficiariesByDisaster(d.getDisasterId()) : List.of();
            }
            default -> base.startsWith("Selected Beneficiaries")
                    ? new ArrayList<>(selectedBeneficiariesList)
                    : List.of();
        };
    }

    private void sendSMSToRecipients(List<BeneficiaryModel> recipients, String message, String method) {
        setSmsLoading(true);
        int total = recipients.size();

        MainFrameController main = MainFrameController.getInstance();
        if (main != null) {
            main.showSmsProgress("Sending SMS (" + method + ")", total);
            main.setSmsCount(0, total);
        }

        if (smsService instanceof SmsServiceImpl impl) {
            impl.setBulkProgressListener(new BulkProgressListener() {
                @Override
                public void onProgress(int done, int tot, int successCount, String m) {
                    Platform.runLater(() -> {
                        if (main != null) main.setSmsCount(done, tot);
                    });
                }
                @Override
                public void onFinished(int tot, int successCount, String m) {
                    Platform.runLater(() -> {
                        setSmsLoading(false);
                        if (main != null) main.hideSmsProgress();
                        AlertDialogManager.showSuccess("Send Complete",
                                successCount + " of " + tot + " messages sent. (" + m + ")");
                        loadSMSLogs();
                    });
                }
            });
        }

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                smsService.sendBulkSMS(recipients, message, method);
                return null;
            }
        };
        task.setOnFailed(ev -> Platform.runLater(() -> {
            setSmsLoading(false);
            if (main != null) main.hideSmsProgress();
            Throwable ex = task.getException();
            AlertDialogManager.showError("Send Failed",
                    ex != null ? ex.getMessage() : "Unknown error");
            loadSMSLogs();
        }));

        Thread t = new Thread(task, "SMS-Bulk-Thread");
        t.setDaemon(true);
        t.start();
    }

    private void updateSendButtonState() {
        boolean enabled;
        if (rbApi != null && rbApi.isSelected()) {
            enabled = true;
        } else if (rbGsm != null && rbGsm.isSelected()) {
            String port = (cbSelectPorts != null)
                    ? cbSelectPorts.getSelectionModel().getSelectedItem() : null;
            enabled = port != null && !port.contains("No") && !port.contains("Error")
                    && smsSender.isConnected();
        } else {
            enabled = false;
        }
        if (btnSendSMS != null) btnSendSMS.setDisable(!enabled);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FXML-bound refresh handlers (wired via FXML onAction or setupEventHandlers)
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void onRefreshPorts()   { populateAvailablePorts(); }
    @FXML private void onDisconnect() {
        if (smsSender != null && smsSender.isConnected()) {
            smsSender.disconnect();
            updateConnectionLabel("Disconnected", "orange");
            AlertDialogManager.showInfo("Disconnected", "GSM modem disconnected.");
            updateSendButtonState();
        }
    }
}