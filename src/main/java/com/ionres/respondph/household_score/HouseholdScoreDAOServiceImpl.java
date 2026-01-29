package com.ionres.respondph.household_score;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.disaster_damage.DisasterDamageModel;
import com.ionres.respondph.familymembers.FamilyMembersModel;
import com.ionres.respondph.vulnerability_indicator.VulnerabilityIndicatorScoreModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HouseholdScoreDAOServiceImpl implements HouseholdScoreDAO {

    private Connection conn;

    @Override
    public VulnerabilityIndicatorScoreModel getVulnerabilityScores() {
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

    @Override
    public BeneficiaryModel getBeneficiaryById(int beneficiaryId) {
        String sql = "SELECT * FROM beneficiary WHERE beneficiary_id = ?";
        try {
            conn = DBConnection.getInstance().getConnection();
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

    @Override
    public List<FamilyMembersModel> getFamilyMembersByBeneficiaryId(int beneficiaryId) {
        List<FamilyMembersModel> members = new ArrayList<>();
        String sql = "SELECT * FROM family_member WHERE beneficiary_id = ?";

        try {
            conn = DBConnection.getInstance().getConnection();
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

    @Override
    public List<DisasterDamageModel> getDisasterDamageById(int beneficiaryId) {
        List<DisasterDamageModel> members = new ArrayList<>();
        String sql = "SELECT * FROM beneficiary_disaster_damage WHERE beneficiary_id = ?";

        try {
            conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                members.add(mapDisasterDamage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
        return members;
    }

    @Override
    public boolean saveHouseholdScore(HouseholdScoreModel score) {
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
                "house_construction_type_score=?, ownership_score=?, " +
                "employment_status_score=?, monthly_income_score=?, education_level_score=?, " +
                "digital_access_score=?, dependency_ratio_score=?, updating_date=NOW() WHERE beneficiary_id=?";

        try {
            conn = DBConnection.getInstance().getConnection();

            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setInt(1, score.getBeneficiaryId());
            ResultSet rs = checkPs.executeQuery();
            boolean recordExists = rs.next();
            rs.close();
            checkPs.close();

            PreparedStatement ps;

            if (recordExists) {
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
                ps.setDouble(11, score.getEmploymentStatusScore());
                ps.setDouble(12, score.getMonthlyIncomeScore());
                ps.setDouble(13, score.getEducationLevelScore());
                ps.setDouble(14, score.getDigitalAccessScore());
                ps.setDouble(15, score.getDependencyRatioScore());
                ps.setInt(16, score.getBeneficiaryId());
            } else {
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
            ps.close();

            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnection();
        }
    }

    @Override
    public boolean saveHouseholdScoreWithDisaster(HouseholdScoreModel score) {
        // ✅ MODIFIED: Check if record exists for THIS beneficiary AND THIS disaster
        String checkSql = "SELECT household_score_id FROM household_score " +
                "WHERE beneficiary_id = ? AND disaster_id = ?";

        String insertSql = "INSERT INTO household_score (beneficiary_id, disaster_id, age_score, gender_score, " +
                "marital_status_score, solo_parent_score, disability_score, health_condition_score, " +
                "access_to_clean_water_score, sanitation_facilities_score, house_construction_type_score, " +
                "ownership_score, damage_severity_score, employment_status_score, monthly_income_score, " +
                "education_level_score, digital_access_score, dependency_ratio_score, creation_date) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())";

        String updateSql = "UPDATE household_score SET " +
                "age_score=?, gender_score=?, marital_status_score=?, solo_parent_score=?, disability_score=?, " +
                "health_condition_score=?, access_to_clean_water_score=?, sanitation_facilities_score=?, " +
                "house_construction_type_score=?, ownership_score=?, damage_severity_score=?, " +
                "employment_status_score=?, monthly_income_score=?, education_level_score=?, " +
                "digital_access_score=?, dependency_ratio_score=?, updating_date=NOW() " +
                "WHERE beneficiary_id=? AND disaster_id=?";

        try {
            conn = DBConnection.getInstance().getConnection();

            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setInt(1, score.getBeneficiaryId());
            checkPs.setInt(2, score.getDisasterId());  // ✅ NEW
            ResultSet rs = checkPs.executeQuery();
            boolean recordExists = rs.next();
            rs.close();
            checkPs.close();

            PreparedStatement ps;

            if (recordExists) {
                // ✅ UPDATE existing record for this beneficiary + disaster combination
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
                ps.setInt(18, score.getDisasterId());  // ✅ NEW
            } else {
                // ✅ INSERT new record for this beneficiary + disaster combination
                ps = conn.prepareStatement(insertSql);
                ps.setInt(1, score.getBeneficiaryId());
                ps.setInt(2, score.getDisasterId());  // ✅ NEW
                ps.setDouble(3, score.getAgeScore());
                ps.setDouble(4, score.getGenderScore());
                ps.setDouble(5, score.getMaritalStatusScore());
                ps.setDouble(6, score.getSoloParentScore());
                ps.setDouble(7, score.getDisabilityScore());
                ps.setDouble(8, score.getHealthConditionScore());
                ps.setDouble(9, score.getAccessToCleanWaterScore());
                ps.setDouble(10, score.getSanitationFacilitiesScore());
                ps.setDouble(11, score.getHouseConstructionTypeScore());
                ps.setDouble(12, score.getOwnershipScore());
                ps.setDouble(13, score.getDamageSeverityScore());
                ps.setDouble(14, score.getEmploymentStatusScore());
                ps.setDouble(15, score.getMonthlyIncomeScore());
                ps.setDouble(16, score.getEducationLevelScore());
                ps.setDouble(17, score.getDigitalAccessScore());
                ps.setDouble(18, score.getDependencyRatioScore());
            }

            int rowsAffected = ps.executeUpdate();
            ps.close();

            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnection();
        }
    }

    @Override
    public List<Integer> getAllBeneficiaryIdsWithHouseholdScores() {
        List<Integer> beneficiaryIds = new ArrayList<>();
        String sql = "SELECT DISTINCT beneficiary_id FROM household_score";

        try {
            conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                beneficiaryIds.add(rs.getInt("beneficiary_id"));
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.err.println("Error fetching beneficiary IDs with household scores: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return beneficiaryIds;
    }


    private VulnerabilityIndicatorScoreModel mapVulnerabilityScores(ResultSet rs) throws SQLException {
        VulnerabilityIndicatorScoreModel model = new VulnerabilityIndicatorScoreModel();

        // Gender scores
        model.setMaleScore(rs.getDouble("male_score"));
        model.setFemaleScore(rs.getDouble("female_score"));

        // Civil status scores (removed divorce_score)
        model.setSingleScore(rs.getDouble("single_score"));
        model.setMarriedScore(rs.getDouble("married_score"));
        model.setWidowedScore(rs.getDouble("widowed_score"));
        model.setSeparatedScore(rs.getDouble("separated_score"));

        // Solo parent scores (changed from 2 to 3 fields)
        model.setSoloParentNotSoloParentScore(rs.getDouble("solo_parent_status_not_solo_parent_score"));
        model.setSoloParentSpWithSnScore(rs.getDouble("solo_parent_status_sp_with_sn_score"));
        model.setSoloParentSpWithoutSnScore(rs.getDouble("solo_parent_status_sp_without_sn_score"));

        // Disability scores (added disability_multiple_score)
        model.setDisabilityNoneScore(rs.getDouble("disability_none_score"));
        model.setDisabilityPhysicalScore(rs.getDouble("disability_physical_score"));
        model.setDisabilityVisualScore(rs.getDouble("disability_visual_score"));
        model.setDisabilityHearingScore(rs.getDouble("disability_hearing_score"));
        model.setDisabilitySpeechScore(rs.getDouble("disability_speech_score"));
        model.setDisabilityIntellectualScore(rs.getDouble("disability_intellectual_score"));
        model.setDisabilityMentalScore(rs.getDouble("disability_mental_or_psychosocial_score"));
        model.setDisabilityChronicScore(rs.getDouble("disability_chronic_score"));
        model.setDisabilityMultipleScore(rs.getDouble("disability_multiple_score"));

        // Health scores (renamed health_with_history_score to health_with_medical_score)
        model.setHealthHealthyScore(rs.getDouble("health_healthy_score"));
        model.setHealthTemporarilyIllScore(rs.getDouble("health_temporarily_ill_score"));
        model.setHealthChronicallyIllScore(rs.getDouble("health_chronically_ill_score"));
        model.setHealthImmunocompromisedScore(rs.getDouble("health_immunocompromisedl_score"));
        model.setHealthTerminallyIllScore(rs.getDouble("health_terminally_ill_score"));
        model.setHealthWithMedicalScore(rs.getDouble("health_with_medical_score"));

        // Clean water access scores (renamed occasionally to irregular)
        model.setCleanWaterYesScore(rs.getDouble("clean_water_access_yes_score"));
        model.setCleanWaterNoScore(rs.getDouble("clean_water_access_no_score"));
        model.setCleanWaterIrregularScore(rs.getDouble("clean_water_access_irregular_score"));

        // Sanitation scores
        model.setSanitationSafelyScore(rs.getDouble("sanitation_safely_score"));
        model.setSanitationSharedScore(rs.getDouble("sanitation_shared_score"));
        model.setSanitationUnimprovedScore(rs.getDouble("sanitation_unimproved_score"));
        model.setSanitationNoScore(rs.getDouble("sanitation_no_score"));

        // House construction scores
        model.setHouseConcreteScore(rs.getDouble("house_construction_concrete_score"));
        model.setHouseLightMaterialsScore(rs.getDouble("house_construction_light_materials_score"));
        model.setHouseSemiConcreteScore(rs.getDouble("house_construction_semi_concrete_score"));
        model.setHouseMakeshiftScore(rs.getDouble("house_construction_makeshirt_score"));

        // Ownership scores
        model.setOwnershipOwnedScore(rs.getDouble("ownership_owned_score"));
        model.setOwnershipOwnedWithoutScore(rs.getDouble("ownership_owned_without_score"));
        model.setOwnershipRentedScore(rs.getDouble("ownership_rented_score"));
        model.setOwnershipInformalScore(rs.getDouble("ownership_informal_score"));
        model.setOwnershipEvictedScore(rs.getDouble("ownership_evicted_score"));

        // Employment scores
        model.setEmploymentRegularScore(rs.getDouble("employment_regular_score"));
        model.setEmploymentSelfEmployedStableScore(rs.getDouble("employment_self_employed_stable_score"));
        model.setEmploymentSelfEmployedUnstableScore(rs.getDouble("employment_self_employed_unstable_score"));
        model.setEmploymentIrregularScore(rs.getDouble("employment_irregular_score"));
        model.setEmploymentUnemployedScore(rs.getDouble("employment_unemployed_score"));

        // Income scores (added income_upper_income_score)
        model.setIncomePoorScore(rs.getDouble("income_poor_score"));
        model.setIncomeLowIncomeScore(rs.getDouble("income_low_income_score"));
        model.setIncomeMiddleIncomeScore(rs.getDouble("income_middle_income_score"));
        model.setIncomeMiddleClassScore(rs.getDouble("income_middle_class_score"));
        model.setIncomeUpperMiddleClassScore(rs.getDouble("income_upper_middle_class_score"));
        model.setIncomeUpperIncomeScore(rs.getDouble("income_upper_income_score"));
        model.setIncomeRichScore(rs.getDouble("income_rich_score"));

        // Education scores
        model.setEducationNoFormalScore(rs.getDouble("education_no_formal_education_score"));
        model.setEducationElementaryScore(rs.getDouble("education_elementary_score"));
        model.setEducationHighschoolScore(rs.getDouble("education_highschool_score"));
        model.setEducationVocationalScore(rs.getDouble("education_vocational_score"));
        model.setEducationCollegeScore(rs.getDouble("education_college_score"));
        model.setEducationGraduatedScore(rs.getDouble("education_graduated_score"));

        // Digital access scores (renamed device_only to limited_access)
        model.setDigitalReliableScore(rs.getDouble("digital_reliable_score"));
        model.setDigitalIntermittentScore(rs.getDouble("digital_intermittent_score"));
        model.setDigitalLimitedAccessScore(rs.getDouble("digital_limited_access_score"));
        model.setDigitalNoDigitalScore(rs.getDouble("digital_no_digital_score"));

        // Damage scores
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
        model.setAgeScore(rs.getDouble("age_score"));
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
        model.setAgeScore(rs.getDouble("age_score"));
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
        model.setDisasterId(rs.getInt("disaster_id"));
        model.setHouseDamageSeverity(rs.getString("house_damage_severity"));
        return model;
    }

    @Override
    public boolean updateNullDisasterIdToSpecificDisaster(int beneficiaryId, int disasterId) {
        String sql = "UPDATE household_score SET disaster_id = ? " +
                "WHERE beneficiary_id = ? AND disaster_id IS NULL";

        try {
            conn = DBConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, disasterId);
            ps.setInt(2, beneficiaryId);

            int rowsAffected = ps.executeUpdate();
            ps.close();

            if (rowsAffected > 0) {
                System.out.println("✓ Updated NULL disaster_id to " + disasterId +
                        " for beneficiary " + beneficiaryId);
            }

            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating NULL disaster_id: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            closeConnection();
        }
    }

    // ==================== UTILITY METHODS ====================

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