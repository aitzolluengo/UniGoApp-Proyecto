package com.tzolas.unigoapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class FavoritosManager {

    private static final String PREFS_NAME = "unigo_favoritos";
    private static final String KEY_PARADAS = "paradas_favoritas";

    public static Set<String> obtenerFavoritos(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(KEY_PARADAS, new HashSet<>()));
    }

    public static void guardarFavoritos(Context context, Set<String> favoritos) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putStringSet(KEY_PARADAS, favoritos).apply();
    }

    public static boolean esFavorita(Context context, String stopId) {
        return obtenerFavoritos(context).contains(stopId);
    }

    public static void toggleFavorito(Context context, String stopId) {
        Set<String> favoritos = obtenerFavoritos(context);
        if (favoritos.contains(stopId)) {
            favoritos.remove(stopId);
        } else {
            favoritos.add(stopId);
        }
        guardarFavoritos(context, favoritos);
    }
}