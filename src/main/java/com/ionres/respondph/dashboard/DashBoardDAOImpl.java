package com.ionres.respondph.dashboard;

import com.ionres.respondph.common.model.DisasterCircleEncrypted;
import com.ionres.respondph.common.model.EvacSiteMappingModel;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.ResourceUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashBoardDAOImpl implements DashBoardDAO {
    private static final Logger LOGGER = Logger.getLogger(DashBoardDAOImpl.class.getName());
    private final DBConnection connection;

    public DashBoardDAOImpl(DBConnection connection) {
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
        return getCount("SELECT COUNT(*) FROM aid_type");
    }

    @Override
    public int getTotalEvacutaionSites() {
        return getCount("SELECT COUNT(*) FROM evac_site");}

    @Override
    public int getCount(String sql) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = connection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Database error getting count", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }
        return 0;
    }

    @Override
    public List<DisasterCircleEncrypted> fetchAllEncrypted() {
        List<DisasterCircleEncrypted> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = connection.getConnection();
            // Fetch all fields to use DisasterCircleEncrypted (dashboard doesn't need name/type, but we'll pass empty strings)
            String sql = "SELECT disaster_id, lat, `long`, radius, type, name FROM disaster " +
                    "WHERE lat IS NOT NULL AND `long` IS NOT NULL AND radius IS NOT NULL";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                int disasterId = rs.getInt("disaster_id");
                String lat = rs.getString("lat");
                String lon = rs.getString("long");
                String radius = rs.getString("radius");
                String type = rs.getString("type");
                String name = rs.getString("name");

                // Use DisasterCircleEncrypted instead of EncryptedCircle for consistency
                list.add(new DisasterCircleEncrypted(lat, lon, radius, disasterId,
                        name != null ? name : "", type != null ? type : ""));
            }

            LOGGER.info("Fetched " + list.size() + " disaster circles from database");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching encrypted circles", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }
        return list;
    }

    @Override
    public List<BeneficiariesMappingModel> fetchAllBeneficiaries() {
        List<BeneficiariesMappingModel> list = new ArrayList<>();
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
                try {
                    int id = rs.getInt("beneficiary_id");
                    String encryptedFirstName = rs.getString("first_name");
                    String encryptedMiddleName = rs.getString("middle_name");
                    String encryptedLastName = rs.getString("last_name");
                    String lat = rs.getString("latitude");
                    String lng = rs.getString("longitude");

                    String encryptedFullName = encryptedFirstName + "|" +
                            (encryptedMiddleName != null ? encryptedMiddleName : "") + "|" +
                            encryptedLastName;

                    list.add(new BeneficiariesMappingModel(id, encryptedFullName, lat, lng));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error processing beneficiary row", e);
                }
            }

            LOGGER.info("Fetched " + list.size() + " beneficiaries from database");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching beneficiaries", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }
        return list;
    }

    @Override
    public List<EvacSiteMappingModel> fetchAllEvacSites() {
        List<EvacSiteMappingModel> list = new ArrayList<>();
        String sql = "SELECT evac_id, name, lat, `long`, capacity " +
                "FROM evac_site " +
                "WHERE lat IS NOT NULL AND `long` IS NOT NULL";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = connection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                try {
                    int evacId = rs.getInt("evac_id");
                    String name = rs.getString("name");
                    String lat = String.valueOf(rs.getDouble("lat"));
                    String lng = String.valueOf(rs.getDouble("long"));
                    String capacity = String.valueOf(rs.getInt("capacity"));

                    list.add(new EvacSiteMappingModel(evacId, name, lat, lng, capacity));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error processing evac site row", e);
                }
            }

            LOGGER.info("Fetched " + list.size() + " evacuation sites from database");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching evacuation sites", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }
        return list;
    }
}