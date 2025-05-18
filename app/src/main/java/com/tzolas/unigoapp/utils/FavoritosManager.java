package com.tzolas.unigoapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.tzolas.unigoapp.api.ApiClient;
import com.tzolas.unigoapp.api.ApiInterface;
import com.tzolas.unigoapp.model.ServerResponse;

import java.util.HashSet;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritosManager {

    private static Set<String> favoritosCache = new HashSet<>();

    public static void cargarFavoritos(Context context, int userId, Runnable onReady) {
        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        Call<Set<String>> call = api.listarFavoritos(userId);

        call.enqueue(new Callback<Set<String>>() {
            @Override
            public void onResponse(Call<Set<String>> call, Response<Set<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    favoritosCache = new HashSet<>(response.body());
                } else {
                    favoritosCache = new HashSet<>();
                }
                onReady.run(); // callback para continuar
            }

            @Override
            public void onFailure(Call<Set<String>> call, Throwable t) {
                favoritosCache = new HashSet<>();
                Toast.makeText(context, "Error al cargar favoritos", Toast.LENGTH_SHORT).show();
                onReady.run();
            }
        });
    }

    public static boolean esFavorita(String stopId) {
        return favoritosCache.contains(stopId);
    }

    public static void toggleFavorito(Context context, int userId, String stopId, Runnable onFinish) {
        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        boolean esFav = favoritosCache.contains(stopId);

        Call<ServerResponse> call = esFav
                ? api.eliminarFavorito(userId, stopId)
                : api.insertarFavorito(userId, stopId);

        call.enqueue(new Callback<ServerResponse>() {
            @Override
            public void onResponse(Call<ServerResponse> call, Response<ServerResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    if (esFav) favoritosCache.remove(stopId);
                    else favoritosCache.add(stopId);
                }
                onFinish.run(); // continuar después del toggle
            }

            @Override
            public void onFailure(Call<ServerResponse> call, Throwable t) {
                Toast.makeText(context, "Error de red", Toast.LENGTH_SHORT).show();
                onFinish.run();
            }
        });
    }
    public static Set<String> getCache() {
        return new HashSet<>(favoritosCache);
    }

}
