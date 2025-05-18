package com.tzolas.unigoapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tzolas.unigoapp.R;
import com.tzolas.unigoapp.utils.SessionUtils;

public class ProfileActivity extends AppCompatActivity {

    private EditText editTextNombre, editTextEmail, editTextPassword;
    private ImageView imagePerfil;
    private Spinner spinnerTransporte;
    private Button btnGuardar, btnCerrarSesion;

    private static final int REQUEST_SELECT_IMAGE = 100;
    private Uri imagenSeleccionadaUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Solicitar permisos (Android 13+ y anteriores)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, 101);
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
            }
        }

        // Inicializar vistas
        imagePerfil = findViewById(R.id.imagePerfil);
        editTextNombre = findViewById(R.id.editTextNombre);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        spinnerTransporte = findViewById(R.id.spinnerTransporte);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        // Cargar preferencias
        SharedPreferences prefs = getSharedPreferences("user_profile", MODE_PRIVATE);
        String imagenUri = prefs.getString("imagen_perfil", null);
        if (imagenUri != null) {
            try {
                imagePerfil.setImageURI(Uri.parse(imagenUri));
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "No se pudo cargar la imagen de perfil", Toast.LENGTH_SHORT).show();
                prefs.edit().remove("imagen_perfil").apply();
            }
        }

        editTextNombre.setText(prefs.getString("nombre", ""));
        editTextEmail.setText(prefs.getString("email", ""));
        editTextPassword.setText(prefs.getString("password", ""));
        String transporte = prefs.getString("transporte", "walking");

        // Spinner traducido
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.transport_options)
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTransporte.setAdapter(adapter);
        spinnerTransporte.setSelection(adapter.getPosition(traducirModo(transporte)));

        // Imagen de perfil
        imagePerfil.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_SELECT_IMAGE);
        });

        // Guardar perfil
        btnGuardar.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();

            if (imagenSeleccionadaUri != null) {
                editor.putString("imagen_perfil", imagenSeleccionadaUri.toString());
            }

            editor.putString("nombre", editTextNombre.getText().toString());
            editor.putString("email", editTextEmail.getText().toString());
            editor.putString("password", editTextPassword.getText().toString());
            editor.putString("transporte", claveModoSeleccionado(spinnerTransporte.getSelectedItem().toString()));
            editor.apply();

            Toast.makeText(this, getString(R.string.btn_guardar) + " ✔️", Toast.LENGTH_SHORT).show();
            finish();
        });

        // Cerrar sesión
        btnCerrarSesion.setOnClickListener(v -> SessionUtils.logout(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SELECT_IMAGE && resultCode == RESULT_OK && data != null) {
            try {
                imagenSeleccionadaUri = data.getData();
                if (imagenSeleccionadaUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                    getContentResolver().takePersistableUriPermission(imagenSeleccionadaUri, takeFlags);
                }
                imagePerfil.setImageURI(imagenSeleccionadaUri);
            } catch (Exception e) {
                Toast.makeText(this, "Error al seleccionar imagen", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    // Convertir clave (en inglés) a valor traducido para mostrar en spinner
    private String traducirModo(String modo) {
        switch (modo.toLowerCase()) {
            case "walking":
                return getString(R.string.a_pie);
            case "bicycle":
                return getString(R.string.en_bicicleta);
            case "bus":
                return getString(R.string.en_autobus);
            default:
                return getString(R.string.a_pie);
        }
    }

    // Convertir valor traducido del spinner a clave interna para guardar
    private String claveModoSeleccionado(String seleccionado) {
        if (seleccionado.equals(getString(R.string.a_pie))) return "walking";
        if (seleccionado.equals(getString(R.string.en_bicicleta))) return "bicycle";
        if (seleccionado.equals(getString(R.string.en_autobus))) return "bus";
        return "walking";
    }
}
