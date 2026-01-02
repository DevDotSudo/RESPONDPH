package com.ionres.respondph.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

public class CustomAlertDialog {
    public void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showWarning(String message) {
        javax.swing.JOptionPane.showMessageDialog(
                null,
                message,
                "Validation Error",
                javax.swing.JOptionPane.WARNING_MESSAGE
        );
    }

//    public static boolean showConfirmationDialog(String title, String message) {
//        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//        alert.setTitle(title);
//        alert.setHeaderText(null);
//        alert.setContentText(message);
//
//        customizeDialog(alert.getDialogPane());
//
//        ButtonType yesButton =
//    }
//
//    private static void customizeDialog(DialogPane pane) {
//        pane.getStylesheets().add(CustomAlertDialog.class.getResource("customalertdialog.css").toExternalForm());
//        pane.getStyleClass().add("dialog-pane");
//        pane.lookupButton(ButtonType.OK).getStyleClass().add("ok-button");
//        pane.lookupButton(ButtonType.YES).getStyleClass().add("yes-button");
//        pane.lookupButton(ButtonType.NO).getStyleClass().add("no-button");
////
//    }
}
