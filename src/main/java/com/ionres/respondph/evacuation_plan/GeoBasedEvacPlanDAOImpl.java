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

        DisasterCircleInfo disasterCircle = getDisasterCircleInfo(disasterId);

        if (disasterCircle == null) {
            System.out.println("⚠ No disaster circle found for disaster ID: " + disasterId);
            return ranked;
        }


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
                        "WHERE 1=1 ";

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

            // ✅ Step 4: Set parameter for the NOT EXISTS clause (if includeAssigned = false)
            if (!includeAssigned) {
                ps.setInt(1, disasterId);
            }

            ResultSet rs = ps.executeQuery();

            System.out.println("✅ Fetching ALL beneficiaries, filtering by geographic location");
            System.out.println("   Disaster circle - Lat: " + disasterCircle.lat +
                    ", Lon: " + disasterCircle.lon + ", Radius: " + disasterCircle.radius + "m");

            int processedCount = 0;
            int insideCount = 0;
            int outsideCount = 0;
            int wrongAidTypeCount = 0;

            // ✅ Step 5: Process each beneficiary
            while (rs.next()) {
                processedCount++;
                int beneficiaryId = rs.getInt("beneficiary_id");

                // ✅ Filter 1: Check aid type (must be "Evac Weight")
                String encryptedAidName = rs.getString("aid_name");
                String decryptedAidName = "";
                try {
                    decryptedAidName = cs.decryptWithOneParameter(encryptedAidName);
                } catch (Exception e) {
                    System.err.println("⚠ Error decrypting aid name: " + e.getMessage());
                    continue;
                }

                // Skip if not "Evac Weight"
                if (!"Evac Weight".equalsIgnoreCase(decryptedAidName)) {
                    wrongAidTypeCount++;
                    continue;
                }

                // ✅ Decrypt beneficiary data
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
                    System.err.println("⚠ Invalid coordinates for beneficiary " + beneficiaryId);
                    continue;
                }

                // ✅ Filter 2: Check geographic distance
                // Calculate distance in kilometers
                double distanceInKm = GeoDistanceCalculator.calculateDistance(
                        beneficiaryLat,
                        beneficiaryLon,
                        disasterCircle.lat,
                        disasterCircle.lon
                );

                // Convert to meters
                double distanceInMeters = distanceInKm * 1000;

                System.out.println("   Beneficiary #" + beneficiaryId +
                        " - Distance: " + String.format("%.2f", distanceInMeters) + "m");

                // Skip if OUTSIDE the disaster radius
                if (distanceInMeters > disasterCircle.radius) {
                    outsideCount++;
                    System.out.println("      ❌ OUTSIDE (distance > radius)");
                    continue;
                }

                insideCount++;
                System.out.println("      ✅ INSIDE disaster area");

                // ✅ Create model for beneficiaries that passed all filters
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

            // ✅ Step 6: Log summary
            System.out.println("\n📊 SUMMARY:");
            System.out.println("   Total beneficiaries processed: " + processedCount);
            System.out.println("   Wrong aid type (not Evac Weight): " + wrongAidTypeCount);
            System.out.println("   Inside disaster radius: " + insideCount);
            System.out.println("   Outside disaster radius: " + outsideCount);
            System.out.println("   ✅ FINAL: " + ranked.size() + " beneficiaries ready for allocation");

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("❌ Error fetching beneficiaries: " + ex.getMessage());
        } finally {
            closeConnection();
        }

        return ranked;
    }

    /**
     * ✅ HELPER METHOD: Get disaster circle info
     * Retrieves the disaster's geographic information (lat, lon, radius)
     */
    private DisasterCircleInfo getDisasterCircleInfo(int disasterId) {
        String sql = "SELECT lat, `long`, radius, type, name FROM disaster WHERE disaster_id = ?";

        try {
            conn = dbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, disasterId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String encryptedLat = rs.getString("lat");
                String encryptedLon = rs.getString("long");
                String encryptedRadius = rs.getString("radius");
                String encryptedType = rs.getString("type");
                String encryptedName = rs.getString("name");

                double lat = Double.parseDouble(cs.decryptWithOneParameter(encryptedLat));
                double lon = Double.parseDouble(cs.decryptWithOneParameter(encryptedLon));
                double radius = Double.parseDouble(cs.decryptWithOneParameter(encryptedRadius));
                String type = cs.decryptWithOneParameter(encryptedType);
                String name = cs.decryptWithOneParameter(encryptedName);

                System.out.println("📍 Disaster Circle Info - Lat: " + lat + ", Lon: " + lon +
                        ", Radius: " + radius + "m, Type: " + type + ", Name: " + name);

                rs.close();
                ps.close();

                return new DisasterCircleInfo(lat, lon, radius, name, type);
            }

            rs.close();
            ps.close();
        } catch (Exception e) {
            System.err.println("❌ Error getting disaster circle info: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
    //  NEW: Helper method to get encrypted aid name
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