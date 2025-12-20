package com.ionres.respondph.util;

import com.ionres.respondph.admin.AdminServiceImpl;
import com.ionres.respondph.admin.login.LoginServiceImpl;
import com.ionres.respondph.beneficiary.BeneficiaryServiceImpl;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.familymembers.FamilyMemberServiceImpl;

public class AppLoader {

    public static void initModules() {
        // You can add other module initializations here
    }

    public static void connectDatabase() {
        AppContext.db = DBConnection.getInstance();
    }

    public static void loadServices() {
        AppContext.loginService = new LoginServiceImpl(AppContext.db);
        AppContext.adminService = new AdminServiceImpl(AppContext.db);
        AppContext.beneficiaryService = new BeneficiaryServiceImpl(AppContext.db);
        AppContext.familyMemberService = new FamilyMemberServiceImpl(AppContext.db);
    }

    public static void configureSettings() {
        // Add any configuration or mapping setup
    }

    public static void prepareUI() throws Exception {

    }
}
