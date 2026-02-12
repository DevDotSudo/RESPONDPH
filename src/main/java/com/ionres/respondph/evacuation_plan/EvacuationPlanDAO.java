package com.ionres.respondph.evacuation_plan;


import com.ionres.respondph.beneficiary.BeneficiaryModel;

import java.util.List;

public interface EvacuationPlanDAO {
    List<RankedBeneficiaryModel> getRankedBeneficiariesByDisaster(int disasterId);
    boolean insertEvacPlan(int beneficiaryId, int evacSiteId, int disasterId, String notes);
    int getOccupiedPersonCount(int evacSiteId, int disasterId);
    boolean isAlreadyAssigned(int beneficiaryId, int evacSiteId, int disasterId);
    List<EvacuationPlanModel> getAll();
    public List<EvacuationPlanModel> search(String searchText);
    public boolean isAlreadyAssignedToDisaster(int beneficiaryId, int disasterId);
    boolean deleteEvacPlan(int planId);
    EvacuationPlanModel getById(int planId);
    int getHouseholdSizeForBeneficiary(int beneficiaryId, int disasterId);
    List<BeneficiaryModel> getBeneficiariesByEvacSiteAndDisaster(int evacSiteId, int disasterId);
    Integer getAssignedEvacSiteId(int beneficiaryId, int disasterId);

}