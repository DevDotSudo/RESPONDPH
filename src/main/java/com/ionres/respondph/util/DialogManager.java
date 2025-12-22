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
            stage.setScene(new Scene(root));

            try {
                controller.getClass().getMethod("setDialogStage", Stage.class)
                        .invoke(controller, stage);
            } catch (NoSuchMethodException ignored) {}
            catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            stages.put(key, stage);
        }

        stage.showAndWait();
    }

}
