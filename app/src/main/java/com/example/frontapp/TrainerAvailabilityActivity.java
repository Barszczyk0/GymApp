package com.example.frontapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TrainerAvailabilityActivity extends AppCompatActivity {

    private LinearLayout layoutAvailability;
    private String token;
    private String trainerId;
    private String csrfToken;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainer_availability);
        layoutAvailability = findViewById(R.id.layoutAvailability);

        this.token = getIntent().getStringExtra("TOKEN");
        this.trainerId = getIntent().getStringExtra("TRAINER_ID");
        this.csrfToken = getIntent().getStringExtra("X-CSRF-TOKEN");

        if (token != null && !token.isEmpty() && trainerId != null && !trainerId.isEmpty()) {
            fetchAvailability(token, trainerId);
        } else {
            Toast.makeText(this, "Invalid data. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
        }


    }

    private void fetchAvailability(String token, String trainerId) {
        new Thread(() -> {
            try {
                // Build the URL
                String urlString = "http://10.0.2.2:5000/api/reservations/availability/" + trainerId;
                URL url = new URL(urlString);
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

                    String jsonResponse = responseBuilder.toString();
                    runOnUiThread(() -> displayAvailability(jsonResponse));
                } else {
                    Log.e("AvailabilityActivity", "Error: Response code " + responseCode);
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e("AvailabilityActivity", "Exception: " + e.getMessage(), e);
            }
        }).start();
    }

    private void displayAvailability(String jsonResponse) {
        try {
            JSONObject responseObject = new JSONObject(jsonResponse);
            JSONArray availabilityArray = responseObject.getJSONArray("availability");

            layoutAvailability.removeAllViews();

            for (int i = 0; i < availabilityArray.length(); i++) {
                JSONObject availabilityObject = availabilityArray.getJSONObject(i);

                String start = availabilityObject.getString("start");
                String end = availabilityObject.getString("end");
                String reservationId = availabilityObject.optString("reservation_id", null);
                boolean isBooked = availabilityObject.getInt("is_booked") == 1;

                String formattedDate = formatAvailabilityDate(start, end);

                LinearLayout entryLayout = new LinearLayout(this);
                entryLayout.setOrientation(LinearLayout.HORIZONTAL);

                // Date Text
                TextView dateTextView = new TextView(this);
                dateTextView.setText(formattedDate);
                dateTextView.setTextSize(16);
                dateTextView.setPadding(16, 16, 16, 16);
                dateTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                dateTextView.setTextColor(Color.rgb(247, 197, 72));
                dateTextView.setBackgroundColor(Color.rgb(27, 32, 33));
                entryLayout.addView(dateTextView);

                // Button
                Button actionButton = new Button(this);
                actionButton.setMinWidth(450);
                actionButton.setText(isBooked ? "Not available" : "Reserve");   // Not available means booked by other users
                actionButton.setEnabled(!isBooked && reservationId != null);
                actionButton.setBackgroundColor(isBooked ? Color.rgb(27, 32, 33) : Color.rgb(48, 52, 63));

                // If booked==1 and reservationId!=null then reservation was made by user making a request to backend
                // reservationId is needed to cancel reservation
                if (!reservationId.equals("null")) {
                    actionButton.setText("Cancel reservation");
                    actionButton.setBackgroundColor(Color.rgb(214, 40, 40));
                    actionButton.setEnabled(true);
                    actionButton.setOnClickListener(v -> handleCancelClick(actionButton, reservationId));
                } else if (!isBooked && reservationId.equals("null")) {
                    actionButton.setOnClickListener(v -> handleReserveClick(actionButton, start));
                }
                entryLayout.addView(actionButton);

                layoutAvailability.addView(entryLayout);
            }
        } catch (Exception e) {
            Log.e("AvailabilityActivity", "Error parsing JSON: " + e.getMessage(), e);
        }
    }

    private String formatAvailabilityDate(String start, String end) {
        try {
            String[] startParts = start.split(" ");
            String[] endParts = end.split(" ");

            String startHour = startParts[1].substring(0, 5);
            String endHour = endParts[1].substring(0, 5);

            String[] dateParts = startParts[0].split("-");
            String day = dateParts[2];
            String month = dateParts[1];
            String year = dateParts[0];

            return startHour + "-" + endHour + " " + day + "." + month + "." + year;
        } catch (Exception e) {
            return start + " - " + end;
        }
    }

    private void handleReserveClick(Button button, String startDate) {
        // First click to mark an option, second click to confirm reservation
        if (button.getText().toString().equals("Reserve")) {
            button.setText("Confirm");
        } else {
            new Thread(() -> {
                try {
                    String urlString = "http://10.0.2.2:5000/api/reservations/book";
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Cookie", "access_token_cookie=" + token);
                    connection.setRequestProperty("X-CSRF-TOKEN", csrfToken);;
                    connection.setDoOutput(true);

                    JSONObject requestBody = new JSONObject();
                    requestBody.put("trainer_id", trainerId);
                    requestBody.put("date", startDate);

                    OutputStream os = connection.getOutputStream();
                    os.write(requestBody.toString().getBytes());
                    os.close();

                    // Handle the response
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        runOnUiThread(() -> {
                            button.setText("Cancel reservation");
                            button.setBackgroundColor(Color.rgb(214, 40, 40));
                            button.setEnabled(true);
                        });
                        finish();
                    } else {
                        Log.e("ReserveActivity", "Error: Response code " + responseCode);
                    }
                } catch (Exception e) {
                    Log.e("ReserveActivity", "Exception: " + e.getMessage(), e);
                }
            }).start();
        }
    }

    private void handleCancelClick(Button button, String reservationId) {
        // First click to mark an option, second click to cancel reservation
        if (button.getText().toString().equals("Cancel reservation")) {
            button.setText("Confirm");
        } else {
            new Thread(() -> {
                try {
                    String urlString = "http://10.0.2.2:5000/api/reservations/cancel/" + reservationId;
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("DELETE");
                    connection.setRequestProperty("Cookie", "access_token_cookie=" + token);
                    connection.setRequestProperty("X-CSRF-TOKEN", csrfToken);;

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        runOnUiThread(() -> {
                            button.setText("Reserve");
                            button.setBackgroundColor(Color.rgb(48, 52, 63));
                            button.setEnabled(true);
                        });
                        finish();
                    } else {
                        Log.e("CancelActivity", "Error: Response code " + responseCode);
                    }
                } catch (Exception e) {
                    Log.e("CancelActivity", "Exception: " + e.getMessage(), e);
                }
            }).start();
        }
    }
}
