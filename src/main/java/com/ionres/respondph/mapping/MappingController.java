package com.ionres.respondph.mapping;

import com.gluonhq.maps.MapView;
import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapLayer;
import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Circle;
import javafx.geometry.Point2D;

public class MappingController {

    @FXML
    private VBox mapContainer;

    final double MIN_ZOOM = 5.0;
    final double MAX_ZOOM = 18.0;

    private final double[][] coordinates = {
            {11.1029, 122.7580},
            {11.0772, 122.7312},
            {11.0765, 122.7446},
            {11.0742, 122.7461},
            {11.0721, 122.7451},
            {11.0700, 122.7470},
            {11.0692, 122.7487},
            {11.0615, 122.7496},
            {11.0484, 122.7719},
            {11.0036, 122.7859},
            {10.9829, 122.7868},
            {10.9762, 122.7965},
            {10.9911, 122.8516},
            {11.0212, 122.8316},
            {11.0416, 122.8201},
            {11.0493, 122.8074},
            {11.0533, 122.8023},
            {11.0583, 122.7993},
            {11.0674, 122.7958},
            {11.0755, 122.7940},
            {11.0896, 122.7797},
            {11.0897, 122.7666},
            {11.0928, 122.7618}
    };


    /* Sample this should be replaced by beneficiaries location */

    private final BarangayPoint[] barangays = {
            new BarangayPoint("Poblacion", 11.0021, 122.8196),
            new BarangayPoint("Alacaygan", 10.9977, 122.8046),
            new BarangayPoint("Bularan", 11.0035, 122.8230),
            new BarangayPoint("Carmelo", 11.0100, 122.8151),
            new BarangayPoint("Talokgangan", 11.0032, 122.8271),
            new BarangayPoint("Zona Sur", 11.0002, 122.8148),
            new BarangayPoint("San Salvador", 10.9992, 122.8356),
            new BarangayPoint("Belen", 10.9971, 122.7970),
            new BarangayPoint("Managopaya", 11.0881, 122.7622),
            new BarangayPoint("De La Paz", 11.0662, 122.7746),
            new BarangayPoint("Bariga", 11.0537, 122.7859),
            new BarangayPoint("Bobon", 11.0580, 122.7820),
            new BarangayPoint("Dugwakan", 11.0127, 122.7864),
            new BarangayPoint("Juanico", 11.0700, 122.7800),
            new BarangayPoint("Libertad", 11.0322, 122.7963),
            new BarangayPoint("Magdalo", 11.0446, 122.7940),
            new BarangayPoint("Merced", 11.0200, 122.8050),
            new BarangayPoint("Fuentes", 10.9983, 122.7853)
    };

    public void initialize() {
        loadMap();
    }

    private void loadMap() {
        MapView mapView = new MapView();
        mapView.setZoom(10);

        MapPoint center = new MapPoint(11.0450, 122.7950);
        mapView.setCenter(center);

        MapLayer polygonLayer = new MapLayer() {
            private Polygon polygon;

            @Override
            protected void layoutLayer() {
                if (polygon == null) {
                    polygon = new Polygon();

                    polygon.setFill(Color.TRANSPARENT);
                    polygon.setStroke(Color.RED);
                    polygon.setStrokeWidth(2);

                    this.getChildren().add(polygon);
                }

                polygon.getPoints().clear();

                for (double[] coord : coordinates) {
                    Point2D point = this.getMapPoint(coord[0], coord[1]);
                    polygon.getPoints().addAll(point.getX(), point.getY());
                }
            }
        };

        MapLayer markerLayer = new MapLayer() {

            @Override
            protected void layoutLayer() {
                this.getChildren().clear();

                for (BarangayPoint barangay : barangays) {

                    Point2D point = this.getMapPoint(barangay.lat, barangay.lon);

                    javafx.scene.image.Image icon = new javafx.scene.image.Image(
                            getClass().getResourceAsStream("/images/person_marker.png"),
                            32, 32, true, true
                    );

                    javafx.scene.image.ImageView iconView = new javafx.scene.image.ImageView(icon);

                    iconView.setX(point.getX() - icon.getWidth() / 2);
                    iconView.setY(point.getY() - icon.getHeight() / 2);

                    this.getChildren().add(iconView);
                }
            }
        };

        mapView.addLayer(polygonLayer);
        mapView.addLayer(markerLayer);
        mapContainer.getChildren().add(mapView);
    }

    private static class BarangayPoint {
        String name;
        double lat;
        double lon;

        BarangayPoint(String name, double lat, double lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }
}