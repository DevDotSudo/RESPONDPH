package com.ionres.respondph.household_score;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.disaster_damage.DisasterDamageModel;
import com.ionres.respondph.familymembers.FamilyMembersModel;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.vulnerability_indicator.VulnerabilityIndicatorScoreModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HouseholdScoreCalculator {
    private Connection conn;
    private static final boolean DEBUG = true;
    Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

    public boolean calculateAndSaveHouseholdScore(int beneficiaryId) {
        try {
            VulnerabilityIndicatorScoreModel vulnScores = getVulnerabilityScores();
            if (vulnScores == null) {
                System.err.println("No vulnerability scores found in database");
                return false;
            }

            BeneficiaryModel beneficiary = getBeneficiaryById(beneficiaryId);
            if (beneficiary == null) {
                System.err.println("Beneficiary not found: " + beneficiaryId);
                return false;
            }
            debug("Gender", beneficiary.getGender());
            debug("Marital Status", beneficiary.getMaritalStatus());

            String gender = cs.decryptWithOneParameter(beneficiary.getGender());
            System.out.println(gender);

            List<FamilyMembersModel> familyMembers = getFamilyMembersByBeneficiaryId(beneficiaryId);
            debug("Family members count", familyMembers.size());


            HouseholdScoreModel householdScore = calculateScores(beneficiary, familyMembers, vulnScores);
            householdScore.setBeneficiaryId(beneficiaryId);

            return saveHouseholdScore(householdScore);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private void debug(String label, Object value) {
        if (DEBUG) {
            System.out.println("[DEBUG] " + label + " => [" + value + "]");
        }
    }

//    // Replace the calculateScores method in HouseholdScoreCalculator.java
//
//    private HouseholdScoreModel calculateScores(
//            BeneficiaryModel beneficiary,
//            List<FamilyMembersModel> familyMembers,
//            VulnerabilityIndicatorScoreModel vulnScores) {
//
//        HouseholdScoreModel result = new HouseholdScoreModel();
//
//        try {
//            int totalMembers = 1 + familyMembers.size();
//            debug("Total household members", totalMembers);
//
//            // Decrypt and get beneficiary scores
//            double beneGenderScore = getGenderScore(cs.decryptWithOneParameter(beneficiary.getGender()), vulnScores);
//            double beneMaritalScore = getMaritalStatusScore(cs.decryptWithOneParameter(beneficiary.getMaritalStatus()), vulnScores);
//            double beneSoloParentScore = getSoloParentScore(cs.decryptWithOneParameter(beneficiary.getSoloParentStatus()), vulnScores);
//            double beneDisabilityScore = getDisabilityScore(cs.decryptWithOneParameter(beneficiary.getDisabilityType()), vulnScores);
//            double beneHealthScore = getHealthScore(cs.decryptWithOneParameter(beneficiary.getHealthCondition()), vulnScores);
//            double beneCleanWaterScore = getCleanWaterScore(cs.decryptWithOneParameter(beneficiary.getCleanWaterAccess()), vulnScores);
//            double beneSanitationScore = getSanitationScore(cs.decryptWithOneParameter(beneficiary.getSanitationFacility()), vulnScores);
//            double beneHouseTypeScore = getHouseTypeScore(cs.decryptWithOneParameter(beneficiary.getHouseType()), vulnScores);
//            double beneOwnershipScore = getOwnershipScore(cs.decryptWithOneParameter(beneficiary.getOwnerShipStatus()), vulnScores);
//            double beneEmploymentScore = getEmploymentScore(cs.decryptWithOneParameter(beneficiary.getEmploymentStatus()), vulnScores);
//            double beneIncomeScore = getIncomeScore(cs.decryptWithOneParameter(beneficiary.getMonthlyIncome()), vulnScores);
//            double beneEducationScore = getEducationScore(cs.decryptWithOneParameter(beneficiary.getEducationalLevel()), vulnScores);
//            double beneDigitalScore = getDigitalAccessScore(cs.decryptWithOneParameter(beneficiary.getDigitalAccess()), vulnScores);
//
//            // Get disaster damage score (fetch actual data from database)
//            double beneDisasterDamageScore = 0.0;
//            List<DisasterDamageModel> disasterDamages = getDisasterDamageById(beneficiary.getId());
//            if (!disasterDamages.isEmpty()) {
//                // Get the most recent disaster damage record
//                DisasterDamageModel latestDamage = disasterDamages.get(disasterDamages.size() - 1);
//                String houseDamage = latestDamage.getHouseDamageSeverity();
//                if (houseDamage != null && !houseDamage.isEmpty()) {
//                    beneDisasterDamageScore = getDisasterDamageScore(
//                            cs.decryptWithOneParameter(houseDamage),
//                            vulnScores
//                    );
//                }
//            }
//
//            debug("Beneficiary disability score", beneDisabilityScore);
//            debug("Disaster Damage Score", beneDisasterDamageScore);
//
//            // Initialize totals with beneficiary scores
//            double totalGenderScore = beneGenderScore;
//            double totalMaritalScore = beneMaritalScore;
//            double totalSoloParentScore = beneSoloParentScore;
//            double totalDisabilityScore = beneDisabilityScore;
//            double totalHealthScore = beneHealthScore;
//            double totalEmploymentScore = beneEmploymentScore;
//            double totalEducationScore = beneEducationScore;
//
//            // Add each family member's score
//            for (FamilyMembersModel fm : familyMembers) {
//                double fmGenderScore = getGenderScore(cs.decryptWithOneParameter(fm.getGender()), vulnScores);
//                double fmMaritalScore = getMaritalStatusScore(cs.decryptWithOneParameter(fm.getMaritalStatus()), vulnScores);
//                double fmDisabilityScore = getDisabilityScore(cs.decryptWithOneParameter(fm.getDisabilityType()), vulnScores);
//                double fmHealthScore = getHealthScore(cs.decryptWithOneParameter(fm.getHealthCondition()), vulnScores);
//                double fmEmploymentScore = getEmploymentScore(cs.decryptWithOneParameter(fm.getEmploymentStatus()), vulnScores);
//                double fmEducationScore = getEducationScore(cs.decryptWithOneParameter(fm.getEducationalLevel()), vulnScores);
//
//                totalGenderScore += fmGenderScore;
//                totalMaritalScore += fmMaritalScore;
//                totalDisabilityScore += fmDisabilityScore;
//                totalHealthScore += fmHealthScore;
//                totalEmploymentScore += fmEmploymentScore;
//                totalEducationScore += fmEducationScore;
//
//                debug("Family member disability score", fmDisabilityScore);
//            }
//
//            debug("Total disability score (sum)", totalDisabilityScore);
//            debug("Average disability score", totalDisabilityScore / totalMembers);
//
//            // Calculate averages: (beneficiary + family_members) / total_members
//            result.setGenderScore(Math.round((totalGenderScore / totalMembers) * 100.0) / 100.0);
//            result.setMaritalStatusScore(Math.round((totalMaritalScore / totalMembers) * 100.0) / 100.0);
//            result.setSoloParentScore(Math.round((totalSoloParentScore / totalMembers) * 100.0) / 100.0);
//            result.setDisabilityScore(Math.round((totalDisabilityScore / totalMembers) * 100.0) / 100.0);
//            result.setHealthConditionScore(Math.round((totalHealthScore / totalMembers) * 100.0) / 100.0);
//            result.setEmploymentStatusScore(Math.round((totalEmploymentScore / totalMembers) * 100.0) / 100.0);
//            result.setEducationLevelScore(Math.round((totalEducationScore / totalMembers) * 100.0) / 100.0);
//
//            // Household-level attributes (from beneficiary only)
//            result.setAccessToCleanWaterScore(Math.round(beneCleanWaterScore * 100.0) / 100.0);
//            result.setSanitationFacilitiesScore(Math.round(beneSanitationScore * 100.0) / 100.0);
//            result.setHouseConstructionTypeScore(Math.round(beneHouseTypeScore * 100.0) / 100.0);
//            result.setOwnershipScore(Math.round(beneOwnershipScore * 100.0) / 100.0);
//            result.setMonthlyIncomeScore(Math.round(beneIncomeScore * 100.0) / 100.0);
//            result.setDigitalAccessScore(Math.round(beneDigitalScore * 100.0) / 100.0);
//
//            // Set disaster damage score (if you have this field in HouseholdScoreModel)
//            // Otherwise, you might want to add it to the model
//            result.setDamageSeverityScore(Math.round(beneDisasterDamageScore * 100.0) / 100.0);
//
//            debug("Final disability score", result.getDisabilityScore());
//
//        } catch (Exception e) {
//            System.err.println("Error calculating scores: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        return result;
//    }
//
//    // Also fix the mapDisasterDamage method:
//    private DisasterDamageModel mapDisasterDamage(ResultSet rs) throws SQLException {
//        DisasterDamageModel model = new DisasterDamageModel();
//        // Fixed: Get the actual value from the ResultSet, not the column name
//        model.setHouseDamageSeverity(rs.getString("house_damage_severity"));
//        return model;
//    }


    private HouseholdScoreModel calculateScores(
            BeneficiaryModel beneficiary,
            List<FamilyMembersModel> familyMembers,
            VulnerabilityIndicatorScoreModel vulnScores) {

        HouseholdScoreModel result = new HouseholdScoreModel();
        DisasterDamageModel ddm = new DisasterDamageModel();

        try {

            int totalMembers = 1 + familyMembers.size();
            debug("Total household members", totalMembers);

            double beneGenderScore = getGenderScore(cs.decryptWithOneParameter(beneficiary.getGender()), vulnScores);
            double beneMaritalScore = getMaritalStatusScore(cs.decryptWithOneParameter(beneficiary.getMaritalStatus()), vulnScores);
            double beneSoloParentScore = getSoloParentScore(cs.decryptWithOneParameter(beneficiary.getSoloParentStatus()), vulnScores);
            double beneDisabilityScore = getDisabilityScore(cs.decryptWithOneParameter(beneficiary.getDisabilityType()), vulnScores);
            double beneHealthScore = getHealthScore(cs.decryptWithOneParameter(beneficiary.getHealthCondition()), vulnScores);
            double beneCleanWaterScore = getCleanWaterScore(cs.decryptWithOneParameter(beneficiary.getCleanWaterAccess()), vulnScores);
            double beneSanitationScore = getSanitationScore(cs.decryptWithOneParameter(beneficiary.getSanitationFacility()), vulnScores);
            double beneHouseTypeScore = getHouseTypeScore(cs.decryptWithOneParameter(beneficiary.getHouseType()), vulnScores);
            double beneOwnershipScore = getOwnershipScore(cs.decryptWithOneParameter(beneficiary.getOwnerShipStatus()), vulnScores);
            double beneEmploymentScore = getEmploymentScore(cs.decryptWithOneParameter(beneficiary.getEmploymentStatus()), vulnScores);
            double beneIncomeScore = getIncomeScore(cs.decryptWithOneParameter(beneficiary.getMonthlyIncome()), vulnScores);
            double beneEducationScore = getEducationScore(cs.decryptWithOneParameter(beneficiary.getEducationalLevel()), vulnScores);
            double beneDigitalScore = getDigitalAccessScore(cs.decryptWithOneParameter(beneficiary.getDigitalAccess()), vulnScores);
//            double beneDisasterDamageScore = getDisasterDamageScore(cs.decryptWithOneParameter(ddm.getHouseDamageSeverity()), vulnScores);

            // Get disaster damage score (fetch actual data from database)
            double beneDisasterDamageScore = 0.0;
            List<DisasterDamageModel> disasterDamages = getDisasterDamageById(beneficiary.getId());
            if (!disasterDamages.isEmpty()) {
                // Get the most recent disaster damage record
                DisasterDamageModel latestDamage = disasterDamages.get(disasterDamages.size() - 1);
                String houseDamage = latestDamage.getHouseDamageSeverity();
                if (houseDamage != null && !houseDamage.isEmpty()) {
                    beneDisasterDamageScore = getDisasterDamageScore(
                            cs.decryptWithOneParameter(houseDamage),
                            vulnScores
                    );
                }
            }

            debug("Beneficiary disability score", beneDisabilityScore);
            debug("Disaster Damage Score", beneDisasterDamageScore);

            double totalGenderScore = beneGenderScore;
            double totalMaritalScore = beneMaritalScore;
            double totalSoloParentScore = beneSoloParentScore;
            double totalDisabilityScore = beneDisabilityScore;
            double totalHealthScore = beneHealthScore;
            double totalEmploymentScore = beneEmploymentScore;
            double totalEducationScore = beneEducationScore;

            for (FamilyMembersModel fm : familyMembers) {
                double fmGenderScore = getGenderScore(cs.decryptWithOneParameter(fm.getGender()), vulnScores);
                double fmMaritalScore = getMaritalStatusScore(cs.decryptWithOneParameter(fm.getMaritalStatus()), vulnScores);
                double fmDisabilityScore = getDisabilityScore(cs.decryptWithOneParameter(fm.getDisabilityType()), vulnScores);
                double fmHealthScore = getHealthScore(cs.decryptWithOneParameter(fm.getHealthCondition()), vulnScores);
                double fmEmploymentScore = getEmploymentScore(cs.decryptWithOneParameter(fm.getEmploymentStatus()), vulnScores);
                double fmEducationScore = getEducationScore(cs.decryptWithOneParameter(fm.getEducationalLevel()), vulnScores);

                totalGenderScore += fmGenderScore;
                totalMaritalScore += fmMaritalScore;
                totalDisabilityScore += fmDisabilityScore;
                totalHealthScore += fmHealthScore;
                totalEmploymentScore += fmEmploymentScore;
                totalEducationScore += fmEducationScore;

                debug("Family member disability score", fmDisabilityScore);
            }

            debug("Total disability score (sum)", totalDisabilityScore);
            debug("Average disability score", totalDisabilityScore / totalMembers);

            result.setGenderScore(Math.round((totalGenderScore / totalMembers) * 100.0) / 100.0);
            result.setMaritalStatusScore(Math.round((totalMaritalScore / totalMembers) * 100.0) / 100.0);
            result.setSoloParentScore(Math.round((totalSoloParentScore / totalMembers) * 100.0) / 100.0);
            result.setDisabilityScore(Math.round((totalDisabilityScore / totalMembers) * 100.0) / 100.0);
            result.setHealthConditionScore(Math.round((totalHealthScore / totalMembers) * 100.0) / 100.0);
            result.setEmploymentStatusScore(Math.round((totalEmploymentScore / totalMembers) * 100.0) / 100.0);
            result.setDisabilityScore(beneDisasterDamageScore);
            result.setEducationLevelScore(Math.round((totalEducationScore / totalMembers) * 100.0) / 100.0);

            result.setAccessToCleanWaterScore(Math.round(beneCleanWaterScore * 100.0) / 100.0);
            result.setSanitationFacilitiesScore(Math.round(beneSanitationScore * 100.0) / 100.0);
            result.setHouseConstructionTypeScore(Math.round(beneHouseTypeScore * 100.0) / 100.0);
            result.setOwnershipScore(Math.round(beneOwnershipScore * 100.0) / 100.0);
            result.setMonthlyIncomeScore(Math.round(beneIncomeScore * 100.0) / 100.0);
            result.setDigitalAccessScore(Math.round(beneDigitalScore * 100.0) / 100.0);

            debug("Final disability score", result.getDisabilityScore());

        } catch (Exception e) {
            System.err.println("Error calculating scores: " + e.getMessage());
            e.printStackTrace();
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
        try {
            conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapVulnerabilityScores(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
        return null;
    }

    private BeneficiaryModel getBeneficiaryById(int beneficiaryId) {
        String sql = "SELECT * FROM beneficiary WHERE beneficiary_id = ?";
        try {
            conn =  DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapBeneficiary(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
        return null;
    }

    private List<FamilyMembersModel> getFamilyMembersByBeneficiaryId(int beneficiaryId) {
        List<FamilyMembersModel> members = new ArrayList<>();
        String sql = "SELECT * FROM family_member WHERE beneficiary_id = ?";

        try {
            conn =  DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                members.add(mapFamilyMember(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
        return members;
    }

    private List<DisasterDamageModel> getDisasterDamageById(int beneficiaryId){
        List<DisasterDamageModel> members = new ArrayList<>();

        String sql = "SELECT * FROM beneficiary_disaster_damage WHERE beneficiary_id = ?";

        try{
            conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()){
                members.add(mapDisasterDamage(rs));
            }

        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            closeConnection();
        }
        return members;
    }

    private boolean saveHouseholdScore(HouseholdScoreModel score) {
        String sql = "INSERT INTO household_score (beneficiary_id, gender_score, marital_status_score, " +
                "solo_parent_score, disability_score, health_condition_score, access_to_clean_water_score, " +
                "sanitation_facilities_score, house_construction_type_score, ownership_score, " +
                "employment_status_score, monthly_income_score, education_level_score, " +
                "digital_access_score, creation_date) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW()) " +
                "ON DUPLICATE KEY UPDATE " +
                "gender_score=VALUES(gender_score), marital_status_score=VALUES(marital_status_score), " +
                "solo_parent_score=VALUES(solo_parent_score), disability_score=VALUES(disability_score), " +
                "health_condition_score=VALUES(health_condition_score), " +
                "access_to_clean_water_score=VALUES(access_to_clean_water_score), " +
                "sanitation_facilities_score=VALUES(sanitation_facilities_score), " +
                "house_construction_type_score=VALUES(house_construction_type_score), " +
                "ownership_score=VALUES(ownership_score), employment_status_score=VALUES(employment_status_score), " +
                "monthly_income_score=VALUES(monthly_income_score), education_level_score=VALUES(education_level_score), " +
                "digital_access_score=VALUES(digital_access_score), updating_date=NOW()";

        try {
            conn =  DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, score.getBeneficiaryId());
            ps.setDouble(2, score.getGenderScore());
            ps.setDouble(3, score.getMaritalStatusScore());
            ps.setDouble(4, score.getSoloParentScore());
            ps.setDouble(5, score.getDisabilityScore());
            ps.setDouble(6, score.getHealthConditionScore());
            ps.setDouble(7, score.getAccessToCleanWaterScore());
            ps.setDouble(8, score.getSanitationFacilitiesScore());
            ps.setDouble(9, score.getHouseConstructionTypeScore());
            ps.setDouble(10, score.getOwnershipScore());
            ps.setDouble(11, score.getEmploymentStatusScore());
            ps.setDouble(12, score.getMonthlyIncomeScore());
            ps.setDouble(13, score.getEducationLevelScore());
            ps.setDouble(14, score.getDigitalAccessScore());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnection();
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
        model.setGender(rs.getString("gender"));
        model.setMaritalStatus(rs.getString("marital_status"));
        model.setDisabilityType(rs.getString("disability_type"));
        model.setHealthCondition(rs.getString("health_condition"));
        model.setEmploymentStatus(rs.getString("employment_status"));
        model.setEducationalLevel(rs.getString("education_level"));
        return model;
    }

    private DisasterDamageModel mapDisasterDamage(ResultSet rs) throws  SQLException{
        DisasterDamageModel model = new DisasterDamageModel();
        model.setHouseDamageSeverity("house_damage_severity");
        return  model;
    }

    private void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}