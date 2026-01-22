package com.ionres.respondph.dashboard;

import com.ionres.respondph.common.model.CircleArea;
import com.ionres.respondph.common.model.EncryptedCircle;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.ConfigLoader;
import com.ionres.respondph.util.Cryptography;

import java.util.ArrayList;
import java.util.List;

public class DashBoardServiceImpl implements DashBoardService{
    private final DashBoardDAO dashBoardDAO;
    private final  Cryptography cs;

    public DashBoardServiceImpl(DBConnection connection){
        this.dashBoardDAO = new DashBoardServiceImplDAO(connection);
        String secretKey = ConfigLoader.get("secretKey");
        this.cs = new Cryptography(secretKey);
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

    @Override
    public List<CircleArea> getCircles() {
        List<CircleArea> result = new ArrayList<>();

        for (EncryptedCircle e : dashBoardDAO.fetchAllEncrypted()) {
            try {
                double lat = Double.parseDouble(cs.decryptWithOneParameter(e.lat));
                double lon = Double.parseDouble(cs.decryptWithOneParameter(e.lon));
                double radius = Double.parseDouble(cs.decryptWithOneParameter(e.radius));

                result.add(new CircleArea(lat, lon, radius));
            }catch (Exception ex){
                System.out.println(ex.getMessage());
            }
        }

        return result;
    }
}
