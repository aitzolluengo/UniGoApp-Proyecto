package com.tzolas.unigoapp.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tzolas.unigoapp.R;
import com.tzolas.unigoapp.api.ApiClient;
import com.tzolas.unigoapp.api.ApiInterface;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword, editTextConfirmPassword;
    private Button btnRegister;
    private ApiInterface api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editTextEmail           = findViewById(R.id.editTextEmail);
        editTextPassword        = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        btnRegister             = findViewById(R.id.btnRegister);

        // Inicializa el cliente Retrofit
        api = ApiClient.getClient().create(ApiInterface.class);

        btnRegister.setOnClickListener(v -> {
            String email   = editTextEmail.getText().toString().trim();
            String pass    = editTextPassword.getText().toString();
            String confirm = editTextConfirmPassword.getText().toString();

            if (email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(confirm)) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            btnRegister.setEnabled(false);
            btnRegister.setText("Registrando...");

            // Llamada a tu API
            // Enviamos un nombre genérico y valores por defecto para transporte y modo oscuro
            Call<ServerResponse> call = api.register(
                    /* name: */        "Usuario",
                    /* email: */       email,
                    /* password: */    pass,
                    /* transportMode */"walking",
                    /* darkMode: */    false
            );

            call.enqueue(new Callback<ServerResponse>() {
                @Override
                public void onResponse(Call<ServerResponse> call, Response<ServerResponse> response) {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Registrar");

                    if (response.isSuccessful() && response.body() != null) {
                        ServerResponse resp = response.body();
                        if (resp.isSuccess()) {
                            Toast.makeText(RegisterActivity.this,
                                    resp.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    resp.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                "Error en el servidor",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }

                @Override
                public void onFailure(Call<ServerResponse> call, Throwable t) {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Registrar");
                    Toast.makeText(RegisterActivity.this,
                            "Fallo de red: " + t.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
        });
    }
}
