package com.ionres.respondph.evac_plan;

import java.util.List;

public interface GeoBasedEvacPlanDAO extends EvacPlanDAO {

    List<RankedBeneficiaryWithLocation> getRankedBeneficiariesWithLocation(int disasterId);

    List<EvacSiteWithDistance> getEvacSitesWithCapacity(int disasterId);
}
