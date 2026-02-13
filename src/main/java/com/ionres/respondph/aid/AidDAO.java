package com.ionres.respondph.aid;

import com.ionres.respondph.aid.KMeansAidDistribution.BeneficiaryCluster;
import java.util.List;


public interface AidDAO {


    boolean saving(AidModel aid);

    List<AidModel> getAll();

    boolean delete(AidModel aid);

    boolean update(AidModel aid);


    List<BeneficiaryCluster> getBeneficiariesWithScores(int aidTypeId, int disasterId);


    boolean hasReceivedAid(int beneficiaryId, int aidTypeId, int disasterId);

    List<AidModel> getAidByBeneficiary(int beneficiaryId);

    List<AidModel> getAidByDisaster(int disasterId);

    List<AidModel> getAidByType(int aidTypeId);

    double getTotalQuantityDistributed(int aidTypeId, int disasterId);

    List<AidModel> getAllAidForTable();


    List<BeneficiaryCluster> getBeneficiariesWithScoresByBarangays(
            int aidTypeId, int disasterId, List<String> barangays);

    List<String> getAllBarangays();


    List<String> getBarangaysByDisaster(int disasterId);

    List<String> getDistinctAidNames();




}