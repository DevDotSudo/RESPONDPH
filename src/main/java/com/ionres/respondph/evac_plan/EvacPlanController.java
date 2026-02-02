package com.ionres.respondph.evac_plan;

import com.ionres.respondph.database.DBConnection;

import java.util.List;

/**
 * Controller for Evacuation Plan management.
 * This is the main entry point for UI components to trigger evacuation site allocation.
 */
public class EvacPlanController {

    private final EvacPlanService evacPlanService;

    public EvacPlanController() {
        DBConnection dbConnection = DBConnection.getInstance();
        this.evacPlanService = new EvacPlanServiceImpl(dbConnection);
    }

    // Constructor for testing (dependency injection)
    public EvacPlanController(EvacPlanService evacPlanService) {
        this.evacPlanService = evacPlanService;
    }

    /**
     * Allocates beneficiaries to an evacuation site based on priority scores.
     *
     * This method should be called when:
     * - A disaster occurs and you need to allocate beneficiaries to evacuation sites
     * - You want to fill an evacuation site with the highest-priority households
     *
     * @param evacSiteId  the evacuation site ID to fill
     * @param disasterId  the disaster ID these beneficiaries are affected by
     * @return list of beneficiaries that were successfully assigned to the evac site
     *
     * Example usage from a button click handler:
     * <pre>
     * EvacPlanController controller = new EvacPlanController();
     * List<RankedBeneficiaryModel> assigned = controller.allocateEvacSite(evacSiteId, disasterId);
     *
     * if (!assigned.isEmpty()) {
     *     showSuccess("Successfully allocated " + assigned.size() + " households");
     * } else {
     *     showError("No beneficiaries could be allocated");
     * }
     * </pre>
     */
    public List<RankedBeneficiaryModel> allocateEvacSite(int evacSiteId, int disasterId) {
        try {
            return evacPlanService.allocateEvacSite(evacSiteId, disasterId);
        } catch (Exception e) {
            System.err.println("Error during evacuation allocation: " + e.getMessage());
            e.printStackTrace();
            return List.of(); // Return empty list on error
        }
    }

    /**
     * Gets the service instance (useful if you need to access additional methods later)
     */
    public EvacPlanService getService() {
        return evacPlanService;
    }
}