package com.ionres.respondph.household_score;

import com.ionres.respondph.aidType_and_household_score.AidHouseholdScoreCascadeUpdater;
import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.disaster_damage.DisasterDamageModel;
import com.ionres.respondph.familymembers.FamilyMembersModel;
import com.ionres.respondph.util.UpdateTrigger;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.vulnerability_indicator.VulnerabilityIndicatorScoreModel;
import java.util.List;

public class HouseholdScoreCalculate {

    private static final boolean DEBUG = true;
    private final Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");
    private final HouseholdScoreDAO dao;

    public HouseholdScoreCalculate() {
        this.dao = new HouseholdScoreDAOServiceImpl();
    }

    public HouseholdScoreCalculate(HouseholdScoreDAO dao) {
        this.dao = dao;
    }



    // ✅ MODIFIED: Add disasterId parameter
    public boolean calculateAndSaveHouseholdScore(int beneficiaryId, int disasterId) {
        try {
            VulnerabilityIndicatorScoreModel vulnScores = dao.getVulnerabilityScores();
            if (vulnScores == null) {
                System.err.println("No vulnerability scores found in database");
                return false;
            }

            BeneficiaryModel beneficiary = dao.getBeneficiaryById(beneficiaryId);
            if (beneficiary == null) {
                System.err.println("Beneficiary not found: " + beneficiaryId);
                return false;
            }

            List<FamilyMembersModel> familyMembers = dao.getFamilyMembersByBeneficiaryId(beneficiaryId);

            // ✅ Calculate scores for this specific disaster
            HouseholdScoreModel householdScore = calculateScoresWithDisaster(beneficiary, familyMembers, vulnScores, disasterId);
            householdScore.setBeneficiaryId(beneficiaryId);
            householdScore.setDisasterId(disasterId);  // ✅ NEW

            return dao.saveHouseholdScoreWithDisaster(householdScore);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean autoRecalculateHouseholdScore(int beneficiaryId) {
        try {
            VulnerabilityIndicatorScoreModel vulnScores = dao.getVulnerabilityScores();
            if (vulnScores == null) {
                System.err.println("Auto-recalculation failed: wala vulnerability scores");
                return false;
            }

            BeneficiaryModel beneficiary = dao.getBeneficiaryById(beneficiaryId);
            if (beneficiary == null) {
                System.err.println("Auto-recalculation failed: Beneficiary not found - " + beneficiaryId);
                return false;
            }

            List<FamilyMembersModel> familyMembers = dao.getFamilyMembersByBeneficiaryId(beneficiaryId);

            HouseholdScoreModel householdScore = calculateScores(beneficiary, familyMembers, vulnScores);
            householdScore.setBeneficiaryId(beneficiaryId);

            return dao.saveHouseholdScore(householdScore);

        } catch (Exception e) {
            System.err.println("Auto-recalculation error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public int recalculateAllHouseholdScores() {
        int recalculatedCount = 0;
        List<Integer> beneficiaryIds = dao.getAllBeneficiaryIdsWithHouseholdScores();

        System.out.println("========== RECALCULATING ALL HOUSEHOLD SCORES ==========");
        System.out.println("Found " + beneficiaryIds.size() + " households to recalculate");

        for (Integer beneficiaryId : beneficiaryIds) {
            try {
                boolean success = autoRecalculateHouseholdScore(beneficiaryId);

                UpdateTrigger beneficiaryUpdateTrigger = new UpdateTrigger();
                beneficiaryUpdateTrigger.triggerCascadeUpdate(beneficiaryId);

                if (success) {
                    recalculatedCount++;
                    System.out.println(" Recalculated household score for beneficiary ID: " + beneficiaryId);
                } else {
                    System.err.println(" Failed to recalculate household score for beneficiary ID: " + beneficiaryId);
                }
            } catch (Exception e) {
                System.err.println(" Error recalculating household score for beneficiary ID " + beneficiaryId + ": " + e.getMessage());
            }
        }

        System.out.println("========== RECALCULATION COMPLETE ==========");
        System.out.println("Successfully recalculated: " + recalculatedCount + " out of " + beneficiaryIds.size());

        return recalculatedCount;
    }



    public int recalculateAllHouseholdScoresWithAidCascade() {
        int recalculatedCount = 0;
        List<Integer> beneficiaryIds = dao.getAllBeneficiaryIdsWithHouseholdScores();

        System.out.println("========== RECALCULATING ALL HOUSEHOLD SCORES WITH CASCADE ==========");
        System.out.println("Found " + beneficiaryIds.size() + " households to recalculate");

        for (Integer beneficiaryId : beneficiaryIds) {
            try {
                boolean success = autoRecalculateHouseholdScore(beneficiaryId);

                UpdateTrigger beneficiaryUpdateTrigger = new UpdateTrigger();
                beneficiaryUpdateTrigger.triggerCascadeUpdate(beneficiaryId);

                if (success) {
                    recalculatedCount++;
                    System.out.println("✓ Recalculated household score for beneficiary ID: " + beneficiaryId);

                    // CASCADE: Update aid scores for this specific beneficiary
                    AidHouseholdScoreCascadeUpdater aidUpdater = new AidHouseholdScoreCascadeUpdater();
                    aidUpdater.recalculateAidScoresForBeneficiary(beneficiaryId);

                } else {
                    System.err.println("✗ Failed to recalculate household score for beneficiary ID: " + beneficiaryId);
                }
            } catch (Exception e) {
                System.err.println("✗ Error recalculating household score for beneficiary ID " + beneficiaryId + ": " + e.getMessage());
            }
        }

        System.out.println("========== RECALCULATION COMPLETE ==========");
        System.out.println("Successfully recalculated: " + recalculatedCount + " out of " + beneficiaryIds.size());

        return recalculatedCount;
    }


    private double calculateAverageAgeScore(BeneficiaryModel beneficiary,
                                            List<FamilyMembersModel> familyMembers) {
        double totalAgeScore = beneficiary.getAgeScore();
        int totalMembers = 1 + familyMembers.size();

        debug("Beneficiary Age Score", beneficiary.getAgeScore());

        for (int i = 0; i < familyMembers.size(); i++) {
            FamilyMembersModel fm = familyMembers.get(i);
            double fmAgeScore = fm.getAgeScore();
            debug("Family Member #" + (i + 1) + " Age Score", fmAgeScore);
            totalAgeScore += fmAgeScore;
        }

        double avgAgeScore = totalAgeScore / totalMembers;
        debug("Total Age Score (sum)", totalAgeScore);
        debug("Average Age Score", avgAgeScore);

        return Math.round(avgAgeScore * 100.0) / 100.0;
    }




    private double calculateDependencyRatioScore(BeneficiaryModel beneficiary,
                                                 List<FamilyMembersModel> familyMembers) {
        try {
            int vulnerableCount = 0;
            int ableBodiedCount = 0;
            int totalMembers = 1 + familyMembers.size();

            debug("========== DEPENDENCY RATIO CALCULATION ==========", "");
            debug("Total household members", totalMembers);

            // Process Beneficiary
            String beneDisability;
            String beneHealth;

            try {
                beneDisability = cs.decryptWithOneParameter(beneficiary.getDisabilityType());
                beneHealth = cs.decryptWithOneParameter(beneficiary.getHealthCondition());
            } catch (Exception e) {
                debug("Error decrypting beneficiary data", e.getMessage());
                beneDisability = "None";
                beneHealth = "Healthy";
            }

            boolean beneIsVulnerable = isVulnerablePerson(
                    beneficiary.getAgeScore(),
                    beneDisability,
                    beneHealth
            );

            if (beneIsVulnerable) {
                vulnerableCount++;
                debug("Beneficiary", "VULNERABLE (Age Score: " + beneficiary.getAgeScore() + ")");
                debug("Beneficiary", "VULNERABLE (Disability:" + beneDisability + " )");
                debug("Beneficiary", "VULNERABLE (Health:" + beneHealth + " )");
            } else {
                ableBodiedCount++;
                debug("Beneficiary", "ABLE-BODIED ADULT");
                debug("Beneficiary", "VULNERABLE (Age Score: " + beneficiary.getAgeScore() + ")");
                debug("Beneficiary", "VULNERABLE (Disability:" + beneDisability + " )");
                debug("Beneficiary", "VULNERABLE (Health:" + beneHealth + " )");
            }

            int memberIndex = 1;
            for (FamilyMembersModel fm : familyMembers) {
                String fmDisability;
                String fmHealth;

                try {
                    fmDisability = cs.decryptWithOneParameter(fm.getDisabilityType());
                    fmHealth = cs.decryptWithOneParameter(fm.getHealthCondition());
                } catch (Exception e) {
                    debug("Error decrypting family member #" + memberIndex, e.getMessage());
                    fmDisability = "None";
                    fmHealth = "Healthy";
                }

                boolean fmIsVulnerable = isVulnerablePerson(
                        fm.getAgeScore(),
                        fmDisability,
                        fmHealth
                );

                if (fmIsVulnerable) {
                    vulnerableCount++;
                    debug("Family Member #" + memberIndex,
                            "VULNERABLE (Age Score: " + fm.getAgeScore() + ")");

                    debug("Family Member", "VULNERABLE (Age Score: " + fm.getAgeScore() + ")");
                    debug("Family Member", "VULNERABLE (Disability:" + fmDisability + " )");
                    debug("Family Member", "VULNERABLE (Health:" + fmHealth + " )");
                } else {
                    ableBodiedCount++;
                    debug("Family Member #" + memberIndex, "ABLE-BODIED ADULT");

                    debug("Family Member", "VULNERABLE (Age Score: " + fm.getAgeScore() + ")");
                    debug("Family Member", "VULNERABLE (Disability:" + fmDisability + " )");
                    debug("Family Member", "VULNERABLE (Health:" + fmHealth + " )");
                }
                memberIndex++;
            }

            debug("Total Vulnerable Members (count)", vulnerableCount);
            debug("Total Able-Bodied Adults (count)", ableBodiedCount);

            double dependencyRatio;

            if (ableBodiedCount == 0) {
                dependencyRatio = vulnerableCount > 0 ? 999.0 : 0.0;
                debug("Dependency Ratio (raw)", "NO ABLE-BODIED ADULTS - using " + dependencyRatio);
            } else {
                dependencyRatio = (double) vulnerableCount / ableBodiedCount;
                debug("Dependency Ratio (raw)", dependencyRatio);
            }

            double vulnerabilityScore;

            if (dependencyRatio >= 2.00) {
                vulnerabilityScore = 1.00;
                debug("DR Range", ">=2.00 (Severe)");
            } else if (dependencyRatio >= 1.00) {
                vulnerabilityScore = 0.67;
                debug("DR Range", "1.00-1.99 (High)");
            } else if (dependencyRatio >= 0.50) {
                vulnerabilityScore = 0.33;
                debug("DR Range", "0.50-0.99 (Moderate)");
            } else {
                vulnerabilityScore = 0.00;  // Low
                debug("DR Range", "0.00-0.49 (Low)");
            }

            debug("Final Dependency Ratio", dependencyRatio);
            debug("Final Vulnerability Score", vulnerabilityScore);
            debug("========== END DEPENDENCY RATIO CALCULATION ==========", "");

            return vulnerabilityScore;

        } catch (Exception e) {
            System.err.println("Error calculating dependency ratio score: " + e.getMessage());
            e.printStackTrace();
            return 0.67;
        }
    }

    private boolean isVulnerablePerson(double ageScore,
                                       String disabilityType,
                                       String healthCondition) {

        if (ageScore == 1.0 || ageScore == 0.7) {
            return true;
        }

        if (disabilityType != null && !disabilityType.trim().equalsIgnoreCase("none")) {
            return true;
        }

        if (healthCondition != null) {
            String hc = healthCondition.trim().toLowerCase();

            if (hc.contains("chronically ill") ||
                    hc.contains("immunocompromised") ||
                    hc.contains("temporarily ill") ||
                    hc.contains("medical equipment") ||
                    hc.contains("terminal illness") ||
                    hc.contains("with Medical Equipment Dependence")) {
                return true;
            }
        }

        return false;
    }

    private HouseholdScoreModel calculateScoresWithDisaster(
            BeneficiaryModel beneficiary,
            List<FamilyMembersModel> familyMembers,
            VulnerabilityIndicatorScoreModel vulnScores, int disasterId) {

        HouseholdScoreModel result = new HouseholdScoreModel();

        try {
            int totalMembers = 1 + familyMembers.size();
            debug("Total household members", totalMembers);

            double avgAgeScore = calculateAverageAgeScore(beneficiary, familyMembers);
            result.setAgeScore(avgAgeScore);
            debug("Final Average Age Score", avgAgeScore);

            debug("Beneficiary Gender (encrypted)", beneficiary.getGender());
            debug("Beneficiary Marital Status (encrypted)", beneficiary.getMaritalStatus());
            debug("Beneficiary Solo Parent Status (encrypted)", beneficiary.getSoloParentStatus());
            debug("Beneficiary Disability Type (encrypted)", beneficiary.getDisabilityType());
            debug("Beneficiary Health Condition (encrypted)", beneficiary.getHealthCondition());
            debug("Beneficiary Clean Water Access (encrypted)", beneficiary.getCleanWaterAccess());
            debug("Beneficiary Sanitation Facility (encrypted)", beneficiary.getSanitationFacility());
            debug("Beneficiary House Type (encrypted)", beneficiary.getHouseType());
            debug("Beneficiary Ownership Status (encrypted)", beneficiary.getOwnerShipStatus());
            debug("Beneficiary Employment Status (encrypted)", beneficiary.getEmploymentStatus());
            debug("Beneficiary Monthly Income (encrypted)", beneficiary.getMonthlyIncome());
            debug("Beneficiary Education Level (encrypted)", beneficiary.getEducationalLevel());
            debug("Beneficiary Digital Access (encrypted)", beneficiary.getDigitalAccess());

            String beneGender = cs.decryptWithOneParameter(beneficiary.getGender());
            String beneMaritalStatus = cs.decryptWithOneParameter(beneficiary.getMaritalStatus());
            String beneSoloParentStatus = cs.decryptWithOneParameter(beneficiary.getSoloParentStatus());
            String beneDisabilityType = cs.decryptWithOneParameter(beneficiary.getDisabilityType());
            String beneHealthCondition = cs.decryptWithOneParameter(beneficiary.getHealthCondition());
            String beneCleanWaterAccess = cs.decryptWithOneParameter(beneficiary.getCleanWaterAccess());
            String beneSanitationFacility = cs.decryptWithOneParameter(beneficiary.getSanitationFacility());
            String beneHouseType = cs.decryptWithOneParameter(beneficiary.getHouseType());
            String beneOwnershipStatus = cs.decryptWithOneParameter(beneficiary.getOwnerShipStatus());
            String beneEmploymentStatus = cs.decryptWithOneParameter(beneficiary.getEmploymentStatus());
            String beneMonthlyIncome = cs.decryptWithOneParameter(beneficiary.getMonthlyIncome());
            String beneEducationLevel = cs.decryptWithOneParameter(beneficiary.getEducationalLevel());
            String beneDigitalAccess = cs.decryptWithOneParameter(beneficiary.getDigitalAccess());

            double beneGenderScore = getGenderScore(beneGender, vulnScores);
            double beneMaritalScore = getMaritalStatusScore(beneMaritalStatus, vulnScores);
            double beneSoloParentScore = getSoloParentScore(beneSoloParentStatus, vulnScores);
            double beneDisabilityScore = getDisabilityScore(beneDisabilityType, vulnScores);
            double beneHealthScore = getHealthScore(beneHealthCondition, vulnScores);
            double beneCleanWaterScore = getCleanWaterScore(beneCleanWaterAccess, vulnScores);
            double beneSanitationScore = getSanitationScore(beneSanitationFacility, vulnScores);
            double beneHouseTypeScore = getHouseTypeScore(beneHouseType, vulnScores);
            double beneOwnershipScore = getOwnershipScore(beneOwnershipStatus, vulnScores);
            double beneEmploymentScore = getEmploymentScore(beneEmploymentStatus, vulnScores);
            double beneIncomeScore = getIncomeScore(beneMonthlyIncome, vulnScores);
            double beneEducationScore = getEducationScore(beneEducationLevel, vulnScores);
            double beneDigitalScore = getDigitalAccessScore(beneDigitalAccess, vulnScores);

            // ✅✅✅ FIXED: Get damage score for THIS SPECIFIC DISASTER ✅✅✅
            double beneDisasterDamageScore = 0.0;
            List<DisasterDamageModel> allDisasterDamages = dao.getDisasterDamageById(beneficiary.getId());

            debug("========== DISASTER DAMAGE LOOKUP ==========", "");
            debug("Total disaster damages for beneficiary", allDisasterDamages.size());
            debug("Looking for damage from Disaster ID", disasterId);

            boolean damageFound = false;
            for (DisasterDamageModel damage : allDisasterDamages) {
                debug("Checking disaster damage record with Disaster ID", damage.getDisasterId());

                if (damage.getDisasterId() == disasterId) {
                    String houseDamageEncrypted = damage.getHouseDamageSeverity();
                    if (houseDamageEncrypted != null && !houseDamageEncrypted.isEmpty()) {
                        String houseDamageDecrypted = cs.decryptWithOneParameter(houseDamageEncrypted);
                        beneDisasterDamageScore = getDisasterDamageScore(houseDamageDecrypted, vulnScores);
                        debug("✓ Found damage for Disaster ID " + disasterId, houseDamageDecrypted);
                        debug("✓ Damage Score", beneDisasterDamageScore);
                        damageFound = true;
                    }
                    break; // Stop searching once we find the matching disaster
                }
            }

            if (!damageFound) {
                debug("⚠ No damage record found for Disaster ID", disasterId);
                debug("⚠ Damage score will be", 0.0);
            }
            debug("========== END DISASTER DAMAGE LOOKUP ==========", "");
            // ✅✅✅ END OF FIX ✅✅✅

            double totalGenderScore = beneGenderScore;
            double totalMaritalScore = beneMaritalScore;
            double totalDisabilityScore = beneDisabilityScore;
            double totalHealthScore = beneHealthScore;
            double totalEmploymentScore = beneEmploymentScore;
            double totalEducationScore = beneEducationScore;

            for (FamilyMembersModel fm : familyMembers) {
                String fmGender = cs.decryptWithOneParameter(fm.getGender());
                String fmMaritalStatus = cs.decryptWithOneParameter(fm.getMaritalStatus());
                String fmDisabilityType = cs.decryptWithOneParameter(fm.getDisabilityType());
                String fmHealthCondition = cs.decryptWithOneParameter(fm.getHealthCondition());
                String fmEmploymentStatus = cs.decryptWithOneParameter(fm.getEmploymentStatus());
                String fmEducationLevel = cs.decryptWithOneParameter(fm.getEducationalLevel());

                totalGenderScore += getGenderScore(fmGender, vulnScores);
                totalMaritalScore += getMaritalStatusScore(fmMaritalStatus, vulnScores);
                totalDisabilityScore += getDisabilityScore(fmDisabilityType, vulnScores);
                totalHealthScore += getHealthScore(fmHealthCondition, vulnScores);
                totalEmploymentScore += getEmploymentScore(fmEmploymentStatus, vulnScores);
                totalEducationScore += getEducationScore(fmEducationLevel, vulnScores);
            }

            double avgGenderScore = totalGenderScore / totalMembers;
            double avgMaritalScore = totalMaritalScore / totalMembers;
            double avgDisabilityScore = totalDisabilityScore / totalMembers;
            double avgHealthScore = totalHealthScore / totalMembers;
            double avgEmploymentScore = totalEmploymentScore / totalMembers;
            double avgEducationScore = totalEducationScore / totalMembers;

            result.setGenderScore(Math.round(avgGenderScore * 100.0) / 100.0);
            result.setMaritalStatusScore(Math.round(avgMaritalScore * 100.0) / 100.0);
            result.setDisabilityScore(Math.round(avgDisabilityScore * 100.0) / 100.0);
            result.setHealthConditionScore(Math.round(avgHealthScore * 100.0) / 100.0);
            result.setEmploymentStatusScore(Math.round(avgEmploymentScore * 100.0) / 100.0);
            result.setEducationLevelScore(Math.round(avgEducationScore * 100.0) / 100.0);

            result.setSoloParentScore(Math.round(beneSoloParentScore * 100.0) / 100.0);
            result.setAccessToCleanWaterScore(Math.round(beneCleanWaterScore * 100.0) / 100.0);
            result.setSanitationFacilitiesScore(Math.round(beneSanitationScore * 100.0) / 100.0);
            result.setHouseConstructionTypeScore(Math.round(beneHouseTypeScore * 100.0) / 100.0);
            result.setOwnershipScore(Math.round(beneOwnershipScore * 100.0) / 100.0);
            result.setMonthlyIncomeScore(Math.round(beneIncomeScore * 100.0) / 100.0);
            result.setDigitalAccessScore(Math.round(beneDigitalScore * 100.0) / 100.0);
            result.setDamageSeverityScore(Math.round(beneDisasterDamageScore * 100.0) / 100.0);

            double dependencyRatioScore = calculateDependencyRatioScore(beneficiary, familyMembers);
            result.setDependencyRatioScore(dependencyRatioScore);
            debug("Final Dependency Ratio Score", dependencyRatioScore);

            debug("========== CALCULATION COMPLETE ==========", "");

        } catch (Exception e) {
            System.err.println("========== ERROR IN CALCULATION ==========");
            System.err.println("Error calculating scores: " + e.getMessage());
            e.printStackTrace();
            System.err.println("==========================================");
        }

        return result;
    }



    private HouseholdScoreModel calculateScores(
            BeneficiaryModel beneficiary,
            List<FamilyMembersModel> familyMembers,
            VulnerabilityIndicatorScoreModel vulnScores) {

        HouseholdScoreModel result = new HouseholdScoreModel();

        try {
            int totalMembers = 1 + familyMembers.size();
            debug("Total household members", totalMembers);

            double avgAgeScore = calculateAverageAgeScore(beneficiary, familyMembers);
            result.setAgeScore(avgAgeScore);
            debug("Final Average Age Score", avgAgeScore);

            debug("Beneficiary Gender (encrypted)", beneficiary.getGender());
            debug("Beneficiary Marital Status (encrypted)", beneficiary.getMaritalStatus());
            debug("Beneficiary Solo Parent Status (encrypted)", beneficiary.getSoloParentStatus());
            debug("Beneficiary Disability Type (encrypted)", beneficiary.getDisabilityType());
            debug("Beneficiary Health Condition (encrypted)", beneficiary.getHealthCondition());
            debug("Beneficiary Clean Water Access (encrypted)", beneficiary.getCleanWaterAccess());
            debug("Beneficiary Sanitation Facility (encrypted)", beneficiary.getSanitationFacility());
            debug("Beneficiary House Type (encrypted)", beneficiary.getHouseType());
            debug("Beneficiary Ownership Status (encrypted)", beneficiary.getOwnerShipStatus());
            debug("Beneficiary Employment Status (encrypted)", beneficiary.getEmploymentStatus());
            debug("Beneficiary Monthly Income (encrypted)", beneficiary.getMonthlyIncome());
            debug("Beneficiary Education Level (encrypted)", beneficiary.getEducationalLevel());
            debug("Beneficiary Digital Access (encrypted)", beneficiary.getDigitalAccess());

            String beneGender = cs.decryptWithOneParameter(beneficiary.getGender());
            String beneMaritalStatus = cs.decryptWithOneParameter(beneficiary.getMaritalStatus());
            String beneSoloParentStatus = cs.decryptWithOneParameter(beneficiary.getSoloParentStatus());
            String beneDisabilityType = cs.decryptWithOneParameter(beneficiary.getDisabilityType());
            String beneHealthCondition = cs.decryptWithOneParameter(beneficiary.getHealthCondition());
            String beneCleanWaterAccess = cs.decryptWithOneParameter(beneficiary.getCleanWaterAccess());
            String beneSanitationFacility = cs.decryptWithOneParameter(beneficiary.getSanitationFacility());
            String beneHouseType = cs.decryptWithOneParameter(beneficiary.getHouseType());
            String beneOwnershipStatus = cs.decryptWithOneParameter(beneficiary.getOwnerShipStatus());
            String beneEmploymentStatus = cs.decryptWithOneParameter(beneficiary.getEmploymentStatus());
            String beneMonthlyIncome = cs.decryptWithOneParameter(beneficiary.getMonthlyIncome());
            String beneEducationLevel = cs.decryptWithOneParameter(beneficiary.getEducationalLevel());
            String beneDigitalAccess = cs.decryptWithOneParameter(beneficiary.getDigitalAccess());

            double beneGenderScore = getGenderScore(beneGender, vulnScores);
            double beneMaritalScore = getMaritalStatusScore(beneMaritalStatus, vulnScores);
            double beneSoloParentScore = getSoloParentScore(beneSoloParentStatus, vulnScores);
            double beneDisabilityScore = getDisabilityScore(beneDisabilityType, vulnScores);
            double beneHealthScore = getHealthScore(beneHealthCondition, vulnScores);
            double beneCleanWaterScore = getCleanWaterScore(beneCleanWaterAccess, vulnScores);
            double beneSanitationScore = getSanitationScore(beneSanitationFacility, vulnScores);
            double beneHouseTypeScore = getHouseTypeScore(beneHouseType, vulnScores);
            double beneOwnershipScore = getOwnershipScore(beneOwnershipStatus, vulnScores);
            double beneEmploymentScore = getEmploymentScore(beneEmploymentStatus, vulnScores);
            double beneIncomeScore = getIncomeScore(beneMonthlyIncome, vulnScores);
            double beneEducationScore = getEducationScore(beneEducationLevel, vulnScores);
            double beneDigitalScore = getDigitalAccessScore(beneDigitalAccess, vulnScores);

            double beneDisasterDamageScore = 0.0;
            List<DisasterDamageModel> disasterDamages = dao.getDisasterDamageById(beneficiary.getId());
            if (!disasterDamages.isEmpty()) {
                DisasterDamageModel latestDamage = disasterDamages.get(disasterDamages.size() - 1);
                String houseDamageEncrypted = latestDamage.getHouseDamageSeverity();
                if (houseDamageEncrypted != null && !houseDamageEncrypted.isEmpty()) {
                    String houseDamageDecrypted = cs.decryptWithOneParameter(houseDamageEncrypted);
                    beneDisasterDamageScore = getDisasterDamageScore(houseDamageDecrypted, vulnScores);
                }
            }

            double totalGenderScore = beneGenderScore;
            double totalMaritalScore = beneMaritalScore;
            double totalDisabilityScore = beneDisabilityScore;
            double totalHealthScore = beneHealthScore;
            double totalEmploymentScore = beneEmploymentScore;
            double totalEducationScore = beneEducationScore;

            for (FamilyMembersModel fm : familyMembers) {
                String fmGender = cs.decryptWithOneParameter(fm.getGender());
                String fmMaritalStatus = cs.decryptWithOneParameter(fm.getMaritalStatus());
                String fmDisabilityType = cs.decryptWithOneParameter(fm.getDisabilityType());
                String fmHealthCondition = cs.decryptWithOneParameter(fm.getHealthCondition());
                String fmEmploymentStatus = cs.decryptWithOneParameter(fm.getEmploymentStatus());
                String fmEducationLevel = cs.decryptWithOneParameter(fm.getEducationalLevel());

                totalGenderScore += getGenderScore(fmGender, vulnScores);
                totalMaritalScore += getMaritalStatusScore(fmMaritalStatus, vulnScores);
                totalDisabilityScore += getDisabilityScore(fmDisabilityType, vulnScores);
                totalHealthScore += getHealthScore(fmHealthCondition, vulnScores);
                totalEmploymentScore += getEmploymentScore(fmEmploymentStatus, vulnScores);
                totalEducationScore += getEducationScore(fmEducationLevel, vulnScores);
            }

            double avgGenderScore = totalGenderScore / totalMembers;
            double avgMaritalScore = totalMaritalScore / totalMembers;
            double avgDisabilityScore = totalDisabilityScore / totalMembers;
            double avgHealthScore = totalHealthScore / totalMembers;
            double avgEmploymentScore = totalEmploymentScore / totalMembers;
            double avgEducationScore = totalEducationScore / totalMembers;

            result.setGenderScore(Math.round(avgGenderScore * 100.0) / 100.0);
            result.setMaritalStatusScore(Math.round(avgMaritalScore * 100.0) / 100.0);
            result.setDisabilityScore(Math.round(avgDisabilityScore * 100.0) / 100.0);
            result.setHealthConditionScore(Math.round(avgHealthScore * 100.0) / 100.0);
            result.setEmploymentStatusScore(Math.round(avgEmploymentScore * 100.0) / 100.0);
            result.setEducationLevelScore(Math.round(avgEducationScore * 100.0) / 100.0);

            result.setSoloParentScore(Math.round(beneSoloParentScore * 100.0) / 100.0);
            result.setAccessToCleanWaterScore(Math.round(beneCleanWaterScore * 100.0) / 100.0);
            result.setSanitationFacilitiesScore(Math.round(beneSanitationScore * 100.0) / 100.0);
            result.setHouseConstructionTypeScore(Math.round(beneHouseTypeScore * 100.0) / 100.0);
            result.setOwnershipScore(Math.round(beneOwnershipScore * 100.0) / 100.0);
            result.setMonthlyIncomeScore(Math.round(beneIncomeScore * 100.0) / 100.0);
            result.setDigitalAccessScore(Math.round(beneDigitalScore * 100.0) / 100.0);
            result.setDamageSeverityScore(Math.round(beneDisasterDamageScore * 100.0) / 100.0);

            double dependencyRatioScore = calculateDependencyRatioScore(beneficiary, familyMembers);
            result.setDependencyRatioScore(dependencyRatioScore);
            debug("Final Dependency Ratio Score", dependencyRatioScore);

            debug("========== CALCULATION COMPLETE ==========", "");

        } catch (Exception e) {
            System.err.println("========== ERROR IN CALCULATION ==========");
            System.err.println("Error calculating scores: " + e.getMessage());
            e.printStackTrace();
            System.err.println("==========================================");
        }

        return result;
    }

    // ==================== SCORE MAPPING METHODS ====================

    private double getGenderScore(String gender, VulnerabilityIndicatorScoreModel vulnScores) {
        if (gender == null) return 0.0;
        switch (gender.trim().toLowerCase()) {
            case "male": return vulnScores.getMaleScore();
            case "female": return vulnScores.getFemaleScore();
            default: return 0.0;
        }
    }

    private double getMaritalStatusScore(String status, VulnerabilityIndicatorScoreModel vulnScores) {
        if (status == null) return 0.0;
        switch (status.trim().toLowerCase()) {
            case "single": return vulnScores.getSingleScore();
            case "married": return vulnScores.getMarriedScore();
            case "widowed": return vulnScores.getWidowedScore();
            case "separated": return vulnScores.getSeparatedScore();
            default: return 0.0;
        }
    }

    private double getSoloParentScore(String status, VulnerabilityIndicatorScoreModel vulnScores) {
        if (status == null) return 0.0;
        switch (status.trim().toLowerCase()){
            case "not a solo parent": return  vulnScores.getSoloParentNotSoloParentScore();
            case "solo parent (with support network)": return  vulnScores.getSoloParentSpWithSnScore();
            case "solo parent (without support)": return  vulnScores.getSoloParentSpWithoutSnScore();
            default: return 0.0;
        }

    }

    private double getDisabilityScore(String type, VulnerabilityIndicatorScoreModel vulnScores) {
        if (type == null) return vulnScores.getDisabilityNoneScore();
        switch (type.trim().toLowerCase()) {
            case "none": return vulnScores.getDisabilityNoneScore();
            case "physical": return vulnScores.getDisabilityPhysicalScore();
            case "visual": return vulnScores.getDisabilityVisualScore();
            case "hearing": return vulnScores.getDisabilityHearingScore();
            case "speech": return vulnScores.getDisabilitySpeechScore();
            case "intellectual": return vulnScores.getDisabilityIntellectualScore();
            case "mental/psychosocial": return vulnScores.getDisabilityMentalScore();
            case "due to chronic illness": return vulnScores.getDisabilityChronicScore();
            case "multiple disabilities": return vulnScores.getDisabilityMultipleScore();
            default: return vulnScores.getDisabilityNoneScore();
        }
    }

    private double getHealthScore(String condition, VulnerabilityIndicatorScoreModel vulnScores) {
        if (condition == null) return vulnScores.getHealthHealthyScore();
        String lower = condition.trim().toLowerCase();
        if (lower.contains("healthy")) return vulnScores.getHealthHealthyScore();
        if (lower.contains("temporarily ill")) return vulnScores.getHealthTemporarilyIllScore();
        if (lower.contains("chronically ill")) return vulnScores.getHealthChronicallyIllScore();
        if (lower.contains("immunocompromised")) return vulnScores.getHealthImmunocompromisedScore();
        if (lower.contains("terminally ill")) return vulnScores.getHealthTerminallyIllScore();
        if (lower.contains("with medical equipment dependence")) return vulnScores.getHealthWithMedicalScore();
        return vulnScores.getHealthHealthyScore();
    }

    private double getCleanWaterScore(String access, VulnerabilityIndicatorScoreModel vulnScores) {
        if (access == null) return 0.0;
        String lower = access.trim().toLowerCase();
        if (lower.contains("daily access")) return vulnScores.getCleanWaterYesScore();
        if (lower.contains("no access")) return vulnScores.getCleanWaterNoScore();
        if (lower.contains("irregular")) return vulnScores.getCleanWaterIrregularScore();
        return 0.0;
    }

    private double getSanitationScore(String facility, VulnerabilityIndicatorScoreModel vulnScores) {
        if (facility == null) return 0.0;
        String lower = facility.trim().toLowerCase();
        if (lower.contains("safely managed")) return vulnScores.getSanitationSafelyScore();
        if (lower.contains("shared")) return vulnScores.getSanitationSharedScore();
        if (lower.contains("unimproved")) return vulnScores.getSanitationUnimprovedScore();
        if (lower.contains("no sanitation")) return vulnScores.getSanitationNoScore();
        return 0.0;
    }

    private double getHouseTypeScore(String type, VulnerabilityIndicatorScoreModel vulnScores) {
        if (type == null) return 0.0;
        String lower = type.trim().toLowerCase();
        // Check specific "semi-concrete" BEFORE general "concrete"
        if (lower.contains("semi-concrete"))
            return vulnScores.getHouseSemiConcreteScore();
        if (lower.contains("concrete") || lower.contains("masonry"))
            return vulnScores.getHouseConcreteScore();
        if (lower.contains("light materials"))
            return vulnScores.getHouseLightMaterialsScore();
        if (lower.contains("makeshift"))
            return vulnScores.getHouseMakeshiftScore();
        return 0.0;
    }

    private double getOwnershipScore(String status, VulnerabilityIndicatorScoreModel vulnScores) {
        if (status == null) return 0.0;
        String lower = status.trim().toLowerCase();
        if (lower.contains("owned with formal")) return vulnScores.getOwnershipOwnedScore();
        if (lower.contains("owned without")) return vulnScores.getOwnershipOwnedWithoutScore();
        if (lower.contains("rented")) return vulnScores.getOwnershipRentedScore();
        if (lower.contains("informal")) return vulnScores.getOwnershipInformalScore();
        if (lower.contains("evicted")) return vulnScores.getOwnershipEvictedScore();
        return 0.0;
    }

    private double getEmploymentScore(String status, VulnerabilityIndicatorScoreModel vulnScores) {
        if (status == null) return 0.0;
        String lower = status.trim().toLowerCase();
        if (lower.contains("irregular")) return vulnScores.getEmploymentIrregularScore();
        if (lower.contains("regular")) return vulnScores.getEmploymentRegularScore();
        if (lower.contains("self-employed with stable"))
            return vulnScores.getEmploymentSelfEmployedStableScore();
        if (lower.contains("self-employed with unstable"))
            return vulnScores.getEmploymentSelfEmployedUnstableScore();
        if (lower.contains("unemployed")) return vulnScores.getEmploymentUnemployedScore();
        return 0.0;
    }

    private double getIncomeScore(String income, VulnerabilityIndicatorScoreModel vulnScores) {
        if (income == null) return 0.0;
        String lower = income.trim().toLowerCase();
        if (lower.contains("poor")) return vulnScores.getIncomePoorScore();
        if (lower.contains("low-income")) return vulnScores.getIncomeLowIncomeScore();
        if (lower.contains("lower middle")) return vulnScores.getIncomeMiddleIncomeScore();
        if (lower.contains("middle class")) return vulnScores.getIncomeMiddleClassScore();
        if (lower.contains("upper middle")) return vulnScores.getIncomeUpperMiddleClassScore();
        if (lower.contains("upper income")) return vulnScores.getIncomeUpperIncomeScore();
        if (lower.contains("rich")) return vulnScores.getIncomeRichScore();
        return 0.0;
    }

    private double getEducationScore(String level, VulnerabilityIndicatorScoreModel vulnScores) {
        if (level == null) return 0.0;
        String lower = level.trim().toLowerCase();
        if (lower.contains("no formal")) return vulnScores.getEducationNoFormalScore();
        if (lower.contains("elementary")) return vulnScores.getEducationElementaryScore();
        if (lower.contains("high school")) return vulnScores.getEducationHighschoolScore();
        if (lower.contains("vocational")) return vulnScores.getEducationVocationalScore();
        if (lower.contains("college")) return vulnScores.getEducationCollegeScore();
        if (lower.contains("graduate")) return vulnScores.getEducationGraduatedScore();
        return 0.0;
    }

    private double getDigitalAccessScore(String access, VulnerabilityIndicatorScoreModel vulnScores) {
        if (access == null) return 0.0;
        String lower = access.trim().toLowerCase();
        if (lower.contains("reliable")) return vulnScores.getDigitalReliableScore();
        if (lower.contains("intermittent")) return vulnScores.getDigitalIntermittentScore();
        if (lower.contains("limited")) return vulnScores.getDigitalLimitedAccessScore();
        if (lower.contains("no digital")) return vulnScores.getDigitalNoDigitalScore();
        return 0.0;
    }

    private double getDisasterDamageScore(String access, VulnerabilityIndicatorScoreModel vulnScores) {
        if (access == null) return 0.0;
        String lower = access.trim().toLowerCase();
        if (lower.contains("no visible")) return vulnScores.getNoVisibleDamageScore();
        if (lower.contains("minor damage")) return vulnScores.getMinorDamageScore();
        if (lower.contains("moderate damage")) return vulnScores.getModerateDamageScore();
        if (lower.contains("severe damage")) return vulnScores.getSevereDamageScore();
        if (lower.contains("destruction")) return vulnScores.getDestructionOrCollapseScore();
        return 0.0;
    }

    private void debug(String label, Object value) {
        if (DEBUG) {
            System.out.println("[DEBUG] " + label + " => [" + value + "]");
        }
    }
}