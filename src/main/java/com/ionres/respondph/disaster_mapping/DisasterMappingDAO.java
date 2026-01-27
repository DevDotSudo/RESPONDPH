package com.ionres.respondph.disaster_mapping;

import com.ionres.respondph.common.model.BeneficiaryEncrypted;
import com.ionres.respondph.common.model.DisasterCircleEncrypted;
import com.ionres.respondph.common.model.DisasterModel;

import java.util.List;

public interface DisasterMappingDAO {

    List<String> getDisasterTypes();

    List<DisasterModel> getAllDisasters();

    List<DisasterModel> getDisastersByType(String encryptedType);

    List<DisasterCircleEncrypted> getAllDisasterCircles();

    List<DisasterCircleEncrypted> getDisasterCirclesByDisasterId(int disasterId);

    List<BeneficiaryEncrypted> getAllBeneficiaries();

    DisasterModel getDisasterById(int disasterId);
}
