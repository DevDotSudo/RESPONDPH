package com.ionres.respondph.admin;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.exception.*;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.util.CryptographyManager;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminServiceImpl implements AdminService {

    private static final Logger LOGGER = Logger.getLogger(AdminServiceImpl.class.getName());
    private final AdminDAO adminDao;
    private static final Cryptography CRYPTO = CryptographyManager.getInstance();

    public AdminServiceImpl(DBConnection dbConnection) {
        this.adminDao = new AdminDAOImpl(dbConnection);
    }

    // ─── GET ALL ──────────────────────────────────────────────────────────────

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
                        a.getRegDate(),
                        a.getRole()
                );

                List<String> decrypted = CRYPTO.decrypt(encrypted);

                AdminModel d = new AdminModel();
                d.setId(a.getId());
                d.setUsername(decrypted.get(0));
                d.setFirstname(decrypted.get(1));
                d.setMiddlename(decrypted.get(2));
                d.setLastname(decrypted.get(3));
                d.setRegDate(decrypted.get(4));
                d.setRole(decrypted.get(5)); // role is not encrypted

                decryptedAdmins.add(d);
            }

            return decryptedAdmins;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error fetching all admins", ex);
            return List.of();
        }
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Override
    public boolean createAdmin(AdminModel admin) {
        try {
            if (admin.getUsername() == null || admin.getUsername().isBlank()) {
                throw ExceptionFactory.missingField("Username");
            }
            if (admin.getRole() == null || admin.getRole().isBlank()) {
                throw ExceptionFactory.missingField("Role");
            }

            List<String> encryptedData = CRYPTO.encrypt(
                    admin.getUsername(),
                    admin.getFirstname(),
                    admin.getMiddlename(),
                    admin.getLastname(),
                    admin.getRegDate(),
                    admin.getRole()
            );

            String hashedPassword    = BCrypt.hashpw(admin.getPassword(), BCrypt.gensalt());
            String encryptedUsername = encryptedData.get(0);
            String encryptedFname   = encryptedData.get(1);
            String encryptedMname   = encryptedData.get(2);
            String encryptedLname   = encryptedData.get(3);
            String encryptedRegDate = encryptedData.get(4);
            String encryptedRole     = encryptedData.get(5);

            if (adminDao.existsByUsername(encryptedUsername)) {
                throw ExceptionFactory.duplicate("Admin", admin.getUsername());
            }

            // Build model with role — role is stored as plain text (not encrypted)
            AdminModel toSave = new AdminModel(
                    encryptedUsername,
                    encryptedFname,
                    encryptedMname,
                    encryptedLname,
                    encryptedRegDate,
                    hashedPassword,
                    encryptedRole     // plain text role
            );

            boolean saved = adminDao.saving(toSave);
            if (!saved) {
                throw ExceptionFactory.failedToCreate("Admin");
            }
            return true;

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQL error creating admin", ex);
            return false;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error creating admin", ex);
            return false;
        }
    }

    // ─── GENERATE ID ──────────────────────────────────────────────────────────

    @Override
    public String generateAdminID() {
        String year   = String.valueOf(LocalDate.now().getYear());
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "ADMIN-" + year + "-" + random;
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

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
            return true;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error deleting admin", ex);
            return false;
        }
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @Override
    public boolean updateAdmin(AdminModel admin) {
        try {
            if (admin.getUsername() == null || admin.getUsername().isBlank()) {
                throw ExceptionFactory.missingField("Username");
            }
            if (admin.getRole() == null || admin.getRole().isBlank()) {
                throw ExceptionFactory.missingField("Role");
            }

            List<String> encryptedData = CRYPTO.encryptUpdate(
                    admin.getUsername(),
                    admin.getFirstname(),
                    admin.getMiddlename(),
                    admin.getLastname(),
                    admin.getRole()
            );

            admin.setUsername(encryptedData.get(0));
            admin.setFirstname(encryptedData.get(1));
            admin.setMiddlename(encryptedData.get(2));
            admin.setLastname(encryptedData.get(3));
            admin.setRole(encryptedData.get(4));
            // role stays as-is (plain text, already set on the model)

            boolean updated = adminDao.update(admin);
            if (!updated) {
                throw ExceptionFactory.failedToCreate("Admin");
            }
            return true;

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQL error updating admin", ex);
            return false;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error updating admin", ex);
            return false;
        }
    }

    // ─── SEARCH ───────────────────────────────────────────────────────────────

    @Override
    public List<AdminModel> searchAdmin(String searchText) {
        List<AdminModel> allAdmins     = getAllAdmins();
        List<AdminModel> filtered = new ArrayList<>();
        String lower = searchText.toLowerCase();

        for (AdminModel admin : allAdmins) {
            if (admin.getUsername().toLowerCase().contains(lower)  ||
                    admin.getFirstname().toLowerCase().contains(lower) ||
                    admin.getLastname().toLowerCase().contains(lower)  ||
                    (admin.getRole() != null && admin.getRole().toLowerCase().contains(lower))) {
                filtered.add(admin);
            }
        }
        return filtered;
    }
}