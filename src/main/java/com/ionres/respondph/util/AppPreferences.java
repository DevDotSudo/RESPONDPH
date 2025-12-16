package com.ionres.respondph.util;

import java.util.prefs.Preferences;

public class AppPreferences {

    private static final Preferences prefs = Preferences.userNodeForPackage(AppPreferences.class);

    private static final String KEY_TOKEN = "login_token";

    public static void saveLoginToken(String token) {
        prefs.put(KEY_TOKEN, token);
    }

    public static String getToken() {
        return prefs.get(KEY_TOKEN, null);
    }

    public static void clearToken() {
        prefs.remove(KEY_TOKEN);
    }

    public static boolean hasToken() {
        return getToken() != null;
    }
}

