package com.ionres.respondph.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class PdfProgressRunner {


    @FunctionalInterface
    public interface PdfProgressCallback {
        void update(double pct, String msg);
    }

    @FunctionalInterface
    public interface PdfTask {
        void execute(PdfProgressCallback progress) throws Exception;
    }

    @FunctionalInterface
    public interface OnSuccess {
        void run();
    }


    public static void run(Stage ownerStage,
                           PdfTask task,
                           Runnable onSuccess,
                           java.util.function.Consumer<String> onFail) {

        ProgressBar pdfProgressBar   = new ProgressBar(0);
        Label       pdfProgressLabel = new Label("Preparing PDF...");
        Stage       pdfProgressStage = buildProgressStage(
                ownerStage, pdfProgressBar, pdfProgressLabel);

        pdfProgressStage.show();

        Task<Void> pdfTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Starting...");
                updateProgress(5, 100);

                task.execute((pct, msg) -> {
                    updateProgress((long) Math.round(pct), 100L);
                    updateMessage(msg);
                });

                updateMessage("Done!");
                updateProgress(100, 100);
                return null;
            }
        };

        pdfProgressLabel.textProperty().bind(pdfTask.messageProperty());
        pdfProgressBar.progressProperty().bind(pdfTask.progressProperty());

        ExecutorService executor = Executors.newSingleThreadExecutor();

        pdfTask.setOnSucceeded(e -> {
            shutDown(executor);
            pdfProgressStage.close();
            if (onSuccess != null) onSuccess.run();
        });

        pdfTask.setOnFailed(e -> {
            shutDown(executor);
            pdfProgressStage.close();
            Throwable ex = pdfTask.getException();
            if (ex != null) ex.printStackTrace();
            if (onFail != null)
                onFail.accept(ex != null ? ex.getMessage() : "Unknown error");
        });

        executor.submit(pdfTask);
    }


    private static Stage buildProgressStage(Stage ownerStage,
                                            ProgressBar pdfProgressBar,
                                            Label pdfProgressLabel) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        if (ownerStage != null) stage.initOwner(ownerStage);

        boolean light = ThemeManager.getInstance().isLightMode();

        pdfProgressBar.setPrefWidth(340);
        pdfProgressBar.setPrefHeight(12);
        pdfProgressBar.setMaxWidth(Double.MAX_VALUE);
        pdfProgressBar.setStyle(
                "-fx-accent: " + (light ? "#B85507" : "rgba(249,115,22,0.95)") + ";" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-radius: 6;");

        pdfProgressLabel.setWrapText(true);
        pdfProgressLabel.setMaxWidth(Double.MAX_VALUE);
        pdfProgressLabel.setStyle(
                "-fx-text-fill: " + (light ? "#1A1A1A" : "rgba(226,232,240,0.90)") + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 700;");

        Label pdfPctLabel = new Label("0%");
        pdfPctLabel.setStyle(
                "-fx-text-fill: " + (light ? "#4A7566" : "rgba(148,163,184,0.80)") + ";" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: 700;");

        pdfProgressBar.progressProperty().addListener((obs, o, n) -> {
            double pct = n.doubleValue();
            Platform.runLater(() ->
                    pdfPctLabel.setText(pct < 0 ? "…" : String.format("%.0f%%", pct * 100)));
        });

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(22, 22);
        spinner.setMaxSize(22, 22);
        spinner.setMinSize(22, 22);
        spinner.setStyle("-fx-progress-color: " + (light ? "#B85507" : "rgba(249,115,22,0.95)") + ";");

        Label titleLbl = new Label("Generating PDF");
        titleLbl.setStyle(
                "-fx-text-fill: " + (light ? "#FFFFFF" : "rgba(248,250,252,0.98)") + ";" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: 900;");

        Label subLbl = new Label("Please wait while the report is being built…");
        subLbl.setStyle(
                "-fx-text-fill: " + (light ? "rgba(255,255,255,0.75)" : "rgba(148,163,184,0.80)") + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: 600;");

        VBox titleBlock = new VBox(3);
        titleBlock.getChildren().addAll(titleLbl, subLbl);

        HBox headerBox = new HBox(12);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(18, 22, 18, 22));
        headerBox.setStyle(
                "-fx-background-color: " + (light ? "#5C8A79" : "rgba(255,255,255,0.025)") + ";" +
                        "-fx-border-color: " + (light ? "rgba(90,130,115,0.45)" : "rgba(148,163,184,0.12)") + ";" +
                        "-fx-border-width: 0 0 1 0;" +
                        "-fx-background-radius: 10 10 0 0;");
        headerBox.getChildren().addAll(spinner, titleBlock);

        // Progress bar wrapper
        VBox barWrapper = new VBox(0);
        barWrapper.setStyle(
                "-fx-background-color: " + (light ? "rgba(176,200,178,0.35)" : "rgba(255,255,255,0.06)") + ";" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: " + (light ? "rgba(176,200,178,0.70)" : "rgba(148,163,184,0.14)") + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-padding: 0;");
        barWrapper.getChildren().add(pdfProgressBar);

        Region pctSpacer = new Region();
        HBox.setHgrow(pctSpacer, Priority.ALWAYS);
        HBox pctRow = new HBox();
        pctRow.getChildren().addAll(pctSpacer, pdfPctLabel);

        VBox bodyBox = new VBox(12);
        bodyBox.setPadding(new Insets(22, 22, 24, 22));
        bodyBox.setStyle("-fx-background-color: transparent;");
        bodyBox.getChildren().addAll(pdfProgressLabel, barWrapper, pctRow);

        VBox card = new VBox(0);
        card.setPrefWidth(420);
        card.setStyle(
                "-fx-background-color: " + (light ? "#EDE8DF" : "#0b1220") + ";" +
                        "-fx-border-color: " + (light ? "rgba(176,200,178,0.80)" : "rgba(148,163,184,0.22)") + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0," + (light ? "0.18" : "0.45") + "), 28, 0.0, 0, 6);");
        card.getChildren().addAll(headerBox, bodyBox);

        Scene scene = new Scene(card);
        scene.setFill(null);
        stage.setScene(scene);

        if (ownerStage != null) {
            stage.setX(ownerStage.getX() + (ownerStage.getWidth()  - 420) / 2);
            stage.setY(ownerStage.getY() + (ownerStage.getHeight() - 180) / 2);
        }

        return stage;
    }
    private static void shutDown(ExecutorService executor) {
        try {
            System.out.println("Attempting to shutdown PDF executor...");
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("PDF executor tasks interrupted.");
        } finally {
            if (!executor.isTerminated())
                System.err.println("Cancelling non-finished PDF tasks...");
            executor.shutdownNow();
            System.out.println("PDF executor shutdown finished.");
        }
    }
}