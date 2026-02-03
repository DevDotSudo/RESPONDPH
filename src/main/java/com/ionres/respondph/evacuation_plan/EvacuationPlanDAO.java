package com.ionres.respondph.evacuation_plan;

import java.util.List;

public interface EvacuationPlanDAO {
    List<RankedBeneficiaryModel> getRankedBeneficiariesByDisaster(int disasterId);
    boolean insertEvacPlan(int beneficiaryId, int evacSiteId, int disasterId, String notes);
    int getOccupiedPersonCount(int evacSiteId, int disasterId);
    boolean isAlreadyAssigned(int beneficiaryId, int evacSiteId, int disasterId);
    boolean decrementEvacSiteCapacity(int evacSiteId, int personCount);
}
