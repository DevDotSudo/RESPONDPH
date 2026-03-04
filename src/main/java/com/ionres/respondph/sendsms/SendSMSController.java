package com.ionres.respondph.sendsms;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.common.interfaces.BulkProgressListener;
import com.ionres.respondph.common.services.NewsGeneratorService;
import com.ionres.respondph.common.services.NewsItem;
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
import com.ionres.respondph.disaster_mapping.DisasterMappingService;
import com.ionres.respondph.disaster_mapping.DisasterMappingServiceImpl;
import com.ionres.respondph.common.model.DisasterCircleInfo;
import com.ionres.respondph.database.DBConnection;
import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SendSMSController implements Initializable {

    private static final Logger LOG = Logger.getLogger(SendSMSController.class.getName());

    private static final Set<String> NATIONAL_CATEGORIES = Set.of(
            "national news", "politics news", "health news", "crime / law / public safety news"
    );

    private static final Set<String> WEATHER_CATEGORIES = Set.of(
            "weather news", "weather", "weather update"
    );

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
    @FXML private Button      btnDisconnect;
    @FXML private TextArea    txtCustomEvacMessage;
    @FXML private Button      btnSaveEvacMessage;
    @FXML private Label       lblMessageStatus;
    private DisasterMappingService disasterMappingService;
    private List<BeneficiaryModel> selectedBeneficiariesList = new ArrayList<>();
    private final List<NewsItem>   storedNewsItems           = new ArrayList<>();
    private RadioButton[]          newsSlots;
    private final AtomicBoolean generationInProgress = new AtomicBoolean(false);
    private final ObservableList<SmsModel> logRows   = FXCollections.observableArrayList();
    private CustomEvacMessageManager evacMessageManager;
    private SmsService               smsService;
    private SMSSender                smsSender;
    private NewsGeneratorService     newsGeneratorService;
    private BeneficiaryDAO           beneficiaryDAO;
    private DisasterDAO              disasterDAO;
    private boolean isOpeningDialog    = false;
    private boolean isUpdatingComboBox = false;
    private ScheduledExecutorService networkPoller;
    private ScheduledFuture<?>       networkPollFuture;
    private volatile boolean lastKnownOnline = true;
    private static final int NETWORK_POLL_INTERVAL_SEC = 15;

    private final javafx.beans.value.ChangeListener<String> portChangeListener =
            (obs, oldPort, newPort) -> {
                if (newPort == null || newPort.equals(oldPort)) return;
                if (newPort.contains("No") || newPort.contains("Error")) {
                    updateConnectionLabel("No valid port selected", "red");
                    updateDisconnectButtonVisibility();
                    updateSendButtonState();
                    return;
                }
                autoConnectToPort(newPort);
            };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        smsService = new SmsServiceImpl();
        smsSender  = SMSSender.getInstance();
        disasterMappingService = new DisasterMappingServiceImpl(DBConnection.getInstance());

        // ── Initialise NewsGeneratorService ───────────────────────────────────
        // The service reads ANTHROPIC_API_KEY and GOOGLE_TRANSLATE_API_KEY from env.
        // If ANTHROPIC_API_KEY is missing the constructor throws; we catch that and
        // disable the AI news feature gracefully.
        try {
            newsGeneratorService = new NewsGeneratorService();
            LOG.info("[SendSMS] NewsGeneratorService ready (pipeline: RSS → Claude EN → Google Translate HIL).");
        } catch (Exception e) {
            newsGeneratorService = null;
            LOG.warning("[SendSMS] AI news disabled: " + e.getMessage());
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

        // Start the background network poller — fires immediately, then every
        // NETWORK_POLL_INTERVAL_SEC seconds.  Replaces the old one-shot
        // checkNetworkStatus() call so the label stays current automatically.
        startNetworkPoller();

        Refresher.registerDisasterAndBeneficiaryCombo(this);

        evacMessageManager = CustomEvacMessageManager.getInstance();
        if (charCount != null) charCount.setText("0/320 characters");

        updateConnectionStatus();
        loadExistingEvacMessage();
    }

    // =========================================================================
    //  News generation — main handler
    // =========================================================================

    /**
     * Called when the user clicks "Generate News".
     *
     * Flow:
     *   1. Validates state (service ready, online, topic selected).
     *   2. Calls NewsGeneratorService.generateNewsHeadlines(topic, category, progress).
     *      Inside that call:
     *        • Step 1 — fetches RSS articles matching the category/geo scope
     *        • Step 2 — Claude reads articles and writes English SMS summaries
     *        • Step 3 — Google Translate converts each summary to Hiligaynon
     *   3. Displays results in the 5 radio-button news slots.
     */
    @FXML
    private void onGenerateNews() {
        if (cbNewsTopic == null || btnGenerateNews == null) return;

        // Guard: prevent concurrent generation
        if (!generationInProgress.compareAndSet(false, true)) {
            LOG.info("[SendSMS] Generation already in progress — ignoring click.");
            return;
        }

        // Guard: service available
        if (newsGeneratorService == null) {
            generationInProgress.set(false);
            AlertDialogManager.showError("AI Disabled",
                    "News generation requires ANTHROPIC_API_KEY.\n"
                            + "Set the environment variable and restart the application.");
            return;
        }

        // Guard: internet — check the cached flag first (instant, no blocking call on FX thread),
        // then fall back to a live probe so we never start with a stale "online" state.
        if (!lastKnownOnline || !InternetConnectionChecker.isOnline()) {
            generationInProgress.set(false);
            AlertDialogManager.showError("No Internet",
                    "An internet connection is required for:\n"
                            + "  • Fetching RSS news articles\n"
                            + "  • Claude AI (English summary)\n"
                            + "  • Google Translate (Hiligaynon)");
            return;
        }

        // Category drives both the geographic scope and the keyword filter
        String category = cbNewsTopic.getSelectionModel().getSelectedItem();
        if (category == null || category.trim().isEmpty()) {
            generationInProgress.set(false);
            AlertDialogManager.showWarning("Topic Required", "Please select a news topic.");
            return;
        }

        String scopeLabel = resolveScopeLabel(category);

        LOG.info("[SendSMS] News generation started. category='" + category
                + "' scope='" + scopeLabel + "'");

        // Reset UI
        storedNewsItems.clear();
        clearNewsSlots();
        updateAiResponseVisibility(false);
        btnGenerateNews.setDisable(true);

        // Show progress in main frame
        MainFrameController main = MainFrameController.getInstance();
        if (main != null) {
            main.setNewsCancelAction(() -> {
                LOG.info("[SendSMS] User cancelled news generation.");
                newsGeneratorService.cancel();
            });
            main.showNewsProgress(category);
        }

        // ── Launch pipeline ───────────────────────────────────────────────────
        newsGeneratorService
                .generateNewsHeadlines(
                        category,   // topic
                        category,   // category (scope)
                        (progress, status) -> Platform.runLater(() -> {
                            if (main != null) main.setNewsProgress(progress, status);
                        }))
                .whenComplete((result, ex) -> Platform.runLater(() -> {

                    generationInProgress.set(false);
                    if (btnGenerateNews != null) btnGenerateNews.setDisable(false);
                    if (main != null) {
                        main.setNewsCancelAction(null);
                        main.hideNewsProgress();
                    }

                    // ── Error ─────────────────────────────────────────────────
                    if (ex != null) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        String msg = cause.getMessage();
                        if (msg == null) msg = cause.getClass().getSimpleName();
                        LOG.log(Level.SEVERE, "[SendSMS] News generation failed: " + msg, ex);

                        // Suppress the generic error when the pipeline was
                        // already cancelled due to an internet drop — the
                        // "Internet Connection Lost" alert was already shown.
                        if (!lastKnownOnline) {
                            LOG.info("[SendSMS] Suppressing post-cancel error because connection is offline.");
                            return;
                        }

                        AlertDialogManager.showError("Generation Failed",
                                "News generation encountered an error:\n\n" + msg
                                        + "\n\nCheck that ANTHROPIC_API_KEY and GOOGLE_TRANSLATE_API_KEY are set.");
                        return;
                    }

                    // ── No results ────────────────────────────────────────────
                    if (result == null || result.isEmpty()) {
                        LOG.warning("[SendSMS] Generation returned 0 items.");
                        AlertDialogManager.showWarning("No Results",
                                "No Hiligaynon news items could be generated for:\n"
                                        + "  Category : " + category + "\n"
                                        + "  Scope    : " + scopeLabel + "\n\n"
                                        + "Possible causes:\n"
                                        + "  • RSS feeds returned no matching articles\n"
                                        + "  • Google Translate API key is missing or invalid\n"
                                        + "Try a different topic or check your API keys.");
                        return;
                    }

                    LOG.info("[SendSMS] Pipeline complete. Items: " + result.size()
                            + " (in Hiligaynon, scope: " + scopeLabel + ")");

                    // ── Populate news slots ───────────────────────────────────
                    storedNewsItems.addAll(result);
                    int n = Math.min(storedNewsItems.size(), newsSlots.length);
                    for (int i = 0; i < n; i++)
                        updateNewsSlot(i, storedNewsItems.get(i));
                    // Hide unused slots
                    for (int i = n; i < newsSlots.length; i++) {
                        RadioButton slot = newsSlots[i];
                        if (slot != null) { slot.setVisible(false); slot.setManaged(false); }
                    }

                    updateAiResponseVisibility(true);

                    // ── Success alert ─────────────────────────────────────────
                    if (n == TARGET) {
                        AlertDialogManager.showSuccess("Generation Complete",
                                TARGET + " Hiligaynon news items generated.\n"
                                        + "Category : " + category + "\n"
                                        + "Scope    : " + scopeLabel);
                    } else {
                        AlertDialogManager.showInfo("Partial Results",
                                n + " of " + TARGET + " items generated.\n"
                                        + "Category : " + category + "\n"
                                        + "Scope    : " + scopeLabel + "\n\n"
                                        + "This topic may have limited recent RSS coverage.\n"
                                        + "Try a broader category or generate again.");
                    }
                }));
    }

    /** TARGET matches NewsGeneratorService.TARGET */
    private static final int TARGET = 5;

    /**
     * Returns a human-readable scope description for alert messages.
     * Mirrors the geo-scope logic in NewsGeneratorService.
     */
    private String resolveScopeLabel(String category) {
        if (category == null) return "Iloilo";
        String lc = category.toLowerCase(Locale.ROOT).trim();
        if (NATIONAL_CATEGORIES.contains(lc)) return "the Philippines (national)";
        if (WEATHER_CATEGORIES.contains(lc))  return "Iloilo Province / Philippines weather";
        return "Iloilo City and Iloilo Province";
    }

    // =========================================================================
    //  Network status — auto-refresh + offline-during-generation cancel
    // =========================================================================

    /**
     * Fires one async HTTP-HEAD to google.com and routes the result through
     * {@link #handleNetworkStatusChange(boolean)}.
     * <p>
     * Called by the background poller ({@link #startNetworkPoller()}) and
     * also directly when {@link #btnRefreshNetwork} is clicked.
     */
    private void checkNetworkStatus() {
        if (lblNetworkStatus == null) return;
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return InternetConnectionChecker.isOnline();
            }
        };
        task.setOnSucceeded(e ->
                Platform.runLater(() ->
                        handleNetworkStatusChange(Boolean.TRUE.equals(task.getValue()))));
        task.setOnFailed(e ->
                Platform.runLater(() -> handleNetworkStatusChange(false)));
        new Thread(task, "Network-Check-Thread").start();
    }

    /**
     * Central handler for every network-status change (both periodic and
     * manual refresh).
     * <ul>
     *   <li>Updates {@link #lblNetworkStatus} via {@link #updateNetworkLabel}.</li>
     *   <li>When the connection <b>drops</b> while {@link #generationInProgress}
     *       is {@code true}, cancels the news-generation pipeline immediately,
     *       resets UI state, and shows a clear error alert.</li>
     * </ul>
     */
    private void handleNetworkStatusChange(boolean online) {
        boolean wasOnline = lastKnownOnline;
        lastKnownOnline   = online;

        updateNetworkLabel(online);

        // ── Connection lost mid-generation → cancel pipeline immediately ──────
        if (!online && wasOnline && generationInProgress.get()) {
            LOG.warning("[SendSMS] Internet lost during generation — cancelling pipeline.");

            if (newsGeneratorService != null) {
                newsGeneratorService.cancel();          // signal the async pipeline to stop
            }

            // Reset the guard now so the UI is unblocked immediately without
            // waiting for the CompletableFuture's whenComplete() to settle.
            generationInProgress.set(false);
            if (btnGenerateNews != null) {
                btnGenerateNews.setDisable(false);
            }

            MainFrameController main = MainFrameController.getInstance();
            if (main != null) {
                main.setNewsCancelAction(null);
                main.hideNewsProgress();
            }

            AlertDialogManager.showError(
                    "Internet Connection Lost",
                    "The internet connection was lost during news generation.\n"
                            + "The pipeline has been cancelled.\n\n"
                            + "Please restore your connection and try again.");
        }
    }

    /**
     * Starts (or restarts) the background network-status poller.
     * <ul>
     *   <li>Fires an immediate check on start (initial delay = 0).</li>
     *   <li>Repeats every {@value #NETWORK_POLL_INTERVAL_SEC} seconds.</li>
     *   <li>Uses a single daemon thread so it never blocks JVM shutdown.</li>
     * </ul>
     * Safe to call multiple times — stops any existing poller first.
     */
    private void startNetworkPoller() {
        stopNetworkPoller();    // cancel any running poller first

        networkPoller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Network-Poller-Thread");
            t.setDaemon(true);  // does not prevent JVM shutdown
            return t;
        });
        networkPollFuture = networkPoller.scheduleAtFixedRate(
                this::checkNetworkStatus,
                0,                              // fire immediately on start
                NETWORK_POLL_INTERVAL_SEC,
                TimeUnit.SECONDS);

        LOG.info("[SendSMS] Network poller started (interval="
                + NETWORK_POLL_INTERVAL_SEC + "s).");
    }

    /** Stops and discards the background network-status poller gracefully. */
    private void stopNetworkPoller() {
        if (networkPollFuture != null) {
            networkPollFuture.cancel(false);
            networkPollFuture = null;
        }
        if (networkPoller != null) {
            networkPoller.shutdownNow();
            networkPoller = null;
        }
    }

    /**
     * Releases background resources (network poller thread) when the view is
     * navigated away from or the application closes.
     * <p>
     * Wire this to whatever lifecycle hook your app provides — e.g. call it
     * from {@code MainFrameController.loadPage()} just before swapping the
     * content area, or register it as an {@code onHidden} listener.
     */
    public void dispose() {
        stopNetworkPoller();
    }

    private void updateNetworkLabel(boolean online) {
        if (lblNetworkStatus == null) return;
        lblNetworkStatus.setText(online
                ? "Online"
                : "Offline — Internet required for AI news");
        lblNetworkStatus.setStyle(
                "-fx-text-fill: " + (online ? "#22c55e" : "#ef4444") + ";");
    }

    // =========================================================================
    //  News slots UI
    // =========================================================================

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
                if (isSelected && index < storedNewsItems.size()) loadNewsToMessage(index);
            });
        }
    }

    /**
     * Populates one news slot with:
     *   • The Hiligaynon SMS body (translated by Google Translate)
     *   • The source article URL (double-click to open in browser)
     */
    private void updateNewsSlot(int slotIndex, NewsItem item) {
        if (slotIndex < 0 || slotIndex >= newsSlots.length || item == null) return;
        RadioButton slot = newsSlots[slotIndex];
        if (slot == null) return;

        double leftIndent = 38;

        // SMS body — this text is in Hiligaynon (output of Google Translate)
        Label smsLabel = new Label(item.smsText());
        smsLabel.setWrapText(true);
        smsLabel.setMaxWidth(Double.MAX_VALUE);

        // Source URL
        Hyperlink link = new Hyperlink(
                item.url() != null && !item.url().isBlank() ? item.url() : "(no source URL)");
        link.setWrapText(true);
        link.setMaxWidth(Double.MAX_VALUE);
        link.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2 && item.url() != null && !item.url().isBlank())
                openInBrowser(item.url());
        });

        VBox box = new VBox(6, smsLabel, link);
        box.setFillWidth(true);
        box.setMaxWidth(Double.MAX_VALUE);
        box.prefWidthProperty().bind(slot.widthProperty().subtract(leftIndent));
        smsLabel.prefWidthProperty().bind(box.prefWidthProperty());
        link.prefWidthProperty().bind(box.prefWidthProperty());

        slot.setText("");
        slot.setGraphic(box);
        slot.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        slot.setDisable(false);

        // Auto-select first slot
        if (slotIndex == 0) slot.setSelected(true);
    }

    private void clearNewsSlots() {
        for (int i = 0; i < newsSlots.length; i++) {
            RadioButton slot = newsSlots[i];
            if (slot == null) continue;
            slot.setGraphic(null);
            slot.setContentDisplay(ContentDisplay.LEFT);
            slot.setText("Slot " + (i + 1) + " (Empty)");
            slot.setDisable(true);
            slot.setSelected(false);
            slot.setVisible(true);
            slot.setManaged(true);
        }
    }

    /** Loads the selected Hiligaynon SMS text into the message text area. */
    private void loadNewsToMessage(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= storedNewsItems.size() || txtMessage == null) return;
        String text = storedNewsItems.get(slotIndex).smsText();
        if (text != null) txtMessage.setText(text.trim());
    }

    private void updateAiResponseVisibility(boolean visible) {
        if (aiResponseContainer != null) {
            aiResponseContainer.setVisible(visible);
            aiResponseContainer.setManaged(visible);
        }
        if (btnUseSelectedNews != null) btnUseSelectedNews.setDisable(!visible);
        for (int i = 0; i < newsSlots.length; i++) {
            RadioButton slot = newsSlots[i];
            if (slot == null) continue;
            boolean show = visible && i < storedNewsItems.size();
            slot.setVisible(show);
            slot.setManaged(show);
        }
        if (!visible && newsAiResponse != null) newsAiResponse.selectToggle(null);
    }

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

    // =========================================================================
    //  News controls setup
    // =========================================================================

    private void setupNewsControls() {
        if (newsAiResponse == null) newsAiResponse = new ToggleGroup();

        boolean aiEnabled = (newsGeneratorService != null);
        updateAiResponseVisibility(false);

        if (btnGenerateNews != null) {
            btnGenerateNews.setDisable(!aiEnabled);
            btnGenerateNews.setOnAction(e -> onGenerateNews());
            if (!aiEnabled)
                btnGenerateNews.setText("AI Disabled (ANTHROPIC_API_KEY missing)");
        }
        if (cbNewsTopic != null) cbNewsTopic.setDisable(!aiEnabled);
        if (btnSaveEvacMessage != null) btnSaveEvacMessage.setOnAction(e -> onSaveEvacMessage());

        LOG.info("[SendSMS] News controls configured. AI enabled: " + aiEnabled);
    }

    // =========================================================================
    //  Misc helpers
    // =========================================================================

    private void openInBrowser(String url) {
        if (url == null || url.isBlank()) return;
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(url));
            else AlertDialogManager.showWarning("Not Supported", "Cannot open browser on this system.");
        } catch (Exception e) {
            AlertDialogManager.showError("Open Link Failed",
                    "Could not open:\n" + url + "\n\n" + e.getMessage());
        }
    }

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

    @FXML
    private void onSaveEvacMessage() {
        String message = txtCustomEvacMessage != null ? txtCustomEvacMessage.getText() : null;
        if (message == null || message.trim().isEmpty()) {
            AlertDialogManager.showWarning("Empty Message",
                    "Please enter a custom evacuation message before saving.");
            return;
        }
        if (message.length() > 320)
            AlertDialogManager.showWarning("Message Too Long",
                    "Message is " + message.length() + " chars. SMS limit is 320.");
        if (!message.contains("{name}") && !message.contains("{evacSite}") && !message.contains("{site}")) {
            boolean proceed = AlertDialogManager.showConfirmation(
                    "Missing Placeholders",
                    "Message doesn't contain {name} or {evacSite} placeholders.\n\nSave anyway?",
                    ButtonType.OK, ButtonType.CANCEL);
            if (!proceed) return;
        }
        evacMessageManager.setCustomEvacuationMessage(message.trim());
        updateEvacMessageStatus(true, message.trim().length());
        AlertDialogManager.showSuccess("Saved", "Custom evacuation message saved.");
    }

    private void updateEvacMessageStatus(boolean saved, int length) {
        if (lblMessageStatus == null) return;
        if (saved) {
            lblMessageStatus.setText("✓ Custom evacuation message saved.");
            lblMessageStatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else {
            lblMessageStatus.setText("No custom evacuation message set (using default format)");
            lblMessageStatus.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
        }
    }

    @FXML
    private void onSendSMS() {
        if (txtMessage == null || txtMessage.getText().trim().isEmpty()) {
            AlertDialogManager.showError("Empty Message", "Please enter message content.");
            return;
        }
        String group = (cbSelectBeneficiary != null) ? cbSelectBeneficiary.getValue() : null;
        if (group == null) { AlertDialogManager.showError("No Group", "Select a recipient group."); return; }

        String method = (rbGsm != null && rbGsm.isSelected()) ? "GSM" : "API";
        List<BeneficiaryModel> recipients = getRecipients(group);
        if (recipients.isEmpty()) {
            AlertDialogManager.showError("No Recipients", "No valid recipients found for this selection.");
            return;
        }
        boolean confirmed = AlertDialogManager.showConfirmation(
                "Confirm Send",
                "Send to " + recipients.size() + " recipient(s) via " + method + "?",
                ButtonType.OK, ButtonType.CANCEL);
        if (confirmed) sendSMSToRecipients(recipients, "RESPONDPH: " + txtMessage.getText(), method);
    }

    private List<BeneficiaryModel> getRecipients(String group) {
        if (group == null) return List.of();
        String base = group.contains("(") ? group.substring(0, group.indexOf("(")).trim() : group;
        return switch (base) {
            case "All Beneficiaries" -> beneficiaryDAO.getAllBeneficiaries();
            case "By Barangay" -> {
                String brgy = (cbSelectBarangay != null) ? cbSelectBarangay.getValue() : null;
                yield brgy != null ? beneficiaryDAO.getBeneficiariesByBarangay(brgy) : List.of();
            }
            case "By Disaster Area" -> {
                DisasterModel d = (cbSelectDisaster != null) ? cbSelectDisaster.getValue() : null;
                yield d != null ? getBeneficiariesInDisasterArea(d) : List.of();
            }
            default -> base.startsWith("Selected Beneficiaries")
                    ? new ArrayList<>(selectedBeneficiariesList)
                    : List.of();
        };
    }

    private void sendSMSToRecipients(List<BeneficiaryModel> recipients, String message, String method) {
        setSmsLoading(true);
        int total = recipients.size();
        smsService.cancelBulkSend();

        MainFrameController main = MainFrameController.getInstance();
        if (main != null) {
            main.showSmsProgress("Sending SMS (" + method + ")", total);
            main.setSmsCount(0, total);
            main.setSmsCancelAction(() -> {
                smsService.cancelBulkSend();
                Platform.runLater(() -> { setSmsLoading(false); loadSMSLogs(); });
            });
        }

        if (smsService instanceof SmsServiceImpl impl) {
            impl.setBulkProgressListener(new BulkProgressListener() {
                @Override public void onProgress(int done, int tot, int success, String m) {
                    Platform.runLater(() -> { if (main != null) main.setSmsCount(done, tot); });
                }
                @Override public void onFinished(int tot, int success, String m) {
                    Platform.runLater(() -> {
                        setSmsLoading(false);
                        if (main != null) { main.setSmsCancelAction(null); main.hideSmsProgress(); }
                        AlertDialogManager.showSuccess("Send Complete",
                                success + " of " + tot + " messages sent.\n(" + m + ")");
                        loadSMSLogs();
                    });
                }
            });
        }

        Task<Void> task = new Task<>() {
            @Override protected Void call() { smsService.sendBulkSMS(recipients, message, method); return null; }
        };
        task.setOnFailed(ev -> Platform.runLater(() -> {
            setSmsLoading(false);
            if (main != null) { main.setSmsCancelAction(null); main.hideSmsProgress(); }
            Throwable ex = task.getException();
            AlertDialogManager.showError("Send Failed", ex != null ? ex.getMessage() : "Unknown error");
            loadSMSLogs();
        }));
        Thread t = new Thread(task, "SMS-Bulk-Thread");
        t.setDaemon(true);
        t.start();
    }

    private void setSmsLoading(boolean loading) {
        if (smsLoadingBox != null) { smsLoadingBox.setVisible(loading); smsLoadingBox.setManaged(loading); }
        if (btnSendSMS  != null) btnSendSMS.setDisable(loading);
        if (txtMessage  != null) txtMessage.setDisable(loading);
    }

    private void setupRadioButtons() {
        if (rbApi != null) rbApi.selectedProperty().addListener((obs, old, selected) -> {
            if (cbSelectPorts   != null) cbSelectPorts.setDisable(selected);
            if (btnRefreshPorts != null) btnRefreshPorts.setDisable(selected);
            updateSendButtonState();
            updateConnectionStatus();
        });
        if (rbGsm != null) rbGsm.selectedProperty().addListener((obs, old, selected) -> {
            if (!selected) return;
            String port = (cbSelectPorts != null)
                    ? cbSelectPorts.getSelectionModel().getSelectedItem() : null;
            if (port != null && !port.contains("No") && !port.contains("Error"))
                autoConnectToPort(port);
            updateSendButtonState();
        });
    }

    private void autoConnectToPort(String portName) {
        updateConnectionLabel("Connecting to " + portName + "…", "orange");
        updateDisconnectButtonVisibility();
        new Thread(() -> {
            boolean ok = smsSender.connectToPort(portName, 5000);
            Platform.runLater(() -> {
                updateConnectionLabel(ok ? "Connected to " + portName : "Failed to connect to " + portName,
                        ok ? "green" : "red");
                if (!ok) AlertDialogManager.showError("Connection Failed",
                        "Could not connect to GSM modem on " + portName);
                updateDisconnectButtonVisibility();
                updateSendButtonState();
            });
        }, "GSM-Connect-Thread").start();
    }

    private void populateAvailablePorts() {
        if (cbSelectPorts == null) return;
        cbSelectPorts.getSelectionModel().selectedItemProperty().removeListener(portChangeListener);

        new Thread(() -> {
            List<String> ports;
            try { ports = smsSender.getGsmPortsOnly(); }
            catch (Exception e) {
                Platform.runLater(() -> {
                    cbSelectPorts.setItems(FXCollections.observableArrayList("Error detecting ports"));
                    cbSelectPorts.setDisable(true);
                    cbSelectPorts.getSelectionModel().selectFirst();
                    updateConnectionLabel("Error: " + e.getMessage(), "red");
                    updateDisconnectButtonVisibility();
                });
                return;
            }
            Platform.runLater(() -> {
                if (ports.isEmpty()) {
                    cbSelectPorts.setItems(FXCollections.observableArrayList("No ports available"));
                    cbSelectPorts.setDisable(true);
                    cbSelectPorts.getSelectionModel().selectFirst();
                    updateConnectionLabel("No ports found", "red");
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
                        updateConnectionLabel("Select a port to connect", "orange");
                    }
                }
                cbSelectPorts.getSelectionModel().selectedItemProperty().addListener(portChangeListener);
                updateDisconnectButtonVisibility();
                updateSendButtonState();
            });
        }, "Port-Scan-Thread").start();
    }

    private void updateConnectionLabel(String text, String color) {
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText(text);
            connectionStatusLabel.setStyle("-fx-text-fill: " + color + ";");
        }
    }

    private void updateDisconnectButtonVisibility() {
        if (btnDisconnect == null) return;
        boolean connected = smsSender.isConnected();
        btnDisconnect.setVisible(connected);
        btnDisconnect.setManaged(connected);
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
                connectionStatusLabel.setText("Select a port to connect");
                connectionStatusLabel.setStyle("-fx-text-fill: orange;");
            }
        }
        updateDisconnectButtonVisibility();
    }

    @FXML private void onDisconnect() {
        if (smsSender != null && smsSender.isConnected()) {
            smsSender.disconnect();
            updateConnectionLabel("Disconnected", "orange");
            updateDisconnectButtonVisibility();
            updateSendButtonState();
            AlertDialogManager.showInfo("Disconnected", "GSM modem disconnected.");
        }
    }

    @FXML private void onRefreshPorts() { populateAvailablePorts(); }

    // =========================================================================
    //  Table / SMS logs
    // =========================================================================

    public void loadSMSLogs() {
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
                                    ok ? "SMS resent." : "Failed to resend SMS.");
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
                @Override protected void updateItem(Void item, boolean empty) {
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

        // Restart the whole poller — fires an immediate check and resets the
        // 15-second cadence from now.
        if (btnRefreshNetwork != null) btnRefreshNetwork.setOnAction(e -> startNetworkPoller());
    }

    private void populateComboBoxes() {
        if (cbSelectBeneficiary != null) {
            cbSelectBeneficiary.setItems(FXCollections.observableArrayList(
                    "All Beneficiaries", "By Barangay", "Selected Beneficiaries",
                    "By Disaster Area"));
            cbSelectBeneficiary.getSelectionModel().selectFirst();
            cbSelectBeneficiary.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, val) -> onRecipientGroupChanged());
        }
    }

    // =========================================================================
    //  Recipient group / barangay / disaster
    // =========================================================================

    private void onRecipientGroupChanged() {
        if (isUpdatingComboBox || isOpeningDialog) return;
        String group = (cbSelectBeneficiary != null) ? cbSelectBeneficiary.getValue() : null;
        if (group == null) return;
        switch (group) {
            case "By Barangay" -> { showBarangaySelection(true);  showDisasterSelection(false); }
            case "By Disaster Area" -> {
                showBarangaySelection(false); showDisasterSelection(true);
                if (cbSelectDisaster != null && cbSelectDisaster.getItems().isEmpty()) loadDisasters();
            }
            default -> {
                showBarangaySelection(false); showDisasterSelection(false);
                if (group.startsWith("Selected Beneficiaries")) openBeneficiarySelectionDialog();
            }
        }
        updateSendButtonState();
    }

    private void showBarangaySelection(boolean show) {
        if (barangaySelectionBox != null) { barangaySelectionBox.setVisible(show); barangaySelectionBox.setManaged(show); }
        if (show && cbSelectBarangay != null && cbSelectBarangay.getItems().isEmpty()) loadBarangayList();
    }

    private void showDisasterSelection(boolean show) {
        if (disasterSelectionBox != null) { disasterSelectionBox.setVisible(show); disasterSelectionBox.setManaged(show); }
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
            }
        }, "LoadBarangayThread").start();
    }

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
            if (!selectedBeneficiariesList.isEmpty()) ctrl.setPreselectedBeneficiaries(selectedBeneficiariesList);
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
            updateComboEntry("Selected Beneficiaries (%d selected)".formatted(selectedBeneficiariesList.size()));
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

    public void reloadBeneficiarySelectionTable() {
        try {
            List<BeneficiaryModel> freshList = beneficiaryDAO.getAllBeneficiaries();
            BeneficiarySelectionDialogController ctrl =
                    DialogManager.getController("selection", BeneficiarySelectionDialogController.class);
            if (ctrl != null) ctrl.setBeneficiaries(freshList);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private List<BeneficiaryModel> getBeneficiariesInDisasterArea(DisasterModel disaster) {
        List<BeneficiaryModel> result = new ArrayList<>();
        try {
            List<DisasterCircleInfo> circles = disasterMappingService
                    .getDisasterCirclesByDisasterId(disaster.getDisasterId());
            if (circles == null || circles.isEmpty()) {
                AlertDialogManager.showWarning("No Disaster Area",
                        "No geographic area defined for: " + disaster.getName()
                                + "\n\nPlease set the disaster location in Disaster Mapping first.");
                return result;
            }
            Set<Integer> addedIds = new HashSet<>();
            for (DisasterCircleInfo circle : circles) {
                for (BeneficiaryModel b : beneficiaryDAO.getAllBeneficiaries()) {
                    if (addedIds.contains(b.getId())) continue;
                    try {
                        double lat = Double.parseDouble(b.getLatitude() != null ? b.getLatitude() : "");
                        double lon = Double.parseDouble(b.getLongitude() != null ? b.getLongitude() : "");
                        double dist = com.ionres.respondph.util.GeographicUtils
                                .calculateDistance(lat, lon, circle.lat, circle.lon);
                        if (!Double.isNaN(dist) && dist <= circle.radius
                                && b.getMobileNumber() != null && !b.getMobileNumber().trim().isEmpty()) {
                            result.add(b);
                            addedIds.add(b.getId());
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (result.isEmpty())
                AlertDialogManager.showWarning("No Beneficiaries in Area",
                        "No beneficiaries with phone numbers found inside: " + disaster.getName());
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialogManager.showError("Error",
                    "Failed to get beneficiaries in disaster area: " + e.getMessage());
        }
        return result;
    }
}