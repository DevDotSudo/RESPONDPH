package com.ionres.respondph.aid;

import com.ionres.respondph.aid.KMeansAidDistribution.BeneficiaryCluster;
import com.ionres.respondph.database.DBConnection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class AidServiceImpl implements AidService {

    private final AidDAO aidDAO;
    private final KMeansAidDistribution kMeans;

    public AidServiceImpl() {
        this.aidDAO = new AidDAOImpl(DBConnection.getInstance());
        this.kMeans = new KMeansAidDistribution();
    }

    public AidServiceImpl(AidDAO aidDAO) {
        this.aidDAO = aidDAO;
        this.kMeans = new KMeansAidDistribution();
    }

    @Override
    public int distributeAidWithKMeans(String aidName, int aidTypeId, int disasterId,
                                       int availableQuantity, int quantityPerBeneficiary, double costPerUnit,
                                       String provider, int numberOfClusters) {

        String disasterContext = disasterId > 0 ? "for disaster #" + disasterId : "(General Distribution - No Disaster)";

        System.out.println("\n========== STARTING K-MEANS AID DISTRIBUTION ==========");
        System.out.println("Aid Type: " + aidName);
        System.out.println("Available Quantity: " + availableQuantity + " units");
        System.out.println("Quantity per Beneficiary: " + quantityPerBeneficiary + " units");
        System.out.println("Aid Type ID: " + aidTypeId);
        System.out.println("Context: " + disasterContext);
        System.out.println("Number of Clusters: " + numberOfClusters);
        System.out.println("=======================================================\n");

        List<BeneficiaryCluster> eligibleBeneficiaries =
                aidDAO.getBeneficiariesWithScores(aidTypeId, disasterId);

        if (eligibleBeneficiaries.isEmpty()) {
            System.out.println("⚠ No eligible beneficiaries found for this aid distribution.");
            System.out.println("Possible reasons:");
            System.out.println("  - No beneficiaries have been scored for this aid type");
            System.out.println("  - All beneficiaries have already received this aid");
            if (disasterId > 0) {
                System.out.println("  - No beneficiaries are affected by this disaster");
            }
            return 0;
        }

        System.out.println("Found " + eligibleBeneficiaries.size() + " eligible beneficiaries\n");

        int maxBeneficiaries = availableQuantity / quantityPerBeneficiary;
        System.out.println("Maximum beneficiaries that can be served: " + maxBeneficiaries +
                " (" + availableQuantity + " units ÷ " + quantityPerBeneficiary + " per person)\n");

        List<BeneficiaryCluster> prioritizedBeneficiaries =
                kMeans.getPrioritizedBeneficiaries(eligibleBeneficiaries, maxBeneficiaries, numberOfClusters);

        System.out.println("\nPrioritized " + prioritizedBeneficiaries.size() + " beneficiaries for aid distribution\n");

        int distributedCount = 0;
        LocalDate today = LocalDate.now();

        System.out.println("--- Starting Distribution ---");

        for (BeneficiaryCluster beneficiary : prioritizedBeneficiaries) {
            AidModel aid = new AidModel();
            aid.setBeneficiaryId(beneficiary.getBeneficiaryId());
            aid.setDisasterId(disasterId); // Will be saved as NULL if disasterId is 0 or negative
            aid.setName(aidName);
            aid.setDate(today);
            aid.setQuantity(quantityPerBeneficiary);
            aid.setCost(costPerUnit * quantityPerBeneficiary);
            aid.setProvider(provider);
            aid.setAidTypeId(aidTypeId);

            String distributionType = disasterId > 0
                    ? "K-means Distribution (Disaster-Specific)"
                    : "K-means Distribution (General Aid)";

            String notes = String.format(
                    "%s | Priority: %s | Score: %.3f | Cluster: %d",
                    distributionType,
                    beneficiary.getScoreCategory(),
                    beneficiary.getFinalScore(),
                    beneficiary.getCluster()
            );
            aid.setNotes(notes);

            if (aidDAO.saving(aid)) {
                distributedCount++;
                System.out.printf("  ✓ Beneficiary #%d | Score: %.3f | Category: %s | Cluster: %d\n",
                        beneficiary.getBeneficiaryId(),
                        beneficiary.getFinalScore(),
                        beneficiary.getScoreCategory(),
                        beneficiary.getCluster());
            } else {
                System.err.printf("  ✗ Failed: Beneficiary #%d\n", beneficiary.getBeneficiaryId());
            }
        }

        System.out.println("\n========== DISTRIBUTION COMPLETE ==========");
        System.out.println("Context: " + disasterContext);
        System.out.println("Successfully distributed: " + distributedCount + " beneficiaries");
        System.out.println("Total units: " + (distributedCount * quantityPerBeneficiary));
        System.out.println("Total cost: ₱" + String.format("%.2f", distributedCount * quantityPerBeneficiary * costPerUnit));
        System.out.println("===========================================\n");

        return distributedCount;
    }

    @Override
    public int distributeAidSimple(String aidName, int aidTypeId, int disasterId,
                                   int availableQuantity, int quantityPerBeneficiary, double costPerUnit, String provider) {

        String disasterContext = disasterId > 0 ? "for disaster #" + disasterId : "(General Distribution - No Disaster)";

        System.out.println("\n========== STARTING SIMPLE AID DISTRIBUTION ==========");
        System.out.println("Aid Type: " + aidName);
        System.out.println("Available Quantity: " + availableQuantity + " units");
        System.out.println("Quantity per Beneficiary: " + quantityPerBeneficiary + " units");
        System.out.println("Context: " + disasterContext);
        System.out.println("======================================================\n");

        List<BeneficiaryCluster> beneficiaries =
                aidDAO.getBeneficiariesWithScores(aidTypeId, disasterId);

        if (beneficiaries.isEmpty()) {
            System.out.println("⚠ No eligible beneficiaries found.");
            return 0;
        }

        int maxBeneficiaries = availableQuantity / quantityPerBeneficiary;
        int limit = Math.min(maxBeneficiaries, beneficiaries.size());

        System.out.println("Maximum beneficiaries that can be served: " + maxBeneficiaries);
        System.out.println("Distributing to top " + limit + " beneficiaries by score\n");

        int distributedCount = 0;
        int totalUnitsDistributed = 0;
        LocalDate today = LocalDate.now();

        System.out.println("--- Starting Distribution ---");

        for (int i = 0; i < limit; i++) {
            BeneficiaryCluster beneficiary = beneficiaries.get(i);

            AidModel aid = new AidModel();
            aid.setBeneficiaryId(beneficiary.getBeneficiaryId());
            aid.setDisasterId(disasterId);
            aid.setName(aidName);
            aid.setDate(today);
            aid.setQuantity(quantityPerBeneficiary);
            aid.setCost(costPerUnit * quantityPerBeneficiary);
            aid.setProvider(provider);
            aid.setAidTypeId(aidTypeId);

            String distributionType = disasterId > 0
                    ? "Simple Distribution (Disaster-Specific)"
                    : "Simple Distribution (General Aid)";

            String notes = String.format(
                    "%s | Priority: %s | Score: %.3f | Rank: %d",
                    distributionType,
                    beneficiary.getScoreCategory(),
                    beneficiary.getFinalScore(),
                    i + 1
            );
            aid.setNotes(notes);

            if (aidDAO.saving(aid)) {
                distributedCount++;
                totalUnitsDistributed += quantityPerBeneficiary;

                System.out.printf("  ✓ Rank #%d: Beneficiary #%d | Score: %.3f | %s\n",
                        i + 1,
                        beneficiary.getBeneficiaryId(),
                        beneficiary.getFinalScore(),
                        beneficiary.getScoreCategory());
            } else {
                System.err.printf("  ✗ Failed: Beneficiary #%d\n", beneficiary.getBeneficiaryId());
            }
        }

        System.out.println("\n========== DISTRIBUTION COMPLETE ==========");
        System.out.println("Context: " + disasterContext);
        System.out.println("Successfully distributed: " + distributedCount + " beneficiaries");
        System.out.println("Total units distributed: " + totalUnitsDistributed + " / " + availableQuantity);
        System.out.println("Units remaining: " + (availableQuantity - totalUnitsDistributed));
        System.out.println("Total cost: ₱" + String.format("%.2f", totalUnitsDistributed * costPerUnit));
        System.out.println("===========================================\n");

        return distributedCount;
    }

    @Override
    public List<BeneficiaryCluster> previewAidDistribution(int aidTypeId, int disasterId,
                                                           int availableQuantity, int quantityPerBeneficiary, int numberOfClusters) {

        String disasterContext = disasterId > 0 ? "Disaster #" + disasterId : "General Aid (No Disaster)";

        System.out.println("\n========== PREVIEW AID DISTRIBUTION ==========");
        System.out.println("Aid Type ID: " + aidTypeId);
        System.out.println("Context: " + disasterContext);
        System.out.println("Available Quantity: " + availableQuantity);
        System.out.println("Quantity per Beneficiary: " + quantityPerBeneficiary);
        System.out.println("Number of Clusters: " + numberOfClusters);
        System.out.println("==============================================\n");

        List<BeneficiaryCluster> beneficiaries =
                aidDAO.getBeneficiariesWithScores(aidTypeId, disasterId);

        if (beneficiaries.isEmpty()) {
            System.out.println("⚠ No eligible beneficiaries found for preview.");
            return beneficiaries;
        }

        int maxBeneficiaries = availableQuantity / quantityPerBeneficiary;

        List<BeneficiaryCluster> selected =
                kMeans.getPrioritizedBeneficiaries(beneficiaries, maxBeneficiaries, numberOfClusters);

        System.out.println("\n--- Preview Results ---");
        System.out.println("Would distribute to " + selected.size() + " beneficiaries:");
        System.out.println("Each receiving: " + quantityPerBeneficiary + " units");
        System.out.println("Total units needed: " + (selected.size() * quantityPerBeneficiary));

        int count = 1;
        for (BeneficiaryCluster b : selected) {
            System.out.printf("%d. Beneficiary #%d | Score: %.3f | %s | Cluster: %d\n",
                    count++,
                    b.getBeneficiaryId(),
                    b.getFinalScore(),
                    b.getScoreCategory(),
                    b.getCluster());
        }

        System.out.println("==============================================\n");

        return selected;
    }



    @Override
    public int distributeAidWithKMeansByBarangay(
            String aidName, int aidTypeId, int disasterId,
            int availableQuantity, int quantityPerBeneficiary, double costPerUnit,
            String provider, int numberOfClusters, String barangay) {

        List<String> barangays = new ArrayList<>();
        barangays.add(barangay);

        return distributeAidWithKMeansByBarangays(
                aidName, aidTypeId, disasterId, availableQuantity,
                quantityPerBeneficiary, costPerUnit, provider,
                numberOfClusters, barangays
        );
    }

    @Override
    public int distributeAidWithKMeansByBarangays(
            String aidName, int aidTypeId, int disasterId,
            int availableQuantity, int quantityPerBeneficiary, double costPerUnit,
            String provider, int numberOfClusters, List<String> barangays) {

        String disasterContext = disasterId > 0 ? "for disaster #" + disasterId : "(General Distribution - No Disaster)";

        System.out.println("\n========== STARTING MULTI-BARANGAY K-MEANS DISTRIBUTION ==========");
        System.out.println("Aid Type: " + aidName);
        System.out.println("Barangays: " + String.join(", ", barangays));
        System.out.println("Available Quantity: " + availableQuantity + " units");
        System.out.println("Quantity per Beneficiary: " + quantityPerBeneficiary + " units");
        System.out.println("Number of Clusters: " + numberOfClusters);
        System.out.println("Context: " + disasterContext);
        System.out.println("==================================================================\n");

        List<BeneficiaryCluster> eligibleBeneficiaries =
                aidDAO.getBeneficiariesWithScoresByBarangays(aidTypeId, disasterId, barangays);

        if (eligibleBeneficiaries.isEmpty()) {
            System.out.println("⚠ No eligible beneficiaries found in selected barangays");
            return 0;
        }

        System.out.println("Found " + eligibleBeneficiaries.size() +
                " eligible beneficiaries across " + barangays.size() + " barangays\n");

        int maxBeneficiaries = availableQuantity / quantityPerBeneficiary;
        System.out.println("Maximum beneficiaries that can be served: " + maxBeneficiaries);

        List<BeneficiaryCluster> prioritizedBeneficiaries =
                kMeans.getPrioritizedBeneficiaries(eligibleBeneficiaries, maxBeneficiaries, numberOfClusters);

        System.out.println("\nPrioritized " + prioritizedBeneficiaries.size() +
                " beneficiaries for distribution\n");

        int distributedCount = 0;
        LocalDate today = LocalDate.now();

        System.out.println("--- Starting Distribution ---");

        for (BeneficiaryCluster beneficiary : prioritizedBeneficiaries) {
            AidModel aid = new AidModel();
            aid.setBeneficiaryId(beneficiary.getBeneficiaryId());
            aid.setDisasterId(disasterId);
            aid.setName(aidName);
            aid.setDate(today);
            aid.setQuantity(quantityPerBeneficiary);
            aid.setCost(costPerUnit * quantityPerBeneficiary);
            aid.setProvider(provider);
            aid.setAidTypeId(aidTypeId);

            String distributionType = disasterId > 0
                    ? "K-means Distribution (Multi-Barangay, Disaster-Specific)"
                    : "K-means Distribution (Multi-Barangay, General Aid)";

            String notes = String.format(
                    "%s | Priority: %s | Score: %.3f | Cluster: %d",
                    distributionType,
                    beneficiary.getScoreCategory(),
                    beneficiary.getFinalScore(),
                    beneficiary.getCluster()
            );
            aid.setNotes(notes);

            if (aidDAO.saving(aid)) {
                distributedCount++;
                System.out.printf("  ✓ Beneficiary #%d | Score: %.3f | %s | Cluster: %d\n",
                        beneficiary.getBeneficiaryId(),
                        beneficiary.getFinalScore(),
                        beneficiary.getScoreCategory(),
                        beneficiary.getCluster());
            } else {
                System.err.printf("  ✗ Failed: Beneficiary #%d\n", beneficiary.getBeneficiaryId());
            }
        }

        System.out.println("\n========== DISTRIBUTION COMPLETE ==========");
        System.out.println("Barangays: " + String.join(", ", barangays));
        System.out.println("Context: " + disasterContext);
        System.out.println("Successfully distributed: " + distributedCount + " beneficiaries");
        System.out.println("Total units: " + (distributedCount * quantityPerBeneficiary));
        System.out.println("Total cost: ₱" + String.format("%.2f", distributedCount * quantityPerBeneficiary * costPerUnit));
        System.out.println("===========================================\n");

        return distributedCount;
    }

    @Override
    public List<BeneficiaryCluster> previewAidDistributionByBarangay(
            int aidTypeId, int disasterId, int availableQuantity,
            int quantityPerBeneficiary, int numberOfClusters, String barangay) {

        List<String> barangays = new ArrayList<>();
        barangays.add(barangay);

        return previewAidDistributionByBarangays(
                aidTypeId, disasterId, availableQuantity,
                quantityPerBeneficiary, numberOfClusters, barangays
        );
    }

    @Override
    public List<BeneficiaryCluster> previewAidDistributionByBarangays(
            int aidTypeId, int disasterId, int availableQuantity,
            int quantityPerBeneficiary, int numberOfClusters, List<String> barangays) {

        String disasterContext = disasterId > 0 ? "Disaster #" + disasterId : "General Aid (No Disaster)";

        System.out.println("\n========== PREVIEW MULTI-BARANGAY DISTRIBUTION ==========");
        System.out.println("Barangays: " + String.join(", ", barangays));
        System.out.println("Aid Type ID: " + aidTypeId);
        System.out.println("Context: " + disasterContext);
        System.out.println("=========================================================\n");

        List<BeneficiaryCluster> beneficiaries =
                aidDAO.getBeneficiariesWithScoresByBarangays(aidTypeId, disasterId, barangays);

        if (beneficiaries.isEmpty()) {
            System.out.println("⚠ No eligible beneficiaries found");
            return beneficiaries;
        }

        int maxBeneficiaries = availableQuantity / quantityPerBeneficiary;

        List<BeneficiaryCluster> selected =
                kMeans.getPrioritizedBeneficiaries(beneficiaries, maxBeneficiaries, numberOfClusters);

        System.out.println("\n--- Preview Results ---");
        System.out.println("Would distribute to " + selected.size() + " beneficiaries");
        System.out.println("=======================\n");

        return selected;
    }





}