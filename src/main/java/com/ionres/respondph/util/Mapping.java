package com.ionres.respondph.util;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mapping {

    private Canvas canvas;
    private GraphicsContext gc;
    private Runnable afterRedraw;
    private double offsetX;
    private double offsetY;
    private double lastX;
    private double lastY;
    public boolean dragging;
    private boolean centered;
    public Point markerPosition;
    private final Image markerImage = new Image("file:/C:/Users/Davie/IdeaProjects/RESPONDPH/src/main/resources/images/placeholder.png", false);
    private static double zoom = 13.2;
    private static final double MIN_ZOOM = 13.2;
    private static final double MAX_ZOOM = 18;
    private static final int TILE_SIZE = 256;
    public LatLng circleCenter;
    public double circleRadiusMeters;
    private static final double BOUND_NORTH = 11.116584029742963;
    private static final double BOUND_SOUTH = 10.984159872049194;
    private static final double BOUND_WEST  = 122.6584666442871;
    private static final double BOUND_EAST  = 122.93484146118163;
    private final Map<String, Image> tileCache = new HashMap<>();

    public void init(Pane container) {
        canvas = new Canvas();
        gc = canvas.getGraphicsContext2D();

        canvas.widthProperty().bind(container.widthProperty());
        canvas.heightProperty().bind(container.heightProperty());
        center();
        container.getChildren().setAll(canvas);

        canvas.setOnMousePressed(this::mousePressed);
        canvas.setOnMouseDragged(this::mouseDragged);
        canvas.setOnMouseReleased(e -> dragging = false);
        canvas.setOnScroll(this::mouseScroll);

        canvas.widthProperty().addListener((o,a,b) -> redrawSafe());
        canvas.heightProperty().addListener((o,a,b) -> redrawSafe());
    }

    private void redrawSafe() {
        if (!centered && canvas.getWidth() > 0 && canvas.getHeight() > 0) {
            zoom = MIN_ZOOM;
            center();
            centered = true;
        } else if (centered) {
            redraw();
        }
    }

    public void setAfterRedraw(Runnable r) {
        afterRedraw = r;
    }

    public void redraw() {
        if (canvas.getWidth() <= 0 || canvas.getHeight() <= 0) return;

        gc.setFill(Color.rgb(240,240,240));
        gc.fillRect(0,0,canvas.getWidth(),canvas.getHeight());

        drawTiles();

        if (markerPosition != null && !dragging) {
            double w = markerImage.getWidth();
            double h = markerImage.getHeight();
            gc.drawImage(markerImage, markerPosition.x - w/2, markerPosition.y - h, w, h);
        }

        if (afterRedraw != null) afterRedraw.run();
    }


    private void center() {
        Point p = latLonToPixel(11.052390, 122.786762, zoom);
        offsetX = canvas.getWidth() / 2 - p.x;
        offsetY = canvas.getHeight() / 2 - p.y;
        clamp();
        redraw();
    }

    private void drawTiles() {
        int baseZoom = (int) Math.floor(zoom);
        double scale = Math.pow(2, zoom - baseZoom);
        int tiles = 1 << baseZoom;

        int startX = (int)Math.floor(-offsetX / (TILE_SIZE * scale));
        int startY = (int)Math.floor(-offsetY / (TILE_SIZE * scale));
        int endX = startX + (int)Math.ceil(canvas.getWidth() / (TILE_SIZE * scale)) + 1;
        int endY = startY + (int)Math.ceil(canvas.getHeight() / (TILE_SIZE * scale)) + 1;

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                if (x < 0 || y < 0 || x >= tiles || y >= tiles) continue;

                Image img = loadTile(baseZoom, x, y);
                if (img == null) continue;

                gc.drawImage(
                        img,
                        x * TILE_SIZE * scale + offsetX,
                        y * TILE_SIZE * scale + offsetY,
                        TILE_SIZE * scale,
                        TILE_SIZE * scale
                );
            }
        }
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public LatLng screenToLatLon(double x, double y) {
        double px = x - offsetX;
        double py = y - offsetY;

        double n = Math.pow(2, zoom);
        double lon = px / (TILE_SIZE * n) * 360.0 - 180.0;
        double lat = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * py / (TILE_SIZE * n)))));

        return new LatLng(lat, lon);
    }

    private void clamp() {
        Point nw = latLonToPixel(BOUND_NORTH, BOUND_WEST, zoom);
        Point se = latLonToPixel(BOUND_SOUTH, BOUND_EAST, zoom);

        double mapWidth = se.x - nw.x;
        double mapHeight = se.y - nw.y;

        if (mapWidth < canvas.getWidth()) {
            offsetX = (canvas.getWidth() - mapWidth) / 2 - nw.x;
        } else {
            offsetX = Math.min(-nw.x, Math.max(canvas.getWidth() - se.x, offsetX));
        }

        if (mapHeight < canvas.getHeight()) {
            offsetY = (canvas.getHeight() - mapHeight) / 2 - nw.y;
        } else {
            offsetY = Math.min(-nw.y, Math.max(canvas.getHeight() - se.y, offsetY));
        }
    }

    private void mousePressed(MouseEvent e) {
        dragging = true;
        lastX = e.getX();
        lastY = e.getY();
    }

    private void mouseDragged(MouseEvent e) {
        if (!dragging) return;

        offsetX += e.getX() - lastX;
        offsetY += e.getY() - lastY;

        lastX = e.getX();
        lastY = e.getY();

        clamp();
        redraw();
    }

    private void mouseScroll(ScrollEvent e) {
        double oldZoom = zoom;
        zoom += e.getDeltaY() > 0 ? 0.2 : -0.2;
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
        if (zoom == oldZoom) return;

        double scale = Math.pow(2, zoom - oldZoom);
        offsetX = e.getX() - (e.getX() - offsetX) * scale;
        offsetY = e.getY() - (e.getY() - offsetY) * scale;

        clamp();
        redraw();
    }

    private Image loadTile(int z, int x, int y) {
        int max = (1 << z) - 1;
        int fy = max - y;
        String key = z + "/" + x + "/" + fy;

        Image img = tileCache.get(key);
        if (img != null) return img;

        try {
            img = new Image("file:/C:/Users/Davie/OneDrive/Documents/IntellijIDEA%20Projects/tiles/" + key + ".png", false);
            if (!img.isError()) tileCache.put(key, img);
        } catch (Exception ignored) {}

        return img;
    }

    private Point latLonToPixel(double lat, double lon, double z) {
        int baseZoom = (int) Math.floor(z);
        double scale = Math.pow(2, z - baseZoom);

        double n = Math.pow(2, baseZoom);
        double x = (lon + 180) / 360 * n * TILE_SIZE * scale;
        double y = (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * n * TILE_SIZE * scale;

        return new Point(x, y);
    }

    public Point latLonToScreen(double lat, double lon) {
        Point p = latLonToPixel(lat, lon, zoom);
        return new Point(p.x + offsetX, p.y + offsetY);
    }


    public double metersPerPixel(double lat) {
        double earth = 40075016.686;
        return Math.cos(Math.toRadians(lat)) * earth / (TILE_SIZE * Math.pow(2, zoom));
    }

    public GraphicsContext getGc() {
        return gc;
    }

    public static class Point {
        public final double x;
        public final double y;
        public Point(double x, double y) { this.x = x; this.y = y; }
    }

    public static class LatLng {
        public final double lat;
        public final double lon;
        public LatLng(double lat, double lon) { this.lat = lat; this.lon = lon; }
    }
}
