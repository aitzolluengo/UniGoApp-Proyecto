package com.tzolas.unigoapp.controller;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;
import com.tzolas.unigoapp.model.ParadaCercana;
import com.tzolas.unigoapp.utils.GtfsUtils;
import com.tzolas.unigoapp.utils.ParadaManager;

import java.util.List;
import java.util.Map;

public class ParadaController {

    /**
     * Devuelve una lista de paradas ordenadas por cercanía desde la ubicación del usuario.
     * Esta clase oculta la lógica de carga y cálculo al fragmento.
     */
    public static List<ParadaCercana> cargarParadasCercanas(Context context, double latUser, double lonUser) {
        Map<String, LatLng> stopCoords = new java.util.HashMap<>();
        Map<String, String> stopNames = GtfsUtils.cargarStopNames(context, stopCoords);

        return ParadaManager.obtenerParadasCercanasDesdeUbicacion(
                latUser, lonUser, stopCoords, stopNames
        );
    }
}
