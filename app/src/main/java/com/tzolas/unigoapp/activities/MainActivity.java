package com.tzolas.unigoapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.tzolas.unigoapp.R;
import com.tzolas.unigoapp.fragments.BusFragment;
import com.tzolas.unigoapp.fragments.InicioFragment;
import com.tzolas.unigoapp.fragments.InfoFragment;


public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SharedPreferences prefs = getSharedPreferences("prefs_unigo", MODE_PRIVATE);
        boolean oscuro = prefs.getBoolean("modo_oscuro", false); // ← por defecto: false (modo claro)
        AppCompatDelegate.setDefaultNightMode(
                oscuro ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

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
                showMessage("Perfil");
                toolbar.setTitle("Perfil");

            }else if (id == R.id.nav_ajustes) {
                    startActivity(new Intent(this, SettingsActivity.class));

            } else if (id == R.id.nav_logout) {
                showMessage("Sesión cerrada");
                toolbar.setTitle("UniGo App");
            } else if (id == R.id.nav_info) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.content_frame, new InfoFragment())
                        .commit();
                toolbar.setTitle("Información");
            }else if (id == R.id.nav_bus) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.content_frame, new BusFragment())
                        .commit();
                toolbar.setTitle("Paradas de bus");
            }



            drawerLayout.closeDrawers();
            return true;
        });

        // Mostrar InicioFragment por defecto al arrancar
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new InicioFragment())
                    .commit();
            toolbar.setTitle("Inicio");
        }
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
