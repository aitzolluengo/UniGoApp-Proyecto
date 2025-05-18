package com.tzolas.unigoapp.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tzolas.unigoapp.R;
import com.tzolas.unigoapp.controller.ParadaController;
import com.tzolas.unigoapp.model.ParadaCercana;
import com.tzolas.unigoapp.utils.FavoritosManager;
import com.tzolas.unigoapp.utils.GtfsUtils;
import com.tzolas.unigoapp.utils.HorarioManager;
import com.tzolas.unigoapp.utils.ParadaUtils;
import androidx.appcompat.widget.SearchView;


import java.util.*;

public class BusFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap map;
    private final LatLng CAMPUS_ALAVA = new LatLng(42.83988450929749, -2.669759213327361);
    private Map<Marker, String> markerStopIds = new HashMap<>();
    private Map<String, String> stopNames = new HashMap<>();
    private Map<String, LatLng> stopCoords = new HashMap<>();
    private Map<String, Set<String>> lineasPorParada = new HashMap<>();
    private Set<String> lineasCampus = new HashSet<>();
    private FusedLocationProviderClient fusedLocationProviderClient;
    private SearchView searchView;
    private Map<String, Marker> nombreToMarker = new HashMap<>();

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

        MaterialButton botonBuscar = rootView.findViewById(R.id.boton_paradas_cercanas);
        botonBuscar.setOnClickListener(v -> mostrarParadasCercanas());


        return rootView;
    }

    @SuppressLint({"PotentialBehaviorOverride", "SetTextI18n"})
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        boolean darkMode = (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES);

        if (darkMode) {
            try {
                boolean success = googleMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_dark)
                );
                if (!success) {
                    Log.e("BusFragment", "Error al aplicar estilo de mapa oscuro");
                }
            } catch (Resources.NotFoundException e) {
                Log.e("BusFragment", "No se encontró el archivo de estilo", e);
            }
        }
        try {
            BitmapDescriptor iconoBus = BitmapDescriptorFactory.fromBitmap(
                    Bitmap.createScaledBitmap(
                            BitmapFactory.decodeResource(getResources(), R.drawable.ic_bus_marker),
                            96, 96, false
                    )
            );

            stopNames = GtfsUtils.cargarStopNames(requireContext(), stopCoords);
            lineasPorParada = GtfsUtils.cargarLineasPorParada(requireContext());
            lineasCampus = GtfsUtils.obtenerLineasAlCampus(requireContext());

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
                    nombreToMarker.put(nombre.toLowerCase(), marker); // importante: todo en minúsculas
                }
            }

            map.setOnMarkerClickListener(marker -> {
                String stopId = markerStopIds.get(marker);
                if (stopId == null) return false;

                Set<String> lineas = lineasPorParada.getOrDefault(stopId, new HashSet<>());
                List<String> horas = HorarioManager.obtenerProximasHorasConLinea(requireContext(), stopId);
                SharedPreferences prefs = requireContext().getSharedPreferences("UniGoPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("ultimaParadaNombre", marker.getTitle());
                editor.putString("ultimaParadaLineas", String.join(", ", lineas));
                editor.putString("ultimaParadaHorarios", horas.isEmpty() ?
                        "🕐 No hay buses disponibles hoy." :
                        "🕐 Próximos buses:\n" + String.join("\n", horas.subList(0, Math.min(3, horas.size())))
                );
                editor.apply();


                View view = LayoutInflater.from(getContext()).inflate(R.layout.bottomsheet_lineas_paradas, null);
                BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
                dialog.setContentView(view);

                TextView titulo = view.findViewById(R.id.titulo_parada);
                titulo.setText("📍 " + marker.getTitle());

                LinearLayout contenedor = view.findViewById(R.id.contenedor_lineas);
                contenedor.removeAllViews();

                if (lineas.isEmpty()) {
                    View paradaView = LayoutInflater.from(getContext()).inflate(R.layout.item_parada_ruta, contenedor, false);
                    TextView texto = paradaView.findViewById(R.id.info_parada);
                    texto.setText("No hay líneas disponibles para esta parada.");
                    paradaView.findViewById(R.id.boton_ir).setOnClickListener(v -> {
                        LatLng pos = marker.getPosition();
                        String uri = "google.navigation:q=" + pos.latitude + "," + pos.longitude + "&mode=w";
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        intent.setPackage("com.google.android.apps.maps");
                        startActivity(intent);
                    });
                    contenedor.addView(paradaView);
                } else {
                    boolean hayCoincidencias = false;
                    StringBuilder sb = new StringBuilder();
                    for (String linea : lineas) {
                        if (lineasCampus.contains(linea)) {
                            sb.append("✅ ").append(linea).append(" (va al campus)\n");
                            hayCoincidencias = true;
                        } else {
                            sb.append("– ").append(linea).append("\n");
                        }
                    }

                    if (!hayCoincidencias) {
                        sb.append("\n❌ Ninguna línea desde esta parada va al campus.");
                    }

                    View paradaView = LayoutInflater.from(getContext()).inflate(R.layout.item_parada_ruta, contenedor, false);
                    TextView texto = paradaView.findViewById(R.id.info_parada);
                    texto.setText(sb.toString().trim());
                    paradaView.findViewById(R.id.boton_ir).setOnClickListener(v -> {
                        LatLng pos = marker.getPosition();
                        String uri = "google.navigation:q=" + pos.latitude + "," + pos.longitude + "&mode=w";
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        intent.setPackage("com.google.android.apps.maps");
                        startActivity(intent);
                    });

                    contenedor.addView(paradaView);
                }
                Button botonFavorito = view.findViewById(R.id.boton_favorito);
                if (FavoritosManager.esFavorita(requireContext(), stopId)) {
                    botonFavorito.setText("⭐ Quitar de favoritos");
                } else {
                    botonFavorito.setText("☆ Añadir a favoritos");
                }

                botonFavorito.setOnClickListener(v -> {
                    FavoritosManager.toggleFavorito(requireContext(), stopId);
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Actualizado favoritos", Toast.LENGTH_SHORT).show();
                });
                TextView horariosTextView = new TextView(getContext());
                horariosTextView.setPadding(16, 16, 16, 16);

                if (horas.isEmpty()) {
                    horariosTextView.setText("🕐 No hay buses disponibles hoy.");
                } else {
                    horariosTextView.setText("🕐 Próximos buses:\n" + String.join("\n", horas.subList(0, Math.min(3, horas.size()))));
                }

                contenedor.addView(horariosTextView);

                dialog.show();

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
                double distanciaACampus = ParadaUtils.calcularDistancia(latUser, lonUser, CAMPUS_ALAVA.latitude, CAMPUS_ALAVA.longitude);

                List<ParadaCercana> todas = new ArrayList<>();
                for (String stopId : stopCoords.keySet()) {
                    LatLng pos = stopCoords.get(stopId);
                    double distancia = ParadaUtils.calcularDistancia(latUser, lonUser, pos.latitude, pos.longitude);
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
    private void mostrarParadasCercanas() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double latUser = location.getLatitude();
                double lonUser = location.getLongitude();

                List<ParadaCercana> paradas = ParadaController.cargarParadasCercanas(
                        requireContext(), latUser, lonUser
                );


                mostrarListaParadas(paradas);
            } else {
                Toast.makeText(getContext(), "No se pudo obtener ubicación", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @SuppressLint("SetTextI18n")
    private void mostrarListaParadas(List<ParadaCercana> paradas) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.bottomsheet_paradas, null);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(view);

        TextView titulo = view.findViewById(R.id.titulo_sheet);
        titulo.setText("Paradas cercanas a tu ubicación");

        LinearLayout contenedor = view.findViewById(R.id.lista_paradas);
        contenedor.removeAllViews();

        for (ParadaCercana parada : paradas.subList(0, Math.min(10, paradas.size()))) {
            View paradaView = LayoutInflater.from(getContext()).inflate(R.layout.item_parada_ruta, contenedor, false);
            TextView texto = paradaView.findViewById(R.id.info_parada);
            texto.setText("📍 " + parada.nombre + " (" + (int) parada.distancia + " m)");

            paradaView.findViewById(R.id.boton_ir).setOnClickListener(v -> {
                LatLng pos = stopCoords.get(parada.stopId);
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
                dialog.dismiss();
            });

            contenedor.addView(paradaView);
        }

        dialog.show();
    }

}