package com.ionres.respondph.evac_plan;

import java.util.List;

public interface EvacPlanService {


    List<RankedBeneficiaryModel> allocateEvacSite(int evacSiteId, int disasterId);
}