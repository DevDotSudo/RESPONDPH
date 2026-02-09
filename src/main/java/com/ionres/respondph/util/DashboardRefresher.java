package com.ionres.respondph.util;

import com.ionres.respondph.aid.dialogs_controller.PrintAidDialogController;
import com.ionres.respondph.dashboard.DashboardController;
import com.ionres.respondph.disaster.DisasterController;
import com.ionres.respondph.sendsms.SendSMSController;
import com.ionres.respondph.vulnerability_indicator.VulnerabilityIndicatorController;
import javafx.application.Platform;

public final class DashboardRefresher {

    private static DashboardController controller;
    private static VulnerabilityIndicatorController vulnerabilityIndicatorController;
    private static PrintAidDialogController printAidDialogController;
    private static SendSMSController sendSMSController;

    private DashboardRefresher() {}

    public static void register(DashboardController ctrl) {
        controller = ctrl;
    }

    public static  void registerLoadVulScore(VulnerabilityIndicatorController controller){
        vulnerabilityIndicatorController = controller;
    }

    public static  void  registerDisasterNameAndAidtypeName(PrintAidDialogController controller){
        printAidDialogController = controller;
    }

    public static void registerDisasterAndBeneficiaryCombo(SendSMSController controller){
        sendSMSController = controller;
    }

    public static void refreshComboBoxOfDNAndAN(){
        if (printAidDialogController != null){
            Platform.runLater(printAidDialogController::refreshComboBoxes);
        }
    }

    public static void refresh() {
        if (controller != null) {
            Platform.runLater(controller::loadDashBoardData);
            Platform.runLater(controller::loadBeneficiariesFromDb);
        }
    }

    public static void refreshBeneInSend() {
        if (sendSMSController != null) {
            Platform.runLater(sendSMSController::loadBarangayList);
        }
    }

    public static void refreshDisasterInSend() {
        if (sendSMSController != null) {
            Platform.runLater(sendSMSController::loadDisasters);
        }
    }


    public static  void refreshFlds(){
        if (vulnerabilityIndicatorController != null){
            Platform.runLater(vulnerabilityIndicatorController::loadVulnerabilityData);
        }
    }
}