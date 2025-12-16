
package com.ionres.respondph.admin;

import com.ionres.respondph.exception.*;
import com.ionres.respondph.util.Cryptography;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;
import java.time.LocalDate;
import java.util.List;
import java.sql.SQLException;


public class AdminServiceImpl implements  AdminService{
    AdminDAO adminDao = new AdminDAOImpl();


    @Override
    public List<AdminModel> getAllAdmins() {
        List<AdminModel> admins = adminDao.getAll();
        System.out.println(generateAdminID());
        return admins;
    }

    @Override
    public boolean createAdmin(AdminModel admin) {
        try {
            if (admin.getUsername() == null || admin.getUsername().isBlank()) {
                throw ExceptionFactory.missingField("Username");
            }

            Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

            List<String> encryptedData = cs.encrypt(admin.getUsername(), admin.getFirstname(), admin.getMiddlename(), admin.getLastname(), admin.getRegDate());
            String hashedPassword = BCrypt.hashpw(admin.getPassword(), BCrypt.gensalt());

            String encryptedUsername = encryptedData.get(0);
            String encryptedFname = encryptedData.get(1);
            String encryptedMname = encryptedData.get(2);
            String encryptedLname = encryptedData.get(3);
            String encryptedRegDate = encryptedData.get(4);

            if (adminDao.existsByUsername(encryptedUsername)) {
                throw ExceptionFactory.duplicate("Admin", admin.getUsername());
            }

            boolean flag = adminDao.saving(new AdminModel(encryptedUsername, encryptedFname, encryptedMname, encryptedLname, encryptedRegDate, hashedPassword));
            if (!flag) {
                throw ExceptionFactory.failedToCreate("Admin");
            }
            return flag;
        } catch (SQLException ex) {
            System.out.println("Error : " + ex);
            return  false;

        } catch (Exception ex) {
            System.out.println("Error: " + ex);
            return  false;
        }
    }

    @Override
    public String generateAdminID() {
        String year = String.valueOf(LocalDate.now().getYear());
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "ADMIN-" + year + "-" + random;
    }

    @Override
    public boolean deleteAdmin(AdminModel admin) {
        try {
            if (admin == null || admin.getId() <= 0) {
                throw ExceptionFactory.missingField("Admin ID");
            }

            boolean deleted = adminDao.delete(admin);

            if (!deleted) {
                throw ExceptionFactory.failedToDelete("Admin");
            }
            return deleted;
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateAdmin(AdminModel admin) {
        try {
            if (admin.getUsername() == null || admin.getUsername().isBlank()) {
                throw ExceptionFactory.missingField("Username");
            }

            Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

            List<String> encryptedData = cs.encryptUpdate(admin.getUsername(), admin.getFirstname(), admin.getMiddlename(), admin.getLastname(), admin.getRegDate());

            String encryptedUsername = encryptedData.get(0);
            String encryptedFname = encryptedData.get(1);
            String encryptedMname = encryptedData.get(2);
            String encryptedLname = encryptedData.get(3);
            String encryptedRegDate = encryptedData.get(4);

            admin.setUsername(encryptedUsername);
            admin.setFirstname(encryptedFname);
            admin.setMiddlename(encryptedMname);
            admin.setLastname(encryptedLname);
            admin.setRegDate(encryptedRegDate);


            boolean flag = adminDao.update(admin);
            if (!flag) {
                throw ExceptionFactory.failedToCreate("Admin");
            }
            return flag;
        } catch (SQLException ex) {
            System.out.println("Error : " + ex);
            return  false;

        } catch (Exception ex) {
            System.out.println("Error: " + ex);
            return  false;
        }
    }

    @Override
    public List<AdminModel> searchAdmin(String searchText) {

        List<AdminModel> allAdmins = getAllAdmins();
        List<AdminModel> filteredAdmins = new ArrayList<>();

        for (AdminModel admin : allAdmins) {
            if (admin.getUsername().toLowerCase().contains(searchText.toLowerCase()) ||
                    admin.getFirstname().toLowerCase().contains(searchText.toLowerCase()) ||
                    admin.getLastname().toLowerCase().contains(searchText.toLowerCase())) {
                filteredAdmins.add(admin);
            }
        }
        return filteredAdmins;
    }



}
