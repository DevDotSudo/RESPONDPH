package com.ionres.respondph.admin;

import java.util.List;

public interface AdminDAO {
    public boolean saving(AdminModel am);
    boolean existsByUsername(String username);
    List<AdminModel> getAll();
    public AdminModel login(String username, String password);
}
