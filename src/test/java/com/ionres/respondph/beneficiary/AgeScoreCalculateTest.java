package com.ionres.respondph.beneficiary;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the age-based vulnerability scoring used in
 * the Household Multivariate Indicator Scoring (HMIS) system.
 */
@DisplayName("AgeScoreCalculate — Age-Based Vulnerability Scoring")
class AgeScoreCalculateTest {

    // ═══════════════════════════════════════════════════════════════════════
    // calculateAgeScore — Age bracket mapping
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("calculateAgeScore — Score Brackets")
    class AgeScoreBracketTests {

        @ParameterizedTest
        @CsvSource({"0, 1.0", "1, 1.0", "2, 1.0", "3, 1.0", "4, 1.0"})
        @DisplayName("Age 0–4 (Infant/Toddler) → score 1.0")
        void infantToddler(int age, double expected) {
            assertEquals(expected, AgeScoreCalculate.calculateAgeScore(age), 1e-10);
        }

        @ParameterizedTest
        @CsvSource({"5, 0.7", "7, 0.7", "10, 0.7", "14, 0.7"})
        @DisplayName("Age 5–14 (Child) → score 0.7")
        void child(int age, double expected) {
            assertEquals(expected, AgeScoreCalculate.calculateAgeScore(age), 1e-10);
        }

        @ParameterizedTest
        @CsvSource({"15, 0.3", "17, 0.3", "20, 0.3", "24, 0.3"})
        @DisplayName("Age 15–24 (Youth/Adolescent) → score 0.3")
        void youth(int age, double expected) {
            assertEquals(expected, AgeScoreCalculate.calculateAgeScore(age), 1e-10);
        }

        @ParameterizedTest
        @CsvSource({"25, 0.1", "30, 0.1", "40, 0.1", "54, 0.1"})
        @DisplayName("Age 25–54 (Working Adult) → score 0.1")
        void workingAdult(int age, double expected) {
            assertEquals(expected, AgeScoreCalculate.calculateAgeScore(age), 1e-10);
        }

        @ParameterizedTest
        @CsvSource({"55, 0.5", "58, 0.5", "60, 0.5", "64, 0.5"})
        @DisplayName("Age 55–64 (Pre-Senior) → score 0.5")
        void preSenior(int age, double expected) {
            assertEquals(expected, AgeScoreCalculate.calculateAgeScore(age), 1e-10);
        }

        @ParameterizedTest
        @CsvSource({"65, 1.0", "70, 1.0", "80, 1.0", "100, 1.0"})
        @DisplayName("Age 65+ (Senior/Elderly) → score 1.0")
        void elderly(int age, double expected) {
            assertEquals(expected, AgeScoreCalculate.calculateAgeScore(age), 1e-10);
        }

        @Test
        @DisplayName("Negative age → score 0.0")
        void negativeAge() {
            assertEquals(0.0, AgeScoreCalculate.calculateAgeScore(-1), 1e-10);
            assertEquals(0.0, AgeScoreCalculate.calculateAgeScore(-100), 1e-10);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // calculateAge — Birthdate to age conversion
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("calculateAge — Birthdate Parsing")
    class CalculateAgeTests {

        @Test
        @DisplayName("Valid birthdate returns correct age")
        void validBirthdate() {
            // Person born exactly 30 years ago
            String birthdate = LocalDate.now().minusYears(30).toString();
            int age = AgeScoreCalculate.calculateAge(birthdate);
            assertEquals(30, age);
        }

        @Test
        @DisplayName("Newborn (born today) → age 0")
        void newborn() {
            String birthdate = LocalDate.now().toString();
            int age = AgeScoreCalculate.calculateAge(birthdate);
            assertEquals(0, age);
        }

        @Test
        @DisplayName("Elderly person (born 80 years ago)")
        void elderly() {
            String birthdate = LocalDate.now().minusYears(80).toString();
            int age = AgeScoreCalculate.calculateAge(birthdate);
            assertEquals(80, age);
        }

        @Test
        @DisplayName("Invalid date format returns 0")
        void invalidFormat() {
            int age = AgeScoreCalculate.calculateAge("not-a-date");
            assertEquals(0, age);
        }

        @Test
        @DisplayName("Null birthdate returns 0")
        void nullBirthdate() {
            int age = AgeScoreCalculate.calculateAge(null);
            assertEquals(0, age);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // calculateAgeScoreFromBirthdate — End-to-end
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("calculateAgeScoreFromBirthdate — End-to-End")
    class EndToEndTests {

        @Test
        @DisplayName("2-year-old child → score 1.0")
        void toddlerScore() {
            String birthdate = LocalDate.now().minusYears(2).toString();
            double score = AgeScoreCalculate.calculateAgeScoreFromBirthdate(birthdate);
            assertEquals(1.0, score, 1e-10);
        }

        @Test
        @DisplayName("10-year-old child → score 0.7")
        void childScore() {
            String birthdate = LocalDate.now().minusYears(10).toString();
            double score = AgeScoreCalculate.calculateAgeScoreFromBirthdate(birthdate);
            assertEquals(0.7, score, 1e-10);
        }

        @Test
        @DisplayName("30-year-old adult → score 0.1")
        void adultScore() {
            String birthdate = LocalDate.now().minusYears(30).toString();
            double score = AgeScoreCalculate.calculateAgeScoreFromBirthdate(birthdate);
            assertEquals(0.1, score, 1e-10);
        }

        @Test
        @DisplayName("70-year-old elderly → score 1.0")
        void elderlyScore() {
            String birthdate = LocalDate.now().minusYears(70).toString();
            double score = AgeScoreCalculate.calculateAgeScoreFromBirthdate(birthdate);
            assertEquals(1.0, score, 1e-10);
        }

        @Test
        @DisplayName("Invalid birthdate → score 0.0 (age=0 → infant bracket 1.0)")
        void invalidBirthdate() {
            // calculateAge returns 0 for invalid, and age 0 maps to score 1.0
            double score = AgeScoreCalculate.calculateAgeScoreFromBirthdate("invalid");
            assertEquals(1.0, score, 1e-10);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Boundary values (exact bracket boundaries)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Boundary Values — Exact Bracket Transitions")
    class BoundaryTests {

        @Test
        @DisplayName("Age 4→5 transition: 1.0 → 0.7")
        void boundary4to5() {
            assertEquals(1.0, AgeScoreCalculate.calculateAgeScore(4), 1e-10);
            assertEquals(0.7, AgeScoreCalculate.calculateAgeScore(5), 1e-10);
        }

        @Test
        @DisplayName("Age 14→15 transition: 0.7 → 0.3")
        void boundary14to15() {
            assertEquals(0.7, AgeScoreCalculate.calculateAgeScore(14), 1e-10);
            assertEquals(0.3, AgeScoreCalculate.calculateAgeScore(15), 1e-10);
        }

        @Test
        @DisplayName("Age 24→25 transition: 0.3 → 0.1")
        void boundary24to25() {
            assertEquals(0.3, AgeScoreCalculate.calculateAgeScore(24), 1e-10);
            assertEquals(0.1, AgeScoreCalculate.calculateAgeScore(25), 1e-10);
        }

        @Test
        @DisplayName("Age 54→55 transition: 0.1 → 0.5")
        void boundary54to55() {
            assertEquals(0.1, AgeScoreCalculate.calculateAgeScore(54), 1e-10);
            assertEquals(0.5, AgeScoreCalculate.calculateAgeScore(55), 1e-10);
        }

        @Test
        @DisplayName("Age 64→65 transition: 0.5 → 1.0")
        void boundary64to65() {
            assertEquals(0.5, AgeScoreCalculate.calculateAgeScore(64), 1e-10);
            assertEquals(1.0, AgeScoreCalculate.calculateAgeScore(65), 1e-10);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Score range validation
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("All age scores are in [0.0, 1.0] range")
    void allScoresInRange() {
        for (int age = -5; age <= 120; age++) {
            double score = AgeScoreCalculate.calculateAgeScore(age);
            assertTrue(score >= 0.0 && score <= 1.0,
                    "Score " + score + " out of range for age " + age);
        }
    }
}

