package com.tzolas.unigoapp.utils;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class GtfsUtils {

    public static Map<String, String> cargarStopNames(Context context, Map<String, LatLng> stopCoords) {
        Map<String, String> stopNames = new HashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("stops.txt")));
            reader.readLine(); // Skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 7) {
                    String stopId = tokens[0];
                    String stopName = tokens[2];
                    double lat = Double.parseDouble(tokens[5]);
                    double lon = Double.parseDouble(tokens[6]);
                    boolean dentroZona = lat >= 42.7900 && lat <= 42.8800 && lon >= -2.7400 && lon <= -2.6200;
                    if (dentroZona) {
                        stopCoords.put(stopId, new LatLng(lat, lon));
                        stopNames.put(stopId, stopName);
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stopNames;
    }

    public static Map<String, Set<String>> cargarLineasPorParada(Context context) {
        Map<String, Set<String>> lineasPorParada = new HashMap<>();
        try {
            Map<String, String> tripToRoute = new HashMap<>();
            Map<String, String> routeNames = new HashMap<>();

            BufferedReader readerTrips = new BufferedReader(new InputStreamReader(context.getAssets().open("trips.txt")));
            readerTrips.readLine();
            String line;
            while ((line = readerTrips.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 3) {
                    tripToRoute.put(tokens[2], tokens[0]);
                }
            }
            readerTrips.close();

            BufferedReader readerRoutes = new BufferedReader(new InputStreamReader(context.getAssets().open("routes.txt")));
            readerRoutes.readLine();
            while ((line = readerRoutes.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 3) {
                    routeNames.put(tokens[0], tokens[2]);
                }
            }
            readerRoutes.close();

            BufferedReader readerStopTimes = new BufferedReader(new InputStreamReader(context.getAssets().open("stop_times.txt")));
            readerStopTimes.readLine();
            while ((line = readerStopTimes.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 4) {
                    String tripId = tokens[0];
                    String stopId = tokens[3];
                    String routeId = tripToRoute.getOrDefault(tripId, "");
                    String routeName = routeNames.getOrDefault(routeId, "");
                    if (!stopId.isEmpty() && !routeName.isEmpty()) {
                        lineasPorParada.putIfAbsent(stopId, new HashSet<>());
                        lineasPorParada.get(stopId).add(routeName);
                    }
                }
            }
            readerStopTimes.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return lineasPorParada;
    }

    public static Set<String> obtenerLineasAlCampus(Context context) {
        Set<String> stopIdsCampus = new HashSet<>(Arrays.asList("57", "58"));
        Set<String> tripIds = new HashSet<>();
        Set<String> routeIds = new HashSet<>();
        Set<String> lineasCampus = new HashSet<>();

        try {
            BufferedReader readerStopTimes = new BufferedReader(new InputStreamReader(context.getAssets().open("stop_times.txt")));
            readerStopTimes.readLine();
            String line;
            while ((line = readerStopTimes.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 4 && stopIdsCampus.contains(tokens[3])) {
                    tripIds.add(tokens[0]);
                }
            }
            readerStopTimes.close();

            BufferedReader readerTrips = new BufferedReader(new InputStreamReader(context.getAssets().open("trips.txt")));
            readerTrips.readLine();
            while ((line = readerTrips.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 3 && tripIds.contains(tokens[2])) {
                    routeIds.add(tokens[0]);
                }
            }
            readerTrips.close();

            BufferedReader readerRoutes = new BufferedReader(new InputStreamReader(context.getAssets().open("routes.txt")));
            readerRoutes.readLine();
            while ((line = readerRoutes.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 3 && routeIds.contains(tokens[0])) {
                    lineasCampus.add(tokens[2]);
                }
            }
            readerRoutes.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return lineasCampus;
    }
    public static Set<String> obtenerLineasDeParada(Context context, String stopIdObjetivo) {
        Set<String> lineas = new HashSet<>();
        try {
            Map<String, String> tripToRoute = new HashMap<>();
            Map<String, String> routeNames = new HashMap<>();

            // Cargar trips.txt → trip_id → route_id
            BufferedReader readerTrips = new BufferedReader(new InputStreamReader(context.getAssets().open("trips.txt")));
            readerTrips.readLine();
            String line;
            while ((line = readerTrips.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 3) {
                    tripToRoute.put(tokens[2], tokens[0]);
                }
            }
            readerTrips.close();

            // Cargar routes.txt → route_id → nombre línea
            BufferedReader readerRoutes = new BufferedReader(new InputStreamReader(context.getAssets().open("routes.txt")));
            readerRoutes.readLine();
            while ((line = readerRoutes.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 3) {
                    routeNames.put(tokens[0], tokens[2]);
                }
            }
            readerRoutes.close();

            // Buscar líneas por stopId
            BufferedReader readerStopTimes = new BufferedReader(new InputStreamReader(context.getAssets().open("stop_times.txt")));
            readerStopTimes.readLine();
            while ((line = readerStopTimes.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 4 && tokens[3].equals(stopIdObjetivo)) {
                    String tripId = tokens[0];
                    String routeId = tripToRoute.getOrDefault(tripId, "");
                    String routeName = routeNames.getOrDefault(routeId, "");
                    if (!routeName.isEmpty()) {
                        lineas.add(routeName);
                    }
                }
            }
            readerStopTimes.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return lineas;
    }

}
