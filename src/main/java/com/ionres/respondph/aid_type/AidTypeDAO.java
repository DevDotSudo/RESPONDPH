package com.ionres.respondph.aid_type;




import java.util.List;

public interface AidTypeDAO {
    public boolean saving(AidTypeModel atm);
    List<AidTypeModel> getAll();
    public boolean delete(AidTypeModel atm);
    AidTypeModel getById(int id);
    public boolean update(AidTypeModel atm);
}
