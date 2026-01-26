package com.ionres.respondph.util;

import com.ionres.respondph.aid.AidController;
import com.ionres.respondph.aid.dialogs_controller.PrintAidDialogController;
import com.ionres.respondph.mapping.MappingController;
import com.ionres.respondph.vulnerability_indicator.VulnerabilityIndicatorController;
import javafx.application.Platform;

public final class DashboardRefresher {

    private static MappingController controller;
    private static VulnerabilityIndicatorController vulnerabilityIndicatorController;
    private static PrintAidDialogController printAidDialogController;

    private DashboardRefresher() {}

    public static void register(MappingController ctrl) {
        controller = ctrl;
    }
    public static  void registerLoadVulScore(VulnerabilityIndicatorController controller){
        vulnerabilityIndicatorController = controller;

    }

    public static  void  registerDisasterNameAndAidtypeName(PrintAidDialogController controller){
        printAidDialogController = controller;
    }

    public static void refreshComboBoxOfDNAndAN(){
        if (printAidDialogController != null){
            Platform.runLater(printAidDialogController::refreshComboBoxes);
        }
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