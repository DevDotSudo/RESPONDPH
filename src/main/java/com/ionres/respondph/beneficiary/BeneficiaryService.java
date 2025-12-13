package com.ionres.respondph.beneficiary;

import java.util.List;

public interface BeneficiaryService {
    List<BeneficiaryModel> getAllBeneficiary();
    boolean createBeneficiary(BeneficiaryModel bm);
    boolean deleteBeneficiary(BeneficiaryModel bm);
    boolean updateBeneficiary(BeneficiaryModel bm);
    public List<BeneficiaryModel> searchBeneficiary(String searchTxt);
    BeneficiaryModel getBeneficiaryById(int id);

}