package com.ionres.respondph.dashboard;

import com.ionres.respondph.database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DashBoardServiceImplDAO implements DashBoardDAO{
    private  final DBConnection connection;
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
            conn = DBConnection.getInstance().getConnection();
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
}
