package com.ionres.respondph.main;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.util.*;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainFrameController {

    private static MainFrameController INSTANCE;
    public static MainFrameController getInstance() { return INSTANCE; }

    @FXML public VBox        contentArea;

    @FXML private VBox        smsProgressToast;
    @FXML private VBox        smsToastBody;
    @FXML private Label       smsProgressTitle;
    @FXML private Label       smsProgressMessage;
    @FXML private ProgressBar smsProgressBar;
    @FXML private Button      smsMinimizeBtn;
    @FXML private Button      smsCloseBtn;

    @FXML private VBox        newsProgressToast;
    @FXML private VBox        newsToastBody;
    @FXML private Label       newsProgressTitle;
    @FXML private ProgressBar newsProgressBar;
    @FXML private Button      newsMinimizeBtn;
    @FXML private Button      newsCloseBtn;

    @FXML private HBox   footerBar;

    @FXML private HBox   smsFooterPill;
    @FXML private Label  footerSmsLabel;
    @FXML private Button btnShowSmsProgress;

    @FXML private HBox   newsFooterPill;
    @FXML private Label  footerNewsLabel;
    @FXML private Button btnShowNewsProgress;

    @FXML private Label  footerDivider;

    @FXML public Button managementSectionBtn;
    @FXML public Button disasterSectionBtn;
    @FXML public Button aidsSectionBtn;
    @FXML public Button evacSectionBtn;
    @FXML private VBox   managementSectionContent;
    @FXML private VBox   disasterSectionContent;
    @FXML private VBox   aidsSectionContent;
    @FXML private VBox   evacSectionContent;
    @FXML private FontAwesomeIconView managementSectionIcon;
    @FXML private FontAwesomeIconView disasterSectionIcon;
    @FXML private FontAwesomeIconView aidsSectionIcon;
    @FXML private FontAwesomeIconView evacSectionIcon;

    @FXML private Button dashboardBtn;
    @FXML private Button manageAdminBtn;
    @FXML public   Button manageBeneficiariesBtn;
    @FXML private Button familyMembersBtn;
    @FXML public   Button disasterBtn;
    @FXML private Button disasterMappingBtn;
    @FXML private Button disasterDamageBtn;
    @FXML private Button vulnerabilityBtn;
    @FXML public   Button evacBtn;
    @FXML private Button evacPlanBtn;
    @FXML private Button aidTypeBtn;
    @FXML public   Button aidBtn;
    @FXML private Button sendSmsBtn;
    @FXML private Button logoutBtn;

    // ── Nav ──────────────────────────────────────────────────────────────────
    public Button activeBtn;

    // ── SMS toast state ───────────────────────────────────────────────────────
    private boolean  smsMinimized   = false;
    private boolean  smsVisible     = false;
    private Task<?>  currentSmsTask = null;
    private Timeline smsSnapTimeline;
    private Runnable smsCancelAction;
    private String   smsFooterText  = "";

    // ── News toast state ──────────────────────────────────────────────────────
    private boolean  newsMinimized  = false;
    private boolean  newsVisible    = false;
    private Timeline newsSnapTimeline;
    private Runnable newsCancelAction;
    private String   newsFooterText = "";

    private VBox       newsActivityPane;
    private VBox       newsLogBox;
    private ScrollPane newsLogScroll;
    private HBox       newsDotBar;
    private Label      newsCountLabel;
    private Label      newsStepLabel;      // ← NEW: shows current step (Step 1/3, Parsing, etc.)
    private Label      streamingLabel;
    private Label      tickLabel;
    private int        confirmedNewsCount = 0;
    private Timeline   pulseTimeline;

    private static final int      MAX_LOG_ROWS     = 14;
    private static final Duration CHEVRON_DURATION = Duration.millis(180);
    private static final double   CHEVRON_COLLAPSED = 0;
    private static final double   CHEVRON_EXPANDED  = 90;
    private static final String   ORANGE_STYLE =
            "-fx-accent: #F97316; -fx-control-inner-background: rgba(249,115,22,0.12);";
    private static final String GROUNDING_SEARCH_EMOJI = "🔍";
    private static final String GROUNDING_PAGE_EMOJI   = "📄";

    // ═════════════════════════════════════════════════════════════════════════
    // INITIALIZE
    // ═════════════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        INSTANCE = this;

        AdminModel currentAdmin = SessionManager.getInstance().getCurrentAdmin();
        if (currentAdmin != null && currentAdmin.getRole() != null) {
            System.out.println(">>> initialize role: " + currentAdmin.getRole());
            applyRoleVisibility(currentAdmin.getRole());
        }

        if (smsMinimizeBtn != null) smsMinimizeBtn.setOnAction(e -> minimizeSmsToFooter());
        if (smsCloseBtn    != null) smsCloseBtn.setOnAction(e -> handleSmsCloseButton());

        if (newsMinimizeBtn != null) newsMinimizeBtn.setOnAction(e -> minimizeNewsToFooter());
        if (newsCloseBtn    != null) newsCloseBtn.setOnAction(e -> handleNewsCloseButton());

        if (btnShowSmsProgress  != null) btnShowSmsProgress.setOnAction(e -> restoreSmsFromFooter());
        if (btnShowNewsProgress != null) btnShowNewsProgress.setOnAction(e -> restoreNewsFromFooter());

        if (smsProgressToast != null) {
            smsProgressToast.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            smsProgressToast.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
            smsProgressToast.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        }
        if (smsToastBody   != null) smsToastBody.setMaxHeight(Region.USE_PREF_SIZE);
        if (smsProgressBar != null) smsProgressBar.setMaxWidth(Double.MAX_VALUE);

        if (newsProgressToast != null) {
            newsProgressToast.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            newsProgressToast.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
            newsProgressToast.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        }
        if (newsToastBody   != null) newsToastBody.setMaxHeight(Region.USE_PREF_SIZE);
        if (newsProgressBar != null) newsProgressBar.setMaxWidth(Double.MAX_VALUE);

        setupSectionToggle(managementSectionBtn, managementSectionContent, managementSectionIcon);
        setupSectionToggle(disasterSectionBtn,   disasterSectionContent,   disasterSectionIcon);
        setupSectionToggle(aidsSectionBtn,        aidsSectionContent,       aidsSectionIcon);
        setupSectionToggle(evacSectionBtn,         evacSectionContent,       evacSectionIcon);

        collapseAllSections();

        String role = (currentAdmin != null) ? currentAdmin.getRole() : null;

        if (role == null || role.equals("Admin") || role.equals("LDRRMO")) {
            loadPage("/view/dashboard/Dashboard.fxml");
            activeButton(dashboardBtn);
        } else if (role.equals("Brgy_Sec") || role.equals("MSWDO")) {
            loadPage("/view/beneficiary/ManageBeneficiaries.fxml");
            activeButton(manageBeneficiariesBtn);
        }

        wireNavButtons();

        hideFooter();
        hideSmsProgress();
        hideNewsProgress();
    }

    private void refreshFooter() {
        boolean smsMini  = smsVisible  && smsMinimized;
        boolean newsMini = newsVisible && newsMinimized;

        if (smsFooterPill != null) {
            smsFooterPill.setVisible(smsMini);
            smsFooterPill.setManaged(smsMini);
        }
        if (footerSmsLabel != null && smsMini)
            footerSmsLabel.setText("📨 " + smsFooterText);

        if (newsFooterPill != null) {
            newsFooterPill.setVisible(newsMini);
            newsFooterPill.setManaged(newsMini);
        }
        if (footerNewsLabel != null && newsMini)
            footerNewsLabel.setText("🤖 " + newsFooterText);

        if (footerDivider != null) {
            footerDivider.setVisible(smsMini && newsMini);
            footerDivider.setManaged(smsMini && newsMini);
        }

        boolean anyMini = smsMini || newsMini;
        if (footerBar != null) {
            footerBar.setVisible(anyMini);
            footerBar.setManaged(anyMini);
        }
    }

    private void hideFooter() {
        setPillVisible(smsFooterPill,  false);
        setPillVisible(newsFooterPill, false);
        setNodeVisible(footerDivider,  false);
        if (footerBar != null) {
            footerBar.setVisible(false);
            footerBar.setManaged(false);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SMS PROGRESS TOAST — public API
    // ═════════════════════════════════════════════════════════════════════════

    public void setSmsCancelAction(Runnable action) { this.smsCancelAction = action; }

    public void showSmsProgress(String title, int total) {
        Platform.runLater(() -> {
            smsMinimized  = false;
            smsVisible    = true;
            smsFooterText = "Preparing…";
            resetSmsBarStyle();
            if (smsProgressTitle   != null) smsProgressTitle.setText(title != null ? title : "Sending SMS");
            if (smsProgressMessage != null) {
                smsProgressMessage.setVisible(true);
                smsProgressMessage.setManaged(true);
            }
            setSmsCount(0, total);
            showSmsToast();
        });
    }

    public void setSmsCount(int sent, int total) {
        Platform.runLater(() -> {
            String msg = "Sending " + sent + " of " + total;
            if (smsProgressMessage != null) smsProgressMessage.setText(msg);
            if (smsProgressBar     != null)
                smsProgressBar.setProgress(total <= 0 ? 0 : sent / (double) total);
            smsFooterText = msg;
            if (smsMinimized) refreshFooter();
        });
    }

    public void bindSmsTask(Task<?> task) {
        Platform.runLater(() -> {
            currentSmsTask = task;
            if (task == null) return;
            task.setOnSucceeded(e -> hideSmsProgress());
            task.setOnFailed(e    -> hideSmsProgress());
            task.setOnCancelled(e -> hideSmsProgress());
        });
    }

    public void hideSmsProgress() {
        Platform.runLater(() -> {
            smsMinimized    = false;
            smsVisible      = false;
            currentSmsTask  = null;
            smsCancelAction = null;
            smsFooterText   = "";
            cancelSmsSnapAnimation();
            resetSmsBarStyle();
            setNodeVisible(smsProgressToast, false);
            refreshFooter();
        });
    }

    private void handleSmsCloseButton() {
        if (smsCancelAction != null) {
            boolean ok = AlertDialogManager.showConfirmation(
                    "Cancel SMS Sending",
                    "SMS sending is still in progress.\n" +
                            "Messages already sent cannot be recalled.\n\nStop sending now?",
                    ButtonType.OK, ButtonType.CANCEL
            );
            if (ok) {
                Runnable action = smsCancelAction;
                smsCancelAction = null;
                if (action != null) action.run();
                hideSmsProgress();
            }
        } else if (currentSmsTask != null) {
            currentSmsTask.cancel();
            hideSmsProgress();
        } else {
            hideSmsProgress();
        }
    }

    private void minimizeSmsToFooter() {
        if (!smsVisible || smsMinimized) return;
        smsMinimized = true;
        if (smsToastBody     != null) { smsToastBody.setVisible(false);     smsToastBody.setManaged(false); }
        if (smsProgressToast != null) { smsProgressToast.setVisible(false); smsProgressToast.setManaged(false); }
        refreshFooter();
    }

    private void restoreSmsFromFooter() {
        if (!smsVisible || !smsMinimized) return;
        smsMinimized = false;
        if (smsToastBody     != null) { smsToastBody.setVisible(true);     smsToastBody.setManaged(true); }
        if (smsProgressToast != null) { smsProgressToast.setVisible(true); smsProgressToast.setManaged(true); }
        refreshFooter();
    }

    private void showSmsToast() {
        if (smsToastBody     != null) { smsToastBody.setVisible(true);     smsToastBody.setManaged(true); }
        if (smsMinimizeBtn   != null) { smsMinimizeBtn.setVisible(true);   smsMinimizeBtn.setManaged(true); }
        if (smsProgressToast != null) { smsProgressToast.setVisible(true); smsProgressToast.setManaged(true); }
    }

    private void cancelSmsSnapAnimation() {
        if (smsSnapTimeline != null) { smsSnapTimeline.stop(); smsSnapTimeline = null; }
    }

    private void resetSmsBarStyle() {
        if (smsProgressBar == null) return;
        smsProgressBar.setStyle("");
        smsProgressBar.getStyleClass().remove("sms-toast-progress-news");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // NEWS PROGRESS TOAST — public API
    // ═════════════════════════════════════════════════════════════════════════

    public void setNewsCancelAction(Runnable action) { this.newsCancelAction = action; }

    public void showNewsProgress(String topic) {
        Platform.runLater(() -> {
            // Always fully reset before showing — fixes stale confirmedNewsCount
            // from a previous generation causing dots not to update.
            resetNewsLogState();
            newsMinimized  = false;
            newsVisible    = true;
            newsFooterText = topic != null ? topic : "AI generating…";

            if (newsProgressTitle != null) newsProgressTitle.setText("AI News Generator");

            if (newsActivityPane != null && newsToastBody != null)
                newsToastBody.getChildren().remove(newsActivityPane);

            newsActivityPane = buildNewsActivityPane(topic);
            if (newsToastBody != null) newsToastBody.getChildren().add(newsActivityPane);

            setNewsBarRaw(0.0);
            applyNewsOrangeStyle();
            showNewsToast();
            startPulse();
        });
    }

    public void setNewsProgress(double progress, String status) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> setNewsProgress(progress, status));
            return;
        }
        if (status == null) { animateNewsBarTo(Math.max(0.0, Math.min(1.0, progress))); return; }

        animateNewsBarTo(Math.max(0.0, Math.min(1.0, progress)));

        // Split header from optional streaming tail (separated by \n▶ )
        String header;
        String streamTail;
        int nlIdx = status.indexOf('\n');
        if (nlIdx >= 0) {
            header    = status.substring(0, nlIdx).trim();
            String after = status.substring(nlIdx + 1).trim();
            streamTail = after.startsWith("▶ ") ? after.substring(2) : after;
        } else {
            header    = status.trim();
            streamTail = null;
        }

        newsFooterText = header;
        if (newsMinimized) refreshFooter();

        // Always update the step label so something is always visible
        updateStepLabel(header);

        if (newsLogBox == null) return;

        // ── Route to the appropriate handler ─────────────────────────────────

        // "N of 5 items found (Xs)"  — dot fill + item confirmed row
        if (header.matches("\\d+ of \\d+ items? found.*")) {
            handleItemConfirmed(header, streamTail);
            return;
        }

        // Grounding events: "🔍 Searching: …" or "📄 Reading: …"
        if (header.startsWith(GROUNDING_SEARCH_EMOJI) || header.startsWith(GROUNDING_PAGE_EMOJI)) {
            handleGroundingEvent(header);
            if (streamTail != null && !streamTail.isBlank()) updateStreamingLabel(streamTail);
            return;
        }

        // Inline live writing / searching label (no dedicated row, just tick)
        if (header.startsWith("Writing news…")
                || header.startsWith("Searching…")
                || header.startsWith("Using gemini")
                || header.startsWith("Switching to")
                || header.startsWith("Found ")
                || header.startsWith("All models")) {
            updateTickLabel(header);
            if (streamTail != null && !streamTail.isBlank()) updateStreamingLabel(streamTail);
            return;
        }

        // N found, searching for M more — treat like tick label
        if (header.matches("Found \\d+.*") || header.matches("Only \\d+.*")) {
            updateTickLabel(header);
            return;
        }

        if (header.startsWith("Validating")) {
            commitStreamingLabel(); clearTickLabel();
            appendRow("🔎 " + header, RowKind.INFO); return;
        }
        if (header.startsWith("Done")) {
            handleDone(header); return;
        }
        if (header.startsWith("Reconnecting")) {
            commitStreamingLabel(); clearTickLabel();
            appendRow("🔄 Reconnecting…", RowKind.INFO); return;
        }
        if (header.startsWith("Cancelled")) {
            commitStreamingLabel(); clearTickLabel(); stopPulse();
            appendRow("🚫 Generation cancelled.", RowKind.INFO);
            return;
        }

        // ── Catch-all: Step X/3, Fetching, Parsing, Translating, Error, etc. ──
        // These were previously silently swallowed. Now they appear in the log.
        if (header.startsWith("Step ")
                || header.startsWith("Fetching")
                || header.startsWith("Parsing")
                || header.startsWith("Translating")
                || header.startsWith("Weather")
                || header.startsWith("Selecting")
                || header.startsWith("Error")) {
            commitStreamingLabel();
            clearTickLabel();
            String emoji = header.startsWith("Error") ? "❌"
                    : header.startsWith("Step 1") ? "📡"
                    : header.startsWith("Step 2") ? "🤖"
                    : header.startsWith("Step 3") ? "🌐"
                    : "⚙️";
            appendRow(emoji + " " + header, RowKind.INFO);
            return;
        }

        // Final fallback — append as an info row so nothing is ever lost
        appendRow("ℹ️ " + header, RowKind.INFO);
    }

    public void hideNewsProgress() {
        Platform.runLater(() -> {
            newsCancelAction = null;
            newsVisible      = false;
            newsMinimized    = false;
            newsFooterText   = "";
            stopPulse();
            resetNewsLogState();
            cancelNewsSnapAnimation();
            resetNewsBarStyle();

            if (newsToastBody != null && newsActivityPane != null)
                newsToastBody.getChildren().remove(newsActivityPane);
            newsActivityPane = null;

            setNodeVisible(newsProgressToast, false);
            refreshFooter();
        });
    }

    // ── News internal ─────────────────────────────────────────────────────────

    private void handleNewsCloseButton() {
        if (newsCancelAction != null) {
            boolean ok = AlertDialogManager.showConfirmation(
                    "Cancel AI News Generation",
                    "News generation is still in progress.\nCancel and discard results?",
                    ButtonType.OK, ButtonType.CANCEL
            );
            if (ok) {
                Runnable action = newsCancelAction;
                newsCancelAction = null;
                if (action != null) action.run();
                hideNewsProgress();
            }
        } else {
            hideNewsProgress();
        }
    }

    private void minimizeNewsToFooter() {
        if (!newsVisible || newsMinimized) return;
        newsMinimized = true;
        if (newsToastBody     != null) { newsToastBody.setVisible(false);     newsToastBody.setManaged(false); }
        if (newsProgressToast != null) { newsProgressToast.setVisible(false); newsProgressToast.setManaged(false); }
        refreshFooter();
    }

    private void restoreNewsFromFooter() {
        if (!newsVisible || !newsMinimized) return;
        newsMinimized = false;
        if (newsToastBody     != null) { newsToastBody.setVisible(true);     newsToastBody.setManaged(true); }
        if (newsProgressToast != null) { newsProgressToast.setVisible(true); newsProgressToast.setManaged(true); }
        refreshFooter();
    }

    private void showNewsToast() {
        if (newsToastBody     != null) { newsToastBody.setVisible(true);     newsToastBody.setManaged(true); }
        if (newsMinimizeBtn   != null) { newsMinimizeBtn.setVisible(true);   newsMinimizeBtn.setManaged(true); }
        if (newsProgressToast != null) { newsProgressToast.setVisible(true); newsProgressToast.setManaged(true); }
    }

    // ── News bar helpers ──────────────────────────────────────────────────────

    private void animateNewsBarTo(double target) {
        if (newsProgressBar == null) return;
        cancelNewsSnapAnimation();
        double cur = newsProgressBar.getProgress();
        if (cur < 0) cur = 0;
        newsSnapTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(newsProgressBar.progressProperty(), cur)),
                new KeyFrame(Duration.millis(180),  new KeyValue(newsProgressBar.progressProperty(), target)));
        newsSnapTimeline.play();
    }

    private void setNewsBarRaw(double v) {
        cancelNewsSnapAnimation();
        if (newsProgressBar != null) newsProgressBar.setProgress(v);
    }

    private void cancelNewsSnapAnimation() {
        if (newsSnapTimeline != null) { newsSnapTimeline.stop(); newsSnapTimeline = null; }
    }

    private void applyNewsOrangeStyle() {
        if (newsProgressBar == null) return;
        newsProgressBar.setStyle(ORANGE_STYLE);
        if (!newsProgressBar.getStyleClass().contains("sms-toast-progress-news"))
            newsProgressBar.getStyleClass().add("sms-toast-progress-news");
    }

    private void resetNewsBarStyle() {
        if (newsProgressBar == null) return;
        newsProgressBar.setStyle("");
        newsProgressBar.getStyleClass().remove("sms-toast-progress-news");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // NEWS ACTIVITY PANE (log / dots / streaming)
    // ═════════════════════════════════════════════════════════════════════════

    private VBox buildNewsActivityPane(String topic) {
        VBox pane = new VBox(8);
        pane.setFillWidth(true);
        pane.setMaxWidth(Double.MAX_VALUE);
        pane.setPadding(new Insets(4, 0, 0, 0));

        // Topic label (dim, italic)
        Label topicLabel = new Label("Topic: " + (topic != null ? topic : "…"));
        topicLabel.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.50); " +
                        "-fx-font-size: 11px; -fx-font-style: italic;");

        // ── NEW: Step label — shows current pipeline step prominently ──────────
        newsStepLabel = new Label("Initializing…");
        newsStepLabel.setWrapText(true);
        newsStepLabel.setMaxWidth(Double.MAX_VALUE);
        newsStepLabel.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.88); " +
                        "-fx-font-size: 11.5px; -fx-font-weight: bold;");

        // Dot bar
        newsDotBar = new HBox(6);
        newsDotBar.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < 5; i++) {
            Circle dot = new Circle(5);
            dot.setId("news-dot-" + i);
            dot.setFill(Color.web("#ffffff18"));
            dot.setStroke(Color.web("#F97316"));
            dot.setStrokeWidth(1.5);
            newsDotBar.getChildren().add(dot);
        }

        newsCountLabel = new Label("0 of 5 items found");
        newsCountLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.60); -fx-font-size: 11px;");
        HBox dotRow = new HBox(10, newsDotBar, newsCountLabel);
        dotRow.setAlignment(Pos.CENTER_LEFT);

        // Log box
        newsLogBox = new VBox(3);
        newsLogBox.setFillWidth(true);
        newsLogBox.setPadding(new Insets(6, 8, 6, 8));
        newsLogBox.setStyle(
                "-fx-background-color: rgba(0,0,0,0.30); -fx-background-radius: 6px;");

        newsLogScroll = new ScrollPane(newsLogBox);
        newsLogScroll.setFitToWidth(true);
        newsLogScroll.setPrefHeight(120);
        newsLogScroll.setMaxHeight(120);
        newsLogScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        newsLogScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        newsLogScroll.setStyle(
                "-fx-background: transparent; -fx-background-color: transparent; " +
                        "-fx-border-color: transparent; -fx-padding: 0;");

        // Stack: topic → step label → dots → log
        pane.getChildren().addAll(topicLabel, newsStepLabel, dotRow, newsLogScroll);
        return pane;
    }

    // ── Status handlers ───────────────────────────────────────────────────────

    private void handleGroundingEvent(String header) {
        String rowText = header.replaceAll("\\s*\\(\\d+s\\)\\s*$", "").trim();
        updateTickLabel(header);
        if (!rowText.equals(lastGroundingRowText())) {
            clearStreamingLabel();
            RowKind kind = header.startsWith(GROUNDING_SEARCH_EMOJI) ? RowKind.SEARCH : RowKind.PAGE;
            appendRowTypewriter(rowText, kind);
        }
    }

    /**
     * Called when NewsGeneratorService emits "N of 5 items found (Xs)".
     *
     * The regex in setNewsProgress matches "\\d+ of \\d+ items? found.*"
     * which handles both singular ("item") and plural ("items").
     * header.split(" ")[0] extracts the N value.
     */
    private void handleItemConfirmed(String header, String streamTail) {
        int n = 0;
        try {
            n = Integer.parseInt(header.split(" ")[0]);
        } catch (NumberFormatException ignored) {}

        // Fill dots from the last confirmed count up to n
        if (n > confirmedNewsCount) {
            for (int i = confirmedNewsCount; i < n && i < 5; i++) {
                fillDot(i);
            }
            confirmedNewsCount = n;

            if (newsCountLabel != null)
                newsCountLabel.setText(n + " of 5 items found");
            newsFooterText = n + " of 5 items found";
            if (newsMinimized) refreshFooter();

            // Update step label to reflect confirmed count
            updateStepLabel("Item " + n + " of 5 confirmed");

            clearTickLabel();
            appendRow("✅ Item " + n + " confirmed", RowKind.ITEM);
        }

        if (streamTail != null && !streamTail.isBlank()) {
            updateStreamingLabel(streamTail);
        }
    }

    private void handleDone(String header) {
        commitStreamingLabel();
        clearTickLabel();
        stopPulse();
        appendRow("🎉 " + header, RowKind.ITEM);
        // Fill any remaining dots in case some confirmations were missed
        for (int i = confirmedNewsCount; i < 5; i++) fillDot(i);
        confirmedNewsCount = 5;
        if (newsCountLabel != null) newsCountLabel.setText("5 of 5 items found ✓");
        updateStepLabel("✓ Generation complete");
        newsFooterText = "5 of 5 items found ✓";
        if (newsMinimized) refreshFooter();
    }

    // ── Log rows ──────────────────────────────────────────────────────────────

    private enum RowKind { SEARCH, PAGE, ITEM, INFO }

    private void appendRow(String text, RowKind kind) {
        if (newsLogBox == null) return;
        ensureLogCapacity();
        Label row = makeRowLabel(text, kind);
        row.setOpacity(0.0);
        newsLogBox.getChildren().add(row);
        FadeTransition ft = new FadeTransition(Duration.millis(220), row);
        ft.setFromValue(0.0); ft.setToValue(1.0); ft.play();
        scrollBottom();
    }

    private void appendRowTypewriter(String fullText, RowKind kind) {
        if (newsLogBox == null || fullText == null || fullText.isBlank()) return;
        ensureLogCapacity();
        Label row = makeRowLabel("", kind);
        newsLogBox.getChildren().add(row);
        scrollBottom();

        int totalChars = fullText.length();
        int[] idx = {0};
        Timeline tw = new Timeline(new KeyFrame(Duration.millis(28), e -> {
            idx[0]++;
            row.setText(idx[0] < totalChars ? fullText.substring(0, idx[0]) + "▌" : fullText);
        }));
        tw.setCycleCount(totalChars);
        PauseTransition pause = new PauseTransition(Duration.millis(60));
        pause.setOnFinished(e -> tw.play());
        pause.play();
    }

    private void ensureLogCapacity() {
        if (newsLogBox == null) return;
        while (newsLogBox.getChildren().size() >= MAX_LOG_ROWS) {
            boolean removed = false;
            for (int i = 0; i < newsLogBox.getChildren().size(); i++) {
                var node = newsLogBox.getChildren().get(i);
                if (node != tickLabel && node != streamingLabel) {
                    newsLogBox.getChildren().remove(i);
                    removed = true;
                    break;
                }
            }
            if (!removed) break;
        }
    }

    private Label makeRowLabel(String text, RowKind kind) {
        Label row = new Label(text);
        row.setWrapText(true);
        row.setMaxWidth(Double.MAX_VALUE);
        switch (kind) {
            case SEARCH -> row.setStyle("-fx-text-fill: #93C5FD; -fx-font-size: 11px;");
            case PAGE   -> row.setStyle("-fx-text-fill: #86EFAC; -fx-font-size: 11px;");
            case ITEM   -> row.setStyle("-fx-text-fill: #FCD34D; -fx-font-size: 11px; -fx-font-weight: bold;");
            case INFO   -> row.setStyle("-fx-text-fill: rgba(255,255,255,0.42); -fx-font-size: 11px;");
        }
        return row;
    }

    // ── Step label ────────────────────────────────────────────────────────────

    /**
     * Updates the prominent step label shown above the dot bar.
     * This is the primary fix: previously all "Step X/3 — …", "Parsing…",
     * "Fetching RSS…", "Translating…" messages were silently dropped.
     * Now they are always displayed here.
     */
    private void updateStepLabel(String text) {
        if (newsStepLabel == null || text == null || text.isBlank()) return;
        newsStepLabel.setText(text);
    }

    // ── Streaming label ───────────────────────────────────────────────────────

    private void updateStreamingLabel(String tail) {
        if (newsLogBox == null || tail == null || tail.isBlank()) return;
        if (streamingLabel == null) {
            ensureLogCapacity();
            streamingLabel = new Label();
            streamingLabel.setWrapText(true);
            streamingLabel.setMaxWidth(Double.MAX_VALUE);
            streamingLabel.setStyle(
                    "-fx-text-fill: rgba(255,255,255,0.30); " +
                            "-fx-font-size: 10.5px; -fx-font-style: italic;");
            newsLogBox.getChildren().add(streamingLabel);
        } else {
            reorderSpecialLabels();
        }
        streamingLabel.setText(tail);
        scrollBottom();
    }

    private void commitStreamingLabel() { streamingLabel = null; }

    private void clearStreamingLabel() {
        if (streamingLabel != null && newsLogBox != null) {
            newsLogBox.getChildren().remove(streamingLabel);
            streamingLabel = null;
        }
    }

    // ── Tick label ────────────────────────────────────────────────────────────

    private void updateTickLabel(String text) {
        if (newsLogBox == null) return;
        if (tickLabel == null) {
            ensureLogCapacity();
            tickLabel = new Label();
            tickLabel.setWrapText(false);
            tickLabel.setMaxWidth(Double.MAX_VALUE);
            tickLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 11px;");
            newsLogBox.getChildren().add(tickLabel);
        } else {
            reorderSpecialLabels();
        }
        tickLabel.setText("🔎 " + text);
        scrollBottom();
    }

    private void clearTickLabel() {
        if (tickLabel != null && newsLogBox != null) {
            newsLogBox.getChildren().remove(tickLabel);
            tickLabel = null;
        }
    }

    private void reorderSpecialLabels() {
        if (newsLogBox == null) return;
        if (tickLabel      != null) { newsLogBox.getChildren().remove(tickLabel);      newsLogBox.getChildren().add(tickLabel); }
        if (streamingLabel != null) { newsLogBox.getChildren().remove(streamingLabel); newsLogBox.getChildren().add(streamingLabel); }
    }

    private String lastGroundingRowText() {
        if (newsLogBox == null) return null;
        for (int i = newsLogBox.getChildren().size() - 1; i >= 0; i--) {
            var node = newsLogBox.getChildren().get(i);
            if (node instanceof Label lbl && lbl != streamingLabel && lbl != tickLabel) {
                String style = lbl.getStyle();
                if (style.contains("#93C5FD") || style.contains("#86EFAC"))
                    return lbl.getText().replace("▌", "");
                break;
            }
        }
        return null;
    }

    private void scrollBottom() {
        Platform.runLater(() -> { if (newsLogScroll != null) newsLogScroll.setVvalue(1.0); });
    }

    // ── Dots + pulse ──────────────────────────────────────────────────────────

    private void fillDot(int i) {
        if (newsDotBar == null || i < 0 || i >= newsDotBar.getChildren().size()) return;
        Circle dot = (Circle) newsDotBar.getChildren().get(i);
        dot.setFill(Color.web("#F97316"));
        dot.setStroke(Color.web("#FDBA74"));
        ScaleTransition pop = new ScaleTransition(Duration.millis(220), dot);
        pop.setFromX(1.0); pop.setFromY(1.0);
        pop.setToX(1.5);   pop.setToY(1.5);
        pop.setAutoReverse(true); pop.setCycleCount(2);
        pop.play();
    }

    private void startPulse() {
        stopPulse();
        pulseTimeline = new Timeline(new KeyFrame(Duration.millis(900), e -> {
            if (newsLogBox == null || newsLogBox.getChildren().isEmpty()) return;
            var last = newsLogBox.getChildren().get(newsLogBox.getChildren().size() - 1);
            FadeTransition ft = new FadeTransition(Duration.millis(450), last);
            ft.setFromValue(1.0); ft.setToValue(0.30);
            ft.setAutoReverse(true); ft.setCycleCount(2);
            ft.play();
        }));
        pulseTimeline.setCycleCount(Timeline.INDEFINITE);
        pulseTimeline.play();
    }

    private void stopPulse() {
        if (pulseTimeline != null) { pulseTimeline.stop(); pulseTimeline = null; }
    }

    /**
     * Resets all news log state. Called at the start of every new generation
     * to ensure dots start from 0, step label resets, and no stale refs remain.
     */
    private void resetNewsLogState() {
        confirmedNewsCount = 0;      // ← critical: dots start from 0 each run
        newsLogBox         = null;
        newsLogScroll      = null;
        newsDotBar         = null;
        newsCountLabel     = null;
        newsStepLabel      = null;   // ← reset step label ref
        streamingLabel     = null;
        tickLabel          = null;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═════════════════════════════════════════════════════════════════════════

    private static void setNodeVisible(VBox node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private static void setNodeVisible(Label node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private static void setPillVisible(HBox pill, boolean visible) {
        if (pill == null) return;
        pill.setVisible(visible);
        pill.setManaged(visible);
    }

    public void openManagementSection() { ensureSectionOpen(managementSectionContent, managementSectionIcon); }
    public void openDisasterSection()   { ensureSectionOpen(disasterSectionContent,   disasterSectionIcon); }
    public void openAidsSection()       { ensureSectionOpen(aidsSectionContent,        aidsSectionIcon); }
    public void openEvacSection()       { ensureSectionOpen(evacSectionContent,        evacSectionIcon); }

    private void wireNavButtons() {
        EventHandler<ActionEvent> nav = this::handleActions;
        Button[] btns = {
                dashboardBtn, manageAdminBtn, manageBeneficiariesBtn, familyMembersBtn,
                disasterBtn, disasterMappingBtn, disasterDamageBtn,
                aidBtn, aidTypeBtn, evacBtn, evacPlanBtn,
                vulnerabilityBtn, sendSmsBtn, logoutBtn
        };
        for (Button b : btns) if (b != null) b.setOnAction(nav);
    }

    private void collapseAllSections() {
        collapseSection(managementSectionContent, managementSectionIcon);
        collapseSection(disasterSectionContent,   disasterSectionIcon);
        collapseSection(aidsSectionContent,        aidsSectionIcon);
        collapseSection(evacSectionContent,        evacSectionIcon);
    }

    private void collapseSection(VBox c, FontAwesomeIconView i) {
        if (c == null || i == null) return;
        c.setVisible(false); c.setManaged(false);
        i.setRotate(CHEVRON_COLLAPSED);
    }

    private void setupSectionToggle(Button b, VBox c, FontAwesomeIconView i) {
        if (b == null || c == null || i == null) return;
        b.setOnAction(e -> {
            boolean opening = !c.isVisible();
            collapseAllSections();
            if (opening) {
                c.setVisible(true); c.setManaged(true);
                animateChevron(i, CHEVRON_EXPANDED);
            }
        });
    }

    private void ensureSectionOpen(VBox c, FontAwesomeIconView i) {
        if (c == null || i == null || c.isVisible()) return;
        collapseAllSections();
        c.setVisible(true); c.setManaged(true);
        animateChevron(i, CHEVRON_EXPANDED);
    }

    private void animateChevron(FontAwesomeIconView i, double angle) {
        if (i == null) return;
        RotateTransition rt = new RotateTransition(CHEVRON_DURATION, i);
        rt.setToAngle(angle); rt.play();
    }

    private void handleActions(ActionEvent ev) {
        Object s = ev.getSource();
        if      (s == dashboardBtn)           handleDashboard();
        else if (s == manageAdminBtn)         { ensureSectionOpen(managementSectionContent, managementSectionIcon); handleManageAdmins(); }
        else if (s == manageBeneficiariesBtn) { ensureSectionOpen(managementSectionContent, managementSectionIcon); handleManageBeneficiaries(); }
        else if (s == familyMembersBtn)       { ensureSectionOpen(managementSectionContent, managementSectionIcon); handleFamilyMembers(); }
        else if (s == disasterBtn)            { ensureSectionOpen(disasterSectionContent,   disasterSectionIcon);   handleDisaster(); }
        else if (s == disasterMappingBtn)     { ensureSectionOpen(disasterSectionContent,   disasterSectionIcon);   handleDisasterMapping(); }
        else if (s == disasterDamageBtn)      { ensureSectionOpen(disasterSectionContent,   disasterSectionIcon);   handleDisasterDamage(); }
        else if (s == vulnerabilityBtn)       handleVulnerabilityIndicator();
        else if (s == sendSmsBtn)             handleSendSms();
        else if (s == logoutBtn)              handleLogout();
        else if (s == aidBtn)                 { ensureSectionOpen(aidsSectionContent, aidsSectionIcon); handleAid(); }
        else if (s == aidTypeBtn)             { ensureSectionOpen(aidsSectionContent, aidsSectionIcon); handleAidType(); }
        else if (s == evacBtn)                { ensureSectionOpen(evacSectionContent,  evacSectionIcon); handleEvacSite(); }
        else if (s == evacPlanBtn)            { ensureSectionOpen(evacSectionContent,  evacSectionIcon); handleEvacPlan(); }
    }

    private void handleDashboard()              { loadPage("/view/dashboard/Dashboard.fxml");                     activeButton(dashboardBtn); }
    private void handleManageAdmins()           { loadPage("/view/admin/ManageAdmins.fxml");                      activeButton(manageAdminBtn); }
    private void handleManageBeneficiaries()    { loadPage("/view/beneficiary/ManageBeneficiaries.fxml");         activeButton(manageBeneficiariesBtn); }
    private void handleFamilyMembers()          { loadPage("/view/family/FamilyMembers.fxml");                    activeButton(familyMembersBtn); }
    private void handleDisaster()               { loadPage("/view/disaster/Disaster.fxml");                       activeButton(disasterBtn); }
    private void handleDisasterMapping()        { loadPage("/view/disaster_mapping/DisasterMapping.fxml");        activeButton(disasterMappingBtn); }
    private void handleDisasterDamage()         { loadPage("/view/disaster_damage/DisasterDamage.fxml");          activeButton(disasterDamageBtn); }
    private void handleAid()                    { loadPage("/view/aid/Aid.fxml");                                 activeButton(aidBtn); }
    private void handleAidType()                { loadPage("/view/aid_type/AidType.fxml");                        activeButton(aidTypeBtn); }
    private void handleEvacSite()               { loadPage("/view/evac_site/EvacSite.fxml");                      activeButton(evacBtn); }
    private void handleEvacPlan()               { loadPage("/view/evacuation_plan/EvacuationPlan.fxml");          activeButton(evacPlanBtn); }
    private void handleSendSms()                { loadPage("/view/send_sms/SendSMS.fxml");                        activeButton(sendSmsBtn); }
    private void handleVulnerabilityIndicator() {
        DashboardRefresher.refreshFlds();
        loadPage("/view/vulnerability_indicator/VulnerabilityIndicator.fxml");
        activeButton(vulnerabilityBtn);
    }

    private void handleLogout() {
        if (!AlertDialogManager.showConfirmation("Logout", "Do you want to logout?")) return;
        new AppPreferences().clearRememberMe();
        SessionManager.getInstance().clearSession();
        Stage stage = (Stage) logoutBtn.getScene().getWindow();
        SceneManager.clearCache("/view/main/MainScreen.fxml");
        stage.close();
        SceneManager.showStage("/view/auth/Login.fxml", "RESPONDPH - Login");
    }

    private void loadPage(String fxml) {
        SceneManager.SceneEntry<?> e = SceneManager.load(fxml);
        Parent root = e.getRoot();
        if (root instanceof Region r) {
            r.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            VBox.setVgrow(r, Priority.ALWAYS);
        }
        contentArea.getChildren().setAll(root);
    }

    public void applyRoleVisibility(String role) {
        if (role == null || role.isBlank()) {
            setVisible(managementSectionBtn, managementSectionContent, false);
            setVisible(disasterSectionBtn,   disasterSectionContent,   false);
            setVisible(aidsSectionBtn,       aidsSectionContent,       false);
            setVisible(evacSectionBtn,       evacSectionContent,       false);
            if (vulnerabilityBtn != null) { vulnerabilityBtn.setVisible(false); vulnerabilityBtn.setManaged(false); }
            if (sendSmsBtn != null)       { sendSmsBtn.setVisible(false);       sendSmsBtn.setManaged(false); }
            return;
        }

        switch (role) {
            case "Admin" -> {
                // All visible — do nothing
            }
            case "Brgy_Sec" -> {
                setVisible(disasterSectionBtn, disasterSectionContent, false);
                setVisible(aidsSectionBtn,     aidsSectionContent,     false);
                setVisible(evacSectionBtn,     evacSectionContent,     false);
                setButtonVisible(vulnerabilityBtn, false);
                setButtonVisible(sendSmsBtn, false);
                setButtonVisible(manageAdminBtn, false);
                setButtonVisible(dashboardBtn, false);
            }
            case "MSWDO" -> {
                setVisible(disasterSectionBtn, disasterSectionContent, false);
                setButtonVisible(sendSmsBtn, false);
                setButtonVisible(manageAdminBtn, false);
                setButtonVisible(dashboardBtn, false);
            }
            case "LDRRMO" -> {
                setButtonVisible(manageAdminBtn, false);
            }
        }
    }

    private void setVisible(Button sectionBtn, VBox sectionContent, boolean visible) {
        if (sectionBtn != null) {
            sectionBtn.setVisible(visible);
            sectionBtn.setManaged(visible);
        }
        if (sectionContent != null) {
            sectionContent.setVisible(visible);
            sectionContent.setManaged(visible);
        }
    }

    private void setButtonVisible(Button btn, boolean visible) {
        if (btn == null) return;
        btn.setVisible(visible);
        btn.setManaged(visible);
    }

    private void activeButton(Button btn) {
        if (btn == null) return;
        if (activeBtn != null) {
            activeBtn.getStyleClass().remove("nav-button-active");
            activeBtn.getStyleClass().remove("nav-button-child-active");
        }
        activeBtn = btn;
        String cls = btn.getStyleClass().contains("nav-button-child")
                ? "nav-button-child-active" : "nav-button-active";
        if (!activeBtn.getStyleClass().contains(cls))
            activeBtn.getStyleClass().add(cls);
    }
}