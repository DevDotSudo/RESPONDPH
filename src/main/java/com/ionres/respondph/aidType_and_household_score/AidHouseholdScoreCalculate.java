
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

    public boolean calculateAndSaveAidHouseholdScoreWithDisaster(int beneficiaryId, int aidTypeId, int adminId, int disasterId) {
        try {
            HouseholdScoreData householdScores = dao.getHouseholdScoresWithDisaster(beneficiaryId, disasterId);
            if (householdScores == null) {
                System.err.println("No household scores found for beneficiary ID: " + beneficiaryId +
                        " and disaster ID: " + disasterId);
                return false;
            }

            // Step 2: Get aid type weights from database
            AidTypeWeights aidWeights = dao.getAidTypeWeights(aidTypeId);
            if (aidWeights == null) {
                System.err.println("No aid type weights found for aid type ID: " + aidTypeId);
                return false;
            }

            HouseholdMemberCounts memberCounts = dao.countHouseholdMembers(beneficiaryId);

            System.out.println("========== MEMBER COUNTS DEBUG ==========");
            System.out.println("Total Members: " + memberCounts.totalMembers);
            System.out.println("Able-Bodied Members: " + memberCounts.ableBodyMembers);
            System.out.println("Vulnerable Members: " + memberCounts.vulnerableMembers);
            System.out.println("=========================================");

            double finalScore = calculateFinalScore(householdScores, aidWeights);

            String scoreCategory = determineScoreCategory(finalScore);

            return dao.saveAidHouseholdScoreWithDisaster(beneficiaryId, aidTypeId, adminId, disasterId,
                    householdScores, finalScore, scoreCategory, memberCounts);

        } catch (Exception e) {
            System.err.println("Error calculating aid household score: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean calculateAndSaveAidHouseholdScore(int beneficiaryId, int aidTypeId, int adminId) {
        try {
            HouseholdScoreData householdScores = dao.getHouseholdScores(beneficiaryId);
            if (householdScores == null) {
                System.err.println("No household scores found for beneficiary ID: " + beneficiaryId +
                        " and disaster ID: " );
                return false;
            }

            AidTypeWeights aidWeights = dao.getAidTypeWeights(aidTypeId);
            if (aidWeights == null) {
                System.err.println("No aid type weights found for aid type ID: " + aidTypeId);
                return false;
            }

            // Step 3: Count household members
            HouseholdMemberCounts memberCounts = dao.countHouseholdMembers(beneficiaryId);

            System.out.println("========== MEMBER COUNTS DEBUG ==========");
            System.out.println("Total Members: " + memberCounts.totalMembers);
            System.out.println("Able-Bodied Members: " + memberCounts.ableBodyMembers);
            System.out.println("Vulnerable Members: " + memberCounts.vulnerableMembers);
            System.out.println("=========================================");

            double finalScore = calculateFinalScore(householdScores, aidWeights);

            String scoreCategory = determineScoreCategory(finalScore);

            return dao.saveAidHouseholdScore(beneficiaryId, aidTypeId, adminId,
                    householdScores, finalScore, scoreCategory, memberCounts);

        } catch (Exception e) {
            System.err.println("Error calculating aid household score: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private double calculateFinalScore(HouseholdScoreData household, AidTypeWeights weights) {

        System.out.println("========== CALCULATING FINAL SCORE (SAW METHOD) ==========");
        System.out.println("Formula: FS = Σ(Weight × Score)");
        System.out.println("=========================================================");

        double finalScore = 0.0;

                // Age score (if available)
        if (household.ageScore != null && weights.ageWeight != null) {
            double contribution = weights.ageWeight * household.ageScore;
            finalScore += contribution;
            System.out.printf("Age:           %.4f × %.4f = %.4f\n",
                    weights.ageWeight, household.ageScore, contribution);
        }

//         Gender score
        double genderContribution = weights.genderWeight * household.genderScore;
        finalScore += genderContribution;
        System.out.printf("Gender:        %.4f × %.4f = %.4f\n",
                weights.genderWeight, household.genderScore, genderContribution);

        // Marital status score
        double maritalContribution = weights.maritalStatusWeight * household.maritalStatusScore;
        finalScore += maritalContribution;
        System.out.printf("Marital:       %.4f × %.4f = %.4f\n",
                weights.maritalStatusWeight, household.maritalStatusScore, maritalContribution);

        // Solo parent score
        double soloParentContribution = weights.soloParentWeight * household.soloParentScore;
        finalScore += soloParentContribution;
        System.out.printf("Solo Parent:   %.4f × %.4f = %.4f\n",
                weights.soloParentWeight, household.soloParentScore, soloParentContribution);

        double disabilityContribution = weights.disabilityWeight * household.disabilityScore;
        finalScore += disabilityContribution;
        System.out.printf("Disability:    %.4f × %.4f = %.4f\n",
                weights.disabilityWeight, household.disabilityScore, disabilityContribution);


        // Health condition score
        double healthContribution = weights.healthConditionWeight * household.healthConditionScore;
        finalScore += healthContribution;
        System.out.printf("Health:        %.4f × %.4f = %.4f\n",
                weights.healthConditionWeight, household.healthConditionScore, healthContribution);

        // Clean water access score
        double waterContribution = weights.cleanWaterWeight * household.cleanWaterScore;
        finalScore += waterContribution;
        System.out.printf("Water:         %.4f × %.4f = %.4f\n",
                weights.cleanWaterWeight, household.cleanWaterScore, waterContribution);

        // Sanitation facilities score
        double sanitationContribution = weights.sanitationWeight * household.sanitationScore;
        finalScore += sanitationContribution;
        System.out.printf("Sanitation:    %.4f × %.4f = %.4f\n",
                weights.sanitationWeight, household.sanitationScore, sanitationContribution);

        double houseContribution = weights.houseConstructionWeight * household.houseConstructionScore;
        finalScore += houseContribution;
        System.out.printf("House:         %.4f × %.4f = %.4f\n",
                weights.houseConstructionWeight, household.houseConstructionScore, houseContribution);

                // Ownership score
        double ownershipContribution = weights.ownershipWeight * household.ownershipScore;
        finalScore += ownershipContribution;
        System.out.printf("Ownership:     %.4f × %.4f = %.4f\n",
                weights.ownershipWeight, household.ownershipScore, ownershipContribution);

        double damageContribution = weights.damageSeverityWeight * household.damageSeverityScore;
        finalScore += damageContribution;
        System.out.printf("Damage:        %.4f × %.4f = %.4f\n",
                weights.damageSeverityWeight, household.damageSeverityScore, damageContribution);

        // Employment status score
        double employmentContribution = weights.employmentStatusWeight * household.employmentStatusScore;
        finalScore += employmentContribution;
        System.out.printf("Employment:    %.4f × %.4f = %.4f\n",
                weights.employmentStatusWeight, household.employmentStatusScore, employmentContribution);

        double incomeContribution = weights.monthlyIncomeWeight * household.monthlyIncomeScore;
        finalScore += incomeContribution;
        System.out.printf("Income:        %.4f × %.4f = %.4f\n",
                weights.monthlyIncomeWeight, household.monthlyIncomeScore, incomeContribution);

        // Education level score
        double educationContribution = weights.educationLevelWeight * household.educationLevelScore;
        finalScore += educationContribution;
        System.out.printf("Education:     %.4f × %.4f = %.4f\n",
                weights.educationLevelWeight, household.educationLevelScore, educationContribution);

        // Digital access score
        double digitalContribution = weights.digitalAccessWeight * household.digitalAccessScore;
        finalScore += digitalContribution;
        System.out.printf("Digital:       %.4f × %.4f = %.4f\n",
                weights.digitalAccessWeight, household.digitalAccessScore, digitalContribution);


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
