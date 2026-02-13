package com.ionres.respondph.aid;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.*;
import java.util.stream.Collectors;


public class FCMAidDistribution {


    public static class BeneficiaryCluster {

        private final int    beneficiaryId;
        private final double finalScore;
        private final String scoreCategory;

        private int    cluster;

        private double[] membershipValues;

        private String clusterPriorityLabel;

        public BeneficiaryCluster(int beneficiaryId, double finalScore, String scoreCategory) {
            this.beneficiaryId       = beneficiaryId;
            this.finalScore          = finalScore;
            this.scoreCategory       = scoreCategory;
            this.cluster             = -1;
            this.membershipValues    = new double[0];
            this.clusterPriorityLabel = scoreCategory; // default until FCM runs
        }


        public int    getBeneficiaryId()    { return beneficiaryId; }
        public double getFinalScore()       { return finalScore; }
        public int    getCluster()          { return cluster; }
        public void   setCluster(int c)     { this.cluster = c; }
        public String getScoreCategory()    { return scoreCategory; }

        public double[] getMembershipValues()              { return membershipValues; }
        public void     setMembershipValues(double[] vals) { this.membershipValues = vals; }

        public String getClusterPriorityLabel()             { return clusterPriorityLabel; }
        public void   setClusterPriorityLabel(String label) { this.clusterPriorityLabel = label; }

        public double getPrimaryMembership() {
            if (membershipValues == null || membershipValues.length == 0) return 0.0;
            return membershipValues[cluster < 0 ? 0 : cluster];
        }

        @Override
        public String toString() {
            return String.format(
                    "Beneficiary #%d [Score: %.3f, Category: %s, Cluster: %d, Priority: %s, Membership: %.3f]",
                    beneficiaryId, finalScore, scoreCategory,
                    cluster, clusterPriorityLabel, getPrimaryMembership()
            );
        }
    }


    private final double fuzzinessM;

    private final double convergenceThreshold;

    private final int    maxIterations;


    public FCMAidDistribution() {
        this.fuzzinessM           = 2.0;
        this.convergenceThreshold = 1e-5;
        this.maxIterations        = 300;
    }


    public FCMAidDistribution(double fuzzinessM,
                              double convergenceThreshold,
                              int maxIterations) {
        this.fuzzinessM           = fuzzinessM;
        this.convergenceThreshold = convergenceThreshold;
        this.maxIterations        = maxIterations;
    }



    public List<BeneficiaryCluster> getPrioritizedBeneficiaries(
            List<BeneficiaryCluster> beneficiaries,
            int availableQuantity,
            int numberOfClusters) {

        if (beneficiaries == null || beneficiaries.isEmpty()) {
            System.out.println("[FCM] No beneficiaries provided.");
            return new ArrayList<>();
        }

        // If we have enough aid for everyone, just sort by score and return all
        if (availableQuantity >= beneficiaries.size()) {
            System.out.println("[FCM] Sufficient aid for all – returning all sorted by score.");
            return beneficiaries.stream()
                    .sorted(Comparator.comparingDouble(BeneficiaryCluster::getFinalScore).reversed())
                    .collect(Collectors.toList());
        }

        int c = Math.min(numberOfClusters, beneficiaries.size());
        if (c < 2) {
            System.out.println("[FCM] Not enough beneficiaries for clustering – using simple sort.");
            return beneficiaries.stream()
                    .sorted(Comparator.comparingDouble(BeneficiaryCluster::getFinalScore).reversed())
                    .limit(availableQuantity)
                    .collect(Collectors.toList());
        }

        System.out.println("========== FCM CLUSTERING ==========");
        System.out.println("Total Beneficiaries : " + beneficiaries.size());
        System.out.println("Available Quantity  : " + availableQuantity);
        System.out.println("Number of Clusters  : " + c);
        System.out.printf ("Fuzziness (m)       : %.1f%n", fuzzinessM);
        System.out.println("=====================================");

        performFCM(beneficiaries, c);

        assignClusterPriorityLabels(beneficiaries, c);

        int highestPriorityCluster = findHighestPriorityCluster(beneficiaries, c);
        System.out.println("[FCM] Highest Priority Cluster: " + highestPriorityCluster);

        List<BeneficiaryCluster> prioritized =
                selectPrioritizedBeneficiaries(beneficiaries, availableQuantity, highestPriorityCluster);

        System.out.println("[FCM] Selected " + prioritized.size() + " beneficiaries.");
        System.out.println("=====================================");
        return prioritized;
    }


    private void performFCM(List<BeneficiaryCluster> beneficiaries, int c) {
        int n = beneficiaries.size();

        double[][] U = initMembershipMatrix(n, c);

        double[][] prevU;
        int iteration = 0;

        while (iteration < maxIterations) {
            prevU = deepCopy(U);

            double[] centres = computeClusterCentres(beneficiaries, U, c);

            U = updateMembershipMatrix(beneficiaries, centres, c);

            if (hasConverged(prevU, U)) {
                System.out.println("[FCM] Converged after " + (iteration + 1) + " iterations.");
                break;
            }
            iteration++;
        }

        if (iteration == maxIterations) {
            System.out.println("[FCM] Reached max iterations (" + maxIterations + ").");
        }

        hardAssignClusters(beneficiaries, U, c);
        printClusterStatistics(beneficiaries, c);
    }


    private double[][] initMembershipMatrix(int n, int c) {
        Random rng = new Random();
        // Build raw random values via Commons Math matrix
        RealMatrix raw = new Array2DRowRealMatrix(n, c);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < c; j++) {
                raw.setEntry(i, j, rng.nextDouble());
            }
        }
        // Normalise each row so it sums to 1
        double[][] U = new double[n][c];
        for (int i = 0; i < n; i++) {
            double rowSum = 0;
            for (int j = 0; j < c; j++) rowSum += raw.getEntry(i, j);
            for (int j = 0; j < c; j++) U[i][j] = raw.getEntry(i, j) / rowSum;
        }
        return U;
    }


    private double[] computeClusterCentres(List<BeneficiaryCluster> beneficiaries,
                                           double[][] U, int c) {
        int n = beneficiaries.size();
        double[] centres   = new double[c];
        double[] weightSum = new double[c];

        for (int i = 0; i < n; i++) {
            double xi = beneficiaries.get(i).getFinalScore();
            for (int j = 0; j < c; j++) {
                double uijM = Math.pow(U[i][j], fuzzinessM);
                centres[j]   += uijM * xi;
                weightSum[j] += uijM;
            }
        }
        for (int j = 0; j < c; j++) {
            centres[j] = (weightSum[j] > 0) ? centres[j] / weightSum[j] : 0.0;
        }
        return centres;
    }


    private double[][] updateMembershipMatrix(List<BeneficiaryCluster> beneficiaries,
                                              double[] centres, int c) {
        int n = beneficiaries.size();
        double expo = 2.0 / (fuzzinessM - 1.0);
        double[][] U = new double[n][c];

        for (int i = 0; i < n; i++) {
            double xi = beneficiaries.get(i).getFinalScore();

            // Detect if the point coincides with any centre
            int exactCluster = -1;
            for (int j = 0; j < c; j++) {
                if (Math.abs(xi - centres[j]) < 1e-12) {
                    exactCluster = j;
                    break;
                }
            }

            if (exactCluster >= 0) {
                // Hard membership: 1 for the exact cluster, 0 for others
                for (int j = 0; j < c; j++) {
                    U[i][j] = (j == exactCluster) ? 1.0 : 0.0;
                }
            } else {
                // Standard FCM membership update
                double[] dist = new double[c];
                for (int j = 0; j < c; j++) {
                    dist[j] = Math.abs(xi - centres[j]);
                }
                for (int j = 0; j < c; j++) {
                    double sum = 0;
                    for (int k = 0; k < c; k++) {
                        sum += Math.pow(dist[j] / dist[k], expo);
                    }
                    U[i][j] = 1.0 / sum;
                }
            }
        }
        return U;
    }


    private boolean hasConverged(double[][] oldU, double[][] newU) {
        for (int i = 0; i < oldU.length; i++) {
            for (int j = 0; j < oldU[i].length; j++) {
                if (Math.abs(newU[i][j] - oldU[i][j]) >= convergenceThreshold) {
                    return false;
                }
            }
        }
        return true;
    }


    private void hardAssignClusters(List<BeneficiaryCluster> beneficiaries,
                                    double[][] U, int c) {
        int n = beneficiaries.size();
        for (int i = 0; i < n; i++) {
            BeneficiaryCluster b = beneficiaries.get(i);

            // Copy membership row
            double[] membership = Arrays.copyOf(U[i], c);
            b.setMembershipValues(membership);

            // Find dominant cluster
            int dominant = 0;
            for (int j = 1; j < c; j++) {
                if (membership[j] > membership[dominant]) dominant = j;
            }
            b.setCluster(dominant);
        }
    }

    // ─── Priority labelling (mirrors KMeansAidDistribution logic) ────────────

    /**
     * Rank clusters by their average score (descending) and stamp each
     * beneficiary with "High Priority", "Medium Priority", or "Low Priority".
     */
    private void assignClusterPriorityLabels(List<BeneficiaryCluster> beneficiaries, int c) {
        double[] sums   = new double[c];
        int[]    counts = new int[c];

        for (BeneficiaryCluster b : beneficiaries) {
            int ci = b.getCluster();
            if (ci >= 0 && ci < c) {
                sums[ci]   += b.getFinalScore();
                counts[ci] += 1;
            }
        }

        double[] avg = new double[c];
        for (int i = 0; i < c; i++) {
            avg[i] = (counts[i] > 0) ? sums[i] / counts[i] : 0.0;
        }

        // Sort cluster indices by average score descending
        List<Integer> sorted = new ArrayList<>();
        for (int i = 0; i < c; i++) {
            if (counts[i] > 0) sorted.add(i);
        }
        sorted.sort((a, b) -> Double.compare(avg[b], avg[a]));

        Map<Integer, String> labelMap = new HashMap<>();
        for (int rank = 0; rank < sorted.size(); rank++) {
            int idx = sorted.get(rank);
            if (rank == 0) {
                labelMap.put(idx, "High Priority");
            } else if (rank == sorted.size() - 1 && sorted.size() > 1) {
                labelMap.put(idx, "Low Priority");
            } else {
                labelMap.put(idx, "Medium Priority");
            }
        }

        for (BeneficiaryCluster b : beneficiaries) {
            b.setClusterPriorityLabel(
                    labelMap.getOrDefault(b.getCluster(), b.getScoreCategory())
            );
        }

        System.out.println("[FCM] Cluster priority labels:");
        labelMap.forEach((idx, lbl) ->
                System.out.printf("  Cluster %d (avg %.3f) → %s%n", idx, avg[idx], lbl));
    }

    /** Returns the cluster index whose average beneficiary score is highest. */
    private int findHighestPriorityCluster(List<BeneficiaryCluster> beneficiaries, int c) {
        double[] sums   = new double[c];
        int[]    counts = new int[c];

        for (BeneficiaryCluster b : beneficiaries) {
            sums[b.getCluster()]   += b.getFinalScore();
            counts[b.getCluster()] += 1;
        }

        int    best    = 0;
        double bestAvg = 0;
        for (int i = 0; i < c; i++) {
            if (counts[i] > 0) {
                double a = sums[i] / counts[i];
                if (a > bestAvg) { bestAvg = a; best = i; }
            }
        }
        return best;
    }

    /**
     * Selects up to {@code quantity} beneficiaries:
     * first from the highest-priority cluster (sorted by score desc),
     * then from remaining clusters (sorted by score desc) if needed.
     */
    private List<BeneficiaryCluster> selectPrioritizedBeneficiaries(
            List<BeneficiaryCluster> beneficiaries,
            int quantity,
            int highestPriorityCluster) {

        List<BeneficiaryCluster> result = new ArrayList<>();

        // High-priority cluster first
        beneficiaries.stream()
                .filter(b -> b.getCluster() == highestPriorityCluster)
                .sorted(Comparator.comparingDouble(BeneficiaryCluster::getFinalScore).reversed())
                .forEach(result::add);

        // Fill remaining slots from other clusters (highest score first)
        if (result.size() < quantity) {
            int needed = quantity - result.size();
            beneficiaries.stream()
                    .filter(b -> b.getCluster() != highestPriorityCluster)
                    .sorted(Comparator.comparingDouble(BeneficiaryCluster::getFinalScore).reversed())
                    .limit(needed)
                    .forEach(result::add);
        }

        return result.subList(0, Math.min(quantity, result.size()));
    }

    // ─── Diagnostics ─────────────────────────────────────────────────────────

    private void printClusterStatistics(List<BeneficiaryCluster> beneficiaries, int c) {
        System.out.println("\n--- FCM Cluster Statistics ---");
        for (int i = 0; i < c; i++) {
            final int ci = i;
            List<BeneficiaryCluster> members = beneficiaries.stream()
                    .filter(b -> b.getCluster() == ci)
                    .collect(Collectors.toList());

            if (!members.isEmpty()) {
                double avg  = members.stream().mapToDouble(BeneficiaryCluster::getFinalScore).average().orElse(0);
                double min  = members.stream().mapToDouble(BeneficiaryCluster::getFinalScore).min().orElse(0);
                double max  = members.stream().mapToDouble(BeneficiaryCluster::getFinalScore).max().orElse(0);
                double avgM = members.stream().mapToDouble(BeneficiaryCluster::getPrimaryMembership).average().orElse(0);
                System.out.printf(
                        "Cluster %d: %d members | Avg Score: %.3f | Range: [%.3f – %.3f] | Avg Membership: %.3f%n",
                        i, members.size(), avg, min, max, avgM
                );
            }
        }
        System.out.println("-------------------------------\n");
    }

    private double[][] deepCopy(double[][] src) {
        double[][] copy = new double[src.length][];
        for (int i = 0; i < src.length; i++) {
            copy[i] = Arrays.copyOf(src[i], src[i].length);
        }
        return copy;
    }
}