package com.example.frontapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.frontapp.Fragments.RatingFragment;
import com.example.frontapp.Fragments.ServicesFragment;
import com.example.frontapp.Fragments.TrainersListFragment;
import com.example.frontapp.Fragments.TrainingHistoryFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Override default ActionBar
        // Use ActionBar with added Logout button
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(R.layout.custom_main_menu);
            Button logoutButton = actionBar.getCustomView().findViewById(R.id.logout_button);
            logoutButton.setOnClickListener(v -> handleLogout());
        }

        // Load ServicesFragment by default
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new ServicesFragment())
                    .commit();
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // 4 different Submenus (fragments)
            if (itemId == R.id.menu_trainers_list) {
                selectedFragment = new TrainersListFragment();
            } else if (itemId == R.id.menu_training_history) {
                selectedFragment = new TrainingHistoryFragment();
            } else if (itemId == R.id.menu_rating) {
                selectedFragment = new RatingFragment();
            } else if (itemId == R.id.menu_services) {
                selectedFragment = new ServicesFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, selectedFragment)
                        .commit();
            }

            return true;
        });
    }

    private void handleLogout() {
        new Thread(() -> {
            try {
                // Build the URL
                String urlString = "http://10.0.2.2:5000/api/auth/logout";
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Cookie", "access_token_cookie=" + getIntent().getStringExtra("TOKEN"));
                connection.setRequestProperty("X-CSRF-TOKEN", getIntent().getStringExtra("X-CSRF-TOKEN"));;
                connection.setDoOutput(true);

                // Create JSON data
                JSONObject json = new JSONObject();

                // Send the JSON data
                OutputStream os = connection.getOutputStream();
                os.write(json.toString().getBytes());
                os.flush();
                os.close();

                // Handle the response
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Logout successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.putExtra("TOKEN", "");
                        intent.putExtra("X-CSRF-TOKEN", "");
                        startActivity(intent);
                        finish();
                    });
                } else {
                    Log.e("MainActivity", "Exception: " + responseCode);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Logout failed", Toast.LENGTH_SHORT).show());
                }

                connection.disconnect();
            } catch (Exception e) {
                Log.e("MainActivity", "Exception: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "An error occurred", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
