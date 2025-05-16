package com.tzolas.unigoapp.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.tzolas.unigoapp.R;
import com.tzolas.unigoapp.activities.MainActivity;

public class UniGoWidget extends AppWidgetProvider {

    private static final String PREFS_NAME = "prefs_unigo";
    private static final String KEY_MODO_TRANSPORTE = "modo_transporte";

    public static void actualizarWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String modo = prefs.getString(KEY_MODO_TRANSPORTE, "walking");

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_unigo);
        views.setTextViewText(R.id.texto_modo, "Modo: " + modo);

        // Intent que lanza MainActivity e incluye una señal para calcular ruta
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("calcular_ruta", true);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        views.setOnClickPendingIntent(R.id.boton_abrir, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            actualizarWidget(context, appWidgetManager, id);
        }
    }
}