package com.ionres.respondph.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for geographic calculations.
 * Provides methods for distance calculations using the Haversine formula.
 */
public final class GeographicUtils {
    private static final Logger LOGGER = Logger.getLogger(GeographicUtils.class.getName());
    
    /** Earth's radius in meters */
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    
    private GeographicUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Calculates the distance between two points on Earth using the Haversine formula.
     * 
     * @param lat1 Latitude of first point in degrees
     * @param lon1 Longitude of first point in degrees
     * @param lat2 Latitude of second point in degrees
     * @param lon2 Longitude of second point in degrees
     * @return Distance in meters, or Double.NaN if inputs are invalid
     */
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
            // Convert differences to radians
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);
            
            // Haversine formula
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
    
    /**
     * Checks if a point is inside a circle defined by center and radius.
     * 
     * @param pointLat Latitude of the point in degrees
     * @param pointLon Longitude of the point in degrees
     * @param circleLat Latitude of the circle center in degrees
     * @param circleLon Longitude of the circle center in degrees
     * @param radiusMeters Radius of the circle in meters
     * @return true if the point is inside the circle, false otherwise
     */
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
