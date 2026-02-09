package com.ionres.respondph.util;

import com.ionres.respondph.admin.AdminServiceImpl;
import com.ionres.respondph.admin.login.LoginServiceImpl;
import com.ionres.respondph.aid_type.AidTypeServiceImpl;
import com.ionres.respondph.beneficiary.BeneficiaryServiceImpl;
import com.ionres.respondph.common.services.EvacSiteMappingServiceImpl;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.disaster.DisasterServiceImpl;
import com.ionres.respondph.disaster_damage.DisasterDamageServiceImpl;
import com.ionres.respondph.disaster_mapping.DisasterMappingServiceImpl;
import com.ionres.respondph.evac_site.EvacSiteServiceImpl;
import com.ionres.respondph.familymembers.FamilyMemberServiceImpl;
import com.ionres.respondph.dashboard.DashBoardServiceImpl;
import com.ionres.respondph.vulnerability_indicator.VulnerabilityIndicatorServiceImpl;
import java.util.logging.Logger;

public class AppLoader {
    private static final Logger LOGGER = Logger.getLogger(AppLoader.class.getName());

    public static void initializeUtilities() {
        try {
            ConfigLoader.get("secretKey");
            
            CryptographyManager.getInstance();
            
            SessionManager.getInstance();
            
            LOGGER.info("Utilities initialized successfully");
        } catch (Exception e) {
            LOGGER.severe("Failed to initialize utilities: " + e.getMessage());
            throw new RuntimeException("Failed to initialize utilities", e);
        }
    }

    public static void connectDatabase() {
        AppContext.db = DBConnection.getInstance();
        LOGGER.info("Database connection established");
    }

    public static void loadServices() {
        AppContext.loginService = new LoginServiceImpl(AppContext.db);
        AppContext.adminService = new AdminServiceImpl(AppContext.db);
        AppContext.beneficiaryService = new BeneficiaryServiceImpl(AppContext.db);
        AppContext.familyMemberService = new FamilyMemberServiceImpl(AppContext.db);
        AppContext.disasterService = new DisasterServiceImpl(AppContext.db);
        AppContext.disasterDamageService = new DisasterDamageServiceImpl(AppContext.db);
        AppContext.aidTypeService = new AidTypeServiceImpl(AppContext.db);
        AppContext.evacSiteService = new EvacSiteServiceImpl(AppContext.db);
        AppContext.dashBoardService = new DashBoardServiceImpl(AppContext.db);
        AppContext.vulnerabilityIndicatorService = new VulnerabilityIndicatorServiceImpl(AppContext.db);
        AppContext.disasterMappingService = new DisasterMappingServiceImpl(AppContext.db);
        AppContext.evacSiteMappingService = new EvacSiteMappingServiceImpl(AppContext.db);

        LOGGER.info("All services loaded successfully");
    }

    public static void prepareUI() throws Exception {
        LOGGER.info("Preloading UI components...");
        preloadScenes();
        preloadDialogs();
        LOGGER.info("UI components preloaded successfully");
    }

    private static void preloadScenes() {
        SceneManager.preload("/view/auth/Login.fxml");
        SceneManager.preload("/view/main/MainScreen.fxml");
        SceneManager.preload("/view/dashboard/Dashboard.fxml");
        SceneManager.preload("/view/admin/ManageAdmins.fxml");
        SceneManager.preload("/view/beneficiary/ManageBeneficiaries.fxml");
        SceneManager.preload("/view/aid/Aid.fxml");
        SceneManager.preload("/view/aid_type/AidType.fxml");
        SceneManager.preload("/view/vulnerability_indicator/VulnerabilityIndicator.fxml");
        SceneManager.preload("/view/family/FamilyMembers.fxml");
        SceneManager.preload("/view/disaster/Disaster.fxml");
        SceneManager.preload("/view/evac_site/EvacSite.fxml");
        SceneManager.preload("/view/disaster_mapping/DisasterMapping.fxml");
        SceneManager.preload("/view/disaster_damage/DisasterDamage.fxml");
        SceneManager.preload("/view/send_sms/SendSMS.fxml");
        SceneManager.preload("/view/settings/Settings.fxml");
    }

    private static void preloadDialogs() throws Exception {
        // Admin dialogs
        DialogManager.preload("addAdmin", "/view/admin/dialog/AddAdminDialog.fxml");
        DialogManager.preload("editAdmin", "/view/admin/dialog/EditAdminDialog.fxml");
        
        // Disaster dialogs
        DialogManager.preload("addDisaster", "/view/disaster/dialog/AddDisasterDialog.fxml");
        DialogManager.preload("editDisaster", "/view/disaster/dialog/EditDisasterDialog.fxml");
        DialogManager.preload("addDisasterDamage", "/view/disaster_damage/dialog/AddDisasterDamageDialog.fxml");
        DialogManager.preload("editDisasterDamage", "/view/disaster_damage/dialog/EditDisasterDamageDialog.fxml");
        
        // Beneficiary dialogs
        DialogManager.preload("addBeneficiary", "/view/beneficiary/dialog/AddBeneficiariesDialog.fxml");
        DialogManager.preload("editBeneficiary", "/view/beneficiary/dialog/EditBeneficiariesDialog.fxml");
        
        // Family member dialogs
        DialogManager.preload("addFamilyMember", "/view/family/dialog/AddFamilyMemberDialog.fxml");
        DialogManager.preload("editFamilyMember", "/view/family/dialog/EditFamilyMemberDialog.fxml");
        
        // Aid type dialogs
        DialogManager.preload("addAidType", "/view/aid_type/dialog/AddAidTypeDialog.fxml");
        DialogManager.preload("editAidType", "/view/aid_type/dialog/EditAidTypeDialog.fxml");

        //Evacuation Plan
        DialogManager.preload("allocate", "/view/evacuation_plan/dialog/AllocateEvacSiteDialog.fxml");
        // Utility dialogs
        DialogManager.preload("mapping", "/view/mapping/MapDialog.fxml");
        DialogManager.preload("beneficiariesInCircle", "/view/disaster_mapping/dialog/BeneficiariesInCircleDialog.fxml");

        // Aid Dialogs
        DialogManager.preload("addAid", "/view/aid/dialog/AddAidDialog.fxml");
        DialogManager.preload("printAidDialog", "/view/aid/dialog/PrintAidDialog.fxml");

        // Evac Site
        DialogManager.preload("addEvacSite", "/view/evac_site/dialog/AddEvacSiteDialog.fxml");
        DialogManager.preload("editEvacSite", "/view/evac_site/dialog/EditEvacSiteDialog.fxml");

        DialogManager.preload("evacuationSiteMapping", "/view/disaster_mapping/dialog/EvacuationSiteMappingDialog.fxml");
        DialogManager.preload("selection", "/view/send_sms/dialog/BeneficiarySelectionDialog.fxml");
    }
}