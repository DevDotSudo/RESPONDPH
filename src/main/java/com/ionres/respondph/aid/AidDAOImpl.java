package com.ionres.respondph.aid;

import com.ionres.respondph.aid.KMeansAidDistribution.BeneficiaryCluster;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.util.GeographicUtils;
import javax.swing.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

public class AidDAOImpl implements AidDAO {

    private final DBConnection dbConnection;
    private final Cryptography cs = new Cryptography("f3ChNqKb/MumOr5XzvtWrTyh0YZsc2cw+VyoILwvBm8=");
    private Connection conn;

    public AidDAOImpl(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public boolean saving(AidModel aid) {
        String sql = "INSERT INTO aid (beneficiary_id, disaster_id, name, date, qty, cost, provider, aid_type_id, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            String encryptedName = cs.encryptWithOneParameter(aid.getName());
            String encryptedProvider = cs.encryptWithOneParameter(aid.getProvider());
            ps.setInt(1, aid.getBeneficiaryId());
            if (aid.getDisasterId() > 0) {
                ps.setInt(2, aid.getDisasterId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, encryptedName);
            ps.setDate(4, Date.valueOf(aid.getDate()));
            ps.setDouble(5, aid.getQuantity());
            ps.setDouble(6, aid.getCost());
            ps.setString(7, encryptedProvider);
            ps.setInt(8, aid.getAidTypeId());
            ps.setString(9, aid.getNotes());
            int rowsAffected = ps.executeUpdate();
            ps.close();
            if (rowsAffected > 0) {
                String disasterInfo = aid.getDisasterId() > 0 ? " for disaster #" + aid.getDisasterId() : " (General Distribution)";
                System.out.println("Aid saved for beneficiary #" + aid.getBeneficiaryId() + disasterInfo);
            }
            return rowsAffected > 0;
        } catch (Exception e) {
            System.err.println("Error saving aid: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Database error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        } finally {
            closeConnection();
        }
    }

    @Override
    public List<AidModel> getAll() {
        List<AidModel> aidList = new ArrayList<>();
        String sql = "SELECT * FROM aid ORDER BY date DESC";
        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) { aidList.add(mapResultSetToAidModel(rs)); }
            rs.close(); ps.close();
        } catch (Exception e) { System.err.println("Error fetching all aid records: " + e.getMessage()); e.printStackTrace(); }
        finally { closeConnection(); }
        return aidList;
    }

    @Override
    public boolean delete(AidModel aid) {
        String sql = "DELETE FROM aid WHERE aid_id = ?";
        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, aid.getAidId());
            int rowsAffected = ps.executeUpdate();
            ps.close();
            return rowsAffected > 0;
        } catch (SQLException e) { System.err.println("Error deleting aid: " + e.getMessage()); e.printStackTrace(); return false; }
        finally { closeConnection(); }
    }

    @Override
    public boolean update(AidModel aid) {
        String sql = "UPDATE aid SET name=?, date=?, qty=?, cost=?, provider=?, notes=? WHERE aid_id=?";
        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            String encryptedName = cs.encryptWithOneParameter(aid.getName());
            String encryptedProvider = cs.encryptWithOneParameter(aid.getProvider());
            ps.setString(1, encryptedName);
            ps.setDate(2, Date.valueOf(aid.getDate()));
            ps.setDouble(3, aid.getQuantity());
            ps.setDouble(4, aid.getCost());
            ps.setString(5, encryptedProvider);
            ps.setString(6, aid.getNotes());
            ps.setInt(7, aid.getAidId());
            int rowsAffected = ps.executeUpdate();
            ps.close();
            return rowsAffected > 0;
        } catch (Exception e) { System.err.println("Error updating aid: " + e.getMessage()); e.printStackTrace(); return false; }
        finally { closeConnection(); }
    }

    // =========================================================================
    //  getBeneficiariesWithScores
    //
    //  FIX: getDisasterCircle() is now called BEFORE opening the main
    //       connection/ResultSet.  Previously it was called AFTER rs =
    //       ps.executeQuery(), and because DBConnection is a shared singleton,
    //       getDisasterCircle closing its tempConn also closed the connection
    //       that `rs` depended on — causing "Not a navigable ResultSet".
    // =========================================================================
    @Override
    public List<BeneficiaryCluster> getBeneficiariesWithScores(int aidTypeId, int disasterId) {
        List<BeneficiaryCluster> beneficiaries = new ArrayList<>();

        // ── STEP 1: Fetch disaster circle FIRST, before opening the main RS ──
        // This must complete (and close its own connection) before we open ours.
        DisasterCircle disasterCircle = null;
        if (disasterId > 0) {
            disasterCircle = getDisasterCircle(disasterId);
            if (disasterCircle != null) {
                System.out.println("Disaster Circle: (" + disasterCircle.lat + ", " + disasterCircle.lon
                        + ")  radius: " + disasterCircle.radius + "m");
            } else {
                System.out.println("No disaster circle found — including all scored beneficiaries");
            }
        }

        // ── STEP 2: Now open the main query connection ─────────────────────
        String sql;
        if (disasterId > 0) {
            sql = "SELECT ahs.beneficiary_id, ahs.final_score, ahs.score_category, " +
                    "       b.latitude, b.longitude " +
                    "FROM aid_and_household_score ahs " +
                    "INNER JOIN beneficiary b ON ahs.beneficiary_id = b.beneficiary_id " +
                    "WHERE ahs.aid_type_id = ? " +
                    "AND b.latitude IS NOT NULL AND b.longitude IS NOT NULL " +
                    "AND b.latitude != '' AND b.longitude != '' " +
                    "ORDER BY ahs.final_score DESC";
        } else {
            sql = "SELECT ahs.beneficiary_id, MAX(ahs.final_score) AS final_score, " +
                    "       MAX(ahs.score_category) AS score_category " +
                    "FROM aid_and_household_score ahs " +
                    "WHERE ahs.aid_type_id = ? " +
                    "GROUP BY ahs.beneficiary_id " +
                    "ORDER BY final_score DESC";
        }

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, aidTypeId);

            System.out.println("========== FETCHING ELIGIBLE BENEFICIARIES ==========");
            System.out.println("Aid Type ID : " + aidTypeId);
            if (disasterId > 0) {
                System.out.println("Disaster ID : " + disasterId);
                System.out.println("Eligibility : Scored beneficiaries inside disaster circle");
            } else {
                System.out.println("Disaster    : NONE (General Aid)");
                System.out.println("Eligibility : All scored beneficiaries for this aid type");
            }
            System.out.println("=====================================================");

            ResultSet rs = ps.executeQuery();

            int count = 0, filteredOut = 0;

            while (rs.next()) {
                int    beneficiaryId = rs.getInt("beneficiary_id");
                double finalScore    = rs.getDouble("final_score");
                String scoreCategory = rs.getString("score_category");

                if (disasterId > 0 && disasterCircle != null) {
                    try {
                        String latStr = cs.decryptWithOneParameter(rs.getString("latitude"));
                        String lonStr = cs.decryptWithOneParameter(rs.getString("longitude"));
                        double bLat   = Double.parseDouble(latStr);
                        double bLon   = Double.parseDouble(lonStr);
                        double distance = GeographicUtils.calculateDistance(
                                bLat, bLon, disasterCircle.lat, disasterCircle.lon);
                        if (distance <= disasterCircle.radius) {
                            beneficiaries.add(new BeneficiaryCluster(beneficiaryId, finalScore, scoreCategory));
                            if (++count <= 5)
                                System.out.println("  \u2713 " + count + ". Beneficiary #" + beneficiaryId
                                        + " | Score: " + finalScore
                                        + " | Dist: " + String.format("%.0f", distance) + "m");
                        } else {
                            if (++filteredOut <= 3)
                                System.out.println("  \u2717 Beneficiary #" + beneficiaryId
                                        + " OUTSIDE area (" + String.format("%.0f", distance) + "m)");
                        }
                    } catch (Exception e) {
                        System.err.println("  \u26A0 Coord error for beneficiary #" + beneficiaryId
                                + ": " + e.getMessage());
                    }
                } else {
                    beneficiaries.add(new BeneficiaryCluster(beneficiaryId, finalScore, scoreCategory));
                    if (++count <= 5)
                        System.out.println("  " + count + ". Beneficiary #" + beneficiaryId
                                + " | Score: " + finalScore);
                }
            }
            rs.close();
            ps.close();

            System.out.println("\n\u2713 Eligible: " + beneficiaries.size()
                    + (disasterId > 0 && disasterCircle != null
                    ? "  \u2717 Outside area: " + filteredOut : ""));
            System.out.println("=====================================================\n");

            if (beneficiaries.isEmpty()) debugNoEligibleBeneficiaries(aidTypeId, disasterId);

        } catch (SQLException e) {
            System.err.println("Error fetching beneficiaries: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return beneficiaries;
    }

    // =========================================================================
    //  getBeneficiariesWithScoresByBarangays
    //
    //  FIX: Same fix applied — getDisasterCircle() called BEFORE opening the
    //       main connection so it doesn't close the connection under our RS.
    // =========================================================================
    @Override
    public List<BeneficiaryCluster> getBeneficiariesWithScoresByBarangays(
            int aidTypeId, int disasterId, List<String> barangays) {

        List<BeneficiaryCluster> beneficiaries = new ArrayList<>();
        if (barangays == null || barangays.isEmpty()) return beneficiaries;

        // ── STEP 1: Fetch disaster circle FIRST ───────────────────────────
        DisasterCircle disasterCircle = null;
        if (disasterId > 0) {
            disasterCircle = getDisasterCircle(disasterId);
            if (disasterCircle != null)
                System.out.println("Disaster Circle: (" + disasterCircle.lat + ", "
                        + disasterCircle.lon + ")  radius: " + disasterCircle.radius + "m");
        }

        // ── STEP 2: Open main query ────────────────────────────────────────
        String sql;
        if (disasterId > 0) {
            sql = "SELECT ahs.beneficiary_id, ahs.final_score, ahs.score_category, " +
                    "       b.barangay, b.latitude, b.longitude " +
                    "FROM aid_and_household_score ahs " +
                    "INNER JOIN beneficiary b ON ahs.beneficiary_id = b.beneficiary_id " +
                    "WHERE ahs.aid_type_id = ? " +
                    "AND b.latitude IS NOT NULL AND b.longitude IS NOT NULL " +
                    "AND b.latitude != '' AND b.longitude != '' " +
                    "ORDER BY ahs.final_score DESC";
        } else {
            sql = "SELECT ahs.beneficiary_id, MAX(ahs.final_score) AS final_score, " +
                    "       MAX(ahs.score_category) AS score_category, b.barangay " +
                    "FROM aid_and_household_score ahs " +
                    "INNER JOIN beneficiary b ON ahs.beneficiary_id = b.beneficiary_id " +
                    "WHERE ahs.aid_type_id = ? " +
                    "GROUP BY ahs.beneficiary_id, b.barangay " +
                    "ORDER BY final_score DESC";
        }

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, aidTypeId);

            System.out.println("========== FETCHING BARANGAY-FILTERED BENEFICIARIES ==========");
            System.out.println("Aid Type ID : " + aidTypeId);
            System.out.println(disasterId > 0 ? "Disaster ID : " + disasterId : "Disaster    : NONE (General Aid)");
            System.out.println("Barangays   : " + String.join(", ", barangays));
            System.out.println("===============================================================");

            ResultSet rs = ps.executeQuery();
            int count = 0, filteredByGeography = 0;

            while (rs.next()) {
                try {
                    String decryptedBarangay = cs.decryptWithOneParameter(rs.getString("barangay"));
                    if (decryptedBarangay == null || !barangays.contains(decryptedBarangay)) continue;

                    int    beneficiaryId = rs.getInt("beneficiary_id");
                    double finalScore    = rs.getDouble("final_score");
                    String scoreCategory = rs.getString("score_category");
                    boolean include      = true;

                    if (disasterId > 0 && disasterCircle != null) {
                        try {
                            String latStr = cs.decryptWithOneParameter(rs.getString("latitude"));
                            String lonStr = cs.decryptWithOneParameter(rs.getString("longitude"));
                            double dist   = GeographicUtils.calculateDistance(
                                    Double.parseDouble(latStr), Double.parseDouble(lonStr),
                                    disasterCircle.lat, disasterCircle.lon);
                            if (dist > disasterCircle.radius) {
                                include = false;
                                filteredByGeography++;
                            }
                        } catch (Exception e) {
                            System.err.println("  \u26A0 Coord error for #" + beneficiaryId);
                        }
                    }

                    if (include) {
                        beneficiaries.add(new BeneficiaryCluster(beneficiaryId, finalScore, scoreCategory));
                        if (++count <= 5)
                            System.out.println("  " + count + ". Beneficiary #" + beneficiaryId
                                    + " | Score: " + finalScore + " | Barangay: " + decryptedBarangay);
                    }
                } catch (Exception e) {
                    System.err.println("Error decrypting barangay: " + e.getMessage());
                }
            }
            rs.close();
            ps.close();

            System.out.println("\n\u2713 Eligible: " + beneficiaries.size() + " across "
                    + barangays.size() + " barangay(s)"
                    + (disasterId > 0 && disasterCircle != null
                    ? "  \u2717 Outside area: " + filteredByGeography : ""));
            System.out.println("===============================================================\n");

        } catch (Exception e) {
            System.err.println("Error fetching by barangays: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return beneficiaries;
    }

    // =========================================================================
    //  getDisasterCircle
    //
    //  FIX: Opens a FRESH connection via dbConnection.getConnection() directly
    //       (not via the shared `conn` field) and closes ONLY that local
    //       connection — so it never accidentally closes a connection that
    //       another method's ResultSet is still using.
    // =========================================================================
    private DisasterCircle getDisasterCircle(int disasterId) {
        String sql = "SELECT lat, `long`, radius FROM disaster WHERE disaster_id = ? " +
                "AND lat IS NOT NULL AND `long` IS NOT NULL AND radius IS NOT NULL";
        // Use a completely separate, locally-scoped connection
        Connection localConn = null;
        try {
            localConn = dbConnection.getConnection();
            PreparedStatement ps = localConn.prepareStatement(sql);
            ps.setInt(1, disasterId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                try {
                    double lat    = Double.parseDouble(cs.decryptWithOneParameter(rs.getString("lat")));
                    double lon    = Double.parseDouble(cs.decryptWithOneParameter(rs.getString("long")));
                    double radius = Double.parseDouble(cs.decryptWithOneParameter(rs.getString("radius")));
                    rs.close();
                    ps.close();
                    return new DisasterCircle(lat, lon, radius);
                } catch (Exception e) {
                    System.err.println("Error decrypting disaster coords: " + e.getMessage());
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error fetching disaster circle: " + e.getMessage());
        } finally {
            // Close ONLY the local connection, never `conn`
            try { if (localConn != null && !localConn.isClosed()) localConn.close(); }
            catch (SQLException e) { System.err.println("Error closing local conn: " + e.getMessage()); }
        }
        return null;
    }

    private static class DisasterCircle {
        final double lat, lon, radius;
        DisasterCircle(double lat, double lon, double radius) {
            this.lat = lat; this.lon = lon; this.radius = radius;
        }
    }

    @Override
    public List<String> getBarangaysByDisaster(int disasterId, int aidTypeId) {
        // ── Fetch circle FIRST before opening main RS ──────────────────────
        DisasterCircle disasterCircle = disasterId > 0 ? getDisasterCircle(disasterId) : null;

        LinkedHashSet<String> barangaySet = new LinkedHashSet<>();
        String sql;
        if (disasterId > 0) {
            sql = "SELECT b.barangay, b.latitude, b.longitude " +
                    "FROM beneficiary b " +
                    "INNER JOIN aid_and_household_score ahs ON b.beneficiary_id = ahs.beneficiary_id " +
                    "WHERE ahs.aid_type_id = ? " +
                    "AND b.barangay IS NOT NULL " +
                    "AND b.latitude IS NOT NULL AND b.longitude IS NOT NULL " +
                    "AND b.latitude != '' AND b.longitude != ''";
        } else {
            sql = "SELECT b.barangay " +
                    "FROM beneficiary b " +
                    "INNER JOIN aid_and_household_score ahs ON b.beneficiary_id = ahs.beneficiary_id " +
                    "WHERE ahs.aid_type_id = ? AND b.barangay IS NOT NULL";
        }
        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, aidTypeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String encryptedBarangay = rs.getString("barangay");
                if (encryptedBarangay != null && !encryptedBarangay.trim().isEmpty()) {
                    try {
                        String decryptedBarangay = cs.decryptWithOneParameter(encryptedBarangay);
                        if (decryptedBarangay != null && !decryptedBarangay.trim().isEmpty()) {
                            boolean includeBarangay = true;
                            if (disasterId > 0 && disasterCircle != null) {
                                try {
                                    double bLat = Double.parseDouble(cs.decryptWithOneParameter(rs.getString("latitude")));
                                    double bLon = Double.parseDouble(cs.decryptWithOneParameter(rs.getString("longitude")));
                                    double dist = GeographicUtils.calculateDistance(bLat, bLon, disasterCircle.lat, disasterCircle.lon);
                                    includeBarangay = (dist <= disasterCircle.radius);
                                } catch (Exception e) { includeBarangay = false; }
                            }
                            if (includeBarangay) barangaySet.add(decryptedBarangay.trim());
                        }
                    } catch (Exception e) { System.err.println("Error decrypting barangay: " + e.getMessage()); }
                }
            }
            rs.close(); ps.close();
        } catch (SQLException e) { System.err.println("Error fetching barangays: " + e.getMessage()); e.printStackTrace(); }
        finally { closeConnection(); }
        List<String> barangays = new ArrayList<>(barangaySet);
        java.util.Collections.sort(barangays);
        System.out.println("Found " + barangays.size() + " unique barangays"
                + (disasterId > 0 ? " for disaster #" + disasterId : " for general aid"));
        return barangays;
    }

    @Override
    public List<String> getAllBarangays() {
        LinkedHashSet<String> barangaySet = new LinkedHashSet<>();
        String sql = "SELECT barangay FROM beneficiary WHERE barangay IS NOT NULL";
        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String enc = rs.getString("barangay");
                if (enc != null && !enc.trim().isEmpty()) {
                    try {
                        String dec = cs.decryptWithOneParameter(enc);
                        if (dec != null && !dec.trim().isEmpty()) barangaySet.add(dec.trim());
                    } catch (Exception e) { System.err.println("Error decrypting barangay: " + e.getMessage()); }
                }
            }
            rs.close(); ps.close();
        } catch (SQLException e) { System.err.println("Error fetching barangays: " + e.getMessage()); e.printStackTrace(); }
        finally { closeConnection(); }
        List<String> barangays = new ArrayList<>(barangaySet);
        java.util.Collections.sort(barangays);
        System.out.println("Found " + barangays.size() + " unique barangays");
        return barangays;
    }

    @Override
    public List<String> getBarangaysByAidNameAndDisaster(int disasterId, String aidName) {
        LinkedHashSet<String> barangaySet = new LinkedHashSet<>();
        String sql = disasterId > 0
                ? "SELECT b.barangay FROM beneficiary b INNER JOIN aid a ON b.beneficiary_id = a.beneficiary_id WHERE a.disaster_id = ? AND b.barangay IS NOT NULL"
                : "SELECT b.barangay FROM beneficiary b INNER JOIN aid a ON b.beneficiary_id = a.beneficiary_id WHERE a.disaster_id IS NULL AND b.barangay IS NOT NULL";
        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            if (disasterId > 0) ps.setInt(1, disasterId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String enc = rs.getString("barangay");
                if (enc != null && !enc.trim().isEmpty()) {
                    try {
                        String dec = cs.decryptWithOneParameter(enc);
                        if (dec != null && !dec.trim().isEmpty()) barangaySet.add(dec.trim());
                    } catch (Exception e) { System.err.println("Error decrypting barangay: " + e.getMessage()); }
                }
            }
            rs.close(); ps.close();
        } catch (SQLException e) { System.err.println("Error fetching barangays by aid name: " + e.getMessage()); e.printStackTrace(); }
        finally { closeConnection(); }
        List<String> barangays = new ArrayList<>(barangaySet);
        java.util.Collections.sort(barangays);
        System.out.println("Found " + barangays.size() + " unique barangays for aid: " + aidName);
        return barangays;
    }

    @Override
    public boolean hasReceivedAid(int beneficiaryId, int aidTypeId, int disasterId) {
        String sql = disasterId > 0
                ? "SELECT COUNT(*) FROM aid WHERE beneficiary_id = ? AND aid_type_id = ? AND disaster_id = ?"
                : "SELECT COUNT(*) FROM aid WHERE beneficiary_id = ? AND aid_type_id = ? AND disaster_id IS NULL";
        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, beneficiaryId);
            ps.setInt(2, aidTypeId);
            if (disasterId > 0) ps.setInt(3, disasterId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) { int c = rs.getInt(1); rs.close(); ps.close(); return c > 0; }
            rs.close(); ps.close();
        } catch (SQLException e) { System.err.println("Error checking aid receipt: " + e.getMessage()); e.printStackTrace(); }
        finally { closeConnection(); }
        return false;
    }

    @Override
    public List<AidModel> getAidByBeneficiary(int beneficiaryId) {
        List<AidModel> aidList = new ArrayList<>();
        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM aid WHERE beneficiary_id = ? ORDER BY date DESC");
            ps.setInt(1, beneficiaryId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) aidList.add(mapResultSetToAidModel(rs));
            rs.close(); ps.close();
        } catch (Exception e) { System.err.println("Error fetching aid by beneficiary: " + e.getMessage()); e.printStackTrace(); }
        finally { closeConnection(); }
        return aidList;
    }

    @Override
    public List<AidModel> getAidByDisaster(int disasterId) {
        List<AidModel> aidList = new ArrayList<>();
        String sql = disasterId > 0
                ? "SELECT * FROM aid WHERE disaster_id = ? ORDER BY date DESC"
                : "SELECT * FROM aid WHERE disaster_id IS NULL ORDER BY date DESC";
        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            if (disasterId > 0) ps.setInt(1, disasterId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) aidList.add(mapResultSetToAidModel(rs));
            rs.close(); ps.close();
        } catch (Exception e) { System.err.println("Error fetching aid by disaster: " + e.getMessage()); e.printStackTrace(); }
        finally { closeConnection(); }
        return aidList;
    }

    @Override
    public List<AidModel> getAidByType(int aidTypeId) {
        List<AidModel> aidList = new ArrayList<>();
        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM aid WHERE aid_type_id = ? ORDER BY date DESC");
            ps.setInt(1, aidTypeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) aidList.add(mapResultSetToAidModel(rs));
            rs.close(); ps.close();
        } catch (Exception e) { System.err.println("Error fetching aid by type: " + e.getMessage()); e.printStackTrace(); }
        finally { closeConnection(); }
        return aidList;
    }

    @Override
    public double getTotalQuantityDistributed(int aidTypeId, int disasterId) {
        String sql = disasterId > 0
                ? "SELECT COALESCE(SUM(qty), 0) as total FROM aid WHERE aid_type_id = ? AND disaster_id = ?"
                : "SELECT COALESCE(SUM(qty), 0) as total FROM aid WHERE aid_type_id = ? AND disaster_id IS NULL";
        double total = 0.0;
        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, aidTypeId);
            if (disasterId > 0) ps.setInt(2, disasterId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) total = rs.getDouble("total");
            rs.close(); ps.close();
        } catch (SQLException e) { System.err.println("Error getting total qty: " + e.getMessage()); e.printStackTrace(); }
        finally { closeConnection(); }
        return total;
    }

    @Override
    public List<AidModel> getAllAidForTable() {
        List<AidModel> aidList = new ArrayList<>();
        String sql = "SELECT a.aid_id, a.name as aid_name, a.date, a.qty, a.cost, " +
                "a.provider, a.notes, a.beneficiary_id, a.disaster_id, a.aid_type_id, " +
                "b.first_name, b.middle_name, b.last_name, d.name as disaster_name " +
                "FROM aid a " +
                "INNER JOIN beneficiary b ON a.beneficiary_id = b.beneficiary_id " +
                "LEFT JOIN disaster d ON a.disaster_id = d.disaster_id " +
                "ORDER BY a.date DESC";
        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String aidName      = cs.decryptWithOneParameter(rs.getString("aid_name"));
                String provider     = cs.decryptWithOneParameter(rs.getString("provider"));
                String firstName    = cs.decryptWithOneParameter(rs.getString("first_name"));
                String middleName   = cs.decryptWithOneParameter(rs.getString("middle_name"));
                String lastName     = cs.decryptWithOneParameter(rs.getString("last_name"));
                String encDisName   = rs.getString("disaster_name");
                String disasterName = encDisName != null ? cs.decryptWithOneParameter(encDisName) : "General Aid (No Disaster)";
                String beneficiaryName = (firstName + " " + (middleName != null && !middleName.isEmpty() ? middleName + " " : "") + lastName).trim();
                AidModel aid = new AidModel();
                aid.setAidId(rs.getInt("aid_id"));
                aid.setBeneficiaryId(rs.getInt("beneficiary_id"));
                int disId = rs.getInt("disaster_id");
                aid.setDisasterId(rs.wasNull() ? 0 : disId);
                aid.setName(aidName);
                aid.setDate(rs.getDate("date").toLocalDate());
                aid.setQuantity(rs.getDouble("qty"));
                aid.setCost(rs.getDouble("cost"));
                aid.setProvider(provider);
                aid.setAidTypeId(rs.getInt("aid_type_id"));
                aid.setNotes(rs.getString("notes"));
                aid.setBeneficiaryName(beneficiaryName);
                aid.setDisasterName(disasterName);
                aidList.add(aid);
            }
            rs.close(); ps.close();
        } catch (Exception e) { System.err.println("Error fetching aid table data: " + e.getMessage()); e.printStackTrace(); }
        finally { closeConnection(); }
        return aidList;
    }

    @Override
    public List<String> getDistinctAidNames() {
        List<String> aidNames = new ArrayList<>();
        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT name FROM aid WHERE name IS NOT NULL");
            ResultSet rs = ps.executeQuery();
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            while (rs.next()) {
                try {
                    String aidName = cs.decryptWithOneParameter(rs.getString("name"));
                    if (aidName != null && !aidName.trim().isEmpty()) seen.add(aidName.trim());
                } catch (Exception e) { System.err.println("Error decrypting aid name: " + e.getMessage()); }
            }
            aidNames.addAll(seen);
            rs.close(); ps.close();
            System.out.println("Found " + aidNames.size() + " distinct aid names");
        } catch (SQLException e) { System.err.println("Error fetching distinct aid names: " + e.getMessage()); e.printStackTrace(); }
        finally { closeConnection(); }
        return aidNames;
    }

    private void debugNoEligibleBeneficiaries(int aidTypeId, int disasterId) {
        try {
            Connection debugConn = dbConnection.getConnection();
            System.out.println("\n========== DEBUGGING: WHY NO ELIGIBLE BENEFICIARIES? ==========");
            PreparedStatement ps1 = debugConn.prepareStatement(
                    "SELECT COUNT(*) as count, MIN(final_score) as min_score, MAX(final_score) as max_score " +
                            "FROM aid_and_household_score WHERE aid_type_id = ?");
            ps1.setInt(1, aidTypeId);
            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) {
                int scoreCount = rs1.getInt("count");
                System.out.println("\n1. Scores for Aid Type #" + aidTypeId + ": " + scoreCount);
                if (scoreCount > 0)
                    System.out.println("   Range: " + rs1.getDouble("min_score") + " to " + rs1.getDouble("max_score"));
                else
                    System.out.println("   \u26A0 NO SCORES — calculate aid household scores first.");
            }
            rs1.close(); ps1.close();
            if (disasterId > 0) {
                DisasterCircle circle = getDisasterCircle(disasterId);
                if (circle == null)
                    System.out.println("\n2. \u26A0 No disaster circle for disaster #" + disasterId + " — all scored beneficiaries included.");
                else
                    System.out.println("\n2. Disaster circle: (" + circle.lat + ", " + circle.lon
                            + ") radius " + circle.radius + "m — beneficiaries outside are excluded.");
            }
            System.out.println("\n================================================================\n");
            debugConn.close();
        } catch (SQLException e) { System.err.println("Error in debug: " + e.getMessage()); }
    }

    @Override
    public Map<Integer, String> getBeneficiaryNames(List<Integer> beneficiaryIds) {
        Map<Integer, String> nameMap = new HashMap<>();
        if (beneficiaryIds == null || beneficiaryIds.isEmpty()) return nameMap;
        String placeholders = beneficiaryIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT beneficiary_id, first_name, middle_name, last_name FROM beneficiary WHERE beneficiary_id IN (" + placeholders + ")";
        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            for (int i = 0; i < beneficiaryIds.size(); i++) ps.setInt(i + 1, beneficiaryIds.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("beneficiary_id");
                try {
                    String firstName  = cs.decryptWithOneParameter(rs.getString("first_name"));
                    String encMid     = rs.getString("middle_name");
                    String middleName = (encMid != null && !encMid.isEmpty()) ? cs.decryptWithOneParameter(encMid) : "";
                    String lastName   = cs.decryptWithOneParameter(rs.getString("last_name"));
                    String fullName   = firstName + (!middleName.isEmpty() ? " " + middleName : "") + " " + lastName;
                    nameMap.put(id, fullName.trim());
                } catch (Exception e) {
                    System.err.println("Error decrypting name for #" + id + ": " + e.getMessage());
                    nameMap.put(id, "Name Unavailable");
                }
            }
            rs.close(); ps.close();
        } catch (SQLException e) { System.err.println("Error fetching names: " + e.getMessage()); e.printStackTrace(); }
        finally { closeConnection(); }
        return nameMap;
    }

    private AidModel mapResultSetToAidModel(ResultSet rs) throws Exception {
        AidModel aid = new AidModel();
        aid.setAidId(rs.getInt("aid_id"));
        aid.setBeneficiaryId(rs.getInt("beneficiary_id"));
        int disasterId = rs.getInt("disaster_id");
        aid.setDisasterId(rs.wasNull() ? 0 : disasterId);
        aid.setName(cs.decryptWithOneParameter(rs.getString("name")));
        aid.setDate(rs.getDate("date").toLocalDate());
        aid.setQuantity(rs.getDouble("qty"));
        aid.setCost(rs.getDouble("cost"));
        aid.setProvider(cs.decryptWithOneParameter(rs.getString("provider")));
        aid.setAidTypeId(rs.getInt("aid_type_id"));
        aid.setNotes(rs.getString("notes"));
        return aid;
    }

    private void closeConnection() {
        try { if (conn != null && !conn.isClosed()) conn.close(); }
        catch (SQLException e) { System.err.println("Error closing connection: " + e.getMessage()); }
    }
}