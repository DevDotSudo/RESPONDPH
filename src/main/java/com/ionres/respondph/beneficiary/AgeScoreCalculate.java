package com.ionres.respondph.beneficiary;

import com.ionres.respondph.database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.Period;

public class AgeScoreCalculate {

    public static int calculateAge(String birthdate) {
        try {
            LocalDate birthDate = LocalDate.parse(birthdate);
            LocalDate currentDate = LocalDate.now();
            return Period.between(birthDate, currentDate).getYears();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static double calculateAgeScore(int age) {
        String sql = "SELECT age_range_0_to_4, age_range_5_to_14, age_range_15_to_24, " +
                "age_range_25_to_54, age_range_55_to_64, age_range_65_and_above " +
                "FROM vulnerability_indicator_score LIMIT 1";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                if (age <= 4) {
                    return rs.getDouble("age_range_0_to_4");
                } else if (age <= 14) {
                    return rs.getDouble("age_range_5_to_14");
                } else if (age <= 24) {
                    return rs.getDouble("age_range_15_to_24");
                } else if (age <= 54) {
                    return rs.getDouble("age_range_25_to_54");
                } else if (age <= 64) {
                    return rs.getDouble("age_range_55_to_64");
                } else {
                    return rs.getDouble("age_range_65_and_above");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback to 0.0 if no scores are configured yet
        return 0.0;
    }

    public static double calculateAgeScoreFromBirthdate(String birthdate) {
        int age = calculateAge(birthdate);
        return calculateAgeScore(age);
    }
}