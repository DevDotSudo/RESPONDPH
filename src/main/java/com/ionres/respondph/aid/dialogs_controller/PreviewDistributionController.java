package com.ionres.respondph.aid.dialogs_controller;

import com.ionres.respondph.aid.AidDAO;
import com.ionres.respondph.aid.AidDAOImpl;
import com.ionres.respondph.aid.KMeansAidDistribution.BeneficiaryCluster;
import com.ionres.respondph.database.DBConnection;

import com.ionres.respondph.util.ThemeManager;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;

public class PreviewDistributionController {

    /* ── FXML — header ───────────────────────────────────────────── */
    @FXML private VBox   root;
    @FXML private Label  scopeLabel;
    @FXML private Label  methodLabel;
    @FXML private Label  totalLabel;
    @FXML private Button closeBtn;

    /* ── FXML — HIGH table ───────────────────────────────────────── */
    @FXML private TableView<BeneficiaryRow>            highPriorityTable;
    @FXML private TableColumn<BeneficiaryRow, Integer> highRankCol;
    @FXML private TableColumn<BeneficiaryRow, Integer> highIdCol;
    @FXML private TableColumn<BeneficiaryRow, String>  highNameCol;
    @FXML private TableColumn<BeneficiaryRow, String>  highScoreCol;
    @FXML private Label highCountBadge;
    @FXML private Label highAvgLabel;

    /* ── FXML — MODERATE table ───────────────────────────────────── */
    @FXML private TableView<BeneficiaryRow>            moderatePriorityTable;
    @FXML private TableColumn<BeneficiaryRow, Integer> modRankCol;
    @FXML private TableColumn<BeneficiaryRow, Integer> modIdCol;
    @FXML private TableColumn<BeneficiaryRow, String>  modNameCol;
    @FXML private TableColumn<BeneficiaryRow, String>  modScoreCol;
    @FXML private Label moderateCountBadge;
    @FXML private Label moderateAvgLabel;

    /* ── FXML — LOW table ────────────────────────────────────────── */
    @FXML private TableView<BeneficiaryRow>            lowPriorityTable;
    @FXML private TableColumn<BeneficiaryRow, Integer> lowRankCol;
    @FXML private TableColumn<BeneficiaryRow, Integer> lowIdCol;
    @FXML private TableColumn<BeneficiaryRow, String>  lowNameCol;
    @FXML private TableColumn<BeneficiaryRow, String>  lowScoreCol;
    @FXML private Label lowCountBadge;
    @FXML private Label lowAvgLabel;

    /* ── State ───────────────────────────────────────────────────── */
    private AidDAO                   aidDAO;
    private String                   scopeText;
    private boolean                  fcmSelected;
    private List<BeneficiaryCluster> allClusters = new ArrayList<>();
    private Map<Integer, String>     nameMap     = new HashMap<>();

    /* ── initialize ──────────────────────────────────────────────── */
    @FXML
    private void initialize() {
        aidDAO = new AidDAOImpl(DBConnection.getInstance());
        wireColumns(highRankCol,  highIdCol,  highNameCol,  highScoreCol);
        wireColumns(modRankCol,   modIdCol,   modNameCol,   modScoreCol);
        wireColumns(lowRankCol,   lowIdCol,   lowNameCol,   lowScoreCol);
        highPriorityTable.setPlaceholder(
                new Label("No high priority beneficiaries."));
        moderatePriorityTable.setPlaceholder(
                new Label("No moderate priority beneficiaries."));
        lowPriorityTable.setPlaceholder(
                new Label("No low priority beneficiaries."));
    }

    public void initData(List<BeneficiaryCluster> clusters,
                         String scopeText,
                         boolean fcm,
                         Stage ownerStage) {

        this.allClusters = clusters != null ? clusters : new ArrayList<>();
        this.scopeText   = scopeText;
        this.fcmSelected = fcm;

        // ── Apply theme ───────────────────────────────────────────────
        // ── Apply theme ───────────────────────────────────────────────
        boolean light = ThemeManager.getInstance().isLightMode();
        if (light) {
            if (!root.getStyleClass().contains("root-light"))
                root.getStyleClass().add("root-light");

            highCountBadge.setStyle("-fx-text-fill: black; -fx-font-size: 16px; -fx-font-weight: 900; ");
            moderateCountBadge.setStyle("-fx-text-fill: black; -fx-font-size: 16px; -fx-font-weight: 900;");
            lowCountBadge.setStyle("-fx-text-fill: black; -fx-font-size: 16px; -fx-font-weight: 900;");
        } else {
            root.getStyleClass().remove("root-light");

            highCountBadge.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 900;");
            moderateCountBadge.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 900;");
            lowCountBadge.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 900;");
        }
        // ─────────────────────────────────────────────────────────────

        if (!allClusters.isEmpty()) {
            List<Integer> ids = allClusters.stream()
                    .map(BeneficiaryCluster::getBeneficiaryId)
                    .collect(Collectors.toList());
            nameMap = aidDAO.getBeneficiaryNames(ids);
        }

        updateHeader();
        populateTables();

        closeBtn.setOnAction(this::handleClose);
    }

    /* ── Header labels ───────────────────────────────────────────── */
    private void updateHeader() {
        scopeLabel.setText(scopeText != null ? scopeText : "—");
        methodLabel.setText(fcmSelected
                ? "Fuzzy C-Means (FCM) — 3 Clusters"
                : "K-Means — 3 Clusters");
        totalLabel.setText(allClusters.size() + " beneficiaries");
    }

    private void populateTables() {
        Map<Integer, String> priorityMap = buildClusterPriorityMap(allClusters);

        Map<String, List<BeneficiaryCluster>> byPriority = new LinkedHashMap<>();
        byPriority.put("High Priority",     new ArrayList<>());
        byPriority.put("Moderate Priority", new ArrayList<>());
        byPriority.put("Low Priority",      new ArrayList<>());

        for (BeneficiaryCluster b : allClusters) {
            String p = priorityMap.getOrDefault(b.getCluster(), "Low Priority");
            byPriority.get(p).add(b);
        }

        // Sort each group by score descending
        byPriority.values().forEach(g ->
                g.sort((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore())));

        int rank = 1;
        rank = fillTable(highPriorityTable,
                byPriority.get("High Priority"),
                highCountBadge, highAvgLabel, rank);
        rank = fillTable(moderatePriorityTable,
                byPriority.get("Moderate Priority"),
                moderateCountBadge, moderateAvgLabel, rank);
        fillTable(lowPriorityTable,
                byPriority.get("Low Priority"),
                lowCountBadge, lowAvgLabel, rank);
    }

    /**
     * Fills a TableView and updates the matching badge/avg labels.
     * Returns the next overall rank so numbering is continuous across all 3 tables.
     */
    private int fillTable(TableView<BeneficiaryRow>   table,
                          List<BeneficiaryCluster>    members,
                          Label                       countBadge,
                          Label                       avgLabel,
                          int                         startRank) {

        ObservableList<BeneficiaryRow> rows = FXCollections.observableArrayList();
        int rank = startRank;

        for (BeneficiaryCluster b : members) {
            String name = nameMap.getOrDefault(b.getBeneficiaryId(), "Unknown");
            rows.add(new BeneficiaryRow(rank++, b.getBeneficiaryId(),
                    name, b.getCluster(), b.getFinalScore()));
        }

        table.setItems(rows);
        countBadge.setText(String.valueOf(members.size()));

        double avg = members.stream()
                .mapToDouble(BeneficiaryCluster::getFinalScore)
                .average().orElse(0.0);
        avgLabel.setText(members.isEmpty() ? "Avg: —"
                : String.format("Avg: %.3f", avg));

        return rank;
    }

    /* ── Close ───────────────────────────────────────────────────── */
    @FXML
    private void handleClose(ActionEvent e) {
        ((Stage) root.getScene().getWindow()).close();
    }

    /* ── Column wiring ───────────────────────────────────────────── */
    private void wireColumns(TableColumn<BeneficiaryRow, Integer> rankCol,
                             TableColumn<BeneficiaryRow, Integer> idCol,
                             TableColumn<BeneficiaryRow, String>  nameCol,
                             TableColumn<BeneficiaryRow, String>  scoreCol) {
        rankCol.setCellValueFactory(
                c -> new ReadOnlyObjectWrapper<>(c.getValue().rank()));
        idCol.setCellValueFactory(
                c -> new ReadOnlyObjectWrapper<>(c.getValue().beneficiaryId()));
        nameCol.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(c.getValue().name()));
        scoreCol.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(
                        String.format("%.3f", c.getValue().score())));
    }

    /* ── Priority mapping (mirrors AddAidController logic) ───────── */
    private Map<Integer, String> buildClusterPriorityMap(List<BeneficiaryCluster> list) {
        Map<Integer, Double>  sum = new HashMap<>();
        Map<Integer, Integer> cnt = new HashMap<>();

        for (BeneficiaryCluster b : list) {
            int c = b.getCluster();
            sum.put(c, sum.getOrDefault(c, 0.0) + b.getFinalScore());
            cnt.put(c, cnt.getOrDefault(c, 0)   + 1);
        }
        // Average each cluster
        sum.replaceAll((c, s) -> s / cnt.get(c));

        // Sort clusters by average score descending
        List<Map.Entry<Integer, Double>> sorted = new ArrayList<>(sum.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        String[] labels = { "High Priority", "Moderate Priority", "Low Priority" };
        Map<Integer, String> result = new HashMap<>();
        for (int i = 0; i < sorted.size() && i < labels.length; i++)
            result.put(sorted.get(i).getKey(), labels[i]);
        return result;
    }

    /* ── Row model ───────────────────────────────────────────────── */
    public record BeneficiaryRow(
            int    rank,
            int    beneficiaryId,
            String name,
            int    cluster,
            double score
    ) {}
}