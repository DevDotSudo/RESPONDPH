/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ionres.respondph.admin;

import java.util.List;

/**
 *
 * @author Davie
 */
public interface AdminDAO {
    public boolean saving(AdminModel am);
    boolean existsByUsername(String username);
    List<AdminModel> getAll();
}
