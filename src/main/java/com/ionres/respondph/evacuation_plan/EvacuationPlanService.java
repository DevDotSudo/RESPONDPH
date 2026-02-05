package com.ionres.respondph.evacuation_plan;

import java.util.List;

public interface EvacuationPlanService {
    List<RankedBeneficiaryModel> allocateEvacSite(int evacSiteId, int disasterId);
    List<EvacuationPlanModel> getAllEvacuationPlans();
    List<EvacuationPlanModel> searchEvacuationPlans(String searchText);
    boolean deleteEvacuationPlan(int planId);
}