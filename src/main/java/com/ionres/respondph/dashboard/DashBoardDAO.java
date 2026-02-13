package com.ionres.respondph.dashboard;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.common.model.DisasterCircleEncrypted;
import com.ionres.respondph.common.model.EvacSiteMappingModel;

import java.util.List;

public interface DashBoardDAO {
    int getTotalBeneficiaries();
    int getTotalDisasters();
    int getTotalAids();
    int getTotalEvacutaionSites();
    int getCount(String sql);
    List<DisasterCircleEncrypted> fetchAllEncrypted();
    List<BeneficiariesMappingModel> fetchAllBeneficiaries();
    List<EvacSiteMappingModel> fetchAllEvacSites();
}