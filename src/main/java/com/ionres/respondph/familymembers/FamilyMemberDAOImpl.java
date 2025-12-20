package com.ionres.respondph.familymembers;

import com.ionres.respondph.database.DBConnection;

import java.util.List;

public class FamilyMemberDAOImpl implements FamilyMemberDAO {
    private final DBConnection dbConnection;

    public FamilyMemberDAOImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public boolean saving(FamilyMembersModel fm) {
        return false;
    }

    @Override
    public List<FamilyMembersModel> getAll() {
        return List.of();
    }

    @Override
    public boolean delete(FamilyMembersModel fm) {
        return false;
    }

    @Override
    public boolean update(FamilyMembersModel fm) {
        return false;
    }

    @Override
    public FamilyMembersModel getById(int id) {
        return null;
    }
//
//    @Override
//    public List<BeneficiaryModel> getAllBeneficiaryByFirstname() {
//        return List.of();
//    }
}
