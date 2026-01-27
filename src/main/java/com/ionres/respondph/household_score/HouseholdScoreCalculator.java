package com.ionres.respondph.household_score;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.disaster_damage.DisasterDamageModel;
import com.ionres.respondph.familymembers.FamilyMembersModel;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.util.ResourceUtils;
import com.ionres.respondph.vulnerability_indicator.VulnerabilityIndicatorScoreModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HouseholdScoreCalculator {
    private static final Logger LOGGER = Logger.getLogger(HouseholdScoreCalculator.class.getName());
    private static final boolean DEBUG = false;
    private static final Cryptography CRYPTO = com.ionres.respondph.util.CryptographyManager.getInstance();

    public boolean calculateAndSaveHouseholdScore(int beneficiaryId) {
        try {
            VulnerabilityIndicatorScoreModel vulnScores = getVulnerabilityScores();
            if (vulnScores == null) {
                LOGGER.warning("No vulnerability scores found in database");
                return false;
            }

            BeneficiaryModel beneficiary = getBeneficiaryById(beneficiaryId);
            if (beneficiary == null) {
                LOGGER.warning("Beneficiary not found: " + beneficiaryId);
                return false;
            }
            debug("Gender", beneficiary.getGender());
            debug("Marital Status", beneficiary.getMaritalStatus());

            List<FamilyMembersModel> familyMembers = getFamilyMembersByBeneficiaryId(beneficiaryId);
            debug("Family members count", familyMembers.size());


            HouseholdScoreModel householdScore = calculateScores(beneficiary, familyMembers, vulnScores);
            householdScore.setBeneficiaryId(beneficiaryId);

            return saveHouseholdScore(householdScore);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating household score", e);
            return false;
        }
    }


    public boolean autoRecalculateHouseholdScore(int beneficiaryId) {
        try {
            VulnerabilityIndicatorScoreModel vulnScores = getVulnerabilityScores();
            if (vulnScores == null) {
                LOGGER.warning("Auto-recalculation failed: No vulnerability scores found");
                return false;
            }

            BeneficiaryModel beneficiary = getBeneficiaryById(beneficiaryId);
            if (beneficiary == null) {
                LOGGER.warning("Auto-recalculation failed: Beneficiary not found - " + beneficiaryId);
                return false;
            }

            List<FamilyMembersModel> familyMembers = getFamilyMembersByBeneficiaryId(beneficiaryId);

            HouseholdScoreModel householdScore = calculateScores(beneficiary, familyMembers, vulnScores);
            householdScore.setBeneficiaryId(beneficiaryId);

            return saveHouseholdScore(householdScore);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Auto-recalculation error", e);
            return false;
        }
    }


    public int recalculateAllHouseholdScores() {
        int recalculatedCount = 0;
        List<Integer> beneficiaryIds = getAllBeneficiaryIdsWithHouseholdScores();

        LOGGER.info("========== RECALCULATING ALL HOUSEHOLD SCORES ==========");
        LOGGER.info("Found " + beneficiaryIds.size() + " households to recalculate");

        for (Integer beneficiaryId : beneficiaryIds) {
            try {
                boolean success = autoRecalculateHouseholdScore(beneficiaryId);
                if (success) {
                    recalculatedCount++;
                    LOGGER.info("✓ Recalculated household score for beneficiary ID: " + beneficiaryId);
                } else {
                    LOGGER.warning("✗ Failed to recalculate household score for beneficiary ID: " + beneficiaryId);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "✗ Error recalculating household score for beneficiary ID " + beneficiaryId, e);
            }
        }

        LOGGER.info("========== RECALCULATION COMPLETE ==========");
        LOGGER.info("Successfully recalculated: " + recalculatedCount + " out of " + beneficiaryIds.size());

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
        int dependents = 0;
        int workingAgeAdults = 0;

        try {
            String beneDisability = null;
            String beneEmployment = null;

            try {
                beneDisability = CRYPTO.decryptWithOneParameter(beneficiary.getDisabilityType());
                beneEmployment = CRYPTO.decryptWithOneParameter(beneficiary.getEmploymentStatus());
            } catch (Exception e) {
                debug("Error decrypting beneficiary data", e.getMessage());
                beneDisability = "None";
                beneEmployment = "Unknown";
            }

            boolean beneIsDependent = isDependentPerson(
                    beneficiary.getAgeScore(),
                    beneDisability,
                    beneEmployment
            );

            if (beneIsDependent) {
                dependents++;
                debug("Beneficiary classified as", "DEPENDENT");
            } else {
                workingAgeAdults++;
                debug("Beneficiary classified as", "WORKING-AGE ADULT");
            }

            int memberIndex = 1;
            for (FamilyMembersModel fm : familyMembers) {
                String fmDisability = null;
                String fmEmployment = null;

                try {
                    fmDisability = CRYPTO.decryptWithOneParameter(fm.getDisabilityType());
                    fmEmployment = CRYPTO.decryptWithOneParameter(fm.getEmploymentStatus());
                } catch (Exception e) {
                    debug("Error decrypting family member #" + memberIndex + " data", e.getMessage());
                    fmDisability = "None";
                    fmEmployment = "Unknown";
                }

                boolean isDependent = isDependentPerson(fm.getAgeScore(), fmDisability, fmEmployment);

                if (isDependent) {
                    dependents++;
                    debug("Family Member #" + memberIndex + " classified as", "DEPENDENT");
                } else {
                    workingAgeAdults++;
                    debug("Family Member #" + memberIndex + " classified as", "WORKING-AGE ADULT");
                }
                memberIndex++;
            }

            debug("Total Dependents", dependents);
            debug("Total Working-Age Adults", workingAgeAdults);

            double dependencyRatio;
            if (workingAgeAdults == 0) {
                dependencyRatio = 1.0;
                debug("Dependency Ratio", "ALL DEPENDENTS (1.0)");
            } else {
                double rawRatio = (double) dependents / workingAgeAdults;
                debug("Raw Dependency Ratio", rawRatio + " (" + dependents + "/" + workingAgeAdults + ")");

                dependencyRatio = Math.min(rawRatio / 2.0, 1.0);
            }

            debug("Normalized Dependency Ratio Score", dependencyRatio);
            return Math.round(dependencyRatio * 100.0) / 100.0;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating dependency ratio score", e);
            return 0.5;
        }
    }

    private boolean isDependentPerson(double ageScore, String disabilityType, String employmentStatus) {
        if (ageScore == 0.3) {
            return true;
        }

        if (ageScore >= 0.6) {
            return true;
        }

        if (disabilityType != null && !disabilityType.trim().equalsIgnoreCase("none")) {
            return true;
        }

        if (employmentStatus != null && employmentStatus.trim().toLowerCase().contains("unemployed")) {
            return true;
        }

        return false;
    }


    private List<Integer> getAllBeneficiaryIdsWithHouseholdScores() {
        List<Integer> beneficiaryIds = new ArrayList<>();
        String sql = "SELECT DISTINCT beneficiary_id FROM household_score";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                beneficiaryIds.add(rs.getInt("beneficiary_id"));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching beneficiary IDs with household scores", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }

        return beneficiaryIds;
    }

    private void debug(String label, Object value) {
        if (DEBUG) {
            LOGGER.info("[DEBUG] " + label + " => [" + value + "]");
        }
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

            String beneGender = CRYPTO.decryptWithOneParameter(beneficiary.getGender());
            String beneMaritalStatus = CRYPTO.decryptWithOneParameter(beneficiary.getMaritalStatus());
            String beneSoloParentStatus = CRYPTO.decryptWithOneParameter(beneficiary.getSoloParentStatus());
            String beneDisabilityType = CRYPTO.decryptWithOneParameter(beneficiary.getDisabilityType());
            String beneHealthCondition = CRYPTO.decryptWithOneParameter(beneficiary.getHealthCondition());
            String beneCleanWaterAccess = CRYPTO.decryptWithOneParameter(beneficiary.getCleanWaterAccess());
            String beneSanitationFacility = CRYPTO.decryptWithOneParameter(beneficiary.getSanitationFacility());
            String beneHouseType = CRYPTO.decryptWithOneParameter(beneficiary.getHouseType());
            String beneOwnershipStatus = CRYPTO.decryptWithOneParameter(beneficiary.getOwnerShipStatus());
            String beneEmploymentStatus = CRYPTO.decryptWithOneParameter(beneficiary.getEmploymentStatus());
            String beneMonthlyIncome = CRYPTO.decryptWithOneParameter(beneficiary.getMonthlyIncome());
            String beneEducationLevel = CRYPTO.decryptWithOneParameter(beneficiary.getEducationalLevel());
            String beneDigitalAccess = CRYPTO.decryptWithOneParameter(beneficiary.getDigitalAccess());

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
            List<DisasterDamageModel> disasterDamages = getDisasterDamageById(beneficiary.getId());
            if (!disasterDamages.isEmpty()) {
                DisasterDamageModel latestDamage = disasterDamages.get(disasterDamages.size() - 1);
                String houseDamageEncrypted = latestDamage.getHouseDamageSeverity();
                if (houseDamageEncrypted != null && !houseDamageEncrypted.isEmpty()) {
                    String houseDamageDecrypted = CRYPTO.decryptWithOneParameter(houseDamageEncrypted);
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
                String fmGender = CRYPTO.decryptWithOneParameter(fm.getGender());
                String fmMaritalStatus = CRYPTO.decryptWithOneParameter(fm.getMaritalStatus());
                String fmDisabilityType = CRYPTO.decryptWithOneParameter(fm.getDisabilityType());
                String fmHealthCondition = CRYPTO.decryptWithOneParameter(fm.getHealthCondition());
                String fmEmploymentStatus = CRYPTO.decryptWithOneParameter(fm.getEmploymentStatus());
                String fmEducationLevel = CRYPTO.decryptWithOneParameter(fm.getEducationalLevel());

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

            // ========== CALCULATE DEPENDENCY RATIO SCORE ==========
            double dependencyRatioScore = calculateDependencyRatioScore(beneficiary, familyMembers);
            result.setDependencyRatioScore(dependencyRatioScore);
            debug("Final Dependency Ratio Score", dependencyRatioScore);

            debug("========== CALCULATION COMPLETE ==========", "");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "========== ERROR IN CALCULATION ==========", e);
        }

        return result;
    }



    private double getGenderScore(String gender, VulnerabilityIndicatorScoreModel vulnScores) {
        if (gender == null) return 0.0;
        switch (gender.trim().toLowerCase()) {
            case "male": return vulnScores.getMaleScore();
            case "female": return vulnScores.getFemaleScore();
            case "other": return vulnScores.getOtherScore();
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
            case "divorced": return vulnScores.getDivorceScore();
            default: return 0.0;
        }
    }

    private double getSoloParentScore(String status, VulnerabilityIndicatorScoreModel vulnScores) {
        if (status == null) return 0.0;
        return status.equalsIgnoreCase("yes") ?
                vulnScores.getSoloParentYesScore() : vulnScores.getSoloParentNoScore();
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
        if (lower.contains("history")) return vulnScores.getHealthWithHistoryScore();
        return vulnScores.getHealthHealthyScore();
    }

    private double getCleanWaterScore(String access, VulnerabilityIndicatorScoreModel vulnScores) {
        if (access == null) return 0.0;
        switch (access.trim().toLowerCase()) {
            case "yes": return vulnScores.getCleanWaterYesScore();
            case "no": return vulnScores.getCleanWaterNoScore();
            case "occasionally": return vulnScores.getCleanWaterOccasionallyScore();
            default: return 0.0;
        }
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
        if (lower.contains("concrete") || lower.contains("masonry"))
            return vulnScores.getHouseConcreteScore();
        if (lower.contains("light materials"))
            return vulnScores.getHouseLightMaterialsScore();
        if (lower.contains("semi-concrete"))
            return vulnScores.getHouseSemiConcreteScore();
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
        if (lower.contains("regular")) return vulnScores.getEmploymentRegularScore();
        if (lower.contains("self-employed with stable"))
            return vulnScores.getEmploymentSelfEmployedStableScore();
        if (lower.contains("self-employed with unstable"))
            return vulnScores.getEmploymentSelfEmployedUnstableScore();
        if (lower.contains("irregular")) return vulnScores.getEmploymentIrregularScore();
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
        if (lower.contains("device only")) return vulnScores.getDigitalDeviceOnlyScore();
        if (lower.contains("no digital")) return vulnScores.getDigitalNoDigitalScore();
        return 0.0;
    }

    private double getDisasterDamageScore(String access, VulnerabilityIndicatorScoreModel vulnScores){
        if(access == null ) return 0.0;

        String lower = access.trim().toLowerCase();
        if(lower.contains("no visible")) return vulnScores.getNoVisibleDamageScore();
        if(lower.contains("minor damage")) return vulnScores.getMinorDamageScore();
        if(lower.contains("moderate damage")) return  vulnScores.getModerateDamageScore();
        if(lower.contains("severe damage")) return  vulnScores.getSevereDamageScore();
        if(lower.contains("destruction")) return vulnScores.getDestructionOrCollapseScore();

        return 0.0;
    }

    private VulnerabilityIndicatorScoreModel getVulnerabilityScores() {
        String sql = "SELECT * FROM vulnerability_indicator_score LIMIT 1";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            if (rs.next()) {
                return mapVulnerabilityScores(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching vulnerability scores", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }
        return null;
    }

    private BeneficiaryModel getBeneficiaryById(int beneficiaryId) {
        String sql = "SELECT * FROM beneficiary WHERE beneficiary_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            rs = ps.executeQuery();

            if (rs.next()) {
                return mapBeneficiary(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching beneficiary by ID", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }
        return null;
    }

    private List<FamilyMembersModel> getFamilyMembersByBeneficiaryId(int beneficiaryId) {
        List<FamilyMembersModel> members = new ArrayList<>();
        String sql = "SELECT * FROM family_member WHERE beneficiary_id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            rs = ps.executeQuery();

            while (rs.next()) {
                members.add(mapFamilyMember(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching family members", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }
        return members;
    }

    private List<DisasterDamageModel> getDisasterDamageById(int beneficiaryId) {
        List<DisasterDamageModel> members = new ArrayList<>();
        String sql = "SELECT * FROM beneficiary_disaster_damage WHERE beneficiary_id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            rs = ps.executeQuery();

            while (rs.next()) {
                members.add(mapDisasterDamage(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching disaster damage", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }
        return members;
    }


    private boolean saveHouseholdScore(HouseholdScoreModel score) {
        String checkSql = "SELECT household_score_id FROM household_score WHERE beneficiary_id = ?";
        String insertSql = "INSERT INTO household_score (beneficiary_id, age_score, gender_score, marital_status_score, " +
                "solo_parent_score, disability_score, health_condition_score, access_to_clean_water_score, " +
                "sanitation_facilities_score, house_construction_type_score, ownership_score, " +
                "damage_severity_score, employment_status_score, monthly_income_score, education_level_score, " +
                "digital_access_score, dependency_ratio_score, creation_date) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())";

        String updateSql = "UPDATE household_score SET " +
                "age_score=?, gender_score=?, marital_status_score=?, solo_parent_score=?, disability_score=?, " +
                "health_condition_score=?, access_to_clean_water_score=?, sanitation_facilities_score=?, " +
                "house_construction_type_score=?, ownership_score=?, damage_severity_score=?, " +
                "employment_status_score=?, monthly_income_score=?, education_level_score=?, " +
                "digital_access_score=?, dependency_ratio_score=?, updating_date=NOW() WHERE beneficiary_id=?";

        Connection conn = null;
        PreparedStatement checkPs = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getInstance().getConnection();

            checkPs = conn.prepareStatement(checkSql);
            checkPs.setInt(1, score.getBeneficiaryId());
            rs = checkPs.executeQuery();
            boolean recordExists = rs.next();
            ResourceUtils.closeResources(rs, checkPs);
            rs = null;
            checkPs = null;

            if (recordExists) {
                debug("Household score exists", "Updating existing record");
                ps = conn.prepareStatement(updateSql);

                ps.setDouble(1, score.getAgeScore());
                ps.setDouble(2, score.getGenderScore());
                ps.setDouble(3, score.getMaritalStatusScore());
                ps.setDouble(4, score.getSoloParentScore());
                ps.setDouble(5, score.getDisabilityScore());
                ps.setDouble(6, score.getHealthConditionScore());
                ps.setDouble(7, score.getAccessToCleanWaterScore());
                ps.setDouble(8, score.getSanitationFacilitiesScore());
                ps.setDouble(9, score.getHouseConstructionTypeScore());
                ps.setDouble(10, score.getOwnershipScore());
                ps.setDouble(11, score.getDamageSeverityScore());
                ps.setDouble(12, score.getEmploymentStatusScore());
                ps.setDouble(13, score.getMonthlyIncomeScore());
                ps.setDouble(14, score.getEducationLevelScore());
                ps.setDouble(15, score.getDigitalAccessScore());
                ps.setDouble(16, score.getDependencyRatioScore());
                ps.setInt(17, score.getBeneficiaryId());

            } else {
                debug("Household score doesn't exist", "Inserting new record");
                ps = conn.prepareStatement(insertSql);

                ps.setInt(1, score.getBeneficiaryId());
                ps.setDouble(2, score.getAgeScore());
                ps.setDouble(3, score.getGenderScore());
                ps.setDouble(4, score.getMaritalStatusScore());
                ps.setDouble(5, score.getSoloParentScore());
                ps.setDouble(6, score.getDisabilityScore());
                ps.setDouble(7, score.getHealthConditionScore());
                ps.setDouble(8, score.getAccessToCleanWaterScore());
                ps.setDouble(9, score.getSanitationFacilitiesScore());
                ps.setDouble(10, score.getHouseConstructionTypeScore());
                ps.setDouble(11, score.getOwnershipScore());
                ps.setDouble(12, score.getDamageSeverityScore());
                ps.setDouble(13, score.getEmploymentStatusScore());
                ps.setDouble(14, score.getMonthlyIncomeScore());
                ps.setDouble(15, score.getEducationLevelScore());
                ps.setDouble(16, score.getDigitalAccessScore());
                ps.setDouble(17, score.getDependencyRatioScore());
            }

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving household score", e);
            return false;
        } finally {
            ResourceUtils.closeResources(rs, checkPs);
            ResourceUtils.closePreparedStatement(ps);
        }
    }

    private VulnerabilityIndicatorScoreModel mapVulnerabilityScores(ResultSet rs) throws SQLException {
        VulnerabilityIndicatorScoreModel model = new VulnerabilityIndicatorScoreModel();
        model.setMaleScore(rs.getDouble("male_score"));
        model.setFemaleScore(rs.getDouble("female_score"));
        model.setOtherScore(rs.getDouble("other_score"));
        model.setSingleScore(rs.getDouble("single_score"));
        model.setMarriedScore(rs.getDouble("married_score"));
        model.setWidowedScore(rs.getDouble("widowed_score"));
        model.setSeparatedScore(rs.getDouble("separated_score"));
        model.setDivorceScore(rs.getDouble("divorce_score"));
        model.setSoloParentYesScore(rs.getDouble("solo_parent_status_yes_score"));
        model.setSoloParentNoScore(rs.getDouble("solo_parent_status_no_score"));
        model.setDisabilityNoneScore(rs.getDouble("disability_none_score"));
        model.setDisabilityPhysicalScore(rs.getDouble("disability_physical_score"));
        model.setDisabilityVisualScore(rs.getDouble("disability_visual_score"));
        model.setDisabilityHearingScore(rs.getDouble("disability_hearing_score"));
        model.setDisabilitySpeechScore(rs.getDouble("disability_speech_score"));
        model.setDisabilityIntellectualScore(rs.getDouble("disability_intellectual_score"));
        model.setDisabilityMentalScore(rs.getDouble("disability_mental_or_psychosocial_score"));
        model.setDisabilityChronicScore(rs.getDouble("disability_chronic_score"));
        model.setHealthHealthyScore(rs.getDouble("health_healthy_score"));
        model.setHealthTemporarilyIllScore(rs.getDouble("health_temporarily_ill_score"));
        model.setHealthChronicallyIllScore(rs.getDouble("health_chronically_ill_score"));
        model.setHealthImmunocompromisedScore(rs.getDouble("health_immunocompromisedl_score"));
        model.setHealthTerminallyIllScore(rs.getDouble("health_terminally_ill_score"));
        model.setHealthWithHistoryScore(rs.getDouble("health_with_history_score"));
        model.setCleanWaterYesScore(rs.getDouble("clean_water_access_yes_score"));
        model.setCleanWaterNoScore(rs.getDouble("clean_water_access_no_score"));
        model.setCleanWaterOccasionallyScore(rs.getDouble("clean_water_access_occasionally_score"));
        model.setSanitationSafelyScore(rs.getDouble("sanitation_safely_score"));
        model.setSanitationSharedScore(rs.getDouble("sanitation_shared_score"));
        model.setSanitationUnimprovedScore(rs.getDouble("sanitation_unimproved_score"));
        model.setSanitationNoScore(rs.getDouble("sanitation_no_score"));
        model.setHouseConcreteScore(rs.getDouble("house_construction_concrete_score"));
        model.setHouseLightMaterialsScore(rs.getDouble("house_construction_light_materials_score"));
        model.setHouseSemiConcreteScore(rs.getDouble("house_construction_semi_concrete_score"));
        model.setHouseMakeshiftScore(rs.getDouble("house_construction_makeshirt_score"));
        model.setOwnershipOwnedScore(rs.getDouble("ownership_owned_score"));
        model.setOwnershipOwnedWithoutScore(rs.getDouble("ownership_owned_without_score"));
        model.setOwnershipRentedScore(rs.getDouble("ownership_rented_score"));
        model.setOwnershipInformalScore(rs.getDouble("ownership_informal_score"));
        model.setOwnershipEvictedScore(rs.getDouble("ownership_evicted_score"));
        model.setEmploymentRegularScore(rs.getDouble("employment_regular_score"));
        model.setEmploymentSelfEmployedStableScore(rs.getDouble("employment_self_employed_stable_score"));
        model.setEmploymentSelfEmployedUnstableScore(rs.getDouble("employment_self_employed_unstable_score"));
        model.setEmploymentIrregularScore(rs.getDouble("employment_irregular_score"));
        model.setEmploymentUnemployedScore(rs.getDouble("employment_unemployed_score"));
        model.setIncomePoorScore(rs.getDouble("income_poor_score"));
        model.setIncomeLowIncomeScore(rs.getDouble("income_low_income_score"));
        model.setIncomeMiddleIncomeScore(rs.getDouble("income_middle_income_score"));
        model.setIncomeMiddleClassScore(rs.getDouble("income_middle_class_score"));
        model.setIncomeUpperMiddleClassScore(rs.getDouble("income_upper_middle_class_score"));
        model.setIncomeRichScore(rs.getDouble("income_rich_score"));
        model.setEducationNoFormalScore(rs.getDouble("education_no_formal_education_score"));
        model.setEducationElementaryScore(rs.getDouble("education_elementary_score"));
        model.setEducationHighschoolScore(rs.getDouble("education_highschool_score"));
        model.setEducationVocationalScore(rs.getDouble("education_vocational_score"));
        model.setEducationCollegeScore(rs.getDouble("education_college_score"));
        model.setEducationGraduatedScore(rs.getDouble("education_graduated_score"));
        model.setDigitalReliableScore(rs.getDouble("digital_reliable_score"));
        model.setDigitalIntermittentScore(rs.getDouble("digital_intermittent_score"));
        model.setDigitalDeviceOnlyScore(rs.getDouble("digital_device_only_score"));
        model.setDigitalNoDigitalScore(rs.getDouble("digital_no_digital_score"));
        model.setNoVisibleDamageScore(rs.getDouble("damage_no_visible_damage"));
        model.setMinorDamageScore(rs.getDouble("damage_minor_damage"));
        model.setModerateDamageScore(rs.getDouble("damage_moderate_damage"));
        model.setSevereDamageScore(rs.getDouble("damage_severe_damage"));
        model.setDestructionOrCollapseScore(rs.getDouble("damage_destruction_or_collapse"));
        return model;
    }

    private BeneficiaryModel mapBeneficiary(ResultSet rs) throws SQLException {
        BeneficiaryModel model = new BeneficiaryModel();
        model.setId(rs.getInt("beneficiary_id"));
        model.setAgeScore(rs.getDouble("age_score"));  // ADD THIS LINE
        model.setGender(rs.getString("gender"));
        model.setMaritalStatus(rs.getString("marital_status"));
        model.setSoloParentStatus(rs.getString("solo_parent_status"));
        model.setDisabilityType(rs.getString("disability_type"));
        model.setHealthCondition(rs.getString("health_condition"));
        model.setCleanWaterAccess(rs.getString("clean_water_access"));
        model.setSanitationFacility(rs.getString("sanitation_facility"));
        model.setHouseType(rs.getString("house_type"));
        model.setOwnerShipStatus(rs.getString("ownership_status"));
        model.setEmploymentStatus(rs.getString("employment_status"));
        model.setMonthlyIncome(rs.getString("monthly_income"));
        model.setEducationalLevel(rs.getString("education_level"));
        model.setDigitalAccess(rs.getString("digital_access"));
        return model;
    }

    private FamilyMembersModel mapFamilyMember(ResultSet rs) throws SQLException {
        FamilyMembersModel model = new FamilyMembersModel();
        model.setAgeScore(rs.getDouble("age_score"));  // ADD THIS LINE
        model.setGender(rs.getString("gender"));
        model.setMaritalStatus(rs.getString("marital_status"));
        model.setDisabilityType(rs.getString("disability_type"));
        model.setHealthCondition(rs.getString("health_condition"));
        model.setEmploymentStatus(rs.getString("employment_status"));
        model.setEducationalLevel(rs.getString("education_level"));
        return model;
    }

    private DisasterDamageModel mapDisasterDamage(ResultSet rs) throws SQLException {
        DisasterDamageModel model = new DisasterDamageModel();
        model.setHouseDamageSeverity(rs.getString("house_damage_severity"));
        return model;
    }
}