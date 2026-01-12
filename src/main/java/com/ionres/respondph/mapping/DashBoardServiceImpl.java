package com.ionres.respondph.mapping;

import com.ionres.respondph.database.DBConnection;

public class DashBoardServiceImpl implements DashBoardService{
    private final DashBoardDAO dashBoardDAO;

    public DashBoardServiceImpl(DBConnection connection){
        this.dashBoardDAO = new DashBoardServiceImplDAO(connection);
    }
    @Override
    public int fetchTotalBeneficiary() {
        return dashBoardDAO.getTotalBeneficiaries();
    }

    @Override
    public int fetchTotalDisasters() {
        return dashBoardDAO.getTotalDisasters();
    }

    @Override
    public int fetchTotalAids() {
        return dashBoardDAO.getTotalAids();
    }
}
