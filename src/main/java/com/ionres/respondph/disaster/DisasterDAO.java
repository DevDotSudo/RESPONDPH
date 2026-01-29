package com.ionres.respondph.disaster;


import com.ionres.respondph.aid_type.AidTypeModelComboBox;

import java.sql.ResultSet;
import java.util.List;

public interface DisasterDAO {
    public boolean saving(DisasterModel dm);
    List<DisasterModel> getAll();
    public boolean delete(DisasterModel dm);
    public boolean update(DisasterModel dm);
    public DisasterModel getById(int id);
    public List<DisasterModelComboBox> findAll();
    public DisasterModelComboBox mapResultSetToDisaster(ResultSet rs) throws Exception;
}