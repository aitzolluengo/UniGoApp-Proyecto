package com.tzolas.unigoapp.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.tzolas.unigoapp.R;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "prefs_unigo";
    private static final String KEY_MODO_OSCURO = "modo_oscuro";
    private static final String KEY_MODO_TRANSPORTE = "modo_transporte";
    private static final String KEY_IDIOMA = "idioma";

    private TextView textoModo;
    private Switch switchModoOscuro;
    private Button botonBorrar, botonIdioma;
    private TextView textoIdiomaActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        textoModo = findViewById(R.id.texto_modo);
        switchModoOscuro = findViewById(R.id.switch_modo_oscuro);
        botonBorrar = findViewById(R.id.boton_borrar);
        botonIdioma = findViewById(R.id.boton_idioma);
        textoIdiomaActual = findViewById(R.id.texto_idioma_actual);

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
            textoIdiomaActual.setText("Idioma actual: -");
        });

        mostrarIdiomaActual();

        botonIdioma.setOnClickListener(v -> mostrarDialogoIdiomas());
    }

    private void mostrarIdiomaActual() {
        Locale currentLocale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            currentLocale = getResources().getConfiguration().getLocales().get(0);
        } else {
            currentLocale = getResources().getConfiguration().locale;
        }
        textoIdiomaActual.setText("Idioma actual: " + currentLocale.getDisplayLanguage(currentLocale));
    }

    private void mostrarDialogoIdiomas() {
        final String[] idiomas = {"Español", "English", "Euskara"};
        final String[] codigos = {"es", "en", "eu"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecciona un idioma")
                .setItems(idiomas, (dialog, which) -> {
                    cambiarIdioma(codigos[which]);
                });
        builder.show();
    }

    public void cambiarIdioma(String codigoIdioma) {
        Locale locale = new Locale(codigoIdioma);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_IDIOMA, codigoIdioma).apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
