package com.ionres.respondph.util;

import com.ionres.respondph.dashboard.DashboardController;
import com.ionres.respondph.vulnerability_indicator.VulnerabilityIndicatorController;
import javafx.application.Platform;

public final class DashboardRefresher {

    private static DashboardController controller;
    private static VulnerabilityIndicatorController vulnerabilityIndicatorController;

    private DashboardRefresher() {}

    public static void register(DashboardController ctrl) {
        controller = ctrl;
    }

    public static  void registerLoadVulScore(VulnerabilityIndicatorController controller){
        vulnerabilityIndicatorController = controller;

    }

    public static void refresh() {
        if (controller != null) {
            Platform.runLater(controller::loadDashBoardData);
        }
    }

    public static  void refreshFlds(){
        if (vulnerabilityIndicatorController != null){
            Platform.runLater(vulnerabilityIndicatorController::loadVulnerabilityData);
        }
    }
}