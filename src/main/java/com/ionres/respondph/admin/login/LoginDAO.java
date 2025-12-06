package com.ionres.respondph.admin.login;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.util.Cryptography;

public interface LoginDAO {
    public AdminModel findByUsernameToLogin(String usernameInput, Cryptography cs);
    
}
