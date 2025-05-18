package com.tzolas.unigoapp.utils;

import static android.content.Context.MODE_PRIVATE;
import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.tzolas.unigoapp.R;
import com.tzolas.unigoapp.activities.LoginActivity;
import com.tzolas.unigoapp.activities.MainActivity;

public class SessionUtils {

    public static void logout(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("UniGoPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(context, context.getString(R.string.sesion_cerrada), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
