package com.ionres.respondph.disaster_mapping;

import com.ionres.respondph.common.model.*;
import com.ionres.respondph.database.DBConnection;
import com.ionres.respondph.util.Cryptography;
import com.ionres.respondph.util.CryptographyManager;
import com.ionres.respondph.util.GeographicUtils;
import com.ionres.respondph.util.Mapping;
import com.ionres.respondph.util.NameDecryptionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DisasterMappingServiceImpl implements DisasterMappingService {
    private static final Logger LOGGER = Logger.getLogger(DisasterMappingServiceImpl.class.getName());
    private final DisasterMappingDAO dao;
    private static final Cryptography CRYPTO = CryptographyManager.getInstance();

    public DisasterMappingServiceImpl(DBConnection connection) {
        this.dao = new DisasterMappingDAOImpl(connection);
    }

    private static Double tryParse(String s) {
        if (s == null) return Double.NaN;
        String t = s.trim();
        if (t.isEmpty()) return Double.NaN;
        // Replace comma decimal separators if present
        t = t.replace(',', '.');
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static boolean looksSwapped(double lat, double lon) {
        // Heuristic: lat outside [-90,90] but within longitude range, and lon within latitude range
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

        @Override
        public List<String> getDisasterTypes() {
            List<String> types = new ArrayList<>();

            for (String encryptedType : dao.getDisasterTypes()) {
                try {
                    String decryptedType = CRYPTO.decryptWithOneParameter(encryptedType);
                    types.add(decryptedType);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error decrypting disaster type", e);
                }
            }

            return types;
        }

        @Override
        public List<DisasterModel> getDisasters() {
            List<DisasterModel> disasters = new ArrayList<>();

            for (DisasterModel encryptedDisaster : dao.getAllDisasters()) {
                try {
                    String decryptedType = CRYPTO.decryptWithOneParameter(encryptedDisaster.getDisasterType());
                    String decryptedName = CRYPTO.decryptWithOneParameter(encryptedDisaster.getDisasterName());

                    disasters.add(new DisasterModel(
                            encryptedDisaster.getDisasterId(),
                            decryptedType,
                            decryptedName
                    ));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error decrypting disaster", e);
                }
            }

            return disasters;
        }

        @Override
        public List<DisasterModel> getDisastersByType(String decryptedType) {
            List<DisasterModel> disasters = new ArrayList<>();

            if (decryptedType == null || decryptedType.trim().isEmpty()) {
                LOGGER.warning("getDisastersByType called with null or empty type");
                return disasters;
            }

            // Since AES-GCM uses random IVs, we can't query encrypted data directly.
            // Instead, fetch all disasters, decrypt them, and filter in memory.
            try {
                List<DisasterModel> allDisasters = getDisasters();
                
                for (DisasterModel disaster : allDisasters) {
                    if (disaster != null && disaster.getDisasterType() != null) {
                        // Compare decrypted types (case-insensitive for better matching)
                        if (disaster.getDisasterType().equalsIgnoreCase(decryptedType)) {
                            disasters.add(disaster);
                        }
                    }
                }

                LOGGER.info("Successfully retrieved " + disasters.size() + " disasters for type: " + decryptedType);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error filtering disasters by type: " + decryptedType, e);
            }

            return disasters;
        }

        @Override
        public List<DisasterCircleInfo> getAllDisasterCircles() {
            List<DisasterCircleInfo> circles = new ArrayList<>();

            for (DisasterCircleEncrypted c : dao.getAllDisasterCircles()) {
                try {
                    Double latVal = tryParse(CRYPTO.decryptWithOneParameter(c.lat));
                    Double lonVal = tryParse(CRYPTO.decryptWithOneParameter(c.lon));
                    Double radiusVal = tryParse(CRYPTO.decryptWithOneParameter(c.radius));
                    if (latVal.isNaN() || lonVal.isNaN() || radiusVal.isNaN() || radiusVal <= 0) {
                        continue;
                    }
                    double[] ll = sanitizeLatLon(latVal, lonVal);
                    if (ll == null) continue;
                    String type = CRYPTO.decryptWithOneParameter(c.disasterType);
                    String name = CRYPTO.decryptWithOneParameter(c.disasterName);

                    circles.add(new DisasterCircleInfo(ll[0], ll[1], radiusVal, name, type));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error decrypting disaster circle", e);
                }
            }

            return circles;
        }

        @Override
        public List<DisasterCircleInfo> getDisasterCirclesByDisasterId(int disasterId) {
            List<DisasterCircleInfo> circles = new ArrayList<>();

            for (DisasterCircleEncrypted c : dao.getDisasterCirclesByDisasterId(disasterId)) {
                try {
                    Double latVal = tryParse(CRYPTO.decryptWithOneParameter(c.lat));
                    Double lonVal = tryParse(CRYPTO.decryptWithOneParameter(c.lon));
                    Double radiusVal = tryParse(CRYPTO.decryptWithOneParameter(c.radius));
                    if (latVal.isNaN() || lonVal.isNaN() || radiusVal.isNaN() || radiusVal <= 0) {
                        continue;
                    }
                    double[] ll = sanitizeLatLon(latVal, lonVal);
                    if (ll == null) continue;
                    String type = CRYPTO.decryptWithOneParameter(c.disasterType);
                    String name = CRYPTO.decryptWithOneParameter(c.disasterName);

                    circles.add(new DisasterCircleInfo(ll[0], ll[1], radiusVal, name, type));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error decrypting disaster circle", e);
                }
            }

            return circles;
        }

        @Override
        public List<BeneficiaryMarker> getBeneficiaries() {
            List<BeneficiaryMarker> beneficiaries = new ArrayList<>();

            for (BeneficiaryEncrypted b : dao.getAllBeneficiaries()) {
                try {
                    Double latVal = tryParse(CRYPTO.decryptWithOneParameter(b.lat));
                    Double lonVal = tryParse(CRYPTO.decryptWithOneParameter(b.lng));
                    if (latVal.isNaN() || lonVal.isNaN()) {
                        continue;
                    }
                    double[] ll = sanitizeLatLon(latVal, lonVal);
                    if (ll == null) continue;
                    String fullName = NameDecryptionUtils.decryptFullName(b.encryptedFullName);

                    beneficiaries.add(new BeneficiaryMarker(b.id, fullName, ll[0], ll[1]));

                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error decrypting beneficiary (ID: " + b.id + ")", e);
                }
            }

            return beneficiaries;
        }

//        @Override
//        public List<BeneficiaryMarker> getBeneficiariesInsideCircle(double circleLat, double circleLon, double radiusMeters) {
//            List<BeneficiaryMarker> insideCircle = new ArrayList<>();
//
//            if (Double.isNaN(circleLat) || Double.isNaN(circleLon) || Double.isNaN(radiusMeters) || radiusMeters <= 0) {
//                LOGGER.warning("Invalid circle parameters for getBeneficiariesInsideCircle");
//                return insideCircle;
//            }
//
//            List<BeneficiaryMarker> allBeneficiaries = getBeneficiaries();
//
//            for (BeneficiaryMarker b : allBeneficiaries) {
//                if (Double.isNaN(b.lat) || Double.isNaN(b.lon)) {
//                    continue;
//                }
//
//                // Calculate distance using Haversine formula
//                double distance = GeographicUtils.calculateDistance(
//                    b.lat, b.lon, circleLat, circleLon
//                );
//
//                if (!Double.isNaN(distance) && distance <= radiusMeters) {
//                    insideCircle.add(b);
//                }
//            }
//
//            LOGGER.info("Found " + insideCircle.size() + " beneficiaries inside circle");
//            return insideCircle;
//        }

    @Override
    public List<BeneficiaryMarker> getBeneficiariesInsideCircle(double circleLat, double circleLon, double radiusMeters) {
        List<BeneficiaryMarker> insideCircle = new ArrayList<>();

        if (Double.isNaN(circleLat) || Double.isNaN(circleLon) || Double.isNaN(radiusMeters) || radiusMeters <= 0) {
            LOGGER.warning("Invalid circle parameters for getBeneficiariesInsideCircle");
            return insideCircle;
        }

        // ✅ Get ALL beneficiaries, not just those with disaster-specific scores
        List<BeneficiaryMarker> allBeneficiaries = getBeneficiaries();

        for (BeneficiaryMarker b : allBeneficiaries) {
            if (Double.isNaN(b.lat) || Double.isNaN(b.lon)) {
                continue;
            }

            // Calculate distance using Haversine formula
            double distance = GeographicUtils.calculateDistance(
                    b.lat, b.lon, circleLat, circleLon
            );

            if (!Double.isNaN(distance) && distance <= radiusMeters) {
                insideCircle.add(b);
            }
        }

        LOGGER.info("Found " + insideCircle.size() + " beneficiaries inside circle");
        return insideCircle;
    }

        @Override
        public DisasterModel getDisasterById(int disasterId) {
            DisasterModel encryptedDisaster = dao.getDisasterById(disasterId);

            if (encryptedDisaster == null) {
                return null;
            }

            try {
                String decryptedType = CRYPTO.decryptWithOneParameter(encryptedDisaster.getDisasterType());
                String decryptedName = CRYPTO.decryptWithOneParameter(encryptedDisaster.getDisasterName());

                return new DisasterModel(
                        encryptedDisaster.getDisasterId(),
                        decryptedType,
                        decryptedName
                );
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error decrypting disaster by ID", e);
                return null;
            }
        }
    }
