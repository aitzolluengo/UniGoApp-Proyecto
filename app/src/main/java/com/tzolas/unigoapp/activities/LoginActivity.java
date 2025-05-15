package com.tzolas.unigoapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tzolas.unigoapp.R;
import com.tzolas.unigoapp.api.ApiClient;
import com.tzolas.unigoapp.api.ApiInterface;
import com.tzolas.unigoapp.model.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button btnLogin, btnGoToRegister;
    private ApiInterface api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextEmail      = findViewById(R.id.editTextEmail);
        editTextPassword   = findViewById(R.id.editTextPassword);
        btnLogin           = findViewById(R.id.btnLogin);
        btnGoToRegister    = findViewById(R.id.btnGoToRegister);

        // Inicializamos Retrofit
        api = ApiClient.getClient().create(ApiInterface.class);

        btnLogin.setOnClickListener(v -> {
            String email    = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText("Iniciando...");

            Call<User> call = api.login(email, password);
            call.enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Iniciar sesión");

                    if (response.isSuccessful() && response.body() != null) {
                        User user = response.body();

                        // Guardamos sesión en SharedPreferences
                        SharedPreferences prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
                        prefs.edit()
                                .putBoolean("IS_LOGGED_IN", true)
                                .putInt("USER_ID", user.getId())
                                .putString("USER_NAME", user.getName())
                                .putString("USER_EMAIL", user.getEmail())
                                .putString("USER_TRANSPORT", user.getTransportMode())
                                .putBoolean("USER_DARK_MODE", user.isDarkMode())
                                .apply();

                        // Lanzamos MainActivity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();

                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Iniciar sesión");
                    Toast.makeText(LoginActivity.this,
                            "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        btnGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }
}
