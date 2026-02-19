package com.ionres.respondph.sendsms;

import com.ionres.respondph.database.DBConnection;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomEvacMessageDAOImpl implements CustomEvacMessageDAO {

    private static final Logger LOGGER = Logger.getLogger(CustomEvacMessageDAOImpl.class.getName());

    private static final String DEFAULT_MESSAGE =
            "{name}, gin-assign ka sa {evacSite} nga evacuation center. Palihog magpadulong dayon didto para sa imo kasiguraduhan. Upda imo pamilya.";

    @Override
    public boolean saveCustomMessage(String messageTemplate) {
        if (messageTemplate == null || messageTemplate.trim().isEmpty()) {
            LOGGER.warning("Cannot save empty message template");
            return false;
        }

        return setActiveMessage(messageTemplate, "system");
    }

    @Override
    public String getActiveCustomMessage() {
        String sql = "SELECT message_template FROM custom_evac_messages " +
                "WHERE is_active = TRUE " +
                "ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                String template = rs.getString("message_template");
                LOGGER.info("Retrieved active custom message from database");
                return template;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving active custom message", e);
        }

        LOGGER.info("No active custom message found, returning default");
        return DEFAULT_MESSAGE;
    }

    @Override
    public boolean hasActiveCustomMessage() {
        String sql = "SELECT COUNT(*) as count FROM custom_evac_messages WHERE is_active = TRUE";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking for active custom message", e);
        }

        return false;
    }

    @Override
    public boolean setActiveMessage(String messageTemplate, String createdBy) {
        if (messageTemplate == null || messageTemplate.trim().isEmpty()) {
            LOGGER.warning("Cannot set empty message template");
            return false;
        }

        Connection conn = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            // Deactivate all previous messages
            String deactivateSql = "UPDATE custom_evac_messages SET is_active = FALSE";
            try (PreparedStatement ps = conn.prepareStatement(deactivateSql)) {
                int deactivated = ps.executeUpdate();
                LOGGER.info("Deactivated " + deactivated + " previous message(s)");
            }

            // Insert new active message
            String insertSql = "INSERT INTO custom_evac_messages " +
                    "(message_template, is_active, created_by, notes) " +
                    "VALUES (?, TRUE, ?, 'User-defined custom message')";

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, messageTemplate.trim());
                ps.setString(2, createdBy != null ? createdBy : "system");

                int rows = ps.executeUpdate();

                if (rows > 0) {
                    conn.commit();
                    LOGGER.info("Successfully saved new active custom message");
                    return true;
                } else {
                    conn.rollback();
                    LOGGER.warning("Failed to insert new message");
                    return false;
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error setting active message", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction", ex);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error resetting auto-commit", e);
                }
            }
        }
    }

    @Override
    public String getDefaultMessage() {
        return DEFAULT_MESSAGE;
    }
}