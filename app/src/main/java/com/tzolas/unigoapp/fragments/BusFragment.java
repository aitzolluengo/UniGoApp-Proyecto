// BusFragment.java actualizado con detección precisa de líneas al campus
package com.tzolas.unigoapp.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tzolas.unigoapp.R;

import java.io.*;
import java.util.*;

public class BusFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap map;
    private final LatLng CAMPUS_ALAVA = new LatLng(42.83988450929749, -2.669759213327361);
    private Map<Marker, String> markerStopIds = new HashMap<>();
    private Map<String, String> stopNames = new HashMap<>();
    private Map<String, LatLng> stopCoords = new HashMap<>();
    private Map<String, Set<String>> lineasPorParada = new HashMap<>();
    private FusedLocationProviderClient fusedLocationProviderClient;
    private final int PERMISSION_REQUEST_LOCATION = 100;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bus, container, false);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.bus_map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        FloatingActionButton fabUbicacion = rootView.findViewById(R.id.fab_desde_ubicacion);
        fabUbicacion.setOnClickListener(v -> checkPermisoYMostrarRutas());

        return rootView;
    }

    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        try {
            BitmapDescriptor iconoBus = BitmapDescriptorFactory.fromBitmap(
                    Bitmap.createScaledBitmap(
                            BitmapFactory.decodeResource(getResources(), R.drawable.ic_bus_marker),
                            96, 96, false
                    )
            );

            cargarStops();
            cargarLineasPorParada();

            for (Map.Entry<String, LatLng> entry : stopCoords.entrySet()) {
                String stopId = entry.getKey();
                LatLng pos = entry.getValue();
                String nombre = stopNames.getOrDefault(stopId, "Sin nombre");

                Marker marker = map.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(nombre)
                        .icon(iconoBus));

                if (marker != null) {
                    markerStopIds.put(marker, stopId);
                }
            }

            map.setOnMarkerClickListener(marker -> {
                String stopId = markerStopIds.get(marker);
                if (stopId == null) return false;

                Set<String> lineas = lineasPorParada.get(stopId);
                String mensaje = (lineas != null && !lineas.isEmpty())
                        ? "Líneas: " + String.join(", ", lineas)
                        : "No hay líneas disponibles para esta parada.";

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 17));

                new AlertDialog.Builder(requireContext())
                        .setTitle("Parada: " + marker.getTitle())
                        .setMessage(mensaje)
                        .setPositiveButton("Cerrar", null)
                        .setNeutralButton("Ir", (dialog, which) -> {
                            String uri = "google.navigation:q=" + marker.getPosition().latitude + "," + marker.getPosition().longitude + "&mode=w";
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                            intent.setPackage("com.google.android.apps.maps");
                            startActivity(intent);
                        })
                        .show();

                return true;
            });

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(CAMPUS_ALAVA, 14));

        } catch (Exception e) {
            Log.e("BUS_FRAGMENT_ERROR", "Error en el mapa", e);
            Toast.makeText(getContext(), "Error cargando mapa de bus", Toast.LENGTH_LONG).show();
        }
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }

    }

    private void cargarStops() throws IOException {
        InputStream inputStream = requireContext().getAssets().open("stops.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        reader.readLine();
        String line;
        while ((line = reader.readLine()) != null) {
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 7) {
                String stopId = tokens.get(0);
                String stopName = tokens.get(2);
                double lat = Double.parseDouble(tokens.get(5));
                double lon = Double.parseDouble(tokens.get(6));
                boolean dentroZona = lat >= 42.7900 && lat <= 42.8800 && lon >= -2.7400 && lon <= -2.6200;
                if (dentroZona) {
                    stopCoords.put(stopId, new LatLng(lat, lon));
                    stopNames.put(stopId, stopName);
                }
            }
        }
        reader.close();
        inputStream.close();
    }

    private void cargarLineasPorParada() {
        try {
            Map<String, String> tripToRoute = new HashMap<>();
            BufferedReader readerTrips = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("trips.txt")));
            readerTrips.readLine();
            String line;
            while ((line = readerTrips.readLine()) != null) {
                List<String> tokens = parseCsvLine(line);
                if (tokens.size() >= 3) {
                    tripToRoute.put(tokens.get(2), tokens.get(0));
                }
            }
            readerTrips.close();

            Map<String, String> routeNames = new HashMap<>();
            BufferedReader readerRoutes = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("routes.txt")));
            readerRoutes.readLine();
            while ((line = readerRoutes.readLine()) != null) {
                List<String> tokens = parseCsvLine(line);
                if (tokens.size() >= 3) {
                    routeNames.put(tokens.get(0), tokens.get(2));
                }
            }
            readerRoutes.close();

            BufferedReader readerStopTimes = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("stop_times.txt")));
            readerStopTimes.readLine();
            while ((line = readerStopTimes.readLine()) != null) {
                List<String> tokens = parseCsvLine(line);
                if (tokens.size() >= 4) {
                    String tripId = tokens.get(0);
                    String stopId = tokens.get(3);
                    String routeId = tripToRoute.getOrDefault(tripId, "");
                    String routeName = routeNames.getOrDefault(routeId, "");
                    if (!stopId.isEmpty() && !routeName.isEmpty()) {
                        lineasPorParada.putIfAbsent(stopId, new HashSet<>());
                        lineasPorParada.get(stopId).add(routeName);
                    }
                }
            }
            readerStopTimes.close();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error cargando líneas por parada", Toast.LENGTH_SHORT).show();
        }
    }

    private Set<String> obtenerLineasQueVanAlCampus() {
        Set<String> stopIdsCampus = new HashSet<>(Arrays.asList("57", "58"));
        Set<String> tripIds = new HashSet<>();
        Set<String> routeIds = new HashSet<>();
        Set<String> lineasCampus = new HashSet<>();

        try {
            BufferedReader readerStopTimes = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("stop_times.txt")));
            readerStopTimes.readLine();
            String line;
            while ((line = readerStopTimes.readLine()) != null) {
                List<String> tokens = parseCsvLine(line);
                if (tokens.size() >= 4 && stopIdsCampus.contains(tokens.get(3))) {
                    tripIds.add(tokens.get(0));
                }
            }
            readerStopTimes.close();

            BufferedReader readerTrips = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("trips.txt")));
            readerTrips.readLine();
            while ((line = readerTrips.readLine()) != null) {
                List<String> tokens = parseCsvLine(line);
                if (tokens.size() >= 3 && tripIds.contains(tokens.get(2))) {
                    routeIds.add(tokens.get(0));
                }
            }
            readerTrips.close();

            BufferedReader readerRoutes = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("routes.txt")));
            readerRoutes.readLine();
            while ((line = readerRoutes.readLine()) != null) {
                List<String> tokens = parseCsvLine(line);
                if (tokens.size() >= 3 && routeIds.contains(tokens.get(0))) {
                    lineasCampus.add(tokens.get(2));
                }
            }
            readerRoutes.close();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error detectando líneas del campus", Toast.LENGTH_SHORT).show();
        }

        return lineasCampus;
    }

    private void checkPermisoYMostrarRutas() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
        } else {
            obtenerUbicacionYMostrarRutas();
        }
    }

    private void obtenerUbicacionYMostrarRutas() {
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double latUser = location.getLatitude();
                double lonUser = location.getLongitude();
                double distanciaACampus = calcularDistancia(latUser, lonUser, CAMPUS_ALAVA.latitude, CAMPUS_ALAVA.longitude);

                List<ParadaCercana> todas = new ArrayList<>();
                for (String stopId : stopCoords.keySet()) {
                    LatLng pos = stopCoords.get(stopId);
                    double distancia = calcularDistancia(latUser, lonUser, pos.latitude, pos.longitude);
                    todas.add(new ParadaCercana(stopId, stopNames.getOrDefault(stopId, "Sin nombre"), distancia));
                }

                todas.sort(Comparator.comparingDouble(p -> p.distancia));
                List<ParadaCercana> seleccionadas = distanciaACampus < 10000 ? todas.subList(0, Math.min(5, todas.size()))
                        : Collections.singletonList(todas.get(0));

                mostrarRutasEnBottomSheet(seleccionadas);
            } else {
                Toast.makeText(getContext(), "No se pudo obtener ubicación", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void mostrarRutasEnBottomSheet(List<ParadaCercana> paradas) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.bottomsheet_paradas, null);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(view);

        TextView titulo = view.findViewById(R.id.titulo_sheet);
        titulo.setText("Rutas al campus desde tu ubicación");

        LinearLayout contenedor = view.findViewById(R.id.lista_paradas);
        contenedor.removeAllViews();

        Set<String> lineasCampus = obtenerLineasQueVanAlCampus();

        boolean hayResultados = false;

        for (ParadaCercana parada : paradas) {
            Set<String> lineas = lineasPorParada.getOrDefault(parada.stopId, new HashSet<>());
            Set<String> comunes = new HashSet<>(lineas);
            comunes.retainAll(lineasCampus);

            if (comunes.isEmpty()) continue;

            View paradaView = LayoutInflater.from(getContext()).inflate(R.layout.item_parada_ruta, contenedor, false);

            TextView texto = paradaView.findViewById(R.id.info_parada);
            texto.setText("📍 " + parada.nombre + " (" + (int) parada.distancia + " m)\n" +
                    "🚍 Líneas: " + String.join(", ", comunes));

            paradaView.findViewById(R.id.boton_ir).setOnClickListener(v -> {
                LatLng pos = stopCoords.get(parada.stopId);
                String uri = "google.navigation:q=" + pos.latitude + "," + pos.longitude + "&mode=w";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                startActivity(intent);
            });

            contenedor.addView(paradaView);
            hayResultados = true;
        }

        if (!hayResultados) {
            TextView fallback = new TextView(getContext());
            fallback.setText("❌ Desde tu ubicación no hay líneas directas al campus.");
            fallback.setPadding(16, 16, 16, 16);
            contenedor.addView(fallback);
        }

        dialog.show();
    }

    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean insideQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '\"') {
                insideQuotes = !insideQuotes;
            } else if (c == ',' && !insideQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens;
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
