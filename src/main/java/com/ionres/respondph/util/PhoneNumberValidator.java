package com.ionres.respondph.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;


public class PhoneNumberValidator {


    public static boolean isValid(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        String cleaned = phoneNumber.replaceAll("[\\s-]", "");

        if (cleaned.startsWith("+63")) {
            if (cleaned.length() == 13) {
                String afterPrefix = cleaned.substring(3);
                return afterPrefix.matches("9\\d{9}");
            }
            return false;
        }

        if (cleaned.startsWith("09")) {
            return cleaned.matches("09\\d{9}");
        }

        return false;
    }


    public static String getErrorMessage(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return "Mobile number is required";
        }

        String cleaned = phoneNumber.replaceAll("[\\s-]", "");

        if (cleaned.startsWith("+63")) {
            if (cleaned.length() != 13) {
                return "Phone number starting with +63 must be 13 digits total (e.g., +639123456789)";
            }
            if (!cleaned.substring(3).matches("9\\d{9}")) {
                return "After +63, number must start with 9 (e.g., +639123456789)";
            }
        } else if (cleaned.startsWith("09")) {
            if (cleaned.length() != 11) {
                return "Phone number starting with 09 must be 11 digits total (e.g., 09123456789)";
            }
        } else {
            return "Mobile number must start with +63 or 09\nExamples:\n  +639123456789\n  09123456789";
        }

        return "Invalid mobile number format";
    }


    public static String normalize(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        return phoneNumber.replaceAll("[\\s-]", "");
    }


    public static String toInternationalFormat(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }

        String cleaned = normalize(phoneNumber);

        if (cleaned.startsWith("+63")) {
            return cleaned;
        }

        if (cleaned.startsWith("09") && cleaned.length() == 11) {
            return "+63" + cleaned.substring(1);
        }

        return null;
    }


    public static String toLocalFormat(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }

        String cleaned = normalize(phoneNumber);

        if (cleaned.startsWith("09")) {
            return cleaned;
        }

        if (cleaned.startsWith("+63") && cleaned.length() == 13) {
            return "0" + cleaned.substring(3);
        }

        return null;
    }

    public static void setupInputFilter(TextField textField) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();

            if (newText.isEmpty()) return change;

            // Allow only digits and + at the start
            if (!newText.matches("[+]?\\d*")) return null;

            // Determine max length based on prefix
            int maxLength;
            if (newText.startsWith("+63")) {
                maxLength = 13; // +63XXXXXXXXXX
            } else if (newText.startsWith("63")) {
                maxLength = 12; // 63XXXXXXXXXX
            } else if (newText.startsWith("0")) {
                maxLength = 11; // 0XXXXXXXXXX  (local format)
            } else {
                maxLength = 11; // default
            }

            if (newText.length() > maxLength) return null;

            return change;
        };

        textField.setTextFormatter(new TextFormatter<>(filter));
    }
}