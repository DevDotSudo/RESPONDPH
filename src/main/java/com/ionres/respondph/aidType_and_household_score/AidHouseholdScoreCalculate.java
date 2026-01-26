package com.ionres.respondph.aidType_and_household_score;

import com.ionres.respondph.aidType_and_household_score.AidHouseholdScoreDAO.*;


public class AidHouseholdScoreCalculate {

    private final AidHouseholdScoreDAO dao;


    public AidHouseholdScoreCalculate() {
        this.dao = new AidHouseholdScoreDAOImpl();
    }
    public AidHouseholdScoreCalculate(AidHouseholdScoreDAO dao) {
        this.dao = dao;
    }


    public boolean calculateAndSaveAidHouseholdScore(int beneficiaryId, int aidTypeId, int adminId) {
        try {
            HouseholdScoreData householdScores = dao.getHouseholdScores(beneficiaryId);
            if (householdScores == null) {
                System.err.println("No household scores found for beneficiary ID: " + beneficiaryId);
                return false;
            }

            // Step 2: Get aid type weights from database
            AidTypeWeights aidWeights = dao.getAidTypeWeights(aidTypeId);
            if (aidWeights == null) {
                System.err.println("No aid type weights found for aid type ID: " + aidTypeId);
                return false;
            }

            // Step 3: Count household members
            HouseholdMemberCounts memberCounts = dao.countHouseholdMembers(beneficiaryId);

            // CRITICAL DEBUG - Check if counts are being retrieved
            System.out.println("========== MEMBER COUNTS DEBUG ==========");
            System.out.println("Total Members: " + memberCounts.totalMembers);
            System.out.println("Able-Bodied Members: " + memberCounts.ableBodyMembers);
            System.out.println("Vulnerable Members: " + memberCounts.vulnerableMembers);
            System.out.println("=========================================");

            // Step 4: Calculate final score (business logic)
            double finalScore = calculateFinalScore(householdScores, aidWeights);

            // Step 5: Determine score category (business logic)
            String scoreCategory = determineScoreCategory(finalScore);

            // Step 6: Save to database via DAO
            return dao.saveAidHouseholdScore(beneficiaryId, aidTypeId, adminId,
                    householdScores, finalScore, scoreCategory, memberCounts);

        } catch (Exception e) {
            System.err.println("Error calculating aid household score: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }// CORRECTED VERSION - Replace calculateFinalScore method in AidHouseholdScoreCalculate.java

    private double calculateFinalScore(HouseholdScoreData household, AidTypeWeights weights) {

        System.out.println("========== CALCULATING FINAL SCORE (SAW METHOD) ==========");
        System.out.println("Formula: FS = Σ(Weight × Score)");
        System.out.println("=========================================================");

        double finalScore = 0.0;


        double disabilityContribution = weights.disabilityWeight * household.disabilityScore;
        finalScore += disabilityContribution;
        System.out.printf("Disability:    %.4f × %.4f = %.4f\n",
                weights.disabilityWeight, household.disabilityScore, disabilityContribution);

        // Health condition score
        double healthContribution = weights.healthConditionWeight * household.healthConditionScore;
        finalScore += healthContribution;
        System.out.printf("Health:        %.4f × %.4f = %.4f\n",
                weights.healthConditionWeight, household.healthConditionScore, healthContribution);


        double houseContribution = weights.houseConstructionWeight * household.houseConstructionScore;
        finalScore += houseContribution;
        System.out.printf("House:         %.4f × %.4f = %.4f\n",
                weights.houseConstructionWeight, household.houseConstructionScore, houseContribution);


        double damageContribution = weights.damageSeverityWeight * household.damageSeverityScore;
        finalScore += damageContribution;
        System.out.printf("Damage:        %.4f × %.4f = %.4f\n",
                weights.damageSeverityWeight, household.damageSeverityScore, damageContribution);


        double incomeContribution = weights.monthlyIncomeWeight * household.monthlyIncomeScore;
        finalScore += incomeContribution;
        System.out.printf("Income:        %.4f × %.4f = %.4f\n",
                weights.monthlyIncomeWeight, household.monthlyIncomeScore, incomeContribution);


        if (household.dependencyRatioScore != null && weights.dependencyRatioWeight != null) {
            double dependencyContribution = weights.dependencyRatioWeight * household.dependencyRatioScore;
            finalScore += dependencyContribution;
            System.out.printf("Dependency:    %.4f × %.4f = %.4f\n",
                    weights.dependencyRatioWeight, household.dependencyRatioScore, dependencyContribution);
        }

        System.out.println("=========================================================");
        System.out.printf("FINAL SCORE (FS = Σ[Weight × Score]): %.4f\n", finalScore);
        System.out.println("=========================================================");

        return Math.round(finalScore * 100.0) / 100.0;
    }

    private String determineScoreCategory(double finalScore) {
        if (finalScore >= 0.40) {
            return "Very High Priority";
        } else if (finalScore >= 0.30) {
            return "High Priority";
        } else if (finalScore >= 0.20) {
            return "Medium Priority";
        } else if (finalScore >= 0.10) {
            return "Low Priority";
        } else {
            return "Very Low Priority";
        }
    }
}


