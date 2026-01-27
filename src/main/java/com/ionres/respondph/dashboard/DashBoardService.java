package com.ionres.respondph.dashboard;

import com.ionres.respondph.common.model.BeneficiaryMarker;
import com.ionres.respondph.common.model.DisasterCircleInfo;

import java.util.List;

public interface DashBoardService {
    int fetchTotalBeneficiary();
    int fetchTotalDisasters();
    int fetchTotalAids();
    List<DisasterCircleInfo> getCircles();
    List<BeneficiaryMarker> getBeneficiaries();
}
