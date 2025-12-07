package com.ionres.respondph;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class RESPONDPH extends Application {
    
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/pages/LoginFrame.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/pages/loginframe.css").toExternalForm());
            
            primaryStage.setTitle("RespondPH - Login");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1600);
            primaryStage.setMinHeight(800);
            primaryStage.setMaximized(true); 
            primaryStage.show();
        } catch (Exception e) {
            
        }
    }
}