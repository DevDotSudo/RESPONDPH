package com.ionres.respondph.admin;

public class AdminModel {

    private int    id;
    private String username;
    private String firstname;
    private String middlename;
    private String lastname;
    private String regDate;
    private String password;
    private String role;        // "Admin" | "Secretary" | "DSWD" | "MDRRMO"

    // ─── Constructors ─────────────────────────────────────────────────────────

    public AdminModel() {}

    /** Used by AdminServiceImpl.createAdmin() */
    public AdminModel(String username, String firstname, String middlename,
                      String lastname, String regDate, String password) {
        this.username   = username;
        this.firstname  = firstname;
        this.middlename = middlename;
        this.lastname   = lastname;
        this.regDate    = regDate;
        this.password   = password;
    }

    /** Full constructor including role */
    public AdminModel(String username, String firstname, String middlename,
                      String lastname, String regDate, String password, String role) {
        this(username, firstname, middlename, lastname, regDate, password);
        this.role = role;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public int getId()                  { return id; }
    public void setId(int id)           { this.id = id; }

    public String getUsername()         { return username; }
    public void setUsername(String u)   { this.username = u; }

    public String getFirstname()        { return firstname; }
    public void setFirstname(String f)  { this.firstname = f; }

    public String getMiddlename()       { return middlename; }
    public void setMiddlename(String m) { this.middlename = m; }

    public String getLastname()         { return lastname; }
    public void setLastname(String l)   { this.lastname = l; }

    public String getRegDate()          { return regDate; }
    public void setRegDate(String r)    { this.regDate = r; }

    public String getPassword()         { return password; }
    public void setPassword(String p)   { this.password = p; }

    public String getRole()             { return role; }
    public void setRole(String role)    { this.role = role; }

    @Override
    public String toString() {
        return "AdminModel{id=" + id + ", username='" + username + "', role='" + role + "'}";
    }
}