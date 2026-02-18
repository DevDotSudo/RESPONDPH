package com.ionres.respondph.aid;

import com.ionres.respondph.aid.KMeansAidDistribution.BeneficiaryCluster;
import java.util.List;
import java.util.Map;


public interface AidDAO {


    boolean saving(AidModel aid);


    List<AidModel> getAll();


    boolean delete(AidModel aid);


    boolean update(AidModel aid);


    List<BeneficiaryCluster> getBeneficiariesWithScores(int aidTypeId, int disasterId);


    List<BeneficiaryCluster> getBeneficiariesWithScoresByBarangays(
            int aidTypeId, int disasterId, List<String> barangays);


    List<String> getBarangaysByDisaster(int disasterId, int aidTypeId);


    List<String> getAllBarangays();


    List<String> getBarangaysByAidNameAndDisaster(int disasterId, String aidName);


    boolean hasReceivedAid(int beneficiaryId, int aidTypeId, int disasterId);


    List<AidModel> getAidByBeneficiary(int beneficiaryId);


    List<AidModel> getAidByDisaster(int disasterId);


    List<AidModel> getAidByType(int aidTypeId);


    double getTotalQuantityDistributed(int aidTypeId, int disasterId);


    List<AidModel> getAllAidForTable();


    List<String> getDistinctAidNames();

    Map<Integer, String> getBeneficiaryNames(List<Integer> beneficiaryIds);
}