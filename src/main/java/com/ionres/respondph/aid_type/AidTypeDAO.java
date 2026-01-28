package com.ionres.respondph.aid_type;




import java.sql.ResultSet;
import java.util.List;

public interface AidTypeDAO {
    public boolean saving(AidTypeModel atm);
    List<AidTypeModel> getAll();
    public boolean delete(AidTypeModel atm);
    AidTypeModel getById(int id);
    public boolean update(AidTypeModel atm);
    public List<AidTypeModelComboBox> findAll();
    public AidTypeModelComboBox mapResultSetToAidType(ResultSet rs) throws Exception;

    List<Integer> getAllAidTypeIds();
    boolean hasAnyAidTypes();
}
