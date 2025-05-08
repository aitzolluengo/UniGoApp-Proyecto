package com.tzolas.unigoapp.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tzolas.unigoapp.R;

public class AjustesFragment extends Fragment {

    private TextView textoModo;
    private Button botonBorrar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ajustes, container, false);

        textoModo = view.findViewById(R.id.texto_modo);
        botonBorrar = view.findViewById(R.id.boton_borrar);

        SharedPreferences prefs = requireContext().getSharedPreferences("prefs_unigo", 0);
        String modo = prefs.getString("modo_transporte", "walking");
        textoModo.setText("Modo preferido: " + modo);

        botonBorrar.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            textoModo.setText("Modo preferido: (borrado)");
        });

        return view;
    }
}
