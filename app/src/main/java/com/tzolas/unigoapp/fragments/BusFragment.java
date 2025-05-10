package com.tzolas.unigoapp.fragments;

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
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonPointStyle;
import com.tzolas.unigoapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BusFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap map;
    private Map<String, List<String>> lineasPorParada = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bus, container, false);
        Log.d("BUS_FRAGMENT", "Iniciando carga de paradas y líneas...");
        cargarLineasDesdeJson(); // Cargar JSON con líneas por parada

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.bus_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

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
            GeoJsonLayer layer = new GeoJsonLayer(map, json);

            for (GeoJsonFeature feature : layer.getFeatures()) {
                if (feature.hasGeometry() && feature.getGeometry().getGeometryType().equals("Point")) {
                    GeoJsonPointStyle style = new GeoJsonPointStyle();
                    style.setAnchor(0.5f, 0.5f);
                    feature.setPointStyle(style);
                }
            }

            layer.addLayerToMap();

            layer.setOnFeatureClickListener(feature -> {
                String nombre = feature.getProperty("stop_name");
                String stopId = feature.getProperty("stop_id");

                Log.d("STOPID_DEBUG", "Stop ID pulsado: " + stopId);

                List<String> lineas = lineasPorParada.get(stopId);
                String mensaje;
                if (lineas != null && !lineas.isEmpty()) {
                    mensaje = "Parada: " + nombre + "\nLíneas: " + String.join(", ", lineas);
                } else {
                    mensaje = "Parada: " + nombre + "\n(No hay líneas disponibles)";
                }

                Toast.makeText(getContext(), mensaje, Toast.LENGTH_LONG).show();
            });

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(42.8467, -2.6723), 14));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al cargar paradas de bus", Toast.LENGTH_SHORT).show();
        }
    }
}
