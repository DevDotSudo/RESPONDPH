package com.ionres.respondph.dashboard;

import com.ionres.respondph.common.model.BeneficiaryMarker;
import com.ionres.respondph.common.model.DisasterCircleInfo;
import com.ionres.respondph.common.model.EvacSiteMarker;

import java.util.List;

public interface DashBoardService {
    int fetchTotalBeneficiary();
    int fetchTotalDisasters();
    int fetchTotalAids();
    int fetchTotalEvacuationSites();
    List<DisasterCircleInfo> getCircles();
    List<BeneficiaryMarker> getBeneficiaries();
    List<EvacSiteMarker> getEvacSites();
}