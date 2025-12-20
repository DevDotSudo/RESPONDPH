package com.ionres.respondph.familymembers;

import com.ionres.respondph.database.DBConnection;

public class FamilyMemberServiceImpl implements FamilyMemberService{
    private final FamilyMemberDAO familyMemberDAO;

    public FamilyMemberServiceImpl(DBConnection dbConnection) {
        this.familyMemberDAO = new FamilyMemberDAOImpl(dbConnection);
    }
}
