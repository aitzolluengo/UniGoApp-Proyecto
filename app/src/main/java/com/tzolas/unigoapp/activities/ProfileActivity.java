package com.tzolas.unigoapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.tzolas.unigoapp.R;
import com.tzolas.unigoapp.utils.SessionUtils;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileActivity extends AppCompatActivity {

    private EditText editTextNombre, editTextEmail, editTextPassword;
    private ImageView imagePerfil;
    private Spinner spinnerTransporte;
    private Button btnGuardar, btnCerrarSesion;

    private static final int REQUEST_SELECT_IMAGE = 100;
    private Bitmap imagenBitmap;
    private String emailUsuario;
    private SharedPreferences prefs;

    private final String BASE_URL = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/xbadiola002/WEB/";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Permisos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 101);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
            }
        }

        imagePerfil = findViewById(R.id.imagePerfil);
        editTextNombre = findViewById(R.id.editTextNombre);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        spinnerTransporte = findViewById(R.id.spinnerTransporte);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        emailUsuario = getSharedPreferences("auth_prefs", MODE_PRIVATE)
                .getString("USER_EMAIL", "prueba2@gmail.com");

        prefs = getSharedPreferences("user_profile_" + emailUsuario, MODE_PRIVATE);

        String nombreImagen = prefs.getString("profile_pic_filename", null);
        if (nombreImagen != null && !nombreImagen.isEmpty()) {
            String urlImagen = BASE_URL + "uploads/" + nombreImagen;
            Glide.with(this).load(urlImagen).into(imagePerfil);
        } else {
            imagePerfil.setImageResource(R.drawable.ic_user);
        }

        editTextNombre.setText(prefs.getString("nombre", ""));
        editTextEmail.setText(emailUsuario);
        editTextPassword.setText(prefs.getString("password", ""));
        String transporte = prefs.getString("transporte", "walking");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.transport_options)
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTransporte.setAdapter(adapter);
        spinnerTransporte.setSelection(adapter.getPosition(traducirModo(transporte)));

        imagePerfil.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_SELECT_IMAGE);
        });

        btnGuardar.setOnClickListener(v -> {
            String nombre = editTextNombre.getText().toString();

            // Guardar en perfil local
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("nombre", nombre);
            editor.putString("email", emailUsuario);
            editor.putString("password", editTextPassword.getText().toString());
            editor.putString("transporte", claveModoSeleccionado(spinnerTransporte.getSelectedItem().toString()));
            editor.apply();

            // Guardar en auth_prefs para el Drawer
            SharedPreferences authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
            authPrefs.edit().putString("USER_NAME", editTextNombre.getText().toString()).apply();

            if (imagenBitmap != null) {
                subirImagenComoBase64(imagenBitmap);
            } else {
                Toast.makeText(this, "Datos guardados", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        btnCerrarSesion.setOnClickListener(v -> SessionUtils.logout(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SELECT_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                imagenBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                imagePerfil.setImageBitmap(imagenBitmap);
            } catch (IOException e) {
                Toast.makeText(this, "Error al seleccionar imagen", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    private void subirImagenComoBase64(Bitmap bitmap) {
        String base64 = convertirABase64(bitmap);
        String nombreImagen = System.currentTimeMillis() + "_" + (int)(Math.random() * 100000) + ".jpg";

        try {
            JSONObject json = new JSONObject();
            json.put("email", emailUsuario);
            json.put("imagen", base64);
            json.put("nombre_imagen", nombreImagen);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    BASE_URL + "upload_profile_base64.php",
                    json,
                    response -> {
                        try {
                            String nombreDevuelto = response.getString("filename");

                            // Guardar ruta en perfil local
                            prefs.edit().putString("profile_pic_filename", nombreDevuelto).apply();

                            // ✅ NUEVO: guardar imagen como base64 en auth_prefs para el Drawer
                            SharedPreferences authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
                            authPrefs.edit().putString("USER_PHOTO", base64).apply();

                            Toast.makeText(this, "Imagen subida", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        } catch (Exception e) {
                            Toast.makeText(this, "Respuesta sin imagen", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        String errorMsg = (error.networkResponse != null)
                                ? new String(error.networkResponse.data)
                                : "Error desconocido";
                        Toast.makeText(this, "Error al subir imagen: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
            );

            Volley.newRequestQueue(this).add(request);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generando JSON", Toast.LENGTH_SHORT).show();
        }
    }

    private String convertirABase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private String traducirModo(String modo) {
        switch (modo.toLowerCase()) {
            case "walking": return getString(R.string.a_pie);
            case "bicycle": return getString(R.string.en_bicicleta);
            case "bus":     return getString(R.string.en_autobus);
            default:        return getString(R.string.a_pie);
        }
    }

    private String claveModoSeleccionado(String seleccionado) {
        if (seleccionado.equals(getString(R.string.a_pie))) return "walking";
        if (seleccionado.equals(getString(R.string.en_bicicleta))) return "bicycle";
        if (seleccionado.equals(getString(R.string.en_autobus))) return "bus";
        return "walking";
    }
}
