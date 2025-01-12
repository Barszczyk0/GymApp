package com.example.frontapp.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.frontapp.R;
import com.example.frontapp.TrainerAvailabilityActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TrainersListFragment extends Fragment {

    private ListView listViewTrainers;
    private ArrayAdapter<String> adapter;
    private final List<Trainer> trainerList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trainers_list, container, false);

        listViewTrainers = view.findViewById(R.id.listViewTrainers);
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1);
        listViewTrainers.setAdapter(adapter);

        String csrfToken = requireActivity().getIntent().getStringExtra("X-CSRF-TOKEN");
        String token = requireActivity().getIntent().getStringExtra("TOKEN");

        // Check if jwt token and csrftoken exists
        if (token != null && !token.isEmpty()) {
            fetchTrainersList(token);
        } else {
            Toast.makeText(getContext(), "Invalid token. Please log in again.", Toast.LENGTH_SHORT).show();
        }

        listViewTrainers.setOnItemClickListener((parent, v, position, id) -> {
            if (position < 0 || position >= trainerList.size()) {
                Toast.makeText(getContext(), "Invalid trainer selected.", Toast.LENGTH_SHORT).show();
                return;
            }

            Trainer selectedTrainer = trainerList.get(position);

            Intent intent = new Intent(getActivity(), TrainerAvailabilityActivity.class);
            intent.putExtra("TOKEN", token);
            intent.putExtra("TRAINER_ID", selectedTrainer.getTrainerId());
            intent.putExtra("X-CSRF-TOKEN", csrfToken);
            startActivity(intent);
        });

        return view;
    }

    private void fetchTrainersList(String token) {
        new Thread(() -> {
            try {
                // Build the URL
                String urlString = "http://10.0.2.2:5000/api/trainers/list";
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Cookie", "access_token_cookie=" + token);

                // Handle the response
                int responseCode = connection.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        responseCode >= 200 && responseCode < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream()
                ));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                reader.close();

                String jsonResponse = responseBuilder.toString();

                if (responseCode >= 200 && responseCode < 300) {
                    parseTrainersList(jsonResponse);
                } else {
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Failed to fetch trainers list", Toast.LENGTH_SHORT).show());
                }

                connection.disconnect();
            } catch (Exception e) {
                Log.e("TrainersListFragment", "Exception: " + e.getMessage(), e);
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "An error occurred", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void parseTrainersList(String jsonResponse) {
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);

            List<Trainer> tempTrainerList = new ArrayList<>();
            List<String> tempAdapterData = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject trainerObject = jsonArray.getJSONObject(i);

                String name = trainerObject.getString("name");
                String surname = trainerObject.getString("surname");
                String trainerId = trainerObject.getString("trainer_id");

                Trainer trainer = new Trainer(name, surname, trainerId);
                tempTrainerList.add(trainer);
                tempAdapterData.add(name + " " + surname);
            }

            requireActivity().runOnUiThread(() -> {
                trainerList.clear();
                trainerList.addAll(tempTrainerList);
                adapter.clear();
                adapter.addAll(tempAdapterData);
                adapter.notifyDataSetChanged();
            });
        } catch (Exception e) {
            Log.e("TrainersListFragment", "Error parsing JSON: " + e.getMessage(), e);
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Error displaying trainers", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private static class Trainer {
        private final String name;
        private final String surname;
        private final String trainerId;

        public Trainer(String name, String surname, String trainerId) {
            this.name = name;
            this.surname = surname;
            this.trainerId = trainerId;
        }

        public String getTrainerId() {
            return trainerId;
        }
    }
}
