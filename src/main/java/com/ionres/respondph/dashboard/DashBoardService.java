package com.ionres.respondph.dashboard;

import com.ionres.respondph.common.model.CircleArea;

import java.util.List;

public interface DashBoardService {
    int fetchTotalBeneficiary();
    int fetchTotalDisasters();
    int fetchTotalAids();
    List<CircleArea> getCircles();
}
