package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;

public interface LoginService {
    AdminModel login(String usernameInput, String passwordInput) throws Exception;
    String createRememberMeToken(int adminId) throws Exception;
    AdminModel loginWithToken(String token) throws Exception;
}