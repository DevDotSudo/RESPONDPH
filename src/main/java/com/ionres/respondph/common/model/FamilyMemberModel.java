package com.ionres.respondph.common.model;

public class FamilyMemberModel {

    private int    familyMemberId;
    private String firstName;
    private String middleName;
    private String lastName;

    public FamilyMemberModel() {}

    public FamilyMemberModel(int familyMemberId, String firstName, String middleName, String lastName) {
        this.familyMemberId = familyMemberId;
        this.firstName      = firstName;
        this.middleName     = middleName;
        this.lastName       = lastName;
    }


    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName  != null && !firstName.isBlank())  sb.append(firstName.trim());
        if (middleName != null && !middleName.isBlank()) sb.append(" ").append(middleName.trim());
        if (lastName   != null && !lastName.isBlank())   sb.append(" ").append(lastName.trim());
        return sb.toString().trim();
    }

    public int    getFamilyMemberId()              { return familyMemberId; }
    public void   setFamilyMemberId(int id)        { this.familyMemberId = id; }

    public String getFirstName()                   { return firstName; }
    public void   setFirstName(String firstName)   { this.firstName = firstName; }

    public String getMiddleName()                  { return middleName; }
    public void   setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName()                    { return lastName; }
    public void   setLastName(String lastName)     { this.lastName = lastName; }

    // ── toString ──────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "FamilyMemberModel{id=" + familyMemberId + ", name='" + getFullName() + "'}";
    }
}