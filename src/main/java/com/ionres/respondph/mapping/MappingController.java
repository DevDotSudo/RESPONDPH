package com.ionres.respondph.mapping;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

public class MappingController {

    @FXML
    private VBox mapContainer;

    public void initialize() {
        loadMap();
    }

    private void loadMap() {
        try {
            WebView webView = new WebView();
            webView.setPrefHeight(1200);
            webView.setPrefWidth(900);

            var mapUrl = getClass().getResource("/map_source/map.html");
            if (mapUrl == null) {
                System.err.println("map.html not found!");
                return;
            }

            webView.getEngine().load(mapUrl.toURI().toString()); // safer for spaces
            mapContainer.getChildren().add(webView);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
