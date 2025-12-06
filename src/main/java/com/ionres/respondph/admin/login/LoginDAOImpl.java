package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;

public class LoginDAOImpl implements LoginDAO{

    @Override
    public boolean saveAdmin(AdminModel adminModel) {
        return true;
    }

    @Override
    public boolean adminLogin(String username, String password) {
        return false;
    }

    @Override
    public void fetchAdmin() {

    }

    @Override
    public void getAdminByUsername(String username) {

    }
}
