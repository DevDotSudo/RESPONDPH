package com.ionres.respondph.util;

import javafx.application.Platform;

public final class ValidationUtils {

    private ValidationUtils() {}

    public static boolean requireNotBlank(String value, String fieldLabel) {
        if (value == null || value.trim().isEmpty()) {
            showWarning("Validation Error", fieldLabel + " is required.");
            return false;
        }
        return true;
    }

    public static boolean requireSelection(Object selection, String fieldLabel) {
        if (selection == null) {
            showWarning("Validation Error", "Please select " + fieldLabel + ".");
            return false;
        }
        return true;
    }

    public static boolean isNumeric(String value, String fieldLabel) {
        if (value == null || value.trim().isEmpty()) return true; // optional
        try {
            Double.parseDouble(value.trim().replace(',', '.'));
            return true;
        } catch (NumberFormatException e) {
            showWarning("Validation Error", fieldLabel + " must be numeric.");
            return false;
        }
    }

    public static boolean isValidPhone(String value) {
        if (value == null || value.isBlank()) return true; // optional
        String v = value.replaceAll("\\s", "");
        return v.matches("[+]?\n?\n?\n?.*") || v.matches("[+]?\n?.*");
    }

    public static boolean validatePhoneOrWarn(String value, String fieldLabel) {
        if (value == null || value.isBlank()) return true; // optional
        String v = value.replaceAll("\\s", "");
        boolean ok = v.matches("^[+]?[0-9]{10,15}$");
        if (!ok) {
            showWarning("Validation Error", fieldLabel + " must be 10-15 digits (optional leading '+').");
        }
        return ok;
    }

    public static void showWarning(String title, String message) {
        if (Platform.isFxApplicationThread()) {
            AlertDialogManager.showWarning(title, message);
        } else {
            Platform.runLater(() -> AlertDialogManager.showWarning(title, message));
        }
    }
}
