package com.example.frontapp.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.frontapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ServicesFragment extends Fragment {

    private LinearLayout servicesContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_services, container, false);

        servicesContainer = view.findViewById(R.id.servicesContainer);
        String token = requireActivity().getIntent().getStringExtra("TOKEN");

        // Check if jwt token exists
        if (token != null && !token.isEmpty()) {
            fetchServices(token);
        } else {
            Toast.makeText(requireContext(), "Invalid token. Please log in again.", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void fetchServices(String token) {
        new Thread(() -> {
            try {
                // Build the URL
                URL url = new URL("http://10.0.2.2:5000/api/services/list");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Handle the response
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    reader.close();
                    parseAndDisplayServices(response.toString());
                } else {
                    showError("Failed to fetch services: " + responseCode);
                }

                connection.disconnect();
            } catch (Exception e) {
                showError("Error: " + e.getMessage());
            }
        }).start();
    }

    private void parseAndDisplayServices(String jsonResponse) {
        try {
            JSONArray servicesArray = new JSONArray(jsonResponse);

            getActivity().runOnUiThread(() -> {
                servicesContainer.removeAllViews();

                for (int i = 0; i < servicesArray.length(); i++) {
                    try {
                        JSONObject service = servicesArray.getJSONObject(i);

                        String name = service.getString("name");
                        String description = service.getString("description");
                        double price = service.getDouble("price");

                        // Create and display service entry
                        View serviceEntry = createServiceEntry(name, description, price);
                        servicesContainer.addView(serviceEntry);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (JSONException e) {
            showError("Invalid JSON response");
        }
    }

    private View createServiceEntry(String name, String description, double price) {
        View entry = LayoutInflater.from(getContext()).inflate(R.layout.list_item_service, servicesContainer, false);

        TextView nameTextView = entry.findViewById(R.id.serviceName);
        TextView descriptionTextView = entry.findViewById(R.id.serviceDescription);
        TextView priceTextView = entry.findViewById(R.id.servicePrice);

        nameTextView.setText(name);
        descriptionTextView.setText(description);
        priceTextView.setText(String.format("$%.2f", price));

        return entry;
    }

    private void showError(String message) {
        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
    }
}

