package com.ionres.respondph.util;

import com.ionres.respondph.admin.AdminService;
import com.ionres.respondph.admin.login.LoginService;
import com.ionres.respondph.aid_type.AidTypeService;
import com.ionres.respondph.beneficiary.BeneficiaryService;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.disaster.DisasterService;
import com.ionres.respondph.disaster_damage.DisasterDamageService;
import com.ionres.respondph.familymembers.FamilyMemberService;

public final class AppContext {
    public static DBConnection db;
    public static LoginService loginService;
    public static AdminService adminService;
    public static BeneficiaryService  beneficiaryService;
    public static FamilyMemberService familyMemberService;
    public static DisasterService disasterService;
    public static DisasterDamageService disasterDamageService;
    public static AidTypeService aidTypeService;

    private AppContext() {}
}
