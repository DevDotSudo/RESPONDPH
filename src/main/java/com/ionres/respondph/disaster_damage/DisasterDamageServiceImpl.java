package com.ionres.respondph.disaster_damage;

import com.ionres.respondph.common.model.BeneficiaryModel;
import com.ionres.respondph.common.model.DisasterModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.exception.ExceptionFactory;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.util.CryptographyManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DisasterDamageServiceImpl implements DisasterDamageService {
    private static final Logger LOGGER = Logger.getLogger(DisasterDamageServiceImpl.class.getName());
    private final DisasterDamageDAO disasterDamageDAO;
    private static final Cryptography CRYPTO = CryptographyManager.getInstance();

    public DisasterDamageServiceImpl(DBConnection dbConnection) {
        this.disasterDamageDAO = new DisasterDamageDAOImpl(dbConnection);
    }
    @Override
    public List<DisasterDamageModel> getAllDisasterDamage() {

        List<DisasterDamageModel> list = disasterDamageDAO.getAll();

        try {
            for (DisasterDamageModel ddm : list) {

                List<String> encrypted = new ArrayList<>();
                encrypted.add(ddm.getBeneficiaryFirstname());
                encrypted.add(ddm.getDisasterType());
                encrypted.add(ddm.getDisasterName());
                encrypted.add(ddm.getHouseDamageSeverity());
                encrypted.add(ddm.getAssessmentDate());
                encrypted.add(ddm.getVerifiedBy());
                encrypted.add(ddm.getNotes());
                encrypted.add(ddm.getRegDate());

                List<String> decrypted = CRYPTO.decrypt(encrypted);

                ddm.setBeneficiaryFirstname(decrypted.get(0));
                ddm.setDisasterType(decrypted.get(1));
                ddm.setDisasterName(decrypted.get(2));
                ddm.setHouseDamageSeverity(decrypted.get(3));
                ddm.setAssessmentDate(decrypted.get(4));
                ddm.setVerifiedBy(decrypted.get(5));
                ddm.setNotes(decrypted.get(6));
                ddm.setRegDate(decrypted.get(7));
            }

        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "Error decrypting disaster damage records: " + e.getMessage()
            );
        }

        return list;
    }


    @Override
    public boolean createDisasterDamage(DisasterDamageModel ddm) {
        try {

            String  encryptedHouseDamage= CRYPTO.encryptWithOneParameter(ddm.getHouseDamageSeverity());
            String encryptedAssessmentDate = CRYPTO.encryptWithOneParameter(ddm.getAssessmentDate());
            String encryptedVerified = CRYPTO.encryptWithOneParameter(ddm.getVerifiedBy());
            String encryptedNotes = CRYPTO.encryptWithOneParameter(ddm.getNotes());
            String encryptedRegDate = CRYPTO.encryptWithOneParameter(ddm.getRegDate());

            boolean flag = disasterDamageDAO.saving(new DisasterDamageModel(ddm.getBeneficiaryId(), ddm.getDisasterId(), encryptedHouseDamage,
                    encryptedAssessmentDate, encryptedVerified, encryptedNotes, encryptedRegDate));

            if (!flag) {
                throw ExceptionFactory.failedToCreate("Disaster");
            }
            return flag;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQL error creating disaster damage", ex);
            return  false;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error creating disaster damage", ex);
            return  false;
        }
    }

    @Override
    public boolean deleteDisasterDamage(DisasterDamageModel ddm) {
        try {
            if (ddm == null || ddm.getDisasterId() <= 0) {
                throw ExceptionFactory.missingField("Disaster Damage ID");
            }

            boolean deleted = disasterDamageDAO.delete(ddm);

            if (!deleted) {
                throw ExceptionFactory.failedToDelete("Disaster Damage");
            }

            return deleted;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error deleting disaster damage", ex);
            return false;
        }
    }

    @Override
    public boolean updateDisasterDamage(DisasterDamageModel ddm) {
        try {
            if (ddm == null || ddm.getBeneficiaryDisasterDamageId() <= 0) {
                throw ExceptionFactory.missingField("Disaster Damage ID");
            }

            String encryptedSeverity =  CRYPTO.encryptWithOneParameter(ddm.getHouseDamageSeverity());
            String encryptedAssessmentDate = CRYPTO.encryptWithOneParameter(ddm.getAssessmentDate());
            String encryptedVerifiedBy = CRYPTO.encryptWithOneParameter(ddm.getVerifiedBy());
            String encryptedNotes = CRYPTO.encryptWithOneParameter(ddm.getNotes());
            String encryptedRegDate = CRYPTO.encryptWithOneParameter(ddm.getRegDate());
            DisasterDamageModel encrypted = new DisasterDamageModel();

            encrypted.setBeneficiaryDisasterDamageId(
                    ddm.getBeneficiaryDisasterDamageId()
            );
            encrypted.setBeneficiaryId(ddm.getBeneficiaryId());
            encrypted.setDisasterId(ddm.getDisasterId());

            encrypted.setHouseDamageSeverity(encryptedSeverity);
            encrypted.setAssessmentDate(encryptedAssessmentDate);
            encrypted.setVerifiedBy(encryptedVerifiedBy);
            encrypted.setNotes(encryptedNotes);
            encrypted.setRegDate(encryptedRegDate);

            boolean updated = disasterDamageDAO.update(encrypted);

            if (!updated) {
                throw ExceptionFactory.failedToUpdate("Disaster Damage");
            }

            return true;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error updating disaster damage", ex);
            ex.printStackTrace();
            return false;
        }
    }


    @Override
    public DisasterDamageModel getDisasterDamageId(int id) {
        try {
            DisasterDamageModel ddm = disasterDamageDAO.getById(id);
            if (ddm == null) {
                LOGGER.warning("Disaster Damage not found with ID: " + id);
                return null;
            }

            List<String> encrypted = List.of(
                    ddm.getBeneficiaryFirstname(),
                    ddm.getDisasterType(),
                    ddm.getDisasterName(),
                    ddm.getHouseDamageSeverity(),
                    ddm.getAssessmentDate(),
                    ddm.getVerifiedBy(),
                    ddm.getNotes(),
                    ddm.getRegDate()
            );

            List<String> decrypted = CRYPTO.decrypt(encrypted);

            ddm.setBeneficiaryFirstname(decrypted.get(0));
            ddm.setDisasterType(decrypted.get(1));
            ddm.setDisasterName(decrypted.get(2));
            ddm.setHouseDamageSeverity(decrypted.get(3));
            ddm.setAssessmentDate(decrypted.get(4));
            ddm.setVerifiedBy(decrypted.get(5));
            ddm.setNotes(decrypted.get(6));
            ddm.setRegDate(decrypted.get(7));

            return ddm;

        } catch (Exception ex) {
            ex.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "Error fetching/decrypting disaster damage record: " + ex.getMessage()
            );
            return null;
        }
    }


    @Override
    public List<BeneficiaryModel> getAllBeneficiaries() {
        try {
            List<BeneficiaryModel> encryptedList = disasterDamageDAO.getAllBeneficiaryByFirstname();

            List<String> encryptedNames = new ArrayList<>();
            for (BeneficiaryModel b : encryptedList) {
                encryptedNames.add(b.getFirstName());
            }

            List<String> decryptedNames = CRYPTO.decrypt(encryptedNames);

            List<BeneficiaryModel> decryptedModels = new ArrayList<>();
            for (int i = 0; i < encryptedList.size(); i++) {
                BeneficiaryModel original = encryptedList.get(i);
                decryptedModels.add(new BeneficiaryModel(
                        original.getBeneficiaryId(),
                        decryptedNames.get(i)
                ));
            }
            return decryptedModels;
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<DisasterModel> getALlDisaster() {
        try {
            List<DisasterModel> encryptedList =
                    disasterDamageDAO.getAllDisasterTypeAndName();


            List<String> encryptedValues = new ArrayList<>();
            for (DisasterModel d : encryptedList) {
                encryptedValues.add(d.getDisasterType());
                encryptedValues.add(d.getDisasterName());
            }

            List<String> decryptedValues = CRYPTO.decrypt(encryptedValues);


            List<DisasterModel> decryptedModels = new ArrayList<>();

            int index = 0;
            for (DisasterModel original : encryptedList) {
                String decryptedType = decryptedValues.get(index++);
                String decryptedName = decryptedValues.get(index++);

                decryptedModels.add(new DisasterModel(
                        original.getDisasterId(),
                        decryptedType,
                        decryptedName
                ));
            }

            return decryptedModels;

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ArrayList<>();
        }
    }


    @Override
    public List<DisasterDamageModel> searchDisasterDamage(String searchTxt) {
        List<DisasterDamageModel> allDisasterDamage = getAllDisasterDamage();
        List<DisasterDamageModel> filteredDisasterDamage = new ArrayList<>();

        if (searchTxt == null || searchTxt.trim().isEmpty()) {
            return allDisasterDamage;
        }

        String search = searchTxt.toLowerCase().trim();

        for (DisasterDamageModel ddm : allDisasterDamage) {
            boolean matchesId = String.valueOf(ddm.getBeneficiaryDisasterDamageId()).contains(search);
            boolean matchesBeneficiaryId = String.valueOf(ddm.getBeneficiaryId()).contains(search);
            boolean matchesBeneficiaryName = ddm.getBeneficiaryFirstname() != null &&
                    ddm.getBeneficiaryFirstname().toLowerCase().contains(search);
            boolean matchesDisasterId = String.valueOf(ddm.getDisasterId()).contains(search);
            boolean matchesDisasterType = ddm.getDisasterType() != null &&
                    ddm.getDisasterType().toLowerCase().contains(search);
            boolean matchesDisasterName = ddm.getDisasterName() != null &&
                    ddm.getDisasterName().toLowerCase().contains(search);
            boolean matchesSeverity = ddm.getHouseDamageSeverity() != null &&
                    ddm.getHouseDamageSeverity().toLowerCase().contains(search);
            boolean matchesVerifiedBy = ddm.getVerifiedBy() != null &&
                    ddm.getVerifiedBy().toLowerCase().contains(search);
            boolean matchesAssessmentDate = ddm.getAssessmentDate() != null &&
                    ddm.getAssessmentDate().toLowerCase().contains(search);
            boolean matchesNotes = ddm.getNotes() != null &&
                    ddm.getNotes().toLowerCase().contains(search);

            if (matchesId || matchesBeneficiaryId || matchesBeneficiaryName ||
                    matchesDisasterId || matchesDisasterType || matchesDisasterName ||
                    matchesSeverity || matchesVerifiedBy || matchesAssessmentDate || matchesNotes) {
                filteredDisasterDamage.add(ddm);
            }
        }

        return filteredDisasterDamage;
    }

}
