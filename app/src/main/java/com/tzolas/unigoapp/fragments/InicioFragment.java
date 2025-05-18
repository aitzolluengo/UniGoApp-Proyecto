package com.tzolas.unigoapp.fragments;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tzolas.unigoapp.R;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class InicioFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private FloatingActionButton fabTransporte;
    private CardView cardBienvenida;
    private Polyline currentPolyline;
    private LatLng posicionActual;
    private String modoSeleccionado;

    private boolean isFabRotated = false;

    private final LatLng campusAlava = new LatLng(42.83988450929749, -2.669759213327361);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_inicio, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        fabTransporte = rootView.findViewById(R.id.fab_transporte);
        cardBienvenida = rootView.findViewById(R.id.card_bienvenida);


        fabTransporte.setOnClickListener(v -> {
            mostrarBottomSheet();
            if (!isFabRotated) {
                rotateFabOpen();
            } else {
                rotateFabClose();
            }
            isFabRotated = !isFabRotated;
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapa);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        crearCanalNotificaciones();
        pedirPermisoNotificaciones();
        modoSeleccionado = cargarModoPreferido();

        comprobarUbicacionEnGasteiz();
        return rootView;
    }

    private void mostrarBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottomsheet_transporte, null);
        dialog.setContentView(sheetView);

        sheetView.findViewById(R.id.opcion_andar).setOnClickListener(v -> {
            if (cardBienvenida.getVisibility() == View.VISIBLE) {
                cardBienvenida.setVisibility(View.GONE);
            }

            if (posicionActual != null) {
                modoSeleccionado = "walking";
                guardarModoPreferido(modoSeleccionado);
                Toast.makeText(getContext(), "Calculando ruta a pie...", Toast.LENGTH_SHORT).show();
                calcularRuta(posicionActual, campusAlava, modoSeleccionado);
            }

            if (isFabRotated) {
                rotateFabClose();
                isFabRotated = false;
            }
            dialog.dismiss();
        });

        sheetView.findViewById(R.id.opcion_bici).setOnClickListener(v -> {
            if (cardBienvenida.getVisibility() == View.VISIBLE) {
                cardBienvenida.setVisibility(View.GONE);
            }

            if (posicionActual != null) {
                modoSeleccionado = "bicycling";
                guardarModoPreferido(modoSeleccionado);
                cargarBidegorris();
                mostrarDialogoBidegorri(); // Mostrar el diálogo visual
                calcularRuta(posicionActual, campusAlava, modoSeleccionado);
            }

            if (isFabRotated) {
                rotateFabClose();
                isFabRotated = false;
            }
            dialog.dismiss();
        });

        sheetView.findViewById(R.id.opcion_bus).setOnClickListener(v -> {
            if (cardBienvenida.getVisibility() == View.VISIBLE) {
                cardBienvenida.setVisibility(View.GONE);
            }

            if (posicionActual != null) {
                modoSeleccionado = "transit";
                guardarModoPreferido(modoSeleccionado);
                Toast.makeText(getContext(), "Cargando paradas de autobús...", Toast.LENGTH_SHORT).show();

                // Ir al BusFragment directamente
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.content_frame, new BusFragment())
                        .addToBackStack(null)
                        .commit();
                requireActivity().setTitle("Paradas de bus");
            }

            if (isFabRotated) {
                rotateFabClose();
                isFabRotated = false;
            }

            dialog.dismiss();
        });


        dialog.setOnDismissListener(dialogInterface -> {
            if (isFabRotated) {
                rotateFabClose();
                isFabRotated = false;
            }
        });

        dialog.show();
    }

    private void mostrarDialogoBidegorri() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_bidegorri, null);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Entendido", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void rotateFabOpen() {
        fabTransporte.animate().rotation(45f).setDuration(200).start();
    }

    private void rotateFabClose() {
        fabTransporte.animate().rotation(0f).setDuration(200).start();
    }

    private void cargarBidegorris() {
        try {
            InputStream inputStream = requireContext().getAssets().open("viasciclistas23.geojson");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            reader.close();
            inputStream.close();

            JSONObject json = new JSONObject(builder.toString());
            GeoJsonLayer layer = new GeoJsonLayer(map, json);

            GeoJsonLineStringStyle lineStyle = new GeoJsonLineStringStyle();
            lineStyle.setColor(Color.parseColor("#FF4444")); // rojo más brillante
            lineStyle.setWidth(10f);
            lineStyle.setZIndex(1000); // que esté por encima de todo


            for (GeoJsonFeature feature : layer.getFeatures()) {
                feature.setLineStringStyle(lineStyle);
            }

            layer.addLayerToMap();
            Log.d("GeoJSON", "Carriles bici cargados desde assets.");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al cargar vías ciclistas", Toast.LENGTH_SHORT).show();
        }
    }

    private void calcularRuta(LatLng origen, LatLng destino, String modo) {
        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origen.latitude + "," + origen.longitude +
                "&destination=" + destino.latitude + "," + destino.longitude +
                "&mode=" + modo +
                "&key=" + getString(R.string.google_maps_key);

        new RutaTask().execute(url);
    }

    private class RutaTask extends AsyncTask<String, Void, ArrayList<LatLng>> {
        @Override
        protected ArrayList<LatLng> doInBackground(String... urls) {
            ArrayList<LatLng> puntos = new ArrayList<>();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    json.append(line);
                }

                JSONObject obj = new JSONObject(json.toString());
                JSONArray routes = obj.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject overview = routes.getJSONObject(0).getJSONObject("overview_polyline");
                    String encoded = overview.getString("points");
                    puntos = decodificarPolyline(encoded);
                }

            } catch (Exception e) {
                Log.e("RutaTask", "Error al descargar ruta: " + e.getMessage());
            }
            return puntos;
        }

        @Override
        protected void onPostExecute(ArrayList<LatLng> puntos) {
            if (map != null && puntos != null && !puntos.isEmpty()) {
                if (currentPolyline != null) currentPolyline.remove();

                currentPolyline = map.addPolyline(new PolylineOptions()
                        .addAll(puntos)
                        .color(Color.BLUE)
                        .width(14f));

                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                for (LatLng point : puntos) {
                    boundsBuilder.include(point);
                }

                map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
                enviarNotificacionRuta();
            } else {
                Toast.makeText(getContext(), "No se pudo calcular la ruta", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private ArrayList<LatLng> decodificarPolyline(String encoded) {
        ArrayList<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;

            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int deltaLat = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lat += deltaLat;

            shift = 0;
            result = 0;

            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int deltaLng = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lng += deltaLng;

            LatLng point = new LatLng(lat / 1E5, lng / 1E5);
            poly.add(point);
        }
        return poly;
    }

    private void crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String canalId = "ruta_unigo";
            NotificationChannel canal = new NotificationChannel(
                    canalId,
                    "Rutas UniGo",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            canal.setDescription("Notificaciones sobre rutas calculadas");
            NotificationManager manager = requireContext().getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(canal);
        }
    }

    private void enviarNotificacionRuta() {
        String canalId = "ruta_unigo";
        String texto;
        switch (modoSeleccionado) {
            case "bicycling":
                texto = "Ruta en bicicleta lista hasta el campus.";
                break;
            case "transit":
                texto = "Ruta en autobús lista hasta el campus.";
                break;
            default:
                texto = "Ruta a pie lista hasta el campus.";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), canalId)
                .setSmallIcon(R.drawable.ic_directions_walk)
                .setContentTitle("Ruta lista")
                .setContentText(texto)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(requireContext()).notify(1, builder.build());
        }
    }

    private void pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private void guardarModoPreferido(String modo) {
        requireContext().getSharedPreferences("prefs_unigo", 0)
                .edit()
                .putString("modo_transporte", modo)
                .apply();
    }

    private String cargarModoPreferido() {
        return requireContext().getSharedPreferences("prefs_unigo", 0)
                .getString("modo_transporte", "walking");
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        SharedPreferences prefs = requireContext().getSharedPreferences("prefs_unigo", 0);
        boolean modoOscuro = prefs.getBoolean("modo_oscuro", false);

        if (modoOscuro) {
            try {
                boolean success = map.setMapStyle(
                        com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(
                                requireContext(), R.raw.map_style_dark));
                if (!success) {
                    Log.e("MapStyle", "Fallo al aplicar estilo oscuro al mapa.");
                }
            } catch (Exception e) {
                Log.e("MapStyle", "No se pudo cargar el estilo oscuro: ", e);
            }
        }

        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }

        map.getUiSettings().setMyLocationButtonEnabled(true);


        // Añadir marcador al campus de Álava
        map.addMarker(new MarkerOptions()
                .position(campusAlava)
                .title("Campus de Álava - EHU")
                .snippet("Destino principal"));

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                posicionActual = new LatLng(location.getLatitude(), location.getLongitude());

                // Centrar y añadir marcador
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(posicionActual, 15));
                map.addMarker(new MarkerOptions()
                        .position(posicionActual)
                        .title("📍 Estás aquí"));

                Bundle args = getArguments();
                if (args != null && args.getBoolean("calcular_ruta", false)) {
                    modoSeleccionado = cargarModoPreferido();
                    calcularRuta(posicionActual, campusAlava, modoSeleccionado);
                    Toast.makeText(getContext(), "Ruta automática desde widget", Toast.LENGTH_SHORT).show();
                }

            } else {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(campusAlava, 14));
                Toast.makeText(getContext(), "Ubicación no disponible", Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onMapReady(map);
        }
    }
    private void comprobarUbicacionEnGasteiz() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();

                boolean dentroGasteiz = lat >= 42.8050 && lat <= 42.8730 && lon >= -2.7150 && lon <= -2.6350;

                if (!dentroGasteiz) {
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Fuera de Vitoria-Gasteiz")
                            .setMessage("Esta aplicación solo está diseñada para funcionar dentro de Vitoria-Gasteiz.")
                            .setPositiveButton("Entendido", null)
                            .setCancelable(false)
                            .show();
                }
            }
        });
    }

}