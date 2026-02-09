package com.ionres.respondph.sendsms;

import com.ionres.respondph.beneficiary.BeneficiaryModel;
import java.util.List;


public interface BeneficiaryDAO {



    List<BeneficiaryModel> getAllBeneficiaries();


    List<BeneficiaryModel> getBeneficiariesByBarangay(String barangay);


    List<String> getAllBarangays();

    List<BeneficiaryModel> getBeneficiariesByDisaster(int disasterId);
}