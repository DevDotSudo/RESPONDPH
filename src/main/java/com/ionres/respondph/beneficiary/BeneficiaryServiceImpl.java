package com.ionres.respondph.beneficiary;

import com.ionres.respondph.admin.AdminModel;
import com.ionres.respondph.exception.ExceptionFactory;
import com.ionres.respondph.util.Cryptography;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.List;

public class BeneficiaryServiceImpl implements  BeneficiaryService{

    BeneficiaryDAO beneficiaryDAO = new BeneficiaryDAOImpl();

    @Override
    public List<BeneficiaryModel> getAllBeneficiary() {
        List<BeneficiaryModel> beneficiary = beneficiaryDAO.getAll();
        return beneficiary;
    }


    @Override
    public boolean createBeneficiary(BeneficiaryModel bm) {
        try {

            Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

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

            Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");

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
                    encryptedRegDate // Keep original registration date
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
            BeneficiaryModel bm = beneficiaryDAO.getById(id);
            if (bm == null) {
                System.out.println("Not Found");
            }
            return bm;
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            return null;
        }
    }

    @Override
    public List<BeneficiaryModel> searchBeneficiary(String searchTxt) {
        return List.of();
    }

}
