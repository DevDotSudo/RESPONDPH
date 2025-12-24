package com.ionres.respondph.beneficiary;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.exception.ExceptionFactory;
import com.ionres.respondph.util.ConfigLoader;
import com.ionres.respondph.util.Cryptography;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BeneficiaryServiceImpl implements  BeneficiaryService{
    private final BeneficiaryDAO beneficiaryDAO;
    private final Cryptography cs;

    public BeneficiaryServiceImpl(DBConnection dbConnection) {
        this.beneficiaryDAO = new BeneficiaryDAOImpl(dbConnection);
        String secretKey = ConfigLoader.get("secretKey");
        this.cs = new Cryptography(secretKey);
    }

    @Override
    public List<BeneficiaryModel> getAllBeneficiary() {

        try {
            List<BeneficiaryModel> encryptedList = beneficiaryDAO.getAll();
            List<BeneficiaryModel> decryptedList = new ArrayList<>();

            for (BeneficiaryModel bm : encryptedList) {

                List<String> decrypted = cs.decrypt(List.of(
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
            String encryptedFirstname = cs.encryptWithOneParameter(bm.getFirstname());
            String encryptedMiddlename = cs.encryptWithOneParameter(bm.getMiddlename());
            String encryptedLastname = cs.encryptWithOneParameter(bm.getLastname());
            String encryptedBirthDate = cs.encryptWithOneParameter(bm.getBirthDate());
            String encryptedGender  = cs.encryptWithOneParameter(bm.getGender());
            String encryptedMaritalStatus = cs.encryptWithOneParameter(bm.getMaritalStatus());
            String encryptedSoloParentStatus = cs.encryptWithOneParameter(bm.getSoloParentStatus());
            String encryptedLatitude = cs.encryptWithOneParameter(bm.getLatitude());
            String encryptedLongitude = cs.encryptWithOneParameter(bm.getLongitude());
            String encryptedMobileNumber = cs.encryptWithOneParameter(bm.getMobileNumber());
            String encryptedDisabilityType = cs.encryptWithOneParameter(bm.getDisabilityType());
            String encryptedHealthCondition = cs.encryptWithOneParameter(bm.getHealthCondition());
            String encryptedCleanWaterAccess = cs.encryptWithOneParameter(bm.getCleanWaterAccess());
            String encryptedSanitationFacility = cs.encryptWithOneParameter(bm.getSanitationFacility());
            String encryptedHouseType = cs.encryptWithOneParameter(bm.getHouseType());
            String encryptedOwnerShipStatus = cs.encryptWithOneParameter(bm.getOwnerShipStatus());
            String encryptedEmploymentStatus = cs.encryptWithOneParameter(bm.getEmploymentStatus());
            String encryptedMonthlyIncome = cs.encryptWithOneParameter(bm.getMonthlyIncome());
            String encryptedEducationalLevel = cs.encryptWithOneParameter(bm.getEducationalLevel());
            String encryptedDigitalAccess = cs.encryptWithOneParameter(bm.getDigitalAccess());
            String encryptedAddedBy = cs.encryptWithOneParameter(bm.getAddedBy());
            String encryptedRegDate = cs.encryptWithOneParameter(bm.getRegDate());



            boolean flag = beneficiaryDAO.saving(
                    new BeneficiaryModel(
                            encryptedFirstname,
                            encryptedMiddlename,
                            encryptedLastname,
                            encryptedBirthDate,
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
            System.out.println("Error : " + ex);
            return  false;

        } catch (Exception ex) {
            System.out.println("Error: " + ex);
            return  false;
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
            System.out.println("Error: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateBeneficiary(BeneficiaryModel bm) {
        try {
            if (bm == null || bm.getId() <= 0) {
                throw ExceptionFactory.missingField("Beneficiary ID");
            }

            String encryptedFirstname = cs.encryptWithOneParameter(bm.getFirstname());
            String encryptedMiddlename = cs.encryptWithOneParameter(bm.getMiddlename());
            String encryptedLastname = cs.encryptWithOneParameter(bm.getLastname());
            String encryptedBirthDate = cs.encryptWithOneParameter(bm.getBirthDate());
            String encryptedGender = cs.encryptWithOneParameter(bm.getGender());
            String encryptedMaritalStatus = cs.encryptWithOneParameter(bm.getMaritalStatus());
            String encryptedSoloParentStatus = cs.encryptWithOneParameter(bm.getSoloParentStatus());
            String encryptedLatitude = cs.encryptWithOneParameter(bm.getLatitude());
            String encryptedLongitude = cs.encryptWithOneParameter(bm.getLongitude());
            String encryptedMobileNumber = cs.encryptWithOneParameter(bm.getMobileNumber());
            String encryptedDisabilityType = cs.encryptWithOneParameter(bm.getDisabilityType());
            String encryptedHealthCondition = cs.encryptWithOneParameter(bm.getHealthCondition());
            String encryptedCleanWaterAccess = cs.encryptWithOneParameter(bm.getCleanWaterAccess());
            String encryptedSanitationFacility = cs.encryptWithOneParameter(bm.getSanitationFacility());
            String encryptedHouseType = cs.encryptWithOneParameter(bm.getHouseType());
            String encryptedOwnerShipStatus = cs.encryptWithOneParameter(bm.getOwnerShipStatus());
            String encryptedEmploymentStatus = cs.encryptWithOneParameter(bm.getEmploymentStatus());
            String encryptedMonthlyIncome = cs.encryptWithOneParameter(bm.getMonthlyIncome());
            String encryptedEducationalLevel = cs.encryptWithOneParameter(bm.getEducationalLevel());
            String encryptedDigitalAccess = cs.encryptWithOneParameter(bm.getDigitalAccess());
            String encryptedAddedBy = cs.encryptWithOneParameter(bm.getAddedBy());
            String encryptedRegDate = cs.encryptWithOneParameter(bm.getRegDate());

            BeneficiaryModel encryptedBm = new BeneficiaryModel(
                    encryptedFirstname,
                    encryptedMiddlename,
                    encryptedLastname,
                    encryptedBirthDate,
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
            System.out.println("Error: " + ex.getMessage());
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

            List<String> decrypted = cs.decrypt(List.of(
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
