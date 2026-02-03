package com.ionres.respondph.evacuation_plan;


public class GeoDistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;


    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = EARTH_RADIUS_KM * c;

        return distance;
    }


    public static String formatDistance(double distanceInKm) {
        if (distanceInKm < 1.0) {
            int meters = (int) Math.round(distanceInKm * 1000);
            return meters + " m";
        } else {
            return String.format("%.2f km", distanceInKm);
        }
    }
}