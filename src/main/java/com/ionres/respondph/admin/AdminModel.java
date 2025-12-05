package com.ionres.respondph.admin;

public class AdminModel {
    private String adminID;
    private String firstName;
    private String middleName;
    private String lastName;
    private String password;

    public AdminModel(String adminID, String firstName, String middleName, String lastName, String password) {
        this.adminID = adminID;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.password = password;
    }

    public String getAdminID() {
        return adminID;
    }

    public String getFirstName() {
        return firstName;
    }

}
