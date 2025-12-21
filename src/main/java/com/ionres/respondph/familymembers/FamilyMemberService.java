package com.ionres.respondph.familymembers;


import java.util.List;

public interface FamilyMemberService {
    List<FamilyMembersModel> getAllFamilyMembers();
    boolean createfamilyMember(FamilyMembersModel fm);
    boolean deletefamilyMember(FamilyMembersModel fm);
    boolean updatefamilyMember(FamilyMembersModel fm);
    public List<FamilyMembersModel> searchFamilyMember(String searchTxt);
    FamilyMembersModel getfamilyMemberId(int id);
    public List<BeneficiaryModel> getAllBeneficiaries();
}