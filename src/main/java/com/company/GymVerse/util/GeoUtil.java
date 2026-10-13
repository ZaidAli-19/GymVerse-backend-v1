package com.company.GymVerse.util;

import org.springframework.stereotype.Component;

/**
 * Utility for geospatial calculations such as geofence distance verification.
 */
@Component
public class GeoUtil {

    public boolean isInsideGym(
            double userLat,
            double userLng,
            double gymLat,
            double gymLng,
            double radiusInMeters
    ) {
        return distanceInMeters(userLat, userLng, gymLat, gymLng) <= radiusInMeters;
    }

    private double distanceInMeters(
            double userLat,
            double userLng,
            double gymLat,
            double gymLng
    ) {
        final int earthRadiusMeters = 6371000;

        double latDistance = Math.toRadians(gymLat - userLat);
        double lngDistance = Math.toRadians(gymLng - userLng);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(userLat))
                * Math.cos(Math.toRadians(gymLat))
                * Math.sin(lngDistance / 2)
                * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusMeters * c;
    }
}
