package com.ionres.respondph.main;

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

/**
 * MainFrameController — main shell controller.
 *
 * News-progress toast shows:
 *  • A scrollable log of grounding events (search queries, pages read, confirmed items).
 *  • A live "streaming preview" label (dim italic) updated with each chunk.
 *  • A dedicated "tick" label (elapsed / current action) updated by the background ticker.
 *  • Five progress dots that fill orange as each item is confirmed.
 *
 * Design rules that keep the UI clean:
 *  - streamingLabel is ONE label, updated in-place, never duplicated.
 *  - tickLabel      is ONE label, updated in-place, never duplicated.
 *  - Both are always kept at the BOTTOM of the log list.
 *  - commitStreamingLabel() promotes the current preview into a permanent row
 *    and clears the reference so the next chunk creates a fresh one.
 */
public class MainFrameController {

    private static MainFrameController INSTANCE;
    public static MainFrameController getInstance() { return INSTANCE; }

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private VBox        contentArea;
    @FXML private VBox        smsProgressToast;
    @FXML private VBox        smsToastBody;
    @FXML private Label       smsProgressTitle;
    @FXML private Label       smsProgressMessage;
    @FXML private ProgressBar smsProgressBar;
    @FXML private Button      smsMinimizeBtn;
    @FXML private Button      smsCloseBtn;

    @FXML private HBox   footerBar;
    @FXML private Label  footerStatusLabel;
    @FXML private Button btnShowProgress;

    @FXML private Button managementSectionBtn;
    @FXML private Button disasterSectionBtn;
    @FXML private Button aidsSectionBtn;
    @FXML private Button evacSectionBtn;
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
    @FXML private Button manageBeneficiariesBtn;
    @FXML private Button familyMembersBtn;
    @FXML private Button disasterBtn;
    @FXML private Button disasterMappingBtn;
    @FXML private Button disasterDamageBtn;
    @FXML private Button vulnerabilityBtn;
    @FXML private Button evacBtn;
    @FXML private Button evacPlanBtn;
    @FXML private Button aidTypeBtn;
    @FXML private Button aidBtn;
    @FXML private Button sendSmsBtn;
    @FXML private Button settingsBtn;
    @FXML private Button logoutBtn;

    // ── Nav state ─────────────────────────────────────────────────────────────
    private Button  activeBtn;

    // ── SMS bulk state ────────────────────────────────────────────────────────
    private boolean smsMinimized  = false;
    private Task<?> currentSmsTask;
    private Timeline snapTimeline;

    // ── News toast state ──────────────────────────────────────────────────────
    private VBox       newsActivityPane;
    private VBox       newsLogBox;
    private ScrollPane newsLogScroll;
    private HBox       newsDotBar;
    private Label      newsCountLabel;

    /**
     * Single label rendered at the BOTTOM of the log for the live streaming
     * preview (dim italic). Updated in-place; replaced on item commit.
     */
    private Label streamingLabel;

    /**
     * Single label rendered at the BOTTOM of the log for the elapsed-seconds
     * ticker / current-action ticker. Updated in-place; removed on state change.
     */
    private Label tickLabel;

    private int      confirmedNewsCount = 0;
    private Timeline pulseTimeline;

    /**
     * Registered by SendSMSController before starting news generation.
     * Cleared automatically when hideNewsProgress() is called.
     */
    private Runnable newsCancelAction;

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final int      MAX_LOG_ROWS      = 14;
    private static final Duration CHEVRON_DURATION  = Duration.millis(180);
    private static final double   CHEVRON_COLLAPSED = 0;
    private static final double   CHEVRON_EXPANDED  = 90;
    private static final String   ORANGE_STYLE =
            "-fx-accent: #F97316; -fx-control-inner-background: rgba(249,115,22,0.12);";

    private static final String GROUNDING_SEARCH_EMOJI = "🔍";
    private static final String GROUNDING_PAGE_EMOJI   = "📄";

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        INSTANCE = this;

        // Minimize button — collapses toast body to footer strip
        if (smsMinimizeBtn != null) smsMinimizeBtn.setOnAction(e -> minimizeToFooter());

        // Close (X) button — context-aware:
        //   news active  → confirm cancel  →  invoke cancel action
        //   bulk SMS     → cancel task     →  hide
        //   nothing      → just hide
        if (smsCloseBtn != null) smsCloseBtn.setOnAction(e -> handleCloseButton());

        if (btnShowProgress != null) btnShowProgress.setOnAction(e -> restoreFromFooter());

        // Ensure toast sizing is consistent
        if (smsProgressToast != null) {
            smsProgressToast.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            smsProgressToast.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
            smsProgressToast.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        }
        if (smsToastBody   != null) smsToastBody.setMaxHeight(Region.USE_PREF_SIZE);
        if (smsProgressBar != null) smsProgressBar.setMaxWidth(Double.MAX_VALUE);

        // Sidebar toggles
        setupSectionToggle(managementSectionBtn, managementSectionContent, managementSectionIcon);
        setupSectionToggle(disasterSectionBtn,   disasterSectionContent,   disasterSectionIcon);
        setupSectionToggle(aidsSectionBtn,        aidsSectionContent,       aidsSectionIcon);
        setupSectionToggle(evacSectionBtn,         evacSectionContent,       evacSectionIcon);

        collapseAllSections();
        loadPage("/view/dashboard/Dashboard.fxml");
        activeButton(dashboardBtn);
        wireNavButtons();

        hideFooter();
        hideSmsProgress();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL ACTION
    // ─────────────────────────────────────────────────────────────────────────

    public void setNewsCancelAction(Runnable action) { this.newsCancelAction = action; }

    private void handleCloseButton() {
        if (newsCancelAction != null) {
            boolean ok = AlertDialogManager.showConfirmation(
                    "Cancel AI News Generation",
                    "News generation is still in progress.\nCancel and discard results?",
                    ButtonType.OK, ButtonType.CANCEL
            );
            if (ok) {
                Runnable action = newsCancelAction;
                newsCancelAction = null;       // clear first — re-entrant safety
                if (action != null) action.run();
                // hideNewsProgress() will be called by SendSMSController's whenComplete handler
            }
        } else if (currentSmsTask != null) {
            currentSmsTask.cancel();
            hideSmsProgress();
        } else {
            hideSmsProgress();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NEWS PROGRESS — public API called by SendSMSController
    // ─────────────────────────────────────────────────────────────────────────

    public void showNewsProgress(String topic) {
        Platform.runLater(() -> {
            resetNewsState();
            if (smsProgressTitle   != null) smsProgressTitle.setText("AI News Generator");
            if (smsProgressMessage != null) setProgressMessageVisible(false);

            // Remove any leftover news pane from a prior run
            if (newsActivityPane != null && smsToastBody != null)
                smsToastBody.getChildren().remove(newsActivityPane);

            newsActivityPane = buildNewsPane(topic);
            if (smsToastBody != null) smsToastBody.getChildren().add(newsActivityPane);

            setBarRaw(0.0);
            applyOrangeStyle();
            showToast();
            hideFooter();
            startPulse();
        });
    }

    /**
     * Central dispatcher — called for every status string from NewsGeneratorService.
     *
     * Status string protocol (defined by NewsGeneratorService):
     *   "🔍 Searching: …"                  — grounding search query
     *   "📄 Reading: …"                     — grounding page read
     *   "Writing news… (Xs)"                — stream has started, no item yet
     *   "N of 5 items found (Xs)\n▶ tail"  — Nth item confirmed
     *   "Validating articles… (Xs)"         — post-stream validation
     *   "Done — N of 5 items in Xs"        — finished
     *   "Reconnecting…"                     — fallback call
     *   "Cancelled."                        — user cancelled
     *   (anything else with \n▶ suffix)    — live stream preview update
     */
    public void setNewsProgress(double progress, String status) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> setNewsProgress(progress, status));
            return;
        }
        if (status == null) { animateBarTo(Math.max(0.0, Math.min(1.0, progress))); return; }

        animateBarTo(Math.max(0.0, Math.min(1.0, progress)));

        // Split header from optional stream tail
        String header;
        String streamTail;
        int nlIdx = status.indexOf('\n');
        if (nlIdx >= 0) {
            header = status.substring(0, nlIdx).trim();
            String after = status.substring(nlIdx + 1).trim();
            streamTail = after.startsWith("▶ ") ? after.substring(2) : after;
        } else {
            header    = status.trim();
            streamTail = null;
        }

        // Mirror to footer strip when minimised
        if (smsMinimized) showFooterText(header);
        if (newsLogBox  == null) return;

        // ── Routing logic ─────────────────────────────────────────────────────

        // Elapsed ticker updates (search/page events that update continuously)
        if (header.startsWith(GROUNDING_SEARCH_EMOJI) || header.startsWith(GROUNDING_PAGE_EMOJI)) {
            handleGroundingEvent(header);
            return;
        }

        // Stream started — only update tick + preview
        if (header.startsWith("Writing news…")) {
            updateTickLabel(header);
            if (streamTail != null && !streamTail.isBlank()) updateStreamingLabel(streamTail);
            return;
        }

        // Item confirmed — N of 5
        if (header.matches("\\d+ of \\d+ items.*")) {
            handleItemConfirmed(header, streamTail);
            return;
        }

        // Post-stream validation phase
        if (header.startsWith("Validating")) {
            commitStreamingLabel();
            clearTickLabel();
            appendRow("🔎 " + header, RowKind.INFO);
            return;
        }

        // Done
        if (header.startsWith("Done")) {
            handleDone(header);
            return;
        }

        // Reconnecting fallback
        if (header.startsWith("Reconnecting")) {
            commitStreamingLabel();
            clearTickLabel();
            appendRow("🔄 Reconnecting — switching to direct call…", RowKind.INFO);
            return;
        }

        // Cancelled
        if (header.startsWith("Cancelled")) {
            commitStreamingLabel();
            clearTickLabel();
            stopPulse();
            appendRow("🚫 Generation cancelled.", RowKind.INFO);
        }
    }

    public void hideNewsProgress() {
        Platform.runLater(() -> {
            newsCancelAction = null;
            stopPulse();
            resetNewsState();
            cancelSnapAnimation();
            resetBarStyle();
            setProgressMessageVisible(true);

            if (smsToastBody != null && newsActivityPane != null)
                smsToastBody.getChildren().remove(newsActivityPane);
            newsActivityPane = null;

            if (smsProgressToast != null) {
                smsProgressToast.setVisible(false);
                smsProgressToast.setManaged(false);
            }
            hideFooter();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal status handlers
    // ─────────────────────────────────────────────────────────────────────────

    private void handleGroundingEvent(String header) {
        // Strip trailing "(Xs)" for the log row, keep it in the tick label
        String rowText = header.replaceAll("\\s*\\(\\d+s\\)\\s*$", "").trim();

        updateTickLabel(header); // tick shows the live elapsed version

        // Only add a log row if this grounding event is different from the last one
        if (!rowText.equals(lastGroundingRowText())) {
            clearStreamingLabel(); // grounding event interrupts stream preview
            clearTickLabel();      // promote tick to a real row
            RowKind kind = header.startsWith(GROUNDING_SEARCH_EMOJI) ? RowKind.SEARCH : RowKind.PAGE;
            appendRowTypewriter(rowText, kind);
        }
    }

    private void handleItemConfirmed(String header, String streamTail) {
        // Parse the item count from "N of 5 items…"
        int n = 0;
        try { n = Integer.parseInt(header.split(" ")[0]); } catch (NumberFormatException ignored) {}

        if (streamTail != null) {
            // Real item event (has stream tail) — advance confirmed state
            commitStreamingLabel();
            clearTickLabel();

            for (int i = confirmedNewsCount; i < n && i < 5; i++) fillDot(i);
            confirmedNewsCount = n;
            if (newsCountLabel != null) newsCountLabel.setText(n + " of 5 items found");
            appendRow("✅ Item " + n + " confirmed", RowKind.ITEM);
            updateStreamingLabel(streamTail);
        } else {
            // Heartbeat only — just refresh tick label
            updateTickLabel(header);
        }
    }

    private void handleDone(String header) {
        commitStreamingLabel();
        clearTickLabel();
        stopPulse();
        appendRow("🎉 " + header, RowKind.ITEM);
        for (int i = confirmedNewsCount; i < 5; i++) fillDot(i);
        confirmedNewsCount = 5;
        if (newsCountLabel != null) newsCountLabel.setText("5 of 5 items found ✓");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Toast builder
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildNewsPane(String topic) {
        VBox pane = new VBox(8);
        pane.setFillWidth(true);
        pane.setMaxWidth(Double.MAX_VALUE);
        pane.setPadding(new Insets(4, 0, 0, 0));

        Label topicLabel = new Label("Topic: " + (topic != null ? topic : "…"));
        topicLabel.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.50); " +
                        "-fx-font-size: 11px; -fx-font-style: italic;");

        // Dots row
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
        newsCountLabel.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.60); -fx-font-size: 11px;");
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

        pane.getChildren().addAll(topicLabel, dotRow, newsLogScroll);
        return pane;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Log row management
    // ─────────────────────────────────────────────────────────────────────────

    private enum RowKind { SEARCH, PAGE, ITEM, INFO }

    /** Append a permanent row, fading it in. Trims list to MAX_LOG_ROWS. */
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

    /** Append a permanent row with a typewriter reveal animation. */
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
            row.setText(idx[0] < totalChars
                    ? fullText.substring(0, idx[0]) + "▌"
                    : fullText);
        }));
        tw.setCycleCount(totalChars);
        // Tiny pause before starting, to let the pane lay out
        PauseTransition pause = new PauseTransition(Duration.millis(60));
        pause.setOnFinished(e -> tw.play());
        pause.play();
    }

    private void ensureLogCapacity() {
        if (newsLogBox == null) return;
        // Never remove the special tick/streaming labels — only remove real rows
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
            if (!removed) break; // only special labels left — stop trimming
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

    // ── Streaming label ───────────────────────────────────────────────────────

    /** Update (or create) the single streaming preview label at the bottom of the log. */
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
            // Ensure it stays at the bottom (tick label may be below it — re-anchor)
            reorderSpecialLabels();
        }
        streamingLabel.setText(tail);
        scrollBottom();
    }

    /**
     * "Promote" the streaming label to a permanent entry by clearing the reference.
     * The label node itself stays in the list but loses its special status, so the
     * next chunk will create a NEW streaming label below it.
     */
    private void commitStreamingLabel() {
        streamingLabel = null;  // orphan the node — next update creates a new one below
    }

    /** Remove the streaming label from the log entirely. */
    private void clearStreamingLabel() {
        if (streamingLabel != null && newsLogBox != null) {
            newsLogBox.getChildren().remove(streamingLabel);
            streamingLabel = null;
        }
    }

    // ── Tick label ────────────────────────────────────────────────────────────

    /** Update (or create) the single elapsed-seconds ticker label at the bottom. */
    private void updateTickLabel(String text) {
        if (newsLogBox == null) return;
        if (tickLabel == null) {
            ensureLogCapacity();
            tickLabel = new Label();
            tickLabel.setWrapText(false);
            tickLabel.setMaxWidth(Double.MAX_VALUE);
            tickLabel.setStyle(
                    "-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 11px;");
            newsLogBox.getChildren().add(tickLabel);
        } else {
            reorderSpecialLabels();
        }
        tickLabel.setText("🔎 " + text);
        scrollBottom();
    }

    /** Remove the tick label. */
    private void clearTickLabel() {
        if (tickLabel != null && newsLogBox != null) {
            newsLogBox.getChildren().remove(tickLabel);
            tickLabel = null;
        }
    }

    /**
     * Ensure special labels (tick, streaming) are always at the very bottom
     * of the log, in the order: [permanent rows…] [tickLabel] [streamingLabel].
     */
    private void reorderSpecialLabels() {
        if (newsLogBox == null) return;
        if (tickLabel      != null) { newsLogBox.getChildren().remove(tickLabel);      newsLogBox.getChildren().add(tickLabel); }
        if (streamingLabel != null) { newsLogBox.getChildren().remove(streamingLabel); newsLogBox.getChildren().add(streamingLabel); }
    }

    /** Returns the text of the last SEARCH or PAGE row (excluding special labels). */
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

    // ─────────────────────────────────────────────────────────────────────────
    // Dot + pulse
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    // SMS bulk progress (unchanged public API)
    // ─────────────────────────────────────────────────────────────────────────

    public void showSmsProgress(String title, int total) {
        Platform.runLater(() -> {
            smsMinimized = false;
            resetBarStyle();
            setProgressMessageVisible(true);
            if (smsProgressTitle != null) smsProgressTitle.setText(title != null ? title : "Sending SMS");
            setSmsCount(0, total);
            showToast();
            hideFooter();
        });
    }

    public void setSmsCount(int sent, int total) {
        Platform.runLater(() -> {
            String msg = "Sending " + sent + " of " + total;
            if (smsProgressMessage != null) smsProgressMessage.setText(msg);
            if (smsProgressBar     != null) smsProgressBar.setProgress(total <= 0 ? 0 : sent / (double) total);
            if (smsMinimized) showFooterText(msg);
            else if (footerStatusLabel != null) footerStatusLabel.setText(msg);
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
            smsMinimized = false; currentSmsTask = null;
            cancelSnapAnimation(); resetBarStyle();
            if (smsProgressToast != null) {
                smsProgressToast.setVisible(false);
                smsProgressToast.setManaged(false);
            }
            hideFooter();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Progress bar helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void animateBarTo(double target) {
        if (smsProgressBar == null) return;
        cancelSnapAnimation();
        double cur = smsProgressBar.getProgress();
        if (cur < 0) cur = 0;
        snapTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(smsProgressBar.progressProperty(), cur)),
                new KeyFrame(Duration.millis(180),  new KeyValue(smsProgressBar.progressProperty(), target)));
        snapTimeline.play();
    }

    private void setBarRaw(double v)    { cancelSnapAnimation(); if (smsProgressBar != null) smsProgressBar.setProgress(v); }
    private void cancelSnapAnimation()  { if (snapTimeline != null) { snapTimeline.stop(); snapTimeline = null; } }

    private void applyOrangeStyle() {
        if (smsProgressBar == null) return;
        smsProgressBar.setStyle(ORANGE_STYLE);
        if (!smsProgressBar.getStyleClass().contains("sms-toast-progress-news"))
            smsProgressBar.getStyleClass().add("sms-toast-progress-news");
    }

    private void resetBarStyle() {
        if (smsProgressBar == null) return;
        smsProgressBar.setStyle("");
        smsProgressBar.getStyleClass().remove("sms-toast-progress-news");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Toast show/hide/minimize
    // ─────────────────────────────────────────────────────────────────────────

    private void showToast() {
        if (smsToastBody     != null) { smsToastBody.setVisible(true);     smsToastBody.setManaged(true); }
        if (smsMinimizeBtn   != null) { smsMinimizeBtn.setVisible(true);   smsMinimizeBtn.setManaged(true); }
        if (smsProgressToast != null) { smsProgressToast.setVisible(true); smsProgressToast.setManaged(true); }
    }

    private void minimizeToFooter() {
        if (smsMinimized) return;
        smsMinimized = true;
        if (smsToastBody     != null) { smsToastBody.setVisible(false);     smsToastBody.setManaged(false); }
        if (smsProgressToast != null) { smsProgressToast.setVisible(false); smsProgressToast.setManaged(false); }
        String footerText = newsCountLabel != null ? newsCountLabel.getText() : "Working…";
        showFooterText(footerText);
    }

    private void restoreFromFooter() {
        if (!smsMinimized) return;
        smsMinimized = false;
        if (smsToastBody     != null) { smsToastBody.setVisible(true);     smsToastBody.setManaged(true); }
        if (smsProgressToast != null) { smsProgressToast.setVisible(true); smsProgressToast.setManaged(true); }
        hideFooter();
    }

    private void showFooterText(String t) {
        if (footerStatusLabel != null) footerStatusLabel.setText(t != null ? t : "Working…");
        if (footerBar != null) { footerBar.setVisible(true); footerBar.setManaged(true); }
    }

    private void hideFooter() {
        if (footerBar != null) { footerBar.setVisible(false); footerBar.setManaged(false); }
    }

    private void setProgressMessageVisible(boolean visible) {
        if (smsProgressMessage != null) {
            smsProgressMessage.setVisible(visible);
            smsProgressMessage.setManaged(visible);
            if (visible) smsProgressMessage.setWrapText(false);
        }
    }

    /** Reset all news-toast tracking state (does NOT touch FXML nodes). */
    private void resetNewsState() {
        smsMinimized        = false;
        confirmedNewsCount  = 0;
        newsLogBox          = null;
        newsLogScroll       = null;
        newsDotBar          = null;
        newsCountLabel      = null;
        streamingLabel      = null;
        tickLabel           = null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────────────

    private void wireNavButtons() {
        EventHandler<ActionEvent> nav = this::handleActions;
        Button[] btns = {
                dashboardBtn, manageAdminBtn, manageBeneficiariesBtn, familyMembersBtn,
                disasterBtn, disasterMappingBtn, disasterDamageBtn,
                aidBtn, aidTypeBtn, evacBtn, evacPlanBtn,
                vulnerabilityBtn, sendSmsBtn, settingsBtn, logoutBtn
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
        else if (s == settingsBtn)            handleSettings();
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
    private void handleSettings()               { loadPage("/view/settings/Settings.fxml");                       activeButton(settingsBtn); }
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