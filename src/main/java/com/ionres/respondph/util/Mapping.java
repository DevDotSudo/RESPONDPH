package com.ionres.respondph.util;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Mapping {
    private static final Logger LOGGER = Logger.getLogger(Mapping.class.getName());

    private Canvas canvas;
    private GraphicsContext gc;
    private Runnable afterRedraw;
    private double offsetX;
    private double offsetY;
    private double lastX;
    private double lastY;
    private boolean dragging;
    private boolean centered;
    private boolean initialized;
    public Point markerPosition;
    private Image markerImage;
    private double zoom;
    private static final double MIN_ZOOM = 13.2;
    private static final double MAX_ZOOM = 21;
    private static final int TILE_SIZE = 256;
    private static final double BOUND_NORTH = 11.116584029742963;
    private static final double BOUND_SOUTH = 10.984159872049194;
    private static final double BOUND_WEST  = 122.6584666442871;
    private static final double BOUND_EAST  = 122.93484146118163;
    private final Map<String, Image> tileCache = new HashMap<>();

    private static String getTileDirectory() {
        
//        try {
//            String configPath = ConfigLoader.get("map.tile.directory");
//            if (configPath != null && !configPath.trim().isEmpty()) {
//                return configPath.trim();
//            }
//        } catch (Exception e) {
//            LOGGER.severe("There was an error getting tile directory");
//        }
//
//        String sysProp = System.getProperty("map.tile.directory");
//        if (sysProp != null && !sysProp.trim().isEmpty()) {
//            return sysProp.trim();
//        }

        return "C:/Users/Davie/IdeaProjects/tiles";
    }

    public Mapping() {
        this.zoom = MIN_ZOOM;
        this.initialized = false;
        loadMarkerImage();
    }

    private void loadMarkerImage() {
        try {
            markerImage = new Image(getClass().getResourceAsStream("/images/placeholder.png"));
            if (markerImage.isError()) {
                LOGGER.warning("Failed to load marker image from resources, using null");
                markerImage = null;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading marker image from resources", e);
            markerImage = null;
        }
    }

    public void init(Pane container) {
        if (container == null) {
            LOGGER.severe("Cannot initialize Mapping: container is null");
            return;
        }

        try {
            canvas = new Canvas();
            gc = canvas.getGraphicsContext2D();

            canvas.widthProperty().bind(container.widthProperty());
            canvas.heightProperty().bind(container.heightProperty());

            container.getChildren().setAll(canvas);

            canvas.setOnMousePressed(this::mousePressed);
            canvas.setOnMouseDragged(this::mouseDragged);
            canvas.setOnMouseReleased(e -> dragging = false);
            canvas.setOnScroll(this::mouseScroll);

            canvas.widthProperty().addListener((o,a,b) -> redrawSafe());
            canvas.heightProperty().addListener((o,a,b) -> redrawSafe());

            center();
            initialized = true;
            LOGGER.fine("Mapping initialized successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing Mapping", e);
            initialized = false;
        }
    }

    public boolean isInitialized() {
        return initialized && canvas != null && gc != null &&
                canvas.getWidth() > 0 && canvas.getHeight() > 0;
    }

    public boolean isDragging() {
        return dragging;
    }

    public static boolean isValidCoordinate(double lat, double lon) {
        if (Double.isNaN(lat) || lat < -90.0 || lat > 90.0) {
            return false;
        }
        if (Double.isNaN(lon) || lon < -180.0 || lon > 180.0) {
            return false;
        }
        if (lat < BOUND_SOUTH || lat > BOUND_NORTH ||
                lon < BOUND_WEST || lon > BOUND_EAST) {
            LOGGER.fine("Coordinate outside application bounds: " + lat + ", " + lon);
        }
        return true;
    }

    private void redrawSafe() {
        if (!isInitialized()) {
            return;
        }

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
        if (!isInitialized()) {
            LOGGER.fine("Cannot redraw: mapping not initialized");
            return;
        }

        try {
            gc.setFill(Color.rgb(240,240,240));
            gc.fillRect(0,0,canvas.getWidth(),canvas.getHeight());

            drawTiles();

            if (markerPosition != null && !dragging && markerImage != null && !markerImage.isError()) {
                double w = markerImage.getWidth();
                double h = markerImage.getHeight();
                if (w > 0 && h > 0) {
                    gc.drawImage(markerImage, markerPosition.x - w/2, markerPosition.y - h, w, h);
                }
            }

            if (afterRedraw != null) {
                try {
                    afterRedraw.run();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error in afterRedraw callback", e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during redraw", e);
        }
    }

    private void center() {
        if (!isInitialized()) {
            return;
        }

        try {
            Point p = latLonToPixel(11.052390, 122.786762, zoom);
            offsetX = canvas.getWidth() / 2 - p.x;
            offsetY = canvas.getHeight() / 2 - p.y;
            clamp();
            redraw();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error centering map", e);
        }
    }

    private void drawTiles() {
        if (!isInitialized()) {
            return;
        }

        try {
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
                    if (img == null || img.isError()) continue;

                    gc.drawImage(
                            img,
                            x * TILE_SIZE * scale + offsetX,
                            y * TILE_SIZE * scale + offsetY,
                            TILE_SIZE * scale,
                            TILE_SIZE * scale
                    );
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error drawing tiles", e);
        }
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public LatLng screenToLatLon(double x, double y) {
        if (!isInitialized()) {
            LOGGER.warning("Cannot convert screen to lat/lon: mapping not initialized");
            return new LatLng(0, 0);
        }

        try {
            double px = x - offsetX;
            double py = y - offsetY;

            int baseZoom = (int) Math.floor(zoom);
            double scale = Math.pow(2, zoom - baseZoom);
            double n = Math.pow(2, baseZoom) * scale;

            double lon = px / (TILE_SIZE * n) * 360.0 - 180.0;
            double lat = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * py / (TILE_SIZE * n)))));

            if (!isValidCoordinate(lat, lon)) {
                LOGGER.fine("Invalid coordinate conversion result: " + lat + ", " + lon);
            }

            return new LatLng(lat, lon);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error converting screen to lat/lon", e);
            return new LatLng(0, 0);
        }
    }


    private void clamp() {
        if (!isInitialized()) {
            return;
        }

        try {
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
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error clamping map bounds", e);
        }
    }

    private void mousePressed(MouseEvent e) {
        if (!isInitialized()) {
            return;
        }
        dragging = true;
        lastX = e.getX();
        lastY = e.getY();
    }

    private void mouseDragged(MouseEvent e) {
        if (!isInitialized() || !dragging) {
            return;
        }

        try {
            offsetX += e.getX() - lastX;
            offsetY += e.getY() - lastY;

            lastX = e.getX();
            lastY = e.getY();

            clamp();
            redraw();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error during mouse drag", ex);
        }
    }

    private void mouseScroll(ScrollEvent e) {
        if (!isInitialized()) {
            return;
        }

        try {
            double oldZoom = zoom;
            zoom += e.getDeltaY() > 0 ? 0.2 : -0.2;
            zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
            if (zoom == oldZoom) return;

            double scale = Math.pow(2, zoom - oldZoom);
            offsetX = e.getX() - (e.getX() - offsetX) * scale;
            offsetY = e.getY() - (e.getY() - offsetY) * scale;

            clamp();
            redraw();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error during mouse scroll", ex);
        }
    }

    private Image loadTile(int z, int x, int y) {
        int max = (1 << z) - 1;
        int fy = max - y;
        String key = z + "/" + x + "/" + fy;

        Image img = tileCache.get(key);
        if (img != null && !img.isError()) {
            return img;
        }

        try {
            String tileDir = getTileDirectory();
            // Construct path: tileDir/z/x/y.png
            File zoomFolder = new File(tileDir, String.valueOf(z));
            File xFolder = new File(zoomFolder, String.valueOf(x));
            File tileFile = new File(xFolder, fy + ".png");

            // Log the full path being attempted - ADD THIS FOR DEBUGGING
            LOGGER.info("Attempting to load tile: " + tileFile.getAbsolutePath() + " (exists: " + tileFile.exists() + ")");

            if (!tileFile.exists() || !tileFile.isFile()) {
                LOGGER.warning("Tile file does not exist: " + tileFile.getAbsolutePath());
                return null;
            }

            String tilePath = tileFile.toURI().toString();
            img = new Image(tilePath, false);

            if (img.isError()) {
                String fallbackPath = "file:/" + tileFile.getAbsolutePath().replace("\\", "/");
                fallbackPath = fallbackPath.replace(" ", "%20");
                img = new Image(fallbackPath, false);

                if (img.isError()) {
                    LOGGER.warning("Failed to load tile: " + key + " from both " + tilePath + " and " + fallbackPath);
                    return null;
                }
            }

            if (img.getWidth() > 0 && img.getHeight() > 0) {
                tileCache.put(key, img);
                LOGGER.info("Successfully loaded tile: " + key);
            } else {
                LOGGER.warning("Tile loaded but has zero dimensions: " + key);
                return null;
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading tile: " + key + " - " + e.getMessage(), e);
            return null;
        }

        return img;
    }

    private Point latLonToPixel(double lat, double lon, double z) {
        if (!isValidCoordinate(lat, lon)) {
            LOGGER.fine("Invalid coordinate in latLonToPixel: " + lat + ", " + lon);
            return new Point(0, 0);
        }

        try {
            double n = Math.pow(2, z);
            double x = (lon + 180) / 360 * n * TILE_SIZE;
            double y = (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * n * TILE_SIZE;
            return new Point(x, y);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error converting lat/lon to pixel", e);
            return new Point(0, 0);
        }
    }

    public Point latLonToScreen(double lat, double lon) {
        if (!isInitialized()) {
            LOGGER.warning("Cannot convert lat/lon to screen: mapping not initialized");
            return new Point(0, 0);
        }

        try {
            Point p = latLonToPixel(lat, lon, zoom);
            return new Point(p.x + offsetX, p.y + offsetY);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error converting lat/lon to screen", e);
            return new Point(0, 0);
        }
    }

    public double metersPerPixel(double lat) {
        if (!isValidCoordinate(lat, 0)) {
            LOGGER.fine("Invalid latitude in metersPerPixel: " + lat);
            return 1.0; // Default fallback
        }

        try {
            double earth = 40075016.686;
            return earth * Math.cos(Math.toRadians(lat)) / Math.pow(2, zoom + 8);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error calculating meters per pixel", e);
            return 1.0;
        }
    }

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double newZoom) {
        if (newZoom >= MIN_ZOOM && newZoom <= MAX_ZOOM) {
            this.zoom = newZoom;
            if (isInitialized()) {
                clamp();
                redraw();
            }
        } else {
            LOGGER.warning("Zoom value out of range: " + newZoom);
        }
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