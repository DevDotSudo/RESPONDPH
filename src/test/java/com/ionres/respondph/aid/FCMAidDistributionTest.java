package com.ionres.respondph.aid;

import com.ionres.respondph.aid.FCMAidDistribution.BeneficiaryCluster;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Fuzzy C-Means (FCM) clustering algorithm
 * used in prioritized aid distribution.
 */
@DisplayName("FCMAidDistribution — Fuzzy C-Means Clustering")
class FCMAidDistributionTest {

    private FCMAidDistribution fcm;

    @BeforeEach
    void setUp() {
        fcm = new FCMAidDistribution(); // m=2.0, threshold=1e-5, maxIter=300
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helper: create test beneficiaries
    // ═══════════════════════════════════════════════════════════════════════

    private List<BeneficiaryCluster> createBeneficiaries(double... scores) {
        List<BeneficiaryCluster> list = new ArrayList<>();
        for (int i = 0; i < scores.length; i++) {
            String category = scores[i] >= 0.7 ? "High" : scores[i] >= 0.4 ? "Moderate" : "Low";
            list.add(new BeneficiaryCluster(i + 1, scores[i], category));
        }
        return list;
    }

    private List<BeneficiaryCluster> createWellSeparatedData() {
        // High cluster: 0.85–0.95, Moderate: 0.45–0.55, Low: 0.05–0.15
        List<BeneficiaryCluster> list = new ArrayList<>();
        int id = 1;
        for (double s : new double[]{0.90, 0.88, 0.92, 0.85, 0.95}) {
            list.add(new BeneficiaryCluster(id++, s, "High"));
        }
        for (double s : new double[]{0.50, 0.48, 0.52, 0.45, 0.55}) {
            list.add(new BeneficiaryCluster(id++, s, "Moderate"));
        }
        for (double s : new double[]{0.10, 0.08, 0.12, 0.05, 0.15}) {
            list.add(new BeneficiaryCluster(id++, s, "Low"));
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Null / Empty input handling
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge Cases — Null and Empty Inputs")
    class EdgeCaseTests {

        @Test
        @DisplayName("Null input returns empty list")
        void nullInputReturnsEmpty() {
            List<BeneficiaryCluster> result = fcm.clusterAllBeneficiaries(null, 3);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Empty list returns empty list")
        void emptyInputReturnsEmpty() {
            List<BeneficiaryCluster> result = fcm.clusterAllBeneficiaries(new ArrayList<>(), 3);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Single beneficiary is returned sorted (no clustering)")
        void singleBeneficiaryNoClustering() {
            List<BeneficiaryCluster> input = createBeneficiaries(0.75);
            List<BeneficiaryCluster> result = fcm.clusterAllBeneficiaries(input, 3);

            assertEquals(1, result.size());
            assertEquals(0.75, result.get(0).getFinalScore(), 1e-10);
        }

        @Test
        @DisplayName("getPrioritizedBeneficiaries with null returns empty list")
        void getPrioritizedWithNullReturnsEmpty() {
            List<BeneficiaryCluster> result = fcm.getPrioritizedBeneficiaries(null, 5, 3);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Cluster Assignment Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cluster Assignment Correctness")
    class ClusterAssignmentTests {

        @Test
        @DisplayName("All beneficiaries are assigned to a valid cluster (0, 1, or 2)")
        void allBeneficiariesGetValidCluster() {
            List<BeneficiaryCluster> input = createWellSeparatedData();
            fcm.clusterAllBeneficiaries(input, 3);

            for (BeneficiaryCluster b : input) {
                assertTrue(b.getCluster() >= 0 && b.getCluster() <= 2,
                        "Beneficiary #" + b.getBeneficiaryId() + " has invalid cluster: " + b.getCluster());
            }
        }

        @Test
        @DisplayName("Three clusters are produced for well-separated data")
        void threeClustersCreated() {
            List<BeneficiaryCluster> input = createWellSeparatedData();
            fcm.clusterAllBeneficiaries(input, 3);

            Set<Integer> clusters = input.stream()
                    .map(BeneficiaryCluster::getCluster)
                    .collect(Collectors.toSet());

            assertEquals(3, clusters.size(), "Expected 3 distinct clusters");
        }

        @Test
        @DisplayName("Cluster 0 has the highest average score (High Priority)")
        void cluster0HasHighestAverage() {
            List<BeneficiaryCluster> input = createWellSeparatedData();
            fcm.clusterAllBeneficiaries(input, 3);

            Map<Integer, Double> clusterAvg = new HashMap<>();
            Map<Integer, Integer> clusterCount = new HashMap<>();

            for (BeneficiaryCluster b : input) {
                clusterAvg.merge(b.getCluster(), b.getFinalScore(), Double::sum);
                clusterCount.merge(b.getCluster(), 1, Integer::sum);
            }

            for (var entry : clusterAvg.entrySet()) {
                entry.setValue(entry.getValue() / clusterCount.get(entry.getKey()));
            }

            double cluster0Avg = clusterAvg.getOrDefault(0, 0.0);
            double cluster1Avg = clusterAvg.getOrDefault(1, 0.0);
            double cluster2Avg = clusterAvg.getOrDefault(2, 0.0);

            assertTrue(cluster0Avg >= cluster1Avg,
                    "Cluster 0 avg (" + cluster0Avg + ") should be >= Cluster 1 avg (" + cluster1Avg + ")");
            assertTrue(cluster1Avg >= cluster2Avg,
                    "Cluster 1 avg (" + cluster1Avg + ") should be >= Cluster 2 avg (" + cluster2Avg + ")");
        }

        @Test
        @DisplayName("With 2 clusters requested, exactly 2 clusters produced")
        void twoClusters() {
            List<BeneficiaryCluster> input = createBeneficiaries(0.9, 0.8, 0.1, 0.2);
            fcm.clusterAllBeneficiaries(input, 2);

            Set<Integer> clusters = input.stream()
                    .map(BeneficiaryCluster::getCluster)
                    .collect(Collectors.toSet());

            assertEquals(2, clusters.size());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Membership Values (Soft Clustering)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("FCM Membership Values")
    class MembershipTests {

        @Test
        @DisplayName("Membership values sum to ~1.0 for each beneficiary")
        void membershipSumsToOne() {
            List<BeneficiaryCluster> input = createWellSeparatedData();
            fcm.clusterAllBeneficiaries(input, 3);

            for (BeneficiaryCluster b : input) {
                double[] membership = b.getMembershipValues();
                assertNotNull(membership, "Membership values should not be null");
                assertEquals(3, membership.length, "Should have 3 membership values");

                double sum = 0;
                for (double v : membership) sum += v;

                assertEquals(1.0, sum, 0.01,
                        "Membership sum for Beneficiary #" + b.getBeneficiaryId()
                                + " should be ~1.0, got " + sum);
            }
        }

        @Test
        @DisplayName("All membership values are in range [0, 1]")
        void membershipValuesInRange() {
            List<BeneficiaryCluster> input = createWellSeparatedData();
            fcm.clusterAllBeneficiaries(input, 3);

            for (BeneficiaryCluster b : input) {
                for (double v : b.getMembershipValues()) {
                    assertTrue(v >= 0.0 && v <= 1.0,
                            "Membership value " + v + " out of [0,1] range for #" + b.getBeneficiaryId());
                }
            }
        }

        @Test
        @DisplayName("Primary membership is the highest membership value in assigned cluster")
        void primaryMembershipIsHighest() {
            List<BeneficiaryCluster> input = createWellSeparatedData();
            fcm.clusterAllBeneficiaries(input, 3);

            for (BeneficiaryCluster b : input) {
                double primary = b.getPrimaryMembership();
                double[] all = b.getMembershipValues();

                double maxMembership = 0;
                for (double v : all) maxMembership = Math.max(maxMembership, v);

                assertEquals(maxMembership, primary, 1e-10,
                        "Primary membership should equal the max membership value for #" + b.getBeneficiaryId());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Priority Labels
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Priority Label Assignment")
    class PriorityLabelTests {

        @Test
        @DisplayName("All beneficiaries receive a priority label")
        void allHavePriorityLabels() {
            List<BeneficiaryCluster> input = createWellSeparatedData();
            fcm.clusterAllBeneficiaries(input, 3);

            Set<String> validLabels = Set.of("High Priority", "Moderate Priority", "Low Priority");

            for (BeneficiaryCluster b : input) {
                assertNotNull(b.getClusterPriorityLabel());
                assertTrue(validLabels.contains(b.getClusterPriorityLabel()),
                        "Unexpected label: " + b.getClusterPriorityLabel());
            }
        }

        @Test
        @DisplayName("High-score beneficiaries get 'High Priority' label")
        void highScoresGetHighPriority() {
            List<BeneficiaryCluster> input = createWellSeparatedData();
            fcm.clusterAllBeneficiaries(input, 3);

            // Beneficiaries with scores 0.85-0.95 should be "High Priority"
            for (BeneficiaryCluster b : input) {
                if (b.getFinalScore() >= 0.85) {
                    assertEquals("High Priority", b.getClusterPriorityLabel(),
                            "Beneficiary #" + b.getBeneficiaryId()
                                    + " (score=" + b.getFinalScore() + ") should be High Priority");
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Prioritized Selection (Limited Aid)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getPrioritizedBeneficiaries — Limited Aid Distribution")
    class PrioritizedSelectionTests {

        @Test
        @DisplayName("Returns exactly the requested quantity")
        void returnsExactQuantity() {
            List<BeneficiaryCluster> input = createWellSeparatedData();
            List<BeneficiaryCluster> result = fcm.getPrioritizedBeneficiaries(input, 5, 3);

            assertEquals(5, result.size());
        }

        @Test
        @DisplayName("Selected beneficiaries are from the highest priority cluster first")
        void highPriorityFirst() {
            List<BeneficiaryCluster> input = createWellSeparatedData();
            List<BeneficiaryCluster> result = fcm.getPrioritizedBeneficiaries(input, 5, 3);

            // The first 5 should primarily come from the high-score group (0.85-0.95)
            long highScoreCount = result.stream()
                    .filter(b -> b.getFinalScore() >= 0.80)
                    .count();

            assertTrue(highScoreCount >= 4,
                    "At least 4 of 5 selected should be high-score beneficiaries, got " + highScoreCount);
        }

        @Test
        @DisplayName("When quantity exceeds population, returns all beneficiaries")
        void quantityExceedsPopulation() {
            List<BeneficiaryCluster> input = createBeneficiaries(0.9, 0.5, 0.1);
            List<BeneficiaryCluster> result = fcm.getPrioritizedBeneficiaries(input, 100, 3);

            assertEquals(3, result.size(), "Cannot return more than available beneficiaries");
        }

        @Test
        @DisplayName("Quantity of 1 returns the single highest-priority beneficiary")
        void quantityOfOne() {
            List<BeneficiaryCluster> input = createWellSeparatedData();
            List<BeneficiaryCluster> result = fcm.getPrioritizedBeneficiaries(input, 1, 3);

            assertEquals(1, result.size());
            assertTrue(result.get(0).getFinalScore() >= 0.85,
                    "The single selected beneficiary should have a high score");
        }

        @Test
        @DisplayName("Result is sorted by score descending")
        void resultSortedDescending() {
            List<BeneficiaryCluster> input = createWellSeparatedData();
            List<BeneficiaryCluster> result = fcm.getPrioritizedBeneficiaries(input, 10, 3);

            for (int i = 1; i < result.size(); i++) {
                assertTrue(result.get(i - 1).getFinalScore() >= result.get(i).getFinalScore(),
                        "Result should be sorted by score descending at index " + i);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Custom Parameters
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Custom FCM Parameters")
    class CustomParameterTests {

        @Test
        @DisplayName("Custom fuzziness m=1.5 still produces valid clusters")
        void customFuzziness() {
            FCMAidDistribution customFcm = new FCMAidDistribution(1.5, 1e-5, 300);
            List<BeneficiaryCluster> input = createWellSeparatedData();
            List<BeneficiaryCluster> result = customFcm.clusterAllBeneficiaries(input, 3);

            assertFalse(result.isEmpty());
            for (BeneficiaryCluster b : result) {
                assertTrue(b.getCluster() >= 0 && b.getCluster() <= 2);
            }
        }

        @Test
        @DisplayName("Very low maxIterations (5) still assigns clusters")
        void lowMaxIterations() {
            FCMAidDistribution fastFcm = new FCMAidDistribution(2.0, 1e-5, 5);
            List<BeneficiaryCluster> input = createWellSeparatedData();
            List<BeneficiaryCluster> result = fastFcm.clusterAllBeneficiaries(input, 3);

            assertFalse(result.isEmpty());
            for (BeneficiaryCluster b : result) {
                assertTrue(b.getCluster() >= 0);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Identical Scores Edge Case
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("All identical scores are handled without errors")
    void allIdenticalScores() {
        List<BeneficiaryCluster> input = createBeneficiaries(0.5, 0.5, 0.5, 0.5, 0.5);

        assertDoesNotThrow(() -> fcm.clusterAllBeneficiaries(input, 3),
                "FCM should handle all identical scores without exception");
    }

    @Test
    @DisplayName("Two beneficiaries with 3 clusters requested uses min(2, size)")
    void moreClustersRequestedThanBeneficiaries() {
        List<BeneficiaryCluster> input = createBeneficiaries(0.9, 0.1);
        List<BeneficiaryCluster> result = fcm.clusterAllBeneficiaries(input, 5);

        assertEquals(2, result.size());
        // Should still work and produce 2 clusters max
        Set<Integer> clusters = result.stream().map(BeneficiaryCluster::getCluster).collect(Collectors.toSet());
        assertTrue(clusters.size() <= 2);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BeneficiaryCluster model tests
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("BeneficiaryCluster Model")
    class BeneficiaryClusterModelTests {

        @Test
        @DisplayName("Default cluster is -1 before clustering")
        void defaultClusterIsNegativeOne() {
            BeneficiaryCluster bc = new BeneficiaryCluster(1, 0.75, "High");
            assertEquals(-1, bc.getCluster());
        }

        @Test
        @DisplayName("Default clusterPriorityLabel equals scoreCategory")
        void defaultLabelIsCategory() {
            BeneficiaryCluster bc = new BeneficiaryCluster(1, 0.75, "High");
            assertEquals("High", bc.getClusterPriorityLabel());
        }

        @Test
        @DisplayName("getPrimaryMembership returns 0 when no membership set")
        void primaryMembershipDefaultsToZero() {
            BeneficiaryCluster bc = new BeneficiaryCluster(1, 0.75, "High");
            assertEquals(0.0, bc.getPrimaryMembership());
        }

        @Test
        @DisplayName("toString contains all key fields")
        void toStringContainsFields() {
            BeneficiaryCluster bc = new BeneficiaryCluster(42, 0.867, "High");
            String str = bc.toString();

            assertTrue(str.contains("42"));
            assertTrue(str.contains("0.867"));
            assertTrue(str.contains("High"));
        }
    }
}

