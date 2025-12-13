
package com.ionres.respondph.admin;


import java.util.List;

public interface AdminService {

    List<AdminModel> getAllAdmins();
    boolean createAdmin(AdminModel admin);
    boolean deleteAdmin(AdminModel admin);
    boolean updateAdmin(AdminModel admin);
    public List<AdminModel> searchAdmin(String searchTxt);
}
