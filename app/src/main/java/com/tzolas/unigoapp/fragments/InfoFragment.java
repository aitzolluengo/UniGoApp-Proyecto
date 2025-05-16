package com.tzolas.unigoapp.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.tzolas.unigoapp.R;

public class InfoFragment extends Fragment {

    public InfoFragment() {
        // Constructor público vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnBici = view.findViewById(R.id.btn_bici_info);
        Button btnBus = view.findViewById(R.id.btn_bus_info);

        btnBici.setOnClickListener(v -> {
            String url = "https://www.vitoria-gasteiz.org/wb021/was/contenidoAction.do?uid=app_j34_0080&idioma=es";
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        btnBus.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new BusFragment())
                    .commit();

            requireActivity().setTitle("Paradas de bus");
        });

    }
}
