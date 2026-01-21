package com.ionres.respondph.dashboard;

public interface DashBoardDAO {
    int getTotalBeneficiaries();
    int getTotalDisasters();
    int getTotalAids();
    int getCount(String sql);
}
