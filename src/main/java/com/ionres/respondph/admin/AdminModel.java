package com.ionres.respondph.admin;

public class AdminModel {
    private int id;
    private String username;
    private String firstname;
    private String middlename;
    private String lastname;
    private String regDate;
    private String password;

    public int getId(){
        return id;
    }

    public void setId(int newId){
        this.id = newId;
    }

    public String getUsername(){
        return  username;
    }

    public void setUsername(String newUsername){
        this.username = newUsername;
    }

    public String getFirstname(){
        return  firstname;
    }
    public void setFirstname(String newFirstname){
        this.firstname = newFirstname;
    }

    public String getMiddlename(){
        return  middlename;
    }
    public void setMiddlename(String newMiddlename){
        this.middlename = newMiddlename;
    }


    public String getLastname(){
        return lastname;

    }
    public void  setLastname(String newLastname){
        this.lastname = newLastname;
    }

    public String getRegDate(){
        return regDate;
    }

    public void setRegDate(String newRegDate){
        this.regDate = newRegDate;
    }

    public String getPassword(){
        return  password;
    }
    public void setPassword(String newPassword){
        this.password =  newPassword;
    }
}
