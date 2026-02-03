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

public final class DialogManager {

    private static final Map<String, Stage> stages = new HashMap<>();
    private static final Map<String, Object> controllers = new HashMap<>();
    private static final Map<String, Parent> roots = new HashMap<>();

    private DialogManager() {}

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

    public static void show(String key) {
        Object controller = controllers.get(key);
        Stage stage = stages.get(key);

        if (stage == null) {
            Parent root = roots.get(key);
            if (root == null) throw new RuntimeException("Dialog not preloaded: " + key);

            stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);

            // Reuse existing Scene if this root already belongs to one to avoid
            // "Node is already set as root of another scene" IllegalArgumentException
            javafx.scene.Scene existingScene = root.getScene();
            if (existingScene != null) {
                stage.setScene(existingScene);
            } else {
                stage.setScene(new javafx.scene.Scene(root));
            }

            try {
                controller.getClass().getMethod("setDialogStage", Stage.class)
                        .invoke(controller, stage);
            }
            catch (NoSuchMethodException ignored) {
                // Controller does not expose setDialogStage; ignore
            }
            catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            stages.put(key, stage);
            // When dialog closes, clear only the Stage/Scene; keep preloaded controller/root for reuse
            stage.setOnHidden(e -> unloadStage(key));
        }
        stage.showAndWait();
    }

    /**
     * Unloads cached resources for a dialog so they can be garbage collected.
     */
    public static void unload(String key) {
        Stage s = stages.remove(key);
        if (s != null) {
            s.setScene(null);
        }
        controllers.remove(key);
        roots.remove(key);
    }

    /**
     * Clears only the Stage/Scene for the given key, keeping the preloaded controller and root.
      * This preserves AppLoader.preload semantics while avoiding Stage leaks.
     */
    public static void unloadStage(String key) {
        Stage s = stages.remove(key);
        if (s != null) {
            s.setScene(null);
        }
    }
}
