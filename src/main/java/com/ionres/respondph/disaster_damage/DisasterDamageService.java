package com.ionres.respondph.disaster_damage;

import com.ionres.respondph.common.model.BeneficiaryModel;
import com.ionres.respondph.common.model.DisasterModel;

import java.util.List;

public interface DisasterDamageService {
    List<DisasterDamageModel> getAllDisasterDamage();
    boolean createDisasterDamage(DisasterDamageModel ddm);
    boolean deleteDisasterDamage(DisasterDamageModel ddm);
    boolean updateDisasterDamage(DisasterDamageModel ddm);
    public List<DisasterDamageModel> searchDisasterDamage(String searchTxt);
    DisasterDamageModel getDisasterDamageId(int id);
    public List<BeneficiaryModel> getAllBeneficiaries();
    public List<DisasterModel> getALlDisaster();
}
