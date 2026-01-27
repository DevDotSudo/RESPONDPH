package com.ionres.respondph.common.model;

public class DisasterCircleInfo {
        public final double lat;
        public final double lon;
        public final double radius;
        public final String disasterName;
        public final String disasterType;

        public DisasterCircleInfo(double lat, double lon, double radius, String disasterName, String disasterType) {
            this.lat = lat;
            this.lon = lon;
            this.radius = radius;
            this.disasterName = disasterName;
            this.disasterType = disasterType;
        }
    }
