package com.ionres.respondph.aid_type;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.exception.ExceptionFactory;
import com.ionres.respondph.familymembers.FamilyMembersModel;
import com.ionres.respondph.util.ConfigLoader;
import com.ionres.respondph.util.Cryptography;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AidTypeServiceImpl implements AidTypeService{
    private final AidTypeDAO aidTypeDAO;
    private final Cryptography cs;

    public AidTypeServiceImpl(DBConnection dbConnection) {
        this.aidTypeDAO = new AidTypeDAOImpl(dbConnection);
        String secretKey = ConfigLoader.get("secretKey");
        this.cs = new Cryptography(secretKey);
    }
    @Override
    public boolean createAidType(AidTypeModel atm) {
        try {
            String encryptAidTypeName = cs.encryptWithOneParameter(atm.getAidTypeName());
            String encryptNotes = cs.encryptWithOneParameter(atm.getNotes());
            String encryptRegDate = cs.encryptWithOneParameter(atm.getRegDate());

            boolean flag = aidTypeDAO.saving(
                    new AidTypeModel(
                            encryptAidTypeName,
                            atm.getAgeWeight(),
                            atm.getGenderWeight(),
                            atm.getMaritalStatusWeight(),
                            atm.getSoloParentWeight(),
                            atm.getDisabilityWeight(),
                            atm.getHealthConditionWeight(),
                            atm.getAccessToCleanWaterWeight(),
                            atm.getSanitationFacilityWeight(),
                            atm.getHouseConstructionTypeWeight(),
                            atm.getOwnershipWeight(),
                            atm.getDamageSeverityWeight(),
                            atm.getEmploymentStatusWeight(),
                            atm.getMonthlyIncomeWeight(),
                            atm.getEducationalLevelWeight(),
                            atm.getDigitalAccessWeight(),
                            atm.getDependencyRatioWeight(),
                            encryptNotes,
                            atm.getAdminId(),
                            encryptRegDate
                    )
            );

            if (!flag) {
                throw ExceptionFactory.failedToCreate("Aid Type");
            }

            return flag;

        } catch (SQLException ex) {
            System.out.println("SQL Error: " + ex);
            return false;

        } catch (Exception ex) {
            System.out.println("Error: " + ex);
            return false;
        }

    }

    @Override
    public List<AidTypeModel> getAllAidType() {

        try {
            List<AidTypeModel> encryptedList = aidTypeDAO.getAll();
            List<AidTypeModel> decryptedList = new ArrayList<>();

            for (AidTypeModel at : encryptedList) {

                List<String> encrypted = List.of(
                        at.getAidTypeName(),
                        at.getNotes(),
                        at.getRegDate(),
                        at.getAdminName()
                );

                List<String> decrypted = cs.decrypt(encrypted);

                AidTypeModel d = new AidTypeModel();
                d.setAidTypeId(at.getAidTypeId());
                d.setAidTypeName(decrypted.get(0));
                d.setNotes(decrypted.get(1));
                d.setRegDate(decrypted.get(2));
                d.setAdminName(decrypted.get(3));

                d.setAdminId(at.getAdminId());

                decryptedList.add(d);
            }

            return decryptedList;

        } catch (Exception ex) {
            ex.printStackTrace();
            return List.of();
        }
    }

    @Override
    public boolean deleteAidType(AidTypeModel atm) {
        try {
            if (atm == null || atm.getAidTypeId() <= 0) {
                throw ExceptionFactory.missingField("Aid Type ID");
            }

            boolean deleted = aidTypeDAO.delete(atm);

            if (!deleted) {
                throw ExceptionFactory.failedToDelete("Aid Type");
            }
            return deleted;

        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateAidType(AidTypeModel atm) {

        try {
            if (atm == null || atm.getAidTypeId() <= 0) {
                throw ExceptionFactory.missingField("Aid Type ID");
            }

            String encryptedAidName = cs.encryptWithOneParameter(atm.getAidTypeName());
            String encryptedNotes = cs.encryptWithOneParameter(atm.getNotes());
            String encryptedRegDate = cs.encryptWithOneParameter(atm.getRegDate());

            AidTypeModel encrypted = new AidTypeModel();

            encrypted.setAidTypeId(atm.getAidTypeId());
            encrypted.setAidTypeName(encryptedAidName);
            encrypted.setNotes(encryptedNotes);
            encrypted.setRegDate(encryptedRegDate);

            encrypted.setAgeWeight(atm.getAgeWeight());
            encrypted.setGenderWeight(atm.getGenderWeight());
            encrypted.setMaritalStatusWeight(atm.getMaritalStatusWeight());
            encrypted.setSoloParentWeight(atm.getSoloParentWeight());
            encrypted.setDisabilityWeight(atm.getDisabilityWeight());
            encrypted.setHealthConditionWeight(atm.getHealthConditionWeight());
            encrypted.setAccessToCleanWaterWeight(atm.getAccessToCleanWaterWeight());
            encrypted.setSanitationFacilityWeight(atm.getSanitationFacilityWeight());
            encrypted.setHouseConstructionTypeWeight(atm.getHouseConstructionTypeWeight());
            encrypted.setOwnershipWeight(atm.getOwnershipWeight());
            encrypted.setDamageSeverityWeight(atm.getDamageSeverityWeight());
            encrypted.setEmploymentStatusWeight(atm.getEmploymentStatusWeight());
            encrypted.setMonthlyIncomeWeight(atm.getMonthlyIncomeWeight());
            encrypted.setEducationalLevelWeight(atm.getEducationalLevelWeight());
            encrypted.setDigitalAccessWeight(atm.getDigitalAccessWeight());
            encrypted.setDependencyRatioWeight(atm.getDependencyRatioWeight());

            encrypted.setAdminId(atm.getAdminId());

            boolean updated = aidTypeDAO.update(encrypted);

            if (!updated) {
                throw ExceptionFactory.failedToUpdate("Aid Type");
            }

            return true;

        } catch (Exception ex) {
            System.out.println("Update Aid Type Error: " + ex.getMessage());
            return false;
        }
    }



    @Override
    public AidTypeModel getAidTypeById(int id) {
        try {
            AidTypeModel encrypted = aidTypeDAO.getById(id);

            if (encrypted == null) {
                return null;
            }

            List<String> decrypted = cs.decrypt(List.of(
                    encrypted.getAidTypeName(),
                    encrypted.getNotes(),
                    encrypted.getRegDate()
            ));

            AidTypeModel d = new AidTypeModel();
            d.setAidTypeId(encrypted.getAidTypeId());
            d.setAidTypeName(decrypted.get(0));
            d.setNotes(decrypted.get(1));
            d.setRegDate(decrypted.get(2));

            d.setAgeWeight(encrypted.getAgeWeight());
            d.setGenderWeight(encrypted.getGenderWeight());
            d.setMaritalStatusWeight(encrypted.getMaritalStatusWeight());
            d.setSoloParentWeight(encrypted.getSoloParentWeight());
            d.setDisabilityWeight(encrypted.getDisabilityWeight());
            d.setHealthConditionWeight(encrypted.getHealthConditionWeight());
            d.setAccessToCleanWaterWeight(encrypted.getAccessToCleanWaterWeight());
            d.setSanitationFacilityWeight(encrypted.getSanitationFacilityWeight());
            d.setHouseConstructionTypeWeight(encrypted.getHouseConstructionTypeWeight());
            d.setOwnershipWeight(encrypted.getOwnershipWeight());
            d.setDamageSeverityWeight(encrypted.getDamageSeverityWeight());
            d.setEmploymentStatusWeight(encrypted.getEmploymentStatusWeight());
            d.setMonthlyIncomeWeight(encrypted.getMonthlyIncomeWeight());
            d.setEducationalLevelWeight(encrypted.getEducationalLevelWeight());
            d.setDigitalAccessWeight(encrypted.getDigitalAccessWeight());
            d.setDependencyRatioWeight(encrypted.getDependencyRatioWeight());

            return d;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public List<AidTypeModel> searchAidType(String searchTxt) {
        List<AidTypeModel> allAidType = getAllAidType();
        List<AidTypeModel> filterAidType = new ArrayList<>();

        for (AidTypeModel aidTypeModel : allAidType) {
            if (aidTypeModel.getAidTypeName().toLowerCase().contains(searchTxt.toLowerCase()) ||
                    aidTypeModel.getNotes().toLowerCase().contains(searchTxt.toLowerCase()) ||
                    aidTypeModel.getAdminName().toLowerCase().contains(searchTxt.toLowerCase())) {
                filterAidType.add(aidTypeModel);
            }
        }
        return filterAidType;
    }

    @Override
    public List<Integer> getAllAidTypeIds() {
        try {
            return aidTypeDAO.getAllAidTypeIds();
        } catch (Exception e) {
            System.err.println("Error getting aid type IDs: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public boolean hasAnyAidTypes() {
        try {
            return aidTypeDAO.hasAnyAidTypes();
        } catch (Exception e) {
            System.err.println("Error checking for aid types: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


}
