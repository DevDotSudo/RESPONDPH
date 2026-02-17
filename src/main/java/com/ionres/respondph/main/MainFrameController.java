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

public class MainFrameController {

    private static MainFrameController INSTANCE;
    public static MainFrameController getInstance() { return INSTANCE; }

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

    private Button   activeBtn;
    private boolean  smsMinimized = false;
    private Task<?>  currentSmsTask;
    private Timeline snapTimeline;

    // ── News toast state ──────────────────────────────────────────────────────
    private VBox       newsActivityPane;
    private VBox       newsLogBox;
    private ScrollPane newsLogScroll;
    private HBox       newsDotBar;
    private Label      newsCountLabel;
    /** Single label updated in-place for live token stream. Replaced on commit. */
    private Label      streamingLabel;
    /** Single label updated in-place for the elapsed-seconds ticker. */
    private Label      tickLabel;
    private int        confirmedNewsCount = 0;
    private Timeline   pulseTimeline;
    private static final int MAX_LOG_ROWS = 12;

    // ── Grounding emoji prefixes — must match what NewsGeneratorService emits ─
    private static final String GROUNDING_SEARCH_EMOJI = "🔍";
    private static final String GROUNDING_PAGE_EMOJI   = "📄";

    private static final Duration CHEVRON_DURATION  = Duration.millis(180);
    private static final double   CHEVRON_COLLAPSED = 0;
    private static final double   CHEVRON_EXPANDED  = 90;
    private static final String   ORANGE_STYLE =
            "-fx-accent: #F97316; -fx-control-inner-background: rgba(249,115,22,0.12);";

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        INSTANCE = this;
        if (smsMinimizeBtn != null) smsMinimizeBtn.setOnAction(e -> minimizeToFooter());
        if (smsCloseBtn    != null) smsCloseBtn.setOnAction(e -> { if (currentSmsTask != null) currentSmsTask.cancel(); hideSmsProgress(); });
        if (btnShowProgress!= null) btnShowProgress.setOnAction(e -> restoreFromFooter());

        if (smsProgressToast != null) {

            smsProgressToast.setMinSize(
                    Region.USE_PREF_SIZE,
                    Region.USE_PREF_SIZE
            );

            smsProgressToast.setPrefSize(
                    Region.USE_COMPUTED_SIZE,
                    Region.USE_COMPUTED_SIZE
            );

            smsProgressToast.setMaxSize(
                    Region.USE_PREF_SIZE,
                    Region.USE_PREF_SIZE
            );
        }

        if (smsToastBody != null) {

            smsToastBody.setMaxHeight(Region.USE_PREF_SIZE);
        }

        if (smsProgressBar != null) {

            smsProgressBar.setMaxWidth(Double.MAX_VALUE);
        }

        setupSectionToggle(managementSectionBtn, managementSectionContent, managementSectionIcon);
        setupSectionToggle(disasterSectionBtn,   disasterSectionContent,   disasterSectionIcon);
        setupSectionToggle(aidsSectionBtn,        aidsSectionContent,       aidsSectionIcon);
        setupSectionToggle(evacSectionBtn,         evacSectionContent,       evacSectionIcon);

        collapseAllSections();
        loadPage("/view/dashboard/Dashboard.fxml");
        activeButton(dashboardBtn);

        EventHandler<ActionEvent> nav = this::handleActions;
        if (dashboardBtn           != null) dashboardBtn.setOnAction(nav);
        if (manageAdminBtn         != null) manageAdminBtn.setOnAction(nav);
        if (manageBeneficiariesBtn != null) manageBeneficiariesBtn.setOnAction(nav);
        if (familyMembersBtn       != null) familyMembersBtn.setOnAction(nav);
        if (disasterBtn            != null) disasterBtn.setOnAction(nav);
        if (disasterMappingBtn     != null) disasterMappingBtn.setOnAction(nav);
        if (disasterDamageBtn      != null) disasterDamageBtn.setOnAction(nav);
        if (aidBtn                 != null) aidBtn.setOnAction(nav);
        if (aidTypeBtn             != null) aidTypeBtn.setOnAction(nav);
        if (evacBtn                != null) evacBtn.setOnAction(nav);
        if (evacPlanBtn            != null) evacPlanBtn.setOnAction(nav);
        if (vulnerabilityBtn       != null) vulnerabilityBtn.setOnAction(nav);
        if (sendSmsBtn             != null) sendSmsBtn.setOnAction(nav);
        if (settingsBtn            != null) settingsBtn.setOnAction(nav);
        if (logoutBtn              != null) logoutBtn.setOnAction(nav);

        hideFooter();
        hideSmsProgress();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NEWS PROGRESS
    // ─────────────────────────────────────────────────────────────────────────

    public void showNewsProgress(String topic) {
        Platform.runLater(() -> {
            smsMinimized = false; confirmedNewsCount = 0; streamingLabel = null; tickLabel = null;
            if (smsProgressTitle   != null) smsProgressTitle.setText("AI News Generator");
            if (smsProgressMessage != null) { smsProgressMessage.setVisible(false); smsProgressMessage.setManaged(false); }
            if (smsToastBody != null && newsActivityPane != null) smsToastBody.getChildren().remove(newsActivityPane);
            newsActivityPane = buildNewsPane(topic);
            if (smsToastBody != null) smsToastBody.getChildren().add(newsActivityPane);
            setBarRaw(0.0); applyOrangeStyle(); showToast(); hideFooter(); startPulse();
        });
    }

    /**
     * Interprets every status string that NewsGeneratorService.generateNewsHeadlines() emits.
     *
     * The service emits these formats (no prefix constants — matched by content):
     *
     *   "Waiting for AI…"
     *       → tick label in log (initial state before grounding starts)
     *
     *   "🔍 Searching: \"…\" (Xs)"   — grounding search query
     *   "📄 Reading: … (Xs)"         — grounding page title
     *       → strip the trailing " (Xs)", typewriter-reveal as a grounding row,
     *         update tick label with the full text including elapsed
     *
     *   "Writing news… (Xs)\n▶ tail"
     *       → tick label update (no new item yet, text is flowing)
     *         tail after \n▶ goes to the streaming label
     *
     *   "N of 5 items found (Xs)\n▶ tail"
     *       → dot fill + item confirmed row + streaming label updated with tail
     *         (the ticker also re-emits this form without a \n▶ tail — handled same way)
     *
     *   "Validating articles… (Xs)"  → dim info row
     *   "Done — N of 5 items in Xs"  → gold done row, fill remaining dots
     *   "Reconnecting…"              → dim info row
     */
    public void setNewsProgress(double progress, String status) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> setNewsProgress(progress, status));
            return;
        }

        double clamped = Math.max(0.0, Math.min(1.0, progress));
        animateBarTo(clamped);

        if (status == null) return;

        // ── Split header from optional stream tail (\n▶ separator) ───────────
        // The service appends "\n▶ <tail>" to item-confirmed and per-chunk events
        // so the toast can show live token preview alongside the progress label.
        String header;
        String streamTail; // may be null
        int nlIdx = status.indexOf('\n');
        if (nlIdx >= 0) {
            header    = status.substring(0, nlIdx).trim();
            String after = status.substring(nlIdx + 1).trim();
            // after is "▶ <tail>" — strip the leading arrow marker
            streamTail = after.startsWith("▶ ") ? after.substring(2) : after;
        } else {
            header    = status.trim();
            streamTail = null;
        }

        // Update footer when minimized — always uses the header (first line)
        if (smsMinimized) showFooterText(header);

        if (newsLogBox == null) return;

        // ── "Waiting for AI…" — initial tick before grounding starts ─────────
        if (header.equals("Waiting for AI…")) {
            updateTickLabel(header);
            return;
        }

        // ── Grounding rows: 🔍 search queries and 📄 page titles ─────────────
        // Format: "🔍 Searching: \"…\" (12s)"  or  "📄 Reading: … (5s)"
        // We show the full string (including elapsed) as the tick label for the
        // footer/minimized view, and strip the trailing " (Ns)" for the typewriter
        // row so the log doesn't fill up with ever-changing elapsed numbers.
        if (header.startsWith(GROUNDING_SEARCH_EMOJI) || header.startsWith(GROUNDING_PAGE_EMOJI)) {
            updateTickLabel(header);           // live elapsed in the tick slot
            clearStreamingLabel();             // grounding supersedes any streaming preview

            // Strip trailing " (Ns)" for the typewriter row — keep it clean
            String rowText = header.replaceAll("\\s*\\(\\d+s\\)\\s*$", "").trim();
            String lastRow = getLastGroundingRowText();
            if (!rowText.equals(lastRow)) {    // skip exact duplicates (ticker re-fires same label)
                clearTickLabel();              // replace tick slot with a real typewriter row
                RowKind kind = header.startsWith(GROUNDING_SEARCH_EMOJI) ? RowKind.SEARCH : RowKind.PAGE;
                appendRowTypewriter(rowText, kind);
            }
            return;
        }

        // ── "Writing news… (Xs)" — text flowing but no item complete yet ─────
        // The service emits this on every chunk before the first item is confirmed.
        // Show elapsed in the tick label; route the stream tail to the streaming label.
        if (header.startsWith("Writing news…")) {
            updateTickLabel(header);
            if (streamTail != null && !streamTail.isBlank()) updateStreamingLabel(streamTail);
            return;
        }

        // ── "N of 5 items found (Xs)" — a complete item just landed ─────────
        // Emitted by both the stream loop (with \n▶ tail) and the ticker (without).
        // Only the stream-loop variant (streamTail != null) advances the dot count.
        if (header.matches("\\d+ of 5 items.*")) {
            int n = 0;
            try { n = Integer.parseInt(header.split(" ")[0]); } catch (NumberFormatException ignored) {}

            if (streamTail != null) {
                // Real item event from the stream loop — advance UI state
                commitStreamingLabel();
                clearTickLabel();
                for (int i = confirmedNewsCount; i < n && i < 5; i++) fillDot(i);
                confirmedNewsCount = n;
                if (newsCountLabel != null) newsCountLabel.setText(n + " of 5 items found");
                appendRow("✅ Item " + n + " confirmed", RowKind.ITEM);
                updateStreamingLabel(streamTail);  // show live tail of next item being written
            } else {
                // Ticker heartbeat — just refresh the tick label with the elapsed count
                updateTickLabel(header);
            }
            return;
        }

        // ── "Validating articles… (Xs)" ──────────────────────────────────────
        if (header.startsWith("Validating")) {
            commitStreamingLabel();
            clearTickLabel();
            appendRow("🔎 " + header, RowKind.INFO);
            return;
        }

        // ── "Done — N of 5 items in Xs" ──────────────────────────────────────
        if (header.startsWith("Done")) {
            commitStreamingLabel();
            clearTickLabel();
            stopPulse();
            appendRow("🎉 " + header, RowKind.ITEM);
            for (int i = confirmedNewsCount; i < 5; i++) fillDot(i);
            if (newsCountLabel != null) newsCountLabel.setText("5 of 5 items found ✓");
            return;
        }

        // ── "Reconnecting…" ───────────────────────────────────────────────────
        if (header.startsWith("Reconnecting")) {
            commitStreamingLabel();
            clearTickLabel();
            appendRow("🔄 Reconnecting…", RowKind.INFO);
        }
    }

    public void hideNewsProgress() {
        Platform.runLater(() -> {
            stopPulse(); smsMinimized = false; currentSmsTask = null;
            cancelSnapAnimation(); resetBarStyle();
            if (smsProgressMessage != null) { smsProgressMessage.setVisible(true); smsProgressMessage.setManaged(true); smsProgressMessage.setWrapText(false); }
            if (smsToastBody != null && newsActivityPane != null) smsToastBody.getChildren().remove(newsActivityPane);
            newsActivityPane = null; newsLogBox = null; newsLogScroll = null;
            newsDotBar = null; newsCountLabel = null; streamingLabel = null; tickLabel = null;
            confirmedNewsCount = 0;
            if (smsProgressToast != null) { smsProgressToast.setVisible(false); smsProgressToast.setManaged(false); }
            hideFooter();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOAST BUILDER
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildNewsPane(String topic) {
        VBox pane = new VBox(8);
        pane.setFillWidth(true); pane.setMaxWidth(Double.MAX_VALUE);
        pane.setPadding(new Insets(4, 0, 0, 0));

        Label topicLabel = new Label("Topic: " + (topic != null ? topic : "…"));
        topicLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.50); -fx-font-size: 11px; -fx-font-style: italic;");

        newsDotBar = new HBox(6); newsDotBar.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < 5; i++) {
            Circle dot = new Circle(5); dot.setId("news-dot-" + i);
            dot.setFill(Color.web("#ffffff18")); dot.setStroke(Color.web("#F97316")); dot.setStrokeWidth(1.5);
            newsDotBar.getChildren().add(dot);
        }
        newsCountLabel = new Label("0 of 5 items found");
        newsCountLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.60); -fx-font-size: 11px;");
        HBox dotRow = new HBox(10, newsDotBar, newsCountLabel); dotRow.setAlignment(Pos.CENTER_LEFT);

        newsLogBox = new VBox(3); newsLogBox.setFillWidth(true);
        newsLogBox.setPadding(new Insets(6, 8, 6, 8));
        newsLogBox.setStyle("-fx-background-color: rgba(0,0,0,0.30); -fx-background-radius: 6px;");

        newsLogScroll = new ScrollPane(newsLogBox);
        newsLogScroll.setFitToWidth(true); newsLogScroll.setPrefHeight(110); newsLogScroll.setMaxHeight(110);
        newsLogScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        newsLogScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        newsLogScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;");

        pane.getChildren().addAll(topicLabel, dotRow, newsLogScroll);
        return pane;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOG ROW MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    private enum RowKind { SEARCH, PAGE, ITEM, INFO }

    /**
     * Standard append with fade-in — used for lifecycle events (items, validating, done).
     */
    private void appendRow(String text, RowKind kind) {
        if (newsLogBox == null) return;
        if (newsLogBox.getChildren().size() >= MAX_LOG_ROWS)
            newsLogBox.getChildren().remove(0);
        Label row = makeRowLabel(text, kind);
        row.setOpacity(0.0);
        newsLogBox.getChildren().add(row);
        FadeTransition ft = new FadeTransition(Duration.millis(220), row);
        ft.setFromValue(0.0); ft.setToValue(1.0); ft.play();
        scrollBottom();
    }

    /**
     * Typewriter reveal — used exclusively for GROUNDING rows (search queries and pages).
     */
    private void appendRowTypewriter(String fullText, RowKind kind) {
        if (newsLogBox == null) return;
        if (newsLogBox.getChildren().size() >= MAX_LOG_ROWS)
            newsLogBox.getChildren().remove(0);

        Label row = makeRowLabel("", kind);
        row.setOpacity(1.0);
        newsLogBox.getChildren().add(row);
        scrollBottom();

        final int totalChars = fullText.length();
        final long charDelayMs = 35;
        final int[] idx = {0};
        Timeline typewriter = new Timeline();
        typewriter.setCycleCount(totalChars);
        KeyFrame kf = new KeyFrame(Duration.millis(charDelayMs), e -> {
            idx[0]++;
            String revealed = fullText.substring(0, idx[0]);
            row.setText(idx[0] < totalChars ? revealed + "▌" : revealed);
        });
        typewriter.getKeyFrames().add(kf);
        PauseTransition pause = new PauseTransition(Duration.millis(80));
        pause.setOnFinished(e -> typewriter.play());
        pause.play();
    }

    /** Build a styled label for the given row kind. */
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

    /**
     * Updates the live streaming preview label in-place.
     * Shows the tail of the current item being written by the AI.
     * Created lazily on first call; replaced (committed) when an item is confirmed.
     */
    private void updateStreamingLabel(String tail) {
        if (newsLogBox == null || tail == null || tail.isBlank()) return;
        if (streamingLabel == null) {
            if (newsLogBox.getChildren().size() >= MAX_LOG_ROWS)
                newsLogBox.getChildren().remove(0);
            streamingLabel = new Label();
            streamingLabel.setWrapText(true); streamingLabel.setMaxWidth(Double.MAX_VALUE);
            streamingLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.32); -fx-font-size: 10.5px; -fx-font-style: italic;");
            newsLogBox.getChildren().add(streamingLabel);
        }
        streamingLabel.setText(tail);
        scrollBottom();
    }

    /** Freezes the current streaming label; next update creates a fresh one. */
    private void commitStreamingLabel() { streamingLabel = null; }

    /** Clears the streaming label from the log entirely (e.g. when grounding starts). */
    private void clearStreamingLabel() {
        if (streamingLabel != null && newsLogBox != null) {
            newsLogBox.getChildren().remove(streamingLabel);
            streamingLabel = null;
        }
    }

    /**
     * Updates the single tick label in-place with the current elapsed/status text.
     * e.g. "🔎 Searching news sources… (12s)" or "🔎 Writing news… (4s)"
     * Created lazily; cleared when real content rows take over.
     */
    private void updateTickLabel(String text) {
        if (newsLogBox == null) return;
        if (tickLabel == null) {
            tickLabel = new Label();
            tickLabel.setWrapText(false);
            tickLabel.setMaxWidth(Double.MAX_VALUE);
            tickLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 11px;");
            newsLogBox.getChildren().add(tickLabel);
        }
        tickLabel.setText("🔎 " + text);
        scrollBottom();
    }

    /** Removes the tick label from the log box when real content starts arriving. */
    private void clearTickLabel() {
        if (tickLabel != null && newsLogBox != null) {
            newsLogBox.getChildren().remove(tickLabel);
            tickLabel = null;
        }
    }

    /**
     * Returns the text of the last typewriter-revealed grounding row (SEARCH or PAGE),
     * used to suppress duplicate rows when the ticker re-emits the same label.
     */
    private String getLastGroundingRowText() {
        if (newsLogBox == null || newsLogBox.getChildren().isEmpty()) return null;
        for (int i = newsLogBox.getChildren().size() - 1; i >= 0; i--) {
            var node = newsLogBox.getChildren().get(i);
            if (node instanceof Label lbl && lbl != streamingLabel && lbl != tickLabel) {
                String style = lbl.getStyle();
                if (style.contains("#93C5FD") || style.contains("#86EFAC")) { // SEARCH or PAGE color
                    return lbl.getText().replace("▌", ""); // strip trailing cursor if still typing
                }
                break; // last real row is not a grounding row
            }
        }
        return null;
    }

    private void scrollBottom() {
        Platform.runLater(() -> { if (newsLogScroll != null) newsLogScroll.setVvalue(1.0); });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DOT + PULSE
    // ─────────────────────────────────────────────────────────────────────────

    private void fillDot(int i) {
        if (newsDotBar == null || i < 0 || i >= newsDotBar.getChildren().size()) return;
        Circle dot = (Circle) newsDotBar.getChildren().get(i);
        dot.setFill(Color.web("#F97316")); dot.setStroke(Color.web("#FDBA74"));
        ScaleTransition pop = new ScaleTransition(Duration.millis(220), dot);
        pop.setFromX(1.0); pop.setFromY(1.0); pop.setToX(1.5); pop.setToY(1.5);
        pop.setAutoReverse(true); pop.setCycleCount(2); pop.play();
    }

    private void startPulse() {
        stopPulse();
        pulseTimeline = new Timeline(new KeyFrame(Duration.millis(900), e -> {
            if (newsLogBox == null || newsLogBox.getChildren().isEmpty()) return;
            var last = newsLogBox.getChildren().get(newsLogBox.getChildren().size() - 1);
            FadeTransition ft = new FadeTransition(Duration.millis(450), last);
            ft.setFromValue(1.0); ft.setToValue(0.30); ft.setAutoReverse(true); ft.setCycleCount(2); ft.play();
        }));
        pulseTimeline.setCycleCount(Timeline.INDEFINITE); pulseTimeline.play();
    }

    private void stopPulse() { if (pulseTimeline != null) { pulseTimeline.stop(); pulseTimeline = null; } }

    // ─────────────────────────────────────────────────────────────────────────
    // SMS BULK PROGRESS
    // ─────────────────────────────────────────────────────────────────────────

    public void showSmsProgress(String title, int total) {
        Platform.runLater(() -> {
            smsMinimized = false; resetBarStyle();
            if (smsProgressMessage != null) { smsProgressMessage.setVisible(true); smsProgressMessage.setManaged(true); }
            if (smsProgressTitle   != null) smsProgressTitle.setText(title != null ? title : "Sending SMS");
            setSmsCount(0, total); showToast(); hideFooter();
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
            smsMinimized = false; currentSmsTask = null; cancelSnapAnimation(); resetBarStyle();
            if (smsProgressToast != null) { smsProgressToast.setVisible(false); smsProgressToast.setManaged(false); }
            hideFooter();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BAR HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void animateBarTo(double target) {
        if (smsProgressBar == null) return; cancelSnapAnimation();
        double cur = smsProgressBar.getProgress(); if (cur < 0) cur = 0;
        snapTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,       new KeyValue(smsProgressBar.progressProperty(), cur)),
                new KeyFrame(Duration.millis(180), new KeyValue(smsProgressBar.progressProperty(), target)));
        snapTimeline.play();
    }

    private void setBarRaw(double v)   { cancelSnapAnimation(); if (smsProgressBar != null) smsProgressBar.setProgress(v); }
    private void cancelSnapAnimation() { if (snapTimeline != null) { snapTimeline.stop(); snapTimeline = null; } }
    private void applyOrangeStyle()    { if (smsProgressBar == null) return; smsProgressBar.setStyle(ORANGE_STYLE); if (!smsProgressBar.getStyleClass().contains("sms-toast-progress-news")) smsProgressBar.getStyleClass().add("sms-toast-progress-news"); }
    private void resetBarStyle()       { if (smsProgressBar == null) return; smsProgressBar.setStyle(""); smsProgressBar.getStyleClass().remove("sms-toast-progress-news"); }

    private void showToast() {
        if (smsToastBody     != null) { smsToastBody.setVisible(true);     smsToastBody.setManaged(true); }
        if (smsMinimizeBtn   != null) { smsMinimizeBtn.setVisible(true);   smsMinimizeBtn.setManaged(true); }
        if (smsProgressToast != null) { smsProgressToast.setVisible(true); smsProgressToast.setManaged(true); }
    }

    private void minimizeToFooter() {
        if (smsMinimized) return; smsMinimized = true;
        if (smsToastBody     != null) { smsToastBody.setVisible(false);     smsToastBody.setManaged(false); }
        if (smsProgressToast != null) { smsProgressToast.setVisible(false); smsProgressToast.setManaged(false); }
        showFooterText(newsCountLabel != null ? newsCountLabel.getText() : "Working…");
    }

    private void restoreFromFooter() {
        if (!smsMinimized) return; smsMinimized = false;
        if (smsToastBody     != null) { smsToastBody.setVisible(true);     smsToastBody.setManaged(true); }
        if (smsProgressToast != null) { smsProgressToast.setVisible(true); smsProgressToast.setManaged(true); }
        hideFooter();
    }

    private void showFooterText(String t) { if (footerStatusLabel != null) footerStatusLabel.setText(t != null ? t : "Working…"); if (footerBar != null) { footerBar.setVisible(true); footerBar.setManaged(true); } }
    private void hideFooter()             { if (footerBar != null) { footerBar.setVisible(false); footerBar.setManaged(false); } }

    // ─────────────────────────────────────────────────────────────────────────
    // NAVIGATION
    // ─────────────────────────────────────────────────────────────────────────

    private void collapseAllSections() {
        collapseSection(managementSectionContent, managementSectionIcon);
        collapseSection(disasterSectionContent,   disasterSectionIcon);
        collapseSection(aidsSectionContent,        aidsSectionIcon);
        collapseSection(evacSectionContent,        evacSectionIcon);
    }
    private void collapseSection(VBox c, FontAwesomeIconView i) { if (c==null||i==null) return; c.setVisible(false); c.setManaged(false); i.setRotate(CHEVRON_COLLAPSED); }
    private void setupSectionToggle(Button b, VBox c, FontAwesomeIconView i) {
        if (b==null||c==null||i==null) return;
        b.setOnAction(e -> { boolean o=!c.isVisible(); collapseAllSections(); if(o){c.setVisible(true);c.setManaged(true);animateChevron(i,CHEVRON_EXPANDED);} });
    }
    private void ensureSectionOpen(VBox c, FontAwesomeIconView i) { if(c==null||i==null||c.isVisible()) return; collapseAllSections(); c.setVisible(true); c.setManaged(true); animateChevron(i,CHEVRON_EXPANDED); }
    private void animateChevron(FontAwesomeIconView i, double a) { if(i==null) return; RotateTransition rt=new RotateTransition(CHEVRON_DURATION,i); rt.setToAngle(a); rt.play(); }

    private void handleActions(ActionEvent ev) {
        Object s = ev.getSource();
        if      (s==dashboardBtn)           handleDashboard();
        else if (s==manageAdminBtn)         { ensureSectionOpen(managementSectionContent,managementSectionIcon); handleManageAdmins(); }
        else if (s==manageBeneficiariesBtn) { ensureSectionOpen(managementSectionContent,managementSectionIcon); handleManageBeneficiaries(); }
        else if (s==familyMembersBtn)       { ensureSectionOpen(managementSectionContent,managementSectionIcon); handleFamilyMembers(); }
        else if (s==disasterBtn)            { ensureSectionOpen(disasterSectionContent,disasterSectionIcon);     handleDisaster(); }
        else if (s==disasterMappingBtn)     { ensureSectionOpen(disasterSectionContent,disasterSectionIcon);     handleDisasterMapping(); }
        else if (s==disasterDamageBtn)      { ensureSectionOpen(disasterSectionContent,disasterSectionIcon);     handleDisasterDamage(); }
        else if (s==vulnerabilityBtn)       handleVulnerabilityIndicator();
        else if (s==sendSmsBtn)             handleSendSms();
        else if (s==settingsBtn)            handleSettings();
        else if (s==logoutBtn)              handleLogout();
        else if (s==aidBtn)                 { ensureSectionOpen(aidsSectionContent,aidsSectionIcon); handleAid(); }
        else if (s==aidTypeBtn)             { ensureSectionOpen(aidsSectionContent,aidsSectionIcon); handleAidType(); }
        else if (s==evacBtn)                { ensureSectionOpen(evacSectionContent,evacSectionIcon);  handleEvacSite(); }
        else if (s==evacPlanBtn)            { ensureSectionOpen(evacSectionContent,evacSectionIcon);  handleEvacPlan(); }
    }

    private void handleDashboard()           { loadPage("/view/dashboard/Dashboard.fxml");                     activeButton(dashboardBtn); }
    private void handleManageAdmins()        { loadPage("/view/admin/ManageAdmins.fxml");                      activeButton(manageAdminBtn); }
    private void handleManageBeneficiaries() { loadPage("/view/beneficiary/ManageBeneficiaries.fxml");         activeButton(manageBeneficiariesBtn); }
    private void handleFamilyMembers()       { loadPage("/view/family/FamilyMembers.fxml");                    activeButton(familyMembersBtn); }
    private void handleDisaster()            { loadPage("/view/disaster/Disaster.fxml");                       activeButton(disasterBtn); }
    private void handleDisasterMapping()     { loadPage("/view/disaster_mapping/DisasterMapping.fxml");        activeButton(disasterMappingBtn); }
    private void handleDisasterDamage()      { loadPage("/view/disaster_damage/DisasterDamage.fxml");          activeButton(disasterDamageBtn); }
    private void handleAid()                 { loadPage("/view/aid/Aid.fxml");                                 activeButton(aidBtn); }
    private void handleAidType()             { loadPage("/view/aid_type/AidType.fxml");                        activeButton(aidTypeBtn); }
    private void handleEvacSite()            { loadPage("/view/evac_site/EvacSite.fxml");                      activeButton(evacBtn); }
    private void handleEvacPlan()            { loadPage("/view/evacuation_plan/EvacuationPlan.fxml");          activeButton(evacPlanBtn); }
    private void handleSendSms()             { loadPage("/view/send_sms/SendSMS.fxml");                        activeButton(sendSmsBtn); }
    private void handleSettings()            { loadPage("/view/settings/Settings.fxml");                       activeButton(settingsBtn); }
    private void handleVulnerabilityIndicator() { DashboardRefresher.refreshFlds(); loadPage("/view/vulnerability_indicator/VulnerabilityIndicator.fxml"); activeButton(vulnerabilityBtn); }

    private void handleLogout() {
        if (!AlertDialogManager.showConfirmation("Logout","Do you want to logout?")) return;
        new AppPreferences().clearRememberMe(); SessionManager.getInstance().clearSession();
        Stage stage=(Stage)logoutBtn.getScene().getWindow(); stage.close();
        SceneManager.showStage("/view/auth/Login.fxml","RESPONDPH - Login");
    }

    private void loadPage(String fxml) {
        SceneManager.SceneEntry<?> e=SceneManager.load(fxml); Parent root=e.getRoot();
        if (root instanceof Region r) { r.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE); VBox.setVgrow(r,Priority.ALWAYS); }
        contentArea.getChildren().setAll(root);
    }

    private void activeButton(Button btn) {
        if (btn==null) return;
        if (activeBtn!=null) { activeBtn.getStyleClass().remove("nav-button-active"); activeBtn.getStyleClass().remove("nav-button-child-active"); }
        activeBtn=btn;
        String cls=btn.getStyleClass().contains("nav-button-child")?"nav-button-child-active":"nav-button-active";
        if (!activeBtn.getStyleClass().contains(cls)) activeBtn.getStyleClass().add(cls);
    }
}