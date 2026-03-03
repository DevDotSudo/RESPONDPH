package com.ionres.respondph.util;

import javafx.scene.Parent;
import javafx.scene.Scene;

import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Lightweight singleton that tracks the current theme (dark / light)
 * and can apply it to any Scene root that opens later (e.g. dialogs).
 */
public final class ThemeManager {

    private static final ThemeManager INSTANCE = new ThemeManager();

    private static final String LIGHT_STYLE_CLASS = "root-light";
    private static final String LIGHT_CSS =
            Objects.requireNonNull(
                    ThemeManager.class.getResource("/styles/main/mainframe-light.css"),
                    "mainframe-light.css not found on classpath"
            ).toExternalForm();

    private static final String PREF_KEY = "light_mode";
    private final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);

    private boolean lightMode;

    private ThemeManager() {
        // Restore saved preference (defaults to false = dark mode)
        lightMode = prefs.getBoolean(PREF_KEY, false);
    }

    public static ThemeManager getInstance() { return INSTANCE; }

    /** Returns {@code true} when the app is in light mode. */
    public boolean isLightMode() { return lightMode; }

    /** Toggles the theme, persists the choice, and applies it to the given scene root. */
    public void setLightMode(boolean light, Scene scene) {
        this.lightMode = light;
        prefs.putBoolean(PREF_KEY, light);   // persist across restarts
        if (scene != null) {
            applyTo(scene.getRoot(), scene);
        }
    }

    /**
     * Applies the current theme to an arbitrary root / scene
     * (called when a dialog Stage is created, for instance).
     *
     * The light CSS is added both to the root node's own stylesheet list
     * (higher cascade priority, persists across scene recreations) and to
     * the scene stylesheet list (for compatibility with the main window).
     */
    public void applyTo(Parent root, Scene scene) {
        if (root == null) return;

        if (lightMode) {
            if (!root.getStyleClass().contains(LIGHT_STYLE_CLASS)) {
                root.getStyleClass().add(LIGHT_STYLE_CLASS);
            }
            // Add to node-level stylesheets so it survives scene recreation
            if (!root.getStylesheets().contains(LIGHT_CSS)) {
                root.getStylesheets().add(LIGHT_CSS);
            }
            // Also add at scene level (used by the main window)
            if (scene != null && !scene.getStylesheets().contains(LIGHT_CSS)) {
                scene.getStylesheets().add(LIGHT_CSS);
            }
        } else {
            root.getStyleClass().remove(LIGHT_STYLE_CLASS);
            root.getStylesheets().remove(LIGHT_CSS);
            if (scene != null) {
                scene.getStylesheets().remove(LIGHT_CSS);
            }
        }
    }
}
