package com.ionres.respondph.util;

import com.ionres.respondph.admin.AdminService;
import com.ionres.respondph.admin.login.LoginService;
import com.ionres.respondph.aid_type.AidTypeService;
import com.ionres.respondph.beneficiary.BeneficiaryService;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.disaster.DisasterService;
import com.ionres.respondph.disaster_damage.DisasterDamageService;
import com.ionres.respondph.disaster_mapping.DisasterMappingService;
import com.ionres.respondph.familymembers.FamilyMemberService;
import com.ionres.respondph.dashboard.DashBoardService;
import com.ionres.respondph.vulnerability_indicator.VulnerabilityIndicatorService;

/**
 * Application context that holds shared services and resources.
 * All services are initialized during application startup in AppLoader.
 * This follows the Service Locator pattern for dependency management.
 */
public final class AppContext {
    // Database connection (singleton)
    public static DBConnection db;
    
    // Application services (initialized in AppLoader.loadServices())
    public static LoginService loginService;
    public static AdminService adminService;
    public static BeneficiaryService beneficiaryService;
    public static FamilyMemberService familyMemberService;
    public static DisasterService disasterService;
    public static DisasterDamageService disasterDamageService;
    public static AidTypeService aidTypeService;
    public static DashBoardService dashBoardService;
    public static VulnerabilityIndicatorService vulnerabilityIndicatorService;
    public static DisasterMappingService disasterMappingService;

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class that only contains static fields.
     */
    private AppContext() {
        throw new AssertionError("AppContext should not be instantiated");
    }
}
