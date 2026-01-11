package com.khchan744.smart_qr_pay.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.khchan744.smart_qr_pay.R;
import com.khchan744.smart_qr_pay.bgservice.Format;
import com.khchan744.smart_qr_pay.network.NetworkClient;
import com.khchan744.smart_qr_pay.network.dto.CredentialsReqDto;
import com.khchan744.smart_qr_pay.network.dto.LoginRespDto;
import com.khchan744.smart_qr_pay.param.UserProfile;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LogInActivity extends AppCompatActivity {

    private static final int USERNAME_MAX_LEN = 15;

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private Button btnLogIn;
    private Button btnRegister;

    private boolean usernameValid = false;
    private boolean passwordValid = false;

    private UserProfile userProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_log_in);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogIn = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        userProfile = UserProfile.getInstance();

        btnLogIn.setOnClickListener(v -> {
            handleLogIn();});
        btnRegister.setOnClickListener(v -> {
            handleClickRegistration();});

        TextWatcher inputWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateInputs();
            }
            @Override public void afterTextChanged(Editable editable) {}
        };

        etUsername.addTextChangedListener(inputWatcher);
        etPassword.addTextChangedListener(inputWatcher);
    }

    @Override
    protected void onResume(){
        super.onResume();
        etUsername.setText(""); etPassword.setText("");
    }

    private void validateInputs() {
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        usernameValid = !username.isEmpty()
                && username.length() <= USERNAME_MAX_LEN
                && username.matches("^[A-Za-z0-9]+$");

        passwordValid = !password.isEmpty();

        btnLogIn.setEnabled(usernameValid && passwordValid);
    }

    private void handleLogIn(){
        btnLogIn.setEnabled(false);
        String username = etUsername.getText() != null ? etUsername.getText().toString() : "";
        userProfile.setUsername(username);
        if(NetworkClient.IS_AVAILABLE){
            String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
            CredentialsReqDto request = new CredentialsReqDto(username, password);
            NetworkClient.getAuthApi().logIn(request).enqueue(new Callback<LoginRespDto>() {
                @Override
                public void onResponse(Call<LoginRespDto> call, Response<LoginRespDto> response) {
                    btnLogIn.setEnabled(usernameValid && passwordValid);

                    if (!response.isSuccessful()) {
                        if(response.code() == 401){
                            Toast.makeText(LogInActivity.this,
                                    "Authentication failed! Incorrect username or password!",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        Toast.makeText(LogInActivity.this,
                                "Request failed: HTTP " + response.code(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    LoginRespDto respBody = response.body();
                    if (respBody == null) {
                        Toast.makeText(LogInActivity.this,
                                "Empty response from server.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String message = respBody.getResponse() != null ? respBody.getResponse() : "";
                    String jwt = respBody.getJwt() != null ? respBody.getJwt() : "";
                    String secret = respBody.getSecret() != null ? respBody.getSecret() : "";
                    byte[] secretBytes = Format.base64Decode(secret);
                    userProfile.setJwt(jwt);
                    userProfile.setSecret(secret);
                    userProfile.setSecretBytes(secretBytes);

                    Toast.makeText(LogInActivity.this, message, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LogInActivity.this, MainMenuActivity.class));
                }

                @Override
                public void onFailure(Call<LoginRespDto> call, Throwable t) {
                    btnLogIn.setEnabled(usernameValid && passwordValid);
                    Toast.makeText(LogInActivity.this,
                            "Network error: " + (t.getMessage() != null ? t.getMessage() : "unknown"),
                            Toast.LENGTH_SHORT).show();
                }
            });


        }else{
            // credentials.setHashedPw(GlobalConst.TEST_HASHED_PW);
            Toast.makeText(this, "Log in success!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainMenuActivity.class));
        }
    }

    private void handleClickRegistration(){
        startActivity(new Intent(this, RegistrationActivity.class));
    }
}