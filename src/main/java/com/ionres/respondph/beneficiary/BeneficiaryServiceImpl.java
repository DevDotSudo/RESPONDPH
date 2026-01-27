package com.ionres.respondph.beneficiary;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.exception.ExceptionFactory;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.util.CryptographyManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BeneficiaryServiceImpl implements BeneficiaryService {
    private static final Logger LOGGER = Logger.getLogger(BeneficiaryServiceImpl.class.getName());
    private final BeneficiaryDAO beneficiaryDAO;
    private static final Cryptography CRYPTO = CryptographyManager.getInstance();

    public BeneficiaryServiceImpl(DBConnection dbConnection) {
        this.beneficiaryDAO = new BeneficiaryDAOImpl(dbConnection);
    }

    @Override
    public List<BeneficiaryModel> getAllBeneficiary() {
        try {
            List<BeneficiaryModel> encryptedList = beneficiaryDAO.getAll();
            List<BeneficiaryModel> decryptedList = new ArrayList<>();

            for (BeneficiaryModel bm : encryptedList) {
                List<String> decrypted = CRYPTO.decrypt(List.of(
                        bm.getFirstname(),
                        bm.getMiddlename(),
                        bm.getLastname(),
                        bm.getBirthDate(),
                        bm.getGender(),
                        bm.getMaritalStatus(),
                        bm.getMobileNumber(),
                        bm.getAddedBy(),
                        bm.getRegDate()
                ));

                BeneficiaryModel d = new BeneficiaryModel();
                d.setId(bm.getId());
                d.setFirstname(decrypted.get(0));
                d.setMiddlename(decrypted.get(1));
                d.setLastname(decrypted.get(2));
                d.setBirthDate(decrypted.get(3));
                d.setAgeScore(bm.getAgeScore()); // Age score is not encrypted
                d.setGender(decrypted.get(4));
                d.setMaritalStatus(decrypted.get(5));
                d.setMobileNumber(decrypted.get(6));
                d.setAddedBy(decrypted.get(7));
                d.setRegDate(decrypted.get(8));

                decryptedList.add(d);
            }

            return decryptedList;

        } catch (Exception ex) {
            ex.printStackTrace();
            return List.of();
        }
    }

    @Override
    public boolean createBeneficiary(BeneficiaryModel bm) {
        try {
            String encryptedFirstname = CRYPTO.encryptWithOneParameter(bm.getFirstname());
            String encryptedMiddlename = CRYPTO.encryptWithOneParameter(bm.getMiddlename());
            String encryptedLastname = CRYPTO.encryptWithOneParameter(bm.getLastname());
            String encryptedBirthDate = CRYPTO.encryptWithOneParameter(bm.getBirthDate());
            String encryptedGender = CRYPTO.encryptWithOneParameter(bm.getGender());
            String encryptedMaritalStatus = CRYPTO.encryptWithOneParameter(bm.getMaritalStatus());
            String encryptedSoloParentStatus = CRYPTO.encryptWithOneParameter(bm.getSoloParentStatus());
            String encryptedLatitude = CRYPTO.encryptWithOneParameter(bm.getLatitude());
            String encryptedLongitude = CRYPTO.encryptWithOneParameter(bm.getLongitude());
            String encryptedMobileNumber = CRYPTO.encryptWithOneParameter(bm.getMobileNumber());
            String encryptedDisabilityType = CRYPTO.encryptWithOneParameter(bm.getDisabilityType());
            String encryptedHealthCondition = CRYPTO.encryptWithOneParameter(bm.getHealthCondition());
            String encryptedCleanWaterAccess = CRYPTO.encryptWithOneParameter(bm.getCleanWaterAccess());
            String encryptedSanitationFacility = CRYPTO.encryptWithOneParameter(bm.getSanitationFacility());
            String encryptedHouseType = CRYPTO.encryptWithOneParameter(bm.getHouseType());
            String encryptedOwnerShipStatus = CRYPTO.encryptWithOneParameter(bm.getOwnerShipStatus());
            String encryptedEmploymentStatus = CRYPTO.encryptWithOneParameter(bm.getEmploymentStatus());
            String encryptedMonthlyIncome = CRYPTO.encryptWithOneParameter(bm.getMonthlyIncome());
            String encryptedEducationalLevel = CRYPTO.encryptWithOneParameter(bm.getEducationalLevel());
            String encryptedDigitalAccess = CRYPTO.encryptWithOneParameter(bm.getDigitalAccess());
            String encryptedAddedBy = CRYPTO.encryptWithOneParameter(bm.getAddedBy());
            String encryptedRegDate = CRYPTO.encryptWithOneParameter(bm.getRegDate());

            boolean flag = beneficiaryDAO.saving(
                    new BeneficiaryModel(
                            encryptedFirstname,
                            encryptedMiddlename,
                            encryptedLastname,
                            encryptedBirthDate,
                            bm.getAgeScore(), // Pass age score (not encrypted)
                            encryptedGender,
                            encryptedMaritalStatus,
                            encryptedSoloParentStatus,
                            encryptedLatitude,
                            encryptedLongitude,
                            encryptedMobileNumber,
                            encryptedDisabilityType,
                            encryptedHealthCondition,
                            encryptedCleanWaterAccess,
                            encryptedSanitationFacility,
                            encryptedHouseType,
                            encryptedOwnerShipStatus,
                            encryptedEmploymentStatus,
                            encryptedMonthlyIncome,
                            encryptedEducationalLevel,
                            encryptedDigitalAccess,
                            encryptedAddedBy,
                            encryptedRegDate
                    )
            );
            if (!flag) {
                throw ExceptionFactory.failedToCreate("Beneficiary");
            }
            return flag;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQL error creating beneficiary", ex);
            return false;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error creating beneficiary", ex);
            return false;
        }
    }

    @Override
    public boolean deleteBeneficiary(BeneficiaryModel bm) {
        try {
            if (bm == null || bm.getId() <= 0) {
                throw ExceptionFactory.missingField("Beneficiary ID");
            }

            boolean deleted = beneficiaryDAO.delete(bm);

            if (!deleted) {
                throw ExceptionFactory.failedToDelete("Beneficiary");
            }

            return deleted;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error deleting beneficiary", ex);
            return false;
        }
    }

    @Override
    public boolean updateBeneficiary(BeneficiaryModel bm) {
        try {
            if (bm == null || bm.getId() <= 0) {
                throw ExceptionFactory.missingField("Beneficiary ID");
            }

            String encryptedFirstname = CRYPTO.encryptWithOneParameter(bm.getFirstname());
            String encryptedMiddlename = CRYPTO.encryptWithOneParameter(bm.getMiddlename());
            String encryptedLastname = CRYPTO.encryptWithOneParameter(bm.getLastname());
            String encryptedBirthDate = CRYPTO.encryptWithOneParameter(bm.getBirthDate());
            String encryptedGender = CRYPTO.encryptWithOneParameter(bm.getGender());
            String encryptedMaritalStatus = CRYPTO.encryptWithOneParameter(bm.getMaritalStatus());
            String encryptedSoloParentStatus = CRYPTO.encryptWithOneParameter(bm.getSoloParentStatus());
            String encryptedLatitude = CRYPTO.encryptWithOneParameter(bm.getLatitude());
            String encryptedLongitude = CRYPTO.encryptWithOneParameter(bm.getLongitude());
            String encryptedMobileNumber = CRYPTO.encryptWithOneParameter(bm.getMobileNumber());
            String encryptedDisabilityType = CRYPTO.encryptWithOneParameter(bm.getDisabilityType());
            String encryptedHealthCondition = CRYPTO.encryptWithOneParameter(bm.getHealthCondition());
            String encryptedCleanWaterAccess = CRYPTO.encryptWithOneParameter(bm.getCleanWaterAccess());
            String encryptedSanitationFacility = CRYPTO.encryptWithOneParameter(bm.getSanitationFacility());
            String encryptedHouseType = CRYPTO.encryptWithOneParameter(bm.getHouseType());
            String encryptedOwnerShipStatus = CRYPTO.encryptWithOneParameter(bm.getOwnerShipStatus());
            String encryptedEmploymentStatus = CRYPTO.encryptWithOneParameter(bm.getEmploymentStatus());
            String encryptedMonthlyIncome = CRYPTO.encryptWithOneParameter(bm.getMonthlyIncome());
            String encryptedEducationalLevel = CRYPTO.encryptWithOneParameter(bm.getEducationalLevel());
            String encryptedDigitalAccess = CRYPTO.encryptWithOneParameter(bm.getDigitalAccess());
            String encryptedAddedBy = CRYPTO.encryptWithOneParameter(bm.getAddedBy());
            String encryptedRegDate = CRYPTO.encryptWithOneParameter(bm.getRegDate());

            BeneficiaryModel encryptedBm = new BeneficiaryModel(
                    encryptedFirstname,
                    encryptedMiddlename,
                    encryptedLastname,
                    encryptedBirthDate,
                    bm.getAgeScore(), // Pass age score (not encrypted)
                    encryptedGender,
                    encryptedMaritalStatus,
                    encryptedSoloParentStatus,
                    encryptedLatitude,
                    encryptedLongitude,
                    encryptedMobileNumber,
                    encryptedDisabilityType,
                    encryptedHealthCondition,
                    encryptedCleanWaterAccess,
                    encryptedSanitationFacility,
                    encryptedHouseType,
                    encryptedOwnerShipStatus,
                    encryptedEmploymentStatus,
                    encryptedMonthlyIncome,
                    encryptedEducationalLevel,
                    encryptedDigitalAccess,
                    encryptedAddedBy,
                    encryptedRegDate
            );
            encryptedBm.setId(bm.getId()); // Set the ID for update

            boolean updated = beneficiaryDAO.update(encryptedBm);

            if (!updated) {
                throw ExceptionFactory.failedToUpdate("Beneficiary");
            }

            return updated;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error deleting beneficiary", ex);
            return false;
        }
    }

    @Override
    public BeneficiaryModel getBeneficiaryById(int id) {
        try {
            BeneficiaryModel encrypted = beneficiaryDAO.getById(id);

            if (encrypted == null) {
                return null;
            }

            List<String> decrypted = CRYPTO.decrypt(List.of(
                    encrypted.getFirstname(),
                    encrypted.getMiddlename(),
                    encrypted.getLastname(),
                    encrypted.getBirthDate(),
                    encrypted.getGender(),
                    encrypted.getMaritalStatus(),
                    encrypted.getSoloParentStatus(),
                    encrypted.getLatitude(),
                    encrypted.getLongitude(),
                    encrypted.getMobileNumber(),
                    encrypted.getDisabilityType(),
                    encrypted.getHealthCondition(),
                    encrypted.getCleanWaterAccess(),
                    encrypted.getSanitationFacility(),
                    encrypted.getHouseType(),
                    encrypted.getOwnerShipStatus(),
                    encrypted.getEmploymentStatus(),
                    encrypted.getMonthlyIncome(),
                    encrypted.getEducationalLevel(),
                    encrypted.getDigitalAccess(),
                    encrypted.getAddedBy(),
                    encrypted.getRegDate()
            ));

            BeneficiaryModel d = new BeneficiaryModel();
            d.setId(encrypted.getId());
            d.setFirstname(decrypted.get(0));
            d.setMiddlename(decrypted.get(1));
            d.setLastname(decrypted.get(2));
            d.setBirthDate(decrypted.get(3));
            d.setAgeScore(encrypted.getAgeScore()); // Age score is not encrypted
            d.setGender(decrypted.get(4));
            d.setMaritalStatus(decrypted.get(5));
            d.setSoloParentStatus(decrypted.get(6));
            d.setLatitude(decrypted.get(7));
            d.setLongitude(decrypted.get(8));
            d.setMobileNumber(decrypted.get(9));
            d.setDisabilityType(decrypted.get(10));
            d.setHealthCondition(decrypted.get(11));
            d.setCleanWaterAccess(decrypted.get(12));
            d.setSanitationFacility(decrypted.get(13));
            d.setHouseType(decrypted.get(14));
            d.setOwnerShipStatus(decrypted.get(15));
            d.setEmploymentStatus(decrypted.get(16));
            d.setMonthlyIncome(decrypted.get(17));
            d.setEducationalLevel(decrypted.get(18));
            d.setDigitalAccess(decrypted.get(19));
            d.setAddedBy(decrypted.get(20));
            d.setRegDate(decrypted.get(21));

            return d;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public List<BeneficiaryModel> searchBeneficiary(String searchTxt) {
        List<BeneficiaryModel> allBeneficiary = getAllBeneficiary();
        List<BeneficiaryModel> filtereBeneficiarys = new ArrayList<>();

        for (BeneficiaryModel beneficiaryModel : allBeneficiary) {
            if (beneficiaryModel.getFirstname().toLowerCase().contains(searchTxt.toLowerCase()) ||
                    beneficiaryModel.getMiddlename().toLowerCase().contains(searchTxt.toLowerCase()) ||
                    beneficiaryModel.getLastname().toLowerCase().contains(searchTxt.toLowerCase())) {
                filtereBeneficiarys.add(beneficiaryModel);
            }
        }
        return filtereBeneficiarys;
    }
}