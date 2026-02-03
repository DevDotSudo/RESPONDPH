package com.ionres.respondph.sendsms;

import com.ionres.respondph.database.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SmsDAOImpl implements SmsDAO {

    @Override
    public void saveSMS(SmsModel sms) {
        String sql = "INSERT INTO sms_logs(beneficiary_id, date_sent, fullname, phonenumber, message, status, send_method) VALUES (?,?,?,?,?,?,?)";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, sms.getBeneficiaryID());
            ps.setTimestamp(2, sms.getDateSent());
            ps.setString(3, sms.getFullname());
            ps.setString(4, sms.getPhonenumber());
            ps.setString(5, sms.getMessage());
            ps.setString(6, sms.getStatus());
            ps.setString(7, sms.getSendMethod());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    sms.setMessageID(generatedKeys.getInt(1));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error saving SMS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<SmsModel> getAllSMS() {
        List<SmsModel> logs = new ArrayList<>();
        String sql = "SELECT * FROM sms_logs ORDER BY date_sent DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                logs.add(mapResultSetToModel(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving all SMS: " + e.getMessage());
            e.printStackTrace();
        }
        return logs;
    }

    @Override
    public List<SmsModel> getSMSByStatus(String status) {
        List<SmsModel> logs = new ArrayList<>();
        String sql = "SELECT * FROM sms_logs WHERE status = ? ORDER BY date_sent DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToModel(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving SMS by status: " + e.getMessage());
            e.printStackTrace();
        }
        return logs;
    }

    @Override
    public List<SmsModel> getSMSByPhoneNumber(String phoneNumber) {
        List<SmsModel> logs = new ArrayList<>();
        String sql = "SELECT * FROM sms_logs WHERE phonenumber = ? ORDER BY date_sent DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, phoneNumber);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToModel(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving SMS by phone number: " + e.getMessage());
            e.printStackTrace();
        }
        return logs;
    }

    @Override
    public void updateSMSStatus(int messageID, String status) {
        String sql = "UPDATE sms_logs SET status = ? WHERE message_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, messageID);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error updating SMS status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deleteSMS(int messageID) {
        String sql = "DELETE FROM sms_logs WHERE message_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, messageID);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error deleting SMS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public SmsModel getSMSById(int messageID) {
        String sql = "SELECT * FROM sms_logs WHERE message_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, messageID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToModel(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving SMS by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private SmsModel mapResultSetToModel(ResultSet rs) throws SQLException {
        SmsModel log = new SmsModel();
        log.setMessageID(rs.getInt("message_id"));
        log.setBeneficiaryID(rs.getInt("beneficiary_id"));
        log.setDateSent(rs.getTimestamp("date_sent"));
        log.setFullname(rs.getString("fullname"));
        String phone = rs.getString("phonenumber");
        log.setPhonenumber(phone);
        log.setPhoneString(phone);
        log.setMessage(rs.getString("message"));
        log.setStatus(rs.getString("status"));
        log.setSendMethod(rs.getString("send_method"));
        return log;
    }
}