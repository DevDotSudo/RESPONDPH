
package com.ionres.respondph.admin;

import com.ionres.respondph.exception.ExceptionFactory;
import org.mindrot.jbcrypt.BCrypt;
import java.util.UUID;
import java.time.LocalDate;
import java.util.List;


public class AdminServiceImpl implements  AdminService{
    AdminDAO adminDao = new AdminDAOImpl();


    @Override
    public List<AdminModel> getAllAdmins() {
        List<AdminModel> admins = adminDao.getAll();
        return admins;
    }

    @Override
    public boolean createAdmin(AdminModel admin) {
        if (admin.getUsername() == null || admin.getUsername().isBlank()) {
            throw ExceptionFactory.missingField("Username");
        }

        if (adminDao.existsByUsername(admin.getUsername())) {
            throw ExceptionFactory.duplicate("Admin", admin.getUsername());
        }
        boolean flag = adminDao.saving(admin);

        if (!flag) {
            throw ExceptionFactory.failedToCreate("Admin");
        }
        return flag;
    }

    @Override
    public boolean updateAdmin(AdminModel admin, String confirmPassword) {
        return false;
    }

    @Override
    public String generateAdminID() {
        String year = String.valueOf(LocalDate.now().getYear());
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "ADMIN-" + year + "-" + random;
    }
}
