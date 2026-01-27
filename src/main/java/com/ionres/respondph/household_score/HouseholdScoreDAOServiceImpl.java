package com.ionres.respondph.household_score;

import com.ionres.respondph.database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HouseholdScoreDAOServiceImpl implements HouseholdScoreDAO {
    private static final Logger LOGGER = Logger.getLogger(HouseholdScoreDAOServiceImpl.class.getName());
    private final DBConnection dbConnection;

    public HouseholdScoreDAOServiceImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public boolean saveHouseholdScore(int beneficiaryId, double genderScore, double maritalStatusScore,
                                      double soloParentScore, double disabilityScore, double healthConditionScore,
                                      double cleanWaterScore, double sanitationScore, double houseConstructionScore,
                                      double ownershipScore, double employmentScore, double monthlyIncomeScore,
                                      double educationScore, double digitalAccessScore) {

        String deleteSql = "DELETE FROM household_score WHERE beneficiary_id = ?";

        String insertSql = "INSERT INTO household_score " +
                "(beneficiary_id, gender_score, marital_status_score, solo_parent_score, " +
                "disability_score, health_condition_score, access_to_clean_water_score, " +
                "sanitation_facilities_score, house_construction_type_score, ownership_score, " +
                "employment_status_score, monthly_income_score, education_level_score, " +
                "digital_access_score, creation_date, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), 'Auto-calculated')";

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
                deletePs.setInt(1, beneficiaryId);
                deletePs.executeUpdate();
            }

            try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                insertPs.setInt(1, beneficiaryId);
                insertPs.setDouble(2, genderScore);
                insertPs.setDouble(3, maritalStatusScore);
                insertPs.setDouble(4, soloParentScore);
                insertPs.setDouble(5, disabilityScore);
                insertPs.setDouble(6, healthConditionScore);
                insertPs.setDouble(7, cleanWaterScore);
                insertPs.setDouble(8, sanitationScore);
                insertPs.setDouble(9, houseConstructionScore);
                insertPs.setDouble(10, ownershipScore);
                insertPs.setDouble(11, employmentScore);
                insertPs.setDouble(12, monthlyIncomeScore);
                insertPs.setDouble(13, educationScore);
                insertPs.setDouble(14, digitalAccessScore);

                int rowsAffected = insertPs.executeUpdate();
                conn.commit();
                return rowsAffected > 0;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving household score", e);
            return false;
        }
    }

}
