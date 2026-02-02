package com.ionres.respondph.evac_plan;

import java.util.List;

public interface EvacPlanDAO {


    List<RankedBeneficiaryModel> getRankedBeneficiariesByDisaster(int disasterId);


    boolean insertEvacPlan(int beneficiaryId, int evacSiteId, int disasterId, String notes);


    int getOccupiedPersonCount(int evacSiteId, int disasterId);


    boolean isAlreadyAssigned(int beneficiaryId, int evacSiteId, int disasterId);
}