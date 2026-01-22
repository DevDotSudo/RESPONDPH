package com.ionres.respondph.dashboard;

import com.ionres.respondph.common.model.EncryptedCircle;

import java.util.List;

public interface DashBoardDAO {
    int getTotalBeneficiaries();
    int getTotalDisasters();
    int getTotalAids();
    int getCount(String sql);
    List<EncryptedCircle> fetchAllEncrypted();
}
