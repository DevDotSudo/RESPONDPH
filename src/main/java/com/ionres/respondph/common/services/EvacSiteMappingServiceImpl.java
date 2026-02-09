package com.ionres.respondph.common.services;

import com.ionres.respondph.common.dao.EvacSiteMappingDAOImpl;
import com.ionres.respondph.common.interfaces.EvacSiteMappingDAO;
import com.ionres.respondph.common.interfaces.EvacSiteMappingService;
import com.ionres.respondph.common.model.EvacSiteModel;
import com.ionres.respondph.common.model.EvacSiteMarker;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.util.CryptographyManager;
import com.ionres.respondph.util.Mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EvacSiteMappingServiceImpl implements EvacSiteMappingService {

    private static final Logger LOGGER = Logger.getLogger(EvacSiteMappingServiceImpl.class.getName());

    private static final Cryptography CRYPTO = CryptographyManager.getInstance();

    private final EvacSiteMappingDAO dao;

    public EvacSiteMappingServiceImpl(DBConnection connection) {
        this.dao = new EvacSiteMappingDAOImpl(connection);
    }

    private static Double tryParse(String s) {
        if (s == null) return Double.NaN;
        String t = s.trim();
        if (t.isEmpty()) return Double.NaN;
        t = t.replace(',', '.');
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static Integer tryParseInt(String s) {
        if (s == null) return 0;
        String t = s.trim();
        if (t.isEmpty()) return 0;
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean looksSwapped(double lat, double lon) {
        return (lat < -90 || lat > 90) && (lon >= -90 && lon <= 90) && (lat >= -180 && lat <= 180);
    }

    private static double[] sanitizeLatLon(double lat, double lon) {
        if (looksSwapped(lat, lon)) {
            double tmp = lat;
            lat = lon;
            lon = tmp;
        }
        if (!Mapping.isValidCoordinate(lat, lon)) {
            return null;
        }
        return new double[]{lat, lon};
    }

    private static String decryptDisasterNameForDisplay(String encrypted) {
        if (encrypted == null) return "";
        String t = encrypted.trim();
        if (t.isEmpty()) return "";

        if (!t.contains(":")) {
            return t;
        }

        try {
            return CRYPTO.decryptWithOneParameter(t);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to decrypt disaster name for display. Using raw value.", e);
            return t;
        }
    }

    @Override
    public List<EvacSiteMarker> getAllEvacSites() {
        List<EvacSiteMarker> evacSiteMarkers = new ArrayList<>();

        for (EvacSiteModel site : dao.getAllEvacSites()) {
            try {
                Double latVal = tryParse(site.getLat());
                Double lonVal = tryParse(site.getLon());

                if (latVal.isNaN() || lonVal.isNaN()) {
                    LOGGER.warning("Skipping evac site ID " + site.getEvacId() + " - invalid coordinates");
                    continue;
                }

                double[] ll = sanitizeLatLon(latVal, lonVal);
                if (ll == null) {
                    LOGGER.warning("Skipping evac site ID " + site.getEvacId() + " - coordinates out of bounds");
                    continue;
                }

                String name = site.getName() != null ? site.getName() : "";
                Integer capacity = tryParseInt(site.getCapacity());
                String notes = site.getNotes() != null ? site.getNotes() : "";

                String disasterName = decryptDisasterNameForDisplay(name);

                evacSiteMarkers.add(new EvacSiteMarker(
                        site.getEvacId(),
                        ll[0],
                        ll[1],
                        disasterName,
                        capacity,
                        notes
                ));

                LOGGER.fine("Loaded evac site: " + name + " at (" + ll[0] + ", " + ll[1] + ")"
                        + " disaster=" + disasterName);

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error processing evacuation site (ID: " + site.getEvacId() + ")", e);
            }
        }

        LOGGER.info("Successfully loaded " + evacSiteMarkers.size() + " evacuation sites (disaster name decrypted for display)");
        return evacSiteMarkers;
    }

    @Override
    public EvacSiteMarker getEvacSiteById(int evacId) {
        EvacSiteModel site = dao.getEvacSiteById(evacId);

        if (site == null) {
            return null;
        }

        try {
            // IMPORTANT: Evacuation site coordinates are stored UNENCRYPTED in the database
            Double latVal = tryParse(site.getLat());
            Double lonVal = tryParse(site.getLon());

            if (latVal.isNaN() || lonVal.isNaN()) {
                LOGGER.warning("Invalid coordinates for evac site ID " + evacId);
                return null;
            }

            double[] ll = sanitizeLatLon(latVal, lonVal);
            if (ll == null) {
                LOGGER.warning("Coordinates out of bounds for evac site ID " + evacId);
                return null;
            }

            String name = site.getName() != null ? site.getName() : "";
            Integer capacity = tryParseInt(site.getCapacity());
            String notes = site.getNotes() != null ? site.getNotes() : "";

            String disasterName = decryptDisasterNameForDisplay(name);

            return new EvacSiteMarker(
                    site.getEvacId(),
                    ll[0],
                    ll[1],
                    disasterName,
                    capacity,
                    notes
            );

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing evacuation site by ID (ID=" + evacId + ")", e);
            return null;
        }
    }
}
