package com.ionres.respondph.disaster_damage;

import com.ionres.respondph.common.model.BeneficiaryModel;
import com.ionres.respondph.common.model.DisasterModel;

import java.util.List;

public interface DisasterDamageDAO {
    public boolean saving(DisasterDamageModel ddm);
    List<DisasterDamageModel> getAll();
    public boolean delete(DisasterDamageModel ddm);
    public boolean update(DisasterDamageModel ddm);
    DisasterDamageModel getById(int id);
    public List<BeneficiaryModel> getAllBeneficiaryByFirstname();
    public List<DisasterModel> getAllDisasterTypeAndName();
}