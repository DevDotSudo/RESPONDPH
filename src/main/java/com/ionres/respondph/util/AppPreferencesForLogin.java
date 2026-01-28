
package com.ionres.respondph.util;

import java.util.prefs.Preferences;

public class AppPreferencesForLogin {
    private static final String REMEMBER_ME_TOKEN = "remember_me_token";
    private final Preferences prefs;

    public AppPreferencesForLogin() {
        this.prefs = Preferences.userNodeForPackage(AppPreferences.class);
    }

    public void saveRememberMeToken(String token) {
        prefs.put(REMEMBER_ME_TOKEN, token);
    }

    public String getRememberMeToken() {
        return prefs.get(REMEMBER_ME_TOKEN, null);
    }

    public void clearRememberMe() {
        prefs.remove(REMEMBER_ME_TOKEN);
    }
}