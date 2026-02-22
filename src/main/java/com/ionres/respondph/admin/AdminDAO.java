package com.ionres.respondph.admin;

import java.util.List;

public interface AdminDAO {

    boolean saving(AdminModel am);

    boolean existsByUsername(String encryptedUsername);

    List<AdminModel> getAll();

    boolean delete(AdminModel am);

    boolean update(AdminModel am);
}