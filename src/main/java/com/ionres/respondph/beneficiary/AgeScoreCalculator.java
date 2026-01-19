package com.ionres.respondph.beneficiary;


import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

/**
 * Utility class to calculate age and age-based vulnerability scores
 */
public class AgeScoreCalculator {

    /**
     * Calculate age from birthdate
     * @param birthdate Date string in format "yyyy-MM-dd"
     * @return age in years
     */
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

    /**
     * Calculate age score based on age
     * Higher age = higher vulnerability score
     * Score ranges from 0.0 to 1.0
     *
     * Scoring logic:
     * - 0-17 years: 0.3 (children/dependents)
     * - 18-59 years: 0.2 (working age, lower vulnerability)
     * - 60-69 years: 0.6 (senior citizens, moderate vulnerability)
     * - 70-79 years: 0.8 (elderly, high vulnerability)
     * - 80+ years: 1.0 (very elderly, highest vulnerability)
     *
     * @param age Age in years
     * @return vulnerability score (0.0 - 1.0)
     */
    public static double calculateAgeScore(int age) {
        if (age < 0) {
            return 0.0;
        } else if (age < 18) {
            // Children and minors - moderate vulnerability
            return 0.3;
        } else if (age < 60) {
            // Working age adults - lowest vulnerability
            return 0.2;
        } else if (age < 70) {
            // Senior citizens (60-69) - moderate-high vulnerability
            return 0.6;
        } else if (age < 80) {
            // Elderly (70-79) - high vulnerability
            return 0.8;
        } else {
            // Very elderly (80+) - highest vulnerability
            return 1.0;
        }
    }

    /**
     * Calculate age score directly from birthdate
     * @param birthdate Date string in format "yyyy-MM-dd"
     * @return vulnerability score (0.0 - 1.0)
     */
    public static double calculateAgeScoreFromBirthdate(String birthdate) {
        int age = calculateAge(birthdate);
        return calculateAgeScore(age);
    }

    /**
     * Get age category description
     * @param age Age in years
     * @return Category description
     */
    public static String getAgeCategory(int age) {
        if (age < 18) {
            return "Minor/Child";
        } else if (age < 60) {
            return "Working Age Adult";
        } else if (age < 70) {
            return "Senior Citizen";
        } else if (age < 80) {
            return "Elderly";
        } else {
            return "Very Elderly";
        }
    }
}