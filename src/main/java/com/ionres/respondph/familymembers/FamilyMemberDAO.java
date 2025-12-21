package com.ionres.respondph.familymembers;


import java.sql.SQLException;
import java.util.List;

public interface FamilyMemberDAO {
    public boolean saving(FamilyMembersModel fm);
    List<FamilyMembersModel> getAll();
    public boolean delete(FamilyMembersModel fm);
    public boolean update(FamilyMembersModel fm);
    FamilyMembersModel getById(int id);
    public List<BeneficiaryModel> getAllBeneficiaryByFirstname();
}