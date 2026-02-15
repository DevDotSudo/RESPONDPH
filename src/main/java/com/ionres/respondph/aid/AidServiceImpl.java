package com.ionres.respondph.aid;

import com.ionres.respondph.aid.KMeansAidDistribution.BeneficiaryCluster;
import com.ionres.respondph.database.DBConnection;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class AidServiceImpl implements AidService {

    private final AidDAO aidDAO;
    private final KMeansAidDistribution kMeans;
    private final FCMAidDistribution    fcm;     // ← NEW

    public AidServiceImpl() {
        this.aidDAO  = new AidDAOImpl(DBConnection.getInstance());
        this.kMeans  = new KMeansAidDistribution();
        this.fcm     = new FCMAidDistribution();   // default params (m=2, ε=1e-5, 300 iter)
    }

    public AidServiceImpl(AidDAO aidDAO) {
        this.aidDAO  = aidDAO;
        this.kMeans  = new KMeansAidDistribution();
        this.fcm     = new FCMAidDistribution();
    }

    // =========================================================================
    //  EXISTING K-MEANS METHODS  (unchanged)
    // =========================================================================

    @Override
    public int distributeAidWithKMeans(String aidName, int aidTypeId, int disasterId,
                                       int availableQuantity, int quantityPerBeneficiary,
                                       double costPerUnit, String provider, int numberOfClusters) {

        String disasterContext = disasterId > 0
                ? "for disaster #" + disasterId
                : "(General Distribution - No Disaster)";

        System.out.println("\n========== STARTING K-MEANS AID DISTRIBUTION ==========");
        System.out.println("Aid Type              : " + aidName);
        System.out.println("Available Quantity    : " + availableQuantity + " units");
        System.out.println("Quantity/Beneficiary  : " + quantityPerBeneficiary + " units");
        System.out.println("Aid Type ID           : " + aidTypeId);
        System.out.println("Context               : " + disasterContext);
        System.out.println("Number of Clusters    : " + numberOfClusters);
        System.out.println("=======================================================\n");

        List<BeneficiaryCluster> eligibleBeneficiaries =
                aidDAO.getBeneficiariesWithScores(aidTypeId, disasterId);

        if (eligibleBeneficiaries.isEmpty()) {
            logNoEligible(disasterId);
            return 0;
        }

        int maxBeneficiaries = availableQuantity / quantityPerBeneficiary;
        System.out.println("Max beneficiaries that can be served: " + maxBeneficiaries + "\n");

        List<BeneficiaryCluster> prioritized =
                kMeans.getPrioritizedBeneficiaries(eligibleBeneficiaries, maxBeneficiaries, numberOfClusters);

        return persistDistribution(prioritized, aidName, aidTypeId, disasterId,
                quantityPerBeneficiary, costPerUnit, provider,
                "K-means Distribution", disasterContext);
    }

    @Override
    public int distributeAidSimple(String aidName, int aidTypeId, int disasterId,
                                   int availableQuantity, int quantityPerBeneficiary,
                                   double costPerUnit, String provider) {

        String disasterContext = disasterId > 0
                ? "for disaster #" + disasterId
                : "(General Distribution - No Disaster)";

        System.out.println("\n========== STARTING SIMPLE AID DISTRIBUTION ==========");
        System.out.println("Aid Type  : " + aidName);
        System.out.println("Quantity  : " + availableQuantity + " units");
        System.out.println("Context   : " + disasterContext);
        System.out.println("======================================================\n");

        List<BeneficiaryCluster> beneficiaries =
                aidDAO.getBeneficiariesWithScores(aidTypeId, disasterId);

        if (beneficiaries.isEmpty()) {
            System.out.println("⚠ No eligible beneficiaries found.");
            return 0;
        }

        int maxBeneficiaries = availableQuantity / quantityPerBeneficiary;
        int limit = Math.min(maxBeneficiaries, beneficiaries.size());

        int distributedCount     = 0;
        int totalUnitsDistributed = 0;
        LocalDate today = LocalDate.now();

        for (int i = 0; i < limit; i++) {
            BeneficiaryCluster b = beneficiaries.get(i);

            AidModel aid = buildAidModel(b.getBeneficiaryId(), aidTypeId, disasterId, aidName,
                    today, quantityPerBeneficiary, costPerUnit, provider);
            aid.setNotes(String.format(
                    "%s | Priority: %s | Score: %.3f | Rank: %d",
                    disasterId > 0 ? "Simple Distribution (Disaster-Specific)"
                            : "Simple Distribution (General Aid)",
                    b.getScoreCategory(), b.getFinalScore(), i + 1
            ));

            if (aidDAO.saving(aid)) {
                distributedCount++;
                totalUnitsDistributed += quantityPerBeneficiary;
                System.out.printf("  ✓ Rank #%d: Beneficiary #%d | Score: %.3f | %s%n",
                        i + 1, b.getBeneficiaryId(), b.getFinalScore(), b.getScoreCategory());
            } else {
                System.err.printf("  ✗ Failed: Beneficiary #%d%n", b.getBeneficiaryId());
            }
        }

        System.out.println("\n========== DISTRIBUTION COMPLETE ==========");
        System.out.println("Context  : " + disasterContext);
        System.out.println("Served   : " + distributedCount);
        System.out.println("Units    : " + totalUnitsDistributed + " / " + availableQuantity);
        System.out.printf ("Cost     : ₱%.2f%n", totalUnitsDistributed * costPerUnit);
        System.out.println("===========================================\n");

        return distributedCount;
    }

    @Override
    public List<BeneficiaryCluster> previewAidDistribution(int aidTypeId, int disasterId,
                                                           int availableQuantity,
                                                           int quantityPerBeneficiary,
                                                           int numberOfClusters) {
        List<BeneficiaryCluster> beneficiaries =
                aidDAO.getBeneficiariesWithScores(aidTypeId, disasterId);

        if (beneficiaries.isEmpty()) return beneficiaries;

        int maxBeneficiaries = availableQuantity / quantityPerBeneficiary;
        return kMeans.getPrioritizedBeneficiaries(beneficiaries, maxBeneficiaries, numberOfClusters);
    }

    @Override
    public int distributeAidWithKMeansByBarangay(
            String aidName, int aidTypeId, int disasterId,
            int availableQuantity, int quantityPerBeneficiary, double costPerUnit,
            String provider, int numberOfClusters, String barangay) {

        List<String> barangays = new ArrayList<>();
        barangays.add(barangay);
        return distributeAidWithKMeansByBarangays(aidName, aidTypeId, disasterId, availableQuantity,
                quantityPerBeneficiary, costPerUnit, provider, numberOfClusters, barangays);
    }

    @Override
    public int distributeAidWithKMeansByBarangays(
            String aidName, int aidTypeId, int disasterId,
            int availableQuantity, int quantityPerBeneficiary, double costPerUnit,
            String provider, int numberOfClusters, List<String> barangays) {

        String disasterContext = disasterId > 0
                ? "for disaster #" + disasterId
                : "(General Distribution - No Disaster)";

        System.out.println("\n========== STARTING MULTI-BARANGAY K-MEANS DISTRIBUTION ==========");
        System.out.println("Barangays : " + String.join(", ", barangays));
        System.out.println("Context   : " + disasterContext);
        System.out.println("Clusters  : " + numberOfClusters);
        System.out.println("==================================================================\n");

        List<BeneficiaryCluster> eligible =
                aidDAO.getBeneficiariesWithScoresByBarangays(aidTypeId, disasterId, barangays);

        if (eligible.isEmpty()) {
            System.out.println("⚠ No eligible beneficiaries in selected barangays.");
            return 0;
        }

        int maxBeneficiaries = availableQuantity / quantityPerBeneficiary;
        List<BeneficiaryCluster> prioritized =
                kMeans.getPrioritizedBeneficiaries(eligible, maxBeneficiaries, numberOfClusters);

        return persistDistribution(prioritized, aidName, aidTypeId, disasterId,
                quantityPerBeneficiary, costPerUnit, provider,
                "K-means Distribution (Multi-Barangay)", disasterContext);
    }

    @Override
    public List<BeneficiaryCluster> previewAidDistributionByBarangay(
            int aidTypeId, int disasterId, int availableQuantity,
            int quantityPerBeneficiary, int numberOfClusters, String barangay) {

        List<String> barangays = new ArrayList<>();
        barangays.add(barangay);
        return previewAidDistributionByBarangays(aidTypeId, disasterId, availableQuantity,
                quantityPerBeneficiary, numberOfClusters, barangays);
    }

    @Override
    public List<BeneficiaryCluster> previewAidDistributionByBarangays(
            int aidTypeId, int disasterId, int availableQuantity,
            int quantityPerBeneficiary, int numberOfClusters, List<String> barangays) {

        List<BeneficiaryCluster> beneficiaries =
                aidDAO.getBeneficiariesWithScoresByBarangays(aidTypeId, disasterId, barangays);

        if (beneficiaries.isEmpty()) return beneficiaries;

        int maxBeneficiaries = availableQuantity / quantityPerBeneficiary;
        return kMeans.getPrioritizedBeneficiaries(beneficiaries, maxBeneficiaries, numberOfClusters);
    }


    @Override
    public int distributeAidWithFCM(String aidName, int aidTypeId, int disasterId,
                                    int availableQuantity, int quantityPerBeneficiary,
                                    double costPerUnit, String provider, int numberOfClusters) {

        String disasterContext = disasterId > 0
                ? "for disaster #" + disasterId
                : "(General Distribution - No Disaster)";

        System.out.println("\n========== STARTING FCM AID DISTRIBUTION ==========");
        System.out.println("Aid Type              : " + aidName);
        System.out.println("Available Quantity    : " + availableQuantity + " units");
        System.out.println("Quantity/Beneficiary  : " + quantityPerBeneficiary + " units");
        System.out.println("Aid Type ID           : " + aidTypeId);
        System.out.println("Context               : " + disasterContext);
        System.out.println("Number of Clusters    : " + numberOfClusters);
        System.out.println("====================================================\n");

        List<BeneficiaryCluster> eligible =
                aidDAO.getBeneficiariesWithScores(aidTypeId, disasterId);

        if (eligible.isEmpty()) {
            logNoEligible(disasterId);
            return 0;
        }

        // Convert KMeans clusters to FCM clusters for processing
        List<FCMAidDistribution.BeneficiaryCluster> fcmList = toFCMList(eligible);

        int maxBeneficiaries = availableQuantity / quantityPerBeneficiary;
        List<FCMAidDistribution.BeneficiaryCluster> prioritized =
                fcm.getPrioritizedBeneficiaries(fcmList, maxBeneficiaries, numberOfClusters);

        return persistFCMDistribution(prioritized, aidName, aidTypeId, disasterId,
                quantityPerBeneficiary, costPerUnit, provider,
                "FCM Distribution", disasterContext);
    }

    /**
     * Distributes aid to a single barangay using Fuzzy C-Means clustering.
     */
    @Override
    public int distributeAidWithFCMByBarangay(
            String aidName, int aidTypeId, int disasterId,
            int availableQuantity, int quantityPerBeneficiary, double costPerUnit,
            String provider, int numberOfClusters, String barangay) {

        List<String> barangays = new ArrayList<>();
        barangays.add(barangay);
        return distributeAidWithFCMByBarangays(aidName, aidTypeId, disasterId, availableQuantity,
                quantityPerBeneficiary, costPerUnit, provider, numberOfClusters, barangays);
    }

    /**
     * Distributes aid to multiple barangays using Fuzzy C-Means clustering.
     */
    @Override
    public int distributeAidWithFCMByBarangays(
            String aidName, int aidTypeId, int disasterId,
            int availableQuantity, int quantityPerBeneficiary, double costPerUnit,
            String provider, int numberOfClusters, List<String> barangays) {

        String disasterContext = disasterId > 0
                ? "for disaster #" + disasterId
                : "(General Distribution - No Disaster)";

        System.out.println("\n========== STARTING MULTI-BARANGAY FCM DISTRIBUTION ==========");
        System.out.println("Barangays : " + String.join(", ", barangays));
        System.out.println("Context   : " + disasterContext);
        System.out.println("Clusters  : " + numberOfClusters);
        System.out.println("==============================================================\n");

        List<BeneficiaryCluster> eligible =
                aidDAO.getBeneficiariesWithScoresByBarangays(aidTypeId, disasterId, barangays);

        if (eligible.isEmpty()) {
            System.out.println("⚠ No eligible beneficiaries in selected barangays.");
            return 0;
        }

        List<FCMAidDistribution.BeneficiaryCluster> fcmList = toFCMList(eligible);

        int maxBeneficiaries = availableQuantity / quantityPerBeneficiary;
        List<FCMAidDistribution.BeneficiaryCluster> prioritized =
                fcm.getPrioritizedBeneficiaries(fcmList, maxBeneficiaries, numberOfClusters);

        return persistFCMDistribution(prioritized, aidName, aidTypeId, disasterId,
                quantityPerBeneficiary, costPerUnit, provider,
                "FCM Distribution (Multi-Barangay)", disasterContext);
    }


    @Override
    public List<BeneficiaryCluster> previewAidDistributionFCM(
            int aidTypeId, int disasterId,
            int availableQuantity, int quantityPerBeneficiary, int numberOfClusters) {

        List<BeneficiaryCluster> eligible =
                aidDAO.getBeneficiariesWithScores(aidTypeId, disasterId);

        if (eligible.isEmpty()) return eligible;

        List<FCMAidDistribution.BeneficiaryCluster> fcmList = toFCMList(eligible);
        int maxBeneficiaries = availableQuantity / quantityPerBeneficiary;

        List<FCMAidDistribution.BeneficiaryCluster> selected =
                fcm.getPrioritizedBeneficiaries(fcmList, maxBeneficiaries, numberOfClusters);

        return fromFCMList(selected);
    }

    /**
     * Preview FCM distribution for a single barangay.
     */
    @Override
    public List<BeneficiaryCluster> previewAidDistributionFCMByBarangay(
            int aidTypeId, int disasterId,
            int availableQuantity, int quantityPerBeneficiary,
            int numberOfClusters, String barangay) {

        List<String> barangays = new ArrayList<>();
        barangays.add(barangay);
        return previewAidDistributionFCMByBarangays(aidTypeId, disasterId, availableQuantity,
                quantityPerBeneficiary, numberOfClusters, barangays);
    }

    /**
     * Preview FCM distribution for multiple barangays.
     */
    @Override
    public List<BeneficiaryCluster> previewAidDistributionFCMByBarangays(
            int aidTypeId, int disasterId,
            int availableQuantity, int quantityPerBeneficiary,
            int numberOfClusters, List<String> barangays) {

        List<BeneficiaryCluster> eligible =
                aidDAO.getBeneficiariesWithScoresByBarangays(aidTypeId, disasterId, barangays);

        if (eligible.isEmpty()) return eligible;

        List<FCMAidDistribution.BeneficiaryCluster> fcmList = toFCMList(eligible);
        int maxBeneficiaries = availableQuantity / quantityPerBeneficiary;

        List<FCMAidDistribution.BeneficiaryCluster> selected =
                fcm.getPrioritizedBeneficiaries(fcmList, maxBeneficiaries, numberOfClusters);

        return fromFCMList(selected);
    }

    // =========================================================================
    //  PRIVATE HELPERS
    // =========================================================================

    /** Build a base AidModel (no notes set yet). */
    private AidModel buildAidModel(int beneficiaryId, int aidTypeId, int disasterId,
                                   String aidName, LocalDate date,
                                   int qty, double costPerUnit, String provider) {
        AidModel aid = new AidModel();
        aid.setBeneficiaryId(beneficiaryId);
        aid.setDisasterId(disasterId);
        aid.setName(aidName);
        aid.setDate(date);
        aid.setQuantity(qty);
        aid.setCost(costPerUnit * qty);
        aid.setProvider(provider);
        aid.setAidTypeId(aidTypeId);
        return aid;
    }

    /**
     * Saves a list of KMeans BeneficiaryCluster objects to the database.
     * @return count of successfully saved records
     */
    private int persistDistribution(List<BeneficiaryCluster> list,
                                    String aidName, int aidTypeId, int disasterId,
                                    int qty, double cost, String provider,
                                    String methodLabel, String disasterContext) {
        int count = 0;
        LocalDate today = LocalDate.now();
        String typeTag = disasterId > 0
                ? methodLabel + " (Disaster-Specific)"
                : methodLabel + " (General Aid)";

        for (BeneficiaryCluster b : list) {
            AidModel aid = buildAidModel(b.getBeneficiaryId(), aidTypeId, disasterId,
                    aidName, today, qty, cost, provider);
            aid.setNotes(String.format(
                    "%s | Priority: %s | Score: %.3f | Cluster: %d",
                    typeTag, b.getScoreCategory(), b.getFinalScore(), b.getCluster()
            ));

            if (aidDAO.saving(aid)) {
                count++;
                System.out.printf("  ✓ Beneficiary #%d | Score: %.3f | %s | Cluster: %d%n",
                        b.getBeneficiaryId(), b.getFinalScore(),
                        b.getScoreCategory(), b.getCluster());
            } else {
                System.err.printf("  ✗ Failed: Beneficiary #%d%n", b.getBeneficiaryId());
            }
        }

        System.out.println("\n========== DISTRIBUTION COMPLETE ==========");
        System.out.println("Context : " + disasterContext);
        System.out.println("Served  : " + count);
        System.out.println("Units   : " + (count * qty));
        System.out.printf ("Cost    : ₱%.2f%n", count * qty * cost);
        System.out.println("===========================================\n");
        return count;
    }

    /**
     * Saves a list of FCM BeneficiaryCluster objects to the database.
     * Notes include the fuzzy membership value for transparency.
     */
    private int persistFCMDistribution(List<FCMAidDistribution.BeneficiaryCluster> list,
                                       String aidName, int aidTypeId, int disasterId,
                                       int qty, double cost, String provider,
                                       String methodLabel, String disasterContext) {
        int count = 0;
        LocalDate today = LocalDate.now();
        String typeTag = disasterId > 0
                ? methodLabel + " (Disaster-Specific)"
                : methodLabel + " (General Aid)";

        for (FCMAidDistribution.BeneficiaryCluster b : list) {
            AidModel aid = buildAidModel(b.getBeneficiaryId(), aidTypeId, disasterId,
                    aidName, today, qty, cost, provider);
            aid.setNotes(String.format(
                    "%s | Priority: %s | Score: %.3f | Cluster: %d | Membership: %.3f",
                    typeTag,
                    b.getClusterPriorityLabel(),
                    b.getFinalScore(),
                    b.getCluster(),
                    b.getPrimaryMembership()
            ));

            if (aidDAO.saving(aid)) {
                count++;
                System.out.printf("  ✓ Beneficiary #%d | Score: %.3f | %s | Cluster: %d | Membership: %.3f%n",
                        b.getBeneficiaryId(), b.getFinalScore(),
                        b.getClusterPriorityLabel(), b.getCluster(), b.getPrimaryMembership());
            } else {
                System.err.printf("  ✗ Failed: Beneficiary #%d%n", b.getBeneficiaryId());
            }
        }

        System.out.println("\n========== FCM DISTRIBUTION COMPLETE ==========");
        System.out.println("Context : " + disasterContext);
        System.out.println("Served  : " + count);
        System.out.println("Units   : " + (count * qty));
        System.out.printf ("Cost    : ₱%.2f%n", count * qty * cost);
        System.out.println("===============================================\n");
        return count;
    }

    /** Convert KMeans BeneficiaryCluster list → FCM BeneficiaryCluster list. */
    private List<FCMAidDistribution.BeneficiaryCluster> toFCMList(List<BeneficiaryCluster> src) {
        List<FCMAidDistribution.BeneficiaryCluster> out = new ArrayList<>();
        for (BeneficiaryCluster b : src) {
            out.add(new FCMAidDistribution.BeneficiaryCluster(
                    b.getBeneficiaryId(), b.getFinalScore(), b.getScoreCategory()));
        }
        return out;
    }


    private List<BeneficiaryCluster> fromFCMList(List<FCMAidDistribution.BeneficiaryCluster> src) {
        List<BeneficiaryCluster> out = new ArrayList<>();
        for (FCMAidDistribution.BeneficiaryCluster b : src) {
            BeneficiaryCluster bc = new BeneficiaryCluster(
                    b.getBeneficiaryId(), b.getFinalScore(), b.getClusterPriorityLabel());
            bc.setCluster(b.getCluster());
            bc.setClusterPriorityLabel(b.getClusterPriorityLabel());
            out.add(bc);
        }
        return out;
    }

    private void logNoEligible(int disasterId) {
        System.out.println("⚠ No eligible beneficiaries found.");
        System.out.println("Possible reasons:");
        System.out.println("  - No beneficiaries have been scored for this aid type");
        System.out.println("  - All beneficiaries have already received this aid");
        if (disasterId > 0) System.out.println("  - No beneficiaries are affected by this disaster");
    }
}