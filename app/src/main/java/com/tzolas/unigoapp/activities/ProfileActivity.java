package com.tzolas.unigoapp.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tzolas.unigoapp.R;

public class ProfileActivity extends AppCompatActivity {

    private EditText editTextNombre, editTextEmail;
    private Spinner spinnerTransporte;
    private Button btnGuardar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        editTextNombre = findViewById(R.id.editTextNombre);
        editTextEmail = findViewById(R.id.editTextEmail);
        spinnerTransporte = findViewById(R.id.spinnerTransporte);
        btnGuardar = findViewById(R.id.btnGuardar);

        // Opciones de transporte
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"A pie", "Bicicleta", "Autobús"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTransporte.setAdapter(adapter);

        // Cargar perfil existente
        SharedPreferences prefs = getSharedPreferences("user_profile", MODE_PRIVATE);
        editTextNombre.setText(prefs.getString("nombre", ""));
        editTextEmail.setText(prefs.getString("email", ""));
        String transporte = prefs.getString("transporte", "A pie");
        int pos = adapter.getPosition(transporte);
        spinnerTransporte.setSelection(pos);

        btnGuardar.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("nombre", editTextNombre.getText().toString());
            editor.putString("email", editTextEmail.getText().toString());
            editor.putString("transporte", spinnerTransporte.getSelectedItem().toString());
            editor.apply();
            Toast.makeText(this, "Perfil guardado correctamente", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
