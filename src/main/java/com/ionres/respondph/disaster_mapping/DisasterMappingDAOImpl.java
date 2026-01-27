package com.ionres.respondph.disaster_mapping;

import com.ionres.respondph.common.model.BeneficiaryEncrypted;
import com.ionres.respondph.common.model.DisasterCircleEncrypted;
import com.ionres.respondph.common.model.DisasterModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.ResourceUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DisasterMappingDAOImpl implements DisasterMappingDAO {
    private static final Logger LOGGER = Logger.getLogger(DisasterMappingDAOImpl.class.getName());
    private final DBConnection connection;

    public DisasterMappingDAOImpl(DBConnection connection) {
        this.connection = connection;
    }

    @Override
    public List<String> getDisasterTypes() {
        List<String> types = new ArrayList<>();
        String sql = "SELECT DISTINCT type FROM disaster WHERE type IS NOT NULL ORDER BY type";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = connection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                String type = rs.getString("type");
                if (type != null && !type.trim().isEmpty()) {
                    types.add(type);
                }
            }

            LOGGER.info("Fetched " + types.size() + " encrypted disaster types from database");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching disaster types", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }
        return types;
    }

    @Override
    public List<DisasterModel> getAllDisasters() {
        List<DisasterModel> disasters = new ArrayList<>();
        String sql = "SELECT disaster_id, type, name FROM disaster";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = connection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("disaster_id");
                String type = rs.getString("type");
                String name = rs.getString("name");

                disasters.add(new DisasterModel(id, type, name));
            }

            LOGGER.info("Fetched " + disasters.size() + " encrypted disasters from database");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching disasters", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }

        return disasters;
    }

    @Override
    public List<DisasterModel> getDisastersByType(String encryptedType) {
        List<DisasterModel> disasters = new ArrayList<>();
        String sql = "SELECT disaster_id, type, name FROM disaster WHERE type = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = connection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, encryptedType);
            rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("disaster_id");
                String disasterType = rs.getString("type");
                String name = rs.getString("name");

                disasters.add(new DisasterModel(id, disasterType, name));
            }

            LOGGER.info("Fetched " + disasters.size() + " disasters of specified type from database");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching disasters by type", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }

        return disasters;
    }

    @Override
    public List<DisasterCircleEncrypted> getAllDisasterCircles() {
        List<DisasterCircleEncrypted> circles = new ArrayList<>();
        String sql = "SELECT disaster_id, lat, `long`, radius, type, name FROM disaster " +
                "WHERE lat IS NOT NULL AND `long` IS NOT NULL AND radius IS NOT NULL";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = connection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                int disasterId = rs.getInt("disaster_id");
                String lat = rs.getString("lat");
                String lon = rs.getString("long");
                String radius = rs.getString("radius");
                String type = rs.getString("type");
                String name = rs.getString("name");

                circles.add(new DisasterCircleEncrypted(lat, lon, radius, disasterId, name, type));
            }

            LOGGER.info("Fetched " + circles.size() + " encrypted disaster circles from database");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching disaster circles", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }

        return circles;
    }

    @Override
    public List<DisasterCircleEncrypted> getDisasterCirclesByDisasterId(int disasterId) {
        List<DisasterCircleEncrypted> circles = new ArrayList<>();
        String sql = "SELECT disaster_id, lat, `long`, radius, type, name FROM disaster " +
                "WHERE disaster_id = ? AND lat IS NOT NULL AND `long` IS NOT NULL AND radius IS NOT NULL";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = connection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, disasterId);
            rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("disaster_id");
                String lat = rs.getString("lat");
                String lon = rs.getString("long");
                String radius = rs.getString("radius");
                String type = rs.getString("type");
                String name = rs.getString("name");

                circles.add(new DisasterCircleEncrypted(lat, lon, radius, id, name, type));
            }

            LOGGER.info("Fetched " + circles.size() + " circles for disaster ID: " + disasterId);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching disaster circles by ID", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }

        return circles;
    }

    @Override
    public List<BeneficiaryEncrypted> getAllBeneficiaries() {
        List<BeneficiaryEncrypted> beneficiaries = new ArrayList<>();
        String sql = "SELECT beneficiary_id, first_name, middle_name, last_name, latitude, longitude " +
                "FROM beneficiary " +
                "WHERE latitude IS NOT NULL AND longitude IS NOT NULL " +
                "AND latitude != '' AND longitude != ''";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = connection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("beneficiary_id");
                String encryptedFirstName = rs.getString("first_name");
                String encryptedMiddleName = rs.getString("middle_name");
                String encryptedLastName = rs.getString("last_name");
                String lat = rs.getString("latitude");
                String lng = rs.getString("longitude");

                String encryptedFullName = encryptedFirstName + "|" +
                        (encryptedMiddleName != null ? encryptedMiddleName : "") + "|" +
                        encryptedLastName;

                beneficiaries.add(new BeneficiaryEncrypted(id, encryptedFullName, lat, lng));
            }

            LOGGER.info("Fetched " + beneficiaries.size() + " encrypted beneficiaries from database");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching beneficiaries", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }

        return beneficiaries;
    }

    @Override
    public DisasterModel getDisasterById(int disasterId) {
        String sql = "SELECT disaster_id, type, name FROM disaster WHERE disaster_id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = connection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, disasterId);
            rs = ps.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("disaster_id");
                String type = rs.getString("type");
                String name = rs.getString("name");

                return new DisasterModel(id, type, name);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching disaster by ID", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }

        return null;
    }
}