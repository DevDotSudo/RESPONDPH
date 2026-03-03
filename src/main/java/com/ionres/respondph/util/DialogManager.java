package com.ionres.respondph.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class DialogManager {

    private static final Map<String, Stage>  stages      = new HashMap<>();
    private static final Map<String, Object> controllers = new HashMap<>();
    private static final Map<String, Parent> roots       = new HashMap<>();

    private DialogManager() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Preloaded dialogs  (fixed FXML, reused controller between calls)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Loads the FXML once and caches root + controller for reuse.
     * Subsequent calls with the same key are no-ops.
     */
    public static void preload(String key, String fxmlPath) throws Exception {
        if (controllers.containsKey(key)) return;

        FXMLLoader loader = new FXMLLoader(DialogManager.class.getResource(fxmlPath));
        Parent root = loader.load();
        Object controller = loader.getController();

        roots.put(key, root);
        controllers.put(key, controller);
    }

    public static Stage getStage(String key) {
        return stages.get(key);
    }

    public static <T> T getController(String key, Class<T> cls) {
        return cls.cast(controllers.get(key));
    }

    /**
     * Shows a preloaded dialog (UNDECORATED, APPLICATION_MODAL).
     * The Stage is created lazily on first call and reused thereafter.
     */
    public static void show(String key) {
        Object controller = controllers.get(key);
        Stage  stage      = stages.get(key);

        if (stage == null) {
            Parent root = roots.get(key);
            if (root == null) throw new RuntimeException("Dialog not preloaded: " + key);

            stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);

            // Reuse existing Scene if root already belongs to one — avoids
            // "Node already set as root of another scene" IllegalArgumentException.
            Scene existingScene = root.getScene();
            if (existingScene != null) {
                stage.setScene(existingScene);
            } else {
                stage.setScene(new Scene(root));
            }

            injectStage(controller, stage);
            stages.put(key, stage);

            // On close: clear Stage/Scene but keep preloaded controller + root.
            stage.setOnHidden(e -> unloadStage(key));
        }

        // Propagate current theme to dialog root
        Parent dialogRoot = roots.get(key);
        if (dialogRoot != null) {
            ThemeManager.getInstance().applyTo(dialogRoot, stage.getScene());
        }

        stage.showAndWait();
    }

    public static <C> void showDynamic(String fxmlPath,
                                       Stage ownerStage,
                                       Consumer<C> setup) {
        showDynamic(fxmlPath, ownerStage, setup, null);
    }

    @SuppressWarnings("unchecked")
    public static <C> void showDynamic(String fxmlPath,
                                       Stage ownerStage,
                                       Consumer<C> setup,
                                       Consumer<Stage> stageConfig) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            C controller = loader.getController();

            if (setup != null) setup.accept(controller);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            if (ownerStage != null) stage.initOwner(ownerStage);

            stage.setScene(new Scene(root));
            injectStage(controller, stage);

            // Propagate current theme to dialog root
            ThemeManager.getInstance().applyTo(root, stage.getScene());

            if (stageConfig != null) stageConfig.accept(stage);

            stage.showAndWait();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load dialog: " + fxmlPath, e);
        }
    }

    public static void unload(String key) {
        Stage s = stages.remove(key);
        if (s != null) s.setScene(null);
        controllers.remove(key);
        roots.remove(key);
    }

    public static void unloadStage(String key) {
        Stage s = stages.remove(key);
        if (s != null) s.setScene(null);
    }

    private static void injectStage(Object controller, Stage stage) {
        if (controller == null) return;
        try {
            controller.getClass()
                    .getMethod("setDialogStage", Stage.class)
                    .invoke(controller, stage);
        } catch (NoSuchMethodException ignored) {
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("setDialogStage injection failed", e);
        }
    }
}