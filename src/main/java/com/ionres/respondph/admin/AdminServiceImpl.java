
package com.ionres.respondph.admin;

import com.ionres.respondph.exception.*;
import com.ionres.respondph.util.Cryptography;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.List;


public class AdminServiceImpl implements  AdminService{
    AdminDAO adminDao = new AdminDAOImpl();
    @Override
    public List<AdminModel> getAllAdmins() {
        List<AdminModel> admins = adminDao.getAll();
        return  admins;
    }

    @Override
    public boolean createAdmin(AdminModel admin) {
       try {
           if (admin.getUsername() == null || admin.getUsername().isBlank()) {
               throw ExceptionFactory.missingField("Username");
           }

           Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

           // Encrypt fields
           String encryptedUsername = cs.encrypt(admin.getUsername());
           String encryptedFname = cs.encrypt(admin.getFirstname());
           String encryptedMname = cs.encrypt(admin.getMiddlename());
           String encryptedLname = cs.encrypt(admin.getLastname());

           // Duplicate check must use encrypted username
           if (adminDao.existsByUsername(encryptedUsername)) {
               throw ExceptionFactory.duplicate("Admin", admin.getUsername());
           }

           // Set encrypted values
           admin.setUsername(encryptedUsername);
           admin.setFirstname(encryptedFname);
           admin.setMiddlename(encryptedMname);
           admin.setLastname(encryptedLname);

           // Hash password
           admin.setPassword(BCrypt.hashpw(admin.getPassword(), BCrypt.gensalt()));

           boolean flag = adminDao.saving(admin);
           if (!flag) {
               throw ExceptionFactory.failedToCreate("Admin");
           }

           return flag;
       } catch (SQLException ex) {
           System.out.println("Error : " + ex);
           return  false;

       } catch (Exception ex) {
           System.out.println("Error : " + ex);
           return  false;
       }
    }


    @Override
    public boolean updateAdmin(AdminModel admin, String confirmPassword) {
        return false;
    }
}
