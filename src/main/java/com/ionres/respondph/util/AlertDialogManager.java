package com.ionres.respondph.util;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AlertDialogManager {

    private static final String CSS_FILE = "/styles/util/alertdialogmanager.css";

    private AlertDialogManager() {}

    public static void showSuccess(String title, String message) {
        showAlert(AlertType.SUCCESS, title, message, ButtonType.OK);
    }

    public static void showError(String title, String message) {
        showAlert(AlertType.ERROR, title, message, ButtonType.OK);
    }

    public static void showWarning(String title, String message) {
        showAlert(AlertType.WARNING, title, message, ButtonType.OK);
    }

    public static void showInfo(String title, String message) {
        showAlert(AlertType.INFO, title, message, ButtonType.OK);
    }

    public static boolean showConfirmation(String title, String message) {
        Alert alert = createAlert(AlertType.CONFIRMATION, title, message, ButtonType.YES, ButtonType.NO);
        return alert.showAndWait().filter(response -> response == ButtonType.YES).isPresent();
    }

    public static boolean showConfirmation(String title, String message, ButtonType confirmButton, ButtonType cancelButton) {
        Alert alert = createAlert(AlertType.CONFIRMATION, title, message, confirmButton, cancelButton);
        return alert.showAndWait().filter(response -> response == confirmButton).isPresent();
    }

    private static void showAlert(AlertType type, String title, String message, ButtonType... buttons) {
        createAlert(type, title, message, buttons).showAndWait();
    }

    private static Alert createAlert(AlertType type, String title, String message, ButtonType... buttons) {
        Alert alert = new Alert(getJavaFXAlertType(type), "", buttons);
        alert.setTitle("");
        alert.setHeaderText(title);
        alert.setContentText(message);

        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.initStyle(StageStyle.TRANSPARENT);
        alert.getDialogPane().getScene().setFill(Color.TRANSPARENT);

        styleAlert(alert, type);
        makeDraggable(alert);

        return alert;
    }

    private static void styleAlert(Alert alert, AlertType type) {
        DialogPane dialogPane = alert.getDialogPane();

        dialogPane.getStyleClass().clear();
        dialogPane.getStyleClass().addAll("dialog-pane", "custom-alert", type.getStyleClass());

        dialogPane.getStylesheets().clear();
        String cssPath = AlertDialogManager.class.getResource(CSS_FILE).toExternalForm();
        dialogPane.getStylesheets().add(cssPath);

        setIcon(dialogPane, type);
        customizeButtons(dialogPane, type);

        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.centerOnScreen();
    }

    private static void setIcon(DialogPane dialogPane, AlertType type) {
        FontAwesomeIconView icon = new FontAwesomeIconView(type.icon);
        icon.setFill(Color.web(type.color));
        icon.setSize("32px");
        icon.getStyleClass().add("custom-alert-icon");
        dialogPane.setGraphic(icon);
    }

    private static void customizeButtons(DialogPane dialogPane, AlertType type) {
        for (ButtonType buttonType : dialogPane.getButtonTypes()) {
            Button button = (Button) dialogPane.lookupButton(buttonType);
            if (button == null) continue;

            button.getStyleClass().clear();
            button.getStyleClass().add("alert-button");

            if (buttonType == ButtonType.OK || buttonType == ButtonType.YES) {
                button.getStyleClass().add("primary-button");
                button.setGraphic(createButtonIcon(getPrimaryIcon(type), "#ffffff"));
                button.setText(buttonType == ButtonType.OK ? "OK" : "YES");
            } else if (buttonType == ButtonType.CANCEL || buttonType == ButtonType.NO) {
                button.getStyleClass().add("secondary-button");
                button.setGraphic(createButtonIcon(FontAwesomeIcon.TIMES, "#cbd5e1"));
                button.setText(buttonType == ButtonType.CANCEL ? "CANCEL" : "NO");
            }

            button.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
            button.setGraphicTextGap(6);
        }
    }

    private static FontAwesomeIconView createButtonIcon(FontAwesomeIcon icon, String color) {
        FontAwesomeIconView iconView = new FontAwesomeIconView(icon);
        iconView.setSize("12px");
        iconView.setFill(Color.web(color));
        return iconView;
    }

    private static FontAwesomeIcon getPrimaryIcon(AlertType type) {
        switch (type) {
            case SUCCESS:
            case CONFIRMATION:
                return FontAwesomeIcon.CHECK;
            case ERROR:
                return FontAwesomeIcon.EXCLAMATION_CIRCLE;
            case WARNING:
                return FontAwesomeIcon.EXCLAMATION_TRIANGLE;
            case INFO:
            default:
                return FontAwesomeIcon.INFO_CIRCLE;
        }
    }

    private static void makeDraggable(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        Stage stage = (Stage) dialogPane.getScene().getWindow();

        final double[] offset = new double[2];

        dialogPane.setOnMousePressed(event -> {
            offset[0] = event.getSceneX();
            offset[1] = event.getSceneY();
        });

        dialogPane.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - offset[0]);
            stage.setY(event.getScreenY() - offset[1]);
        });

        for (ButtonType buttonType : dialogPane.getButtonTypes()) {
            Button button = (Button) dialogPane.lookupButton(buttonType);
            if (button != null) {
                button.setOnMousePressed(event -> event.consume());
                button.setOnMouseDragged(event -> event.consume());
            }
        }
    }

    private static Alert.AlertType getJavaFXAlertType(AlertType type) {
        switch (type) {
            case ERROR:
                return Alert.AlertType.ERROR;
            case WARNING:
                return Alert.AlertType.WARNING;
            case CONFIRMATION:
                return Alert.AlertType.CONFIRMATION;
            case SUCCESS:
            case INFO:
            default:
                return Alert.AlertType.INFORMATION;
        }
    }

    public enum AlertType {
        SUCCESS("success-alert", FontAwesomeIcon.CHECK_CIRCLE, "#10b981"),
        ERROR("error-alert", FontAwesomeIcon.EXCLAMATION_CIRCLE, "#ef4444"),
        WARNING("warning-alert", FontAwesomeIcon.EXCLAMATION_TRIANGLE, "#f59e0b"),
        INFO("info-alert", FontAwesomeIcon.INFO_CIRCLE, "#3b82f6"),
        CONFIRMATION("confirmation-alert", FontAwesomeIcon.QUESTION_CIRCLE, "#3b82f6");

        private final String styleClass;
        private final FontAwesomeIcon icon;
        private final String color;

        AlertType(String styleClass, FontAwesomeIcon icon, String color) {
            this.styleClass = styleClass;
            this.icon = icon;
            this.color = color;
        }

        public String getStyleClass() {
            return styleClass;
        }
    }
}