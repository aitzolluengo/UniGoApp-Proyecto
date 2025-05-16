package com.tzolas.unigoapp.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.tzolas.unigoapp.R;
import com.tzolas.unigoapp.workers.UbicacionWorker;

public class BusWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_ACTUALIZAR_WIDGET = "com.tzolas.unigoapp.ACTION_ACTUALIZAR_WIDGET";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_bus);

            SharedPreferences prefs = context.getSharedPreferences("UniGoPrefs", Context.MODE_PRIVATE);
            String parada = prefs.getString("ultimaParadaNombre", "Desconocida");
            String lineas = prefs.getString("ultimaParadaLineas", "Ninguna");
            String horarios = prefs.getString("ultimaParadaHorarios", "No disponibles");

            views.setTextViewText(R.id.widget_parada, "Parada: " + parada);
            views.setTextViewText(R.id.widget_lineas, "Líneas: " + lineas);
            views.setTextViewText(R.id.widget_horarios, horarios);

            // CONFIGURAR EL BOTÓN
            Intent intent = new Intent(context, BusWidgetProvider.class);
            intent.setAction(ACTION_ACTUALIZAR_WIDGET);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.btn_actualizar_widget, pendingIntent);

            manager.updateAppWidget(id, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_ACTUALIZAR_WIDGET.equals(intent.getAction())) {
            // Ejecutar el Worker manualmente
            OneTimeWorkRequest workRequest =
                    new OneTimeWorkRequest.Builder(UbicacionWorker.class).build();
            WorkManager.getInstance(context).enqueue(workRequest);

            Toast.makeText(context, "Actualizando parada cercana...", Toast.LENGTH_SHORT).show();
        }
    }
}


