package com.ionres.respondph.evacuation_plan;

import java.util.List;

public interface EvacuationPlanService {
    List<RankedBeneficiaryModel> allocateEvacSite(int evacSiteId, int disasterId);
}
