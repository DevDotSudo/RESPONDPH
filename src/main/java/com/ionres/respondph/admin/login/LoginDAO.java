package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;

public interface LoginDAO {
    boolean saveAdmin(AdminModel adminModel);
    boolean adminLogin(String username, String password);
    public void fetchAdmin();
    public void getAdminByUsername(String username);
}
