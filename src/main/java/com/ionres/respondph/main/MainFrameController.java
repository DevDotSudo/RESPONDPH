package com.ionres.respondph.main;

import javafx.fxml.FXML;

public class MainFrameController {
    
    @FXML
    private void handleDashboard() {
        System.out.println("Dashboard clicked");
    }
    
    @FXML
    private void handleManageAdmins() {
        System.out.println("Manage Admin clicked");
    }
    
    @FXML
    private void handleManageBeneficiaries() {
        System.out.println("Manage Beneficiaries clicked");
    }
    
    @FXML
    private void handleLogout() {
        System.out.println("Logout clicked");
    }
}