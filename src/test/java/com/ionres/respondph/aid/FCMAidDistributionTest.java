package com.ionres.respondph.aid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FCMAidDistribution")
class FCMAidDistributionTest {

    private FCMAidDistribution fcm;

    @BeforeEach
    void setUp() {
        fcm = new FCMAidDistribution();
    }

    // ─── BeneficiaryCluster inner class ───────────────────────────────────────

    @Nested
    @DisplayName("BeneficiaryCluster")
    class BeneficiaryClusterTests {

        @Test
        @DisplayName("constructor sets fields correctly")
        void constructorFields() {
            FCMAidDistribution.BeneficiaryCluster bc =
                    new FCMAidDistribution.BeneficiaryCluster(5, 0.72, "Moderate Vulnerability");
            assertEquals(5, bc.getBeneficiaryId());
            assertEquals(0.72, bc.getFinalScore(), 0.001);
            assertEquals("Moderate Vulnerability", bc.getScoreCategory());
            assertEquals(-1, bc.getCluster());
            assertEquals("Moderate Vulnerability", bc.getClusterPriorityLabel());
            assertEquals(0, bc.getMembershipValues().length);
        }

        @Test
        @DisplayName("getPrimaryMembership returns 0 for empty membership")
        void emptyMembership() {
            FCMAidDistribution.BeneficiaryCluster bc =
                    new FCMAidDistribution.BeneficiaryCluster(1, 0.5, "Med");
            assertEquals(0.0, bc.getPrimaryMembership());
        }

        @Test
        @DisplayName("getPrimaryMembership returns correct value after assignment")
        void assignedMembership() {
            FCMAidDistribution.BeneficiaryCluster bc =
                    new FCMAidDistribution.BeneficiaryCluster(1, 0.5, "Med");
            bc.setCluster(1);
            bc.setMembershipValues(new double[]{0.3, 0.7});
            assertEquals(0.7, bc.getPrimaryMembership(), 0.001);
        }

        @Test
        @DisplayName("toString contains all relevant fields")
        void toStringFormat() {
            FCMAidDistribution.BeneficiaryCluster bc =
                    new FCMAidDistribution.BeneficiaryCluster(10, 0.85, "High");
            bc.setCluster(0);
            bc.setClusterPriorityLabel("High Priority");
            bc.setMembershipValues(new double[]{0.9, 0.1});
            String str = bc.toString();
            assertTrue(str.contains("10"));
            assertTrue(str.contains("0.850"));
            assertTrue(str.contains("High Priority"));
            assertTrue(str.contains("Membership"));
        }
    }

    // ─── clusterAllBeneficiaries ──────────────────────────────────────────────

    @Nested
    @DisplayName("clusterAllBeneficiaries()")
    class ClusterAll {

        @Test
        @DisplayName("returns empty list for null input")
        void nullInput() {
            List<FCMAidDistribution.BeneficiaryCluster> result =
                    fcm.clusterAllBeneficiaries(null, 3);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty list for empty input")
        void emptyInput() {
            List<FCMAidDistribution.BeneficiaryCluster> result =
                    fcm.clusterAllBeneficiaries(new ArrayList<>(), 3);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("single beneficiary returns sorted by score")
        void singleBeneficiary() {
            List<FCMAidDistribution.BeneficiaryCluster> input = List.of(
                    new FCMAidDistribution.BeneficiaryCluster(1, 0.9, "High")
            );
            List<FCMAidDistribution.BeneficiaryCluster> result =
                    fcm.clusterAllBeneficiaries(input, 3);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("all beneficiaries get cluster assignments")
        void allAssigned() {
            List<FCMAidDistribution.BeneficiaryCluster> input = createTestBeneficiaries();
            List<FCMAidDistribution.BeneficiaryCluster> result =
                    fcm.clusterAllBeneficiaries(input, 3);

            assertEquals(input.size(), result.size());
            for (FCMAidDistribution.BeneficiaryCluster b : result) {
                assertTrue(b.getCluster() >= 0, "Cluster should be non-negative");
                assertNotNull(b.getClusterPriorityLabel());
                assertNotNull(b.getMembershipValues());
                assertTrue(b.getMembershipValues().length > 0, "Should have membership values");
            }
        }

        @Test
        @DisplayName("results are sorted by score descending")
        void sortedDescending() {
            List<FCMAidDistribution.BeneficiaryCluster> input = createTestBeneficiaries();
            List<FCMAidDistribution.BeneficiaryCluster> result =
                    fcm.clusterAllBeneficiaries(input, 3);

            for (int i = 1; i < result.size(); i++) {
                assertTrue(result.get(i - 1).getFinalScore() >= result.get(i).getFinalScore());
            }
        }

        @Test
        @DisplayName("membership values sum to approximately 1.0")
        void membershipsSumToOne() {
            List<FCMAidDistribution.BeneficiaryCluster> input = createTestBeneficiaries();
            List<FCMAidDistribution.BeneficiaryCluster> result =
                    fcm.clusterAllBeneficiaries(input, 3);

            for (FCMAidDistribution.BeneficiaryCluster b : result) {
                double sum = 0;
                for (double v : b.getMembershipValues()) {
                    sum += v;
                }
                assertEquals(1.0, sum, 0.01,
                        "Membership values should sum to ~1.0 for beneficiary " + b.getBeneficiaryId());
            }
        }

        @Test
        @DisplayName("highest-scored beneficiary gets High Priority")
        void highScoreGetsHighPriority() {
            List<FCMAidDistribution.BeneficiaryCluster> input = createTestBeneficiaries();
            List<FCMAidDistribution.BeneficiaryCluster> result =
                    fcm.clusterAllBeneficiaries(input, 3);

            FCMAidDistribution.BeneficiaryCluster highest = result.get(0);
            assertEquals("High Priority", highest.getClusterPriorityLabel());
        }
    }

    // ─── getPrioritizedBeneficiaries ──────────────────────────────────────────

    @Nested
    @DisplayName("getPrioritizedBeneficiaries()")
    class GetPrioritized {

        @Test
        @DisplayName("returns empty list for null input")
        void nullInput() {
            assertTrue(fcm.getPrioritizedBeneficiaries(null, 5, 3).isEmpty());
        }

        @Test
        @DisplayName("returns empty list for empty input")
        void emptyInput() {
            assertTrue(fcm.getPrioritizedBeneficiaries(new ArrayList<>(), 5, 3).isEmpty());
        }

        @Test
        @DisplayName("limits output to available quantity")
        void limitsToQuantity() {
            List<FCMAidDistribution.BeneficiaryCluster> input = createTestBeneficiaries();
            int quantity = 3;
            List<FCMAidDistribution.BeneficiaryCluster> result =
                    fcm.getPrioritizedBeneficiaries(input, quantity, 3);
            assertTrue(result.size() <= quantity);
        }

        @Test
        @DisplayName("returns all when quantity exceeds count")
        void quantityExceedsCount() {
            List<FCMAidDistribution.BeneficiaryCluster> input = createTestBeneficiaries();
            List<FCMAidDistribution.BeneficiaryCluster> result =
                    fcm.getPrioritizedBeneficiaries(input, 100, 3);
            assertEquals(input.size(), result.size());
        }

        @Test
        @DisplayName("prioritizes high-score beneficiaries first")
        void highScoreFirst() {
            List<FCMAidDistribution.BeneficiaryCluster> input = createTestBeneficiaries();
            List<FCMAidDistribution.BeneficiaryCluster> result =
                    fcm.getPrioritizedBeneficiaries(input, 3, 3);

            double avgSelected = result.stream()
                    .mapToDouble(FCMAidDistribution.BeneficiaryCluster::getFinalScore)
                    .average().orElse(0);
            double avgAll = input.stream()
                    .mapToDouble(FCMAidDistribution.BeneficiaryCluster::getFinalScore)
                    .average().orElse(0);
            assertTrue(avgSelected >= avgAll);
        }
    }

    // ─── Custom constructor ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Custom parameters")
    class CustomParams {

        @Test
        @DisplayName("accepts custom fuzziness, convergence, and maxIterations")
        void customConstructor() {
            FCMAidDistribution custom = new FCMAidDistribution(2.5, 1e-4, 50);
            List<FCMAidDistribution.BeneficiaryCluster> input = createTestBeneficiaries();
            List<FCMAidDistribution.BeneficiaryCluster> result =
                    custom.clusterAllBeneficiaries(input, 3);
            assertFalse(result.isEmpty());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private List<FCMAidDistribution.BeneficiaryCluster> createTestBeneficiaries() {
        List<FCMAidDistribution.BeneficiaryCluster> list = new ArrayList<>();
        list.add(new FCMAidDistribution.BeneficiaryCluster(1, 0.95, "High Vulnerability"));
        list.add(new FCMAidDistribution.BeneficiaryCluster(2, 0.90, "High Vulnerability"));
        list.add(new FCMAidDistribution.BeneficiaryCluster(3, 0.85, "High Vulnerability"));
        list.add(new FCMAidDistribution.BeneficiaryCluster(4, 0.55, "Moderate Vulnerability"));
        list.add(new FCMAidDistribution.BeneficiaryCluster(5, 0.50, "Moderate Vulnerability"));
        list.add(new FCMAidDistribution.BeneficiaryCluster(6, 0.45, "Moderate Vulnerability"));
        list.add(new FCMAidDistribution.BeneficiaryCluster(7, 0.15, "Low Vulnerability"));
        list.add(new FCMAidDistribution.BeneficiaryCluster(8, 0.10, "Low Vulnerability"));
        list.add(new FCMAidDistribution.BeneficiaryCluster(9, 0.05, "Low Vulnerability"));
        return list;
    }
}

