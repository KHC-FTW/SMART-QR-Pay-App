package com.khchan744.smart_qr_pay.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.khchan744.smart_qr_pay.R;
import com.khchan744.smart_qr_pay.network.NetworkClient;
import com.khchan744.smart_qr_pay.network.dto.StandardRespDto;
import com.khchan744.smart_qr_pay.network.dto.TopUpReqDto;
import com.khchan744.smart_qr_pay.param.UserProfile;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TopUpActivity extends AppCompatActivity {

    private static final int MAX_TOTAL_CHARS = 6;     // e.g. 9999.9
    private static final int MAX_INT_DIGITS = 4;      // 0 - 9999
    private static final int MAX_DEC_DIGITS = 1;     // 0 - 9

    private TextView tvAmountValue;
    private Button btnTopUp;
    private UserProfile userProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_top_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvAmountValue = findViewById(R.id.tvAmountValue);
        btnTopUp = findViewById(R.id.btnTopUp);
        btnTopUp.setOnClickListener(v -> handleTopUp());
        userProfile = UserProfile.getInstance();
        setupKeypadListeners();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

    }

    private void handleTopUp() {
        if(userProfile.isOnline()){
            String bearerToken = "Bearer " + userProfile.getJwt();
            String topUpAmount = safeText(tvAmountValue);
            if(topUpAmount.isBlank()){
                String errorMsg = "Fail to retrieve top up value. Please re-try.";
                Toast.makeText(TopUpActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                return;
            }
            NetworkClient.getAuthApi().topUp(bearerToken, new TopUpReqDto(topUpAmount))
                    .enqueue(new Callback<StandardRespDto>() {
                @Override
                public void onResponse(Call<StandardRespDto> call, Response<StandardRespDto> response) {
                    if (!response.isSuccessful()) {
                        String errorMsg = "Request failed: HTTP " + response.code();
                        Toast.makeText(TopUpActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    StandardRespDto respBody = response.body();
                    if (respBody == null) {
                        String errorMsg = "Empty response from server.";
                        Toast.makeText(TopUpActivity.this,errorMsg, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(respBody.isSuccess()){
                        String message = "Top up success!";
                        Toast.makeText(TopUpActivity.this, message, Toast.LENGTH_SHORT).show();
                        finish();
                    }else{
                        String message = "Top up failed! Please re-try.";
                        Toast.makeText(TopUpActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<StandardRespDto> call, Throwable t) {
                    String failMsg = "Network error: " + (t.getMessage() != null ? t.getMessage() : "unknown");
                    Toast.makeText(TopUpActivity.this, failMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            String errMsg = "Network unavailable so this action is not performed.";
            Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupKeypadListeners(){
        findViewById(R.id.btn0).setOnClickListener(v -> onKeypadPress("0"));
        findViewById(R.id.btn1).setOnClickListener(v -> onKeypadPress("1"));
        findViewById(R.id.btn2).setOnClickListener(v -> onKeypadPress("2"));
        findViewById(R.id.btn3).setOnClickListener(v -> onKeypadPress("3"));
        findViewById(R.id.btn4).setOnClickListener(v -> onKeypadPress("4"));
        findViewById(R.id.btn5).setOnClickListener(v -> onKeypadPress("5"));
        findViewById(R.id.btn6).setOnClickListener(v -> onKeypadPress("6"));
        findViewById(R.id.btn7).setOnClickListener(v -> onKeypadPress("7"));
        findViewById(R.id.btn8).setOnClickListener(v -> onKeypadPress("8"));
        findViewById(R.id.btn9).setOnClickListener(v -> onKeypadPress("9"));
        findViewById(R.id.btnDot).setOnClickListener(v -> onKeypadPress("."));
        findViewById(R.id.btnBackspace).setOnClickListener(v -> onKeypadPress("x"));
    }

    private void onKeypadPress(@NonNull String key) {
        String current = safeText(tvAmountValue);
        if ("x".equals(key)) {
            if (!current.isEmpty()) {
                tvAmountValue.setText(current.substring(0, current.length() - 1));
            }
        } else if (".".equals(key)) {
            if (!current.isEmpty() && !current.contains(".")) {
                tvAmountValue.setText(current + ".");
            }
        } else { // Digit
            if (current.length() >= MAX_TOTAL_CHARS) return;
            if ("0".equals(current)) return; // Must be followed by a dot

            String[] parts = splitAmountParts(current);
            boolean hasDot = current.contains(".");

            if (!hasDot) {
                if (parts[0].length() < MAX_INT_DIGITS) {
                    tvAmountValue.setText(current + key);
                }
            } else {
                if (parts[1].isEmpty()) {
                    tvAmountValue.setText(current + key);
                }
            }
        }
        updateGenerateButtonEnabled();
    }

    @NonNull
    private static String safeText(@NonNull TextView tv) {
        CharSequence cs = tv.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    @NonNull
    private String[] splitAmountParts(@NonNull String amount) {
        int dotIdx = amount.indexOf('.');
        if (dotIdx < 0) return new String[]{amount, ""};
        String integer = amount.substring(0, dotIdx);
        String fraction = (dotIdx + 1 < amount.length()) ? amount.substring(dotIdx + 1) : "";
        return new String[]{integer, fraction};
    }

    private void updateGenerateButtonEnabled() {
        String amount = safeText(tvAmountValue);
        btnTopUp.setEnabled(isValidNonZeroAmount(amount));
    }

    private boolean isValidNonZeroAmount(@NonNull String amount) {
        if (TextUtils.isEmpty(amount) || amount.endsWith(".")) return false;
        try {
            return Double.parseDouble(amount) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}