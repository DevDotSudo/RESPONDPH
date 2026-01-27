package com.ionres.respondph.splash;

import com.ionres.respondph.util.AppLoader;
import com.ionres.respondph.util.SceneManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

public class SplashScreenController {


    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label loadingText;

    public void initialize() {
        Task<Void> loadingTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int totalSteps = 5;
                int currentStep = 0;

                updateMessage("Initializing utilities...");
                AppLoader.initializeUtilities();
                currentStep++;
                updateProgress(currentStep, totalSteps);

                updateMessage("Connecting to database...");
                AppLoader.connectDatabase();
                currentStep++;
                updateProgress(currentStep, totalSteps);

                updateMessage("Loading services...");
                AppLoader.loadServices();
                currentStep++;
                updateProgress(currentStep, totalSteps);

                updateMessage("Preparing interface...");
                AppLoader.prepareUI();
                currentStep++;
                updateProgress(currentStep, totalSteps);

                updateMessage("Opening application...");
                Thread.sleep(200);
                currentStep++;
                updateProgress(currentStep, totalSteps);

                return null;
            }
        };

        progressBar.progressProperty().bind(loadingTask.progressProperty());
        loadingText.textProperty().bind(loadingTask.messageProperty());

        loadingTask.setOnSucceeded(event -> Platform.runLater(() -> {
            try {
                Stage splashStage = (Stage) loadingText.getScene().getWindow();
                splashStage.close();

                SceneManager.showStage(
                        "/view/auth/Login.fxml",
                        "RESPONDPH - Login"
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        loadingTask.setOnFailed(event -> Platform.runLater(() -> {
            loadingText.textProperty().unbind();
            loadingText.setText("Error loading application");
            java.util.logging.Logger.getLogger(SplashScreenController.class.getName())
                    .log(java.util.logging.Level.SEVERE, "Loading failed", loadingTask.getException());
        }));

        Thread loadingThread = new Thread(loadingTask);
        loadingThread.setDaemon(true);
        loadingThread.start();
    }
}
