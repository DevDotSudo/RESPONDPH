package com.ionres.respondph.familymembers;

import com.ionres.respondph.common.model.BeneficiaryModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.exception.ExceptionFactory;
import com.ionres.respondph.util.ConfigLoader;
import com.ionres.respondph.util.Cryptography;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FamilyMemberServiceImpl implements FamilyMemberService{
    private final FamilyMemberDAO familyMemberDAO;
    private final Cryptography cs;

    public FamilyMemberServiceImpl(DBConnection dbConnection) {
        this.familyMemberDAO = new FamilyMemberDAOImpl(dbConnection);
        String secretKey = ConfigLoader.get("secretKey");
        this.cs = new Cryptography(secretKey);
    }

    @Override
    public List<FamilyMembersModel> getAllFamilyMembers() {

        try {
            List<FamilyMembersModel> encryptedList = familyMemberDAO.getAll();
            List<FamilyMembersModel> decryptedList = new ArrayList<>();

            for (FamilyMembersModel fm : encryptedList) {

                List<String> encrypted = List.of(
                        fm.getFirstName(),
                        fm.getMiddleName(),
                        fm.getLastName(),
                        fm.getRelationshipToBeneficiary(),
                        fm.getBirthDate(),
                        fm.getGender(),
                        fm.getMaritalStatus(),
                        fm.getNotes(),
                        fm.getRegDate(),
                        fm.getBeneficiaryName()
                );

                List<String> decrypted = cs.decrypt(encrypted);

                FamilyMembersModel d = new FamilyMembersModel();
                d.setFamilyId(fm.getFamilyId());

                // ✅ FIX: ADD THIS LINE - Preserve the beneficiaryId
                d.setBeneficiaryId(fm.getBeneficiaryId());

                d.setFirstName(decrypted.get(0));
                d.setMiddleName(decrypted.get(1));
                d.setLastName(decrypted.get(2));
                d.setRelationshipToBeneficiary(decrypted.get(3));
                d.setBirthDate(decrypted.get(4));
                d.setAgeScore(fm.getAgeScore());
                d.setGender(decrypted.get(5));
                d.setMaritalStatus(decrypted.get(6));
                d.setNotes(decrypted.get(7));
                d.setRegDate(decrypted.get(8));
                d.setBeneficiaryName(decrypted.get(9));

                decryptedList.add(d);
            }

            return decryptedList;

        } catch (Exception ex) {
            ex.printStackTrace();
            return List.of();
        }
    }


    @Override
    public boolean createfamilyMember(FamilyMembersModel fm) {
        try {
            String encryptFirstname = cs.encryptWithOneParameter(fm.getFirstName());
            String encryptMiddlename = cs.encryptWithOneParameter(fm.getMiddleName());
            String encryptLastname = cs.encryptWithOneParameter(fm.getLastName());
            String encryptRelationshipToBeneficiary = cs.encryptWithOneParameter(fm.getRelationshipToBeneficiary());
            String encryptBirthdate = cs.encryptWithOneParameter(fm.getBirthDate());
            String encryptGender = cs.encryptWithOneParameter(fm.getGender());
            String encryptMaritalStatus = cs.encryptWithOneParameter(fm.getMaritalStatus());
            String encryptDisabilityType = cs.encryptWithOneParameter(fm.getDisabilityType());
            String encryptHealthCondition = cs.encryptWithOneParameter(fm.getHealthCondition());
            String encryptEducationalLevel = cs.encryptWithOneParameter(fm.getEducationalLevel());
            String encryptEmploymentStatus = cs.encryptWithOneParameter(fm.getEmploymentStatus());
            int encryptBeneficiary = fm.getBeneficiaryId();
            String encryptNotes = cs.encryptWithOneParameter(fm.getNotes());
            String encryptRegDate = cs.encryptWithOneParameter(fm.getRegDate());

            boolean flag = familyMemberDAO.saving(
                    new FamilyMembersModel(
                            encryptFirstname,
                            encryptMiddlename,
                            encryptLastname,
                            encryptRelationshipToBeneficiary,
                            encryptBirthdate,
                            fm.getAgeScore(),
                            encryptGender,
                            encryptMaritalStatus,
                            encryptDisabilityType,
                            encryptHealthCondition,
                            encryptEmploymentStatus,
                            encryptEducationalLevel,
                            encryptBeneficiary,
                            encryptNotes,
                            encryptRegDate));
            if (!flag) {
                throw ExceptionFactory.failedToCreate("Family Members");
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
    public boolean deletefamilyMember(FamilyMembersModel fm) {
        try {
            if (fm == null || fm.getFamilyId() <= 0) {
                throw ExceptionFactory.missingField("Family Member ID");
            }

            boolean deleted = familyMemberDAO.delete(fm);

            if (!deleted) {
                throw ExceptionFactory.failedToDelete("Family Member");
            }

            return deleted;

        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean updatefamilyMember(FamilyMembersModel fm) {
        try {
            if (fm == null || fm.getFamilyId() <= 0) {
                throw ExceptionFactory.missingField("Family Member ID");
            }


            String encryptedFirstname = cs.encryptWithOneParameter(fm.getFirstName());
            String encryptedMiddlename = cs.encryptWithOneParameter(fm.getMiddleName());
            String encryptedLastname = cs.encryptWithOneParameter(fm.getLastName());
            String encryptedRelationship = cs.encryptWithOneParameter(fm.getRelationshipToBeneficiary());
            String encryptedBirthDate = cs.encryptWithOneParameter(fm.getBirthDate());
            String encryptedGender = cs.encryptWithOneParameter(fm.getGender());
            String encryptedMaritalStatus = cs.encryptWithOneParameter(fm.getMaritalStatus());
            String encryptedDisabilityType = cs.encryptWithOneParameter(fm.getDisabilityType());
            String encryptedHealthCondition = cs.encryptWithOneParameter(fm.getHealthCondition());
            String encryptedEmploymentStatus = cs.encryptWithOneParameter(fm.getEmploymentStatus());
            String encryptedEducationalLevel = cs.encryptWithOneParameter(fm.getEducationalLevel());
            String encryptedNotes = cs.encryptWithOneParameter(fm.getNotes());
            String encryptedRegDate = cs.encryptWithOneParameter(fm.getRegDate());

            FamilyMembersModel encryptedFm = new FamilyMembersModel();
            encryptedFm.setFirstName(encryptedFirstname);
            encryptedFm.setMiddleName(encryptedMiddlename);
            encryptedFm.setLastName(encryptedLastname);
            encryptedFm.setRelationshipToBeneficiary(encryptedRelationship);
            encryptedFm.setBirthDate(encryptedBirthDate);
            encryptedFm.setAgeScore(fm.getAgeScore());
            encryptedFm.setGender(encryptedGender);
            encryptedFm.setMaritalStatus(encryptedMaritalStatus);
            encryptedFm.setDisabilityType(encryptedDisabilityType);
            encryptedFm.setHealthCondition(encryptedHealthCondition);
            encryptedFm.setEmploymentStatus(encryptedEmploymentStatus);
            encryptedFm.setEducationalLevel(encryptedEducationalLevel);
            encryptedFm.setNotes(encryptedNotes);
            encryptedFm.setRegDate(encryptedRegDate);
            encryptedFm.setFamilyId(fm.getFamilyId());

            boolean updated = familyMemberDAO.update(encryptedFm);

            if (!updated) {
                throw ExceptionFactory.failedToUpdate("Family Member");
            }

            return updated;

        } catch (Exception ex) {
            System.out.println("Error updating family member: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public List<FamilyMembersModel> searchFamilyMember(String searchTxt) {
        List<FamilyMembersModel> allFamilyMembers = getAllFamilyMembers();
        List<FamilyMembersModel> filterFamilyMembers = new ArrayList<>();

        for (FamilyMembersModel familyMembersModel : allFamilyMembers) {
            if (familyMembersModel.getFirstName().toLowerCase().contains(searchTxt.toLowerCase()) ||
                    familyMembersModel.getMiddleName().toLowerCase().contains(searchTxt.toLowerCase()) ||
                    familyMembersModel.getLastName().toLowerCase().contains(searchTxt.toLowerCase())) {
                filterFamilyMembers.add(familyMembersModel);
            }
        }
        return filterFamilyMembers;
    }

    @Override
    public FamilyMembersModel getfamilyMemberId(int id) {

        try {
            FamilyMembersModel encrypted = familyMemberDAO.getById(id);

            if (encrypted == null) {
                return null;
            }

            List<String> decrypted = cs.decrypt(List.of(
                    encrypted.getFirstName(),
                    encrypted.getMiddleName(),
                    encrypted.getLastName(),
                    encrypted.getRelationshipToBeneficiary(),
                    encrypted.getBirthDate(),
                    encrypted.getGender(),
                    encrypted.getMaritalStatus(),
                    encrypted.getDisabilityType(),
                    encrypted.getHealthCondition(),
                    encrypted.getEmploymentStatus(),
                    encrypted.getEducationalLevel(),
                    encrypted.getNotes(),
                    encrypted.getRegDate()
            ));

            FamilyMembersModel d = new FamilyMembersModel();
            d.setFamilyId(encrypted.getFamilyId());

            // ✅ FIX: ADD THIS LINE - Preserve the beneficiaryId from the encrypted model
            d.setBeneficiaryId(encrypted.getBeneficiaryId());

            d.setFirstName(decrypted.get(0));
            d.setMiddleName(decrypted.get(1));
            d.setLastName(decrypted.get(2));
            d.setRelationshipToBeneficiary(decrypted.get(3));
            d.setBirthDate(decrypted.get(4));
            d.setGender(decrypted.get(5));
            d.setMaritalStatus(decrypted.get(6));
            d.setDisabilityType(decrypted.get(7));
            d.setHealthCondition(decrypted.get(8));
            d.setEmploymentStatus(decrypted.get(9));
            d.setEducationalLevel(decrypted.get(10));
            d.setNotes(decrypted.get(11));
            d.setRegDate(decrypted.get(12));

            return d;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public List<BeneficiaryModel> getAllBeneficiaries() {
        try {
            List<BeneficiaryModel> encryptedList = familyMemberDAO.getAllBeneficiaryByFirstname();

            List<String> encryptedNames = new ArrayList<>();
            for (BeneficiaryModel b : encryptedList) {
                encryptedNames.add(b.getFirstName());
                encryptedNames.add(b.getMiddlename());
                encryptedNames.add(b.getLastname());
            }

            List<String> decryptedNames = cs.decrypt(encryptedNames);

            List<BeneficiaryModel> decryptedModels = new ArrayList<>();
            for (int i = 0; i < encryptedList.size(); i++) {
                BeneficiaryModel original = encryptedList.get(i);

                int baseIndex = i * 3;

                decryptedModels.add(new BeneficiaryModel(
                        original.getBeneficiaryId(),
                        decryptedNames.get(baseIndex),      // firstName
                        decryptedNames.get(baseIndex + 1),  // middlename
                        decryptedNames.get(baseIndex + 2)   // lastname
                ));
            }
            return decryptedModels;
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ArrayList<>();
        }
    }
}