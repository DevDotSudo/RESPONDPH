package com.ionres.respondph.aid;

import java.util.*;
import java.util.stream.Collectors;

public class KMeansAidDistribution {
    public static class BeneficiaryCluster {
        private final int beneficiaryId;
        private final double finalScore;
        private final String scoreCategory;
        private int cluster;
        private String clusterPriorityLabel;

        public BeneficiaryCluster(int beneficiaryId, double finalScore, String scoreCategory) {
            this.beneficiaryId = beneficiaryId;
            this.finalScore = finalScore;
            this.scoreCategory = scoreCategory;
            this.cluster = -1;
            this.clusterPriorityLabel = scoreCategory;
        }

        public int getBeneficiaryId() {
            return beneficiaryId;
        }

        public double getFinalScore() {
            return finalScore;
        }

        public int getCluster() {
            return cluster;
        }

        public void setCluster(int cluster) {
            this.cluster = cluster;
        }

        public String getScoreCategory() {
            return scoreCategory;
        }

        public String getClusterPriorityLabel() {
            return clusterPriorityLabel;
        }

        public void setClusterPriorityLabel(String label) {
            this.clusterPriorityLabel = label;
        }

        @Override
        public String toString() {
            return String.format("Beneficiary #%d [Score: %.3f, Category: %s, Cluster: %d, Priority: %s]",
                    beneficiaryId, finalScore, scoreCategory, cluster, clusterPriorityLabel);
        }
    }

    private final int maxIterations;
    private final double convergenceThreshold;

    public KMeansAidDistribution() {
        this.maxIterations = 100;
        this.convergenceThreshold = 0.001;
    }

    public KMeansAidDistribution(int maxIterations, double convergenceThreshold) {
        this.maxIterations = maxIterations;
        this.convergenceThreshold = convergenceThreshold;
    }

    public List<BeneficiaryCluster> clusterAllBeneficiaries(
            List<BeneficiaryCluster> beneficiaries,
            int numberOfClusters) {

        if (beneficiaries == null || beneficiaries.isEmpty()) {
            System.out.println("No beneficiaries provided for clustering");
            return new ArrayList<>();
        }

        int k = Math.min(numberOfClusters, beneficiaries.size());

        if (k < 2) {
            System.out.println("Not enough beneficiaries for clustering. Assigning single cluster.");
            for (BeneficiaryCluster b : beneficiaries) {
                b.setCluster(0);
                b.setClusterPriorityLabel("High Priority");
            }
            return beneficiaries.stream()
                    .sorted(Comparator.comparingDouble(BeneficiaryCluster::getFinalScore).reversed())
                    .collect(Collectors.toList());
        }

        System.out.println("========== K-MEANS CLUSTERING (PREVIEW ALL) ==========");
        System.out.println("Total Beneficiaries : " + beneficiaries.size());
        System.out.println("Number of Clusters  : " + k);

        performKMeansClustering(beneficiaries, k);

        assignClusterPriorityLabels(beneficiaries, k);

        System.out.println("======================================================");

        return beneficiaries.stream()
                .sorted(Comparator.comparingDouble(BeneficiaryCluster::getFinalScore).reversed())
                .collect(Collectors.toList());
    }

    public List<BeneficiaryCluster> getPrioritizedBeneficiaries(
            List<BeneficiaryCluster> beneficiaries,
            int availableQuantity,
            int numberOfClusters) {

        if (beneficiaries == null || beneficiaries.isEmpty()) {
            System.out.println("No beneficiaries provided for clustering");
            return new ArrayList<>();
        }

        int k = Math.min(numberOfClusters, beneficiaries.size());
        if (k < 2) {
            System.out.println("Not enough beneficiaries for clustering. Using simple sort.");
            return beneficiaries.stream()
                    .sorted(Comparator.comparingDouble(BeneficiaryCluster::getFinalScore).reversed())
                    .limit(availableQuantity)
                    .collect(Collectors.toList());
        }

        String distributionMode = availableQuantity >= beneficiaries.size()
                ? "(Sufficient Aid for All - Clustering for Priority Order)"
                : "(Limited Aid - Clustering for Priority Selection)";

        System.out.println("\n========== K-MEANS CLUSTERING ==========");
        System.out.println("Total Beneficiaries: " + beneficiaries.size());
        System.out.println("Available Quantity: " + availableQuantity);
        System.out.println("Distribution Mode: " + distributionMode);
        System.out.println("Number of Clusters: " + k);

        performKMeansClustering(beneficiaries, k);

        assignClusterPriorityLabels(beneficiaries, k);

        int highestPriorityCluster = findHighestPriorityCluster(beneficiaries, k);
        System.out.println("Highest Priority Cluster: " + highestPriorityCluster);

        List<BeneficiaryCluster> prioritized = selectPrioritizedBeneficiaries(
                beneficiaries, availableQuantity, highestPriorityCluster);

        System.out.println("Selected " + prioritized.size() + " beneficiaries for distribution");
        System.out.println("========================================");

        return prioritized;
    }

    private void assignClusterPriorityLabels(List<BeneficiaryCluster> beneficiaries, int k) {
        double[] clusterSums = new double[k];
        int[]    clusterCounts = new int[k];

        for (BeneficiaryCluster b : beneficiaries) {
            int c = b.getCluster();
            if (c >= 0 && c < k) {
                clusterSums[c]   += b.getFinalScore();
                clusterCounts[c] += 1;
            }
        }

        double[] clusterAvg = new double[k];
        for (int i = 0; i < k; i++) {
            clusterAvg[i] = clusterCounts[i] > 0 ? clusterSums[i] / clusterCounts[i] : 0.0;
        }

        List<Integer> sortedClusters = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            if (clusterCounts[i] > 0) sortedClusters.add(i);
        }
        sortedClusters.sort((a, b) -> Double.compare(clusterAvg[b], clusterAvg[a]));

        Map<Integer, Integer> oldToNewCluster = new HashMap<>();
        for (int rank = 0; rank < sortedClusters.size(); rank++) {
            int oldClusterIdx = sortedClusters.get(rank);
            oldToNewCluster.put(oldClusterIdx, rank);
        }

        // Reassign cluster numbers based on priority ranking
        for (BeneficiaryCluster b : beneficiaries) {
            int oldCluster = b.getCluster();
            int newCluster = oldToNewCluster.getOrDefault(oldCluster, oldCluster);
            b.setCluster(newCluster);
        }

        // Assign labels based on new cluster numbers
        Map<Integer, String> clusterLabel = new HashMap<>();
        clusterLabel.put(0, "High Priority");
        if (k >= 2) clusterLabel.put(1, "Moderate Priority");
        if (k >= 3) clusterLabel.put(2, "Low Priority");

        for (BeneficiaryCluster b : beneficiaries) {
            String label = clusterLabel.getOrDefault(b.getCluster(), b.getScoreCategory());
            b.setClusterPriorityLabel(label);
        }

        System.out.println("Cluster priority labels assigned:");
        for (int newIdx = 0; newIdx < k; newIdx++) {
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
                System.out.printf("  Cluster %d (avg %.3f) → %s%n", newIdx, clusterAvg[oldIdx], label);
            }
        }
    }

    private void performKMeansClustering(List<BeneficiaryCluster> beneficiaries, int k) {
        double[] centroids = initializeCentroidsKMeansPlusPlus(beneficiaries, k);

        boolean converged = false;
        int iteration = 0;

        while (!converged && iteration < maxIterations) {
            assignToClusters(beneficiaries, centroids);
            double[] newCentroids = calculateNewCentroids(beneficiaries, k);
            converged = hasConverged(centroids, newCentroids);
            centroids = newCentroids;
            iteration++;
        }

        System.out.println("K-means converged after " + iteration + " iterations");
        printClusterStatistics(beneficiaries, k);
    }

    private double[] initializeCentroidsKMeansPlusPlus(List<BeneficiaryCluster> beneficiaries, int k) {
        double[] centroids = new double[k];
        Random random = new Random(67);

        centroids[0] = beneficiaries.get(random.nextInt(beneficiaries.size())).getFinalScore();

        for (int i = 1; i < k; i++) {
            double[] distances = new double[beneficiaries.size()];
            double totalDistance = 0;

            for (int j = 0; j < beneficiaries.size(); j++) {
                double minDist = Double.MAX_VALUE;
                for (int c = 0; c < i; c++) {
                    double dist = Math.pow(beneficiaries.get(j).getFinalScore() - centroids[c], 2);
                    minDist = Math.min(minDist, dist);
                }
                distances[j] = minDist;
                totalDistance += minDist;
            }

            if (totalDistance > 0) {
                double threshold = random.nextDouble() * totalDistance;
                double sum = 0;
                for (int j = 0; j < beneficiaries.size(); j++) {
                    sum += distances[j];
                    if (sum >= threshold) {
                        centroids[i] = beneficiaries.get(j).getFinalScore();
                        break;
                    }
                }
            } else {
                centroids[i] = beneficiaries.get(random.nextInt(beneficiaries.size())).getFinalScore();
            }
        }

        return centroids;
    }

    private void assignToClusters(List<BeneficiaryCluster> beneficiaries, double[] centroids) {
        for (BeneficiaryCluster beneficiary : beneficiaries) {
            int nearestCluster = findNearestCentroid(beneficiary.getFinalScore(), centroids);
            beneficiary.setCluster(nearestCluster);
        }
    }

    private int findNearestCentroid(double score, double[] centroids) {
        int nearest = 0;
        double minDistance = Math.abs(score - centroids[0]);

        for (int i = 1; i < centroids.length; i++) {
            double distance = Math.abs(score - centroids[i]);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = i;
            }
        }

        return nearest;
    }

    private double[] calculateNewCentroids(List<BeneficiaryCluster> beneficiaries, int k) {
        double[] newCentroids = new double[k];
        int[] counts = new int[k];

        for (BeneficiaryCluster beneficiary : beneficiaries) {
            int cluster = beneficiary.getCluster();
            newCentroids[cluster] += beneficiary.getFinalScore();
            counts[cluster]++;
        }

        for (int i = 0; i < k; i++) {
            if (counts[i] > 0) {
                newCentroids[i] /= counts[i];
            }
        }

        return newCentroids;
    }

    private boolean hasConverged(double[] oldCentroids, double[] newCentroids) {
        for (int i = 0; i < oldCentroids.length; i++) {
            if (Math.abs(oldCentroids[i] - newCentroids[i]) > convergenceThreshold) {
                return false;
            }
        }
        return true;
    }

    private int findHighestPriorityCluster(List<BeneficiaryCluster> beneficiaries, int k) {
        // After reassignment, Cluster 0 is always the highest priority
        return 0;
    }

    private List<BeneficiaryCluster> selectPrioritizedBeneficiaries(
            List<BeneficiaryCluster> beneficiaries,
            int quantity,
            int highestPriorityCluster) {

        List<BeneficiaryCluster> prioritized = new ArrayList<>();

        List<BeneficiaryCluster> highPriority = beneficiaries.stream()
                .filter(b -> b.getCluster() == highestPriorityCluster)
                .sorted(Comparator.comparingDouble(BeneficiaryCluster::getFinalScore).reversed())
                .collect(Collectors.toList());

        prioritized.addAll(highPriority);

        if (prioritized.size() < quantity) {
            List<BeneficiaryCluster> remaining = beneficiaries.stream()
                    .filter(b -> b.getCluster() != highestPriorityCluster)
                    .sorted(Comparator.comparingDouble(BeneficiaryCluster::getFinalScore).reversed())
                    .collect(Collectors.toList());

            int needed = quantity - prioritized.size();
            int toAdd = Math.min(needed, remaining.size());
            prioritized.addAll(remaining.subList(0, toAdd));
        }

        return prioritized.subList(0, Math.min(quantity, prioritized.size()));
    }

    private void printClusterStatistics(List<BeneficiaryCluster> beneficiaries, int k) {
        System.out.println("\n--- Cluster Statistics ---");

        for (int i = 0; i < k; i++) {
            final int cluster = i;
            List<BeneficiaryCluster> clusterMembers = beneficiaries.stream()
                    .filter(b -> b.getCluster() == cluster)
                    .collect(Collectors.toList());

            if (!clusterMembers.isEmpty()) {
                double avgScore = clusterMembers.stream()
                        .mapToDouble(BeneficiaryCluster::getFinalScore)
                        .average()
                        .orElse(0.0);

                double minScore = clusterMembers.stream()
                        .mapToDouble(BeneficiaryCluster::getFinalScore)
                        .min()
                        .orElse(0.0);

                double maxScore = clusterMembers.stream()
                        .mapToDouble(BeneficiaryCluster::getFinalScore)
                        .max()
                        .orElse(0.0);

                System.out.printf("Cluster %d: %d members | Avg Score: %.3f | Range: [%.3f - %.3f]\n",
                        i, clusterMembers.size(), avgScore, minScore, maxScore);
            }
        }
        System.out.println("--------------------------\n");
    }
}