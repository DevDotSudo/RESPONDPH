package com.ionres.respondph.aid;

import com.ionres.respondph.aid.KMeansAidDistribution.BeneficiaryCluster;
import java.util.List;

public interface AidService {

    int distributeAidWithKMeans(String aidName, int aidTypeId, int disasterId,
                                int availableQuantity, int quantityPerBeneficiary,
                                double costPerUnit, String provider, int numberOfClusters);

    int distributeAidSimple(String aidName, int aidTypeId, int disasterId,
                            int availableQuantity, int quantityPerBeneficiary,
                            double costPerUnit, String provider);

    List<BeneficiaryCluster> previewAidDistribution(int aidTypeId, int disasterId,
                                                    int availableQuantity,
                                                    int quantityPerBeneficiary,
                                                    int numberOfClusters);

    int distributeAidWithKMeansByBarangay(String aidName, int aidTypeId, int disasterId,
                                          int availableQuantity, int quantityPerBeneficiary,
                                          double costPerUnit, String provider,
                                          int numberOfClusters, String barangay);

    int distributeAidWithKMeansByBarangays(String aidName, int aidTypeId, int disasterId,
                                           int availableQuantity, int quantityPerBeneficiary,
                                           double costPerUnit, String provider,
                                           int numberOfClusters, List<String> barangays);

    List<BeneficiaryCluster> previewAidDistributionByBarangay(int aidTypeId, int disasterId,
                                                              int availableQuantity,
                                                              int quantityPerBeneficiary,
                                                              int numberOfClusters,
                                                              String barangay);

    List<BeneficiaryCluster> previewAidDistributionByBarangays(int aidTypeId, int disasterId,
                                                               int availableQuantity,
                                                               int quantityPerBeneficiary,
                                                               int numberOfClusters,
                                                               List<String> barangays);

    // =========================================================================
    //  NEW FCM METHODS
    // =========================================================================

    /** Distribute aid (all barangays) using Fuzzy C-Means clustering. */
    int distributeAidWithFCM(String aidName, int aidTypeId, int disasterId,
                             int availableQuantity, int quantityPerBeneficiary,
                             double costPerUnit, String provider, int numberOfClusters);

    /** Distribute aid to a single barangay using Fuzzy C-Means clustering. */
    int distributeAidWithFCMByBarangay(String aidName, int aidTypeId, int disasterId,
                                       int availableQuantity, int quantityPerBeneficiary,
                                       double costPerUnit, String provider,
                                       int numberOfClusters, String barangay);

    /** Distribute aid to multiple barangays using Fuzzy C-Means clustering. */
    int distributeAidWithFCMByBarangays(String aidName, int aidTypeId, int disasterId,
                                        int availableQuantity, int quantityPerBeneficiary,
                                        double costPerUnit, String provider,
                                        int numberOfClusters, List<String> barangays);

    /** Preview FCM distribution (all barangays). */
    List<BeneficiaryCluster> previewAidDistributionFCM(int aidTypeId, int disasterId,
                                                       int availableQuantity,
                                                       int quantityPerBeneficiary,
                                                       int numberOfClusters);

    /** Preview FCM distribution for a single barangay. */
    List<BeneficiaryCluster> previewAidDistributionFCMByBarangay(int aidTypeId, int disasterId,
                                                                 int availableQuantity,
                                                                 int quantityPerBeneficiary,
                                                                 int numberOfClusters,
                                                                 String barangay);

    /** Preview FCM distribution for multiple barangays. */
    List<BeneficiaryCluster> previewAidDistributionFCMByBarangays(int aidTypeId, int disasterId,
                                                                  int availableQuantity,
                                                                  int quantityPerBeneficiary,
                                                                  int numberOfClusters,
                                                                  List<String> barangays);



}