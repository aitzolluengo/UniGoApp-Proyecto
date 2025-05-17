package com.tzolas.unigoapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tzolas.unigoapp.R;


public class ProfileActivity extends AppCompatActivity {

    private EditText editTextNombre, editTextEmail;
    private EditText editTextPassword;
    private ImageView imagePerfil;

    private static final int REQUEST_SELECT_IMAGE = 100;
    private Uri imagenSeleccionadaUri;

    private Spinner spinnerTransporte;
    private Button btnGuardar;

    private Button btnCerrarSesion;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        imagePerfil = findViewById(R.id.imagePerfil);

        imagePerfil.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_SELECT_IMAGE);
        });

        editTextNombre = findViewById(R.id.editTextNombre);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        spinnerTransporte = findViewById(R.id.spinnerTransporte);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCerrarSesion=findViewById(R.id.btnCerrarSesion);
        btnCerrarSesion.setOnClickListener(v -> {
            SharedPreferences.Editor editor = getSharedPreferences("user_profile", MODE_PRIVATE).edit();
            editor.clear(); // Borra todos los datos del perfil
            editor.apply();

            Toast.makeText(this, getString(R.string.sesion_cerrada), Toast.LENGTH_SHORT).show();

            // Volver a MainActivity o pantalla de inicio
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Opciones de transporte
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"A pie", "Bicicleta", "Autobús"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTransporte.setAdapter(adapter);

        // Cargar perfil existente
        SharedPreferences prefs = getSharedPreferences("user_profile", MODE_PRIVATE);
        String imagenUri = prefs.getString("imagen_perfil", null);
        if (imagenUri != null) {
            imagePerfil.setImageURI(Uri.parse(imagenUri));
        }
        editTextNombre.setText(prefs.getString("nombre", ""));
        editTextEmail.setText(prefs.getString("email", ""));
        editTextPassword.setText(prefs.getString("password", ""));
        String transporte = prefs.getString("transporte", "A pie");
        int pos = adapter.getPosition(transporte);
        spinnerTransporte.setSelection(pos);

        btnGuardar.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();

            if (imagenSeleccionadaUri != null) {
                editor.putString("imagen_perfil", imagenSeleccionadaUri.toString());
            }

            editor.putString("nombre", editTextNombre.getText().toString());
            editor.putString("email", editTextEmail.getText().toString());
            editor.putString("password", editTextPassword.getText().toString());
            editor.putString("transporte", spinnerTransporte.getSelectedItem().toString());
            editor.apply();

            Toast.makeText(this, "Perfil guardado correctamente", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SELECT_IMAGE && resultCode == RESULT_OK && data != null) {
            imagenSeleccionadaUri = data.getData();
            imagePerfil.setImageURI(imagenSeleccionadaUri);
        }
    }

}
