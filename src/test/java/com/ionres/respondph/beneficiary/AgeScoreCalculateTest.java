package com.ionres.respondph.beneficiary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AgeScoreCalculate")
class AgeScoreCalculateTest {

    @Nested
    @DisplayName("calculateAge()")
    class CalculateAge {

        @Test
        @DisplayName("calculates correct age for adult born 30 years ago")
        void adultAge() {
            LocalDate thirtyYearsAgo = LocalDate.now().minusYears(30);
            int age = AgeScoreCalculate.calculateAge(thirtyYearsAgo.toString());
            assertEquals(30, age);
        }

        @Test
        @DisplayName("calculates correct age for child born 5 years ago")
        void childAge() {
            LocalDate fiveYearsAgo = LocalDate.now().minusYears(5);
            int age = AgeScoreCalculate.calculateAge(fiveYearsAgo.toString());
            assertEquals(5, age);
        }

        @Test
        @DisplayName("calculates correct age for elderly born 70 years ago")
        void elderlyAge() {
            LocalDate seventyYearsAgo = LocalDate.now().minusYears(70);
            int age = AgeScoreCalculate.calculateAge(seventyYearsAgo.toString());
            assertEquals(70, age);
        }

        @Test
        @DisplayName("calculates correct age for infant born today")
        void infantAge() {
            int age = AgeScoreCalculate.calculateAge(LocalDate.now().toString());
            assertEquals(0, age);
        }

        @Test
        @DisplayName("returns 0 for invalid date string")
        void invalidDate() {
            int age = AgeScoreCalculate.calculateAge("not-a-date");
            assertEquals(0, age);
        }

        @Test
        @DisplayName("returns 0 for null date")
        void nullDate() {
            int age = AgeScoreCalculate.calculateAge(null);
            assertEquals(0, age);
        }

        @Test
        @DisplayName("handles birthday not yet reached this year")
        void birthdayNotYetReached() {
            // Born exactly 25 years and 1 day from now (birthday hasn't passed)
            LocalDate futureDay = LocalDate.now().plusDays(1).minusYears(25);
            int age = AgeScoreCalculate.calculateAge(futureDay.toString());
            assertEquals(24, age);
        }

        @Test
        @DisplayName("handles birthday already passed this year")
        void birthdayAlreadyPassed() {
            // Born exactly 25 years ago minus 1 day (birthday has passed)
            LocalDate pastDay = LocalDate.now().minusDays(1).minusYears(25);
            int age = AgeScoreCalculate.calculateAge(pastDay.toString());
            assertEquals(25, age);
        }
    }
}

