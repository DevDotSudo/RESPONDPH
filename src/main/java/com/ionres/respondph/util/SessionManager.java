package com.ionres.respondph.util;

import com.ionres.respondph.admin.AdminModel;


public class SessionManager {
    private static SessionManager instance;
    private AdminModel currentAdmin;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentAdmin(AdminModel admin) {
        this.currentAdmin = admin;
    }

    public AdminModel getCurrentAdmin() {
        return currentAdmin;
    }

    public String getCurrentAdminFullName() {
        if (currentAdmin != null) {
            return currentAdmin.getFirstname() + " " + currentAdmin.getLastname();
        }
        return "Unknown";
    }

    public String getCurrentAdminFirstName() {
        if (currentAdmin != null) {
            return currentAdmin.getFirstname();
        }
        return "Unknown";
    }

    public void clearSession() {
        currentAdmin = null;
    }
}