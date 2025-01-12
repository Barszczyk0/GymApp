package com.example.frontapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private EditText editTextPassword;
    private Button buttonLogin;
    private Button buttonGoToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonGoToRegister = findViewById(R.id.buttonGoToRegister);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = editTextUsername.getText().toString();
                String password = editTextPassword.getText().toString();

                if (!username.isEmpty() && !password.isEmpty()) {
                    loginUser(username, password);
                } else {
                    Toast.makeText(LoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser(String email, String password) {
        new Thread(() -> {
            try {
                URL url = new URL("http://10.0.2.2:5000/api/auth/login");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("password", password);

                OutputStream os = connection.getOutputStream();
                os.write(json.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");

                    String csrfToken = null;
                    if (cookies != null) {
                        for (String cookie : cookies) {
                            if (cookie.startsWith("csrf_access_token=")) {
                                csrfToken = cookie.split(";")[0].split("=")[1];
                                break;
                            }
                        }
                    }

                    String token = null;
                    if (cookies != null) {
                        for (String cookie : cookies) {
                            if (cookie.startsWith("access_token_cookie=")) {
                                token = cookie.split(";")[0].split("=")[1];
                                break;
                            }
                        }
                    }

                    if (token != null) {
                        Log.d("LoginActivity", "Token: " + token);

                        String finalToken = token;
                        String finalCsrfToken = csrfToken;
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("TOKEN", finalToken);
                            intent.putExtra("X-CSRF-TOKEN", finalCsrfToken);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        Log.e("LoginActivity", "Token not found in cookies");
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login failed: Token not found", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    br.close();

                    JSONObject errorJson = new JSONObject(errorResponse.toString());
                    String errorMessage = errorJson.optString("message", "Login failed");

                    Log.e("LoginActivity", "Error Response: " + errorResponse.toString());

                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }

                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "An error occurred", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}