package com.ionres.respondph.beneficiary;

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
        if (age < 0) {
            return 0.0;
        } else if (age <= 4) {
            return 1.0;
        } else if (age <= 14) {
            return 0.7;
        } else if (age <= 24) {
            return 0.3;
        } else if (age <= 54) {
            return 0.1;
        } else if (age <= 64) {
            return 0.5;
        } else {
            return 1.0;
        }
    }


    public static double calculateAgeScoreFromBirthdate(String birthdate) {
        int age = calculateAge(birthdate);
        return calculateAgeScore(age);
    }



}