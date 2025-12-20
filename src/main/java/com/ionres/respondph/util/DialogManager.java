package com.ionres.respondph.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.HashMap;
import java.util.Map;

public final class DialogManager {

    private static final Map<String, Stage> stages = new HashMap<>();
    private static final Map<String, Object> controllers = new HashMap<>();

    private DialogManager() {}

    public static void load(String key, String fxmlPath) throws Exception {
        FXMLLoader loader =
                new FXMLLoader(DialogManager.class.getResource(fxmlPath));
        Parent root = loader.load();

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(new Scene(root));

        stages.put(key, stage);
        controllers.put(key, loader.getController());
    }

    public static Stage getStage(String key) {
        return stages.get(key);
    }

    public static <T> T getController(String key, Class<T> cls) {
        return cls.cast(controllers.get(key));
    }
}
