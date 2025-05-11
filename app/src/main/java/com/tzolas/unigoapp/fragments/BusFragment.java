package com.tzolas.unigoapp.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tzolas.unigoapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

public class BusFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap map;
    private Map<String, List<String>> lineasPorParada = new HashMap<>();
    private Map<Marker, String> markerStopIds = new HashMap<>();
    private final LatLng CAMPUS_ALAVA = new LatLng(42.83988450929749, -2.669759213327361);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bus, container, false);

        cargarLineasDesdeJson();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.bus_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        FloatingActionButton fabCampus = rootView.findViewById(R.id.fab_lineas_campus);
        fabCampus.setOnClickListener(v -> mostrarParadasMasCercanasAlCampus());

        return rootView;
    }

    private void cargarLineasDesdeJson() {
        try {
            InputStream inputStream = requireContext().getAssets().open("lineas_por_parada.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();
            inputStream.close();

            JSONObject jsonObject = new JSONObject(jsonBuilder.toString());

            for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                String stopId = it.next();
                JSONArray lineasArray = jsonObject.getJSONArray(stopId);
                List<String> lineas = new ArrayList<>();
                for (int i = 0; i < lineasArray.length(); i++) {
                    lineas.add(lineasArray.getString(i));
                }
                lineasPorParada.put(stopId, lineas);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al cargar líneas por parada", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        try {
            InputStream inputStream = requireContext().getAssets().open("stops_gasteiz.geojson");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            inputStream.close();

            JSONObject json = new JSONObject(builder.toString());
            JSONArray features = json.getJSONArray("features");

            BitmapDescriptor iconoBus = BitmapDescriptorFactory.fromBitmap(
                    Bitmap.createScaledBitmap(
                            BitmapFactory.decodeResource(getResources(), R.drawable.ic_bus_marker),
                            96, 96, false
                    )
            );

            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");
                JSONObject props = feature.getJSONObject("properties");

                double lat = geometry.getJSONArray("coordinates").getDouble(1);
                double lon = geometry.getJSONArray("coordinates").getDouble(0);
                String stopId = props.getString("stop_id");
                String stopName = props.getString("stop_name");

                LatLng pos = new LatLng(lat, lon);
                Marker marker = map.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(stopName)
                        .icon(iconoBus));

                if (marker != null) {
                    markerStopIds.put(marker, stopId);
                }
            }

            map.setOnMarkerClickListener(marker -> {
                String stopId = markerStopIds.get(marker);
                String nombre = marker.getTitle();

                if (stopId == null) return false;

                List<String> lineas = lineasPorParada.get(stopId);
                String mensaje;
                if (lineas != null && !lineas.isEmpty()) {
                    mensaje = "Líneas: " + String.join(", ", lineas);
                } else {
                    mensaje = "No hay líneas disponibles para esta parada.";
                }

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 17));

                new AlertDialog.Builder(requireContext())
                        .setTitle("Parada: " + nombre)
                        .setMessage(mensaje)
                        .setPositiveButton("Cerrar", null)
                        .setNeutralButton("Ir", (dialog, which) -> {
                            String uri = "google.navigation:q=" + marker.getPosition().latitude + "," + marker.getPosition().longitude + "&mode=w";
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                            intent.setPackage("com.google.android.apps.maps");
                            if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                                startActivity(intent);
                            } else {
                                Toast.makeText(getContext(), "Google Maps no está instalado", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show();

                return true;
            });

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(CAMPUS_ALAVA, 14));

        } catch (Exception e) {
            Log.e("BUS_FRAGMENT_ERROR", "Error cargando mapa de bus", e);
            Toast.makeText(getContext(), "Error cargando mapa de bus", Toast.LENGTH_LONG).show();
        }
    }

    private void mostrarParadasMasCercanasAlCampus() {
        List<ParadaCercana> paradas = new ArrayList<>();

        try {
            InputStream inputStream = requireContext().getAssets().open("stops_gasteiz_con_id.geojson");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            inputStream.close();

            JSONObject geoJson = new JSONObject(builder.toString());
            JSONArray features = geoJson.getJSONArray("features");

            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");
                JSONArray coordinates = geometry.getJSONArray("coordinates");
                JSONObject props = feature.getJSONObject("properties");

                double lon = coordinates.getDouble(0);
                double lat = coordinates.getDouble(1);
                double distancia = calcularDistancia(CAMPUS_ALAVA.latitude, CAMPUS_ALAVA.longitude, lat, lon);

                String stopId = props.optString("stop_id", "desconocido");
                String stopName = props.optString("stop_name", "sin nombre");

                paradas.add(new ParadaCercana(stopId, stopName, distancia));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        paradas.sort(Comparator.comparingDouble(p -> p.distancia));
        List<ParadaCercana> top5 = paradas.subList(0, Math.min(5, paradas.size()));

        StringBuilder mensaje = new StringBuilder();
        for (ParadaCercana p : top5) {
            mensaje.append("📍 ").append(p.nombre)
                    .append(" (").append(p.stopId)
                    .append("): ").append((int) p.distancia).append(" m\n");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Paradas más cercanas al campus")
                .setMessage(mensaje.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radio Tierra
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private static class ParadaCercana {
        String stopId;
        String nombre;
        double distancia;

        ParadaCercana(String stopId, String nombre, double distancia) {
            this.stopId = stopId;
            this.nombre = nombre;
            this.distancia = distancia;
        }
    }
}
