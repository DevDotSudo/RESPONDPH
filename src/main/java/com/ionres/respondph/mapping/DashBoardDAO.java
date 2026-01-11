package com.ionres.respondph.mapping;

public interface DashBoardDAO {
    int getTotalBeneficiaries();
    int getTotalDisasters();
    int getTotalAids();
    int getCount(String sql);
}
