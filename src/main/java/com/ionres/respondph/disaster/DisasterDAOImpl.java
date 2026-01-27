package com.ionres.respondph.disaster;

import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.util.CryptographyManager;
import com.ionres.respondph.util.ResourceUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DisasterDAOImpl implements DisasterDAO {
    private static final Logger LOGGER = Logger.getLogger(DisasterDAOImpl.class.getName());
    private final DBConnection dbConnection;
    private static final Cryptography CRYPTO = CryptographyManager.getInstance();

    public DisasterDAOImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public boolean saving(DisasterModel dm) {
        String sql = "INSERT INTO disaster (type, name, date, lat, `long`, radius, notes, reg_date)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = dbConnection.getConnection();
            ps = conn.prepareStatement(sql);

            ps.setString(1, dm.getDisasterType());
            ps.setString(2, dm.getDisasterName());
            ps.setString(3, dm.getDate());
            ps.setString(4, dm.getLat());
            ps.setString(5, dm.getLongi());
            ps.setString(6, dm.getRadius());
            ps.setString(7, dm.getNotes());
            ps.setString(8, dm.getRegDate());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error occurred while saving disaster", e);
            return false;
        } finally {
            ResourceUtils.closePreparedStatement(ps);
        }
    }

    @Override
    public List<DisasterModel> getAll() {
        List<DisasterModel> disaster = new ArrayList<>();
        String sql = "SELECT disaster_id, type, name, date, notes, reg_date  FROM disaster";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = dbConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                DisasterModel dm = new DisasterModel();

                List<String> encrypted = new ArrayList<>();
                encrypted.add(rs.getString("type"));
                encrypted.add(rs.getString("name"));
                encrypted.add(rs.getString("date"));
                encrypted.add(rs.getString("notes"));
                encrypted.add(rs.getString("reg_date"));

                List<String> decrypted = CRYPTO.decrypt(encrypted);

                dm.setDisasterId(rs.getInt("disaster_id"));
                dm.setDisasterType(decrypted.get(0));
                dm.setDisasterName(decrypted.get(1));
                dm.setDate(decrypted.get(2));
                dm.setNotes(decrypted.get(3));
                dm.setRegDate(decrypted.get(4));

                disaster.add(dm);
            }

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error fetching disasters", ex);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }

        return disaster;
    }

    @Override
    public boolean delete(DisasterModel dm) {
        String sql = "DELETE FROM disaster WHERE disaster_id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = dbConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, dm.getDisasterId());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error occurred while deleting disaster", e);
            return false;
        } finally {
            ResourceUtils.closePreparedStatement(ps);
        }
    }

    @Override
    public boolean update(DisasterModel dm) {
        String sql = "UPDATE disaster SET " +
                "type = ?, name = ?, date = ?, lat = ?, `long` = ?, radius = ?, notes = ?, reg_date = ? " +
                "WHERE disaster_id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = dbConnection.getConnection();
            ps = conn.prepareStatement(sql);

            ps.setString(1, dm.getDisasterType());
            ps.setString(2, dm.getDisasterName());
            ps.setString(3, dm.getDate());
            ps.setString(4, dm.getLat());
            ps.setString(5, dm.getLongi());
            ps.setString(6, dm.getRadius());
            ps.setString(7, dm.getNotes());
            ps.setString(8, dm.getRegDate());
            ps.setInt(9, dm.getDisasterId());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error occurred while updating disaster", e);
            return false;
        } finally {
            ResourceUtils.closePreparedStatement(ps);
        }
    }

    @Override
    public DisasterModel getById(int id) {
        DisasterModel dm = null;
        String sql = "SELECT * FROM disaster WHERE disaster_id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = dbConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            rs = ps.executeQuery();

            if (rs.next()) {
                dm = new DisasterModel();

                List<String> encrypted = new ArrayList<>();
                encrypted.add(rs.getString("type"));
                encrypted.add(rs.getString("name"));
                encrypted.add(rs.getString("date"));
                encrypted.add(rs.getString("lat"));
                encrypted.add(rs.getString("long"));
                encrypted.add(rs.getString("radius"));
                encrypted.add(rs.getString("notes"));
                encrypted.add(rs.getString("reg_date"));

                List<String> decrypted = CRYPTO.decrypt(encrypted);

                dm.setDisasterId(rs.getInt("disaster_id"));
                dm.setDisasterType(decrypted.get(0));
                dm.setDisasterName(decrypted.get(1));
                dm.setDate(decrypted.get(2));
                dm.setLat(decrypted.get(3));
                dm.setLongi(decrypted.get(4));
                dm.setRadius(decrypted.get(5));
                dm.setNotes(decrypted.get(6));
                dm.setRegDate(decrypted.get(7));
            }

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error fetching disaster by ID", ex);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }
        return dm;
    }

    @Override
    public List<String[]> getEncryptedDisasters() {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT disaster_id, lat, `long`, radius FROM disaster";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = dbConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new String[]{
                        rs.getString("disaster_id"),
                        rs.getString("lat"),
                        rs.getString("long"),
                        rs.getString("radius")
                });
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching encrypted disasters", e);
        } finally {
            ResourceUtils.closeResources(rs, ps);
        }
        return list;
    }
}