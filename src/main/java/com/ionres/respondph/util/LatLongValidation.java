package com.ionres.respondph.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;

import java.util.function.UnaryOperator;

public class LatLongValidation {

    public static void setNumericCoordinateFilter(TextField textField, double maxValue, String fieldName) {
        String coordinatePattern = "-?\\d*\\.?\\d*";

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();

            if (newText.matches(coordinatePattern)) {
                if (newText.isEmpty() || newText.equals("-") || newText.equals("-.") || newText.equals(".")) {
                    return change;
                }

                try {
                    double value = Double.parseDouble(newText);
                    if (Math.abs(value) <= maxValue) {
                        return change;
                    } else {
                        String warning = String.format("%s must be between -%.1f and +%.1f", fieldName, maxValue, maxValue);
                        textField.setStyle("-fx-border-color: orange; -fx-border-width: 1px;");

                        if (!textField.getTooltip().getText().contains("Invalid")) {
                            textField.setTooltip(new Tooltip(warning));
                        }
                        return null;
                    }
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        };

        textField.setTextFormatter(new TextFormatter<>(filter));

        String rangeMsg = String.format("Enter %s value (-%.1f to +%.1f)", fieldName.toLowerCase(), maxValue, maxValue);
        textField.setTooltip(new Tooltip(rangeMsg));

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (textField.getStyle().contains("orange")) {
                textField.setStyle("");
                textField.setTooltip(new Tooltip(rangeMsg));
            }
        });
    }

    public static void setNumericNumberFilter(TextField textField) {
        String phoneNumberPattern = "[-+()\\d\\s]*";

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();

            if (newText.matches(phoneNumberPattern)) {
                return change;
            }
            return null;
        };

        textField.setTextFormatter(new TextFormatter<>(filter));

        textField.setTooltip(new Tooltip("Enter phone number (digits, +, -, (, ), spaces allowed)"));
    }

}
