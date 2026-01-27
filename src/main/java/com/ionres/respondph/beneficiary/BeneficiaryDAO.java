package com.ionres.respondph.beneficiary;

import java.util.List;

public interface BeneficiaryDAO {
    public boolean saving(BeneficiaryModel bm);
    List<BeneficiaryModel> getAll();
    public boolean delete(BeneficiaryModel bm);
    public boolean update(BeneficiaryModel bm);
    BeneficiaryModel getById(int id);
    public List<String[]> getEncryptedLocations();
}