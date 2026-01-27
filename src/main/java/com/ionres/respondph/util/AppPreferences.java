package com.ionres.respondph.util;

import java.util.prefs.Preferences;

/**
 * Manages application preferences using Java Preferences API.
 * Handles persistent storage of user preferences like remember me functionality.
 */
public final class AppPreferences {
    private static final Preferences PREFS = Preferences.userNodeForPackage(AppPreferences.class);
    
    // Preference keys
    private static final String KEY_REMEMBER_ME = "remember_me_enabled";
    private static final String KEY_SAVED_USERNAME = "saved_username";
    private static final String KEY_LOGIN_TOKEN = "login_token";
    
    private AppPreferences() {
        // Utility class - prevent instantiation
    }
    
    // Remember Me functionality
    public static void setRememberMe(boolean enabled) {
        PREFS.putBoolean(KEY_REMEMBER_ME, enabled);
    }
    
    public static boolean isRememberMeEnabled() {
        return PREFS.getBoolean(KEY_REMEMBER_ME, false);
    }
    
    public static void saveUsername(String username) {
        if (username != null && !username.trim().isEmpty()) {
            PREFS.put(KEY_SAVED_USERNAME, username);
        } else {
            PREFS.remove(KEY_SAVED_USERNAME);
        }
    }
    
    public static String getSavedUsername() {
        return PREFS.get(KEY_SAVED_USERNAME, null);
    }
    
    public static void clearRememberMe() {
        PREFS.remove(KEY_REMEMBER_ME);
        PREFS.remove(KEY_SAVED_USERNAME);
    }
    
    // Token functionality (for future use)
    public static void saveLoginToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            PREFS.put(KEY_LOGIN_TOKEN, token);
        } else {
            PREFS.remove(KEY_LOGIN_TOKEN);
        }
    }
    
    public static String getToken() {
        return PREFS.get(KEY_LOGIN_TOKEN, null);
    }
    
    public static void clearToken() {
        PREFS.remove(KEY_LOGIN_TOKEN);
    }
    
    public static boolean hasToken() {
        return getToken() != null;
    }
    
    /**
     * Clears all stored preferences.
     * Use with caution - this will remove all user preferences.
     */
    public static void clearAll() {
        try {
            PREFS.clear();
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(AppPreferences.class.getName())
                .warning("Failed to clear preferences: " + e.getMessage());
        }
    }
}

