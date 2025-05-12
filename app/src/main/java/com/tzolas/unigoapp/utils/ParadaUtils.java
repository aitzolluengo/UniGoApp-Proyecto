package com.tzolas.unigoapp.utils;

public class ParadaUtils {
    public static boolean estaEnVitoria(double lat, double lon) {
        return lat >= 42.7900 && lat <= 42.8800 && lon >= -2.7400 && lon <= -2.6200;
    }

    public static double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
