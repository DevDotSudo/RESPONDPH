package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.util.Cryptography;

public interface LoginDAO {
        AdminModel findByUsernameToLogin(String usernameInput, Cryptography cs);
        void saveRememberMeToken(int adminId, String token);
        AdminModel findByRememberMeToken(String token, Cryptography cs);
}
