package com.tzolas.unigoapp.fragments;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.tzolas.unigoapp.R;

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

    private ImageButton btnPie, btnBici, btnBus;
    private Polyline currentPolyline;
    private LatLng posicionActual;
    private String modoSeleccionado;

    private final LatLng campusAlava = new LatLng(42.8467, -2.6723);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_inicio, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapa);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnPie = rootView.findViewById(R.id.btn_pie);
        btnBici = rootView.findViewById(R.id.btn_bici);
        btnBus = rootView.findViewById(R.id.btn_bus);

        btnPie.setOnClickListener(v -> {
            if (posicionActual != null) {
                modoSeleccionado = "walking";
                guardarModoPreferido(modoSeleccionado);
                Toast.makeText(getContext(), "Calculando ruta a pie...", Toast.LENGTH_SHORT).show();
                calcularRuta(posicionActual, campusAlava, modoSeleccionado);
            } else {
                Toast.makeText(getContext(), "Ubicación aún no disponible", Toast.LENGTH_SHORT).show();
            }
        });

        btnBici.setOnClickListener(v -> {
            if (posicionActual != null) {
                modoSeleccionado = "bicycling";
                guardarModoPreferido(modoSeleccionado);
                Toast.makeText(getContext(), "Calculando ruta en bici...", Toast.LENGTH_SHORT).show();
                calcularRuta(posicionActual, campusAlava, modoSeleccionado);
            } else {
                Toast.makeText(getContext(), "Ubicación aún no disponible", Toast.LENGTH_SHORT).show();
            }
        });

        btnBus.setOnClickListener(v -> {
            if (posicionActual != null) {
                modoSeleccionado = "transit";
                guardarModoPreferido(modoSeleccionado);
                Toast.makeText(getContext(), "Calculando ruta en autobús...", Toast.LENGTH_SHORT).show();
                calcularRuta(posicionActual, campusAlava, modoSeleccionado);
            } else {
                Toast.makeText(getContext(), "Ubicación aún no disponible", Toast.LENGTH_SHORT).show();
            }
        });

        crearCanalNotificaciones();
        pedirPermisoNotificaciones();

        modoSeleccionado = cargarModoPreferido();
        return rootView;
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
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }

        map.setMyLocationEnabled(true);

        // Cargar carriles bici desde assets
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
            lineStyle.setColor(Color.RED);

            lineStyle.setWidth(10f);

            for (GeoJsonFeature feature : layer.getFeatures()) {
                feature.setLineStringStyle(lineStyle);
            }

            layer.addLayerToMap();
            Log.d("GeoJSON", "Carriles bici cargados desde assets.");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al cargar vías ciclistas", Toast.LENGTH_SHORT).show();
        }


        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                posicionActual = new LatLng(location.getLatitude(), location.getLongitude());
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(posicionActual, 15));
            } else {
                Toast.makeText(getContext(), "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
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
}
