package com.ionres.respondph.util;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
    private static final double MIN_ZOOM = 13;
    private static final double MAX_ZOOM = 20.0;
    private static final int TILE_SIZE = 256;
    private static final double BOUND_NORTH = 11.116584029742963;
    private static final double BOUND_SOUTH = 10.984159872049194;
    private static final double BOUND_WEST  = 122.6584666442871;
    private static final double BOUND_EAST  = 122.93484146118163;
    private final Map<String, Image> tileCache = new HashMap<>();
    private final Properties props = new Properties();

    private String getTileDirectory() {

        try (InputStream input =
                     Mapping.class.getClassLoader()
                             .getResourceAsStream("config/Outlet.properties")) {

            if (input == null) {
                throw new RuntimeException("Outlet.properties not found in /config folder!");
            }

            props.load(input);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load Outlet.properties", e);
        }

        return props.getProperty("map.tile.directory");
    }

    public Mapping() {
        this.initialized = false;
        this.zoom = MIN_ZOOM;
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
        if (container == null) return;

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

            canvas.widthProperty().addListener((o, a, b) -> redrawSafe());
            canvas.heightProperty().addListener((o, a, b) -> redrawSafe());

            initialized = true;

            // FIX: Wait for the UI thread to finish layout before centering
            javafx.application.Platform.runLater(() -> {
                if (isInitialized()) {
                    center();
                    centered = true;
                }
            });

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
        if (!isInitialized()) return;

        if (canvas.getWidth() > 0 && canvas.getHeight() > 0) {
            if (!centered) {
                center(); // center() calls clamp() and redraw() internally
                centered = true;
                LOGGER.info("Map auto-centered on first layout.");
            } else {
                redraw();
            }
        }
    }

    public void setAfterRedraw(Runnable r) {
        afterRedraw = r;
    }

    public void redraw() {
        if (!isInitialized()) return;

        try {
            // Set background to the blue color from your screenshot
            gc.setFill(Color.rgb(63, 91, 156));
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

            drawTiles();

            if (markerPosition != null && !dragging && markerImage != null && !markerImage.isError()) {
                double w = markerImage.getWidth();
                double h = markerImage.getHeight();
                if (w > 0 && h > 0) {
                    gc.drawImage(markerImage, markerPosition.x - w/2, markerPosition.y - h, w, h);
                }
            }

            if (afterRedraw != null) {
                afterRedraw.run();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during redraw", e);
        }
    }

    private void center() {
        int baseZoom = (int) Math.floor(zoom);
        double scale = Math.pow(2, zoom - baseZoom);

        Point p = latLonToPixel(11.052390, 122.786762, baseZoom);

        offsetX = canvas.getWidth() / 2 - p.x * scale;
        offsetY = canvas.getHeight() / 2 - p.y * scale;

        clamp();
        redraw();
    }
    private void drawTiles() {
        if (!isInitialized()) return;
        gc.setImageSmoothing(true);

        int baseZoom = (int) Math.floor(zoom);
        double scale = Math.pow(2, zoom - baseZoom);
        int tilesAtBase = 1 << baseZoom;

        int startX = (int) Math.floor(-offsetX / (TILE_SIZE * scale));
        int startY = (int) Math.floor(-offsetY / (TILE_SIZE * scale));
        int endX = startX + (int) Math.ceil(canvas.getWidth() / (TILE_SIZE * scale)) + 1;
        int endY = startY + (int) Math.ceil(canvas.getHeight() / (TILE_SIZE * scale)) + 1;

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                if (x < 0 || y < 0 || x >= tilesAtBase || y >= tilesAtBase) continue;

                TileResult result = findBestTile(baseZoom, x, y);

                if (result != null) {
                    double overzoomScale = Math.pow(2, baseZoom - result.zoom);
                    double s = scale * overzoomScale;

                    double sx = (x % overzoomScale) * (TILE_SIZE / overzoomScale);
                    double sy = (y % overzoomScale) * (TILE_SIZE / overzoomScale);
                    double sw = TILE_SIZE / overzoomScale;
                    double sh = TILE_SIZE / overzoomScale;

                    gc.drawImage(
                            result.image,
                            sx, sy, sw, sh, // Source (part of the tile)
                            x * TILE_SIZE * scale + offsetX, // Destination X
                            y * TILE_SIZE * scale + offsetY, // Destination Y
                            TILE_SIZE * scale, // Destination Width
                            TILE_SIZE * scale  // Destination Height
                    );
                }
            }
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
        if (!isInitialized() || canvas.getWidth() <= 0) return;

        int baseZoom = (int) Math.floor(zoom);
        double scale = Math.pow(2, zoom - baseZoom);

        // 1. Get the pixel bounds of your specific area (Banate)
        Point nw = latLonToPixel(BOUND_NORTH, BOUND_WEST, baseZoom);
        Point se = latLonToPixel(BOUND_SOUTH, BOUND_EAST, baseZoom);

        double mapWidthPixels = (se.x - nw.x) * scale;
        double mapHeightPixels = (se.y - nw.y) * scale;

        // 2. Clamp Horizontal (X)
        if (mapWidthPixels <= canvas.getWidth()) {
            // If map is skinnier than the window, center it
            offsetX = (canvas.getWidth() - mapWidthPixels) / 2 - (nw.x * scale);
        } else {
            // Otherwise, prevent showing anything outside BOUND_WEST and BOUND_EAST
            double minX = canvas.getWidth() - (se.x * scale);
            double maxX = -nw.x * scale;
            if (offsetX < minX) offsetX = minX;
            if (offsetX > maxX) offsetX = maxX;
        }

        // 3. Clamp Vertical (Y)
        if (mapHeightPixels <= canvas.getHeight()) {
            // If map is shorter than the window, center it
            offsetY = (canvas.getHeight() - mapHeightPixels) / 2 - (nw.y * scale);
        } else {
            // Otherwise, prevent showing anything outside BOUND_NORTH and BOUND_SOUTH
            double minY = canvas.getHeight() - (se.y * scale);
            double maxY = -nw.y * scale;
            if (offsetY < minY) offsetY = minY;
            if (offsetY > maxY) offsetY = maxY;
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
        if (!isInitialized()) return;

        double oldZoom = zoom;
        double delta = e.getDeltaY() > 0 ? 0.2 : -0.2;
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom + delta));

        if (zoom == oldZoom) return;

        // Calculate how much the map scaled
        double scale = Math.pow(2, zoom - oldZoom);

        // Adjust offsets so the zoom stays centered on the mouse cursor
        offsetX = e.getX() - (e.getX() - offsetX) * scale;
        offsetY = e.getY() - (e.getY() - offsetY) * scale;

        // Crucial: snap to bounds immediately so no "dead space" is shown
        clamp();
        redraw();
    }

    private Image loadTile(int z, int x, int y) {
        String key = z + "/" + x + "/" + y;

        Image cached = tileCache.get(key);
        if (cached != null && !cached.isError()) {
            return cached;
        }

        try {
            File zoomDir = new File(getTileDirectory(), String.valueOf(z));
            File xDir = new File(zoomDir, String.valueOf(x));
            File tileFile = new File(xDir, y + ".png");

            if (!tileFile.exists()) {
                return null;
            }

            Image img = loadTileFromFile(tileFile);

            if (img != null && !img.isError() && !isTileEmpty(img)) {
                tileCache.put(key, img);
                return img;
            }

            return null;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Tile load failed: " + key, e);
            return null;
        }
    }

    private boolean isTileEmpty(Image img) {
        try {
            int width = (int) img.getWidth();
            int height = (int) img.getHeight();
            javafx.scene.image.PixelReader reader = img.getPixelReader();
            if (reader == null) return true;

            for (int i = 0; i < width; i += 16) {
                for (int j = 0; j < height; j += 16) {
                    Color c = reader.getColor(i, j);
                    if (c.getOpacity() > 0 && (c.getRed() < 0.99 || c.getGreen() < 0.99 || c.getBlue() < 0.99)) {
                        return false; // found a non-white/opaque pixel
                    }
                }
            }
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking if tile is empty", e);
            return true;
        }
    }

    private Image loadTileFromFile(File tileFile) {
        try {
            return new Image(tileFile.toURI().toString(), false);
        } catch (Exception e) {
            return null;
        }
    }


    private Point latLonToPixel(double lat, double lon, int z) {
        double n = Math.pow(2, z);
        double x = (lon + 180.0) / 360.0 * n * TILE_SIZE;
        double y = (1 - Math.log(Math.tan(Math.toRadians(lat)) +
                1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2
                * n * TILE_SIZE;
        return new Point(x, y);
    }

    public Point latLonToScreen(double lat, double lon) {
        int baseZoom = (int) Math.floor(zoom);
        double scale = Math.pow(2, zoom - baseZoom);

        Point p = latLonToPixel(lat, lon, baseZoom);
        return new Point(
                p.x * scale + offsetX,
                p.y * scale + offsetY
        );
    }

    public double metersPerPixel(double lat) {
        if (!isValidCoordinate(lat, 0)) {
            LOGGER.fine("Invalid latitude in metersPerPixel: " + lat);
            return 1.0;
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

    private class TileResult {
        Image image;
        int zoom;
        TileResult(Image i, int z) { this.image = i; this.zoom = z; }
    }

    private TileResult findBestTile(int z, int x, int y) {
        int currentZ = z;
        int currentX = x;
        int currentY = y;

        while (currentZ >= (int)MIN_ZOOM) {
            Image img = loadTile(currentZ, currentX, currentY);
            if (img != null && !img.isError()) {
                return new TileResult(img, currentZ);
            }
            currentZ--;
            currentX /= 2;
            currentY /= 2;
        }
        return null;
    }
}