package com.ionres.respondph.dashboard;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.common.model.DisasterCircleEncrypted;

import java.util.List;

public interface DashBoardDAO {
    int getTotalBeneficiaries();
    int getTotalDisasters();
    int getTotalAids();
    int getCount(String sql);
    List<DisasterCircleEncrypted> fetchAllEncrypted();
    List<BeneficiariesMappingModel> fetchAllBeneficiaries();
}
