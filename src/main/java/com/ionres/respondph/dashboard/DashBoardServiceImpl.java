package com.ionres.respondph.dashboard;

import com.ionres.respondph.common.model.*;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.util.CryptographyManager;
import com.ionres.respondph.util.NameDecryptionUtils;
import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashBoardServiceImpl implements DashBoardService {
    private static final Logger LOGGER = Logger.getLogger(DashBoardServiceImpl.class.getName());
    private final DashBoardDAO dashBoardDAO;
    private static final Cryptography CRYPTO = CryptographyManager.getInstance();

    public DashBoardServiceImpl(DBConnection connection) {
        this.dashBoardDAO = new DashBoardDAOImpl(connection);
    }

    // ── Counts ────────────────────────────────────────────────────────────────

    @Override
    public int fetchTotalBeneficiary() { return dashBoardDAO.getTotalBeneficiaries(); }

    @Override
    public int fetchTotalDisasters() { return dashBoardDAO.getTotalDisasters(); }

    @Override
    public int fetchTotalAids() { return dashBoardDAO.getTotalAids(); }

    @Override
    public int fetchTotalEvacuationSites() { return dashBoardDAO.getTotalEvacutaionSites(); }

    // ── Map data ──────────────────────────────────────────────────────────────

    @Override
    public List<DisasterCircleInfo> getCircles() {
        List<DisasterCircleInfo> result = new ArrayList<>();
        for (DisasterCircleEncrypted e : dashBoardDAO.fetchAllEncrypted()) {
            try {
                double lat    = Double.parseDouble(CRYPTO.decryptWithOneParameter(e.lat));
                double lon    = Double.parseDouble(CRYPTO.decryptWithOneParameter(e.lon));
                double radius = Double.parseDouble(CRYPTO.decryptWithOneParameter(e.radius));
                result.add(new DisasterCircleInfo(lat, lon, radius, "", ""));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error decrypting circle coordinates", ex);
            }
        }
        return result;
    }

    @Override
    public List<BeneficiaryMarker> getBeneficiaries() {
        List<BeneficiaryMarker> result = new ArrayList<>();
        for (BeneficiariesMappingModel b : dashBoardDAO.fetchAllBeneficiaries()) {
            try {
                double lat       = Double.parseDouble(CRYPTO.decryptWithOneParameter(b.getLat()));
                double lon       = Double.parseDouble(CRYPTO.decryptWithOneParameter(b.getLng()));
                String fullName  = NameDecryptionUtils.decryptFullName(b.getBeneficiaryName());
                result.add(new BeneficiaryMarker(b.getId(), fullName, lat, lon));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error decrypting beneficiary (ID: " + b.getId() + ")", ex);
            }
        }
        return result;
    }

    @Override
    public List<EvacSiteMarker> getEvacSites() {
        List<EvacSiteMarker> result = new ArrayList<>();
        for (EvacSiteMappingModel e : dashBoardDAO.fetchAllEvacSites()) {
            try {
                double lat   = Double.parseDouble(e.getLat());
                double lon   = Double.parseDouble(e.getLng());
                String name  = CRYPTO.decryptWithOneParameter(e.getName());
                int capacity = Integer.parseInt(e.getCapacity());
                result.add(new EvacSiteMarker(e.getEvacId(), lat, lon, name, capacity));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error processing evac site (ID: " + e.getEvacId() + ")", ex);
            }
        }
        return result;
    }

    @Override
    public List<FamilyMemberModel> getFamilyMembers(int beneficiaryId) {
        List<FamilyMemberModel> result = new ArrayList<>();
        for (FamilyMemberModel raw : dashBoardDAO.fetchFamilyMembers(beneficiaryId)) {
            try {
                String firstName  = safeDecrypt(raw.getFirstName());
                String middleName = safeDecrypt(raw.getMiddleName());
                String lastName   = safeDecrypt(raw.getLastName());
                result.add(new FamilyMemberModel(
                        raw.getFamilyMemberId(), firstName, middleName, lastName));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error decrypting family member (ID: " + raw.getFamilyMemberId() + ")", ex);
            }
        }
        return result;
    }

    // ── Change password ───────────────────────────────────────────────────────

    /**
     * 1. Fetches the stored BCrypt hash for {@code adminId}.
     * 2. Verifies {@code currentPassword} against it with BCrypt.checkpw().
     * 3. If valid, hashes {@code newPassword} with a fresh salt and persists it.
     *
     * Returns false if:
     *   – the admin record is not found
     *   – the current password does not match
     *   – a database error occurs
     */
    @Override
    public boolean changePassword(int adminId, String currentPassword, String newPassword) {
        try {
            // Step 1: retrieve stored hash
            String storedHash = dashBoardDAO.getPasswordHashById(adminId);
            if (storedHash == null || storedHash.isBlank()) {
                LOGGER.warning("No password hash found for admin ID " + adminId);
                return false;
            }

            // Step 2: verify current password against stored BCrypt hash
            if (!BCrypt.checkpw(currentPassword, storedHash)) {
                LOGGER.info("Change-password failed: wrong current password for admin ID " + adminId);
                return false;
            }

            // Step 3: hash new password with a fresh salt and persist
            String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            boolean updated = dashBoardDAO.updatePassword(adminId, newHash);

            if (updated) {
                LOGGER.info("Password updated successfully for admin ID " + adminId);
            } else {
                LOGGER.warning("updatePassword returned false for admin ID " + adminId);
            }
            return updated;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during changePassword for admin ID " + adminId, e);
            return false;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String safeDecrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) return null;
        try {
            return CRYPTO.decryptWithOneParameter(encrypted);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Decryption failed, returning null", e);
            return null;
        }
    }
}