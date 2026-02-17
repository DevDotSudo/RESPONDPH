package com.ionres.respondph.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class GeographicUtils {
    private static final Logger LOGGER = Logger.getLogger(GeographicUtils.class.getName());
    
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    
    private GeographicUtils() {
        // Utility class - prevent instantiation
    }
    
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Validate inputs
        if (Double.isNaN(lat1) || Double.isNaN(lon1) || Double.isNaN(lat2) || Double.isNaN(lon2)) {
            LOGGER.fine("Invalid coordinates for distance calculation");
            return Double.NaN;
        }
        
        if (!Mapping.isValidCoordinate(lat1, lon1) || !Mapping.isValidCoordinate(lat2, lon2)) {
            LOGGER.fine("Coordinates out of valid range for distance calculation");
            return Double.NaN;
        }
        
        try {
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);
            
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2);
            
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double distance = EARTH_RADIUS_METERS * c;
            
            return distance;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error calculating distance", e);
            return Double.NaN;
        }
    }

    public static boolean isInsideCircle(double pointLat, double pointLon,
                                        double circleLat, double circleLon, 
                                        double radiusMeters) {
        if (Double.isNaN(pointLat) || Double.isNaN(pointLon) ||
            Double.isNaN(circleLat) || Double.isNaN(circleLon) || 
            Double.isNaN(radiusMeters) || radiusMeters <= 0) {
            return false;
        }
        
        double distance = calculateDistance(pointLat, pointLon, circleLat, circleLon);
        return !Double.isNaN(distance) && distance <= radiusMeters;
    }
}
