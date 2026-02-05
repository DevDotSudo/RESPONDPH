package com.ionres.respondph.evacuation_plan;

import com.ionres.respondph.database.DBConnection;

import java.util.*;


public class GeoBasedEvacPlanService {

    private final GeoBasedEvacPlanDAO geoDAO;

    public GeoBasedEvacPlanService(DBConnection dbConnection) {
        this.geoDAO = new GeoBasedEvacPlanDAOImpl(dbConnection);
    }

    public GeoBasedEvacPlanService(GeoBasedEvacPlanDAO geoDAO) {
        this.geoDAO = geoDAO;
    }


    public List<RankedBeneficiaryWithLocation> autoAllocateByProximity(int disasterId) {
        List<RankedBeneficiaryWithLocation> assignedBeneficiaries = new ArrayList<>();

        System.out.println("========== GEO-BASED AUTO ALLOCATION ==========");
        System.out.println("Disaster ID: " + disasterId);

        List<RankedBeneficiaryWithLocation> beneficiaries = geoDAO.getRankedBeneficiariesWithLocation(disasterId);

        if (beneficiaries.isEmpty()) {
            System.out.println("No beneficiaries found for disaster ID: " + disasterId);
            System.out.println("==============================================");
            return assignedBeneficiaries;
        }

        System.out.println("Total beneficiaries to allocate: " + beneficiaries.size());

        List<EvacSiteWithDistance> evacSites = geoDAO.getEvacSitesWithCapacity(disasterId);

        if (evacSites.isEmpty()) {
            System.out.println("No evacuation sites with available capacity.");
            System.out.println("==============================================");
            return assignedBeneficiaries;
        }

        System.out.println("Available evacuation sites: " + evacSites.size());
        System.out.println("----------------------------------------------");

        Map<Integer, Integer> siteCapacities = new HashMap<>();
        for (EvacSiteWithDistance site : evacSites) {
            siteCapacities.put(site.getEvacSiteId(), site.getRemainingCapacity());
        }

        int totalAssigned = 0;
        int totalPersons = 0;

        for (RankedBeneficiaryWithLocation beneficiary : beneficiaries) {

            if (geoDAO.isAlreadyAssigned(beneficiary.getBeneficiaryId(), -1, disasterId)) {
                System.out.printf("[SKIP] Beneficiary #%d already assigned.\n",
                        beneficiary.getBeneficiaryId());
                continue;
            }

            EvacSiteWithDistance nearestSite = findNearestSiteWithCapacity(
                    beneficiary, evacSites, siteCapacities);

            if (nearestSite == null) {
                System.out.printf("[SKIP] Beneficiary #%d (%s %s) | No evacuation site has capacity for %d persons\n",
                        beneficiary.getBeneficiaryId(),
                        beneficiary.getFirstName(),
                        beneficiary.getLastName(),
                        beneficiary.getHouseholdMembers());
                continue;
            }

            String notes = String.format(
                    "Auto-allocated (Geo-Based) | Score: %.2f | Category: %s | Household: %d | Distance: %.2f km",
                    beneficiary.getFinalScore(),
                    beneficiary.getScoreCategory(),
                    beneficiary.getHouseholdMembers(),
                    nearestSite.getDistanceInKm());

            boolean inserted = geoDAO.insertEvacPlan(
                    beneficiary.getBeneficiaryId(),
                    nearestSite.getEvacSiteId(),
                    disasterId,
                    notes);

            if (inserted) {
                int currentCapacity = siteCapacities.get(nearestSite.getEvacSiteId());
                int newCapacity = currentCapacity - beneficiary.getHouseholdMembers();
                siteCapacities.put(nearestSite.getEvacSiteId(), newCapacity);

                beneficiary.setAssignedEvacSiteId(nearestSite.getEvacSiteId());
                beneficiary.setAssignedEvacSiteName(nearestSite.getEvacSiteName());
                beneficiary.setDistanceToEvacSite(nearestSite.getDistanceInKm());

                assignedBeneficiaries.add(beneficiary);
                totalAssigned++;
                totalPersons += beneficiary.getHouseholdMembers();

                System.out.printf("[ASSIGNED] Beneficiary #%d (%s %s) | Score: %.2f | " +
                                "Household: %d → %s (%.2f km) | Remaining capacity: %d\n",
                        beneficiary.getBeneficiaryId(),
                        beneficiary.getFirstName(),
                        beneficiary.getLastName(),
                        beneficiary.getFinalScore(),
                        beneficiary.getHouseholdMembers(),
                        nearestSite.getEvacSiteName(),
                        nearestSite.getDistanceInKm(),
                        newCapacity);


            } else {
                System.err.printf("[ERROR] Failed to insert evac_plan for Beneficiary #%d\n",
                        beneficiary.getBeneficiaryId());
            }
        }

        System.out.println("----------------------------------------------");
        System.out.println("AUTO-ALLOCATION COMPLETE");
        System.out.println("Total beneficiaries assigned: " + totalAssigned);
        System.out.println("Total persons evacuated: " + totalPersons);
        System.out.println("Note: Database capacity remains unchanged");
        System.out.println("==============================================");

        return assignedBeneficiaries;
    }



    private EvacSiteWithDistance findNearestSiteWithCapacity(
            RankedBeneficiaryWithLocation beneficiary,
            List<EvacSiteWithDistance> evacSites,
            Map<Integer, Integer> siteCapacities) {

        EvacSiteWithDistance nearestSite = null;
        double minDistance = Double.MAX_VALUE;

        for (EvacSiteWithDistance site : evacSites) {
            int remainingCapacity = siteCapacities.get(site.getEvacSiteId());
            if (remainingCapacity < beneficiary.getHouseholdMembers()) {
                continue; // Not enough space
            }

            double distance = GeoDistanceCalculator.calculateDistance(
                    beneficiary.getLatitude(),
                    beneficiary.getLongitude(),
                    site.getLatitude(),
                    site.getLongitude());

            if (distance < minDistance) {
                minDistance = distance;
                site.setDistanceInKm(distance);
                nearestSite = site;
            }
        }

        return nearestSite;
    }
}