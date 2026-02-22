package com.ionres.respondph.util;

import com.ionres.respondph.admin.AdminModel;
import javafx.application.Platform;

public class SessionManager {

    // ─── Singleton ────────────────────────────────────────────────────────────
    private static final SessionManager instance = new SessionManager();

    private SessionManager() {}

    public static SessionManager getInstance() {
        return instance;
    }

    // ─── State ────────────────────────────────────────────────────────────────
    private AdminModel currentAdmin;
    private Runnable   onSessionChanged;

    // ─── Session Listener ─────────────────────────────────────────────────────

    /**
     * Register a callback that fires (on the JavaFX thread) whenever
     * the session admin is set or cleared.
     */
    public void setOnSessionChanged(Runnable callback) {
        this.onSessionChanged = callback;
    }

    private void notifySessionChanged() {
        if (onSessionChanged != null) {
            Platform.runLater(onSessionChanged);
        }
    }

    // ─── Session Management ───────────────────────────────────────────────────

    public void setCurrentAdmin(AdminModel admin) {
        this.currentAdmin = admin;
        notifySessionChanged();
    }

    public AdminModel getCurrentAdmin() {
        return currentAdmin;
    }

    public void clearSession() {
        this.currentAdmin = null;
        notifySessionChanged();
    }

    public boolean isLoggedIn() {
        return currentAdmin != null;
    }

    // ─── Convenience Getters ──────────────────────────────────────────────────

    public int getCurrentAdminId() {
        requireLoggedIn();
        return currentAdmin.getId();
    }

    public String getCurrentAdminUsername() {
        requireLoggedIn();
        return currentAdmin.getUsername();
    }

    public String getCurrentAdminFirstName() {
        if (currentAdmin != null) {
            return currentAdmin.getFirstname();
        }
        return "Unknown";
    }

    public String getCurrentAdminFullName() {
        if (currentAdmin != null) {
            return currentAdmin.getFirstname() + " " + currentAdmin.getLastname();
        }
        return "Unknown";
    }

    /**
     * Returns the role of the currently logged-in admin.
     * Possible values: "Admin", "Secretary", "DSWD", "MDRRMO"
     */
    public String getCurrentAdminRole() {
        if (currentAdmin != null) {
            return currentAdmin.getRole();
        }
        return "Unknown";
    }

    // ─── Role Checks ──────────────────────────────────────────────────────────

    public boolean isAdmin() {
        return "Admin".equalsIgnoreCase(getCurrentAdminRole());
    }

    public boolean isSecretary() {
        return "Secretary".equalsIgnoreCase(getCurrentAdminRole());
    }

    public boolean isDSWD() {
        return "DSWD".equalsIgnoreCase(getCurrentAdminRole());
    }

    public boolean isMDRRMO() {
        return "MDRRMO".equalsIgnoreCase(getCurrentAdminRole());
    }

    /**
     * Returns true if the current admin has access to the Management section.
     * Roles: Admin, Secretary
     */
    public boolean canAccessManagement() {
        String role = getCurrentAdminRole();
        return "Admin".equalsIgnoreCase(role) ||
                "Secretary".equalsIgnoreCase(role);
    }

    /**
     * Returns true if the current admin has access to the Disaster Management section.
     * Roles: Admin, MDRRMO
     */
    public boolean canAccessDisasterManagement() {
        String role = getCurrentAdminRole();
        return "Admin".equalsIgnoreCase(role) ||
                "MDRRMO".equalsIgnoreCase(role);
    }

    /**
     * Returns true if the current admin has access to the Aids Management section.
     * Roles: Admin, DSWD
     */
    public boolean canAccessAidsManagement() {
        String role = getCurrentAdminRole();
        return "Admin".equalsIgnoreCase(role) ||
                "DSWD".equalsIgnoreCase(role);
    }

    /**
     * Returns true if the current admin has access to the Evacuation Site section.
     * Roles: Admin, MDRRMO
     */
    public boolean canAccessEvacuationSite() {
        String role = getCurrentAdminRole();
        return "Admin".equalsIgnoreCase(role) ||
                "MDRRMO".equalsIgnoreCase(role);
    }

    /**
     * Returns true if the current admin has access to the Vulnerability Indicator.
     * Roles: Admin, DSWD, MDRRMO
     */
    public boolean canAccessVulnerability() {
        String role = getCurrentAdminRole();
        return "Admin".equalsIgnoreCase(role)    ||
                "DSWD".equalsIgnoreCase(role)     ||
                "MDRRMO".equalsIgnoreCase(role);
    }

    /**
     * Returns true if the current admin can send SMS.
     * Roles: Admin, DSWD, MDRRMO
     */
    public boolean canSendSms() {
        String role = getCurrentAdminRole();
        return "Admin".equalsIgnoreCase(role)    ||
                "DSWD".equalsIgnoreCase(role)     ||
                "MDRRMO".equalsIgnoreCase(role);
    }

    // ─── Internal Helpers ─────────────────────────────────────────────────────

    private void requireLoggedIn() {
        if (currentAdmin == null) {
            throw new IllegalStateException("No admin is currently logged in.");
        }
    }
}