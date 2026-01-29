package com.ionres.respondph.aid;

import com.ionres.respondph.aid.KMeansAidDistribution.BeneficiaryCluster;
import java.util.List;

public interface AidService {


    int distributeAidWithKMeans(String aidName, int aidTypeId, int disasterId,
                                int availableQuantity, int quantityPerBeneficiary,double costPerUnit,
                                String provider, int numberOfClusters);


    int distributeAidSimple(String aidName, int aidTypeId, int disasterId,
                            int availableQuantity, int quantityPerBeneficiary, double costPerUnit, String provider);

    List<BeneficiaryCluster> previewAidDistribution(int aidTypeId, int disasterId,
                                                    int availableQuantity, int quantityPerBeneficiary, int numberOfClusters);


}