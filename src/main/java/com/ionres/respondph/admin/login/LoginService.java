package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;

public interface LoginService {
    public AdminModel login(String usernameInput, String passwordInput) throws Exception;
    
}
