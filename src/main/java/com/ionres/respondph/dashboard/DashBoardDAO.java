package com.ionres.respondph.dashboard;

import com.ionres.respondph.common.model.DisasterCircleEncrypted;
import com.ionres.respondph.common.model.EvacSiteMappingModel;
import com.ionres.respondph.common.model.FamilyMemberModel;

import java.util.List;

public interface DashBoardDAO {
    int getTotalBeneficiaries();
    int getTotalDisasters();
    int getTotalAids();
    int getDistinctAidCount();
    int getTotalEvacutaionSites();
    int getCount(String sql);

    List<DisasterCircleEncrypted> fetchAllEncrypted();
    List<BeneficiariesMappingModel> fetchAllBeneficiaries();
    List<EvacSiteMappingModel> fetchAllEvacSites();
    List<FamilyMemberModel> fetchFamilyMembers(int beneficiaryId);
    String getPasswordHashById(int adminId);
    boolean updatePassword(int adminId, String newHashedPassword);
}