package com.ionres.respondph.evac_plan;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.evac_site.EvacSiteDAO;
import com.ionres.respondph.evac_site.EvacSiteDAOServiceImpl;
import com.ionres.respondph.evac_site.EvacSiteModel;

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

    @Override
    public List<RankedBeneficiaryModel> allocateEvacSite(int evacSiteId, int disasterId) {
        List<RankedBeneficiaryModel> assigned = new ArrayList<>();

        System.out.println("========== EVAC SITE ALLOCATION ==========");
        System.out.println("Evac Site ID : " + evacSiteId);
        System.out.println("Disaster ID  : " + disasterId);

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

        int alreadyOccupied = evacPlanDAO.getOccupiedPersonCount(evacSiteId, disasterId);
        int remainingCapacity = capacity - alreadyOccupied;

        System.out.println("Already Occupied : " + alreadyOccupied + " persons");
        System.out.println("Remaining        : " + remainingCapacity + " persons");

        if (remainingCapacity <= 0) {
            System.out.println("Evacuation site is already full.");
            System.out.println("==========================================");
            return assigned;
        }

        List<RankedBeneficiaryModel> rankedBeneficiaries = evacPlanDAO.getRankedBeneficiariesByDisaster(disasterId);

        if (rankedBeneficiaries.isEmpty()) {
            System.out.println("No scored beneficiaries found for disaster ID: " + disasterId);
            System.out.println("==========================================");
            return assigned;
        }

        System.out.println("Total ranked beneficiaries: " + rankedBeneficiaries.size());
        System.out.println("------------------------------------------");

        int occupiedSoFar = alreadyOccupied;
        int totalPersonsAssigned = 0; // Track total persons to decrement from capacity

        for (RankedBeneficiaryModel beneficiary : rankedBeneficiaries) {

            if (occupiedSoFar >= capacity) {
                break;
            }

            if (evacPlanDAO.isAlreadyAssigned(beneficiary.getBeneficiaryId(), evacSiteId, disasterId)) {
                System.out.printf("[SKIP] Beneficiary #%d already assigned to this site.\n",
                        beneficiary.getBeneficiaryId());
                continue;
            }

            int householdSize = beneficiary.getHouseholdMembers();

            if (occupiedSoFar + householdSize <= capacity) {

                String notes = String.format("Auto-allocated | Score: %.2f | Category: %s | Household: %d persons",
                        beneficiary.getFinalScore(), beneficiary.getScoreCategory(), householdSize);

                boolean inserted = evacPlanDAO.insertEvacPlan(
                        beneficiary.getBeneficiaryId(), evacSiteId, disasterId, notes);

                if (inserted) {
                    occupiedSoFar += householdSize;
                    totalPersonsAssigned += householdSize;
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
                System.out.printf("[SKIP] Beneficiary #%d | Household: %d persons | Remaining: %d persons (doesn't fit)\n",
                        beneficiary.getBeneficiaryId(),
                        householdSize,
                        capacity - occupiedSoFar);
            }
        }

        if (totalPersonsAssigned > 0) {
            boolean capacityUpdated = evacPlanDAO.decrementEvacSiteCapacity(evacSiteId, totalPersonsAssigned);

            if (capacityUpdated) {
                System.out.println("✓ Evac site capacity decremented by " + totalPersonsAssigned + " persons.");
            } else {
                System.err.println("✗ WARNING: Failed to decrement evac site capacity!");
            }
        }

        System.out.println("------------------------------------------");
        System.out.println("ALLOCATION COMPLETE");
        System.out.println("Total persons assigned : " + totalPersonsAssigned);
        System.out.println("Total persons occupied : " + occupiedSoFar + " / " + capacity);
        System.out.println("Beneficiaries assigned : " + assigned.size());
        System.out.println("==========================================");

        return assigned;
    }
}