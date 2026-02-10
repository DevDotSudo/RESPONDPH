package com.ionres.respondph.evacuation_plan;

import java.util.List;

public interface GeoBasedEvacPlanDAO extends EvacuationPlanDAO {

    List<RankedBeneficiaryWithLocation> getRankedBeneficiariesWithLocation(int disasterId);

    List<EvacSiteWithDistance> getEvacSitesWithCapacity(int disasterId);
    List<RankedBeneficiaryWithLocation> getRankedBeneficiariesWithLocation(int disasterId, boolean includeAssigned);
}