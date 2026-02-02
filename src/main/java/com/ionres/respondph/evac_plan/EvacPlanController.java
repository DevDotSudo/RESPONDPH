package com.ionres.respondph.evac_plan;

import com.ionres.respondph.database.DBConnection;

import java.util.List;

public class EvacPlanController {

    private final EvacPlanService evacPlanService;

    public EvacPlanController() {
        DBConnection dbConnection = DBConnection.getInstance();
        this.evacPlanService = new EvacPlanServiceImpl(dbConnection);
    }

    public EvacPlanController(EvacPlanService evacPlanService) {
        this.evacPlanService = evacPlanService;
    }


    public List<RankedBeneficiaryModel> allocateEvacSite(int evacSiteId, int disasterId) {
        try {
            return evacPlanService.allocateEvacSite(evacSiteId, disasterId);
        } catch (Exception e) {
            System.err.println("Error during evacuation allocation: " + e.getMessage());
            e.printStackTrace();
            return List.of(); // Return empty list on error
        }
    }


    public EvacPlanService getService() {
        return evacPlanService;
    }
}