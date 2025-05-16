package com.tzolas.unigoapp.workers;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.tzolas.unigoapp.R;
import com.tzolas.unigoapp.controller.ParadaController;
import com.tzolas.unigoapp.model.ParadaCercana;
import com.tzolas.unigoapp.utils.GtfsUtils;
import com.tzolas.unigoapp.utils.HorarioManager;
import com.tzolas.unigoapp.widgets.BusWidgetProvider;

import java.util.List;
import java.util.Set;

public class UbicacionWorker extends Worker {

    public UbicacionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure();
        }

        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();

                // 1. Obtener parada más cercana
                ParadaCercana parada = ParadaController.obtenerParadaMasCercana(getApplicationContext(), lat, lon);

                if (parada == null) return;

                // 2. Obtener líneas y horarios
                Set<String> lineas = GtfsUtils.obtenerLineasDeParada(getApplicationContext(), parada.stopId);
                List<String> horarios = HorarioManager.obtenerProximasHorasConLinea(getApplicationContext(), parada.stopId);

                // 3. Guardar en SharedPreferences
                SharedPreferences prefs = getApplicationContext().getSharedPreferences("UniGoPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("ultimaParadaNombre", parada.nombre);
                editor.putString("ultimaParadaLineas", String.join(", ", lineas));
                editor.putString("ultimaParadaHorarios", horarios.isEmpty() ?
                        "No disponibles" :
                        String.join("\n", horarios.subList(0, Math.min(3, horarios.size())))
                );
                editor.apply();

                // 4. Actualizar el widget
                AppWidgetManager manager = AppWidgetManager.getInstance(getApplicationContext());
                ComponentName widget = new ComponentName(getApplicationContext(), BusWidgetProvider.class);
                RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_bus);

                views.setTextViewText(R.id.widget_parada, "Parada: " + parada.nombre);
                views.setTextViewText(R.id.widget_lineas, "Líneas: " + String.join(", ", lineas));
                views.setTextViewText(R.id.widget_horarios,
                        horarios.isEmpty() ? "Próximos: No disponibles" : "Próximos:\n" + String.join("\n", horarios.subList(0, Math.min(3, horarios.size())))
                );

                manager.updateAppWidget(widget, views);
            }
        });

        return Result.success();
    }
}
