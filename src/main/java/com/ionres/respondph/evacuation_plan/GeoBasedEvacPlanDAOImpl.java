package com.ionres.respondph.evacuation_plan;

import com.ionres.respondph.common.model.DisasterCircleInfo;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.ConfigLoader;
import com.ionres.respondph.util.Cryptography;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class GeoBasedEvacPlanDAOImpl extends EvacuationPlanDAOImpl implements GeoBasedEvacPlanDAO {

    private final Cryptography cs;
    private Connection conn;
    private final DBConnection dbConnection;

    /** Temporary polygon coordinates loaded by getDisasterCircleInfo for POLYGON disasters */
    private double[][] currentPolygon;

    public GeoBasedEvacPlanDAOImpl(DBConnection dbConnection) {
        super(dbConnection);
        this.dbConnection = dbConnection;
        String secretKey = ConfigLoader.get("secretKey");
        this.cs = new Cryptography(secretKey);
    }

    @Override
    public List<RankedBeneficiaryWithLocation> getRankedBeneficiariesWithLocation(int disasterId) {
        List<RankedBeneficiaryWithLocation> ranked = new ArrayList<>();

        String sql =
                "SELECT " +
                        "    ahs.beneficiary_id, " +
                        "    b.first_name, " +
                        "    b.last_name, " +
                        "    b.latitude, " +
                        "    b.longitude, " +
                        "    ahs.final_score, " +
                        "    ahs.score_category, " +
                        "    CASE " +
                        "        WHEN ahs.household_members IS NOT NULL AND ahs.household_members > 0 " +
                        "            THEN ahs.household_members " +
                        "        ELSE (SELECT COUNT(*) FROM family_member fm WHERE fm.beneficiary_id = ahs.beneficiary_id) + 1 " +
                        "    END AS household_members " +
                        "FROM aid_and_household_score ahs " +
                        "INNER JOIN beneficiary b ON ahs.beneficiary_id = b.beneficiary_id " +
                        "WHERE ahs.disaster_id = ? " +
                        // EXCLUDE beneficiaries already assigned to ANY site for this disaster
                        "  AND NOT EXISTS ( " +
                        "    SELECT 1 FROM evac_plan ep " +
                        "    WHERE ep.beneficiary_id = ahs.beneficiary_id " +
                        "      AND ep.disaster_id = ahs.disaster_id " +
                        "  ) " +
                        "ORDER BY ahs.final_score DESC";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, disasterId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                List<String> encrypted = new ArrayList<>();
                encrypted.add(rs.getString("first_name"));
                encrypted.add(rs.getString("last_name"));
                encrypted.add(rs.getString("latitude"));
                encrypted.add(rs.getString("longitude"));

                List<String> decrypted = cs.decrypt(encrypted);

                RankedBeneficiaryWithLocation model = new RankedBeneficiaryWithLocation();
                model.setBeneficiaryId(rs.getInt("beneficiary_id"));
                model.setFirstName(decrypted.get(0));
                model.setLastName(decrypted.get(1));
                model.setFinalScore(rs.getDouble("final_score"));
                model.setScoreCategory(rs.getString("score_category"));
                model.setHouseholdMembers(rs.getInt("household_members"));

                try {
                    model.setLatitude(Double.parseDouble(decrypted.get(2)));
                    model.setLongitude(Double.parseDouble(decrypted.get(3)));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid coordinates for beneficiary " +
                            model.getBeneficiaryId() + ": " + e.getMessage());
                    model.setLatitude(0.0);
                    model.setLongitude(0.0);
                }

                ranked.add(model);
            }

            rs.close();
            ps.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error fetching ranked beneficiaries with location: " + ex.getMessage());
        } finally {
            closeConnection();
        }

        return ranked;
    }

    @Override
    public List<EvacSiteWithDistance> getEvacSitesWithCapacity(int disasterId) {
        List<EvacSiteWithDistance> sites = new ArrayList<>();


        String sql =
                "SELECT " +
                        "    es.evac_id, " +
                        "    es.name, " +
                        "    es.lat, " +
                        "    es.long, " +
                        "    es.capacity, " +
                        "    COALESCE(occupied.total_occupied, 0) AS occupied, " +
                        "    (es.capacity - COALESCE(occupied.total_occupied, 0)) AS remaining_capacity " +
                        "FROM evac_site es " +
                        "LEFT JOIN ( " +
                        "    SELECT " +
                        "        ep.evac_site_id, " +
                        "        SUM(CASE " +
                        "            WHEN ahs.household_members IS NOT NULL AND ahs.household_members > 0 " +
                        "                THEN ahs.household_members " +
                        "            ELSE (SELECT COUNT(*) FROM family_member fm WHERE fm.beneficiary_id = ep.beneficiary_id) + 1 " +
                        "        END) AS total_occupied " +
                        "    FROM evac_plan ep " +
                        "    LEFT JOIN aid_and_household_score ahs " +
                        "        ON ep.beneficiary_id = ahs.beneficiary_id " +
                        "        AND ahs.disaster_id = ep.disaster_id " +
                        "    WHERE ep.disaster_id = ? " +
                        "    GROUP BY ep.evac_site_id " +
                        ") occupied ON es.evac_id = occupied.evac_site_id " +
                        "WHERE (es.capacity - COALESCE(occupied.total_occupied, 0)) > 0 " +
                        "ORDER BY remaining_capacity DESC";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, disasterId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                // Decrypt site name
                String encryptedName = rs.getString("name");
                String decryptedName = cs.decryptWithOneParameter(encryptedName);

                EvacSiteWithDistance site = new EvacSiteWithDistance();
                site.setEvacSiteId(rs.getInt("evac_id"));
                site.setEvacSiteName(decryptedName);
                site.setLatitude(rs.getDouble("lat"));
                site.setLongitude(rs.getDouble("long"));
                site.setRemainingCapacity(rs.getInt("remaining_capacity"));
                site.setDistanceInKm(0.0); // Will be calculated per beneficiary

                sites.add(site);
            }

            rs.close();
            ps.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error fetching evac sites with capacity: " + ex.getMessage());
        } finally {
            closeConnection();
        }

        return sites;
    }
    @Override
    public List<RankedBeneficiaryWithLocation> getRankedBeneficiariesWithLocation(int disasterId, boolean includeAssigned) {
        List<RankedBeneficiaryWithLocation> ranked = new ArrayList<>();

        //  Get the disaster circle information to filter geographically
        DisasterCircleInfo disasterCircle = getDisasterCircleInfo(disasterId);

        if (disasterCircle == null) {
            System.out.println(" No disaster circle found for disaster ID: " + disasterId);
            return ranked;
        }

        boolean isBanateArea = disasterCircle.radius == -1;
        boolean isPolygon   = disasterCircle.radius == -2;

        // For polygon disasters, currentPolygon was set by getDisasterCircleInfo
        if (isPolygon && (currentPolygon == null || currentPolygon.length < 3)) {
            System.out.println(" Polygon disaster detected but polygon data is invalid");
            return ranked;
        }

        //  Get ALL beneficiaries with scores (no disaster_id filter)
        String sql =
                "SELECT " +
                        "    ahs.beneficiary_id, " +
                        "    b.first_name, " +
                        "    b.last_name, " +
                        "    b.latitude, " +
                        "    b.longitude, " +
                        "    ahs.final_score, " +
                        "    ahs.score_category, " +
                        "    ahs.aid_type_id, " +
                        "    at.aid_name, " +
                        "    CASE " +
                        "        WHEN ahs.household_members IS NOT NULL AND ahs.household_members > 0 " +
                        "            THEN ahs.household_members " +
                        "        ELSE (SELECT COUNT(*) FROM family_member fm WHERE fm.beneficiary_id = ahs.beneficiary_id) + 1 " +
                        "    END AS household_members " +
                        "FROM aid_and_household_score ahs " +
                        "INNER JOIN beneficiary b ON ahs.beneficiary_id = b.beneficiary_id " +
                        "INNER JOIN aid_type at ON ahs.aid_type_id = at.aid_type_id " +
                        "WHERE 1=1 ";  // NO disaster filter - get ALL beneficiaries

        if (!includeAssigned) {
            sql += "  AND NOT EXISTS ( " +
                    "    SELECT 1 FROM evac_plan ep " +
                    "    WHERE ep.beneficiary_id = ahs.beneficiary_id " +
                    "      AND ep.disaster_id = ? " +
                    "  ) ";
        }

        sql += "ORDER BY ahs.final_score DESC";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            if (!includeAssigned) {
                ps.setInt(1, disasterId);
            }

            ResultSet rs = ps.executeQuery();

            if (isBanateArea) {
                System.out.println(" Banate Area disaster — filtering by boundary polygon");
            } else if (isPolygon) {
                System.out.println(" Polygon disaster — filtering by custom polygon (" + currentPolygon.length + " points)");
            } else {
                System.out.println(" Fetching ALL beneficiaries, filtering by geographic location");
                System.out.println(" Disaster circle - Lat: " + disasterCircle.lat +
                        ", Lon: " + disasterCircle.lon + ", Radius: " + disasterCircle.radius + "m");
            }

            int processedCount = 0;
            int insideCount = 0;
            int outsideCount = 0;
            int wrongAidTypeCount = 0;

            while (rs.next()) {
                processedCount++;
                int beneficiaryId = rs.getInt("beneficiary_id");

                // Decrypt aid name and filter
                String encryptedAidName = rs.getString("aid_name");
                String decryptedAidName = "";
                try {
                    decryptedAidName = cs.decryptWithOneParameter(encryptedAidName);
                } catch (Exception e) {
                    System.err.println(" Error decrypting aid name: " + e.getMessage());
                    continue;
                }

                // FILTER: Only include "Evac Weight"
                if (!"Evac Weight".equalsIgnoreCase(decryptedAidName)) {
                    wrongAidTypeCount++;
                    continue;
                }

                List<String> encrypted = new ArrayList<>();
                encrypted.add(rs.getString("first_name"));
                encrypted.add(rs.getString("last_name"));
                encrypted.add(rs.getString("latitude"));
                encrypted.add(rs.getString("longitude"));

                List<String> decrypted = cs.decrypt(encrypted);

                double beneficiaryLat = 0.0;
                double beneficiaryLon = 0.0;

                try {
                    beneficiaryLat = Double.parseDouble(decrypted.get(2));
                    beneficiaryLon = Double.parseDouble(decrypted.get(3));
                } catch (NumberFormatException e) {
                    System.err.println(" Invalid coordinates for beneficiary " + beneficiaryId);
                    continue;
                }

                boolean isInside;
                if (isBanateArea) {
                    isInside = isPointInBanateBoundary(beneficiaryLat, beneficiaryLon);
                } else if (isPolygon) {
                    isInside = isPointInPolygon(beneficiaryLat, beneficiaryLon, currentPolygon);
                } else {
                    double distanceInKm = GeoDistanceCalculator.calculateDistance(
                            beneficiaryLat, beneficiaryLon,
                            disasterCircle.lat, disasterCircle.lon
                    );
                    double distanceInMeters = distanceInKm * 1000;
                    isInside = distanceInMeters <= disasterCircle.radius;

                    System.out.println("   Beneficiary #" + beneficiaryId +
                            " - Distance: " + String.format("%.2f", distanceInMeters) + "m");
                }

                if (!isInside) {
                    outsideCount++;
                    System.out.println(" OUTSIDE disaster area");
                    continue;
                }

                insideCount++;
                System.out.println(" INSIDE disaster area");

                RankedBeneficiaryWithLocation model = new RankedBeneficiaryWithLocation();
                model.setBeneficiaryId(beneficiaryId);
                model.setFirstName(decrypted.get(0));
                model.setLastName(decrypted.get(1));
                model.setFinalScore(rs.getDouble("final_score"));
                model.setScoreCategory(rs.getString("score_category"));
                model.setHouseholdMembers(rs.getInt("household_members"));
                model.setLatitude(beneficiaryLat);
                model.setLongitude(beneficiaryLon);

                ranked.add(model);
            }

            rs.close();
            ps.close();

            System.out.println("\n SUMMARY:");
            System.out.println("   Total beneficiaries processed: " + processedCount);
            System.out.println("   Wrong aid type (not Evac Weight): " + wrongAidTypeCount);
            System.out.println("   Inside disaster area: " + insideCount);
            System.out.println("   Outside disaster area: " + outsideCount);
            System.out.println("   FINAL: " + ranked.size() + " beneficiaries ready for allocation");

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println(" Error fetching beneficiaries: " + ex.getMessage());
        } finally {
            closeConnection();
        }

        return ranked;
    }

    /** Banate municipal boundary polygon coordinates */
    private static final double[][] BANATE_BOUNDARY = {
            {11.0775,122.7315},{11.1031,122.7581},{11.0925,122.7618},
            {11.0912,122.7648},{11.0897,122.7662},{11.0896,122.7796},
            {11.0756,122.7942},{11.0674,122.7957},{11.0584,122.7991},
            {11.0533,122.8023},{11.0416,122.8200},{10.9914,122.8514},
            {10.9907,122.8483},{10.9899,122.8462},{10.9904,122.8449},
            {10.9920,122.8447},{10.9951,122.8433},{10.9968,122.8443},
            {10.9966,122.8417},{10.9963,122.8340},{10.9988,122.8287},
            {10.9976,122.8156},{10.9909,122.7957},{10.9919,122.7865},
            {11.0034,122.7861},{11.0480,122.7722},{11.0613,122.7499},
            {11.0681,122.7489},{11.0719,122.7453},{11.0761,122.7454}
    };

    /** Ray-casting point-in-polygon test */
    private boolean isPointInBanateBoundary(double lat, double lon) {
        return isPointInPolygon(lat, lon, BANATE_BOUNDARY);
    }

    /** General ray-casting point-in-polygon test for any polygon */
    private boolean isPointInPolygon(double lat, double lon, double[][] polygon) {
        int intersections = 0;
        for (int i = 0; i < polygon.length; i++) {
            double[] p1 = polygon[i];
            double[] p2 = polygon[(i + 1) % polygon.length];
            if (((p1[0] > lat) != (p2[0] > lat)) &&
                (lon < (p2[1] - p1[1]) * (lat - p1[0]) / (p2[0] - p1[0]) + p1[1])) {
                intersections++;
            }
        }
        return (intersections % 2) == 1;
    }

    //  Helper method to get disaster circle info
    private DisasterCircleInfo getDisasterCircleInfo(int disasterId) {
        String sql = "SELECT lat, `long`, radius, type, name, is_banate_area, poly_lat_long FROM disaster WHERE disaster_id = ?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, disasterId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                boolean isBanateArea = rs.getBoolean("is_banate_area");
                String encryptedType = rs.getString("type");
                String encryptedName = rs.getString("name");
                String type = cs.decryptWithOneParameter(encryptedType);
                String name = cs.decryptWithOneParameter(encryptedName);

                if (isBanateArea) {
                    System.out.println("Banate Area disaster detected - using boundary polygon");
                    currentPolygon = null;
                    rs.close();
                    ps.close();
                    return new DisasterCircleInfo(0, 0, -1, name, type);
                }

                // Check for polygon disaster (poly_lat_long present)
                String encryptedPoly = rs.getString("poly_lat_long");
                if (encryptedPoly != null && !encryptedPoly.isEmpty()) {
                    String decryptedPoly = cs.decryptWithOneParameter(encryptedPoly);
                    if (decryptedPoly != null && !decryptedPoly.trim().isEmpty()) {
                        currentPolygon = parsePolyLatLong(decryptedPoly);
                        if (currentPolygon != null && currentPolygon.length >= 3) {
                            System.out.println("Polygon disaster detected - " + currentPolygon.length + " points, Name: " + name);
                            rs.close();
                            ps.close();
                            // Use radius = -2 to signal polygon disaster (vs -1 for Banate)
                            return new DisasterCircleInfo(0, 0, -2, name, type);
                        }
                    }
                }

                String encryptedLat = rs.getString("lat");
                String encryptedLon = rs.getString("long");
                String encryptedRadius = rs.getString("radius");

                if (encryptedLat == null || encryptedLon == null || encryptedRadius == null) {
                    System.out.println("Disaster has null location fields");
                    rs.close();
                    ps.close();
                    return null;
                }

                String decLat = cs.decryptWithOneParameter(encryptedLat);
                String decLon = cs.decryptWithOneParameter(encryptedLon);
                String decRadius = cs.decryptWithOneParameter(encryptedRadius);

                if (decLat == null || decLat.trim().isEmpty()
                        || decLon == null || decLon.trim().isEmpty()
                        || decRadius == null || decRadius.trim().isEmpty()) {
                    System.out.println("Disaster has empty decrypted location fields");
                    rs.close();
                    ps.close();
                    return null;
                }

                double lat = Double.parseDouble(decLat.trim());
                double lon = Double.parseDouble(decLon.trim());
                double radius = Double.parseDouble(decRadius.trim());

                System.out.println("Disaster Circle Info - Lat: " + lat + ", Lon: " + lon +
                        ", Radius: " + radius + "m, Type: " + type + ", Name: " + name);

                currentPolygon = null;
                rs.close();
                ps.close();

                return new DisasterCircleInfo(lat, lon, radius, name, type);
            }

            rs.close();
            ps.close();
        } catch (Exception e) {
            System.err.println("Error getting disaster circle info: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /** Parses "lat,lon;lat,lon;..." into double[][] */
    private double[][] parsePolyLatLong(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String[] parts = raw.split(";");
        List<double[]> points = new ArrayList<>();
        for (String part : parts) {
            String[] coords = part.trim().split(",");
            if (coords.length == 2) {
                try {
                    double lat = Double.parseDouble(coords[0].trim());
                    double lon = Double.parseDouble(coords[1].trim());
                    points.add(new double[]{lat, lon});
                } catch (NumberFormatException e) {
                    System.err.println("Invalid polygon coordinate: " + part);
                }
            }
        }
        return points.isEmpty() ? null : points.toArray(new double[0][]);
    }

    private String getEncryptedAidName(String aidName) {
        try {
            return cs.encryptWithOneParameter(aidName);
        } catch (Exception e) {
            System.err.println("Error encrypting aid name: " + e.getMessage());
            return "";
        }
    }


    private void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}