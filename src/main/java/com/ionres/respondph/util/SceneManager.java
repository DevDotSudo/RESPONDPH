package com.ionres.respondph.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SceneManager {

    private static final Map<String, SceneEntry<?>> CACHE =
            new ConcurrentHashMap<>();

    private SceneManager() {}

    public static <T> SceneEntry<T> load(String fxmlPath) {
        if (CACHE.containsKey(fxmlPath)) {
            return (SceneEntry<T>) CACHE.get(fxmlPath);
        }

        try {
            FXMLLoader loader =
                    new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            T controller = loader.getController();

            SceneEntry<T> entry = new SceneEntry<>(root, controller);
            CACHE.put(fxmlPath, entry);
            return entry;

        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML: " + fxmlPath, e);
        }
    }

    public static void preload(String fxmlPath) {
        load(fxmlPath);
    }

    public static Stage showStage(
            String fxmlPath,
            String title
    ) {
        SceneEntry<?> entry = load(fxmlPath);

        Stage stage = new Stage();
        Scene scene = new Scene(entry.getRoot(), 1600, 800);

        stage.setTitle(title);
        stage.setScene(scene);
        stage.setMinWidth(1600);
        stage.setMinHeight(800);
        stage.setMaximized(true);
        stage.show();

        return stage;
    }

    public static class SceneEntry<T> {
        private final Parent root;
        private final T controller;

        private SceneEntry(Parent root, T controller) {
            this.root = root;
            this.controller = controller;
        }

        public Parent getRoot() {
            return root;
        }

        public T getController() {
            return controller;
        }
    }
}
