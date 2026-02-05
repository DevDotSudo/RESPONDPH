package com.ionres.respondph.evacuation_plan;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.evac_site.EvacSiteDAO;
import com.ionres.respondph.evac_site.EvacSiteDAOServiceImpl;
import com.ionres.respondph.evac_site.EvacSiteModel;
import java.util.ArrayList;
import java.util.List;

public class EvacuationPlanServiceImpl implements EvacuationPlanService {
    private final EvacuationPlanDAO evacPlanDAO;
    private final EvacSiteDAO evacSiteDAO;

    public EvacuationPlanServiceImpl(DBConnection dbConnection) {
        this.evacPlanDAO = new EvacuationPlanDAOImpl(dbConnection);
        this.evacSiteDAO = new EvacSiteDAOServiceImpl(dbConnection);
    }

    public EvacuationPlanServiceImpl(EvacuationPlanDAO evacPlanDAO, EvacSiteDAO evacSiteDAO) {
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
        int totalPersonsAssigned = 0;

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

        System.out.println("------------------------------------------");
        System.out.println("ALLOCATION COMPLETE");
        System.out.println("Total persons assigned : " + totalPersonsAssigned);
        System.out.println("Total persons occupied : " + occupiedSoFar + " / " + capacity);
        System.out.println("Beneficiaries assigned : " + assigned.size());
        System.out.println("Note: Database capacity remains unchanged");
        System.out.println("==========================================");

        return assigned;
    }
    @Override
    public List<EvacuationPlanModel> getAllEvacuationPlans() {
        try {
            return evacPlanDAO.getAll();
        } catch (Exception e) {
            System.err.println("Error getting all evacuation plans: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public boolean deleteEvacuationPlan(int planId) {
        System.out.println("========== DELETING EVAC PLAN ==========");
        System.out.println("Plan ID: " + planId);

        EvacuationPlanModel plan = evacPlanDAO.getById(planId);

        if (plan == null) {
            System.err.println("ERROR: Evacuation plan not found for ID: " + planId);
            return false;
        }

        int evacSiteId = plan.getEvacSiteId();
        int disasterId = plan.getDisasterId();
        int beneficiaryId = plan.getBeneficiaryId();

        int householdSize = evacPlanDAO.getHouseholdSizeForBeneficiary(beneficiaryId, disasterId);

        System.out.println("Beneficiary: " + plan.getBeneficiaryName());
        System.out.println("Evacuation Site: " + plan.getEvacSiteName());
        System.out.println("Household Size: " + householdSize + " persons");

        boolean deleted = evacPlanDAO.deleteEvacPlan(planId);

        if (deleted) {
            System.out.println("✓ Evacuation plan deleted successfully");
            System.out.println("Note: Database capacity remains unchanged");
            System.out.println("Available capacity updated automatically through evac_plan records");
            System.out.println("==========================================");
            return true;
        } else {
            System.err.println("✗ Failed to delete evacuation plan");
            System.out.println("==========================================");
            return false;
        }
    }

    @Override
    public List<EvacuationPlanModel> searchEvacuationPlans(String searchText) {
        try {
            if (searchText == null || searchText.trim().isEmpty()) {
                return getAllEvacuationPlans();
            }
            return evacPlanDAO.search(searchText.trim());
        } catch (Exception e) {
            System.err.println("Error searching evacuation plans: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}