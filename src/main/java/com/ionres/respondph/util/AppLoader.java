    package com.ionres.respondph.util;

    import com.ionres.respondph.admin.AdminServiceImpl;
    import com.ionres.respondph.admin.login.LoginServiceImpl;
    import com.ionres.respondph.aid_type.AidTypeServiceImpl;
    import com.ionres.respondph.beneficiary.BeneficiaryServiceImpl;
    import com.ionres.respondph.database.DBConnection;
    import com.ionres.respondph.disaster.DisasterServiceImpl;
    import com.ionres.respondph.disaster_damage.DisasterDamageServiceImpl;
    import com.ionres.respondph.familymembers.FamilyMemberServiceImpl;

    public class AppLoader {

        public static void initModules() {
            // Module initialization logic
        }

        public static void connectDatabase() {
            AppContext.db = DBConnection.getInstance();
        }

        public static void loadServices() {
            AppContext.loginService = new LoginServiceImpl(AppContext.db);
            AppContext.adminService = new AdminServiceImpl(AppContext.db);
            AppContext.beneficiaryService = new BeneficiaryServiceImpl(AppContext.db);
            AppContext.familyMemberService = new FamilyMemberServiceImpl(AppContext.db);
            AppContext.disasterService  = new DisasterServiceImpl(AppContext.db);
            AppContext.disasterDamageService = new DisasterDamageServiceImpl(AppContext.db);
            AppContext.aidTypeService = new AidTypeServiceImpl(AppContext.db);
         }

        public static void configureSettings() {
            // Configuration and mapping setup
        }

        public static void prepareUI() throws Exception {

            //load pages
            SceneManager.preload("/view/auth/Login.fxml");
            SceneManager.preload("/view/main/MainScreen.fxml");
            SceneManager.preload("/view/dashboard/Dashboard.fxml");
            SceneManager.preload("/view/admin/ManageAdmins.fxml");
            SceneManager.preload("/view/beneficiary/ManageBeneficiaries.fxml");
            SceneManager.preload("/view/aid_type/AidType.fxml");
            SceneManager.preload("/view/family/FamilyMembers.fxml");
            SceneManager.preload("/view/disaster/Disaster.fxml");
            SceneManager.preload("/view/disaster_damage/DisasterDamage.fxml");
            SceneManager.preload("/view/send_sms/SendSMS.fxml");
            SceneManager.preload("/view/settings/Settings.fxml");

            //load dialog
            DialogManager.preload("addAdmin", "/view/admin/dialog/AddAdminDialog.fxml");
            DialogManager.preload("editAdmin", "/view/admin/dialog/EditAdminDialog.fxml");
            DialogManager.preload("addDisasterDamage", "/view/disaster_damage/dialog/AddDisasterDamageDialog.fxml");
            DialogManager.preload("editDisasterDamage", "/view/disaster_damage/dialog/EditDisasterDamageDialog.fxml");
            DialogManager.preload("addDisaster", "/view/disaster/dialog/AddDisasterDialog.fxml");
            DialogManager.preload("editDisaster", "/view/disaster/dialog/EditDisasterDialog.fxml");
            DialogManager.preload("addBeneficiary", "/view/beneficiary/dialog/AddBeneficiariesDialog.fxml");
            DialogManager.preload("editBeneficiary", "/view/beneficiary/dialog/EditBeneficiariesDialog.fxml");
            DialogManager.preload("addFamilyMember", "/view/family/dialog/AddFamilyMemberDialog.fxml");
            DialogManager.preload("editFamilyMember", "/view/family/dialog/EditFamilyMemberDialog.fxml");
            DialogManager.preload("addAidType", "/view/aid_type/dialog/AddAidTypeDialog.fxml");
            DialogManager.preload("editAidType", "/view/aid_type/dialog/EditAidTypeDialog.fxml");
        }
    }