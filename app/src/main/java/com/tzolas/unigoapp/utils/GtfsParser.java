package com.tzolas.unigoapp.utils;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;

import java.io.*;
import java.util.*;

public class GtfsParser {

    public static Map<String, String> cargarNombresParadas(Context context, Map<String, LatLng> coords) throws IOException {
        Map<String, String> nombres = new HashMap<>();
        InputStream inputStream = context.getAssets().open("stops.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        reader.readLine();
        String line;
        while ((line = reader.readLine()) != null) {
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 7) {
                String stopId = tokens.get(0);
                String name = tokens.get(2);
                double lat = Double.parseDouble(tokens.get(5));
                double lon = Double.parseDouble(tokens.get(6));
                if (ParadaUtils.estaEnVitoria(lat, lon)) {
                    coords.put(stopId, new LatLng(lat, lon));
                    nombres.put(stopId, name);
                }
            }
        }
        reader.close();
        return nombres;
    }

    public static Map<String, Set<String>> cargarLineasPorParada(Context context) throws IOException {
        Map<String, String> tripToRoute = new HashMap<>();
        Map<String, String> routeNames = new HashMap<>();
        Map<String, Set<String>> lineasPorParada = new HashMap<>();

        BufferedReader readerTrips = new BufferedReader(new InputStreamReader(context.getAssets().open("trips.txt")));
        readerTrips.readLine();
        String line;
        while ((line = readerTrips.readLine()) != null) {
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 3) {
                tripToRoute.put(tokens.get(2), tokens.get(0));
            }
        }
        readerTrips.close();

        BufferedReader readerRoutes = new BufferedReader(new InputStreamReader(context.getAssets().open("routes.txt")));
        readerRoutes.readLine();
        while ((line = readerRoutes.readLine()) != null) {
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 3) {
                routeNames.put(tokens.get(0), tokens.get(2));
            }
        }
        readerRoutes.close();

        BufferedReader readerStopTimes = new BufferedReader(new InputStreamReader(context.getAssets().open("stop_times.txt")));
        readerStopTimes.readLine();
        while ((line = readerStopTimes.readLine()) != null) {
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 4) {
                String tripId = tokens.get(0);
                String stopId = tokens.get(3);
                String routeId = tripToRoute.get(tripId);
                String routeName = routeNames.get(routeId);
                if (routeName != null) {
                    lineasPorParada.putIfAbsent(stopId, new HashSet<>());
                    lineasPorParada.get(stopId).add(routeName);
                }
            }
        }
        readerStopTimes.close();

        return lineasPorParada;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean insideQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '\"') {
                insideQuotes = !insideQuotes;
            } else if (c == ',' && !insideQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens;
    }
}
