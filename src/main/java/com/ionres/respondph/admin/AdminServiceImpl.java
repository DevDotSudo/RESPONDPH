
package com.ionres.respondph.admin;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.exception.*;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.util.CryptographyManager;
import org.mindrot.jbcrypt.BCrypt;
import java.util.ArrayList;
import java.util.UUID;
import java.time.LocalDate;
import java.util.List;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminServiceImpl implements AdminService {
    private static final Logger LOGGER = Logger.getLogger(AdminServiceImpl.class.getName());
    private final AdminDAO adminDao;
    private static final Cryptography CRYPTO = CryptographyManager.getInstance();

    public AdminServiceImpl(DBConnection dbConnection) {
        this.adminDao = new AdminDAOImpl(dbConnection);
    }

    @Override
    public List<AdminModel> getAllAdmins() {

        try {
            List<AdminModel> encryptedAdmins = adminDao.getAll();
            List<AdminModel> decryptedAdmins = new ArrayList<>();

            for (AdminModel a : encryptedAdmins) {

                List<String> encrypted = List.of(
                        a.getUsername(),
                        a.getFirstname(),
                        a.getMiddlename(),
                        a.getLastname(),
                        a.getRegDate()
                );

                List<String> decrypted = CRYPTO.decrypt(encrypted);

                AdminModel d = new AdminModel();
                d.setId(a.getId());
                d.setUsername(decrypted.get(0));
                d.setFirstname(decrypted.get(1));
                d.setMiddlename(decrypted.get(2));
                d.setLastname(decrypted.get(3));
                d.setRegDate(decrypted.get(4));

                decryptedAdmins.add(d);
            }

            return decryptedAdmins;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error fetching all admins", ex);
            return List.of();
        }
    }

    @Override
    public boolean createAdmin(AdminModel admin) {
        try {
            if (admin.getUsername() == null || admin.getUsername().isBlank()) {
                throw ExceptionFactory.missingField("Username");
            }


            List<String> encryptedData = CRYPTO.encrypt(admin.getUsername(), admin.getFirstname(), admin.getMiddlename(), admin.getLastname(), admin.getRegDate());
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
            LOGGER.log(Level.SEVERE, "SQL error creating admin", ex);
            return false;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error creating admin", ex);
            return false;
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
            LOGGER.log(Level.SEVERE, "Error deleting admin", ex);
            return false;
        }
    }

    @Override
    public boolean updateAdmin(AdminModel admin) {
        try {
            if (admin.getUsername() == null || admin.getUsername().isBlank()) {
                throw ExceptionFactory.missingField("Username");
            }


            List<String> encryptedData = CRYPTO.encryptUpdate(admin.getUsername(), admin.getFirstname(), admin.getMiddlename(), admin.getLastname());

            String encryptedUsername = encryptedData.get(0);
            String encryptedFname = encryptedData.get(1);
            String encryptedMname = encryptedData.get(2);
            String encryptedLname = encryptedData.get(3);

            admin.setUsername(encryptedUsername);
            admin.setFirstname(encryptedFname);
            admin.setMiddlename(encryptedMname);
            admin.setLastname(encryptedLname);


            boolean flag = adminDao.update(admin);
            if (!flag) {
                throw ExceptionFactory.failedToCreate("Admin");
            }
            return flag;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQL error updating admin", ex);
            return false;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error updating admin", ex);
            return false;
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
