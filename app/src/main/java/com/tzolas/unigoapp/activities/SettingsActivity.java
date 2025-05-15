package com.tzolas.unigoapp.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.tzolas.unigoapp.R;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "prefs_unigo";
    private static final String KEY_MODO_OSCURO = "modo_oscuro";
    private static final String KEY_MODO_TRANSPORTE = "modo_transporte";

    private TextView textoModo;
    private Switch switchModoOscuro;
    private Button botonBorrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        textoModo = findViewById(R.id.texto_modo);
        switchModoOscuro = findViewById(R.id.switch_modo_oscuro);
        botonBorrar = findViewById(R.id.boton_borrar);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String modo = prefs.getString(KEY_MODO_TRANSPORTE, "walking");
        boolean oscuro = prefs.getBoolean(KEY_MODO_OSCURO, false);

        textoModo.setText("Modo preferido: " + modo);
        switchModoOscuro.setChecked(oscuro);

        switchModoOscuro.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_MODO_OSCURO, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        botonBorrar.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            textoModo.setText("Modo preferido: (borrado)");
            switchModoOscuro.setChecked(false);
        });
    }
}