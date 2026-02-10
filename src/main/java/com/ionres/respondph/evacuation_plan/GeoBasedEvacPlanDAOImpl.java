package com.ionres.respondph.evacuation_plan;

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
                        "WHERE ahs.disaster_id = ? ";

        // Only exclude already assigned if includeAssigned is false
        if (!includeAssigned) {
            sql += "  AND NOT EXISTS ( " +
                    "    SELECT 1 FROM evac_plan ep " +
                    "    WHERE ep.beneficiary_id = ahs.beneficiary_id " +
                    "      AND ep.disaster_id = ahs.disaster_id " +
                    "  ) ";
        }

        sql += "ORDER BY ahs.final_score DESC";

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