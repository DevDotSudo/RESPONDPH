package com.ionres.respondph.evac_plan;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.evac_site.EvacSiteDAO;
import com.ionres.respondph.evac_site.EvacSiteDAOServiceImpl;
import com.ionres.respondph.evac_site.EvacSiteModel;
import com.ionres.respondph.evac_site.EvacSiteService;
import com.ionres.respondph.util.AppContext;

import java.util.ArrayList;
import java.util.List;

public class EvacPlanServiceImpl implements EvacPlanService {

    private final EvacPlanDAO evacPlanDAO;
    private final EvacSiteDAO evacSiteDAO;

    public EvacPlanServiceImpl(DBConnection dbConnection) {
        this.evacPlanDAO = new EvacPlanDAOImpl(dbConnection);
        this.evacSiteDAO = new EvacSiteDAOServiceImpl(dbConnection);
    }

    public EvacPlanServiceImpl(EvacPlanDAO evacPlanDAO, EvacSiteDAO evacSiteDAO) {
        this.evacPlanDAO = evacPlanDAO;
        this.evacSiteDAO = evacSiteDAO;
    }

    /**
     * Core logic:
     *   1. Load the evac site to get its capacity (in persons).
     *   2. Check how many persons are already assigned (in case of partial allocation).
     *   3. Fetch all beneficiaries ranked by final_score DESC for this disaster.
     *   4. Walk through the ranked list. For each beneficiary:
     *        - Skip if already assigned to this site.
     *        - If (occupiedSoFar + theirHouseholdSize) <= capacity → assign them.
     *        - Otherwise skip (their household is too large to fit in remaining space).
     *   5. Insert each selected beneficiary into evac_plan.
     *   6. Return the list of beneficiaries that were assigned.
     *
     * Example with capacity = 20:
     *   Beneficiary A: score 0.45, household = 5  → occupiedSoFar = 5  ✓
     *   Beneficiary B: score 0.42, household = 7  → occupiedSoFar = 12 ✓
     *   Beneficiary C: score 0.38, household = 8  → occupiedSoFar = 20 ✓  (full)
     *   Beneficiary D: score 0.35, household = 3  → 20 + 3 = 23 > 20   ✗  (skipped)
     */
    @Override
    public List<RankedBeneficiaryModel> allocateEvacSite(int evacSiteId, int disasterId) {
        List<RankedBeneficiaryModel> assigned = new ArrayList<>();

        System.out.println("========== EVAC SITE ALLOCATION ==========");
        System.out.println("Evac Site ID : " + evacSiteId);
        System.out.println("Disaster ID  : " + disasterId);

        // Step 1: Get evac site and its capacity
        EvacSiteModel evacSite = evacSiteDAO.getById(evacSiteId);
        if (evacSite == null) {
            System.err.println("ERROR: Evacuation site not found for ID: " + evacSiteId);
            return assigned;
        }

        int capacity;
        try {
            capacity = Integer.parseInt(evacSite.getCapacity());
        } catch (NumberFormatException e) {
            System.err.println("ERROR: Invalid capacity value for evac site: " + evacSite.getCapacity());
            return assigned;
        }

        System.out.println("Site Name    : " + evacSite.getName());
        System.out.println("Capacity     : " + capacity + " persons");

        // Step 2: Check already occupied persons (supports repeated/partial allocations)
        int alreadyOccupied = evacPlanDAO.getOccupiedPersonCount(evacSiteId, disasterId);
        int remainingCapacity = capacity - alreadyOccupied;

        System.out.println("Already Occupied : " + alreadyOccupied + " persons");
        System.out.println("Remaining        : " + remainingCapacity + " persons");

        if (remainingCapacity <= 0) {
            System.out.println("Evacuation site is already full.");
            System.out.println("==========================================");
            return assigned;
        }

        // Step 3: Get all beneficiaries ranked by priority score (highest first)
        List<RankedBeneficiaryModel> rankedBeneficiaries = evacPlanDAO.getRankedBeneficiariesByDisaster(disasterId);

        if (rankedBeneficiaries.isEmpty()) {
            System.out.println("No scored beneficiaries found for disaster ID: " + disasterId);
            System.out.println("==========================================");
            return assigned;
        }

        System.out.println("Total ranked beneficiaries: " + rankedBeneficiaries.size());
        System.out.println("------------------------------------------");

        // Step 4: Fill the site by iterating highest priority first
        int occupiedSoFar = alreadyOccupied;

        for (RankedBeneficiaryModel beneficiary : rankedBeneficiaries) {

            // Can't fit any more households at all
            if (occupiedSoFar >= capacity) {
                break;
            }

            // Skip if already assigned to this site for this disaster
            if (evacPlanDAO.isAlreadyAssigned(beneficiary.getBeneficiaryId(), evacSiteId, disasterId)) {
                System.out.printf("[SKIP] Beneficiary #%d already assigned to this site.\n",
                        beneficiary.getBeneficiaryId());
                continue;
            }

            int householdSize = beneficiary.getHouseholdMembers();

            // Check if this household fits in the remaining space
            if (occupiedSoFar + householdSize <= capacity) {

                // Step 5: Insert into evac_plan
                String notes = String.format("Auto-allocated | Score: %.2f | Category: %s | Household: %d persons",
                        beneficiary.getFinalScore(), beneficiary.getScoreCategory(), householdSize);

                boolean inserted = evacPlanDAO.insertEvacPlan(
                        beneficiary.getBeneficiaryId(), evacSiteId, disasterId, notes);

                if (inserted) {
                    occupiedSoFar += householdSize;
                    assigned.add(beneficiary);

                    System.out.printf("[ASSIGNED] Beneficiary #%d (%s %s) | Score: %.2f | Household: %d | Total Occupied: %d/%d\n",
                            beneficiary.getBeneficiaryId(),
                            beneficiary.getFirstName(),
                            beneficiary.getLastName(),
                            beneficiary.getFinalScore(),
                            householdSize,
                            occupiedSoFar,
                            capacity);
                } else {
                    System.err.printf("[ERROR] Failed to insert evac_plan for Beneficiary #%d\n",
                            beneficiary.getBeneficiaryId());
                }
            } else {
                // Household too large for remaining space — skip, try next smaller household
                System.out.printf("[SKIP] Beneficiary #%d | Household: %d persons | Remaining: %d persons (doesn't fit)\n",
                        beneficiary.getBeneficiaryId(),
                        householdSize,
                        capacity - occupiedSoFar);
            }
        }

        // Step 6: Summary
        System.out.println("------------------------------------------");
        System.out.println("ALLOCATION COMPLETE");
        System.out.println("Total persons assigned : " + (occupiedSoFar - alreadyOccupied));
        System.out.println("Total persons occupied : " + occupiedSoFar + " / " + capacity);
        System.out.println("Beneficiaries assigned : " + assigned.size());
        System.out.println("==========================================");

        return assigned;
    }
}