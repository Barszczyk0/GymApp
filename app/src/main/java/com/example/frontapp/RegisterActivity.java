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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextSurname;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText editTextConfirmPassword; // Added Confirm Password Field
    private Button buttonRegister;
    private Button buttonGoToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editTextName = findViewById(R.id.editTextName);
        editTextSurname = findViewById(R.id.editTextSurname);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword); // Initialize Confirm Password
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonGoToLogin = findViewById(R.id.buttonGoToLogin);

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editTextName.getText().toString();
                String surname = editTextSurname.getText().toString();
                String email = editTextEmail.getText().toString();
                String password = editTextPassword.getText().toString();
                String confirmPassword = editTextConfirmPassword.getText().toString(); // Get Confirm Password

                // Validate all fields
                if (!name.isEmpty() && !surname.isEmpty() && !email.isEmpty() && !password.isEmpty() && !confirmPassword.isEmpty()) {
                    // Check if password and confirm password match
                    if (password.equals(confirmPassword)) {
                        registerUser(name, surname, email, password);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void registerUser(String name, String surname, String email, String password) {
        new Thread(() -> {
            try {
                // Build the URL
                URL url = new URL("http://10.0.2.2:5000/api/auth/register");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Create JSON data
                JSONObject json = new JSONObject();
                json.put("name", name);
                json.put("surname", surname);
                json.put("email", email);
                json.put("password", password);

                OutputStream os = connection.getOutputStream();
                os.write(json.toString().getBytes());
                os.flush();
                os.close();

                // Handle the response
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show());
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this, "An error occurred", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
