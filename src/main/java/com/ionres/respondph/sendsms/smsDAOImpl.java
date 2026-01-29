package com.ionres.respondph.sendsms;

import com.ionres.respondph.database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class smsDAOImpl implements smsDAO{

    @Override
    public void saveSMS(smsModel sms) {
        String sql = "INSERT INTO sms_logs(date_sent, fullname, phonenumber, message, status) VALUES (?,?,?,?,?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, sms.getDateSent());
            ps.setString(2, sms.getFullname());
            ps.setString(3, sms.getPhonenumber());
            ps.setString(4, sms.getMessage());
            ps.setString(5, sms.getStatus());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<smsModel> getAllSMS() {

        List<smsModel> logs = new ArrayList<>();
        String sql = "SELECT * FROM sms_logs ORDER BY date_sent DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                smsModel log = new smsModel();
                log.setDateSent(rs.getTimestamp("date_sent"));
                log.setFullname(rs.getString("fullname"));
                String phone = rs.getString("phonenumber");
                log.setPhonenumber(phone);
                log.setPhoneString(phone);
                log.setMessage(rs.getString("message"));
                log.setStatus(rs.getString("status"));
                logs.add(log);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    @Override
    public void resendSMS(smsModel sms) {
        if (sms == null) return;
        String phone = sms.getPhonenumber();
        Timestamp ts = sms.getDateSent();

        if (phone != null && !phone.isEmpty()) {
            String sql = "UPDATE sms_logs SET status = ? WHERE phonenumber = ? AND date_sent = (SELECT mx FROM (SELECT MAX(date_sent) AS mx FROM sms_logs WHERE phonenumber = ?) AS tmp)";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, sms.getStatus());
                ps.setString(2, phone);
                ps.setString(3, phone);
                ps.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }
        if (ts != null) {
            String sql = "UPDATE sms_logs SET status = ? WHERE date_sent = ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, sms.getStatus());
                ps.setTimestamp(2, ts);
                ps.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
