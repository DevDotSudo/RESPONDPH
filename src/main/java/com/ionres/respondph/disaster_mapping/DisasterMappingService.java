package com.ionres.respondph.disaster_mapping;

import com.ionres.respondph.common.model.BeneficiaryMarker;
import com.ionres.respondph.common.model.DisasterCircleInfo;
import com.ionres.respondph.common.model.DisasterModel;
import java.util.List;

public interface DisasterMappingService {

    List<String> getDisasterTypes();

    List<DisasterModel> getDisasters();

    List<DisasterModel> getDisastersByType(String decryptedType);

    List<DisasterCircleInfo> getAllDisasterCircles();

    List<DisasterCircleInfo> getDisasterCirclesByDisasterId(int disasterId);

    List<BeneficiaryMarker> getBeneficiaries();

    List<BeneficiaryMarker> getBeneficiariesInsideCircle(double circleLat, double circleLon, double radiusMeters);

    DisasterModel getDisasterById(int disasterId);
}
