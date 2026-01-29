package com.ionres.respondph.aidType_and_household_score;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AidHouseholdScoreDAOImpl implements AidHouseholdScoreDAO {

    private Connection conn;
    private final Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

    @Override
    public HouseholdScoreData getHouseholdScoresWithDisaster(int beneficiaryId, int disasterId) {
        // ✅ MODIFIED: Added disaster_id to WHERE clause
        String sql = "SELECT * FROM household_score WHERE beneficiary_id = ? AND disaster_id = ?";

        try {
            conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ps.setInt(2, disasterId);  // ✅ NEW
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                HouseholdScoreData data = new HouseholdScoreData();

                try {
                    data.ageScore = rs.getObject("age_score") != null ? rs.getDouble("age_score") : null;
                    System.out.println("Retrieved age_score from household_score: " + data.ageScore);
                } catch (SQLException e) {
                    System.err.println("Error retrieving age_score: " + e.getMessage());
                    data.ageScore = null;
                }

                data.genderScore = rs.getDouble("gender_score");
                data.maritalStatusScore = rs.getDouble("marital_status_score");
                data.soloParentScore = rs.getDouble("solo_parent_score");
                data.disabilityScore = rs.getDouble("disability_score");
                data.healthConditionScore = rs.getDouble("health_condition_score");
                data.cleanWaterScore = rs.getDouble("access_to_clean_water_score");
                data.sanitationScore = rs.getDouble("sanitation_facilities_score");
                data.houseConstructionScore = rs.getDouble("house_construction_type_score");
                data.ownershipScore = rs.getDouble("ownership_score");
                data.damageSeverityScore = rs.getDouble("damage_severity_score");
                data.employmentStatusScore = rs.getDouble("employment_status_score");
                data.monthlyIncomeScore = rs.getDouble("monthly_income_score");
                data.educationLevelScore = rs.getDouble("education_level_score");
                data.digitalAccessScore = rs.getDouble("digital_access_score");

                try {
                    data.dependencyRatioScore = rs.getObject("dependency_ratio_score") != null ?
                            rs.getDouble("dependency_ratio_score") : null;
                    System.out.println("Retrieved dependency_ratio_score: " + data.dependencyRatioScore);
                } catch (SQLException e) {
                    System.err.println("Error retrieving dependency_ratio_score: " + e.getMessage());
                    data.dependencyRatioScore = null;
                }

                return data;
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return null;
    }

    @Override
    public HouseholdScoreData getHouseholdScores(int beneficiaryId) {
        String sql = "SELECT * FROM household_score WHERE beneficiary_id = ?";

        try {
            conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                HouseholdScoreData data = new HouseholdScoreData();

                try {
                    data.ageScore = rs.getObject("age_score") != null ? rs.getDouble("age_score") : null;
                    System.out.println("Retrieved age_score from household_score: " + data.ageScore);
                } catch (SQLException e) {
                    System.err.println("Error retrieving age_score: " + e.getMessage());
                    data.ageScore = null;
                }

                data.genderScore = rs.getDouble("gender_score");
                data.maritalStatusScore = rs.getDouble("marital_status_score");
                data.soloParentScore = rs.getDouble("solo_parent_score");
                data.disabilityScore = rs.getDouble("disability_score");
                data.healthConditionScore = rs.getDouble("health_condition_score");
                data.cleanWaterScore = rs.getDouble("access_to_clean_water_score");
                data.sanitationScore = rs.getDouble("sanitation_facilities_score");
                data.houseConstructionScore = rs.getDouble("house_construction_type_score");
                data.ownershipScore = rs.getDouble("ownership_score");
                data.damageSeverityScore = rs.getDouble("damage_severity_score");
                data.employmentStatusScore = rs.getDouble("employment_status_score");
                data.monthlyIncomeScore = rs.getDouble("monthly_income_score");
                data.educationLevelScore = rs.getDouble("education_level_score");
                data.digitalAccessScore = rs.getDouble("digital_access_score");

                try {
                    data.dependencyRatioScore = rs.getObject("dependency_ratio_score") != null ?
                            rs.getDouble("dependency_ratio_score") : null;
                    System.out.println("Retrieved dependency_ratio_score: " + data.dependencyRatioScore);
                } catch (SQLException e) {
                    System.err.println("Error retrieving dependency_ratio_score: " + e.getMessage());
                    data.dependencyRatioScore = null;
                }

                return data;
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return null;
    }

    @Override
    public AidTypeWeights getAidTypeWeights(int aidTypeId) {
        String sql = "SELECT * FROM aid_type WHERE aid_type_id = ?";

        try {
            conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, aidTypeId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                AidTypeWeights weights = new AidTypeWeights();
                weights.ageWeight = rs.getObject("age_weight") != null ? rs.getDouble("age_weight") : null;
                weights.genderWeight = rs.getDouble("gender_weight");
                weights.maritalStatusWeight = rs.getDouble("marital_status_weight");
                weights.soloParentWeight = rs.getDouble("solo_parent_weight");
                weights.disabilityWeight = rs.getDouble("disability_weight");
                weights.healthConditionWeight = rs.getDouble("health_condition_weight");
                weights.cleanWaterWeight = rs.getDouble("access_to_clean_water_weight");
                weights.sanitationWeight = rs.getDouble("sanitation_facilities_weight");
                weights.houseConstructionWeight = rs.getDouble("house_construction_type_weight");
                weights.ownershipWeight = rs.getDouble("ownership_weight");
                weights.damageSeverityWeight = rs.getDouble("damage_severity_weight");
                weights.employmentStatusWeight = rs.getDouble("employment_status_weight");
                weights.monthlyIncomeWeight = rs.getDouble("monthly_income_weight");
                weights.educationLevelWeight = rs.getDouble("education_level_weight");
                weights.digitalAccessWeight = rs.getDouble("digital_access_weight");
                weights.dependencyRatioWeight = rs.getObject("dependency_ratio_weight") != null ?
                        rs.getDouble("dependency_ratio_weight") : null;
                return weights;
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return null;
    }

    @Override
    public HouseholdMemberCounts countHouseholdMembers(int beneficiaryId) {
        HouseholdMemberCounts counts = new HouseholdMemberCounts();

        String beneficiarySql = "SELECT age_score, disability_type, health_condition " +
                "FROM beneficiary WHERE beneficiary_id = ?";

        String familySql = "SELECT age_score, disability_type, health_condition " +
                "FROM family_member WHERE beneficiary_id = ?";

        try {
            conn = DBConnection.getInstance().getConnection();

            int ableBodyCount = 0;
            int vulnerableCount = 0;

            // Count beneficiary first
            PreparedStatement benPs = conn.prepareStatement(beneficiarySql);
            benPs.setInt(1, beneficiaryId);
            ResultSet benRs = benPs.executeQuery();

            if (benRs.next()) {
                double bAgeScore = benRs.getDouble("age_score");
                String bDisability = benRs.getString("disability_type");
                String bHealth = benRs.getString("health_condition");

                try {
                    bDisability = cs.decryptWithOneParameter(bDisability);
                    bHealth = cs.decryptWithOneParameter(bHealth);
                } catch (Exception e) {
                    System.err.println("Error decrypting beneficiary data: " + e.getMessage());
                    bDisability = "None";
                    bHealth = "Healthy";
                }

                if (isVulnerablePerson(bAgeScore, bDisability, bHealth)) {
                    vulnerableCount++;
                    System.out.println("✓ Beneficiary classified as VULNERABLE");
                } else {
                    ableBodyCount++;
                    System.out.println("✓ Beneficiary classified as ABLE-BODIED");
                }
            }
            benRs.close();
            benPs.close();

            // Count family members
            PreparedStatement famPs = conn.prepareStatement(familySql);
            famPs.setInt(1, beneficiaryId);
            ResultSet famRs = famPs.executeQuery();

            int familyMemberIndex = 1;
            while (famRs.next()) {
                double fmAgeScore = famRs.getDouble("age_score");
                String fmDisability = famRs.getString("disability_type");
                String fmHealth = famRs.getString("health_condition");

                try {
                    fmDisability = cs.decryptWithOneParameter(fmDisability);
                    fmHealth = cs.decryptWithOneParameter(fmHealth);
                } catch (Exception e) {
                    System.err.println("Error decrypting family member data: " + e.getMessage());
                    fmDisability = "None";
                    fmHealth = "Healthy";
                }

                if (isVulnerablePerson(fmAgeScore, fmDisability, fmHealth)) {
                    vulnerableCount++;
                    System.out.println("✓ Family Member #" + familyMemberIndex + " classified as VULNERABLE");
                } else {
                    ableBodyCount++;
                    System.out.println("✓ Family Member #" + familyMemberIndex + " classified as ABLE-BODIED");
                }
                familyMemberIndex++;
            }
            famRs.close();
            famPs.close();

            // Set final counts
            counts.totalMembers = ableBodyCount + vulnerableCount;
            counts.ableBodyMembers = ableBodyCount;
            counts.vulnerableMembers = vulnerableCount;

            System.out.println("========== HOUSEHOLD MEMBER COUNTS ==========");
            System.out.println("Total Household Members: " + counts.totalMembers);
            System.out.println("Able-Bodied Members: " + counts.ableBodyMembers);
            System.out.println("Vulnerable Members: " + counts.vulnerableMembers);
            System.out.println("============================================");

        } catch (SQLException e) {
            System.err.println("Error counting household members: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return counts;
    }

    private boolean isVulnerablePerson(double ageScore, String disability, String health) {

        if (ageScore == 1.0 || ageScore == 0.7) {
            System.out.println("  → Vulnerable due to age (Score: " + ageScore + ")");
            return true;
        }

        if (disability != null && !disability.trim().equalsIgnoreCase("none")
                && !disability.trim().isEmpty()) {
            System.out.println("  → Vulnerable due to disability: " + disability);
            return true;
        }

        if (health != null) {
            String hc = health.trim().toLowerCase();
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

    @Override
    public boolean saveAidHouseholdScoreWithDisaster(int beneficiaryId, int aidTypeId, int adminId, int disasterId,
                                                     HouseholdScoreData household, double finalScore,
                                                     String scoreCategory, HouseholdMemberCounts memberCounts) {

        AidTypeWeights weights = getAidTypeWeights(aidTypeId);
        if (weights == null) {
            System.err.println("Cannot save: Aid type weights not found for aid type ID: " + aidTypeId);
            return false;
        }

        // Calculate combined scores WITH PROPER ROUNDING
        Double combinedAgeScore = null;
        if (household.ageScore != null && weights.ageWeight != null) {
            combinedAgeScore = roundToTwoDecimals(household.ageScore * weights.ageWeight);
        }

        double combinedGenderScore = roundToTwoDecimals(household.genderScore * weights.genderWeight);
        double combinedMaritalScore = roundToTwoDecimals(household.maritalStatusScore * weights.maritalStatusWeight);
        double combinedSoloParentScore = roundToTwoDecimals(household.soloParentScore * weights.soloParentWeight);
        double combinedDisabilityScore = roundToTwoDecimals(household.disabilityScore * weights.disabilityWeight);
        double combinedHealthScore = roundToTwoDecimals(household.healthConditionScore * weights.healthConditionWeight);
        double combinedWaterScore = roundToTwoDecimals(household.cleanWaterScore * weights.cleanWaterWeight);
        double combinedSanitationScore = roundToTwoDecimals(household.sanitationScore * weights.sanitationWeight);
        double combinedHouseScore = roundToTwoDecimals(household.houseConstructionScore * weights.houseConstructionWeight);
        double combinedOwnershipScore = roundToTwoDecimals(household.ownershipScore * weights.ownershipWeight);
        double combinedDamageScore = roundToTwoDecimals(household.damageSeverityScore * weights.damageSeverityWeight);
        double combinedEmploymentScore = roundToTwoDecimals(household.employmentStatusScore * weights.employmentStatusWeight);
        double combinedIncomeScore = roundToTwoDecimals(household.monthlyIncomeScore * weights.monthlyIncomeWeight);
        double combinedEducationScore = roundToTwoDecimals(household.educationLevelScore * weights.educationLevelWeight);
        double combinedDigitalScore = roundToTwoDecimals(household.digitalAccessScore * weights.digitalAccessWeight);

        Double combinedDependencyScore = null;
        if (household.dependencyRatioScore != null && weights.dependencyRatioWeight != null) {
            combinedDependencyScore = roundToTwoDecimals(household.dependencyRatioScore * weights.dependencyRatioWeight);
        }

        // ✅ MODIFIED: Check includes disaster_id
        String checkSql = "SELECT beneficiary_family_score_id FROM aid_and_household_score " +
                "WHERE beneficiary_id = ? AND aid_type_id = ? AND disaster_id = ?";

        // ✅ MODIFIED: Insert includes disaster_id
        String insertSql = "INSERT INTO aid_and_household_score (" +
                "beneficiary_id, disaster_id, age_score, gender_score, marital_status_score, solo_parent_score, " +
                "disability_score, health_condition_score, access_to_clean_water_score, " +
                "sanitation_facilities_score, house_construction_type_score, ownership_score, " +
                "damage_severity_score, employment_status_score, monthly_income_score, " +
                "education_level_score, digital_access_score, household_members, able_bodied_members, " +
                "vulnerable_members, dependency_ratio_score, final_score, score_category, " +
                "scoring_model, assessment_date, aid_type_id, admin_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?)";

        // ✅ MODIFIED: Update WHERE clause includes disaster_id
        String updateSql = "UPDATE aid_and_household_score SET " +
                "age_score=?, gender_score=?, marital_status_score=?, solo_parent_score=?, " +
                "disability_score=?, health_condition_score=?, access_to_clean_water_score=?, " +
                "sanitation_facilities_score=?, house_construction_type_score=?, ownership_score=?, " +
                "damage_severity_score=?, employment_status_score=?, monthly_income_score=?, " +
                "education_level_score=?, digital_access_score=?, household_members=?, able_bodied_members=?, " +
                "vulnerable_members=?, dependency_ratio_score=?, final_score=?, score_category=?, " +
                "scoring_model=?, assessment_date=NOW(), admin_id=? " +
                "WHERE beneficiary_id=? AND aid_type_id=? AND disaster_id=?";

        try {
            conn = DBConnection.getInstance().getConnection();

            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setInt(1, beneficiaryId);
            checkPs.setInt(2, aidTypeId);
            checkPs.setInt(3, disasterId);  // ✅ NEW
            ResultSet rs = checkPs.executeQuery();
            boolean recordExists = rs.next();
            rs.close();
            checkPs.close();

            PreparedStatement ps;

            if (recordExists) {
                System.out.println("Aid-Household score exists - Updating existing record for disaster ID: " + disasterId);
                ps = conn.prepareStatement(updateSql);

                int i = 1;
                ps.setObject(i++, combinedAgeScore);
                ps.setDouble(i++, combinedGenderScore);
                ps.setDouble(i++, combinedMaritalScore);
                ps.setDouble(i++, combinedSoloParentScore);
                ps.setDouble(i++, combinedDisabilityScore);
                ps.setDouble(i++, combinedHealthScore);
                ps.setDouble(i++, combinedWaterScore);
                ps.setDouble(i++, combinedSanitationScore);
                ps.setDouble(i++, combinedHouseScore);
                ps.setDouble(i++, combinedOwnershipScore);
                ps.setDouble(i++, combinedDamageScore);
                ps.setDouble(i++, combinedEmploymentScore);
                ps.setDouble(i++, combinedIncomeScore);
                ps.setDouble(i++, combinedEducationScore);
                ps.setDouble(i++, combinedDigitalScore);
                ps.setInt(i++, memberCounts.totalMembers);
                ps.setInt(i++, memberCounts.ableBodyMembers);
                ps.setInt(i++, memberCounts.vulnerableMembers);
                ps.setObject(i++, combinedDependencyScore);
                ps.setDouble(i++, roundToTwoDecimals(finalScore));
                ps.setString(i++, scoreCategory);
                ps.setString(i++, "Weighted Average Model");
                ps.setInt(i++, adminId);
                ps.setInt(i++, beneficiaryId);
                ps.setInt(i++, aidTypeId);
                ps.setInt(i++, disasterId);  // ✅ NEW

            } else {
                System.out.println("Aid-Household score doesn't exist - Inserting new record for disaster ID: " + disasterId);
                ps = conn.prepareStatement(insertSql);

                int i = 1;
                ps.setInt(i++, beneficiaryId);
                ps.setInt(i++, disasterId);  // ✅ NEW
                ps.setObject(i++, combinedAgeScore);
                ps.setDouble(i++, combinedGenderScore);
                ps.setDouble(i++, combinedMaritalScore);
                ps.setDouble(i++, combinedSoloParentScore);
                ps.setDouble(i++, combinedDisabilityScore);
                ps.setDouble(i++, combinedHealthScore);
                ps.setDouble(i++, combinedWaterScore);
                ps.setDouble(i++, combinedSanitationScore);
                ps.setDouble(i++, combinedHouseScore);
                ps.setDouble(i++, combinedOwnershipScore);
                ps.setDouble(i++, combinedDamageScore);
                ps.setDouble(i++, combinedEmploymentScore);
                ps.setDouble(i++, combinedIncomeScore);
                ps.setDouble(i++, combinedEducationScore);
                ps.setDouble(i++, combinedDigitalScore);
                ps.setInt(i++, memberCounts.totalMembers);
                ps.setInt(i++, memberCounts.ableBodyMembers);
                ps.setInt(i++, memberCounts.vulnerableMembers);
                ps.setObject(i++, combinedDependencyScore);
                ps.setDouble(i++, roundToTwoDecimals(finalScore));
                ps.setString(i++, scoreCategory);
                ps.setString(i++, "Weighted Average Model");
                ps.setInt(i++, aidTypeId);
                ps.setInt(i++, adminId);
            }

            int rowsAffected = ps.executeUpdate();
            ps.close();

            if (rowsAffected > 0) {
                System.out.println("Successfully saved aid_and_household_score with:");
                System.out.println("  - beneficiary_id: " + beneficiaryId);
                System.out.println("  - disaster_id: " + disasterId);
                System.out.println("  - aid_type_id: " + aidTypeId);
                System.out.println("  - household_members: " + memberCounts.totalMembers);
                System.out.println("  - able_bodied_members: " + memberCounts.ableBodyMembers);
                System.out.println("  - vulnerable_members: " + memberCounts.vulnerableMembers);
                System.out.println("  - final_score: " + roundToTwoDecimals(finalScore));
            }

            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnection();
        }
    }


    @Override
    public boolean saveAidHouseholdScore(int beneficiaryId, int aidTypeId, int adminId,
                                         HouseholdScoreData household, double finalScore,
                                         String scoreCategory, HouseholdMemberCounts memberCounts) {

        AidTypeWeights weights = getAidTypeWeights(aidTypeId);
        if (weights == null) {
            System.err.println("Cannot save: Aid type weights not found for aid type ID: " + aidTypeId);
            return false;
        }

        // Calculate combined scores WITH PROPER ROUNDING
        Double combinedAgeScore = null;
        if (household.ageScore != null && weights.ageWeight != null) {
            combinedAgeScore = roundToTwoDecimals(household.ageScore * weights.ageWeight);
        }

        double combinedGenderScore = roundToTwoDecimals(household.genderScore * weights.genderWeight);
        double combinedMaritalScore = roundToTwoDecimals(household.maritalStatusScore * weights.maritalStatusWeight);
        double combinedSoloParentScore = roundToTwoDecimals(household.soloParentScore * weights.soloParentWeight);
        double combinedDisabilityScore = roundToTwoDecimals(household.disabilityScore * weights.disabilityWeight);
        double combinedHealthScore = roundToTwoDecimals(household.healthConditionScore * weights.healthConditionWeight);
        double combinedWaterScore = roundToTwoDecimals(household.cleanWaterScore * weights.cleanWaterWeight);
        double combinedSanitationScore = roundToTwoDecimals(household.sanitationScore * weights.sanitationWeight);
        double combinedHouseScore = roundToTwoDecimals(household.houseConstructionScore * weights.houseConstructionWeight);
        double combinedOwnershipScore = roundToTwoDecimals(household.ownershipScore * weights.ownershipWeight);
        double combinedDamageScore = roundToTwoDecimals(household.damageSeverityScore * weights.damageSeverityWeight);
        double combinedEmploymentScore = roundToTwoDecimals(household.employmentStatusScore * weights.employmentStatusWeight);
        double combinedIncomeScore = roundToTwoDecimals(household.monthlyIncomeScore * weights.monthlyIncomeWeight);
        double combinedEducationScore = roundToTwoDecimals(household.educationLevelScore * weights.educationLevelWeight);
        double combinedDigitalScore = roundToTwoDecimals(household.digitalAccessScore * weights.digitalAccessWeight);

        Double combinedDependencyScore = null;
        if (household.dependencyRatioScore != null && weights.dependencyRatioWeight != null) {
            combinedDependencyScore = roundToTwoDecimals(household.dependencyRatioScore * weights.dependencyRatioWeight);
        }

        String checkSql = "SELECT beneficiary_family_score_id FROM aid_and_household_score " +
                "WHERE beneficiary_id = ? AND aid_type_id = ?";

        String insertSql = "INSERT INTO aid_and_household_score (" +
                "beneficiary_id, age_score, gender_score, marital_status_score, solo_parent_score, " +
                "disability_score, health_condition_score, access_to_clean_water_score, " +
                "sanitation_facilities_score, house_construction_type_score, ownership_score, " +
                "damage_severity_score, employment_status_score, monthly_income_score, " +
                "education_level_score, digital_access_score, household_members, able_bodied_members, " +
                "vulnerable_members, dependency_ratio_score, final_score, score_category, " +
                "scoring_model, assessment_date, aid_type_id, admin_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?)";

        String updateSql = "UPDATE aid_and_household_score SET " +
                "age_score=?, gender_score=?, marital_status_score=?, solo_parent_score=?, " +
                "disability_score=?, health_condition_score=?, access_to_clean_water_score=?, " +
                "sanitation_facilities_score=?, house_construction_type_score=?, ownership_score=?, " +
                "damage_severity_score=?, employment_status_score=?, monthly_income_score=?, " +
                "education_level_score=?, digital_access_score=?, household_members=?, able_bodied_members=?, " +
                "vulnerable_members=?, dependency_ratio_score=?, final_score=?, score_category=?, " +
                "scoring_model=?, assessment_date=NOW(), admin_id=? " +
                "WHERE beneficiary_id=? AND aid_type_id=?";

        try {
            conn = DBConnection.getInstance().getConnection();

            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setInt(1, beneficiaryId);
            checkPs.setInt(2, aidTypeId);
            ResultSet rs = checkPs.executeQuery();
            boolean recordExists = rs.next();
            rs.close();
            checkPs.close();

            PreparedStatement ps;

            if (recordExists) {
                System.out.println("Aid-Household score exists - Updating existing record");
                ps = conn.prepareStatement(updateSql);

                int i = 1;
                ps.setObject(i++, combinedAgeScore);
                ps.setDouble(i++, combinedGenderScore);
                ps.setDouble(i++, combinedMaritalScore);
                ps.setDouble(i++, combinedSoloParentScore);
                ps.setDouble(i++, combinedDisabilityScore);
                ps.setDouble(i++, combinedHealthScore);
                ps.setDouble(i++, combinedWaterScore);
                ps.setDouble(i++, combinedSanitationScore);
                ps.setDouble(i++, combinedHouseScore);
                ps.setDouble(i++, combinedOwnershipScore);
                ps.setDouble(i++, combinedDamageScore);
                ps.setDouble(i++, combinedEmploymentScore);
                ps.setDouble(i++, combinedIncomeScore);
                ps.setDouble(i++, combinedEducationScore);
                ps.setDouble(i++, combinedDigitalScore);
                ps.setInt(i++, memberCounts.totalMembers);
                ps.setInt(i++, memberCounts.ableBodyMembers);
                ps.setInt(i++, memberCounts.vulnerableMembers);
                ps.setObject(i++, combinedDependencyScore);
                ps.setDouble(i++, roundToTwoDecimals(finalScore));
                ps.setString(i++, scoreCategory);
                ps.setString(i++, "Weighted Average Model");
                ps.setInt(i++, adminId);
                ps.setInt(i++, beneficiaryId);
                ps.setInt(i++, aidTypeId);

            } else {
                System.out.println("Aid-Household score doesn't exist - Inserting new record");
                ps = conn.prepareStatement(insertSql);

                int i = 1;
                ps.setInt(i++, beneficiaryId);
                ps.setObject(i++, combinedAgeScore);
                ps.setDouble(i++, combinedGenderScore);
                ps.setDouble(i++, combinedMaritalScore);
                ps.setDouble(i++, combinedSoloParentScore);
                ps.setDouble(i++, combinedDisabilityScore);
                ps.setDouble(i++, combinedHealthScore);
                ps.setDouble(i++, combinedWaterScore);
                ps.setDouble(i++, combinedSanitationScore);
                ps.setDouble(i++, combinedHouseScore);
                ps.setDouble(i++, combinedOwnershipScore);
                ps.setDouble(i++, combinedDamageScore);
                ps.setDouble(i++, combinedEmploymentScore);
                ps.setDouble(i++, combinedIncomeScore);
                ps.setDouble(i++, combinedEducationScore);
                ps.setDouble(i++, combinedDigitalScore);
                ps.setInt(i++, memberCounts.totalMembers);
                ps.setInt(i++, memberCounts.ableBodyMembers);
                ps.setInt(i++, memberCounts.vulnerableMembers);
                ps.setObject(i++, combinedDependencyScore);
                ps.setDouble(i++, roundToTwoDecimals(finalScore));
                ps.setString(i++, scoreCategory);
                ps.setString(i++, "Weighted Average Model");
                ps.setInt(i++, aidTypeId);
                ps.setInt(i++, adminId);
            }

            int rowsAffected = ps.executeUpdate();
            ps.close();

            if (rowsAffected > 0) {
                System.out.println("Successfully saved aid_and_household_score with:");
                System.out.println("  - household_members: " + memberCounts.totalMembers);
                System.out.println("  - able_bodied_members: " + memberCounts.ableBodyMembers);
                System.out.println("  - vulnerable_members: " + memberCounts.vulnerableMembers);
                System.out.println("  - final_score: " + roundToTwoDecimals(finalScore));
            }

            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnection();
        }
    }


    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
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



