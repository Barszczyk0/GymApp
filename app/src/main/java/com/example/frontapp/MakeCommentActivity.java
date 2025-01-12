package com.example.frontapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MakeCommentActivity extends AppCompatActivity {

    private EditText editTextComment;
    private RatingBar ratingBar;
    private Button buttonSubmit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_comment);

        // Get trainer ID and token from intent
        String trainerId = getIntent().getStringExtra("TRAINER_ID");
        String token = getIntent().getStringExtra("TOKEN");
        String csrfToken = getIntent().getStringExtra("X-CSRF-TOKEN");


        if (trainerId == null || trainerId.isEmpty() || token == null || token.isEmpty()) {
            Toast.makeText(this, "Invalid data. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        editTextComment = findViewById(R.id.editTextComment);
        ratingBar = findViewById(R.id.ratingBar);
        buttonSubmit = findViewById(R.id.buttonSubmit);

        // Set up button click listener
        buttonSubmit.setOnClickListener(v -> {
            String comment = editTextComment.getText().toString();
            int rating = (int) ratingBar.getRating();

            if (comment.isEmpty() || rating < 0 || rating > 10) {
                Toast.makeText(MakeCommentActivity.this, "Please provide a valid comment and rating (0-10)", Toast.LENGTH_SHORT).show();
                return;
            }

            submitComment(token, csrfToken, trainerId, rating, comment);
        });
    }

    private void submitComment(String token, String csrfToken, String trainerId, int rating, String comment) {
        new Thread(() -> {
            try {
                // Build the URL
                String urlString = "http://10.0.2.2:5000/api/trainers/ratings/" + trainerId;
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Cookie", "access_token_cookie=" + token);
                connection.setRequestProperty("X-CSRF-TOKEN", csrfToken);;
                connection.setDoOutput(true);

                // Create JSON data
                JSONObject json = new JSONObject();
                json.put("rating", rating);
                json.put("comment", comment);

                // Send the JSON data
                OutputStream os = connection.getOutputStream();
                os.write(json.toString().getBytes());
                os.flush();
                os.close();

                // Handle the response
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    runOnUiThread(() -> {
                        Toast.makeText(MakeCommentActivity.this, "Comment submitted successfully!", Toast.LENGTH_SHORT).show();
                        finish(); // Close the activity
                    });
                } else {
                    Log.e("MakeCommentActivity", "Error: Response code " + responseCode);
                    runOnUiThread(() -> Toast.makeText(MakeCommentActivity.this, "You can only make 1 comment", Toast.LENGTH_SHORT).show());
                }

                connection.disconnect();
            } catch (Exception e) {
                Log.e("MakeCommentActivity", "Exception: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(MakeCommentActivity.this, "An error occurred", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
