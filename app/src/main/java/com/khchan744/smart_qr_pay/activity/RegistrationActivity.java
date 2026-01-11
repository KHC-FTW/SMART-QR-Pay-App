package com.khchan744.smart_qr_pay.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.khchan744.smart_qr_pay.R;
import com.khchan744.smart_qr_pay.network.NetworkClient;
import com.khchan744.smart_qr_pay.network.dto.CredentialsReqDto;
import com.khchan744.smart_qr_pay.network.dto.StandardRespDto;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistrationActivity extends AppCompatActivity {

    private static final int USERNAME_MAX_LEN = 15;
    private static final int USERNAME_MIN_LEN = 5;
    private static final int PASSWORD_MAX_LEN = 15;
    private static final int PASSWORD_MIN_LEN = 8;

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private TextInputEditText etReenterPassword;
    private TextView tvUsernameHint;
    private TextView tvPasswordHint;
    private TextView tvReenterPasswordHint;
    private Button btnRegister;
    private Button btnBack;

    private boolean isUsernameValid = false;
    private boolean isPasswordValid = false;
    private boolean isReenterPasswordValid = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etReenterPassword = findViewById(R.id.etReenterPassword);
        tvUsernameHint = findViewById(R.id.tvUsernameHint);
        tvUsernameHint.setVisibility(View.GONE);
        tvPasswordHint = findViewById(R.id.tvPasswordHint);
        tvPasswordHint.setVisibility(View.GONE);
        tvReenterPasswordHint = findViewById(R.id.tvReenterPasswordHint);
        tvReenterPasswordHint.setVisibility(View.GONE);
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);

        TextWatcher usernameInputWatcher = new TextWatcher() {
            @Override public void afterTextChanged(Editable editable) {}
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateUsername();
            }
        };

        TextWatcher passwordInputWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {}
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                validatePassword();
            }
        };

        TextWatcher reenterPasswordInputWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {}
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                validateReenterPassword();
            }
        };

        etUsername.addTextChangedListener(usernameInputWatcher);
        etPassword.addTextChangedListener(passwordInputWatcher);
        etReenterPassword.addTextChangedListener(reenterPasswordInputWatcher);

        btnRegister.setOnClickListener(v -> handleRegistration());
        btnBack.setOnClickListener(v -> finish());
    }

    private void validateUsername(){
        String username = etUsername.getText() != null ? etUsername.getText().toString() : "";
        isUsernameValid = username.length() >= USERNAME_MIN_LEN
                && username.length() <= USERNAME_MAX_LEN
                && username.matches("^[A-Za-z0-9]+$");

        tvUsernameHint.setVisibility(isUsernameValid ? View.GONE : View.VISIBLE);
        btnRegister.setEnabled(isUsernameValid && isPasswordValid && isReenterPasswordValid);
    }

    private void validatePassword(){
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        isPasswordValid = password.length() >= PASSWORD_MIN_LEN
                && password.length() <= PASSWORD_MAX_LEN;
        tvPasswordHint.setVisibility(isPasswordValid ? View.GONE : View.VISIBLE);
        btnRegister.setEnabled(isUsernameValid && isPasswordValid && isReenterPasswordValid);
    }

    private void validateReenterPassword(){
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String reenterPassword = etReenterPassword.getText() != null ? etReenterPassword.getText().toString() : "";
        isReenterPasswordValid = password.equals(reenterPassword);
        tvReenterPasswordHint.setVisibility(isReenterPasswordValid ? View.GONE : View.VISIBLE);
        btnRegister.setEnabled(isUsernameValid && isPasswordValid && isReenterPasswordValid);
    }

    private void handleRegistration() {
        String username = etUsername.getText() != null ? etUsername.getText().toString() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        // Basic guard. Button SHOULD already be disabled unless valid, but keep it safe.
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username and password cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);

        CredentialsReqDto request = new CredentialsReqDto(username, password);
        NetworkClient.getAuthApi().register(request).enqueue(new Callback<StandardRespDto>() {
            @Override
            public void onResponse(Call<StandardRespDto> call, Response<StandardRespDto> response) {
                btnRegister.setEnabled(isUsernameValid && isPasswordValid && isReenterPasswordValid);

                if (!response.isSuccessful()) {
                    Toast.makeText(RegistrationActivity.this,
                            "Request failed: HTTP " + response.code(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                StandardRespDto respBody = response.body();
                if (respBody == null) {
                    Toast.makeText(RegistrationActivity.this,
                            "Empty response from server.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                String message = respBody.getResponse() != null ? respBody.getResponse() : "";
                Toast.makeText(RegistrationActivity.this, message, Toast.LENGTH_LONG).show();

                if (respBody.isSuccess()) {
                    finish();
                }else etUsername.setText("");
            }

            @Override
            public void onFailure(Call<StandardRespDto> call, Throwable t) {
                btnRegister.setEnabled(isUsernameValid && isPasswordValid && isReenterPasswordValid);
                Toast.makeText(RegistrationActivity.this,
                        "Network error: " + (t.getMessage() != null ? t.getMessage() : "unknown"),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}