package com.ionres.respondph.aid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KMeansAidDistribution")
class KMeansAidDistributionTest {

    private KMeansAidDistribution kmeans;

    @BeforeEach
    void setUp() {
        kmeans = new KMeansAidDistribution();
    }

    // ─── BeneficiaryCluster inner class ───────────────────────────────────────

    @Nested
    @DisplayName("BeneficiaryCluster")
    class BeneficiaryClusterTests {

        @Test
        @DisplayName("constructor sets fields correctly")
        void constructorFields() {
            KMeansAidDistribution.BeneficiaryCluster bc =
                    new KMeansAidDistribution.BeneficiaryCluster(1, 0.85, "High Vulnerability");
            assertEquals(1, bc.getBeneficiaryId());
            assertEquals(0.85, bc.getFinalScore(), 0.001);
            assertEquals("High Vulnerability", bc.getScoreCategory());
            assertEquals(-1, bc.getCluster());
            assertEquals("High Vulnerability", bc.getClusterPriorityLabel());
        }

        @Test
        @DisplayName("setters work correctly")
        void setters() {
            KMeansAidDistribution.BeneficiaryCluster bc =
                    new KMeansAidDistribution.BeneficiaryCluster(1, 0.5, "Medium");
            bc.setCluster(2);
            bc.setClusterPriorityLabel("Low Priority");
            assertEquals(2, bc.getCluster());
            assertEquals("Low Priority", bc.getClusterPriorityLabel());
        }

        @Test
        @DisplayName("toString includes all fields")
        void toStringFormat() {
            KMeansAidDistribution.BeneficiaryCluster bc =
                    new KMeansAidDistribution.BeneficiaryCluster(10, 0.75, "High");
            bc.setCluster(0);
            bc.setClusterPriorityLabel("High Priority");
            String str = bc.toString();
            assertTrue(str.contains("10"));
            assertTrue(str.contains("0.750"));
            assertTrue(str.contains("High Priority"));
        }
    }

    // ─── clusterAllBeneficiaries ──────────────────────────────────────────────

    @Nested
    @DisplayName("clusterAllBeneficiaries()")
    class ClusterAll {

        @Test
        @DisplayName("returns empty list for null input")
        void nullInput() {
            List<KMeansAidDistribution.BeneficiaryCluster> result =
                    kmeans.clusterAllBeneficiaries(null, 3);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty list for empty input")
        void emptyInput() {
            List<KMeansAidDistribution.BeneficiaryCluster> result =
                    kmeans.clusterAllBeneficiaries(new ArrayList<>(), 3);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("single beneficiary gets cluster 0 and High Priority")
        void singleBeneficiary() {
            List<KMeansAidDistribution.BeneficiaryCluster> input = List.of(
                    new KMeansAidDistribution.BeneficiaryCluster(1, 0.9, "High")
            );
            List<KMeansAidDistribution.BeneficiaryCluster> result =
                    kmeans.clusterAllBeneficiaries(input, 3);
            assertEquals(1, result.size());
            assertEquals(0, result.get(0).getCluster());
            assertEquals("High Priority", result.get(0).getClusterPriorityLabel());
        }

        @Test
        @DisplayName("clusters are assigned to all beneficiaries")
        void allClustered() {
            List<KMeansAidDistribution.BeneficiaryCluster> input = createTestBeneficiaries();
            List<KMeansAidDistribution.BeneficiaryCluster> result =
                    kmeans.clusterAllBeneficiaries(input, 3);

            assertEquals(input.size(), result.size());
            for (KMeansAidDistribution.BeneficiaryCluster b : result) {
                assertTrue(b.getCluster() >= 0, "Cluster should be assigned");
                assertNotNull(b.getClusterPriorityLabel());
            }
        }

        @Test
        @DisplayName("results are sorted by score descending")
        void sortedDescending() {
            List<KMeansAidDistribution.BeneficiaryCluster> input = createTestBeneficiaries();
            List<KMeansAidDistribution.BeneficiaryCluster> result =
                    kmeans.clusterAllBeneficiaries(input, 3);

            for (int i = 1; i < result.size(); i++) {
                assertTrue(result.get(i - 1).getFinalScore() >= result.get(i).getFinalScore(),
                        "Results should be sorted by score descending");
            }
        }

        @Test
        @DisplayName("high-score beneficiaries get High Priority label")
        void highScoresGetHighPriority() {
            List<KMeansAidDistribution.BeneficiaryCluster> input = createTestBeneficiaries();
            List<KMeansAidDistribution.BeneficiaryCluster> result =
                    kmeans.clusterAllBeneficiaries(input, 3);

            // The highest-scored beneficiary should be in High Priority
            KMeansAidDistribution.BeneficiaryCluster highest = result.get(0);
            assertEquals("High Priority", highest.getClusterPriorityLabel());
        }

        @Test
        @DisplayName("number of clusters is capped at beneficiary count")
        void clustersCapped() {
            List<KMeansAidDistribution.BeneficiaryCluster> input = List.of(
                    new KMeansAidDistribution.BeneficiaryCluster(1, 0.9, "High"),
                    new KMeansAidDistribution.BeneficiaryCluster(2, 0.3, "Low")
            );
            List<KMeansAidDistribution.BeneficiaryCluster> result =
                    kmeans.clusterAllBeneficiaries(input, 10);
            assertEquals(2, result.size());
        }
    }

    // ─── getPrioritizedBeneficiaries ──────────────────────────────────────────

    @Nested
    @DisplayName("getPrioritizedBeneficiaries()")
    class GetPrioritized {

        @Test
        @DisplayName("returns empty list for null input")
        void nullInput() {
            assertTrue(kmeans.getPrioritizedBeneficiaries(null, 5, 3).isEmpty());
        }

        @Test
        @DisplayName("returns empty list for empty input")
        void emptyInput() {
            assertTrue(kmeans.getPrioritizedBeneficiaries(new ArrayList<>(), 5, 3).isEmpty());
        }

        @Test
        @DisplayName("limits output to available quantity")
        void limitsToQuantity() {
            List<KMeansAidDistribution.BeneficiaryCluster> input = createTestBeneficiaries();
            int quantity = 3;
            List<KMeansAidDistribution.BeneficiaryCluster> result =
                    kmeans.getPrioritizedBeneficiaries(input, quantity, 3);
            assertTrue(result.size() <= quantity);
        }

        @Test
        @DisplayName("returns all when quantity exceeds beneficiary count")
        void quantityExceedsCount() {
            List<KMeansAidDistribution.BeneficiaryCluster> input = createTestBeneficiaries();
            List<KMeansAidDistribution.BeneficiaryCluster> result =
                    kmeans.getPrioritizedBeneficiaries(input, 100, 3);
            assertEquals(input.size(), result.size());
        }

        @Test
        @DisplayName("prioritizes high-score beneficiaries first")
        void highScoreFirst() {
            List<KMeansAidDistribution.BeneficiaryCluster> input = createTestBeneficiaries();
            List<KMeansAidDistribution.BeneficiaryCluster> result =
                    kmeans.getPrioritizedBeneficiaries(input, 3, 3);

            // The first selected should have high scores
            double avgSelectedScore = result.stream()
                    .mapToDouble(KMeansAidDistribution.BeneficiaryCluster::getFinalScore)
                    .average().orElse(0);
            double avgAllScore = input.stream()
                    .mapToDouble(KMeansAidDistribution.BeneficiaryCluster::getFinalScore)
                    .average().orElse(0);
            assertTrue(avgSelectedScore >= avgAllScore,
                    "Selected beneficiaries should have higher average scores");
        }
    }

    // ─── Custom constructor ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Custom parameters")
    class CustomParams {

        @Test
        @DisplayName("accepts custom maxIterations and convergenceThreshold")
        void customConstructor() {
            KMeansAidDistribution custom = new KMeansAidDistribution(50, 0.01);
            List<KMeansAidDistribution.BeneficiaryCluster> input = createTestBeneficiaries();
            List<KMeansAidDistribution.BeneficiaryCluster> result =
                    custom.clusterAllBeneficiaries(input, 3);
            assertFalse(result.isEmpty());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private List<KMeansAidDistribution.BeneficiaryCluster> createTestBeneficiaries() {
        List<KMeansAidDistribution.BeneficiaryCluster> list = new ArrayList<>();
        // High vulnerability group
        list.add(new KMeansAidDistribution.BeneficiaryCluster(1, 0.95, "High Vulnerability"));
        list.add(new KMeansAidDistribution.BeneficiaryCluster(2, 0.90, "High Vulnerability"));
        list.add(new KMeansAidDistribution.BeneficiaryCluster(3, 0.88, "High Vulnerability"));
        // Medium vulnerability group
        list.add(new KMeansAidDistribution.BeneficiaryCluster(4, 0.55, "Moderate Vulnerability"));
        list.add(new KMeansAidDistribution.BeneficiaryCluster(5, 0.50, "Moderate Vulnerability"));
        list.add(new KMeansAidDistribution.BeneficiaryCluster(6, 0.48, "Moderate Vulnerability"));
        // Low vulnerability group
        list.add(new KMeansAidDistribution.BeneficiaryCluster(7, 0.15, "Low Vulnerability"));
        list.add(new KMeansAidDistribution.BeneficiaryCluster(8, 0.10, "Low Vulnerability"));
        list.add(new KMeansAidDistribution.BeneficiaryCluster(9, 0.08, "Low Vulnerability"));
        return list;
    }
}

