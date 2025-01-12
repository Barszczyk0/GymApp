package com.example.frontapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RatingDetailActivity extends AppCompatActivity {

    private LinearLayout layoutRatings;
    private Button buttonMakeComment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating_detail);

        layoutRatings = findViewById(R.id.layoutRatings);
        buttonMakeComment = findViewById(R.id.buttonMakeComment);

        String token = getIntent().getStringExtra("TOKEN");
        String trainerId = getIntent().getStringExtra("TRAINER_ID");
        String csrfToken = getIntent().getStringExtra("X-CSRF-TOKEN");
        if (token != null && !token.isEmpty()) {
            if (trainerId != null && !trainerId.isEmpty()) {
                fetchTrainerRatings(token, trainerId);
            } else {
                Toast.makeText(this, "Invalid trainer ID", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Invalid token. Please log in again.", Toast.LENGTH_SHORT).show();
        }

        // Set up the button to navigate to MakeCommentActivity

        buttonMakeComment.setOnClickListener(v -> {
            Intent intent = new Intent(RatingDetailActivity.this, MakeCommentActivity.class);
            intent.putExtra("TRAINER_ID", trainerId);
            intent.putExtra("TOKEN", token);
            intent.putExtra("X-CSRF-TOKEN", csrfToken);
            startActivity(intent);
        });

    }

    private void fetchTrainerRatings(String token, String trainerId) {
        new Thread(() -> {
            try {
                // Build the URL
                String urlString = "http://10.0.2.2:5000/api/trainers/ratings/" + trainerId;
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

                    // Parse and display the JSON data
                    String jsonResponse = responseBuilder.toString();
                    runOnUiThread(() -> displayRatings(jsonResponse));
                } else {
                    Log.e("RatingDetailActivity", "Error: Response code " + responseCode);
                    runOnUiThread(() -> Toast.makeText(this, "Failed to fetch ratings", Toast.LENGTH_SHORT).show());
                }

                connection.disconnect();
            } catch (Exception e) {
                Log.e("RatingDetailActivity", "Exception: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "An error occurred", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void displayRatings(String jsonResponse) {
        try {
            // Parse the JSON array
            JSONArray ratingsArray = new JSONArray(jsonResponse);

            for (int i = 0; i < ratingsArray.length(); i++) {
                JSONObject ratingObject = ratingsArray.getJSONObject(i);

                String comment = ratingObject.getString("comment");
                int rating = ratingObject.getInt("rating");

                // Create a TextView for each rating
                TextView textView = new TextView(this);
                textView.setText("Rating: " + rating + "\nComment: " + comment);
                textView.setTextColor(Color.WHITE);
                textView.setTextSize(16);
                textView.setPadding(16, 16, 16, 16);

                // Add the TextView to the layout
                layoutRatings.addView(textView);
            }
        } catch (Exception e) {
            Log.e("RatingDetailActivity", "Error parsing JSON: " + e.getMessage(), e);
            Toast.makeText(this, "Error displaying ratings", Toast.LENGTH_SHORT).show();
        }
    }
}
