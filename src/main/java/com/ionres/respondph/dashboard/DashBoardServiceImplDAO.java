package com.ionres.respondph.dashboard;

import com.ionres.respondph.common.model.CircleArea;
import com.ionres.respondph.common.model.EncryptedCircle;
import com.ionres.respondph.database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DashBoardServiceImplDAO implements DashBoardDAO{
    private final DBConnection connection;
    private Connection conn;

    public DashBoardServiceImplDAO(DBConnection connection){
        this.connection = connection;
    }

    @Override
    public int getTotalBeneficiaries() {
        return getCount("SELECT COUNT(*) FROM beneficiary");
    }

    @Override
    public int getTotalDisasters() {
        return getCount("SELECT COUNT(*) FROM disaster");
    }

    @Override
    public int getTotalAids() {
        return getCount("SELECT COUNT(*) FROM admin");
    }

    @Override
    public int getCount(String sql) {
        try{
            conn = connection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                return rs.getInt(1);
            }

        }
        catch (Exception e){
            System.out.println("Database Error" + e.getMessage());
        }

        return 0;
    }

    @Override
    public List<EncryptedCircle> fetchAllEncrypted() {
        List<EncryptedCircle> list = new ArrayList<>();

        String sql = "SELECT lat, `long`, radius FROM disaster";

        try {
            conn = connection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new EncryptedCircle(
                        rs.getString("lat"),
                        rs.getString("long"),
                        rs.getString("radius")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}
