package com.ionres.respondph.aid;
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
            this.beneficiaryId        = beneficiaryId;
            this.finalScore           = finalScore;
            this.scoreCategory        = scoreCategory;
            this.cluster              = -1;
            this.membershipValues     = new double[0];
            this.clusterPriorityLabel = scoreCategory; // default until FCM runs
        }

        public int    getBeneficiaryId()    { return beneficiaryId; }
        public double getFinalScore()       { return finalScore; }
        public int    getCluster()          { return cluster; }
        public void   setCluster(int c)     { this.cluster = c; }
        public String getScoreCategory()    { return scoreCategory; }

        public double[] getMembershipValues()              { return membershipValues; }
        public void     setMembershipValues(double[] vals) { this.membershipValues = vals; }

        public String getClusterPriorityLabel()              { return clusterPriorityLabel; }
        public void   setClusterPriorityLabel(String label)  { this.clusterPriorityLabel = label; }

        public double getPrimaryMembership() {
            if (membershipValues == null || membershipValues.length == 0) return 0.0;
            int idx = (cluster < 0 || cluster >= membershipValues.length) ? 0 : cluster;
            return membershipValues[idx];
        }

        @Override
        public String toString() {
            return String.format(
                    "Beneficiary #%d [Score: %.3f, Category: %s, Cluster: %d, Priority: %s, Membership: %.3f]",
                    beneficiaryId, finalScore, scoreCategory,
                    cluster, clusterPriorityLabel, getPrimaryMembership());
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


    public List<BeneficiaryCluster> clusterAllBeneficiaries(
            List<BeneficiaryCluster> beneficiaries,
            int numberOfClusters) {

        if (beneficiaries == null || beneficiaries.isEmpty()) return new ArrayList<>();

        int c = Math.min(numberOfClusters, beneficiaries.size());
        if (c < 2) {
            return beneficiaries.stream()
                    .sorted(Comparator.comparingDouble(BeneficiaryCluster::getFinalScore).reversed())
                    .collect(Collectors.toList());
        }

        System.out.println("========== FCM CLUSTER-ALL ==========");
        System.out.println("Total Beneficiaries : " + beneficiaries.size());
        System.out.println("Number of Clusters  : " + c);
        System.out.printf ("Fuzziness (m)       : %.1f%n", fuzzinessM);
        System.out.println("=====================================");

        performFCM(beneficiaries, c);
        assignClusterPriorityLabels(beneficiaries, c);
        printClusterStatistics(beneficiaries, c);

        return beneficiaries.stream()
                .sorted(Comparator.comparingDouble(BeneficiaryCluster::getFinalScore).reversed())
                .collect(Collectors.toList());
    }


    public List<BeneficiaryCluster> getPrioritizedBeneficiaries(
            List<BeneficiaryCluster> beneficiaries,
            int availableQuantity,
            int numberOfClusters) {

        if (beneficiaries == null || beneficiaries.isEmpty()) {
            System.out.println("[FCM] No beneficiaries provided.");
            return new ArrayList<>();
        }

        int c = Math.min(numberOfClusters, beneficiaries.size());
        if (c < 2) {
            System.out.println("[FCM] Not enough beneficiaries for clustering – using simple sort.");
            return beneficiaries.stream()
                    .sorted(Comparator.comparingDouble(BeneficiaryCluster::getFinalScore).reversed())
                    .limit(availableQuantity)
                    .collect(Collectors.toList());
        }

        String distributionMode = availableQuantity >= beneficiaries.size()
                ? "(Sufficient Aid for All - Clustering for Priority Order)"
                : "(Limited Aid - Clustering for Priority Selection)";

        System.out.println("\n========== FCM CLUSTERING ==========");
        System.out.println("Total Beneficiaries : " + beneficiaries.size());
        System.out.println("Available Quantity  : " + availableQuantity);
        System.out.println("Distribution Mode   : " + distributionMode);
        System.out.println("Number of Clusters  : " + c);
        System.out.printf ("Fuzziness (m)       : %.1f%n", fuzzinessM);

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
        double[][] U = new double[n][c];

        for (int i = 0; i < n; i++) {
            double rowSum = 0;
            for (int j = 0; j < c; j++) {
                U[i][j] = rng.nextDouble() + 1e-9;  // avoid exact zero
                rowSum  += U[i][j];
            }
            for (int j = 0; j < c; j++) U[i][j] /= rowSum;
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
                double uijM  = Math.pow(U[i][j], fuzzinessM);
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

            int exactCluster = -1;
            for (int j = 0; j < c; j++) {
                if (Math.abs(xi - centres[j]) < 1e-12) {
                    exactCluster = j;
                    break;
                }
            }

            if (exactCluster >= 0) {
                for (int j = 0; j < c; j++) {
                    U[i][j] = (j == exactCluster) ? 1.0 : 0.0;
                }
            } else {
                double[] dist = new double[c];
                for (int j = 0; j < c; j++) dist[j] = Math.abs(xi - centres[j]);

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
                if (Math.abs(newU[i][j] - oldU[i][j]) >= convergenceThreshold) return false;
            }
        }
        return true;
    }

    private void hardAssignClusters(List<BeneficiaryCluster> beneficiaries,
                                    double[][] U, int c) {
        int n = beneficiaries.size();
        for (int i = 0; i < n; i++) {
            BeneficiaryCluster b = beneficiaries.get(i);

            double[] membership = Arrays.copyOf(U[i], c);
            b.setMembershipValues(membership);

            int dominant = 0;
            for (int j = 1; j < c; j++) {
                if (membership[j] > membership[dominant]) dominant = j;
            }
            b.setCluster(dominant);
        }
    }


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

        // Sort clusters by average score (highest to lowest)
        List<Integer> sorted = new ArrayList<>();
        for (int i = 0; i < c; i++) {
            if (counts[i] > 0) sorted.add(i);
        }
        sorted.sort((a, b) -> Double.compare(avg[b], avg[a]));

        // Create mapping: old cluster index → new cluster index
        // Highest avg score cluster → 0 (High Priority)
        // Middle avg score cluster → 1 (Moderate Priority)
        // Lowest avg score cluster → 2 (Low Priority)
        Map<Integer, Integer> oldToNewCluster = new HashMap<>();
        for (int rank = 0; rank < sorted.size(); rank++) {
            int oldClusterIdx = sorted.get(rank);
            oldToNewCluster.put(oldClusterIdx, rank);
        }

        // Reassign cluster numbers based on priority ranking
        for (BeneficiaryCluster b : beneficiaries) {
            int oldCluster = b.getCluster();
            int newCluster = oldToNewCluster.getOrDefault(oldCluster, oldCluster);
            b.setCluster(newCluster);

            // Also remap the membership values array to match new cluster order
            double[] oldMembership = b.getMembershipValues();
            if (oldMembership != null && oldMembership.length == c) {
                double[] newMembership = new double[c];
                for (Map.Entry<Integer, Integer> entry : oldToNewCluster.entrySet()) {
                    int oldIdx = entry.getKey();
                    int newIdx = entry.getValue();
                    newMembership[newIdx] = oldMembership[oldIdx];
                }
                b.setMembershipValues(newMembership);
            }
        }

        // Assign labels based on new cluster numbers
        Map<Integer, String> clusterLabel = new HashMap<>();
        clusterLabel.put(0, "High Priority");
        if (c >= 2) clusterLabel.put(1, "Moderate Priority");
        if (c >= 3) clusterLabel.put(2, "Low Priority");

        for (BeneficiaryCluster b : beneficiaries) {
            String label = clusterLabel.getOrDefault(b.getCluster(), b.getScoreCategory());
            b.setClusterPriorityLabel(label);
        }

        System.out.println("[FCM] Cluster priority labels assigned:");
        for (int newIdx = 0; newIdx < c; newIdx++) {
            String label = clusterLabel.get(newIdx);
            // Find the old cluster that mapped to this new index
            int oldIdx = -1;
            for (Map.Entry<Integer, Integer> entry : oldToNewCluster.entrySet()) {
                if (entry.getValue() == newIdx) {
                    oldIdx = entry.getKey();
                    break;
                }
            }
            if (oldIdx >= 0) {
                System.out.printf("  Cluster %d (avg %.3f) → %s%n", newIdx, avg[oldIdx], label);
            }
        }
    }

    private int findHighestPriorityCluster(List<BeneficiaryCluster> beneficiaries, int c) {
        // After reassignment, Cluster 0 is always the highest priority
        return 0;
    }


    private List<BeneficiaryCluster> selectPrioritizedBeneficiaries(
            List<BeneficiaryCluster> beneficiaries,
            int quantity,
            int highestPriorityCluster) {

        List<BeneficiaryCluster> result = new ArrayList<>();

        beneficiaries.stream()
                .filter(b -> b.getCluster() == highestPriorityCluster)
                .sorted(Comparator.comparingDouble(BeneficiaryCluster::getFinalScore).reversed())
                .limit(quantity)
                .forEach(result::add);

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



    private void printClusterStatistics(List<BeneficiaryCluster> beneficiaries, int c) {
        System.out.println("\n--- FCM Cluster Statistics ---");
        for (int i = 0; i < c; i++) {
            final int ci = i;
            List<BeneficiaryCluster> members = beneficiaries.stream()
                    .filter(b -> b.getCluster() == ci)
                    .collect(Collectors.toList());

            if (!members.isEmpty()) {
                double avgScore = members.stream()
                        .mapToDouble(BeneficiaryCluster::getFinalScore).average().orElse(0);
                double min = members.stream()
                        .mapToDouble(BeneficiaryCluster::getFinalScore).min().orElse(0);
                double max = members.stream()
                        .mapToDouble(BeneficiaryCluster::getFinalScore).max().orElse(0);
                double avgM = members.stream()
                        .mapToDouble(BeneficiaryCluster::getPrimaryMembership).average().orElse(0);
                System.out.printf(
                        "Cluster %d: %d members | Avg Score: %.3f | Range: [%.3f – %.3f] | Avg Membership: %.3f%n",
                        i, members.size(), avgScore, min, max, avgM);
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