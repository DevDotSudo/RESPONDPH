    package com.ionres.respondph.util;

    import com.ionres.respondph.admin.AdminServiceImpl;
    import com.ionres.respondph.admin.login.LoginServiceImpl;
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
        }

        public static void configureSettings() {
            // Configuration and mapping setup
        }

        public static void prepareUI() throws Exception {

            //load pages
            SceneManager.preload("/view/pages/LoginFrame.fxml");
            SceneManager.preload("/view/pages/MainFrame.fxml");
            SceneManager.preload("/view/pages/Dashboard.fxml");
            SceneManager.preload("/view/pages/ManageAdmins.fxml");
            SceneManager.preload("/view/pages/ManageBeneficiaries.fxml");
            SceneManager.preload("/view/pages/Aids.fxml");
            SceneManager.preload("/view/pages/FamilyMembers.fxml");
            SceneManager.preload("/view/pages/Disaster.fxml");
            SceneManager.preload("/view/pages/DisasterDamage.fxml");
            SceneManager.preload("/view/pages/SendSMS.fxml");
            SceneManager.preload("/view/pages/Settings.fxml");

            //load dialogs
            DialogManager.preload("addAdmin", "/view/dialogs/AddAdminDialog.fxml");
            DialogManager.preload("editAdmin", "/view/dialogs/EditAdminDialog.fxml");
            DialogManager.preload("addDisasterDamage","/view/dialogs/AddDisasterDamageDialog.fxml");
            DialogManager.preload("editDisasterDamage","/view/dialogs/EditDisasterDamageDialog.fxml");
            DialogManager.preload("addDisaster","/view/dialogs/AddDisasterDialog.fxml");
            DialogManager.preload("editDisaster","/view/dialogs/EditDisasterDialog.fxml");
            DialogManager.preload("addBeneficiary","/view/dialogs/AddBeneficiariesDialog.fxml");
            DialogManager.preload("editBeneficiary","/view/dialogs/EditBeneficiariesDialog.fxml");
            DialogManager.preload("addFamilyMember","/view/dialogs/AddFamilyMemberDialog.fxml");
            DialogManager.preload("editFamilyMember","/view/dialogs/EditFamilyMemberDialog.fxml");
        }
    }