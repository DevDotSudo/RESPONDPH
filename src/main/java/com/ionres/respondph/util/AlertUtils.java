package com.ionres.respondph.util;

import javafx.scene.control.ButtonType;

public final class AlertUtils {
    private AlertUtils() {}

    public static void showError(String title, String message) {
        AlertDialogManager.showError(title, message);
    }

    public static void showInfo(String title, String message) {
        AlertDialogManager.showInfo(title, message);
    }

    public static void showWarning(String title, String message) {
        AlertDialogManager.showWarning(title, message);
    }

    public static boolean showConfirmation(String title, String message) {
        return AlertDialogManager.showConfirmation(title, message, ButtonType.OK, ButtonType.CANCEL);
    }
}
