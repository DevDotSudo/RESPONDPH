package com.ionres.respondph.aid;

import com.ionres.respondph.aid.KMeansAidDistribution.BeneficiaryCluster;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KMeansAidDistribution — K-Means++ Clustering")
class KMeansAidDistributionTest {

    private KMeansAidDistribution kMeans;

    @BeforeEach
    void setUp() { kMeans = new KMeansAidDistribution(); }

    private List<BeneficiaryCluster> make(double... scores) {
        List<BeneficiaryCluster> l = new ArrayList<>();
        for (int i = 0; i < scores.length; i++)
            l.add(new BeneficiaryCluster(i + 1, scores[i],
                    scores[i] >= 0.7 ? "High" : scores[i] >= 0.4 ? "Mod" : "Low"));
        return l;
    }

    private List<BeneficiaryCluster> wellSeparated() {
        List<BeneficiaryCluster> l = new ArrayList<>();
        int id = 1;
        for (double s : new double[]{.90, .88, .92, .85, .95})
            l.add(new BeneficiaryCluster(id++, s, "High"));
        for (double s : new double[]{.50, .48, .52, .45, .55})
            l.add(new BeneficiaryCluster(id++, s, "Mod"));
        for (double s : new double[]{.10, .08, .12, .05, .15})
            l.add(new BeneficiaryCluster(id++, s, "Low"));
        return l;
    }

    // ── Edge Cases ──────────────────────────────────────────────────────────

    @Test @DisplayName("Null input → empty list")
    void nullReturnsEmpty() {
        assertTrue(kMeans.clusterAllBeneficiaries(null, 3).isEmpty());
    }

    @Test @DisplayName("Empty input → empty list")
    void emptyReturnsEmpty() {
        assertTrue(kMeans.clusterAllBeneficiaries(List.of(), 3).isEmpty());
    }

    @Test @DisplayName("Single beneficiary → cluster 0, High Priority")
    void singleBeneficiary() {
        var r = kMeans.clusterAllBeneficiaries(make(0.75), 3);
        assertEquals(1, r.size());
        assertEquals(0, r.get(0).getCluster());
        assertEquals("High Priority", r.get(0).getClusterPriorityLabel());
    }

    @Test @DisplayName("getPrioritizedBeneficiaries(null) → empty")
    void getPrioritizedNullEmpty() {
        assertTrue(kMeans.getPrioritizedBeneficiaries(null, 5, 3).isEmpty());
    }

    // ── Cluster Assignment ──────────────────────────────────────────────────

    @Test @DisplayName("All beneficiaries get valid clusters (0-2)")
    void allHaveValidClusters() {
        var input = wellSeparated();
        kMeans.clusterAllBeneficiaries(input, 3);
        for (var b : input)
            assertTrue(b.getCluster() >= 0 && b.getCluster() <= 2);
    }

    @Test @DisplayName("3 distinct clusters for well-separated data")
    void threeDistinctClusters() {
        var input = wellSeparated();
        kMeans.clusterAllBeneficiaries(input, 3);
        assertEquals(3, input.stream()
                .map(BeneficiaryCluster::getCluster).collect(Collectors.toSet()).size());
    }

    @Test @DisplayName("Cluster 0 has highest avg score (priority ordering)")
    void cluster0IsHighestPriority() {
        var input = wellSeparated();
        kMeans.clusterAllBeneficiaries(input, 3);
        var stats = input.stream().collect(Collectors.groupingBy(
                BeneficiaryCluster::getCluster,
                Collectors.averagingDouble(BeneficiaryCluster::getFinalScore)));
        assertTrue(stats.getOrDefault(0, 0.0) >= stats.getOrDefault(1, 0.0));
        assertTrue(stats.getOrDefault(1, 0.0) >= stats.getOrDefault(2, 0.0));
    }

    @Test @DisplayName("Scores ≥ 0.85 end up in cluster 0")
    void highScoresInCluster0() {
        var input = wellSeparated();
        kMeans.clusterAllBeneficiaries(input, 3);
        for (var b : input)
            if (b.getFinalScore() >= 0.85)
                assertEquals(0, b.getCluster(),
                        "#" + b.getBeneficiaryId() + " score=" + b.getFinalScore());
    }

    @Test @DisplayName("Scores ≤ 0.15 end up in cluster 2")
    void lowScoresInCluster2() {
        var input = wellSeparated();
        kMeans.clusterAllBeneficiaries(input, 3);
        for (var b : input)
            if (b.getFinalScore() <= 0.15)
                assertEquals(2, b.getCluster(),
                        "#" + b.getBeneficiaryId() + " score=" + b.getFinalScore());
    }

    // ── Priority Labels ─────────────────────────────────────────────────────

    @Test @DisplayName("All labels are valid priority labels")
    void allHaveValidLabels() {
        var input = wellSeparated();
        kMeans.clusterAllBeneficiaries(input, 3);
        var valid = Set.of("High Priority", "Moderate Priority", "Low Priority");
        for (var b : input)
            assertTrue(valid.contains(b.getClusterPriorityLabel()),
                    "Unexpected: " + b.getClusterPriorityLabel());
    }

    @Test @DisplayName("2 clusters → High and Moderate only")
    void twoClustersLabels() {
        var input = make(0.9, 0.85, 0.1, 0.15);
        kMeans.clusterAllBeneficiaries(input, 2);
        var labels = input.stream()
                .map(BeneficiaryCluster::getClusterPriorityLabel).collect(Collectors.toSet());
        assertTrue(labels.contains("High Priority"));
        assertTrue(labels.contains("Moderate Priority"));
    }

    // ── Prioritized Selection ───────────────────────────────────────────────

    @Test @DisplayName("Returns exact requested quantity")
    void exactQuantity() {
        assertEquals(7, kMeans.getPrioritizedBeneficiaries(wellSeparated(), 7, 3).size());
    }

    @Test @DisplayName("High-priority cluster selected first")
    void highPriorityFirst() {
        var r = kMeans.getPrioritizedBeneficiaries(wellSeparated(), 5, 3);
        assertEquals(5, r.stream().filter(b -> b.getCluster() == 0).count());
    }

    @Test @DisplayName("Request > available → returns all")
    void requestExceedsAvailable() {
        assertEquals(3,
                kMeans.getPrioritizedBeneficiaries(make(0.9, 0.5, 0.1), 999, 3).size());
    }

    // ── Ordering ────────────────────────────────────────────────────────────

    @Test @DisplayName("clusterAll result sorted descending by score")
    void sortedDescending() {
        var r = kMeans.clusterAllBeneficiaries(wellSeparated(), 3);
        for (int i = 1; i < r.size(); i++)
            assertTrue(r.get(i - 1).getFinalScore() >= r.get(i).getFinalScore());
    }

    // ── Robustness ──────────────────────────────────────────────────────────

    @Test @DisplayName("All identical scores handled without error")
    void identicalScores() {
        assertDoesNotThrow(() -> kMeans.clusterAllBeneficiaries(make(0.5, 0.5, 0.5, 0.5), 3));
    }

    @Test @DisplayName("1000 beneficiaries stress test")
    void largeDataset() {
        Random rng = new Random(42);
        List<BeneficiaryCluster> input = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            double s = rng.nextDouble();
            input.add(new BeneficiaryCluster(i + 1, s, s >= 0.7 ? "H" : "L"));
        }
        var r = kMeans.clusterAllBeneficiaries(input, 3);
        assertEquals(1000, r.size());
        assertEquals(3, r.stream()
                .map(BeneficiaryCluster::getCluster).collect(Collectors.toSet()).size());
    }

    // ── BeneficiaryCluster Model ────────────────────────────────────────────

    @Test @DisplayName("Model defaults: cluster=-1, label=scoreCategory")
    void modelDefaults() {
        var bc = new BeneficiaryCluster(42, 0.867, "Mod");
        assertEquals(-1, bc.getCluster());
        assertEquals("Mod", bc.getClusterPriorityLabel());
        assertEquals(42, bc.getBeneficiaryId());
        assertEquals(0.867, bc.getFinalScore(), 1e-10);
        assertEquals("Mod", bc.getScoreCategory());
    }

    @Test @DisplayName("Custom KMeans parameters work")
    void customParams() {
        var custom = new KMeansAidDistribution(50, 0.0001);
        var r = custom.clusterAllBeneficiaries(wellSeparated(), 3);
        assertEquals(15, r.size());
    }
}

