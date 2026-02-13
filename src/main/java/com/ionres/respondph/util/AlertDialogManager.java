package com.ionres.respondph.util;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
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

    public static void styleAlert(Alert alert, AlertType type) {
        if (alert == null) {
            return;
        }
        AlertType resolvedType = type != null ? type : AlertType.INFO;
        DialogPane dialogPane = alert.getDialogPane();
        applyDialogPaneStyle(dialogPane, resolvedType);
        applyWindowStyle(dialogPane);
        makeDraggable(dialogPane);
    }

    public static void styleDialog(Dialog<?> dialog, AlertType type) {
        if (dialog == null) {
            return;
        }
        AlertType resolvedType = type != null ? type : AlertType.INFO;
        DialogPane dialogPane = dialog.getDialogPane();
        applyDialogPaneStyle(dialogPane, resolvedType);
        applyWindowStyle(dialogPane);
        makeDraggable(dialogPane);
    }

    private static Alert createAlert(AlertType type, String title, String message, ButtonType... buttons) {
        Alert alert = new Alert(getJavaFXAlertType(type), "", buttons);
        alert.setTitle("");
        alert.setHeaderText(title);
        alert.setContentText(message);

        styleAlert(alert, type);

        return alert;
    }

    private static void applyDialogPaneStyle(DialogPane dialogPane, AlertType type) {
        if (dialogPane == null) {
            return;
        }

        dialogPane.getStyleClass().clear();
        dialogPane.getStyleClass().addAll("dialog-pane", "custom-alert", type.getStyleClass());

        dialogPane.getStylesheets().clear();
        String cssPath = AlertDialogManager.class.getResource(CSS_FILE).toExternalForm();
        dialogPane.getStylesheets().add(cssPath);

        setIcon(dialogPane, type);
        customizeButtons(dialogPane, type);
    }

    private static void applyWindowStyle(DialogPane dialogPane) {
        if (dialogPane == null) {
            return;
        }

        if (dialogPane.getScene() != null) {
            applyWindowStyleNow(dialogPane);
            return;
        }

        dialogPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                applyWindowStyleNow(dialogPane);
            }
        });
    }

    private static void applyWindowStyleNow(DialogPane dialogPane) {
        if (dialogPane.getScene() == null) {
            return;
        }

        dialogPane.getScene().setFill(Color.TRANSPARENT);

        if (dialogPane.getScene().getWindow() instanceof Stage) {
            Stage stage = (Stage) dialogPane.getScene().getWindow();
            try {
                stage.initStyle(StageStyle.TRANSPARENT);
            } catch (IllegalStateException ignored) {
                // Stage style can only be initialized once before showing.
            }
            stage.centerOnScreen();
        }
    }

    private static void setIcon(DialogPane dialogPane, AlertType type) {
        FontAwesomeIconView icon = new FontAwesomeIconView(type.icon);

        // Use orange for info and confirmation to match theme
        String iconColor;
        switch (type) {
            case SUCCESS:
                iconColor = "#10b981";
                break;
            case ERROR:
                iconColor = "#ef4444";
                break;
            case WARNING:
                iconColor = "#f59e0b";
                break;
            case INFO:
            case CONFIRMATION:
            default:
                iconColor = "#f97316"; // Orange accent
                break;
        }

        icon.setFill(Color.web(iconColor));
        icon.setSize("32px"); // Slightly smaller icon
        icon.getStyleClass().add("custom-alert-icon");
        dialogPane.setGraphic(icon);
    }

    private static void customizeButtons(DialogPane dialogPane, AlertType type) {
        boolean isConfirmation = type == AlertType.CONFIRMATION;

        for (ButtonType buttonType : dialogPane.getButtonTypes()) {
            Button button = (Button) dialogPane.lookupButton(buttonType);
            if (button == null) continue;

            button.getStyleClass().clear();
            button.getStyleClass().add("alert-button");

            if (buttonType == ButtonType.OK || buttonType == ButtonType.YES) {
                button.getStyleClass().add("primary-button");
                if (!isConfirmation) {
                    button.setGraphic(createButtonIcon(getPrimaryIcon(type), "#ffffff"));
                } else {
                    button.setGraphic(null);
                }
                button.setText(buttonType == ButtonType.OK ? "OK" : "YES");
            } else if (buttonType == ButtonType.CANCEL || buttonType == ButtonType.NO) {
                // Use cancel-button for error/warning, secondary-button for others
                if (type == AlertType.ERROR || type == AlertType.WARNING) {
                    button.getStyleClass().add("cancel-button");
                    if (!isConfirmation) {
                        button.setGraphic(createButtonIcon(FontAwesomeIcon.TIMES, "#ef4444"));
                    } else {
                        button.setGraphic(null);
                    }
                } else {
                    button.getStyleClass().add("secondary-button");
                    if (!isConfirmation) {
                        button.setGraphic(createButtonIcon(FontAwesomeIcon.TIMES, "rgba(148, 163, 184, 0.85)"));
                    } else {
                        button.setGraphic(null);
                    }
                }
                button.setText(buttonType == ButtonType.CANCEL ? "CANCEL" : "NO");
            }

            if (isConfirmation) {
                button.setContentDisplay(javafx.scene.control.ContentDisplay.TEXT_ONLY);
            } else {
                button.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
                button.setGraphicTextGap(6);
            }

            button.setAlignment(javafx.geometry.Pos.CENTER);
            button.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            button.setMinHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
            button.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
            button.setMinWidth(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
            button.setPrefWidth(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
        }
    }

    private static FontAwesomeIconView createButtonIcon(FontAwesomeIcon icon, String color) {
        FontAwesomeIconView iconView = new FontAwesomeIconView(icon);
        iconView.setSize("12px"); // Smaller icon
        iconView.setGlyphStyle("-fx-font-family: 'FontAwesome'");
        iconView.setFill(Color.web(color));
        return iconView;
    }

    private static FontAwesomeIcon getPrimaryIcon(AlertType type) {
        switch (type) {
            case SUCCESS:
                return FontAwesomeIcon.CHECK;
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

    private static void makeDraggable(DialogPane dialogPane) {
        if (dialogPane == null) {
            return;
        }

        if (dialogPane.getScene() == null || !(dialogPane.getScene().getWindow() instanceof Stage)) {
            dialogPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && newScene.getWindow() instanceof Stage) {
                    attachDraggableBehavior(dialogPane, (Stage) newScene.getWindow());
                }
            });
            return;
        }

        attachDraggableBehavior(dialogPane, (Stage) dialogPane.getScene().getWindow());
    }

    private static void attachDraggableBehavior(DialogPane dialogPane, Stage stage) {
        if (dialogPane == null || stage == null) {
            return;
        }

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
        INFO("info-alert", FontAwesomeIcon.INFO_CIRCLE, "#f97316"),
        CONFIRMATION("confirmation-alert", FontAwesomeIcon.QUESTION_CIRCLE, "#f97316");

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
