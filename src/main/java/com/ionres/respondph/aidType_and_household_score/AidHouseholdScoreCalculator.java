package com.ionres.respondph.aidType_and_household_score;

import com.ionres.respondph.database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AidHouseholdScoreCalculator {
    private Connection conn;


    public boolean calculateAndSaveAidHouseholdScore(int beneficiaryId, int aidTypeId, int adminId) {
        try {
            HouseholdScoreData householdScores = getHouseholdScores(beneficiaryId);
            if (householdScores == null) {
                System.err.println("No household scores found for beneficiary ID: " + beneficiaryId);
                return false;
            }

            AidTypeWeights aidWeights = getAidTypeWeights(aidTypeId);
            if (aidWeights == null) {
                System.err.println("No aid type weights found for aid type ID: " + aidTypeId);
                return false;
            }

            double finalScore = calculateFinalScore(householdScores, aidWeights);

            String scoreCategory = determineScoreCategory(finalScore);

            return saveAidHouseholdScore(beneficiaryId, aidTypeId, adminId,
                    householdScores, finalScore, scoreCategory);

        } catch (Exception e) {
            System.err.println("Error calculating aid household score: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private double calculateFinalScore(HouseholdScoreData household, AidTypeWeights weights) {
        double totalScore = 0.0;
        int indicatorCount = 0;

        System.out.println("========== CALCULATING FINAL SCORE ==========");

        if (household.ageScore != null && weights.ageWeight != null) {
            double ageContribution = (household.ageScore + weights.ageWeight) / 2.0;
            totalScore += ageContribution;
            indicatorCount++;
            System.out.println("Age: (" + household.ageScore + " + " + weights.ageWeight + ") / 2 = " + ageContribution);
        }

        double genderContribution = (household.genderScore + weights.genderWeight) / 2.0;
        totalScore += genderContribution;
        indicatorCount++;
        System.out.println("Gender: (" + household.genderScore + " + " + weights.genderWeight + ") / 2 = " + genderContribution);

        double maritalContribution = (household.maritalStatusScore + weights.maritalStatusWeight) / 2.0;
        totalScore += maritalContribution;
        indicatorCount++;
        System.out.println("Marital: (" + household.maritalStatusScore + " + " + weights.maritalStatusWeight + ") / 2 = " + maritalContribution);

        double soloParentContribution = (household.soloParentScore + weights.soloParentWeight) / 2.0;
        totalScore += soloParentContribution;
        indicatorCount++;
        System.out.println("Solo Parent: (" + household.soloParentScore + " + " + weights.soloParentWeight + ") / 2 = " + soloParentContribution);

        double disabilityContribution = (household.disabilityScore + weights.disabilityWeight) / 2.0;
        totalScore += disabilityContribution;
        indicatorCount++;
        System.out.println("Disability: (" + household.disabilityScore + " + " + weights.disabilityWeight + ") / 2 = " + disabilityContribution);

        double healthContribution = (household.healthConditionScore + weights.healthConditionWeight) / 2.0;
        totalScore += healthContribution;
        indicatorCount++;
        System.out.println("Health: (" + household.healthConditionScore + " + " + weights.healthConditionWeight + ") / 2 = " + healthContribution);

        double waterContribution = (household.cleanWaterScore + weights.cleanWaterWeight) / 2.0;
        totalScore += waterContribution;
        indicatorCount++;
        System.out.println("Water: (" + household.cleanWaterScore + " + " + weights.cleanWaterWeight + ") / 2 = " + waterContribution);

        double sanitationContribution = (household.sanitationScore + weights.sanitationWeight) / 2.0;
        totalScore += sanitationContribution;
        indicatorCount++;
        System.out.println("Sanitation: (" + household.sanitationScore + " + " + weights.sanitationWeight + ") / 2 = " + sanitationContribution);

        double houseContribution = (household.houseConstructionScore + weights.houseConstructionWeight) / 2.0;
        totalScore += houseContribution;
        indicatorCount++;
        System.out.println("House: (" + household.houseConstructionScore + " + " + weights.houseConstructionWeight + ") / 2 = " + houseContribution);

        double ownershipContribution = (household.ownershipScore + weights.ownershipWeight) / 2.0;
        totalScore += ownershipContribution;
        indicatorCount++;
        System.out.println("Ownership: (" + household.ownershipScore + " + " + weights.ownershipWeight + ") / 2 = " + ownershipContribution);

        double damageContribution = (household.damageSeverityScore + weights.damageSeverityWeight) / 2.0;
        totalScore += damageContribution;
        indicatorCount++;
        System.out.println("Damage: (" + household.damageSeverityScore + " + " + weights.damageSeverityWeight + ") / 2 = " + damageContribution);

        double employmentContribution = (household.employmentStatusScore + weights.employmentStatusWeight) / 2.0;
        totalScore += employmentContribution;
        indicatorCount++;
        System.out.println("Employment: (" + household.employmentStatusScore + " + " + weights.employmentStatusWeight + ") / 2 = " + employmentContribution);

        double incomeContribution = (household.monthlyIncomeScore + weights.monthlyIncomeWeight) / 2.0;
        totalScore += incomeContribution;
        indicatorCount++;
        System.out.println("Income: (" + household.monthlyIncomeScore + " + " + weights.monthlyIncomeWeight + ") / 2 = " + incomeContribution);

        double educationContribution = (household.educationLevelScore + weights.educationLevelWeight) / 2.0;
        totalScore += educationContribution;
        indicatorCount++;
        System.out.println("Education: (" + household.educationLevelScore + " + " + weights.educationLevelWeight + ") / 2 = " + educationContribution);

        double digitalContribution = (household.digitalAccessScore + weights.digitalAccessWeight) / 2.0;
        totalScore += digitalContribution;
        indicatorCount++;
        System.out.println("Digital: (" + household.digitalAccessScore + " + " + weights.digitalAccessWeight + ") / 2 = " + digitalContribution);

        if (household.dependencyRatioScore != null && weights.dependencyRatioWeight != null) {
            double dependencyContribution = (household.dependencyRatioScore + weights.dependencyRatioWeight) / 2.0;
            totalScore += dependencyContribution;
            indicatorCount++;
            System.out.println("Dependency: (" + household.dependencyRatioScore + " + " + weights.dependencyRatioWeight + ") / 2 = " + dependencyContribution);
        }

        double averageScore = indicatorCount > 0 ? totalScore / indicatorCount : 0.0;

        System.out.println("─────────────────────────────────────────────");
        System.out.println("TOTAL SUM: " + totalScore);
        System.out.println("INDICATOR COUNT: " + indicatorCount);
        System.out.println("AVERAGE (FINAL SCORE): " + averageScore);
        System.out.println("=============================================");

        return Math.round(averageScore * 100.0) / 100.0; // Round to 2 decimal places
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


    private HouseholdScoreData getHouseholdScores(int beneficiaryId) {
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
                    System.out.println("Retrieved dependency_ratio_score from household_score: " + data.dependencyRatioScore);
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


    private AidTypeWeights getAidTypeWeights(int aidTypeId) {
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


    private boolean saveAidHouseholdScore(int beneficiaryId, int aidTypeId, int adminId,
                                          HouseholdScoreData household, double finalScore,
                                          String scoreCategory) {

        AidTypeWeights weights = getAidTypeWeights(aidTypeId);
        if (weights == null) {
            System.err.println("Cannot save: Aid type weights not found for aid type ID: " + aidTypeId);
            return false;
        }

        // Calculate combined scores
        Double combinedAgeScore = null;
        if (household.ageScore != null && weights.ageWeight != null) {
            combinedAgeScore = (household.ageScore + weights.ageWeight) / 2.0;
            System.out.println("Combined age_score: " + combinedAgeScore);
        }

        double combinedGenderScore = (household.genderScore + weights.genderWeight) / 2.0;
        double combinedMaritalScore = (household.maritalStatusScore + weights.maritalStatusWeight) / 2.0;
        double combinedSoloParentScore = (household.soloParentScore + weights.soloParentWeight) / 2.0;
        double combinedDisabilityScore = (household.disabilityScore + weights.disabilityWeight) / 2.0;
        double combinedHealthScore = (household.healthConditionScore + weights.healthConditionWeight) / 2.0;
        double combinedWaterScore = (household.cleanWaterScore + weights.cleanWaterWeight) / 2.0;
        double combinedSanitationScore = (household.sanitationScore + weights.sanitationWeight) / 2.0;
        double combinedHouseScore = (household.houseConstructionScore + weights.houseConstructionWeight) / 2.0;
        double combinedOwnershipScore = (household.ownershipScore + weights.ownershipWeight) / 2.0;
        double combinedDamageScore = (household.damageSeverityScore + weights.damageSeverityWeight) / 2.0;
        double combinedEmploymentScore = (household.employmentStatusScore + weights.employmentStatusWeight) / 2.0;
        double combinedIncomeScore = (household.monthlyIncomeScore + weights.monthlyIncomeWeight) / 2.0;
        double combinedEducationScore = (household.educationLevelScore + weights.educationLevelWeight) / 2.0;
        double combinedDigitalScore = (household.digitalAccessScore + weights.digitalAccessWeight) / 2.0;

        Double combinedDependencyScore = null;
        if (household.dependencyRatioScore != null && weights.dependencyRatioWeight != null) {
            combinedDependencyScore = (household.dependencyRatioScore + weights.dependencyRatioWeight) / 2.0;
            System.out.println("Combined dependency_ratio_score: " + combinedDependencyScore);
        }

        // Check if record exists
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
                ps.setObject(i++, Math.round(combinedAgeScore * 100.0) / 100.0);
                ps.setDouble(i++, Math.round(combinedGenderScore * 100.0) / 100.0);
                ps.setDouble(i++, Math.round(combinedMaritalScore * 100.0) / 100.0);
                ps.setDouble(i++, Math.round(combinedSoloParentScore * 100.0) / 100.0);
                ps.setDouble(i++, Math.round(combinedDisabilityScore * 100.0) / 100.0);
                ps.setDouble(i++, Math.round(combinedHealthScore * 100.0) / 100.0);
                ps.setDouble(i++, Math.round(combinedWaterScore * 100.0) / 100.0);
                ps.setDouble(i++, Math.round(combinedSanitationScore * 100.0) / 100.0);
                ps.setDouble(i++, Math.round(combinedHouseScore * 100.0) / 100.0);
                ps.setDouble(i++, Math.round(combinedOwnershipScore * 100.0) / 100.0);
                ps.setDouble(i++, Math.round(combinedDamageScore * 100.0) / 100.0);
                ps.setDouble(i++, Math.round(combinedEmploymentScore * 100.0) / 100.0);
                ps.setDouble(i++, Math.round(combinedIncomeScore * 100.0) / 100.0);
                ps.setDouble(i++, Math.round(combinedEducationScore * 100.0) / 100.0);
                ps.setDouble(i++, Math.round(combinedDigitalScore * 100.0) / 100.0);
                ps.setObject(i++, null); // household_members
                ps.setObject(i++, null); // able_bodied_members
                ps.setObject(i++, null); // vulnerable_members
                ps.setObject(i++, Math.round(combinedDependencyScore * 100.0) / 100.0);
                ps.setDouble(i++, finalScore);
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
                ps.setObject(i++, null); // household_members
                ps.setObject(i++, null); // able_bodied_members
                ps.setObject(i++, null); // vulnerable_members
                ps.setObject(i++, combinedDependencyScore);
                ps.setDouble(i++, finalScore);
                ps.setString(i++, scoreCategory);
                ps.setString(i++, "Weighted Average Model");
                ps.setInt(i++, aidTypeId);
                ps.setInt(i++, adminId);
            }

            int rowsAffected = ps.executeUpdate();
            ps.close();

            if (rowsAffected > 0) {
                System.out.println("Successfully saved aid_and_household_score with:");
                System.out.println("  - age_score: " + combinedAgeScore);
                System.out.println("  - dependency_ratio_score: " + combinedDependencyScore);
            }

            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnection();
        }
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

    // Data holder classes
    private static class HouseholdScoreData {
        Double ageScore;
        double genderScore;
        double maritalStatusScore;
        double soloParentScore;
        double disabilityScore;
        double healthConditionScore;
        double cleanWaterScore;
        double sanitationScore;
        double houseConstructionScore;
        double ownershipScore;
        double damageSeverityScore;
        double employmentStatusScore;
        double monthlyIncomeScore;
        double educationLevelScore;
        double digitalAccessScore;
        Double dependencyRatioScore;
    }

    private static class AidTypeWeights {
        Double ageWeight;
        double genderWeight;
        double maritalStatusWeight;
        double soloParentWeight;
        double disabilityWeight;
        double healthConditionWeight;
        double cleanWaterWeight;
        double sanitationWeight;
        double houseConstructionWeight;
        double ownershipWeight;
        double damageSeverityWeight;
        double employmentStatusWeight;
        double monthlyIncomeWeight;
        double educationLevelWeight;
        double digitalAccessWeight;
        Double dependencyRatioWeight;
    }
}