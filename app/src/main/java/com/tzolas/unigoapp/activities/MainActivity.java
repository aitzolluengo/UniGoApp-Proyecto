// === MainActivity.java ===
package com.tzolas.unigoapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationView;
import com.tzolas.unigoapp.R;
import com.tzolas.unigoapp.fragments.BusFragment;
import com.tzolas.unigoapp.fragments.InicioFragment;
import com.tzolas.unigoapp.fragments.InfoFragment;
import com.tzolas.unigoapp.utils.FavoritosManager;
import com.tzolas.unigoapp.utils.GtfsUtils;
import com.tzolas.unigoapp.utils.SessionUtils;
import com.tzolas.unigoapp.workers.UbicacionWorker;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("prefs_unigo", MODE_PRIVATE);
        boolean oscuro = prefs.getBoolean("modo_oscuro", false);
        AppCompatDelegate.setDefaultNightMode(
                oscuro ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        String idioma = prefs.getString("idioma", "es");
        Locale locale = new Locale(idioma);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        actualizarHeaderUsuario();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.content_frame, new InicioFragment())
                        .commit();
                toolbar.setTitle("Inicio");

            } else if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, ProfileActivity.class));

            } else if (id == R.id.nav_ajustes) {
                startActivity(new Intent(this, SettingsActivity.class));

            } else if (id == R.id.nav_logout) {
                SessionUtils.logout(this);

            } else if (id == R.id.nav_info) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.content_frame, new InfoFragment())
                        .commit();
                toolbar.setTitle("Información");

            } else if (id == R.id.nav_bus) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.content_frame, new BusFragment())
                        .commit();
                toolbar.setTitle("Paradas de bus");

            } else if (id == R.id.nav_favoritos) {
                mostrarParadasFavoritas();
            }

            drawerLayout.closeDrawers();
            return true;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new InicioFragment())
                    .commit();
            toolbar.setTitle("Inicio");
        }

        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(UbicacionWorker.class, 15, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "ActualizarParadaCercana",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }

    private void actualizarHeaderUsuario() {
        View headerView = navigationView.getHeaderView(0);
        TextView nombreTextView = headerView.findViewById(R.id.nav_header_name);
        ImageView imageView = headerView.findViewById(R.id.nav_header_image);

        SharedPreferences prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        String nombre = prefs.getString("USER_NAME", "Usuario");
        String fotoBase64 = prefs.getString("USER_PHOTO", null);

        nombreTextView.setText(nombre);

        if (fotoBase64 != null) {
            try {
                byte[] decoded = Base64.decode(fotoBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                imageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            imageView.setImageResource(R.drawable.ic_user);
        }
    }

    private void mostrarParadasFavoritas() {
        Set<String> favoritos = FavoritosManager.obtenerFavoritos(this);
        if (favoritos.isEmpty()) {
            Toast.makeText(this, "No tienes paradas favoritas aún.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, LatLng> stopCoords = new HashMap<>();
        Map<String, String> stopNames = GtfsUtils.cargarStopNames(this, stopCoords);

        View view = LayoutInflater.from(this).inflate(R.layout.bottomsheet_paradas, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        TextView titulo = view.findViewById(R.id.titulo_sheet);
        titulo.setText("⭐ Tus paradas favoritas");

        LinearLayout contenedor = view.findViewById(R.id.lista_paradas);
        contenedor.removeAllViews();

        for (String stopId : favoritos) {
            if (!stopCoords.containsKey(stopId)) continue;

            String nombre = stopNames.getOrDefault(stopId, "Sin nombre");
            LatLng pos = stopCoords.get(stopId);

            View paradaView = LayoutInflater.from(this).inflate(R.layout.item_parada_ruta, contenedor, false);
            TextView texto = paradaView.findViewById(R.id.info_parada);
            texto.setText("📍 " + nombre);

            paradaView.findViewById(R.id.boton_ir).setOnClickListener(v -> {
                String uri = "google.navigation:q=" + pos.latitude + "," + pos.longitude + "&mode=w";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                startActivity(intent);
                dialog.dismiss();
            });

            contenedor.addView(paradaView);
        }

        dialog.show();
    }
} 