package com.ionres.respondph.common.dao;

import com.ionres.respondph.common.interfaces.EvacSiteMappingDAO;
import com.ionres.respondph.common.model.EvacSiteModel;
import com.ionres.respondph.database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EvacSiteMappingDAOImpl implements EvacSiteMappingDAO {
    private static final Logger LOGGER = Logger.getLogger(EvacSiteMappingDAOImpl.class.getName());
    private final DBConnection connection;

    public EvacSiteMappingDAOImpl(DBConnection connection) {
        this.connection = connection;
    }

    @Override
    public List<EvacSiteModel> getAllEvacSites() {
        List<EvacSiteModel> evacSites = new ArrayList<>();
        String sql = "SELECT evac_id, lat, `long`, name, capacity, notes FROM evac_site " +
                "WHERE lat IS NOT NULL AND `long` IS NOT NULL";

        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int evacId = rs.getInt("evac_id");
                String lat = rs.getString("lat");
                String lon = rs.getString("long");
                String name = rs.getString("name");
                String capacity = rs.getString("capacity");
                String notes = rs.getString("notes");

                evacSites.add(new EvacSiteModel(evacId, lat, lon, name, capacity, notes));
            }

            LOGGER.info("Fetched " + evacSites.size() + " encrypted evacuation sites from database");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching evacuation sites", e);
        }

        return evacSites;
    }

    @Override
    public EvacSiteModel getEvacSiteById(int evacId) {
        String sql = "SELECT evac_id, lat, `long`, name, capacity, notes FROM evac_site " +
                "WHERE evac_id = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, evacId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("evac_id");
                    String lat = rs.getString("lat");
                    String lon = rs.getString("long");
                    String name = rs.getString("name");
                    String capacity = rs.getString("capacity");
                    String notes = rs.getString("notes");

                    return new EvacSiteModel(id, lat, lon, name, capacity, notes);
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching evacuation site by ID", e);
        }

        return null;
    }
}