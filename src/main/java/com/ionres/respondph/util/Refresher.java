package com.ionres.respondph.util;
import com.ionres.respondph.aid.dialogs_controller.PrintAidDialogController;
import com.ionres.respondph.dashboard.DashboardController;
import com.ionres.respondph.disaster_mapping.DisasterMappingController;
import com.ionres.respondph.disaster_mapping.dialogs_controller.EvacuationSiteMappingController;
import com.ionres.respondph.evacuation_plan.EvacuationPlanController;
import com.ionres.respondph.sendsms.SendSMSController;
import com.ionres.respondph.vulnerability_indicator.VulnerabilityIndicatorController;
import javafx.application.Platform;
import com.ionres.respondph.beneficiary.BeneficiaryModel;
import com.ionres.respondph.sendsms.dialogs_controller.BeneficiarySelectionDialogController;

public final class Refresher {

    private static DashboardController controller;
    private static VulnerabilityIndicatorController vulnerabilityIndicatorController;
    private static PrintAidDialogController printAidDialogController;
    private static SendSMSController sendSMSController;
    private static DisasterMappingController disasterMappingController;
    private static EvacuationSiteMappingController evacuationSiteMappingController;
    private static EvacuationPlanController evacuationPlanController;
    private static BeneficiarySelectionDialogController beneficiarySelectionDialogController;

    private Refresher() {}

    public static void register(DashboardController ctrl) {
        controller = ctrl;
    }

    public static  void registerLoadVulScore(VulnerabilityIndicatorController controller){
        vulnerabilityIndicatorController = controller;
    }

    public static void registerBeneficiarySelectionDialog(BeneficiarySelectionDialogController controller) {
        beneficiarySelectionDialogController = controller;
    }

    public static  void  registerDisasterNameAndAidtypeName(PrintAidDialogController controller){
        printAidDialogController = controller;
    }

    public static void registerDisasterAndBeneficiaryCombo(SendSMSController controller){
        sendSMSController = controller;
    }

    public static void registerBeneficiaryMapInDisasterMapping(DisasterMappingController controller){
        disasterMappingController = controller;
    }

    public static void registerEvacSiteInMap(EvacuationSiteMappingController controller){
        evacuationSiteMappingController = controller;
    }

    public static void registerEvacuationPlanController(EvacuationPlanController controller){
        evacuationPlanController = controller;

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

    public static void refreshBeneficiarySelectionTable() {
        if (sendSMSController != null) {
            Platform.runLater(sendSMSController::reloadBeneficiarySelectionTable);
        }
    }

    public static void refreshSMSLogs(){
        if(sendSMSController != null){
            Platform.runLater(sendSMSController::loadSMSLogs);
        }
    }

    public static  void refreshFlds(){
        if (vulnerabilityIndicatorController != null){
            Platform.runLater(vulnerabilityIndicatorController::loadVulnerabilityData);
        }
    }

    public static void refreshMapInDisasterMapping(){
        if(disasterMappingController != null){
            Platform.runLater(disasterMappingController::loadBeneficiariesFromDb);
        }
    }

    public static void refreshComboAllTypesDisaster(){
        if (disasterMappingController != null){
            Platform.runLater(disasterMappingController::loadDisasterTypes);
        }
    }

    public static  void refreshEvacSiteMap(){
        if (evacuationSiteMappingController != null){
            Platform.runLater(evacuationSiteMappingController::loadEvacSitesFromDb);
        }
    }

    public static void refreshEvacuationPlanController(){
        if (evacuationPlanController != null){
            Platform.runLater(evacuationPlanController::loadTable);
        }
    }

    // This already exists, but add this new method:
    public static void refreshDisasterCircle(int disasterId) {
        if (disasterMappingController != null) {
            Platform.runLater(() -> disasterMappingController.reloadCircleIfSelected(disasterId));
        }
    }
}