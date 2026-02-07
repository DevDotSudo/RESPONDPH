package com.ionres.respondph.util;

import java.util.ArrayList;
import java.util.List;


public class BarangayMapper {

    private static BarangayMapper instance;
    private final List<BarangayPolygon> barangays;

    private BarangayMapper() {
        barangays = new ArrayList<>();
        initializeBarangays();
    }

    public static synchronized BarangayMapper getInstance() {
        if (instance == null) {
            instance = new BarangayMapper();
        }
        return instance;
    }


    private void initializeBarangays() {
        // Barangay San Salvador
        // Based on your coordinates from the map
        double[][] sanSalvadorBoundary = {
                {10.9989, 122.8339},
                {10.9988, 122.8350},
                {11.0006, 122.8363},
                {11.0026, 122.8366},
                {11.0058, 122.8384},
                {11.0096, 122.8380},
                {11.0079, 122.8442},
                {11.0060, 122.8486},
                {11.0037, 122.8498},
                {10.9978, 122.8527},
                {10.9977, 122.8537},
                {10.9945, 122.8545},
                {10.9912, 122.8519},
                {10.9896, 122.8451},
                {10.9948, 122.8442},
                {10.9963, 122.8411},
                {10.9974, 122.8338},
                {10.9982, 122.8333}
        };
        barangays.add(new BarangayPolygon("San Salvador", sanSalvadorBoundary));

        double[][] talokganganBoundary = {
                {11.0014, 122.8357},
                {10.9999, 122.8355},
                {11.0019, 122.8257},
                {11.0046, 122.8243},
                {11.0198, 122.8318},
                {11.0117, 122.8375},
                {11.0084, 122.8380},
                {11.0040, 122.8373},
                {11.0022, 122.8361},
        };

        barangays.add(new BarangayPolygon("Talokgangan", talokganganBoundary));


        double[][] bularanBoundary = {
                {11.0016, 122.8255},
                {11.0011, 122.8227},
                {11.0044, 122.8217},
                {11.0047, 122.8226},
                {11.0043, 122.8227},
                {11.0050, 122.8241}
        };

        barangays.add(new BarangayPolygon("Bularan", bularanBoundary));

        double[][] poblacionBoundary = {
                {11.0012, 122.8225},
                {10.9998, 122.8175},
                {11.0027, 122.8166},
                {11.0047, 122.8220}
        };

        barangays.add(new BarangayPolygon("Poblacion", poblacionBoundary));

    }


    public String getBarangayName(double latitude, double longitude) {
        for (BarangayPolygon barangay : barangays) {
            if (barangay.contains(latitude, longitude)) {
                return barangay.getName();
            }
        }
        return null;
    }

    /**
     * Get barangay name from string coordinates
     */
    public String getBarangayName(String latStr, String lonStr) {
        if (latStr == null || lonStr == null || latStr.isEmpty() || lonStr.isEmpty()) {
            return null;
        }

        try {
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);
            return getBarangayName(lat, lon);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get all registered barangay names
     */
    public List<String> getAllBarangayNames() {
        List<String> names = new ArrayList<>();
        for (BarangayPolygon barangay : barangays) {
            names.add(barangay.getName());
        }
        return names;
    }

    /**
     * Inner class representing a barangay with polygon boundary
     * Uses ray-casting algorithm to check if point is inside polygon
     */
    private static class BarangayPolygon {
        private final String name;
        private final double[][] boundary;  // Array of [lat, lon] coordinates

        public BarangayPolygon(String name, double[][] boundary) {
            this.name = name;
            this.boundary = boundary;
        }

        public String getName() {
            return name;
        }

        /**
         * Check if a point (lat, lon) is inside this barangay's polygon
         * Uses ray-casting algorithm - same as used in GIS systems
         */
        public boolean contains(double lat, double lon) {
            if (boundary == null || boundary.length < 3) {
                return false;
            }

            boolean inside = false;
            int j = boundary.length - 1;

            for (int i = 0; i < boundary.length; i++) {
                double latI = boundary[i][0];
                double lonI = boundary[i][1];
                double latJ = boundary[j][0];
                double lonJ = boundary[j][1];

                // Ray-casting algorithm
                if ((lonI > lon) != (lonJ > lon) &&
                        lat < (latJ - latI) * (lon - lonI) / (lonJ - lonI) + latI) {
                    inside = !inside;
                }

                j = i;
            }

            return inside;
        }

        @Override
        public String toString() {
            return String.format("%s (boundary points: %d)", name, boundary.length);
        }
    }
}