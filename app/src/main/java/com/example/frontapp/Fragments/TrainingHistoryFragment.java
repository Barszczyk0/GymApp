package com.example.frontapp.Fragments;

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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrainingHistoryFragment extends Fragment {

    private ListView listViewTrainingHistory;
    private List<TrainingHistory> trainingHistoryList = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_training_history, container, false);

        listViewTrainingHistory = view.findViewById(R.id.listViewTrainingHistory);

        String token = requireActivity().getIntent().getStringExtra("TOKEN");
        if (token != null && !token.isEmpty()) {
            fetchTrainingHistory(token);
        } else {
            Toast.makeText(requireContext(), "Invalid token. Please log in again.", Toast.LENGTH_SHORT).show();
        }
        return view;
    }

    private void fetchTrainingHistory(String token) {
        new Thread(() -> {
            try {
                URL url = new URL("http://10.0.2.2:5000/api/training-history/list");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Cookie", "access_token_cookie=" + token);

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
                        sortTrainingHistoryByDate();
                        TrainingHistoryAdapter adapter = new TrainingHistoryAdapter();
                        listViewTrainingHistory.setAdapter(adapter);
                    });
                } else {
                    Log.e("TrainingHistoryFragment", "Error: Response code " + responseCode);
                    Log.e("TrainingHistoryFragment", "Error: Response code " + connection.getInputStream());

                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Failed to fetch training history", Toast.LENGTH_SHORT).show());
                }

                connection.disconnect();
            } catch (Exception e) {
                Log.e("TrainingHistoryFragment", "Exception: " + e.getMessage(), e);
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "An error occurred", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void parseJsonResponse(String jsonResponse) {
        try {
            JSONArray trainingHistoryArray = new JSONArray(jsonResponse);
            trainingHistoryList.clear();
            for (int i = 0; i < trainingHistoryArray.length(); i++) {
                JSONObject trainingObject = trainingHistoryArray.getJSONObject(i);

                String dateString = trainingObject.getString("date");
                String details = trainingObject.getString("details");

                Date date = dateFormat.parse(dateString);
                trainingHistoryList.add(new TrainingHistory(date, details));
            }
        } catch (Exception e) {
            Log.e("TrainingHistoryFragment", "Error parsing JSON: " + e.getMessage(), e);
        }
    }

    private void sortTrainingHistoryByDate() {
        Collections.sort(trainingHistoryList, new Comparator<TrainingHistory>() {
            @Override
            public int compare(TrainingHistory o1, TrainingHistory o2) {
                return o2.date.compareTo(o1.date); // Sort by date in descending order
            }
        });
    }

    private class TrainingHistoryAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return trainingHistoryList.size();
        }

        @Override
        public Object getItem(int position) {
            return trainingHistoryList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.list_item_training_history, parent, false);
            }

            TextView textDate = convertView.findViewById(R.id.textDate);
            TextView textDetails = convertView.findViewById(R.id.textDetails);

            TrainingHistory trainingHistory = trainingHistoryList.get(position);
            textDate.setText(dateFormat.format(trainingHistory.date));
            textDetails.setText(trainingHistory.details);

            return convertView;
        }
    }

    private static class TrainingHistory {
        Date date;
        String details;

        TrainingHistory(Date date, String details) {
            this.date = date;
            this.details = details;
        }
    }
}
