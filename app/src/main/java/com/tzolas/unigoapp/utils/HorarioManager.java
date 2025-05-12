package com.tzolas.unigoapp.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HorarioManager {

    public static List<String> obtenerProximasHorasConLinea(Context context, String stopId) {
        Map<String, String> tripToRoute = new HashMap<>();
        Map<String, String> routeNames = new HashMap<>();
        List<String> resultados = new ArrayList<>();
        Set<String> combinacionesUnicas = new HashSet<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm:ss");
        LocalTime ahora = LocalTime.now();

        try {
            // Cargar trips.txt → trip_id → route_id
            BufferedReader readerTrips = new BufferedReader(new InputStreamReader(context.getAssets().open("trips.txt")));
            readerTrips.readLine(); // Saltar cabecera
            String line;
            while ((line = readerTrips.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 3) {
                    String tripId = tokens[2];
                    String routeId = tokens[0];
                    tripToRoute.put(tripId, routeId);
                }
            }
            readerTrips.close();

            // Cargar routes.txt → route_id → route_short_name
            BufferedReader readerRoutes = new BufferedReader(new InputStreamReader(context.getAssets().open("routes.txt")));
            readerRoutes.readLine();
            while ((line = readerRoutes.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 3) {
                    String routeId = tokens[0];
                    String shortName = tokens[2];
                    routeNames.put(routeId, shortName);
                }
            }
            readerRoutes.close();

            // Leer stop_times.txt
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("stop_times.txt")));
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 4 && tokens[3].equals(stopId)) {
                    String tripId = tokens[0];
                    String horaStr = tokens[1];

                    try {
                        LocalTime hora = LocalTime.parse(horaStr, formatter);
                        if (hora.isAfter(ahora)) {
                            String routeId = tripToRoute.getOrDefault(tripId, "");
                            String routeName = routeNames.getOrDefault(routeId, "Línea ?");
                            String combinacion = horaStr + " – Línea " + routeName;

                            // Evitar duplicados exactos de hora + línea
                            if (combinacionesUnicas.add(combinacion)) {
                                resultados.add(combinacion);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
            reader.close();

        } catch (Exception e) {
            Log.e("HorarioManager", "Error leyendo horarios con línea", e);
        }

        Collections.sort(resultados);
        return resultados;
    }
}
