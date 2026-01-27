package com.ionres.respondph.disaster;

import java.util.List;

public interface DisasterDAO {
    public boolean saving(DisasterModel dm);
    List<DisasterModel> getAll();
    public boolean delete(DisasterModel dm);
    public boolean update(DisasterModel dm);
    public DisasterModel getById(int id);
    public List<String[]> getEncryptedDisasters();
}