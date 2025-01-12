package com.example.frontapp.Fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.frontapp.R;
import com.example.frontapp.RatingDetailActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RatingFragment extends Fragment {

    private ListView listViewRatings;
    private List<Trainer> trainers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rating, container, false);
        listViewRatings = view.findViewById(R.id.listViewRatings);

        String token = requireActivity().getIntent().getStringExtra("TOKEN");
        String csrfToken = requireActivity().getIntent().getStringExtra("X-CSRF-TOKEN");

        // Check if jwt token exists
        if (token != null && !token.isEmpty()) {
            fetchTrainerRatings(token);
        } else {
            Toast.makeText(requireContext(), "Invalid token. Please log in again.", Toast.LENGTH_SHORT).show();
        }

        listViewRatings.setOnItemClickListener((parent, view1, position, id) -> {
            Trainer selectedTrainer = trainers.get(position);

            // Start the new activity and pass trainer ID
            Intent intent = new Intent(requireContext(), RatingDetailActivity.class);
            intent.putExtra("TRAINER_ID", selectedTrainer.trainerId);
            intent.putExtra("TOKEN", token);
            intent.putExtra("X-CSRF-TOKEN", csrfToken);
            startActivity(intent);
        });



        return view;
    }

    private void fetchTrainerRatings(String token) {
        new Thread(() -> {
            try {
                // Build the URL
                URL url = new URL("http://10.0.2.2:5000/api/trainers/list");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Cookie", "access_token_cookie=" + token);

                // Handle the response
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    reader.close();

                    // Parse JSON response
                    parseJsonResponse(responseBuilder.toString());

                    // Update the UI
                    getActivity().runOnUiThread(() -> {
                        TrainerAdapter adapter = new TrainerAdapter();
                        listViewRatings.setAdapter(adapter);
                    });
                } else {
                    Log.e("RatingFragment", "Error: Response code " + responseCode);
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Failed to fetch trainers' ratings", Toast.LENGTH_SHORT).show());
                }

                connection.disconnect();
            } catch (Exception e) {
                Log.e("RatingFragment", "Exception: " + e.getMessage(), e);
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "An error occurred", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void parseJsonResponse(String jsonResponse) {
        try {
            JSONArray trainersArray = new JSONArray(jsonResponse);
            trainers.clear();
            for (int i = 0; i < trainersArray.length(); i++) {
                JSONObject trainerObject = trainersArray.getJSONObject(i);

                String name = trainerObject.getString("name");
                String surname = trainerObject.getString("surname");
                String trainerId = trainerObject.getString("trainer_id");

                // Get the number of ratings
                JSONArray ratingsArray = trainerObject.getJSONArray("ratings");
                int numRatings = ratingsArray.length();

                trainers.add(new Trainer(name, surname, numRatings, trainerId));
            }
        } catch (Exception e) {
            Log.e("RatingFragment", "Error parsing JSON: " + e.getMessage(), e);
        }
    }

    private class TrainerAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return trainers.size();
        }

        @Override
        public Object getItem(int position) {
            return trainers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.list_item_trainer, parent, false);
            }

            TextView textNameSurname = convertView.findViewById(R.id.textNameSurname);
            TextView textNumRatings = convertView.findViewById(R.id.textNumRatings);

            Trainer trainer = trainers.get(position);
            textNameSurname.setText(trainer.name + " " + trainer.surname);
            textNameSurname.setTextColor(Color.WHITE);
            textNumRatings.setText(String.valueOf(trainer.numRatings));
            textNumRatings.setTextColor(Color.rgb(247, 197, 72));

            return convertView;
        }
    }

    private static class Trainer {
        String name;
        String surname;
        int numRatings;
        String trainerId;

        Trainer(String name, String surname, int numRatings, String trainerId) {
            this.name = name;
            this.surname = surname;
            this.numRatings = numRatings;
            this.trainerId = trainerId;
        }
    }
}