package com.tzolas.unigoapp.utils;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;
import com.tzolas.unigoapp.model.ParadaCercana;

import java.util.*;

public class ParadaManager {

    public static List<ParadaCercana> obtenerParadasCercanasDesdeUbicacion(
            double latUser,
            double lonUser,
            Map<String, LatLng> stopCoords,
            Map<String, String> stopNames
    ) {
        List<ParadaCercana> paradas = new ArrayList<>();

        for (String stopId : stopCoords.keySet()) {
            LatLng pos = stopCoords.get(stopId);
            double distancia = ParadaUtils.calcularDistancia(latUser, lonUser, pos.latitude, pos.longitude);
            String nombre = stopNames.getOrDefault(stopId, "Sin nombre");
            paradas.add(new ParadaCercana(stopId, nombre, distancia));
        }

        paradas.sort(Comparator.comparingDouble(p -> p.distancia));
        return paradas;
    }
}
